package co.aplomo.web;

import co.aplomo.config.Contacto;
import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.servicio.Archivo;
import co.aplomo.servicio.Avisos;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-002/024/033, RNF-002/007: contacto por campo y detalles con navegación y permisos conservados.
 */
class HttpPaso31Test extends HttpPrueba {
    String anterior;

    @BeforeEach
    void contactoVacio() throws Exception {
        anterior = System.getProperty("aplomo.contacto.archivo");
        var archivo = carpetaFotos.resolve("contacto.properties");
        Files.writeString(archivo, "contacto.nombre=\ncontacto.cargo=\ncontacto.correo=\ncontacto.telefono=\n" +
                "contacto.whatsapp=\ncontacto.empresa.correo=empresa@prueba.invalid\n");
        System.setProperty("aplomo.contacto.archivo", archivo.toString());
    }

    @AfterEach
    void restaurar() {
        if (anterior == null) System.clearProperty("aplomo.contacto.archivo");
        else System.setProperty("aplomo.contacto.archivo", anterior);
    }

    @Test
    void contactoVacioSeVePublicoYConSesionYRespaldaEjemplosPorCampo() throws Exception {
        for (boolean autenticada : List.of(false, true)) {
            if (autenticada) entrar(admin);
            var respuesta = get("/ayuda");
            assertEquals(200, respuesta.statusCode());
            for (String dato : List.of("Laura Gabriela Romero", "Administradora principal", "gabriela.romero@alineo.co",
                    "+57 300 000 0000", "https://wa.me/573000000000"))
                assertTrue(respuesta.body().contains(dato), dato);
        }
        var archivo = carpetaFotos.resolve("ejemplos.properties");
        Files.writeString(archivo, "contacto.nombre=Persona de ejemplo\ncontacto.cargo=Responsable de ejemplo\n" +
                "contacto.correo=correo@ejemplo.com\ncontacto.telefono=+57 321 222 3333\n");
        Contacto contacto = Contacto.leer(archivo);
        assertEquals("Laura Gabriela Romero", contacto.nombre());
        assertEquals("Administradora principal", contacto.cargo());
        assertEquals("gabriela.romero@alineo.co", contacto.correo());
        assertEquals("+57 321 222 3333", contacto.telefono());
        assertEquals("573000000000", contacto.whatsapp());
    }

    @Test
    void mensajeUsaCorreoLuegoGabrielaLuegoPrimerAdministradorActivo() throws Exception {
        Sql.actualizar(con, "UPDATE cuenta SET nombre='Cuenta QA' WHERE rol='ADMINISTRADOR'");
        int gabriela = dao.crear(con, "Gabriella QA", UUID.randomUUID() + "@prueba.invalid", "123", "Pruebas",
                Cuenta.Rol.ADMINISTRADOR, null, cuenta(admin).hash());
        var avisos = new Avisos(tx, reloj);
        assertEquals(gabriela, avisos.principal(cuenta(admin)).id());
        var archivo = carpetaFotos.resolve("exacto.properties");
        Files.writeString(archivo, "contacto.correo=" + cuenta(admin).correo());
        System.setProperty("aplomo.contacto.archivo", archivo.toString());
        assertEquals(admin, avisos.principal(cuenta(admin)).id());
        Files.writeString(archivo, "contacto.correo=ausente@prueba.invalid\n");
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", gabriela);
        int primero = Sql.uno(con, "SELECT MIN(id_cuenta) FROM cuenta WHERE rol='ADMINISTRADOR' AND estado='ACTIVA'",
                f -> f.getInt(1));
        assertEquals(primero, avisos.principal(cuenta(admin)).id());
        dao.contrasena(con, usuario, cuenta(usuario).hash(), false);
        entrar(usuario);
        var formulario = get("/avisos/nuevo");
        assertEquals(200, formulario.statusCode());
        assertEquals(303, post("/avisos/nuevo",
                Map.of("csrf", csrf(formulario), "texto", "Mensaje QA reversible", "destinatario",
                        Integer.toString(usuario))).statusCode());
        int destino = Sql.uno(con, "SELECT id_destinatario FROM mensaje WHERE texto=? ORDER BY id_mensaje DESC LIMIT 1",
                f -> f.getInt(1), "Mensaje QA reversible");
        assertEquals(primero, destino);
        assertEquals(Cuenta.Rol.ADMINISTRADOR, cuenta(destino).rol());
    }

