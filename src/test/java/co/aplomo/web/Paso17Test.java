package co.aplomo.web;
import co.aplomo.dao.*;import co.aplomo.db.*;import co.aplomo.modelo.*;import co.aplomo.servicio.*;import co.aplomo.soporte.*;import org.junit.jupiter.api.Test;import java.util.*;import java.nio.file.*;import static org.junit.jupiter.api.Assertions.*;
/** Paso 17: práctica separada, pantallas y permisos con datos reversibles. */
class Paso17Test extends HttpPrueba {
 @Test void practicaNoCambiaLasOnceListasNiSusCatalogosNiElSemaforo()throws Exception{
  var evs=new Evaluaciones(tx,reloj,new Fotos(carpetaFotos),new BitacoraDao());var consultas=evs.consultas();var a=cuenta(admin);var antes=new EnumMap<SeccionConsulta,Integer>(SeccionConsulta.class);for(var seccion:SeccionConsulta.values())antes.put(seccion,consultas.listar(a,seccion,Map.of()).filas().size());long[] kpi=evs.resumen(a);
  var destino=new Visitas(tx,reloj).practica().abrir(a,"PLANTA");
  byte[] png=Files.readAllBytes(Path.of("src/main/resources/static/img/demo/07_bodega_levantando_caja_perfil.jpg"));
  evs.guardar(a,destino.visita(),new Evaluaciones.Datos(destino.puesto(),"Práctica","MAS_INADECUADA","La postura más incómoda.",180,false,false,false,false,false,false,"",true,false),List.of(new Fotos.Entrada(png,"PERFIL","Foto ficticia")),List.of(new Analisis.Solicitud(0,CasosRulaPrueba.todos().get(7).entrada())));
  assertArrayEquals(kpi,evs.resumen(a));int practica=Sql.uno(con,"SELECT id_empresa FROM empresa WHERE nit=?",x->x.getInt(1),PracticaEvaluacion.NIT);
  for(var seccion:SeccionConsulta.values()){var p=consultas.listar(a,seccion,Map.of());assertEquals(antes.get(seccion),p.filas().size(),seccion.name());assertTrue(p.catalogos().values().stream().flatMap(List::stream).noneMatch(o->o.empresa()==practica));}
  assertTrue(new Reservas(tx,reloj).listar(a).isEmpty());assertTrue(new Visitas(tx,reloj).listar(a).isEmpty());assertTrue(new Visitas(tx,reloj).paraEvaluar(a).isEmpty());assertTrue(new Reservas(tx,reloj).catalogo(a).empresas().stream().noneMatch(e->e.id()==practica));assertThrows(SecurityException.class,()->evs.calificaciones().consultar(a,practica));
 }
 @Test void inicioTieneUnSoloBloqueDeVisitasYContinuarPracticaSeparado()throws Exception{
  entrar(admin);String h=get("/").body();assertFalse(h.contains("Evaluar ahora"));assertFalse(h.contains("Continuar práctica"));var destino=new Visitas(tx,reloj).practica().abrir(cuenta(admin),"OFICINA");h=get("/").body();assertTrue(h.contains("Continuar práctica"));assertTrue(h.contains("/visitas/"+destino.visita()));assertEquals(1,h.split("id=\"visitas-en-curso\"",-1).length-1);assertFalse(h.contains("Todos los puestos están evaluados"));
 }
 @Test void pestañasYOrdenDentroDeMasFiltros()throws Exception{
  entrar(admin);String h=get("/empresas").body();assertFalse(h.contains("Ver otras secciones"));assertTrue(h.contains("secciones-tabs"));assertTrue(h.contains("href=\"/empresas\" class=\"on\" aria-current=\"page\""));int inicio=h.indexOf("Más filtros"),orden=h.indexOf("Ordenar por",inicio),fin=h.indexOf("</details>",inicio);assertTrue(inicio>0&&orden>inicio&&orden<fin);
 }
 @Test void visitaOcultaNotasYFinVaciosYOfreceEvaluar()throws Exception{
  var salon=crearSalon();new Empresas(tx).guardar(cuenta(admin),new Puesto(0,salon.id(),null,null,1,"Puesto","MESA","Aquí",null,true));var reservas=new Reservas(tx,reloj);int reserva=reservas.guardar(cuenta(admin),0,solicitud(salon));reservas.aceptar(cuenta(admin),reserva);int visita=new Visitas(tx,reloj).iniciar(cuenta(admin),reserva);entrar(admin);String h=get("/visitas/"+visita).body();assertFalse(h.contains("<dt>Fin real</dt>"));assertFalse(h.contains("<dt>Notas</dt>"));assertTrue(h.contains(">Ver reserva</a>"));assertTrue(h.contains("class=\"btn p\">Evaluar</span>"));
 }
 @Test void faviconEsPublicoYErroresTienenSalidaSinDetallesTecnicos()throws Exception{
  for(String ruta:List.of("/favicon.ico","/favicon.svg")){var f=get(ruta);assertEquals(200,f.statusCode());assertTrue(f.headers().firstValue("Content-Type").orElse("").startsWith("image/svg+xml"));assertTrue(f.body().contains("plom")||f.body().contains("<svg"));}
  assertTrue(get("/login").body().contains("Iniciar sesión · ALINEO."));entrar(admin);var f=get("/no-existe");assertEquals(404,f.statusCode());assertTrue(f.body().contains("Volver al inicio"));assertTrue(Html.error("No pudimos abrir esta página","Inténtalo otra vez.").contains("Volver al inicio"));
 }
 @Test void cuelloManualMenosSeisConservaCuatroPuntos() {assertEquals(4,co.aplomo.rula.PuntuadorSegmentos.cuello(-6,false,false));}
}
