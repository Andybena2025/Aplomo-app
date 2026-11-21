package co.aplomo.web;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Salon;
import co.aplomo.seguridad.Sesiones;
import co.aplomo.servicio.ColoresAgenda;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Usuarios;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Paso 26, RF-001/002/024/033: contratos HTTP y reglas nuevas, con reversión de datos de prueba. */
class HttpPaso26Test extends HttpPrueba {
    private int evaluador(List<Integer> empresas) throws Exception {
        int id = servicioUsuarios().crear(cuenta(admin), new Usuarios.Datos("Evaluador QA",
                UUID.randomUUID() + "@prueba.invalid", "123", "Prueba", Cuenta.Rol.EVALUADOR,
                null, empresas, ColoresAgenda.PALETA.get(0)), clave);
        dao.contrasena(con, id, cuenta(id).hash(), false);
        return id;
    }

    @Test void ingresoComunNoRevelaBloqueoAntesDelTercerFallo() throws Exception {
        String correo = cuenta(admin).correo();
        for (int intento = 1; intento <= 5; intento++) {
            var respuesta = post("/login", Map.of("csrf", csrf(get("/login")), "correo", correo, "clave", "Incorrecta"));
            assertEquals(401, respuesta.statusCode());
            if (intento < 3) assertFalse(respuesta.body().contains("15 minutos"));
            if (intento == 3) assertTrue(respuesta.body().contains("Te quedan 2 intentos."));
            if (intento == 4) assertTrue(respuesta.body().contains("Te queda 1 intento."));
            if (intento == 5) assertTrue(respuesta.body().contains("Por seguridad, tu cuenta quedó bloqueada 15 minutos."));
        }
    }

    @Test void correoDesconocidoRecibeLosMismosAvisos() throws Exception {
        for (int intento = 1; intento <= 5; intento++) {
            var respuesta = post("/login", Map.of("csrf", csrf(get("/login")), "correo", "ausente@prueba.invalid",
                    "clave", "Incorrecta"));
            assertEquals(401, respuesta.statusCode());
            assertEquals(intento >= 5, respuesta.body().contains("Por seguridad, tu cuenta quedó bloqueada 15 minutos."));
            assertEquals(intento == 3, respuesta.body().contains("Te quedan 2 intentos."));
        }
    }

