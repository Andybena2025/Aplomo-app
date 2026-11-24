package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.*;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Trabajador;
import co.aplomo.rula.*;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;
import java.sql.SQLException;
import java.util.*;

/** RF-002/020/023/027: explicación autorizada; nunca persiste el puntaje proyectado. */
public final class Resultados40 {
    /** RF-021: conserva el cálculo autorizado junto a sus datos de presentación. */
    public record Vista(MotorRula.Calculo calculo, Map<String, Object> datos) { }

    private final Evaluaciones servicio;
    private final Transacciones tx;

    /** RF-002: comparte los servicios y transacciones existentes. */
    public Resultados40(Evaluaciones servicio, Transacciones tx) { this.servicio = servicio; this.tx = tx; }

    /** RF-020/027: previsualiza con el motor existente y verifica el puesto dentro de la visita autorizada. */
    public Vista previsualizar(Cuenta actor, int visita, int puesto, MotorRula.Entrada entrada,
            Map<String, Boolean> condiciones, Trabajador trabajador, int minutos) throws SQLException {
        var contexto = servicio.contexto(actor, visita);
        if (contexto.puestos().stream().noneMatch(p -> p.id() == puesto))
            throw new SecurityException("El puesto no pertenece a esta visita.");
        var calculo = servicio.analisis().previsualizar(actor, visita, entrada);
        var reglas = servicio.sugerencias().previsualizar(actor, calculo, condiciones);
        return tx.ejecutar(con -> {
            var tablas = new TablasRula(); tablas.cargar(con);
            var datos = new PlanAtencion40(tablas).explicar(calculo, reglas);
            completar(datos, actor, trabajador, minutos);
            datos.put("anterior", anterior(con, puesto, 0));
            datos.put("reserva", reserva(actor, contexto.visita().empresa(), contexto.visita().sede(),
                    contexto.visita().salon(), puesto));
            return new Vista(calculo, datos);
        });
    }

    /** RF-002/021: datos históricos de una evaluación, con sugerencias y permisos vigentes. */
    public List<Vista> guardados(Cuenta actor, int evaluacion) throws SQLException {
        var ficha = ficha(actor, evaluacion);
        var ids = tx.ejecutar(con -> Sql.listar(con,
                "SELECT id_evaluacion FROM evaluacion WHERE id_visita=? AND id_puesto=? ORDER BY id_evaluacion",
                f -> f.getInt(1), ficha.visita(), ficha.puesto()));
        List<Vista> resultados = new ArrayList<>();
        for (int id : ids) {
            EvaluacionDao.Ficha ev;
            try { ev = ficha(actor, id); }
            catch (SecurityException ajena) { continue; }
            var resumenes = tx.ejecutar(con -> new AnalisisDao().listar(con, id));
            if (resumenes.isEmpty()) continue;
            boolean editable = actor.rol() != Cuenta.Rol.USUARIO && servicio.permiteTrabajo(actor, id);
            boolean seguimiento = editable || actor.rol() == Cuenta.Rol.USUARIO;
            var contexto = editable ? servicio.sugerencias().contexto(actor, id) : null;
            List<SugerenciaDao.Ficha> sugerencias = seguimiento ? servicio.sugerencias().listar(actor, id) : List.of();
            var reglas = tx.ejecutar(con -> new SugerenciaDao().reglas(con));
            var peor = resumenes.stream().max(Comparator.comparingInt(AnalisisDao.Resumen::puntaje)).orElseThrow();
            var guardado = analisis(actor, id, peor.id());
            var datos = tx.ejecutar(con -> {
                var tablas = new TablasRula(); tablas.cargar(con);
                return new PlanAtencion40(tablas).explicar(guardado.calculo(), List.of());
            });
            datos.put("titulo", ev.datos().postura());
            completar(datos, actor, ev.datos().trabajador(), ev.datos().minutos());
            datos.put("anterior", seguimiento ? tx.ejecutar(con -> anterior(con, ficha.puesto(), ids.get(0))) : null);
            datos.put("reserva", editable ? reserva(actor, contexto.empresa(), contexto.sede(), contexto.salon(),
                    ficha.puesto()) : "");
            datos.put("editable", editable);
            datos.put("guardado", true);
            datos.put("seguimientoDisponible", seguimiento);
            datos.put("sinSugerencias", seguimiento && guardado.calculo().resultado().puntajeFinal() > 2 &&
                    sugerencias.isEmpty());
            datos.put("evaluacion", id);
            var marcas = tx.ejecutar(con -> new SugerenciaDao().marcasPlan(con, id));
            asociar(datos, sugerencias, reglas, marcas);
            resultados.add(new Vista(guardado.calculo(), datos));
        }
        return resultados;
    }

    /** RF-002: Usuario conserva exactamente la consulta existente de empresa propia y visita Finalizada. */
    private EvaluacionDao.Ficha ficha(Cuenta actor, int id) throws SQLException {
        if (actor.rol() != Cuenta.Rol.USUARIO) return servicio.consultar(actor, id).ficha();
        servicio.consultas().evaluacion(actor, id);
        return tx.ejecutar(con -> new EvaluacionDao().consultar(con, id));
    }

