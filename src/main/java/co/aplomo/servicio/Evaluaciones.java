package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.*;
import co.aplomo.modelo.*;
import co.aplomo.seguridad.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-014, RF-015, RF-016, RNF-005/006: guarda la evaluación completa en una visita abierta. */
public final class Evaluaciones {
    /** Datos aprobados de postura, seis condiciones y consentimiento; sin datos del trabajador. */
    public record Datos(int puesto, String postura, String criterio, String motivo, int minutos,
                        boolean escritorio, boolean pantalla, boolean pies, boolean apoyapies,
                        boolean apoyabrazos, boolean regulable, String observaciones,
                        boolean aceptaFotos, boolean sinFotos) {}
    /** Contexto permitido al Administrador para iniciar el asistente. */
    public record Contexto(VisitaDao.Ficha visita, List<Puesto> puestos) {}
    /** Contenido autorizado y tipo real para la respuesta HTTP de una fotografía. */
    public record Contenido(byte[] bytes, String tipo) {}
    /** Consulta administrativa sin exponer fotografías a cuentas Usuario. */
    public record Consulta(EvaluacionDao.Ficha ficha, List<EvaluacionDao.Foto> fotos, boolean enCurso) {}
    private final Transacciones tx;
    private final Clock reloj;
    private final Fotos archivos;
    private final BitacoraDao bitacora;
    private final Analisis analisis;
    private final EvaluacionDao dao = new EvaluacionDao();

    /** Usa MySQL, la carpeta fotos/ y la hora de Colombia. */
    public Evaluaciones() { this(new Transacciones(), Clock.system(ZoneId.of("America/Bogota")), new Fotos(), new BitacoraDao()); }
    /** Comparte conexión, reloj y carpeta aislada con las pruebas. */
    public Evaluaciones(Transacciones tx, Clock reloj, Fotos archivos, BitacoraDao bitacora) {
        this.tx = tx; this.reloj = reloj; this.archivos = archivos; this.bitacora = bitacora; this.analisis=new Analisis(tx,reloj,archivos,bitacora);
    }
    /** Comprueba permisos y visita abierta antes de ofrecer puestos activos del salón reservado. */
    public Contexto contexto(Cuenta actor, int visita) throws SQLException {
        return tx.ejecutar(con -> {
            admin(con, actor);
            var ficha = abierta(con, visita, false);
            return new Contexto(ficha, dao.puestos(con, ficha.salon()));
        });
    }

    /** Valida todo, guarda evaluación, condiciones, fotos y auditoría; revierte archivos nuevos ante fallos. */
    public int guardar(Cuenta actor, int visita, Datos entrada, List<Fotos.Entrada> fotos) throws SQLException, IOException {
        return guardar(actor,visita,entrada,fotos,null);
    }

