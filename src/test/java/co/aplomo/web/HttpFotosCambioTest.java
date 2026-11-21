package co.aplomo.web;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.db.Sql;
import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.FotosEvaluacion;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpFotosCambioTest extends HttpPrueba {
    int evaluacion, foto, visita;
    String token;

    @BeforeEach
    void datos() throws Exception {
        var salon = crearSalon();
        var empresas = new Empresas(tx);
        int puesto = empresas.guardar(cuenta(admin),
                        new co.aplomo.modelo.Puesto(0, salon.id(), null, null, 1, "Práctica", "MESA", "Centro", null,
                                true))
                .id();
        var rs = new Reservas(tx, reloj);
        int reserva = rs.guardar(evaluadorTrabajo((solicitud(salon)).empresa()), 0, solicitud(salon));
        rs.aceptar(evaluadorReserva(reserva), reserva);
        visita = new Visitas(tx, reloj).iniciar(evaluadorReserva(reserva), reserva);
        evaluacion = new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new BitacoraDao()).guardar(cuenta(admin),
                visita,
                new Evaluaciones.Datos(puesto, "Prueba", "MAS_INADECUADA", "", 180, false, false, false, false, false,
                        false, "", true, false), List.of(new Fotos.Entrada(png(), "PERFIL", "Perfil original")));
        foto = new EvaluacionDao().fotos(con, evaluacion).get(0).id();
        entrar(admin);
        token = csrf(get("/evaluaciones/" + evaluacion + "/analizar"));
    }

    byte[] png() throws Exception {
        var elemento = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(40, 80, 1), "png", elemento);
        return elemento.toByteArray();
    }

    HttpResponse<String> cambiar(Integer anterior, byte[] bytes, String csrf) throws Exception {
        String limite = "CambioFotoPrueba";
        var elemento = new ByteArrayOutputStream();
        for (var parametros : Map.of("csrf", csrf, "vista", "PERFIL", "descripcion", "Perfil nuevo").entrySet())
            parte(elemento, limite, parametros.getKey(), parametros.getValue());
        if (anterior != null) parte(elemento, limite, "anterior", "" + anterior);
        if (bytes != null) {
            elemento.write(
                    ("--" + limite + "\r\nContent-Disposition: form-data; name=\"foto\"; filename=\"imagen.jpg\"\r" +
                            "\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            elemento.write(bytes);
            elemento.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        elemento.write(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return cliente.send(HttpRequest.newBuilder(URI.create(raiz + "/evaluaciones/" + evaluacion + "/fotos"))
                        .header("Content-Type", "multipart/form-data; boundary=" + limite)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(elemento.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    void parte(ByteArrayOutputStream elemento, String limite, String nombre, String valor) throws Exception {
        elemento.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n" + valor +
                "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void cambiarConservaArchivoOriginalYAnalisisYMarcaEnBitacora() throws Exception {
        var evs = new Evaluaciones(tx, reloj, new Fotos(carpetaFotos), new BitacoraDao());
        evs.analisis().guardar(cuenta(admin), evaluacion,
                List.of(new Analisis.Solicitud(foto, CasosRulaPrueba.todos().get(0).entrada())));
        var respuesta = cambiar(foto, png(), token);
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(2, new EvaluacionDao().fotos(con, evaluacion).size());
        assertTrue(new EvaluacionDao().foto(con, foto).descripcion().startsWith(FotosEvaluacion.REEMPLAZADA));
        assertEquals(200, get("/fotos/" + foto).statusCode());
        assertEquals(1, evs.analisis().listar(cuenta(admin), evaluacion).size());
        assertFalse(get("/evaluaciones/" + evaluacion + "/analizar").body().contains("data-foto-id=\"" + foto + "\""));
        assertEquals(1, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='fotografia' AND id_registro=? AND" + " detalle LIKE ?",
                fila -> fila.getInt(1), foto, "%Perfil original%"));
    }

    @Test
    void quitarSoloMarcaYNuncaBorra() throws Exception {
        assertEquals(200, cambiar(foto, null, token).statusCode());
        assertEquals(1, new EvaluacionDao().fotos(con, evaluacion).size());
        assertEquals(200, get("/fotos/" + foto).statusCode());
        assertEquals(200, cambiar(foto, null, token).statusCode());
    }

    @Test
    void pdfRenombradoNoReemplazaNiCreaArchivos() throws Exception {
        assertEquals(400, cambiar(foto, "%PDF-1.7".getBytes(), token).statusCode());
        assertEquals("Perfil original", new EvaluacionDao().foto(con, foto).descripcion());
        assertEquals(1, new EvaluacionDao().fotos(con, evaluacion).size());
    }

    @Test
    void sinCsrfOConFotoAjenaSeRechaza() throws Exception {
        assertEquals(403, cambiar(foto, png(), "incorrecto").statusCode());
        assertEquals(403, cambiar(Integer.MAX_VALUE, png(), token).statusCode());
        assertEquals(1, new EvaluacionDao().fotos(con, evaluacion).size());
    }

    @Test
    void usuarioNoPuedeCambiarFotosPorDireccionManual() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        post("/logout", Map.of("csrf", token));
        entrar(usuario);
        assertEquals(403, cambiar(foto, png(), csrf(get("/"))).statusCode());
        assertEquals("Perfil original", new EvaluacionDao().foto(con, foto).descripcion());
    }

    @Test
    void visitaFinalizadaConservaFotosSinCambios() throws Exception {
        Sql.actualizar(con,
                "UPDATE visita SET estado='FINALIZADA',hora_real_fin=hora_real_inicio WHERE i" + "d_visita=?", visita);
        assertEquals(400, cambiar(foto, png(), token).statusCode());
        assertEquals("Perfil original", new EvaluacionDao().foto(con, foto).descripcion());
    }

    @Test
    void textosExactosYControlesDeFotoEstanEnLaPagina() throws Exception {
        String texto = get("/evaluaciones/nueva?visita=" + visita).body();
        for (String texto2 : List.of("Hombro subido en el brazo que hace el esfuerzo",
                "Brazo que hace el esfuerzo separado del cuerpo", "Brazo que hace el esfuerzo apoyado",
                "Antebrazo del brazo que hace el esfuerzo cruza el cuerpo o sale hacia un lado",
                "Brazo que hace el esfuerzo:", "Cambiar foto", "Quitar foto", "data-seguir-zona",
                "data-siguiente disabled", "class=\"btn agregar-foto\""))
            assertTrue(texto.contains(texto2), texto2);
    }
}
