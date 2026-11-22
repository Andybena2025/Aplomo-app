package co.aplomo.instalacion;

import co.aplomo.config.Config;
import co.aplomo.db.Sql;

import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Herramienta voluntaria de desarrollo; nunca se ejecuta al iniciar la web. RNF-011.
 */
public final class Demo {
    public static final String CONFIRMACION = "REINICIAR DEMO";
    private static final String EMPRESAS = "nit IN ('900.512.384-1','901.638.205-4','012345'," +
            "'900.782.156-2','901.428.603-7','900.315.928-5','901.267.840-3')";
    private static final List<String> MUESTRAS = List.of("de100001-493b-4bc9-a64e-d51b4ea3c901.jpg",
            "de100002-493b-4bc9-a64e-d51b4ea3c902.jpg", "de100003-493b-4bc9-a64e-d51b4ea3c903.jpg",
            "de100004-493b-4bc9-a64e-d51b4ea3c904.jpg");

    private Demo() {
    }

    /**
     * Usa la base configurada y pide contraseña oculta y confirmación antes de borrar. RNF-011.
     */
    public static void ejecutar(boolean reiniciar) {
        Console terminal = System.console();
        if (terminal == null) throw new IllegalStateException(
                "Abre este script directamente en una terminal para confirmar y escribir la c" +
                        "ontraseña de forma privada.");
        terminal.printf("ALINEO · %s de datos de demostración%nBase configurada: %s%n",
                reiniciar ? "Reinicio" : "Carga", base(Config.url()));
        String usuario = terminal.readLine("Usuario administrador de MySQL (Enter = root): ");
        if (usuario == null) throw new IllegalArgumentException("Operación cancelada.");
        usuario = usuario.isBlank() ? "root" : usuario.trim();
        if (usuario.equalsIgnoreCase(Config.usuario()))
            throw new IllegalArgumentException("Usa el administrador de MySQL, no la cuenta de la aplicación.");
        char[] clave = terminal.readPassword("Contraseña de MySQL (Enter si no tiene): ");
        if (clave == null) throw new IllegalArgumentException("Operación cancelada.");
        Properties credenciales = new Properties();
        credenciales.setProperty("user", usuario);
        credenciales.setProperty("password", new String(clave));
        Arrays.fill(clave, '\0');
        try (Connection con = DriverManager.getConnection(Config.url(), credenciales)) {
            credenciales.remove("password");
            verificarAdministrador(con);
            String confirmacion = "";
            if (reiniciar) {
                terminal.printf("Se reiniciarán SOLO las siete empresas identificadas de la demo. Las cuentas nunca " +
                        "se borran. Sus empresas asignadas se conservan por NIT.%n");
                confirmacion = terminal.readLine("Escribe %s para continuar: ", CONFIRMACION);
                if (!CONFIRMACION.equals(confirmacion)) {
                    terminal.printf("Cancelado. No se cambió ningún dato.%n");
                    return;
                }
            }
            con.setAutoCommit(false);
            List<String> fotos = List.of();
            try {
                if (reiniciar) fotos = reiniciar(con, confirmacion, Path.of("docs/base_de_datos/datos_demo.sql"),
                        Config.carpetaFotos());
                else cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"));
                con.commit();
            } catch (Exception error) {
                con.rollback();
                throw error;
            }
            int borradas = limpiarFotos(con, Config.carpetaFotos(), fotos);
            copiarMuestras(Config.carpetaFotos());
            terminal.printf("Datos listos. Entra como Evaluador y abre Mi agenda o Visitas en curso. " +
                    "Fotos antiguas retiradas: %d.%n", borradas);
            terminal.printf("No se modificaron otras empresas, cuentas ni config/db.properties.%n");
        } catch (Exception error2) {
            throw new IllegalStateException(
                    "No se pudo completar la demostración. Si falló la base, todos sus cambios se" +
                            " revirtieron. Revisa la conexión, los permisos y las cuentas vinculadas. " +
                            (error2 instanceof IllegalArgumentException ? error2.getMessage() : ""), error2);
        } finally {
            credenciales.clear();
        }
    }

    private static String base(String url) {
        int fin = url.indexOf('?');
        String texto2 = fin < 0 ? url : url.substring(0, fin);
        return texto2.substring(texto2.lastIndexOf('/') + 1);
    }