    /** Guarda el asistente completo de forma atómica; cualquier fallo revierte también fotos y análisis (RF-021). */
    public int guardar(Cuenta actor,int visita,Datos entrada,List<Fotos.Entrada> fotos,List<Analisis.Solicitud> solicitudes) throws SQLException,IOException {
        List<String> nuevos = new ArrayList<>();
        try {
            return tx.ejecutar(con -> {
                Cuenta responsable = admin(con, actor);
                // Bloquea primero la reserva para compartir el orden con la finalización de visitas.
                var previa = new VisitaDao().buscar(con, visita, 0, false, false);
                if (previa == null) throw new SecurityException("La visita no está disponible.");
                new ReservaDao().buscar(con, previa.visita().reserva(), 0, true);
                var ficha = abierta(con, visita, true);
                Datos d = validar(entrada);
                Puesto puesto = new PuestoDao().buscar(con, d.puesto(), 0, true);
                if (puesto == null || !puesto.activo() || puesto.salon() != ficha.salon())
                    throw new IllegalArgumentException("Elige un puesto activo del salón de esta visita.");
                boolean jerarquia = Boolean.TRUE.equals(Sql.uno(con, "SELECT (e.activo AND s.activo AND l.activo) FROM salon l JOIN sede s ON s.id_sede=l.id_sede JOIN empresa e ON e.id_empresa=s.id_empresa WHERE l.id_salon=?", r -> r.getBoolean(1), ficha.salon()));
                if (!jerarquia) throw new IllegalArgumentException("La empresa, sede o salón está desactivado. Revisa su estado antes de evaluar.");
                List<Fotos.Imagen> imagenes;
                if (d.sinFotos()) {
                    if (d.aceptaFotos() || fotos != null && !fotos.isEmpty())
                        throw new IllegalArgumentException("Sin registro fotográfico no se aceptan fotos ni la casilla de aceptación.");
                    imagenes = List.of();
                } else {
                    if (!d.aceptaFotos()) throw new IllegalArgumentException("Marca la aceptación de las fotos o elige Sin registro fotográfico.");
                    imagenes = archivos.validar(fotos);
                }
                int id = dao.crear(con, visita, d, d.motivo(), LocalDate.now(reloj));
                dao.condiciones(con, id, d);
                List<Integer> idsFotos=new ArrayList<>();
                for (var imagen : imagenes) {
                    String nombre;
                    try { nombre = archivos.guardar(imagen); }
                    catch (IOException e) { throw new UncheckedIOException(e); }
                    nuevos.add(nombre);
                    idsFotos.add(dao.foto(con, id, nombre, imagen.vista(), imagen.descripcion(), imagen.bytes().length));
                }
                bitacora.registrar(con, responsable.id(), LocalDateTime.now(reloj), "evaluacion", id, "CREAR", null,
                        null, "Evaluación del puesto " + puesto.numero() + (d.sinFotos() ? " · Sin registro fotográfico" : " · Con fotos"));
                if(solicitudes!=null) {
                    if(d.sinFotos()&&!solicitudes.isEmpty())throw new IllegalArgumentException("Sin fotos no se puede guardar un análisis RULA.");
                    if(!d.sinFotos()&&(solicitudes.isEmpty()||solicitudes.size()>10))throw new IllegalArgumentException("Mide al menos una foto antes de guardar el resultado.");
                    Set<Integer> usadas=new HashSet<>();
                    for(var solicitud:solicitudes) {
                        if(solicitud.foto()<0||solicitud.foto()>=idsFotos.size()||!usadas.add(solicitud.foto()))throw new IllegalArgumentException("Revisa la selección de fotos de los análisis.");
                        try{analisis.guardarEnTransaccion(con,responsable,idsFotos.get(solicitud.foto()),solicitud.entrada());}catch(IOException x){throw new UncheckedIOException(x);}
                    }
                }
                new AnalisisDao().actualizarPuesto(con,d.puesto());
                return id;
            });
        } catch (SQLException | RuntimeException fallo) {
            try { archivos.revertirNuevos(nuevos); } catch (IOException limpieza) { fallo.addSuppressed(limpieza); }
            if (fallo instanceof UncheckedIOException e) throw e.getCause();
            throw fallo;
        }
    }

    /** Autoriza antes de leer la ruta y entregar una foto; un Usuario nunca recibe contenido ni metadatos. */
    public Contenido foto(Cuenta actor, int id) throws SQLException, IOException {
        var foto = tx.ejecutar(con -> { admin(con, actor); return dao.foto(con, id); });
        if (foto == null) throw new FileNotFoundException("Foto no disponible.");
        return new Contenido(archivos.leer(foto.nombre()), foto.nombre().endsWith(".png") ? "image/png" : "image/jpeg");
    }

