package co.aplomo.instalacion;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.VisitaDao;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Visitas;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * RF-020/022/025: resultados ficticios obtenidos con el motor y los servicios existentes.
 */
final class ResultadosDemo {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int[] CASOS = {2, 5, 6, 2, 1, 4, 0, 7};
    private static final String[] POSTURAS = {"Alcance a la repisa alta", "Digitación en pantalla",
            "Levantamiento de caja", "Inspección de pieza", "Atención al público", "Empaque en mesa",
            "Uso del celular", "Pesaje en báscula"};
    private static final int[][] DIAS_OCTUBRE = {{2, 7, 13, 19, 22, 28}, {1, 6, 9, 14, 20, 26}, {5, 8, 15, 21, 23, 29}};
    private static final int[] DESFASES_LIMITE = {-2, 3, 8, 15, 25};

    private ResultadosDemo() {
    }

    static void preparar(Connection con, Path fotos, int admin, List<Integer> empresas,
                         List<Integer> evaluadores) throws SQLException, IOException {
        int[] responsables = {1, 0, 0, 1, 2, 2, 1};
        int[] turnos = new int[3];
        for (int i = 0; i < empresas.size(); i++) {
            int evaluador = evaluadores.get(responsables[i]);
            if (i == 6) new co.aplomo.dao.CuentaEmpresaDao().sincronizar(con, admin, evaluador,
                    List.of(empresas.get(0), empresas.get(3), empresas.get(6)), false);
            for (int salon : Sql.listar(con, "SELECT l.id_salon FROM salon l JOIN sede s USING(id_sede) " +
                    "WHERE s.id_empresa=? ORDER BY l.id_salon", f -> f.getInt(1), empresas.get(i))) {
                int persona = responsables[i];
                int turno = turnos[persona]++;
                LocalDate fecha = LocalDate.of(2026, 10, DIAS_OCTUBRE[persona][turno]);
                LocalTime hora = LocalTime.ofSecondOfDay((420 + ((turno * 5 + persona * 7) % 18) * 30) * 60L);
                int duracion = 1 + (turno + persona) % 3;
                int reserva = Sql.insertar(con, "INSERT INTO reserva(id_salon,id_administrador,id_evaluador," +
                                "codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,estado) " +
                                "VALUES(?,?,?,?,?,?,?,'EVALUACION_INICIAL',?,'EN_COLA')", salon, evaluador, evaluador,
                        "D28-H-" + salon, fecha, hora, hora.plusHours(duracion), "Revisión de las tareas del turno");
                finalizar(con, fotos, evaluador, reserva, salon, fecha, hora, duracion);
            }
            if (i == 6) new co.aplomo.dao.CuentaEmpresaDao().sincronizar(con, admin, evaluador,
                    List.of(empresas.get(0), empresas.get(3)), false);
        }
    }

    static int caso(Connection con, int puesto, int postura, LocalDate fecha) throws SQLException {
        int indice = indice(con, puesto);
        // RF-022: la demo mejora por fecha; casos distintos por puesto y postura, calculados por el motor real.
        int inicio = fecha.isBefore(LocalDate.of(2026, 11, 8)) ? 0 :
                fecha.isBefore(LocalDate.of(2026, 11, 17)) ? 4 : 6;
        int cantidad = inicio == 0 ? 4 : 2;
        return CASOS[inicio + Math.floorMod(indice + postura, cantidad)];
    }

    private static int indice(Connection con, int puesto) throws SQLException {
        return Sql.uno(con, "SELECT COUNT(*) FROM puesto_trabajo p JOIN puesto_trabajo elegido " +
                        "ON p.id_salon=elegido.id_salon WHERE elegido.id_puesto=? AND " +
                        "(p.numero<elegido.numero OR p.numero=elegido.numero AND p.id_puesto<elegido.id_puesto)",
                f -> f.getInt(1), puesto);
    }

