package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.dao.CalificacionesDao.PorSede;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.*;
import co.aplomo.modelo.InformeEmpresa.*;
import co.aplomo.seguridad.*;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-025/026: indicadores del equipo y composición del informe, sin recalcular RULA ni modificar datos. */
public final class Calificaciones {
    /** (Propuesta), respuesta de Andy del 11 nov 2026: mínimo de sedes calificadas para una general. */
    public static final int MINIMO_SEDES_GENERAL = 2;
    /** (Propuesta) Clasificar el promedio real al entero más cercano, mitades hacia arriba. */
    public static final RoundingMode REDONDEO_COLOR = RoundingMode.HALF_UP;
    private final Transacciones tx;
    private final Clock reloj;
    private final CalificacionesDao dao = new CalificacionesDao();

    /** Comparte la conexión y el reloj de la aplicación y de sus pruebas reversibles. */
    public Calificaciones(Transacciones tx, Clock reloj) { this.tx=tx; this.reloj=reloj; }

    /** Ofrece solo empresas autorizadas; no elige una empresa arbitrariamente. */
    public List<Empresa> empresas(Cuenta actor)throws SQLException {
        return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);return new EmpresaDao().buscarTodos(c,0,AccesoDatos.empresaPermitida(a));});
    }

    /** Usuario consulta únicamente indicadores de su propia empresa, siempre de visitas Finalizadas. */
    public InformeEmpresa consultar(Cuenta actor,int empresa)throws SQLException {
        return tx.ejecutar(c->cargar(c,AccesoDatos.vigente(c,actor),empresa));
    }

    /** Solo el Administrador genera una vista imprimible y solo si existe una visita Finalizada. */
    public InformeEmpresa informe(Cuenta actor,int empresa)throws SQLException {
        return tx.ejecutar(c->{var a=AccesoDatos.vigente(c,actor);Permisos.administrador(a);
            var informe=cargar(c,a,empresa);
            if(informe.sedes().stream().allMatch(s->s.visitas().isEmpty()))
                throw new IllegalArgumentException("Esta empresa aún no tiene visitas Finalizadas. Finaliza al menos una visita para generar el informe.");
            return informe;
        });
    }

    private InformeEmpresa cargar(Connection c,Cuenta actor,int empresa)throws SQLException {
        if(empresa<1)throw new IllegalArgumentException("Elige una empresa para consultar sus calificaciones.");
        int permitida=AccesoDatos.empresaPermitida(actor);
        Empresa e=new EmpresaDao().buscar(c,empresa,permitida,false);
        if(e==null)throw new SecurityException("Acceso denegado. La empresa no está disponible para tu cuenta.");
        var sedes=new SedeDao().buscarTodos(c,empresa,permitida);
        var riesgos=dao.riesgos(c);
        var visitas=dao.visitas(c,empresa);var puestos=dao.puestos(c,empresa);
        var pendientes=dao.pendientes(c,empresa);var sugerencias=dao.sugerencias(c,empresa);
        var fotos=dao.fotosAnalizadas(c,empresa);
        List<SedeResumen> resumenes=new ArrayList<>();
        for(var sede:sedes){
            var vs=deSede(visitas,sede.id()).stream().map(v->new VisitaResumen(v.id(),v.codigo(),v.inicio(),v.fin(),v.notas(),
                    v.calificacion()==null?null:calificar(v.calificacion().promedio(),riesgos))).toList();
            var ps=deSede(puestos,sede.id()).stream().map(p->new PuestoResumen(p.id(),p.evaluacion(),p.visita(),p.numero(),p.salon(),
                    p.area(),p.cargo(),p.tipoCargo(),p.descripcion(),p.postura(),p.motivo(),p.condiciones(),p.fecha(),p.puntaje(),
                    p.puntaje()==null?null:riesgo(p.puntaje(),riesgos))).toList();
            var pa=deSede(pendientes,sede.id()).stream().map(p->new PendienteResumen(p.id(),p.evaluacion(),p.numero(),p.salon(),
                    p.descripcion(),p.prioridad(),p.estado(),p.fecha(),p.limite(),p.puntaje(),riesgo(p.puntaje(),riesgos))).toList();
            var sg=deSede(sugerencias,sede.id());
            Calificacion nota=promediar(vs.stream().map(VisitaResumen::calificacion).filter(Objects::nonNull).toList(),riesgos);
            resumenes.add(new SedeResumen(sede,nota,vs,ps,pa,sg,estadisticas(vs,ps,pa,sg,fotos.getOrDefault(sede.id(),0))));
        }
        var calificadas=resumenes.stream().map(SedeResumen::calificacion).filter(Objects::nonNull).toList();
        Calificacion general=calificadas.size()>=MINIMO_SEDES_GENERAL?promediar(calificadas,riesgos):null;
        var faltan=resumenes.stream().filter(s->s.calificacion()==null).map(s->s.sede().nombre()).toList();
        var totales=estadisticas(resumenes.stream().flatMap(s->s.visitas().stream()).toList(),
                resumenes.stream().flatMap(s->s.puestos().stream()).toList(),
                resumenes.stream().flatMap(s->s.pendientes().stream()).toList(),
                resumenes.stream().flatMap(s->s.sugerencias().stream()).toList(),
                resumenes.stream().mapToInt(s->s.estadisticas().fotosAnalizadas()).sum());
        return new InformeEmpresa(e,LocalDateTime.now(reloj),List.copyOf(resumenes),general,faltan,riesgos,totales);
    }

    private static Estadisticas estadisticas(List<VisitaResumen> vs,List<PuestoResumen> ps,List<PendienteResumen> pa,
                                             List<SugerenciaResumen> sg,int fotos){
        Map<String,Integer> colores=new TreeMap<>(),pendientes=new TreeMap<>(),areas=new TreeMap<>(),cargos=new TreeMap<>(),rojos=new TreeMap<>();
        Map<String,Map<String,Integer>> sugerencias=new TreeMap<>();
        for(var p:ps){sumar(areas,texto(p.area(),"Sin área"));sumar(cargos,texto(p.tipoCargo(),"Sin cargo"));
            if(p.riesgo()!=null){sumar(colores,p.riesgo().color());if(p.riesgo().color().equals("ROJO"))sumar(rojos,texto(p.area(),"Sin área"));}}
        pa.forEach(p->sumar(pendientes,p.estado()));
        for(var s:sg)sumar(sugerencias.computeIfAbsent(s.tipo(),k->new TreeMap<>()),s.estado());
        int maximo=rojos.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        var mas=rojos.entrySet().stream().filter(x->x.getValue()==maximo).map(Map.Entry::getKey).toList();
        Map<String,Map<String,Integer>> tipos=new TreeMap<>();sugerencias.forEach((k,v)->tipos.put(k,Map.copyOf(v)));
        return new Estadisticas(vs.size(),ps.size(),(int)ps.stream().filter(p->p.puntaje()!=null).count(),fotos,
                Map.copyOf(colores),Map.copyOf(pendientes),Map.copyOf(tipos),Map.copyOf(areas),Map.copyOf(cargos),mas);
    }
    private static String texto(String valor,String vacio){return valor==null||valor.isBlank()?vacio:valor;}
    private static void sumar(Map<String,Integer> mapa,String clave){mapa.merge(clave,1,Integer::sum);}
    private static <T> List<T> deSede(List<PorSede<T>> filas,int sede){return filas.stream().filter(f->f.sede()==sede).map(PorSede::dato).toList();}
    private static Calificacion promediar(List<Calificacion> valores,List<Riesgo> riesgos){
        if(valores.isEmpty())return null;
        BigDecimal suma=valores.stream().map(Calificacion::promedio).reduce(BigDecimal.ZERO,BigDecimal::add);
        return calificar(suma.divide(BigDecimal.valueOf(valores.size()),12,RoundingMode.HALF_UP).stripTrailingZeros(),riesgos);
    }
    private static Calificacion calificar(BigDecimal promedio,List<Riesgo> riesgos){
        int redondeado=promedio.setScale(0,REDONDEO_COLOR).intValueExact();
        return new Calificacion(promedio,redondeado,riesgo(redondeado,riesgos));
    }
    private static Riesgo riesgo(int puntaje,List<Riesgo> riesgos){
        return riesgos.stream().filter(n->puntaje>=n.minimo()&&puntaje<=n.maximo()).findFirst()
                .orElseThrow(()->new IllegalArgumentException("No está disponible la clasificación de riesgo. Revisa la configuración antes de generar el informe."));
    }
}