    /** Abre la evaluación guardada con sus condiciones y fotografías solo para Administrador. */
    public Consulta consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con->{
            admin(con,actor);var ficha=dao.consultar(con,id);
            if(ficha==null) throw new SecurityException("La evaluación no está disponible.");
            var visita=new VisitaDao().buscar(con,ficha.visita(),0,false,false);
            return new Consulta(ficha,dao.fotos(con,id),visita!=null&&visita.visita().estado().equals("EN_CURSO"));
        });
    }

    /** Comparte el servicio de análisis para vista previa y nuevos análisis de fotos existentes. */
    public Analisis analisis() { return analisis; }

    /** Comparte seguimiento, transacciones y reloj con las rutas del Paso 9. */
    public Pendientes pendientes(){return new Pendientes(tx,reloj,bitacora);}

    /** RF-023a/b: comparte reglas, permisos y auditoría con las rutas de sugerencias. */
    public Sugerencias sugerencias(){return new Sugerencias(tx,reloj,bitacora);}

    /** RF-024: consultas de solo lectura que comparten la transacción autorizada. */
    public Consultas consultas(){return new Consultas(tx);}

    /** Cuenta evaluaciones y puestos rojos visibles para las tarjetas de Inicio (RF-024). */
    public long[] resumen(Cuenta actor) throws SQLException {
        return tx.ejecutar(c->{
            Cuenta actual=AccesoDatos.vigente(c,actor);int empresa=AccesoDatos.empresaPermitida(actual);boolean usuario=actual.rol()==Cuenta.Rol.USUARIO;
            long evaluaciones=Sql.uno(c,"SELECT COUNT(*) FROM evaluacion e JOIN visita v USING(id_visita) JOIN puesto_trabajo p USING(id_puesto) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE (?=0 OR s.id_empresa=?) AND (?=FALSE OR v.estado='FINALIZADA')",r->r.getLong(1),empresa,empresa,usuario);
            long rojos;
            if(!usuario)rojos=Sql.uno(c,"SELECT COUNT(*) FROM puesto_trabajo p JOIN nivel_riesgo n ON p.ultimo_puntaje BETWEEN n.puntaje_minimo AND n.puntaje_maximo WHERE n.color=?",r->r.getLong(1),"ROJO");
            else rojos=Sql.uno(c,"SELECT COUNT(*) FROM puesto_trabajo p JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE s.id_empresa=? AND EXISTS (SELECT 1 FROM nivel_riesgo n WHERE n.color=? AND (SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) WHERE f.id_evaluacion=(SELECT MAX(e.id_evaluacion) FROM evaluacion e JOIN visita v USING(id_visita) WHERE e.id_puesto=p.id_puesto AND v.estado='FINALIZADA')) BETWEEN n.puntaje_minimo AND n.puntaje_maximo)",r->r.getLong(1),empresa,"ROJO");
            return new long[]{evaluaciones,rojos};
        });
    }

    private static Datos validar(Datos d) {
        if (d == null) throw new IllegalArgumentException("Completa los datos de la evaluación.");
        String postura = Validacion.texto(d.postura(), "postura o tarea", 150);
        String criterio = Validacion.opcion(d.criterio(), "criterio de elección", "MAS_INADECUADA", "MAYOR_EXPOSICION");
        String motivo = Validacion.texto(d.motivo(), "motivo de elección de la postura", 255);
        Validacion.entero(Integer.toString(d.minutos()), "minutos por jornada", 1, 720);
        String obs = Validacion.notas(d.observaciones());
        if (obs.codePointCount(0, obs.length()) > 255) throw new IllegalArgumentException("Otras observaciones permite hasta 255 caracteres.");
        return new Datos(d.puesto(), postura, criterio, motivo, d.minutos(), d.escritorio(), d.pantalla(), d.pies(),
                d.apoyapies(), d.apoyabrazos(), d.regulable(), obs, d.aceptaFotos(), d.sinFotos());
    }
    private static Cuenta admin(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor); Permisos.administrador(actual); return actual;
    }
    private static VisitaDao.Ficha abierta(Connection con, int id, boolean bloquear) throws SQLException {
        var ficha = new VisitaDao().buscar(con, id, 0, false, bloquear);
        if (ficha == null) throw new SecurityException("La visita no está disponible.");
        if (!ficha.visita().estado().equals("EN_CURSO")) throw new IllegalArgumentException("Solo puedes evaluar dentro de una visita En curso.");
        return ficha;
    }
}