    @ParameterizedTest
    @EnumSource(Cuenta.Rol.class)
    void detallesCompartenUnTituloEnContenidoUnRegresoYAlcanceDelServidor(Cuenta.Rol rol) throws Exception {
        var datos = new EscenarioConsultas(this, carpetaFotos);
        var evaluador = evaluadorTrabajo(empresa);
        Sql.actualizar(con, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                "SET r.id_evaluador=? WHERE s.id_empresa=?", evaluador.id(), empresa);
        Sql.actualizar(con, "UPDATE bitacora SET id_cuenta=? WHERE tabla='evaluacion' AND accion='CREAR' " +
                "AND id_registro IN (?,?)", evaluador.id(), datos.evalVerde, datos.evalRojo);
        int actor = rol == Cuenta.Rol.ADMINISTRADOR ? admin : rol == Cuenta.Rol.USUARIO ? usuario : evaluador.id();
        dao.contrasena(con, actor, cuenta(actor).hash(), false);
        entrar(actor);
        for (String ruta : List.of("/archivo?empresa=" + empresa, "/puestos/" + datos.puesto1,
                "/visitas/" + datos.visitaFinal, "/reservas/" + datos.reservaFinal, "/empresas/" + empresa,
                "/cuenta")) {
            var respuesta = get(ruta);
            assertEquals(200, respuesta.statusCode(), ruta);
            String html = respuesta.body();
            String franja = html.substring(html.indexOf("<header"), html.indexOf("</header>") + 9);
            String contenido = html.substring(html.indexOf("<main"), html.indexOf("</main>"));
            assertFalse(franja.contains("Atrás"), ruta);
            assertFalse(franja.contains("cabecera-rol"), ruta);
            assertFalse(franja.contains("detalle-acciones"), ruta);
            assertFalse(franja.contains("<h1>"), ruta);
            assertFalse(html.contains("class=\"migas\""), ruta);
            assertEquals(1, Pattern.compile("<h1>").matcher(contenido).results().count(), ruta);
            assertEquals(1, Pattern.compile("class=\"detalle-volver\"").matcher(contenido).results().count(), ruta);
        }
        if (rol == Cuenta.Rol.USUARIO) {
            assertEquals(403, get("/visitas/" + datos.visitaAbierta).statusCode());
            assertEquals(403, get("/archivo?empresa=" + datos.empresaB).statusCode());
        }
    }

    @Test
    void archivoTieneCuatroTarjetasUnaFechaYCuentaNumericaSinRepetirEtiqueta() throws Exception {
        var fila = new Fila(
                Map.of("id", "1", "empresa", "7", "puesto", "9", "fecha", "2026-11-14", "color", "VERDE", "evaluador",
                        "2", "evaluador_nombre", "Persona QA"));
        var datos = new Archivo.Datos(List.of(new Archivo.Empresa(7, "Empresa <QA>", true)), List.of(fila), Map.of(),
                List.of());
        String html = PaginasArchivo.pagina(
                new co.aplomo.seguridad.Sesiones.Sesion("token", "csrf", cuenta(admin), java.time.Instant.now()), datos,
                Map.of("empresa", "7"));
        String indicadores = html.substring(html.indexOf("class=\"archivo-indicadores\""),
                html.indexOf("<form method=\"get\" class=\"archivo-filtros"));
        assertEquals(4, indicadores.split("<article", -1).length - 1);
        assertFalse(indicadores.contains(" – "));
        assertTrue(indicadores.contains("14/11/2026"));
        assertTrue(indicadores.contains("archivo-numero\">1</strong>"));
        assertFalse(indicadores.contains("1 evaluación"));
        assertFalse(indicadores.contains("Evaluador(a)"));
        assertTrue(html.contains("Empresa &lt;QA&gt;"));
    }

    @Test
    void usuarioConDosEmpresasTieneResumenYListaCompletaEscapada() throws Exception {
        String html = TablaUsuarios.pagina(
                new co.aplomo.seguridad.Sesiones.Sesion("token", "csrf", cuenta(admin), java.time.Instant.now()),
                List.of(cuenta(usuario)), List.of(new co.aplomo.dao.EmpresaDao.Opcion(1, "Primera <empresa>", true),
                        new co.aplomo.dao.EmpresaDao.Opcion(2, "Segunda empresa", true)),
                Map.of(usuario, List.of(1, 2)), "");
        assertTrue(html.contains("empresas-cantidad\">+1</small>"));
        assertTrue(html.contains("title=\"Primera &lt;empresa&gt; · Segunda empresa\""));
        assertTrue(html.contains("<span>Primera &lt;empresa&gt;</span>"));
        assertFalse(html.contains("<span>Primera <empresa>"));
    }
}