    @Test void perfilEditaSoloDatosPropiosYRegistraBitacora() throws Exception {
        entrar(admin);
        assertTrue(get("/").body().contains("href=\"/cuenta\" class=\"avatar\""));
        var respuesta = post("/cuenta", Map.of("csrf", csrf(get("/cuenta")), "nombre", "Mi nombre <b>",
                "telefono", "300123", "cargo", "Coordinación", "rol", "USUARIO", "empresa", "999999"));
        assertEquals(303, respuesta.statusCode());
        assertEquals("Mi nombre <b>", cuenta(admin).nombre());
        assertEquals(Cuenta.Rol.ADMINISTRADOR, cuenta(admin).rol());
        assertTrue(new CuentaEmpresaDao().ids(con, admin).isEmpty());
        assertTrue(get("/cuenta").body().contains("Mi nombre &lt;b&gt;"));
        assertEquals(1, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM bitacora WHERE tabla='cuenta' "
                + "AND id_registro=? AND accion='EDITAR'", fila -> fila.getInt(1), admin));
    }

    @Test void perfilExigeCsrfYConservaCamposAlFallar() throws Exception {
        entrar(admin);
        assertEquals(403, post("/cuenta", Map.of("nombre", "Intruso")).statusCode());
        var respuesta = post("/cuenta", Map.of("csrf", csrf(get("/cuenta")), "nombre", "Conservar",
                "telefono", "", "cargo", "QA"));
        assertEquals(400, respuesta.statusCode());
        assertTrue(respuesta.body().contains("value=\"Conservar\""));
        assertFalse(cuenta(admin).nombre().equals("Conservar"));
    }

    @Test void perfilCambiaClaveYRechazaActualIncorrecta() throws Exception {
        entrar(admin);
        assertEquals(400, post("/cuenta/clave", Map.of("csrf", csrf(get("/cuenta")), "actual", "Incorrecta",
                "nueva", "NuevaClave2026", "confirmacion", "NuevaClave2026")).statusCode());
        assertEquals(303, post("/cuenta/clave", Map.of("csrf", csrf(get("/cuenta")), "actual", clave,
                "nueva", "NuevaClave2026", "confirmacion", "NuevaClave2026")).statusCode());
        assertFalse(cuenta(admin).temporal());
        assertNotNull(auth.ingresar(cuenta(admin).correo(), "NuevaClave2026"));
    }

    @Test void archivoAdministradorIncluyeEmpresaInactiva() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        new Empresas(tx).estadoEmpresa(cuenta(admin), escenario.empresaB, false);
        entrar(admin);
        String html = get("/archivo").body();
        assertTrue(html.contains("Empresa ajena QA"), html);
        assertTrue(html.contains("Inactiva"));
        assertEquals(200, get("/archivo?empresa=" + escenario.empresaB).statusCode());
    }

    @Test void archivoEvaluadorConservaSoloSuAutoriaAlRetirarAsignacion() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        int evaluador = evaluador(List.of(empresa));
        Sql.actualizar(con, "UPDATE bitacora SET id_cuenta=? WHERE tabla='evaluacion' "
                + "AND id_registro=? AND accion='CREAR'", evaluador, escenario.evalRojo);
        entrar(evaluador);
        new CuentaEmpresaDao().sincronizar(con, admin, evaluador, List.of(), false);
        String html = get("/archivo?empresa=" + empresa).body();
        assertEquals(200, get("/archivo").statusCode());
        assertTrue(html.contains("Ver historial"));
        assertFalse(html.contains("/puestos/" + escenario.puesto2 + "\""));
        assertEquals(403, get("/archivo?empresa=" + escenario.empresaB).statusCode());
        assertEquals(200, get("/puestos/" + escenario.puesto3).statusCode());
        assertEquals(303, get("/mis-evaluaciones").statusCode());
    }

    @Test void archivoUsuarioSoloFinalizadasSinFotos() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        entrar(usuario);
        String html = get("/archivo?empresa=" + empresa).body();
        assertTrue(html.contains("/puestos/" + escenario.puesto1 + "\""), html);
        assertFalse(html.contains("/puestos/" + escenario.puesto2 + "\""));
        assertFalse(html.contains("<img"));
        assertEquals(403, get("/archivo?empresa=" + escenario.empresaB).statusCode());
        String historial = get("/puestos/" + escenario.puesto1).body();
        assertTrue(historial.contains("/evaluaciones/" + escenario.evalVerde + "\""));
        assertFalse(historial.contains("/evaluaciones/" + escenario.evalAbierta + "\""));
    }

    @Test void agendaAbreMesConColorDeCuentaYUsuarioNoEntra() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        int evaluador = evaluador(List.of(empresa));
        var agenda = new Reservas(tx, reloj).agenda();
        var vista = agenda.consultar(cuenta(evaluador), Map.of());
        assertTrue(vista.mes());
        assertTrue(vista.citas().stream().allMatch(cita -> "#FF8FB8".equals(cita.color())));
        entrar(evaluador);
        assertTrue(get("/agenda").body().contains("color-0"));
        assertEquals("27/11/2026", Html.fecha(LocalDate.of(2026, 11, 27)));
        assertThrows(SecurityException.class, () -> agenda.consultar(cuenta(usuario), Map.of()));
    }

    @Test void citasCruzadasSeRechazanEnCualquierEmpresaYCanceladasNoBloquean() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        int evaluador = evaluador(List.of(empresa, escenario.empresaB));
        Reservas reservas = new Reservas(tx, reloj);
        Reservas.Datos cruzada = new Reservas.Datos(escenario.empresaB, escenario.sedeB, escenario.salonB,
                LocalDate.of(2026, 11, 11), LocalTime.of(11, 30), LocalTime.of(12, 30), "SEGUIMIENTO", "QA", "");
        var error = assertThrows(IllegalArgumentException.class, () -> reservas.guardar(cuenta(evaluador), 0, cruzada));
        assertEquals("Ese evaluador ya tiene una cita de 11:00 a 12:00 ese día. Elige otra hora.", error.getMessage());
        Reservas.Datos libre = new Reservas.Datos(escenario.empresaB, escenario.sedeB, escenario.salonB,
                LocalDate.of(2026, 11, 11), LocalTime.of(15, 0), LocalTime.of(16, 0), "SEGUIMIENTO", "QA", "");
        assertTrue(reservas.guardar(cuenta(evaluador), 0, libre) > 0);
    }

    @Test void notasJsonSoloPropiasYOtrosRolesReciben403() throws Exception {
        var escenario = new EscenarioConsultas(this, carpetaFotos);
        int evaluador = evaluador(List.of(empresa));
        var agenda = new Reservas(tx, reloj).agenda();
        agenda.guardarNota(cuenta(admin), escenario.reservaAbierta, "Revisar \"apoyos\"\n<script>");
        agenda.guardarNota(cuenta(admin), escenario.reservaB, "NOTA AJENA");
        entrar(evaluador);
        var respuesta = get("/notas/nuevas");
        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
        assertTrue(respuesta.body().contains("Revisar \\\"apoyos\\\" <script>"), respuesta.body());
        assertFalse(respuesta.body().contains("NOTA AJENA"));
        assertFalse(respuesta.body().contains("id_cuenta"));
        assertEquals(303, post("/reservas/" + escenario.reservaAbierta + "/entendido",
                Map.of("csrf", csrf(get("/")))).statusCode());
        assertEquals("[]", get("/notas/nuevas").body());
        post("/logout", Map.of("csrf", csrf(get("/"))));
        entrar(admin);
        assertEquals(403, get("/notas/nuevas").statusCode());
        post("/logout", Map.of("csrf", csrf(get("/"))));
        entrar(usuario);
        assertEquals(403, get("/notas/nuevas").statusCode());
    }

    @Test void consultaAutomaticaNoProlongaLaSesion() throws Exception {
        Sesiones sesiones = new Sesiones(reloj);
        var sesion = sesiones.crear(cuenta(admin));
        reloj.avanzar(Duration.ofMinutes(29));
        assertNotNull(sesiones.consultar(sesion.id()));
        reloj.avanzar(Duration.ofMinutes(1));
        assertEquals(null, sesiones.consultar(sesion.id()));
    }

    @Test void colorSeValidaYConservaSinNuevaEleccion() throws Exception {
        int evaluador = evaluador(List.of(empresa));
        assertEquals("#FF8FB8", cuenta(evaluador).colorAgenda());
        var cuenta = cuenta(evaluador);
        servicioUsuarios().editar(cuenta(admin), evaluador, new Usuarios.Datos(cuenta.nombre(), cuenta.correo(),
                "123", "QA", Cuenta.Rol.EVALUADOR, null, List.of(empresa)), "");
        assertEquals("#FF8FB8", cuenta(evaluador).colorAgenda());
        assertThrows(IllegalArgumentException.class, () -> ColoresAgenda.validar("red"));
    }
}
