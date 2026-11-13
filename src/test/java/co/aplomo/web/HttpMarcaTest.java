package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.soporte.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Identidad visual sin cambiar navegación, permisos ni resultados del motor. */
class HttpMarcaTest extends HttpPrueba {
    @Test void paginasDeProductoConservanPieYNoMuestranEtiquetasDelProyecto() throws Exception {
        var login=get("/login");assertTrue(login.body().contains(">Bienvenido</span>"));
        assertFalse(login.body().contains("class=\"pie\""));
        var ingreso=entrar(admin);assertEquals("/",ingreso.headers().firstValue("Location").orElseThrow());
        for(String ruta:List.of("/","/empresas","/sedes","/salones","/areas","/cargos","/puestos","/reservas","/visitas","/evaluaciones","/pendientes","/sugerencias","/usuarios","/calificaciones","/empresas/nueva","/reservas/nueva","/usuarios/nuevo","/cuenta/contrasena")){
            var r=get(ruta);assertEquals(200,r.statusCode(),ruta);String h=r.body();
            assertTrue(h.contains("© 2026 APLOMO"),ruta);assertTrue(h.contains("Ergonomía con precisión · Método RULA"),ruta);
            assertFalse(h.toLowerCase(Locale.ROOT).contains("proyecto nuclear"),ruta);
            assertFalse(h.toLowerCase(Locale.ROOT).contains("software real"),ruta);
            assertFalse(h.contains("v1.0"),ruta);
        }
        assertFalse(login.body().toLowerCase(Locale.ROOT).contains("proyecto nuclear"));
    }
    @Test void inicioOmiteAtrasSinQuitarElBotonDelRestoNiCrearBienvenida() throws Exception {
        entrar(admin);var inicio=get("/");assertFalse(inicio.body().contains("aria-label=\"Atrás\""));
        assertTrue(inicio.body().contains("Hola, "));assertTrue(inicio.body().contains("© 2026 APLOMO"));
        assertTrue(get("/usuarios").body().contains("aria-label=\"Atrás\""));
        cliente=java.net.http.HttpClient.newBuilder().cookieHandler(new java.net.CookieManager(null,java.net.CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);assertFalse(get("/").body().contains("aria-label=\"Atrás\""));
        assertFalse(get("/").body().contains("Semáforo de puestos"));
    }
    @Test void semaforoLeeColoresRealesYSeActualizaConLosDatos() throws Exception {
        DatosDemoPrueba.cargar(con);entrar(admin);String h=get("/").body();
        conteo(h,"VERDE",2);conteo(h,"AMARILLO",1);conteo(h,"ROJO",1);assertTrue(h.contains("class=\"barra\""));
        int puesto=Sql.uno(con,"SELECT e.id_puesto FROM evaluacion e JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) WHERE r.codigo=?",r->r.getInt(1),"DEMO-FIN-02");
        Sql.actualizar(con,"UPDATE puesto_trabajo SET ultimo_puntaje=? WHERE id_puesto=?",3,puesto);
        h=get("/").body();conteo(h,"VERDE",1);conteo(h,"AMARILLO",2);conteo(h,"ROJO",1);
        assertTrue(h.indexOf("Semáforo de puestos")<h.indexOf("<h3>Visitas en curso</h3>"));
    }
    @Test void ceroPuestosNoMuestraUnaBarraProporcionalVacia() throws Exception {
        entrar(admin);String h=get("/").body();for(String color:List.of("VERDE","AMARILLO","ROJO"))conteo(h,color,0);
        assertFalse(h.contains("class=\"barra\""));
    }
    @Test void panelesSoloEnLosTresFormulariosNuevosConCamposYAccionesIntactos() throws Exception {
        entrar(admin);
        String[] rutas={"/empresas/nueva","/reservas/nueva","/usuarios/nuevo"};
        String[] lemas={"Cada puesto, medido con precisión.","Mide la jornada real.","Cada empresa, solo lo suyo."};
        for(int i=0;i<rutas.length;i++){
            String h=get(rutas[i]).body();assertTrue(h.contains("class=\"con-panel\""));assertTrue(h.contains("<aside class=\"panel-marca\""));
            assertTrue(h.contains(lemas[i]));assertTrue(h.contains("Consejo RULA"));assertTrue(h.contains("name=\"csrf\""));
        }
        assertFalse(get("/usuarios/"+usuario+"/editar").body().contains("class=\"panel-marca\""));
        assertFalse(get("/empresas/"+empresa+"/editar").body().contains("class=\"panel-marca\""));
    }
    @Test void vaciosConservanTextoBotonesYDecoracionesSonSoloPresentacion() throws Exception {
        entrar(admin);String h=get("/").body();assertTrue(h.contains("class=\"empty marca\""));assertTrue(h.contains("Iniciar desde una reserva"));
        assertTrue(h.contains("class=\"riel riel-izq\" aria-hidden=\"true\""));assertTrue(h.contains("viewBox=\"0 0 300 300\""));assertTrue(h.contains("viewBox=\"0 0 300 380\""));
        assertFalse(h.contains("class=\"fondo\""));
        assertTrue(get("/usuarios").body().contains("class=\"empty marca\" data-usuarios-vacios hidden"));
        assertTrue(get("/empresas?q=no-existe-qa-marca").body().contains("<p>No hay registros con estos filtros."));
    }
    private static void conteo(String html,String color,int cantidad){assertTrue(html.contains("data-color=\""+color+"\" data-cantidad=\""+cantidad+"\""),color+"="+cantidad);}
}
