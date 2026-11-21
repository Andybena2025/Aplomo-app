package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Puesto;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpEvaluarAhoraTest extends HttpPrueba {
    @Test
    void administradorVePendientesActivosYElEnlaceAbreElPuestoCorrecto() throws Exception {
        var salon = crearSalon();
        var empresas = new Empresas(tx);
        var visitas = new Visitas(tx, reloj);
        int pendiente = empresas.guardar(cuenta(admin),
                        new Puesto(0, salon.id(), null, null, 1, "Computador <ficticio>", "ESCRITORIO", "Centro",
                                null, true))
                .id();
        int inactivo = empresas.guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 2, "Inactivo", "MESA", "Centro", null, false)).id();
        empresas.estadoPuesto(cuenta(admin), inactivo, false);
        int reserva = new Reservas(tx, reloj).guardar(cuenta(admin), 0, solicitud(salon));
        new Reservas(tx, reloj).aceptar(cuenta(admin), reserva);
        int visita = visitas.iniciar(cuenta(admin), reserva);
        evaluacion(visita, salon.id(), 3);
        entrar(admin);
        String texto = get("/").body();
        assertFalse(texto.contains("Evaluar ahora"));
        assertTrue(texto.contains("id=\"visitas-en-curso\""));
        assertTrue(texto.contains("Computador &lt;ficticio&gt;"));
        assertTrue(texto.contains("/evaluaciones/nueva?visita=" + visita + "&amp;puesto=" + pendiente));
        assertFalse(texto.contains("&amp;puesto=" + inactivo));
        assertEquals(1, visitas.paraEvaluar(cuenta(admin)).size());
        assertEquals(200, get("/evaluaciones/nueva?visita=" + visita + "&puesto=" + pendiente).statusCode());
        evaluacion(visita, salon.id(), 1);
        assertFalse(get("/").body().contains("Todos los puestos están evaluados."));
    }

    @Test
    void sinVisitasMuestraMensajeYReservas() throws Exception {
        entrar(admin);
        String texto = get("/").body();
        assertTrue(texto.contains("No hay una visita en curso"));
        assertTrue(texto.contains("href=\"/reservas\">Ir a Reservas</a>"));
    }

    @Test
    void usuarioNoVeElAtajoNiPuedeEntrarPorDireccionManual() throws Exception {
        var salon = crearSalon();
        var reservas = new Reservas(tx, reloj);
        int reserva = reservas.guardar(cuenta(admin), 0, solicitud(salon));
        reservas.aceptar(cuenta(admin), reserva);
        new Empresas(tx).guardar(cuenta(admin),
                new Puesto(0, salon.id(), null, null, 1, "Mesa", "MESA", "Centro", null, true));
        int visita = new Visitas(tx, reloj).iniciar(cuenta(admin), reserva);
        Sql.actualizar(con, "UPDATE cuenta SET contrasena_temporal=FALSE WHERE id_cuenta=?", usuario);
        entrar(usuario);
        String texto = get("/").body();
        assertFalse(texto.contains("evaluar-ahora"));
        assertFalse(texto.contains("id=\"visitas-en-curso\""));
        assertEquals(403, get("/evaluaciones/nueva?visita=" + visita).statusCode());
        assertThrows(SecurityException.class, () -> new Visitas(tx, reloj).paraEvaluar(cuenta(usuario)));
    }
}
