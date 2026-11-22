package co.aplomo.web;

import co.aplomo.soporte.HttpPrueba;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.servicio.Usuarios;
import co.aplomo.config.Contacto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 25: páginas públicas, permisos de ayuda, contacto privado y datos dinámicos.
 */
class HttpAyudaTest extends HttpPrueba {
    String propiedadAnterior;

    @BeforeEach
    void contactoAusente() {
        propiedadAnterior = System.getProperty("aplomo.contacto.archivo");
        System.setProperty("aplomo.contacto.archivo", carpetaFotos.resolve("no-existe.properties").toString());
    }

    @AfterEach
    void restaurar() {
        if (propiedadAnterior == null) System.clearProperty("aplomo.contacto.archivo");
        else System.setProperty("aplomo.contacto.archivo", propiedadAnterior);
    }

    @Test
    void portadaContieneTresIdeasYAcercaEstaEnAyuda() throws Exception {
        String html = get("/login").body();
        for (String texto : List.of("Fotografía el puesto", "Mide los ángulos", "Conoce el riesgo",
                "Evaluamos posturas de trabajo con el método RULA.", "© 2026 ALINEO"))
            assertTrue(html.contains(texto));
        assertFalse(html.contains("Conoce ALINEO. ↓"));
        assertFalse(html.contains("Volver a iniciar sesión"));
        assertFalse(html.contains("nosotros.js"));
        assertTrue(get("/ayuda").body().contains("Proyecto Nuclear 2026-2"));
    }