    static void finalizar(Connection con, Path fotos, int evaluador, int reserva, int salon, LocalDate fecha,
                          LocalTime hora, int duracion) throws SQLException, IOException {
        CuentaDao cuentas = new CuentaDao();
        var original = cuentas.porId(con, evaluador, false);
        cuentas.contrasena(con, evaluador, original.hash(), false);
        Clock reloj = Clock.fixed(fecha.atTime(hora.plusHours(duracion).minusMinutes(5)).atZone(ZONA).toInstant(),
                ZONA);
        Transacciones tx = new Transacciones(() -> DemoCompleta.sinConfirmar(con));
        try {
            var actor = cuentas.porId(con, evaluador, false);
            int visita = new VisitaDao().iniciar(con, reserva, fecha.atTime(hora));
            var servicio = new Evaluaciones(tx, reloj, new Fotos(fotos), new BitacoraDao());
            byte[] imagen;
            try (var entrada = ResultadosDemo.class.getResourceAsStream(
                    "/static/img/demo/08_laboratorio_buena_postura_perfil.jpg")) {
                if (entrada == null) throw new IOException("Falta la foto de muestra.");
                imagen = entrada.readAllBytes();
            }
            for (int puesto : Sql.listar(con, "SELECT id_puesto FROM puesto_trabajo WHERE id_salon=? ORDER BY numero",
                    f -> f.getInt(1), salon)) {
                for (int postura = 0; postura < 2; postura++) {
                    servicio.guardar(actor, visita, new Evaluaciones.Datos(puesto,
                                    POSTURAS[Math.floorMod(indice(con, puesto) * 2 + postura, POSTURAS.length)],
                                    postura == 0 ? "MAS_INADECUADA" : "MAYOR_EXPOSICION", "Observación del turno",
                                    120, false,
                                    false, false, false, false, false,
                                    "Demostración: caso manual simulado; no corresponde a una medición de la foto.",
                                    true,
                                    false), List.of(new Fotos.Entrada(imagen, "PERFIL", "Foto de demostración")),
                            List.of(new Analisis.Solicitud(0,
                                    DemoCompleta.entrada(caso(con, puesto, postura, fecha)))));
                }
            }
            new Visitas(tx, reloj).finalizar(actor, visita, "Revisión de las tareas del turno.");
        } finally {
            cuentas.contrasena(con, evaluador, original.hash(), original.temporal());
        }
    }

    static void seguimiento(Connection con, int admin, List<Integer> empresas) throws SQLException {
        var filas = Sql.listar(con, "SELECT pa.id_pendiente,e.id_puesto,pa.estado FROM pendiente_arreglo pa " +
                        "JOIN evaluacion e USING(id_evaluacion) JOIN puesto_trabajo p USING(id_puesto) " +
                        "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE s.id_empresa IN " + "(" +
                        String.join(",", java.util.Collections.nCopies(empresas.size(), "?")) + ") " +
                        "ORDER BY e.id_visita DESC,pa.id_pendiente DESC", f -> new int[]{f.getInt(1), f.getInt(2)},
                empresas.toArray());
        java.util.Set<Integer> abiertos = new java.util.HashSet<>();
        for (var fila : filas) {
            boolean abierto = abiertos.size() < 5 && abiertos.add(fila[1]);
            Sql.actualizar(con, "UPDATE pendiente_arreglo SET estado=?,prioridad=?,fecha_limite=?,descripcion=? " +
                            "WHERE id_pendiente=?", abierto ? "ABIERTO" : "ARREGLADO",
                    abierto && abiertos.size() % 2 == 1 ? "ALTA" : "MEDIA",
                    LocalDate.now(ZONA).plusDays(DESFASES_LIMITE[Math.max(0, abiertos.size() - 1)]),
                    "Revisar el alcance y los apoyos del puesto", fila[0]);
            new BitacoraDao().registrar(con, admin, java.time.LocalDateTime.now(ZONA), "pendiente_arreglo", fila[0],
                    "EDITAR", null, abierto ? "ABIERTO" : "ARREGLADO", "Seguimiento inicial de la demo planeada");
        }
    }
}