    /**
     * No concede permisos a la cuenta de la app.
     */
    static void verificarAdministrador(Connection con) throws SQLException {
        String actual = Sql.uno(con, "SELECT CURRENT_USER()", fila -> fila.getString(1));
        if (actual.split("@", 2)[0].equalsIgnoreCase(Config.usuario()))
            throw new IllegalArgumentException("Usa el administrador de MySQL, no la cuenta de la aplicación.");
        var privilegios = Sql.listar(con,
                "SELECT PRIVILEGE_TYPE FROM information_schema.USER_PRIVILEGES WHERE GRANTEE=" +
                        "CONCAT(QUOTE(SUBSTRING_INDEX(CURRENT_USER(),'@',1)),'@',QUOTE(SUBSTRING_INDE" +
                        "X(CURRENT_USER(),'@',-1)))", fila2 -> fila2.getString(1));
        if (!privilegios.contains("DELETE") || !(privilegios.contains("SUPER") || privilegios.contains("CREATE USER")))
            throw new IllegalArgumentException(
                    "Esta operación requiere una cuenta administradora de MySQL con permisos globales.");
    }

    /**
     * Carga explícita del perfil Docker demo; usa únicamente los datos ficticios del cargador. RNF-011.
     */
    public static void ejecutarEntorno() {
        if (!"si".equals(System.getenv("APLOMO_DEMO_CARGA")))
            throw new IllegalArgumentException("Activa el perfil demo para cargar la demostración.");
        try (Connection conexion = co.aplomo.db.Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                cargar(conexion, Path.of("/app/demo/datos_demo.sql"), Config.carpetaFotos());
                conexion.commit();
            } catch (Exception error) {
                conexion.rollback();
                throw error;
            }
            System.out.println("Demostración completa cargada. No se cambiaron cuentas ni empresas reales.");
        } catch (Exception error2) {
            throw new IllegalStateException("No se pudo cargar la demostración.", error2);
        }
    }

    /**
     * RF-003, RNF-009: carga idempotente; no confirma ni crea bases.
     */
    public static void cargar(Connection con, Path sql) throws IOException, SQLException {
        cargar(con, sql, Config.carpetaFotos());
    }

    /**
     * RF-003, RNF-009: aísla los archivos de pruebas sin tocar la carpeta real.
     */
    public static void cargar(Connection con, Path sql, Path fotos) throws IOException, SQLException {
        if (con.getAutoCommit()) throw new IllegalArgumentException("La demostración requiere una transacción.");
        Integer admin = Sql.uno(con, "SELECT id_cuenta FROM cuenta WHERE rol='ADMINISTRADOR' AND estado='ACTIVA' A" +
                "ND contrasena_temporal=FALSE ORDER BY id_cuenta LIMIT 1", fila -> fila.getInt(1));
        // Andy: después de cargar la demo la cuenta inicial queda Inactiva; repetir no la reactiva.
        if (admin == null) admin = Sql.uno(con,
                "SELECT id_cuenta FROM cuenta WHERE correo='andres@rula.local' AND rol='ADMINISTRADOR' " +
                        "AND estado='ACTIVA' AND (SELECT COUNT(*) FROM cuenta equipo WHERE equipo.correo " +
                        "IN ('andres@rula.local','johan@rula.local','gabby@rula.local') " +
                        "AND equipo.rol='ADMINISTRADOR' AND equipo.estado='ACTIVA')=3 " +
                        "AND EXISTS(SELECT 1 FROM cuenta inicial WHERE inicial.correo='admin@rula.local' " +
                        "AND inicial.rol='ADMINISTRADOR' AND inicial.estado='INACTIVA')", fila -> fila.getInt(1));
        if (admin == null) throw new IllegalArgumentException(
                "Primero ingresa como Administrador y cambia la contraseña temporal de ALINEO.");
        if (!Files.isRegularFile(sql)) throw new IllegalArgumentException("Falta el archivo de la demo.");
        DemoIdentidades.preparar(con, admin);
        Integer ajenas = Sql.uno(con, "SELECT COUNT(*) FROM reserva r JOIN salon l USING(id_salon) JOIN sede s USIN" +
                "G(id_sede) JOIN empresa e USING(id_empresa) WHERE r.codigo IN ('DEMO-FIN-01'" +
                ",'DEMO-FIN-02','DEMO-COL-01','DEMO-COL-02','DEMO-ESP-01','DEMO-ESP-02','DEMO" +
                "-LST-E1','DEMO-LST-E2','DEMO-LST-E3') AND NOT (" + EMPRESAS + ")", fila2 -> fila2.getInt(1));
        if (ajenas > 0) throw new IllegalArgumentException("Hay un código demo en otra empresa. No se cambió nada.")
                ;
        int anteriores = Sql.uno(con, "SELECT COUNT(*) FROM empresa WHERE " + EMPRESAS, fila -> fila.getInt(1));
        int plan = Sql.uno(con, "SELECT COUNT(*) FROM bitacora WHERE tabla='empresa' " + "AND detalle='DEMO-33-PLAN'",
                fila -> fila.getInt(1));
        if (plan == 0) {
            Integer compatible = Sql.uno(con, "SELECT b.id_registro FROM bitacora b JOIN empresa e " +
                    "ON e.id_empresa=b.id_registro WHERE b.tabla='empresa' AND b.detalle='DEMO-32-PLAN' " +
                    "AND " + EMPRESAS + " ORDER BY b.id_bitacora LIMIT 1", fila -> fila.getInt(1));
            if (compatible != null) {
                // RF-003, RNF-009: Paso 33 cambia nombres; conserva resultados y marca anterior de auditoría.
                new co.aplomo.dao.BitacoraDao().registrar(con, admin,
                        java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")), "empresa",
                        compatible, "EDITAR", null, null, "DEMO-33-PLAN");
                plan = 1;
            }
        }
        if (anteriores > 0 && plan == 0) throw new IllegalArgumentException(
                "La demo conserva cargas anteriores. Ejecuta scripts/reiniciar_demo.sh y confirma el reinicio.");
        DemoRoles.cargar(con, fotos, admin);
        DemoIdentidades.completar(con, admin);
    }

    /**
     * RNF-009: desvincula y restaura las mismas cuentas por NIT, sin confirmar fuera de la transacción.
     */
    public static List<String> reiniciar(Connection con, String confirmacion, Path sql,
                                         Path fotos) throws SQLException, IOException {
        if (!CONFIRMACION.equals(confirmacion))
            throw new IllegalArgumentException("Falta la confirmación REINICIAR DEMO.");
        if (con.getAutoCommit()) throw new IllegalArgumentException("El reinicio requiere una transacción.");
        var punto = con.setSavepoint();
        try {
            var vinculos = VinculosDemo.guardar(con);
            vinculos.desvincular(con);
            List<String> anteriores = vaciar(con, confirmacion);
            cargar(con, sql, fotos);
            vinculos.restaurar(con);
            return anteriores;
        } catch (SQLException | IOException | RuntimeException error) {
            con.rollback(punto);
            throw error;
        }
    }

    /**
     * Borra solo las siete empresas identificadas; el llamador confirma o revierte. RNF-011.
     */
    public static List<String> vaciar(Connection con, String confirmacion) throws SQLException {
        if (!CONFIRMACION.equals(confirmacion))
            throw new IllegalArgumentException("Falta la confirmación REINICIAR DEMO.");
        if (con.getAutoCommit()) throw new IllegalArgumentException("El reinicio requiere una transacción.");
        LinkedHashMap<String, List<Integer>> ids = new LinkedHashMap<>();
        ids.put("empresa", Sql.listar(con, "SELECT id_empresa FROM empresa WHERE " + EMPRESAS + " FOR UPDATE",
                fila -> fila.getInt(1)));
        List<Integer> empresas = ids.get("empresa");
        if (empresas.isEmpty()) return List.of();
        DemoIdentidades.exigirIdentidad(con, empresas);
        if (!hijos(con, "cuenta", "id_cuenta", "id_empresa", empresas).isEmpty()) throw new IllegalArgumentException(
                "Hay cuentas vinculadas a una empresa demo. Reasígnalas antes; no se borró nada.");
        if (!hijos(con, "solicitud_acceso", "id_solicitud", "id_empresa", empresas).isEmpty()) {
            throw new IllegalArgumentException("Hay solicitudes de acceso vinculadas. Revísalas antes del reinicio.");
        }
        ids.put("sede", hijos(con, "sede", "id_sede", "id_empresa", empresas));
        ids.put("salon", hijos(con, "salon", "id_salon", "id_sede", ids.get("sede")));
        ids.put("area", hijos(con, "area", "id_area", "id_empresa", empresas));
        ids.put("cargo", hijos(con, "cargo", "id_cargo", "id_empresa", empresas));
        ids.put("puesto_trabajo", hijos(con, "puesto_trabajo", "id_puesto", "id_salon", ids.get("salon")));
        ids.put("reserva", hijos(con, "reserva", "id_reserva", "id_salon", ids.get("salon")));
        ids.put("visita", hijos(con, "visita", "id_visita", "id_reserva", ids.get("reserva")));
        ids.put("evaluacion", hijos(con, "evaluacion", "id_evaluacion", "id_visita", ids.get("visita")));
        ids.put("condiciones_puesto",
                hijos(con, "condiciones_puesto", "id_evaluacion", "id_evaluacion", ids.get("evaluacion")));
        ids.put("fotografia", hijos(con, "fotografia", "id_foto", "id_evaluacion", ids.get("evaluacion")));
        ids.put("analisis_rula", hijos(con, "analisis_rula", "id_analisis", "id_foto", ids.get("fotografia")));
        ids.put("medicion_angulo",
                hijos(con, "medicion_angulo", "id_medicion", "id_analisis", ids.get("analisis_rula")));
        ids.put("sugerencia", hijos(con, "sugerencia", "id_sugerencia", "id_evaluacion", ids.get("evaluacion")));
        ids.put("pendiente_arreglo",
                hijos(con, "pendiente_arreglo", "id_pendiente", "id_evaluacion", ids.get("evaluacion")));
        ids.put("cuenta_empresa", hijos(con, "cuenta_empresa", "id_cuenta_empresa", "id_empresa", empresas));
        ids.put("mensaje", hijos(con, "mensaje", "id_mensaje", "id_reserva", ids.get("reserva")));
        ids.put("informe_empresa", hijos(con, "informe_empresa", "id_informe", "id_empresa", empresas));
        List<String> rutas = ids.get("fotografia").isEmpty() ? List.of() : Sql.listar(con,
                "SELECT ruta FROM fotografia WHERE id_foto IN (" + marcas(ids.get("fotografia")) + ")",
                fila2 -> fila2.getString(1), ids.get("fotografia").toArray());
        for (var entrada : ids.entrySet())
            if (!entrada.getValue().isEmpty()) {
                List<Object> parametros = new ArrayList<>();
                parametros.add(entrada.getKey());
                parametros.addAll(entrada.getValue());
                Sql.actualizar(con,
                        "DELETE FROM bitacora WHERE tabla=? AND id_registro IN (" + marcas(entrada.getValue()) + ")",
                        parametros.toArray());
            }
        Map<String, String> claves = Map.of("puesto_trabajo", "id_puesto", "condiciones_puesto", "id_evaluacion",
                "fotografia", "id_foto", "analisis_rula", "id_analisis", "medicion_angulo", "id_medicion",
                "pendiente_arreglo", "id_pendiente", "informe_empresa", "id_informe", "cuenta_empresa",
                "id_cuenta_empresa");
        for (String tabla : List.of("mensaje", "cuenta_empresa", "medicion_angulo", "analisis_rula", "fotografia",
                "sugerencia", "pendiente_arreglo", "condiciones_puesto", "evaluacion", "visita", "reserva",
                "puesto_trabajo", "salon", "sede", "area", "cargo", "informe_empresa", "empresa")) {
            var lista = ids.get(tabla);
            if (!lista.isEmpty()) Sql.actualizar(con,
                    "DELETE FROM " + tabla + " WHERE " + claves.getOrDefault(tabla, "id_" + tabla) + " IN (" +
                            marcas(lista) + ")", lista.toArray());
        }
        return rutas;
    }

    private static List<Integer> hijos(Connection con, String tabla, String id, String fk,
                                       List<Integer> padres) throws SQLException {
        return padres.isEmpty() ? List.of() : Sql.listar(con,
                "SELECT " + id + " FROM " + tabla + " WHERE " + fk + " IN (" + marcas(padres) + ") FOR UPDATE",
                fila -> fila.getInt(1), padres.toArray());
    }

    private static String marcas(List<?> lista) {
        return String.join(",", Collections.nCopies(lista.size(), "?"));
    }

    /**
     * No toca archivos ajenos, compartidos, externos ni enlaces simbólicos. RNF-011.
     */
    public static int limpiarFotos(Connection con, Path carpeta, List<String> rutas) throws SQLException, IOException {
        if (!Files.isDirectory(carpeta, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(carpeta)) return 0;
        Path base = carpeta.toRealPath();
        int cantidad = 0;
        for (String ruta : new LinkedHashSet<>(rutas)) {
            if (ruta == null || ruta.isBlank() || Path.of(ruta).isAbsolute()) continue;
            Path archivo = base.resolve(ruta).normalize();
            if (!archivo.startsWith(base) || archivo.equals(base)) continue;
            boolean enlace = false;
            for (Path p = archivo; p != null && !p.equals(base); p = p.getParent())
                if (Files.isSymbolicLink(p)) {
                    enlace = true;
                    break;
                }
            if (enlace || !Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS) ||
                    !archivo.toRealPath().startsWith(base)) continue;
            Integer referencias = Sql.uno(con, "SELECT COUNT(*) FROM fotografia WHERE ruta=?", fila -> fila.getInt(1),
                    ruta);
            if (referencias == 0 && Files.deleteIfExists(archivo)) cantidad++;
        }
        return cantidad;
    }

    private static void copiarMuestras(Path carpeta) throws IOException {
        if (Files.isSymbolicLink(carpeta)) throw new IOException("La carpeta de fotos no puede ser un enlace.");
        Files.createDirectories(carpeta);
        Path origen = Path.of("src/main/resources/static/img/muestra_perfil.jpg");
        for (String nombre : MUESTRAS) {
            Path destino = carpeta.resolve(nombre);
            if (!Files.exists(destino, LinkOption.NOFOLLOW_LINKS)) Files.copy(origen, destino);
        }
    }
}
