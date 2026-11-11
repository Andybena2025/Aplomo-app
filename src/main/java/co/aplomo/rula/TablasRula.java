package co.aplomo.rula;

import co.aplomo.db.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Carga las tablas A, B y C del metodo RULA y los niveles de riesgo
 * desde la base de datos (RF-027, RNF-010, RNF-011).
 * Los valores se leen UNA vez y se guardan en memoria.
 * Nunca se escriben las tablas en el codigo.
 */
public class TablasRula {

    // Solo se publica una carga completa; una lectura fallida no deja ceros utilizables.
    private volatile Datos datos;

    /** Crea un lector sin cargar; debe llamarse cargar antes de calcular (RF-027). */
    public TablasRula() {}

    /** Carga una sola vez desde MySQL y cierra la conexión propia (RF-027). */
    public synchronized void cargar() throws SQLException {
        if (datos != null) return;
        try (Connection con = Conexion.obtener()) {
            cargar(con);
        }
    }

    /** Carga una sola vez sin cerrar la conexión del llamador (RF-027). */
    public synchronized void cargar(Connection con) throws SQLException {
        if (datos != null) return;
        java.util.Objects.requireNonNull(con, "Falta la conexión a MySQL.");
        int[][] a = new int[18][8];
        int[][] b = new int[6][12];
        int[][] c = new int[8][7];
        cargarTabla(con, "A", a);
        cargarTabla(con, "B", b);
        cargarTabla(con, "C", c);
        NivelRiesgo[] niveles = cargarNiveles(con);
        datos = new Datos(a, b, c, niveles);
    }

    private void cargarTabla(Connection con, String nombre, int[][] destino) throws SQLException {
        String sql = "SELECT fila, columna, valor FROM valor_tabla_rula WHERE tabla = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int fila = rs.getInt("fila") - 1;     // la BD usa 1-based
                    int col = rs.getInt("columna") - 1;
                    int valor = rs.getInt("valor");
                    if (fila < 0 || fila >= destino.length || col < 0 || col >= destino[0].length
                            || valor < 1 || valor > (nombre.equals("C") ? 7 : 9)
                            || destino[fila][col] != 0) {
                        throw new SQLException("La tabla RULA " + nombre + " contiene datos inválidos o repetidos.");
                    }
                    destino[fila][col] = valor;
                }
            }
        }
        for (int[] fila : destino) {
            for (int valor : fila) {
                if (valor == 0) throw new SQLException("La tabla RULA " + nombre + " está incompleta.");
            }
        }
    }

    private NivelRiesgo[] cargarNiveles(Connection con) throws SQLException {
        String sql = "SELECT id_nivel, color, puntaje_minimo, puntaje_maximo, nombre, accion, nivel_oficial_rula "
                   + "FROM nivel_riesgo ORDER BY puntaje_minimo";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            NivelRiesgo[] niveles = new NivelRiesgo[7];
            while (rs.next()) {
                int minimo = rs.getInt("puntaje_minimo");
                int maximo = rs.getInt("puntaje_maximo");
                int oficial = rs.getInt("nivel_oficial_rula");
                String color = rs.getString("color");
                String nombre = rs.getString("nombre");
                String accion = rs.getString("accion");
                int id = rs.getInt("id_nivel");
                if (id < 1 || minimo < 1 || maximo > 7 || maximo < minimo || oficial < 1 || oficial > 4
                        || !("VERDE".equals(color) || "AMARILLO".equals(color) || "ROJO".equals(color))
                        || nombre == null || nombre.isBlank() || accion == null || accion.isBlank()) {
                    throw new SQLException("Los niveles de riesgo contienen datos inválidos.");
                }
                NivelRiesgo nivel = new NivelRiesgo(color, minimo, maximo, nombre, accion, oficial, id);
                for (int p = minimo; p <= maximo; p++) {
                    if (niveles[p - 1] != null) throw new SQLException("Los rangos de riesgo se superponen.");
                    niveles[p - 1] = nivel;
                }
            }
            for (NivelRiesgo nivel : niveles) {
                if (nivel == null) throw new SQLException("Los niveles de riesgo están incompletos.");
            }
            return niveles;
        }
    }

    // RF-018: Tabla A — fila = (brazo-1)*3 + (antebrazo-1), columna = (muneca-1)*2 + (giro-1)
    /** Busca el valor en la Tabla A. Brazo 1-6, antebrazo 1-3, muneca 1-4, giro 1-2. */
    public int buscarA(int brazo, int antebrazo, int muneca, int giro) {
        Datos d = datosCargados();
        validar(brazo, 6, "brazo");
        validar(antebrazo, 3, "antebrazo");
        validar(muneca, 4, "muñeca");
        validar(giro, 2, "giro de muñeca");
        int fila = (brazo - 1) * 3 + (antebrazo - 1);
        int col = (muneca - 1) * 2 + (giro - 1);
        return d.a()[fila][col];
    }

    // RF-018: Tabla B — fila = cuello-1, columna = (tronco-1)*2 + (piernas-1)
    /** Busca el valor en la Tabla B. Cuello 1-6, tronco 1-6, piernas 1-2. */
    public int buscarB(int cuello, int tronco, int piernas) {
        Datos d = datosCargados();
        validar(cuello, 6, "cuello");
        validar(tronco, 6, "tronco");
        validar(piernas, 2, "piernas");
        int fila = cuello - 1;
        int col = (tronco - 1) * 2 + (piernas - 1);
        return d.b()[fila][col];
    }

    // RF-018: Tabla C — fila = puntaje A ajustado (1-8, 8+ = 8), columna = puntaje B ajustado (1-7, 7+ = 7)
    /** Busca el puntaje final en la Tabla C. Puntaje A 1-8+, puntaje B 1-7+. */
    public int buscarC(int puntajeA, int puntajeB) {
        Datos d = datosCargados();
        validar(puntajeA, Integer.MAX_VALUE, "puntaje A");
        validar(puntajeB, Integer.MAX_VALUE, "puntaje B");
        int fila = Math.min(puntajeA, 8) - 1;
        int col = Math.min(puntajeB, 7) - 1;
        return d.c()[fila][col];
    }

    // RF-020: Nivel de riesgo segun el puntaje final
    /** Devuelve el nivel de riesgo para un puntaje final de 1 a 7. */
    public NivelRiesgo nivelDe(int puntajeFinal) {
        Datos d = datosCargados();
        validar(puntajeFinal, 7, "puntaje final");
        return d.niveles()[puntajeFinal - 1];
    }

    private Datos datosCargados() {
        Datos d = datos;
        if (d == null) throw new IllegalStateException("Primero carga las tablas RULA desde MySQL.");
        return d;
    }

    private static void validar(int valor, int maximo, String campo) {
        if (valor < 1 || valor > maximo) {
            throw new IllegalArgumentException("El valor de " + campo + " debe estar entre 1 y " + maximo + ".");
        }
    }

    private record Datos(int[][] a, int[][] b, int[][] c, NivelRiesgo[] niveles) {}

    /** Registro inmutable con los datos de un nivel de riesgo (RF-020). */
    public record NivelRiesgo(
        String color,
        int puntajeMinimo,
        int puntajeMaximo,
        String nombre,
        String accion,
        int nivelOficialRula,
        int idNivel
    ) {}
}