    @Test
    void nosotrosTieneMarcaAnimadaSinCrearSesion() throws Exception {
        var respuesta = get("/nosotros");
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("marca-llegada"));
        assertTrue(respuesta.body().contains("Quiénes somos"));
        assertTrue(respuesta.headers().allValues("Set-Cookie").isEmpty());
    }

    @Test
    void ayudaPublicaSoloIngresoYContactoSinCrearSesion() throws Exception {
        var respuesta = get("/ayuda");
        assertEquals(200, respuesta.statusCode());
        String texto2 = respuesta.body();
        assertTrue(texto2.contains("id=\"entrar\""));
        assertTrue(texto2.contains("id=\"acerca\""));
        for (int i = 2; i <= 9; i++) assertFalse(texto2.contains("data-tema=\"" + i + "\""));
        assertTrue(texto2.contains("Tu administradora"));
        assertTrue(texto2.contains("mailto:laura.romero@alineo.co"));
        assertTrue(texto2.contains("wa.me/573000000000"));
        assertTrue(respuesta.headers().allValues("Set-Cookie").isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Cuenta.Rol.class)
    void cadaRolSoloRecibeSusTemas(Cuenta.Rol rol) throws Exception {
        int id = admin;
        if (rol == Cuenta.Rol.EVALUADOR) {
            id = servicioUsuarios().crear(cuenta(admin),
                    new Usuarios.Datos("Evaluadora QA", UUID.randomUUID() + "@prueba.invalid", "123", "QA", rol, null,
                            List.of(empresa)), clave);
            dao.contrasena(con, id, cuenta(id).hash(), false);
        } else if (rol == Cuenta.Rol.USUARIO) {
            id = usuario;
            dao.contrasena(con, id, cuenta(id).hash(), false);
        }
        entrar(id);
        String texto2 = get("/ayuda").body();
        Set<String> esperados = switch (rol) {
            case ADMINISTRADOR ->
                    Set.of("cuentas", "asignaciones", "empresas", "estados", "agenda", "notas", "acceso", "resultados",
                            "archivo", "pendientes", "cuenta");
            case EVALUADOR ->
                    Set.of("agenda", "solicitudes", "evaluacion", "durante", "fotos", "resultados", "practicar",
                            "archivo", "pendientes", "cuenta", "iniciar", "proximas");
            case USUARIO ->
                    Set.of("resultados", "semaforo", "solicitudes", "proximas", "archivo", "pendientes", "cuenta",
                            "acerca");
        };
        for (String tema : Set.of("cuentas", "asignaciones", "empresas", "estados", "agenda", "notas", "acceso",
                "resultados", "archivo", "entrar", "solicitudes", "evaluacion", "durante", "fotos", "practicar",
                "semaforo", "proximas", "acerca")) {
            assertEquals(esperados.contains(tema), texto2.contains("id=\"" + tema + "\""), tema);
        }
        assertFalse(texto2.contains("aria-label=\"Atrás\""));
        assertTrue(texto2.contains("class=\"ayuda-tareas\""));
    }

    @Test
    void nivelesYAccionesVienenDeMysql() throws Exception {
        entrar(admin);
        String antes = get("/ayuda?tema=resultados").body();
        int id = Sql.uno(con, "SELECT id_nivel FROM nivel_riesgo ORDER BY puntaje_minimo LIMIT 1",
                fila -> fila.getInt(1));
        Sql.actualizar(con, "UPDATE nivel_riesgo SET nombre=?,accion=? WHERE id_nivel=?", "Nombre de QA",
                "Acción diferente <script>", id);
        String despues = get("/ayuda?tema=resultados").body();
        assertNotEquals(antes, despues);
        assertTrue(despues.contains("Nombre de QA"));
        assertTrue(despues.contains("Acción diferente &lt;script&gt;"));
    }

    @Test
    void contactoPrivadoSeLeeYSeEscapaYWhatsappSeNormaliza() throws Exception {
        Path archivo = carpetaFotos.resolve("contacto.properties");
        Files.writeString(archivo, "contacto.nombre=Persona <b>QA</b>\ncontacto.cargo=Responsable de prueba\ncon" +
                "tacto.correo=soporte@prueba.invalid\ncontacto.telefono=+57 300 123 4567\ncon" +
                "tacto.whatsapp=+57 300 123 4567\n");
        System.setProperty("aplomo.contacto.archivo", archivo.toString());
        String texto2 = get("/ayuda").body();
        assertTrue(texto2.contains("Persona &lt;b&gt;QA&lt;/b&gt;"));
        assertTrue(texto2.contains("mailto:soporte@prueba.invalid"));
        assertTrue(texto2.contains("https://wa.me/573001234567"));
        assertFalse(texto2.contains("Escríbele al Administrador de tu cuenta.</p>"));
    }

    @Test
    void contactoIncompletoNoInventaDatosYNuncaInyectaEnlaces() throws Exception {
        Path archivo = carpetaFotos.resolve("parcial.properties");
        Files.writeString(archivo, "contacto.nombre=Persona QA\ncontacto.correo=javascript:alert(1)\ncontacto.wh" +
                "atsapp=https://sitio.invalid\n");
        var elemento = Contacto.leer(archivo);
        assertTrue(elemento.incompleto());
        assertEquals("", elemento.correo());
        assertEquals("", elemento.whatsapp());
        System.setProperty("aplomo.contacto.archivo", archivo.toString());
        String texto2 = get("/ayuda").body();
        assertTrue(texto2.contains("Persona QA"));
        assertFalse(texto2.contains("javascript:"));
        assertTrue(texto2.contains("Tu administradora"));
    }

    @Test
    void contactoNoSeIgnoraSoloPorNoTenerNombre() throws Exception {
        Path archivo = carpetaFotos.resolve("correo.properties");
        Files.writeString(archivo, "contacto.correo=solo@prueba.invalid");
        System.setProperty("aplomo.contacto.archivo", archivo.toString());
        String texto2 = get("/ayuda").body();
        assertTrue(texto2.contains("mailto:solo@prueba.invalid"));
        assertTrue(texto2.contains("Tu administradora"));
    }

    @Test
    void marcaNoCambiaIdentificadoresNiIconoNiEncuadre() throws Exception {
        var respuesta = get("/login");
        assertTrue(respuesta.headers().allValues("Set-Cookie").get(0).startsWith("APLOMO_SESION="));
        assertTrue(respuesta.body().contains("viewBox=\"0 0 1400 933\""));
        assertTrue(respuesta.body().contains("x1=\"1020\" y1=\"20\""));
        assertTrue(respuesta.body().contains("/static/css/aplomo.css"));
        assertEquals("DEMO-APLOMO-PRACTICA", co.aplomo.modelo.Demostracion.NIT_PRACTICA);
        assertEquals("ALINEO · Práctica de evaluación", Html.escapar(co.aplomo.servicio.PracticaEvaluacion.NOMBRE));
    }
}
