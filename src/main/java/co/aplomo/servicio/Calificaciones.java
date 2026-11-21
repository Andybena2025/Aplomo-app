package co.aplomo.servicio;

import co.aplomo.dao.CalificacionesDao;
import co.aplomo.dao.EmpresaDao;
import co.aplomo.dao.SedeDao;
import co.aplomo.dao.CalificacionesDao.PorSede;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.InformeEmpresa;
import co.aplomo.modelo.InformeEmpresa.Calificacion;
import co.aplomo.modelo.InformeEmpresa.Estadisticas;
import co.aplomo.modelo.InformeEmpresa.PendienteResumen;
import co.aplomo.modelo.InformeEmpresa.PuestoResumen;
import co.aplomo.modelo.InformeEmpresa.Riesgo;
import co.aplomo.modelo.InformeEmpresa.SedeResumen;
import co.aplomo.modelo.InformeEmpresa.SugerenciaResumen;
import co.aplomo.modelo.InformeEmpresa.VisitaResumen;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * RF-025/026: indicadores del equipo y composición del informe, sin recalcular RULA ni modificar datos.
 */
public final class Calificaciones {

    /**
     * (Propuesta), respuesta de Andy del 11 nov 2026: mínimo de sedes calificadas para una general.
     */
    public static final int MINIMO_SEDES_GENERAL = 2;

    /**
     * (Propuesta) Clasificar el promedio real al entero más cercano, mitades hacia arriba.
     */
    public static final RoundingMode REDONDEO_COLOR = RoundingMode.HALF_UP;
    private final Transacciones tx;
    private final Clock reloj;
    private final CalificacionesDao dao = new CalificacionesDao();

    /**
     * Comparte la conexión y el reloj de la aplicación y de sus pruebas reversibles. RNF-011.
     */
    public Calificaciones(Transacciones tx, Clock reloj) {
        this.tx = tx;
        this.reloj = reloj;
    }