    /** RF-002/021: después de autorizar la evaluación, restaura resultados sin leer ni entregar fotos. */
    private Analisis.Guardado analisis(Cuenta actor, int evaluacion, int analisis) throws SQLException {
        if (actor.rol() != Cuenta.Rol.USUARIO) return servicio.analisis().consultar(actor, evaluacion, analisis);
        return tx.ejecutar(con -> {
            var dao = new AnalisisDao(); var f = dao.consultar(con, analisis);
            if (f == null || f.evaluacion() != evaluacion) throw new SecurityException("Acceso denegado.");
            var tablas = new TablasRula(); tablas.cargar(con);
            return new Analisis.Guardado(f, new MotorRula(tablas).restaurar(f.entrada(),
                    dao.mediciones(con, analisis), f.puntajeA(), f.puntajeB(), f.fin()));
        });
    }

    /** RF-023: cada sugerencia conserva su identidad, texto editado y estado, incluso si hay varias por parte. */
    @SuppressWarnings("unchecked")
    private static void asociar(Map<String, Object> datos, List<SugerenciaDao.Ficha> sugerencias,
            List<SugerenciaDao.Regla> reglas, Map<Integer, String> marcas) {
        var segmentos = (List<Map<String, Object>>) datos.get("segmentos");
        for (var segmento : segmentos) {
            String clave = segmento.get("clave").toString();
            var acciones = sugerencias.stream().map(SugerenciaDao.Ficha::sugerencia)
                .filter(s -> s.origen().equals("SISTEMA") && (s.regla() == null ?
                    s.titulo().equals("Plan · " + PlanAtencion40.METAS.get(clave).nombre()) ||
                    ("Plan de atención · " + PlanAtencion40.METAS.get(clave).nombre() + ".")
                        .equals(marcas.get(s.id())) :
                    reglas.stream().anyMatch(r -> r.id() == s.regla() && r.segmento().equals(clave))))
                .map(s -> Map.<String, Object>of("id", s.id(), "estado", s.estado(), "texto", s.descripcion()))
                .toList();
            segmento.put("acciones", acciones);
            if (!acciones.isEmpty()) segmento.put("cambio", acciones.stream().map(a -> a.get("texto").toString())
                    .collect(java.util.stream.Collectors.joining(" ")));
        }
        datos.put("otras", sugerencias.stream().map(SugerenciaDao.Ficha::sugerencia)
            .filter(s -> s.origen().equals("ADMINISTRADOR"))
            .map(s -> Map.of("id", s.id(), "texto", s.descripcion(), "estado", s.estado())).toList());
    }

    /** RF-002: la política de Trabajador es la única fuente de visibilidad; no se incluye en informes RF-026. */
    private static void completar(Map<String, Object> datos, Cuenta actor, Trabajador trabajador, int minutos) {
        var t = trabajador.para(actor);
        Map<String, Object> contexto = new LinkedHashMap<>();
        if (t.genero() != null) contexto.put("genero", t.genero());
        if (t.edad() != null) contexto.put("edad", t.edad());
        if (t.antiguedad() != null) contexto.put("antiguedad", t.antiguedad());
        if (Trabajador.ROLES_VISIBLES.contains(actor.rol()) && minutos > 0) contexto.put("minutos", minutos);
        if (!contexto.isEmpty()) datos.put("trabajador", contexto);
    }

    /** RF-021: última evaluación Finalizada previa; no compara una postura con otra de la visita actual. */
    private static Integer anterior(java.sql.Connection con, int puesto, int evaluacion) throws SQLException {
        return Sql.uno(con, "SELECT MAX(a.puntaje_final) AS puntaje FROM fotografia f " +
                "JOIN analisis_rula a USING(id_foto) " +
                "WHERE f.id_evaluacion=(SELECT e.id_evaluacion FROM evaluacion e JOIN visita v USING(id_visita) " +
                "WHERE e.id_puesto=? AND v.estado='FINALIZADA' AND (?=0 OR e.id_evaluacion<?) " +
                "ORDER BY e.fecha DESC,e.id_evaluacion DESC LIMIT 1) HAVING COUNT(a.id_analisis)>0",
                f -> Sql.enteroOpcional(f, "puntaje"), puesto, evaluacion, evaluacion);
    }

    /** RF-011/RF-002: solo el Evaluador puede reservar; el formulario existente revisa su empresa. */
    private static String reserva(Cuenta actor, int empresa, int sede, int salon, int puesto) {
        return actor.rol() == Cuenta.Rol.EVALUADOR ? "/reservas/nueva?empresa=" + empresa + "&sede=" + sede +
                "&salon=" + salon + "&puesto=" + puesto + "&categoria=REEVALUACION" : "";
    }
}
