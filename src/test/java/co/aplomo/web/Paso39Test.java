package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.SeccionConsulta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Consultas.Pagina;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** RF-016/017/024: contratos de presentación del Paso 39, sin cálculos ni persistencia nuevos. */
class Paso39Test {
    private Sesion sesion() {
        var cuenta = new Cuenta(1, null, "Ana QA", "qa@prueba.invalid", "", "", "", false, 0, null,
                Cuenta.Estado.ACTIVA, Cuenta.Rol.ADMINISTRADOR);
        return new Sesion("qa", "csrf", cuenta, Instant.now());
    }

    private Fila fila(int id, LocalDate fecha, String estado) {
        return new Fila(Map.ofEntries(Map.entry("id", "" + id), Map.entry("fecha", fecha.toString()),
                Map.entry("estado", estado), Map.entry("empresa_nombre", "Empresa <script>"),
                Map.entry("sede_nombre", "Sede"), Map.entry("salon_nombre", "Lugar"), Map.entry("inicio", "09:00:00"),
                Map.entry("fin", "10:00:00"), Map.entry("puestos_evaluados", "1"), Map.entry("puntaje", "7"),
                Map.entry("color", "ROJO"), Map.entry("nivel", "4")));
    }

    @Test
    void permisoTieneDosTarjetasIndependientesYConservaValores() {
        String html = FotosGuiadas.permiso(true);
        assertEquals(2, html.split("class=\"campo-opcion permiso-opcion\"", -1).length - 1);
        assertTrue(html.contains("data-valor=\"CON_FOTOS\""));
        assertTrue(html.contains("data-valor=\"SIN_REGISTRO_FOTOGRAFICO\""));
        assertTrue(html.contains("name=\"registro\""));
        assertTrue(html.contains("Guardamos los datos del puesto, sin fotos ni puntaje."));
        assertTrue(FotosGuiadas.consentimiento().contains("name=\"acepta\""));
        assertTrue(FotosGuiadas.consentimiento().contains("Por escrito, antes de empezar."));
    }

    @Test
    void cadaDiaDeReservasTieneSuCajaYLaHoraNoTieneSegundos() {
        var filas = List.of(fila(1, Html.hoy(), "EN_COLA"), fila(2, Html.hoy().plusDays(1), "EN_COLA"));
        String html = TablasTrabajo.pagina(sesion(), new Pagina(SeccionConsulta.RESERVAS, filas,
                Map.of("estado", "TODOS"), Map.of(), Map.of(), null), null);
        assertEquals(2, html.split("data-dia-reservas", -1).length - 1);
        assertTrue(html.contains("es-hoy"));
        assertTrue(html.contains("Mañana"));
        assertTrue(html.contains("1 reserva"));
        assertTrue(html.contains("9:00"));
        assertFalse(html.contains(">09:00:00<"));
        assertTrue(html.contains("Empresa &lt;script&gt;"));
    }

    @Test
    void mesesSeAbrenDeCincoEnCincoYElMasRecienteEstaAbierto() {
        var filas = new ArrayList<Fila>();
        LocalDate mes = Html.hoy().withDayOfMonth(1);
        for (int i = 0; i < 8; i++) filas.add(fila(i + 1, mes.plusDays(i), "FINALIZADA"));
        filas.add(fila(20, mes.minusMonths(1), "FINALIZADA"));
        String html = MesesVisitas.mostrar(sesion(), filas);
        assertEquals(2, html.split("data-mes-visitas=", -1).length - 1);
        assertTrue(html.contains("open><summary>"));
        assertTrue(html.contains("Ver las 3 visitas restantes"));
        assertTrue(html.contains("Resultado más alto"));
        assertTrue(html.contains("8 visitas ·"));
        assertTrue(html.indexOf(mes.toString().substring(0, 7)) <
                html.indexOf(mes.minusMonths(1).toString().substring(0, 7)));
    }

    @Test
    void verPorOfreceLosModosDeEmpresaYUsuarios() {
        String empresas = TablaEmpresas.pagina(sesion(), List.of(), "", true, Map.of());
        for (String opcion : List.of("EVALUADOR", "PROXIMA", "CIUDAD", "AZ"))
            assertTrue(empresas.contains("data-ver-por=\"" + opcion + "\""));
        assertFalse(empresas.contains("data-lista-orden"));
        String usuarios = TablaUsuarios.pagina(sesion(), List.of(), List.of(), Map.of(), "");
        for (String opcion : List.of("ROL", "EMPRESA", "AZ"))
            assertTrue(usuarios.contains("data-ver-por=\"" + opcion + "\""));
    }
}
