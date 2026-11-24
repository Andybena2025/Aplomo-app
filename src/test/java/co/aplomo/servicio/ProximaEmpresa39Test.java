package co.aplomo.servicio;

import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;
import co.aplomo.soporte.EscenarioConsultas;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** RF-002/024: próximas reservas de empresas propias, sin modificar alcance ni estados. */
class ProximaEmpresa39Test extends BasePrueba {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path fotos;

    private void reserva(int salon, LocalDate fecha, String estado) throws Exception {
        Sql.insertar(con, "INSERT INTO reserva(id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin," +
                "categoria,descripcion,estado,motivo_cancelacion) " +
                "VALUES(?,?,?,?,?,?,'SEGUIMIENTO','Próxima QA',?,?)", salon, admin,
                "P39-" + UUID.randomUUID().toString().substring(0, 8), fecha, LocalTime.of(9, 0),
                LocalTime.of(10, 0), estado, estado.equals("CANCELADA") ? "Cancelada para prueba" : null);
    }

    @Test
    void proximaExcluyeCanceladasYPasadasYNoRevelaEmpresaAjena() throws Exception {
        var e = new EscenarioConsultas(this, fotos);
        var hoy = LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        reserva(e.salonA, hoy.minusDays(1), "EN_COLA");
        reserva(e.salonA, hoy.plusDays(1), "CANCELADA");
        reserva(e.salonA, hoy.plusDays(2), "EN_COLA");
        reserva(e.salonA, hoy.plusDays(3), "ESPERANDO");
        reserva(e.salonB, hoy.plusDays(1), "EN_COLA");
        var filas = new Empresas(tx).resumen(cuenta(usuario));
        assertEquals(1, filas.size());
        assertEquals(e.empresaA, filas.get(0).empresa().id());
        assertEquals(hoy.plusDays(2).atTime(9, 0), filas.get(0).proximaVisita());
        assertTrue(filas.stream().noneMatch(f -> f.empresa().id() == e.empresaB));
    }
}
