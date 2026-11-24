package co.aplomo.web;

import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Sugerencias;
import co.aplomo.servicio.Visitas;
import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.modelo.Puesto;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.soporte.ReglasSugerenciasPrueba;
import co.aplomo.db.Sql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-023a/b, RF-002, RNF-004/007: formulario, CSRF, origen protegido y reserva de Reevaluación.
 */
class HttpSugerenciasTest extends HttpPrueba {
    Evaluaciones evs;
    Sugerencias servicio;
    int visita, evaluacion, id;

    @BeforeEach
    void contexto() throws Exception {
        ReglasSugerenciasPrueba.cargar(con);
        evs = new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new BitacoraDao());
        servicio = evs.sugerencias();
        var salon = crearSalon();
        int puesto = new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Ensamble", "MESA", "Centro", null, true)).id();
        var reserva2 = new Reservas(tx, reloj);
        int reserva = reserva2.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        reserva2.aceptar(evaluadorReserva(reserva), reserva);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(reserva), reserva);
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(12, 12, 1), "png", elemento);
        evaluacion = evs.guardar(cuenta(admin), visita,
                new Evaluaciones.Datos(puesto, "Trabajo", "MAS_INADECUADA", "Postura mantenida", 120, true, true, true,
                        false, false, true, "", true, false),
                List.of(new Fotos.Entrada(elemento.toByteArray(), "PERFIL", "Prueba")),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(9).entrada())));
        id = servicio.listar(cuenta(admin), evaluacion).get(0).sugerencia().id();
        entrar(admin);
    }

    @Test
    void crearEditarYOrigenProtegidoConHtmlEscapado() throws Exception {
        var texto = new HashMap<>(
                Map.of("csrf", csrf(get("/sugerencias/" + id + "/editar")), "titulo", "<script>inocuo</script>",
                        "descripcion", "Texto\nconservado", "tipo", "OTRA", "prioridad", "BAJA", "fecha", "2026-11-25",
                        "origen", "ADMINISTRADOR", "id_regla", "999999"));
        assertEquals(303, post("/sugerencias/" + id, texto).statusCode());
        var sugerencia = servicio.consultar(cuenta(admin), id).sugerencia();
        assertEquals("SISTEMA", sugerencia.origen());
        assertNotEquals(999999, sugerencia.regla());
        var respuesta = get("/sugerencias/" + id);
        assertTrue(respuesta.body().contains("&lt;script&gt;inocuo&lt;/script&gt;"));
        assertFalse(respuesta.body().contains("<script>inocuo"));
        assertTrue(respuesta.body().contains("25/11/2026"));
        assertFalse(respuesta.body().contains("/fotos/"));
        texto.put("csrf", csrf(get("/sugerencias/nueva?evaluacion=" + evaluacion)));
        assertEquals(303, post("/sugerencias?evaluacion=" + evaluacion, texto).statusCode());
    }

    @Test
    void erroresConservanFormularioYUltimaSugerenciaNoSeQuita() throws Exception {
        String token = csrf(get("/sugerencias/" + id + "/editar"));
        var respuesta = post("/sugerencias/" + id,
                Map.of("csrf", token, "titulo", "Mi mejora", "descripcion", "No perder este texto", "tipo", "POSTURA",
                        "prioridad", "ALTA", "fecha", "0001-01-01"));
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("No perder este texto"));
        assertTrue(respuesta.body().contains("value=\"ALTA\" selected"));
        assertTrue(respuesta.body().contains("0001-01-01"));
        assertFalse(respuesta.body().contains("SQLException"));
        for (var sugerencia : servicio.listar(cuenta(admin), evaluacion))
            if (sugerencia.sugerencia().id() != id) servicio.quitar(cuenta(admin), sugerencia.sugerencia().id());
        assertEquals(400, post("/sugerencias/" + id + "/quitar", Map.of("csrf", token)).statusCode());
        assertTrue(servicio.consultar(cuenta(admin), id).sugerencia().activo());
    }

    @Test
    void estadoCsrfYMetodosProtegidos() throws Exception {
        assertEquals(405, get("/sugerencias/" + id + "/quitar").statusCode());
        assertEquals(405, get("/sugerencias/" + id + "/estado").statusCode());
        assertEquals(403,
                post("/sugerencias/" + id + "/estado", Map.of("csrf", "malo", "estado", "HECHA")).statusCode());
        String texto = csrf(get("/sugerencias/" + id));
        assertEquals(303,
                post("/sugerencias/" + id + "/estado", Map.of("csrf", texto, "estado", "EN_PROCESO")).statusCode());
        assertEquals("EN_PROCESO", servicio.consultar(cuenta(admin), id).sugerencia().estado());
    }

    @Test
    void hechasOfreceReservaPreparadaSinCrearlaAutomaticamente() throws Exception {
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM reserva", fila -> fila.getInt(1));
        for (var sugerencia : servicio.listar(cuenta(admin), evaluacion))
            servicio.estado(cuenta(admin), sugerencia.sugerencia().id(), "HECHA");
        var respuesta = get("/sugerencias/" + id);
        assertFalse(respuesta.body().contains("Registrar reevaluación"));
        var sugerencia2 = servicio.contexto(cuenta(admin), evaluacion);
        post("/logout", Map.of("csrf", csrf(get("/"))));
        entrar(evaluadorTrabajo().id());
        var nueva = get("/reservas/nueva?empresa=" + sugerencia2.empresa() + "&sede=" + sugerencia2.sede() + "&salon=" +
                sugerencia2.salon() + "&categoria=REEVALUACION");
        assertEquals(200, nueva.statusCode());
        assertTrue(nueva.body().contains("value=\"REEVALUACION\" selected"));
        assertEquals(antes, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM reserva", r1 -> r1.getInt(1)));
    }

    @Test
    void usuarioConsultaSinFotosYNoModificaPorDireccionDirecta() throws Exception {
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertEquals(403, get("/sugerencias/" + id).statusCode());
        new Visitas(tx, reloj).finalizar(cuenta(admin), visita, "");
        var respuesta = get("/sugerencias/" + id);
        assertEquals(200, respuesta.statusCode());
        assertFalse(respuesta.body().contains("Editar sugerencia"));
        assertFalse(respuesta.body().contains("Quitar sugerencia"));
        assertFalse(respuesta.body().contains("Guardar estado"));
        assertFalse(respuesta.body().contains("/fotos/"));
        assertEquals(403, get("/sugerencias/" + id + "/editar").statusCode());
        assertEquals(403, get("/sugerencias/nueva?evaluacion=" + evaluacion).statusCode());
        String texto = csrf(get("/"));
        assertEquals(403,
                post("/sugerencias/" + id + "/estado", Map.of("csrf", texto, "estado", "HECHA")).statusCode());
        assertEquals(403, post("/sugerencias/" + id + "/quitar", Map.of("csrf", texto)).statusCode());
    }

    @Test
    void otraEmpresaYQuitadasNoSonVisiblesParaUsuario() throws Exception {
        new Visitas(tx, reloj).finalizar(cuenta(admin), visita, "");
        servicio.quitar(cuenta(admin), id);
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(usuario);
        assertEquals(403, get("/sugerencias/" + id).statusCode());
        int otra = Sql.insertar(con, "INSERT INTO empresa (id_administrador,nombre,nit) VALUES (?,?,?)", admin,
                "Otra empresa", UUID.randomUUID().toString().substring(0, 20));
        int cantidad = insertar("USUARIO", otra, cuenta(admin).hash(), false);
        cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        entrar(cantidad);
        assertEquals(403, get("/sugerencias?evaluacion=" + evaluacion).statusCode());
    }

    @Test
    void sinReglasAplicablesConservaAnalisisYPideSugerenciaDentroDelPlan() throws Exception {
        var evaluacion2 = new EvaluacionDao().fotos(con, evaluacion).get(0);
        int nueva = evs.guardar(cuenta(admin), visita,
                new Evaluaciones.Datos(servicio.contexto(cuenta(admin), evaluacion).puesto(), "Solo carga",
                        "MAS_INADECUADA", "Trabajo", 60, false, false, false, false, false, false, "", true, false),
                List.of(new Fotos.Entrada(new Fotos(carpetaFotos).leer(evaluacion2.nombre()), "PERFIL", "Prueba")),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(8).entrada())));
        assertTrue(servicio.listar(cuenta(admin), nueva).isEmpty());
        var respuesta = get("/evaluaciones/" + nueva);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("data-resultado40-json"));
        assertTrue(respuesta.body().contains("\"sinSugerencias\":true"));
        assertTrue(respuesta.body().contains("\"evaluacion\":" + nueva));
        assertEquals(1, new co.aplomo.dao.AnalisisDao().listar(con, nueva).size());
    }

    @Test
    void vistaPreviaIncluyeReglaDentroDelCalculoSinInsertar() throws Exception {
        int antes = servicio.listar(cuenta(admin), evaluacion).size();
        var campos = CasosRulaPrueba.campos(CasosRulaPrueba.todos().get(9).entrada());
        campos.put("csrf", csrf(get("/")));
        campos.put("visita", "" + visita);
        campos.put("pies", "si");
        var respuesta = post("/evaluaciones/calcular", campos);
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("reposapiés o bajar la silla"));
        assertTrue(respuesta.body().contains("Sugerencias · aparte del puntaje"));
        assertEquals(antes, servicio.listar(cuenta(admin), evaluacion).size());
        assertTrue(get("/static/js/sugerencias.js").body().contains("Tus campos"));
        assertTrue(get("/pendientes").body().contains("7 · Rojo · Nivel 4"));
        assertTrue(get("/visitas/" + visita).body().contains("7 · Rojo · Nivel 4"));
    }
}
