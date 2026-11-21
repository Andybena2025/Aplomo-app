package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.PracticaEvaluacion;
import co.aplomo.servicio.Visitas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Práctica permanente con HTTP real y datos revertidos solo en la base de pruebas.
 */
class HttpPracticaEvaluacionTest extends HttpPrueba {
    int cantidad() throws Exception {
        return Sql.uno(con, "SELECT COUNT(*) FROM empresa WHERE nit=?", fila -> fila.getInt(1), PracticaEvaluacion.NIT);
    }

    HttpResponse<String> abrir(String tipo) throws Exception {
        return post("/demo/evaluacion", Map.of("csrf", csrf(get("/demo/evaluacion")), "tipo", tipo));
    }

    @Test
    void inicioSiempreOfrecePracticaYSuConsultaNoCreaDatos() throws Exception {
        entrar(evaluadorTrabajo().id());
        int antes = cantidad();
        assertTrue(get("/").body().contains("Practicar una evaluación"));
        String texto = get("/demo/evaluacion").body();
        assertTrue(texto.contains("Probar Planta"));
        assertTrue(texto.contains("Probar Oficina"));
        assertTrue(texto.contains("no cuentan en tus resultados"));
        assertEquals(antes, cantidad());
    }

    @Test
    void preparaVisitaYAmbosPuestosYAbreLaEvaluacionReal() throws Exception {
        entrar(evaluadorTrabajo().id());
        var respuesta = abrir("PLANTA");
        assertEquals(303, respuesta.statusCode());
        String ruta = respuesta.headers().firstValue("Location").orElseThrow();
        assertTrue(ruta.matches("/evaluaciones/nueva\\?visita=\\d+&puesto=\\d+"));
        String texto = get(ruta).body();
        assertTrue(texto.contains("MODO PRÁCTICA · No cuenta en tus resultados"));
        assertTrue(texto.contains("data-conversacion=\"si\""));
        assertTrue(texto.contains("data-modo>Planta"));
        assertEquals(1, cantidad());
        assertEquals(7, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE id_cuenta=? AND fecha_hora=? AND detalle" + " LIKE ?",
                fila -> fila.getInt(1), evaluadorTrabajo().id(), java.time.LocalDateTime.now(reloj), "%práctica%"));
        assertEquals(List.of("ESCRITORIO", "MESA"), Sql.listar(con,
                "SELECT p.tipo FROM puesto_trabajo p JOIN salon l USING(id_salon) JOIN sede s" +
                        " USING(id_sede) JOIN empresa e USING(id_empresa) WHERE e.nit=? ORDER BY p.nu" + "mero",
                fila2 -> fila2.getString(1), PracticaEvaluacion.NIT));
    }

    @Test
    void seRepiteTrasEvaluarSinDuplicarVisitaNiBorrarElIntento() throws Exception {
        var practica = new Visitas(tx, reloj).practica();
        var evaluacion = practica.abrir(evaluadorTrabajo(), "OFICINA");
        new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new co.aplomo.dao.BitacoraDao()).guardar(cuenta(admin),
                evaluacion.visita(),
                new Evaluaciones.Datos(evaluacion.puesto(), "Práctica", "MAS_INADECUADA", "", 180, false, false, false,
                        false, false, false, "", true, true), List.of());
        entrar(evaluadorTrabajo().id());
        var respuesta = abrir("OFICINA");
        assertEquals("/evaluaciones/nueva?visita=" + evaluacion.visita() + "&puesto=" + evaluacion.puesto(),
                respuesta.headers().firstValue("Location").orElseThrow());
        assertTrue(get("/").body().contains("Practicar una evaluación"));
        assertEquals(1,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion WHERE id_visita=?", fila -> fila.getInt(1),
                        evaluacion.visita()));
    }

    @Test
    void visitaFinalizadaGeneraOtraSinAlterarLaAnterior() throws Exception {
        var practica = new Visitas(tx, reloj).practica();
        var evaluacion = practica.abrir(evaluadorTrabajo(), "PLANTA");
        Sql.actualizar(con,
                "UPDATE visita SET estado='FINALIZADA',hora_real_fin=hora_real_inicio WHERE i" + "d_visita=?",
                evaluacion.visita());
        Sql.actualizar(con, "UPDATE reserva SET estado='FINALIZADA' WHERE id_reserva=(SELECT id_reserva F" +
                "ROM visita WHERE id_visita=?)", evaluacion.visita());
        var evaluacion2 = practica.abrir(evaluadorTrabajo(), "PLANTA");
        assertNotEquals(evaluacion.visita(), evaluacion2.visita());
        assertEquals(evaluacion.puesto(), evaluacion2.puesto());
        assertEquals("FINALIZADA",
                Sql.<String>uno(con, "SELECT estado FROM visita WHERE id_visita=?", fila -> fila.getString(1),
                        evaluacion.visita()));
        assertEquals("EN_CURSO",
                Sql.<String>uno(con, "SELECT estado FROM visita WHERE id_visita=?", fila2 -> fila2.getString(1),
                        evaluacion2.visita()));
    }

    @Test
    void usuarioNoVeElAccesoNiPuedeCrearPracticaConDireccionManual() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        entrar(usuario);
        var inicio = get("/");
        assertFalse(inicio.body().contains("Practicar una evaluación"));
        assertEquals(403, get("/demo/evaluacion").statusCode());
        assertEquals(403, post("/demo/evaluacion", Map.of("csrf", csrf(inicio), "tipo", "PLANTA")).statusCode());
        assertEquals(0, cantidad());
        assertThrows(SecurityException.class, () -> new Visitas(tx, reloj).practica().abrir(cuenta(usuario), "PLANTA"));
    }

    @Test
    void csrfYTipoInvalidoNoCreanDatos() throws Exception {
        entrar(evaluadorTrabajo().id());
        assertEquals(403, post("/demo/evaluacion", Map.of("csrf", "incorrecto", "tipo", "PLANTA")).statusCode());
        assertEquals(400, abrir("OTRO").statusCode());
        assertEquals(0, cantidad());
    }

    @Test
    void banderaDeDemoOcultaYBloqueaElAcceso() throws Exception {
        entrar(evaluadorTrabajo().id());
        String token = csrf(get("/demo/evaluacion")), previo = System.getProperty(GaleriaDemo.BANDERA);
        try {
            System.setProperty(GaleriaDemo.BANDERA, "false");
            assertFalse(get("/").body().contains("Practicar una evaluación"));
            assertEquals(404, get("/demo/evaluacion").statusCode());
            assertEquals(404, post("/demo/evaluacion", Map.of("csrf", token, "tipo", "PLANTA")).statusCode());
            assertEquals(0, cantidad());
        } finally {
            if (previo == null) System.clearProperty(GaleriaDemo.BANDERA);
            else System.setProperty(GaleriaDemo.BANDERA, previo);
        }
    }

    @Test
    void noReactivaUnaEmpresaDesactivadaNiTocaUnaEmpresaAjena() throws Exception {
        crearSalon();
        int ajena = empresa;
        var practica = new Visitas(tx, reloj).practica();
        practica.abrir(evaluadorTrabajo(), "PLANTA");
        Sql.actualizar(con, "UPDATE empresa SET activo=FALSE WHERE nit=?", PracticaEvaluacion.NIT);
        assertThrows(IllegalArgumentException.class, () -> practica.abrir(evaluadorTrabajo(), "PLANTA"));
        assertFalse(Sql.<Boolean>uno(con, "SELECT activo FROM empresa WHERE nit=?", fila -> fila.getBoolean(1),
                PracticaEvaluacion.NIT));
        assertTrue(Sql.<Boolean>uno(con, "SELECT activo FROM empresa WHERE id_empresa=?", fila2 -> fila2.getBoolean(1),
                ajena));
    }
}
