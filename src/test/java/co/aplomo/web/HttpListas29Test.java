package co.aplomo.web;

import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.soporte.HttpPrueba;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-024, RNF-002: 50 visitas, 50 reservas y 30 pendientes en HTTP, con permisos por rol.
 */
class HttpListas29Test extends HttpPrueba {
    @ParameterizedTest
    @EnumSource(value = Cuenta.Rol.class, names = {"ADMINISTRADOR", "EVALUADOR"})
    void listasAbrenConDiezFilasYConservanTituloMesYControles(Cuenta.Rol rol) throws Exception {
        var salon = crearSalon();
        var evaluador = evaluadorTrabajo(empresa);
        LocalDate hoy = Html.hoy();
        for (int i = 0; i < 100; i++) {
            LocalDate fecha = i < 50 ? hoy.withDayOfMonth(1).plusDays(i % hoy.getDayOfMonth()) : hoy.plusDays(i % 8);
            int reserva = Sql.insertar(con, "INSERT INTO reserva(id_salon,id_administrador,id_evaluador," +
                            "codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,estado) " +
                            "VALUES(?,?,?,?,?,?,?,'EVALUACION_INICIAL',?,?)", salon.id(), admin, evaluador.id(),
                    "P29" + "-" + i, fecha, LocalTime.of(7 + i / (i < 50 ? hoy.getDayOfMonth() : 8), 0),
                    LocalTime.of(8 + i / (i < 50 ? hoy.getDayOfMonth() : 8), 0), "Tarea " + i,
                    i < 50 ? "FINALIZADA" : "EN_COLA");
            if (i >= 50) continue;
            int visita = Sql.insertar(con, "INSERT INTO visita(id_reserva,hora_real_inicio,hora_real_fin,estado) " +
                    "VALUES(?,?,?,'FINALIZADA')", reserva, fecha.atTime(8, 0), fecha.atTime(9, 0));
            if (i >= 30) continue;
            int evaluacion = evaluacion(visita, salon.id(), i + 1);
            Sql.insertar(con, "INSERT INTO pendiente_arreglo(id_evaluacion,puntaje,descripcion,prioridad," +
                    "fecha_registro,estado) VALUES(?,6,'Mejorar los apoyos','ALTA',?,'ABIERTO')", evaluacion, fecha);
        }
        int actor = rol == Cuenta.Rol.ADMINISTRADOR ? admin : evaluador.id();
        Sql.actualizar(con, "UPDATE cuenta SET nombre='Ing. Andrés Benavides' WHERE id_cuenta=?", actor);
        entrar(actor);
        for (String ruta : List.of("/visitas", "/reservas", "/pendientes")) {
            var respuesta = get(ruta);
            assertEquals(200, respuesta.statusCode(), ruta);
            String html = respuesta.body();
            var filas = Pattern.compile("<tr\\b(?=[^>]*data-registro=)[^>]*>").matcher(html);
            int visibles = 0;
            while (filas.find()) if (!filas.group().contains(" hidden")) visibles++;
            assertEquals(ruta.equals("/visitas") ? 5 : 10, visibles, ruta);
            assertTrue(html.contains(ruta.equals("/visitas") ? "data-ver-mes" : "Ver 10 más"), ruta);
            assertFalse(html.contains("class=\"metaNameRole\""));
            assertFalse(html.contains(">Más filtros<"));
            assertFalse(html.contains(" evaluados"));
            if (!ruta.equals("/pendientes")) {
                var grises = Pattern.compile("<a class=\"fila-enlace\"[^>]*><strong>[^<]*</strong><small[^>]*>([^<]*)")
                        .matcher(html);
                while (grises.find()) assertFalse(grises.group(1).contains("Empresa de prueba sin confirmar"));
            }
            if (ruta.equals("/visitas")) {
                assertTrue(html.contains("50 visitas ·"));
                assertTrue(html.contains("data-mes-visitas=\"" + hoy.toString().substring(0, 7) + "\""));
                assertTrue(html.contains("Ver las 45 visitas restantes"));
            }
        }
        assertTrue(get("/").body().contains("Hola, Andrés"));
        assertEquals("AB", Html.iniciales("Rep. Andrés Benavides"));
    }
}