    /**
     * Ofrece solo empresas autorizadas; no elige una empresa arbitrariamente. RNF-011.
     */
    public List<Empresa> empresas(Cuenta actor) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            return new EmpresaDao().buscarTodos(conexion, 0, AccesoDatos.empresaPermitida(cuenta));
        });
    }

    /**
     * Usuario consulta únicamente indicadores de su propia empresa, siempre de visitas Finalizadas. RNF-011.
     */
    public InformeEmpresa consultar(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(conexion -> cargar(conexion, AccesoDatos.vigente(conexion, actor), empresa));
    }

    /**
     * Solo el Administrador genera una vista imprimible y solo si existe una visita Finalizada. RNF-011.
     */
    public InformeEmpresa informe(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(conexion -> {
            var cuenta = AccesoDatos.vigente(conexion, actor);
            Permisos.trabajo(cuenta);
            var informe = cargar(conexion, cuenta, empresa);
            if (informe.sedes().stream().allMatch(empresa2 -> empresa2.visitas().isEmpty()))
                throw new IllegalArgumentException(
                        "Esta empresa aún no tiene visitas Finalizadas. Finaliza al menos una visita " +
                                "para generar el informe.");
            return informe;
        });
    }

    private InformeEmpresa cargar(Connection conexion, Cuenta actor, int empresa) throws SQLException {
        if (empresa < 1) throw new IllegalArgumentException("Elige una empresa para consultar sus calificaciones.");
        var permitida = AccesoDatos.empresaPermitida(actor);
        Empresa empresa2 = new EmpresaDao().buscar(conexion, empresa, permitida, false);
        if (empresa2 == null || co.aplomo.modelo.Demostracion.NIT_PRACTICA.equals(empresa2.nit()))
            throw new SecurityException("Acceso denegado. La empresa no está disponible para tu cuenta.");
        var sedes = new SedeDao().buscarTodos(conexion, empresa, permitida);
        var riesgos = dao.riesgos(conexion);
        var visitas = dao.visitas(conexion, empresa);
        var puestos = dao.puestos(conexion, empresa);
        var pendientes = dao.pendientes(conexion, empresa);
        var sugerencias = dao.sugerencias(conexion, empresa);
        var fotos = dao.fotosAnalizadas(conexion, empresa);
        List<SedeResumen> resumenes = new ArrayList<>();
        for (var sede : sedes) {
            var vs = deSede(visitas, sede.id()).stream()
                    .map(visita -> new VisitaResumen(visita.id(), visita.codigo(), visita.inicio(), visita.fin(),
                            visita.notas(),
                            visita.calificacion() == null ? null : calificar(visita.calificacion().promedio(),
                                    riesgos))).toList();
            var ps = deSede(puestos, sede.id()).stream()
                    .map(empresa3 -> new PuestoResumen(empresa3.id(), empresa3.evaluacion(), empresa3.visita(),
                            empresa3.numero(), empresa3.salon(), empresa3.area(), empresa3.cargo(),
                            empresa3.tipoCargo(), empresa3.descripcion(), empresa3.postura(), empresa3.motivo(),
                            empresa3.condiciones(), empresa3.fecha(), empresa3.puntaje(),
                            empresa3.puntaje() == null ? null : riesgo(empresa3.puntaje(), riesgos))).toList();
            var pa = deSede(pendientes, sede.id()).stream()
                    .map(empresa4 -> new PendienteResumen(empresa4.id(), empresa4.evaluacion(), empresa4.numero(),
                            empresa4.salon(), empresa4.descripcion(), empresa4.prioridad(), empresa4.estado(),
                            empresa4.fecha(), empresa4.limite(), empresa4.puntaje(),
                            riesgo(empresa4.puntaje(), riesgos))).toList();
            var sg = deSede(sugerencias, sede.id());
            Calificacion nota = promediar(
                    vs.stream().map(VisitaResumen::calificacion).filter(Objects::nonNull).toList(), riesgos);
            resumenes.add(new SedeResumen(sede, nota, vs, ps, pa, sg,
                    estadisticas(vs, ps, pa, sg, fotos.getOrDefault(sede.id(), 0))));
        }
        var calificadas = resumenes.stream().map(SedeResumen::calificacion).filter(Objects::nonNull).toList();
        Calificacion general = calificadas.size() >= MINIMO_SEDES_GENERAL ? promediar(calificadas, riesgos) : null;
        var faltan = resumenes.stream().filter(empresa5 -> empresa5.calificacion() == null)
                .map(empresa6 -> empresa6.sede().nombre()).toList();
        var totales = estadisticas(resumenes.stream().flatMap(empresa7 -> empresa7.visitas().stream()).toList(),
                resumenes.stream().flatMap(empresa8 -> empresa8.puestos().stream()).toList(),
                resumenes.stream().flatMap(empresa9 -> empresa9.pendientes().stream()).toList(),
                resumenes.stream().flatMap(empresa10 -> empresa10.sugerencias().stream()).toList(),
                resumenes.stream().mapToInt(empresa11 -> empresa11.estadisticas().fotosAnalizadas()).sum());
        return new InformeEmpresa(empresa2, LocalDateTime.now(reloj), List.copyOf(resumenes), general, faltan, riesgos,
                totales);
    }

    private static Estadisticas estadisticas(List<VisitaResumen> vs, List<PuestoResumen> ps, List<PendienteResumen> pa,
            List<SugerenciaResumen> sg, int fotos) {
        Map<String, Integer> colores = new TreeMap<>(), pendientes = new TreeMap<>(), areas = new TreeMap<>(),
                cargos = new TreeMap<>(), rojos = new TreeMap<>();
        Map<String, Map<String, Integer>> sugerencias = new TreeMap<>();
        for (var empresa2 : ps) {
            sumar(areas, texto(empresa2.area(), "Sin área"));
            sumar(cargos, texto(empresa2.tipoCargo(), "Sin cargo"));
            if (empresa2.riesgo() != null) {
                sumar(colores, empresa2.riesgo().color());
                if (empresa2.riesgo().color().equals("ROJO")) sumar(rojos, texto(empresa2.area(), "Sin área"));
            }
        }
        pa.forEach(empresa3 -> sumar(pendientes, empresa3.estado()));
        for (var sugerencia : sg)
            sumar(sugerencias.computeIfAbsent(sugerencia.tipo(), k -> new TreeMap<>()), sugerencia.estado());
        int maximo = rojos.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        var mas = rojos.entrySet().stream().filter(parametros -> parametros.getValue() == maximo).map(Map.Entry::getKey)
                .toList();
        Map<String, Map<String, Integer>> tipos = new TreeMap<>();
        sugerencias.forEach((k, parametros2) -> tipos.put(k, Map.copyOf(parametros2)));
        return new Estadisticas(vs.size(), ps.size(),
                (int) ps.stream().filter(empresa4 -> empresa4.puntaje() != null).count(), fotos, Map.copyOf(colores),
                Map.copyOf(pendientes), Map.copyOf(tipos), Map.copyOf(areas), Map.copyOf(cargos), mas);
    }

    private static String texto(String valor, String vacio) {
        return valor == null || valor.isBlank() ? vacio : valor;
    }

    private static void sumar(Map<String, Integer> mapa, String clave) {
        mapa.merge(clave, 1, Integer::sum);
    }

    private static <T> List<T> deSede(List<PorSede<T>> filas, int sede) {
        return filas.stream().filter(elemento -> elemento.sede() == sede).map(PorSede::dato).toList();
    }

    private static Calificacion promediar(List<Calificacion> valores, List<Riesgo> riesgos) {
        if (valores.isEmpty()) return null;
        BigDecimal suma = valores.stream().map(Calificacion::promedio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return calificar(suma.divide(BigDecimal.valueOf(valores.size()), 12, RoundingMode.HALF_UP).stripTrailingZeros(),
                riesgos);
    }

    private static Calificacion calificar(BigDecimal promedio, List<Riesgo> riesgos) {
        int redondeado = promedio.setScale(0, REDONDEO_COLOR).intValueExact();
        return new Calificacion(promedio, redondeado, riesgo(redondeado, riesgos));
    }

    private static Riesgo riesgo(int puntaje, List<Riesgo> riesgos) {
        return riesgos.stream().filter(empresa2 -> puntaje >= empresa2.minimo() && puntaje <= empresa2.maximo())
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "No está disponible la clasificación de riesgo. Revisa la configuración antes" +
                                " de generar el informe."));
    }
}
