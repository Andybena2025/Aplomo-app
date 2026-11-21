package co.aplomo.servicio;

import co.aplomo.db.Sql;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** RF-033, RNF-007: colores personales; nunca representan el semáforo RULA. */
public final class ColoresAgenda {
    public static final List<String> PALETA = List.of(
            "#FF8FB8", "#6FA8FF", "#B9A7FF", "#7FD1E0", "#F4B393", "#C9CCD2");

    private ColoresAgenda() {}

    /** RF-003/033: valida una elección o devuelve null para asignación automática. */
    public static String validar(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String normalizado = color.strip().toUpperCase(Locale.ROOT);
        if (!PALETA.contains(normalizado)) {
            throw new IllegalArgumentException("Elige un color de la lista.");
        }
        return normalizado;
    }

    /** RF-003/033: conserva el color existente o elige el primero libre bajo el bloqueo de cuentas. */
    public static String elegir(Connection conexion, int cuenta, String solicitado) throws SQLException {
        String elegido = validar(solicitado);
        if (elegido != null) {
            return elegido;
        }
        String anterior = Sql.uno(conexion, "SELECT COALESCE(color_agenda,'') FROM cuenta WHERE id_cuenta=?",
                fila -> fila.getString(1), cuenta);
        if (PALETA.contains(anterior)) {
            return anterior;
        }
        List<String> usados = Sql.listar(conexion,
                "SELECT color_agenda FROM cuenta WHERE rol='EVALUADOR' AND estado<>'INACTIVA' "
                        + "AND id_cuenta<>? AND color_agenda IS NOT NULL",
                fila -> fila.getString(1), cuenta);
        return PALETA.stream().filter(color -> !usados.contains(color)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ya se usaron los seis colores. Elige uno para este evaluador."));
    }

    /** RF-003: guarda únicamente el color de la cuenta que el servicio ya autorizó. */
    public static void guardar(Connection conexion, int cuenta, String color) throws SQLException {
        Sql.actualizar(conexion, "UPDATE cuenta SET color_agenda=? WHERE id_cuenta=?", color, cuenta);
    }
}
