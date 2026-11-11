package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.*;
import co.aplomo.modelo.Cuenta;
import co.aplomo.rula.*;
import co.aplomo.seguridad.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-017 a RF-021: autorización, persistencia y auditoría; el cálculo permanece en el módulo RULA. */
public final class Analisis {
    /** Referencia a una foto y sus entradas; no incluye ningún puntaje enviado por la pantalla. */
    public record Solicitud(int foto,MotorRula.Entrada entrada) {}
    private final Transacciones tx;private final Clock reloj;private final Fotos archivos;private final BitacoraDao bitacora;
    private final TablasRula tablas=new TablasRula();private final AnalisisDao dao=new AnalisisDao();
    /** Comparte transacciones, fotos y reloj con las evaluaciones y las pruebas. */
    public Analisis(Transacciones tx,Clock reloj,Fotos archivos,BitacoraDao bitacora) {
        this.tx=tx;this.reloj=reloj;this.archivos=archivos;this.bitacora=bitacora;
    }
    /** Calcula una vista previa autorizada, sin guardar ni aceptar puntajes del navegador. */
    public MotorRula.Calculo previsualizar(Cuenta actor,int visita,MotorRula.Entrada entrada) throws SQLException {
        return tx.ejecutar(c->{admin(c,actor);abierta(c,visita,false);tablas.cargar(c);return new MotorRula(tablas).calcular(entrada);});
    }
    /** Inserta un análisis dentro de la transacción de una evaluación, usando las dimensiones reales de su foto. */
    int guardarEnTransaccion(Connection c,Cuenta actor,int foto,MotorRula.Entrada entrada) throws SQLException,IOException {
        if(entrada==null)throw new IllegalArgumentException("Completa las mediciones de la foto.");
        var f=new EvaluacionDao().foto(c,foto);if(f==null)throw new SecurityException("La foto no está disponible.");
        int[] dimensiones=archivos.dimensiones(f.nombre());
        var real=new MotorRula.Entrada(entrada.lado(),dimensiones[0],dimensiones[1],entrada.mediciones(),entrada.ajustes(),entrada.actividad(),entrada.carga());
        tablas.cargar(c);var calculo=new MotorRula(tablas).calcular(real);
        int id=dao.guardar(c,foto,LocalDateTime.now(reloj),calculo);
        bitacora.registrar(c,actor.id(),LocalDateTime.now(reloj),"analisis_rula",id,"CREAR",null,null,"Análisis nuevo · "+real.lado()+" · Foto "+foto);
        return id;
    }
    /** Añade análisis a fotos ya guardadas; verifica relación, visita abierta y conserva todo el historial. */
    public void guardar(Cuenta actor,int evaluacion,List<Solicitud> solicitudes) throws SQLException,IOException {
        try {
            tx.ejecutar(c->{
                Cuenta responsable=admin(c,actor);var e=new EvaluacionDao().consultar(c,evaluacion);
                if(e==null)throw new SecurityException("La evaluación no está disponible.");
                var previa=abierta(c,e.visita(),false);new ReservaDao().buscar(c,previa.visita().reserva(),0,true);abierta(c,e.visita(),true);
                new PuestoDao().buscar(c,e.puesto(),0,true);
                if(solicitudes==null||solicitudes.isEmpty()||solicitudes.size()>10)throw new IllegalArgumentException("Completa al menos una foto y guarda hasta diez análisis a la vez.");
                Set<Integer> vistas=new HashSet<>();
                for(var s:solicitudes){
                    var f=new EvaluacionDao().foto(c,s.foto());
                    if(f==null||f.evaluacion()!=evaluacion)throw new SecurityException("La foto no pertenece a esta evaluación.");
                    if(!vistas.add(s.foto()))throw new IllegalArgumentException("Selecciona cada foto una sola vez; puedes volver a analizarla después.");
                    try{guardarEnTransaccion(c,responsable,s.foto(),s.entrada());}catch(IOException x){throw new UncheckedIOException(x);}
                }
                dao.actualizarPuesto(c,e.puesto());return null;
            });
        }catch(UncheckedIOException x){throw x.getCause();}
    }
    /** Consulta el historial administrativo sin entregar fotos a un Usuario. */
    public List<AnalisisDao.Resumen> listar(Cuenta actor,int evaluacion) throws SQLException {
        return tx.ejecutar(c->{admin(c,actor);return dao.listar(c,evaluacion);});
    }
    /** Resultado histórico autorizado, con fecha y responsable (RF-021, RNF-005). */
    public record Guardado(AnalisisDao.Ficha ficha,MotorRula.Calculo calculo) {}
    /** Abre el resultado guardado sin repetir ni reemplazar un análisis. */
    public Guardado consultar(Cuenta actor,int evaluacion,int analisis) throws SQLException {
        return tx.ejecutar(c->{admin(c,actor);var f=dao.consultar(c,analisis);if(f==null||f.evaluacion()!=evaluacion)throw new SecurityException("El análisis no está disponible.");tablas.cargar(c);return new Guardado(f,new MotorRula(tablas).restaurar(f.entrada(),dao.mediciones(c,analisis),f.puntajeA(),f.puntajeB(),f.fin()));});
    }
    private static Cuenta admin(Connection c,Cuenta a)throws SQLException{var actual=AccesoDatos.vigente(c,a);Permisos.administrador(actual);return actual;}
    private static VisitaDao.Ficha abierta(Connection c,int visita,boolean bloquear)throws SQLException{
        var f=new VisitaDao().buscar(c,visita,0,false,bloquear);if(f==null)throw new SecurityException("La visita no está disponible.");
        if(!f.visita().estado().equals("EN_CURSO"))throw new IllegalArgumentException("La visita ya está finalizada. Sus análisis se conservan sin cambios.");return f;
    }
}
