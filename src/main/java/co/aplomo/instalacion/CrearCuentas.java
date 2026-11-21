package co.aplomo.instalacion;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.servicio.Autenticacion;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Usuarios;
import co.aplomo.seguridad.Permisos;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Importación voluntaria por el servicio de Usuarios; nunca guarda claves en archivos. RNF-011.
 */
public final class CrearCuentas {
    private CrearCuentas() {
    }

    private static final SecureRandom AZAR = new SecureRandom();
    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz", NUMEROS = "23456789";

    /**
     * RNF-011: componente de la aplicación.
     */
    public record Fila(String nombre, String correo, Rol rol, String nit) {
    }

    /**
     * RNF-011: componente de la aplicación.
     */
    public record Contacto(String telefono, String cargo) {
    }

    /**
     * La terminal real mantiene la contraseña de ingreso oculta, fuera de argumentos y archivos. RNF-011.
     */
    public static void main(String[] args) {
        try {
            Console terminal = System.console();
            if (terminal == null) throw new IllegalArgumentException(
                    "Abre una Terminal y ejecuta el script sin redirigir su salida. Las claves se" +
                            " muestran solo allí.");
            List<Fila> filas;
            try (Reader elemento = Files.newBufferedReader(Path.of("config/cuentas.csv"), StandardCharsets.UTF_8)) {
                filas = leer(elemento);
            }
            String correo = terminal.readLine("Correo del Administrador actual: ");
            char[] entrada = terminal.readPassword("Contraseña actual (no se muestra): ");
            if (correo == null || entrada == null)
                throw new IllegalArgumentException("Se canceló la lectura. No se crearon cuentas.");
            Cuenta actor;
            try {
                actor = new Autenticacion().ingresar(correo, new String(entrada));
            } finally {
                Arrays.fill(entrada, '\0');
            }
            if (actor == null) throw new IllegalArgumentException(
                    "No pudimos iniciar sesión. Revisa los datos o el bloqueo de la cuenta.");
            int errores = crear(new Usuarios(), new Empresas(), actor, filas, elemento2 -> {
                terminal.printf("Datos de %s%n", elemento2.correo());
                return new Contacto(terminal.readLine("Teléfono: "), terminal.readLine("Cargo: "));
            }, terminal.writer());
            if (errores > 0) System.exit(1);
        } catch (IllegalArgumentException | SecurityException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        } catch (IOException error2) {
            System.err.println("No pudimos leer config/cuentas.csv. Copia la plantilla y guárdala como CSV UTF-8.");
            System.exit(1);
        } catch (SQLException error3) {
            System.err.println("No pudimos completar la operación. Revisa MySQL y los permisos; las cuentas " +
                    "existentes se conservan.");
            System.exit(1);
        }
    }

    /**
     * Cuatro columnas; comillas dobles permiten comas en nombres. Sin contraseñas en el CSV. RNF-011.
     */
    public static List<Fila> leer(Reader entrada) throws IOException {
        var filas = new ArrayList<Fila>();
        var correos = new HashSet<String>();
        var lineas = new BufferedReader(entrada);
        String primera = lineas.readLine();
        if (primera == null ||
                !campos(primera.replaceFirst("^\\uFEFF", "")).equals(List.of("nombre", "correo", "rol", "nit_empresa")))
            throw new IllegalArgumentException("La primera línea debe ser: nombre,correo,rol,nit_empresa.");
        String linea;
        int numero = 1;
        while ((linea = lineas.readLine()) != null) {
            numero++;
            if (linea.isBlank()) continue;
            if (linea.length() > 4000 || filas.size() >= 1000) throw new IllegalArgumentException(
                    "El archivo admite hasta 1000 cuentas y líneas de hasta 4000 caracteres.");
            var elementos = campos(linea);
            if (elementos.size() != 4)
                throw new IllegalArgumentException("Revisa las cuatro columnas de la línea " + numero + ".");
            Rol rol;
            try {
                rol = Rol.valueOf(elementos.get(2));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "El rol de la línea " + numero + " debe ser ADMINISTRADOR, EVALUADOR o USUARIO.");
            }
            String correo = elementos.get(1).toLowerCase(Locale.ROOT), nit = elementos.get(3);

            // Reutiliza las reglas del formulario antes de crear ninguna cuenta.
            var datos = new Usuarios.Datos(elementos.get(0), correo, "Pendiente", "Pendiente", rol,
                    rol == Rol.USUARIO ? 1 : null);
            if (rol == Rol.USUARIO && nit.isBlank())
                throw new IllegalArgumentException("El Usuario de la línea " + numero + " necesita nit_empresa.");
            if (rol == Rol.ADMINISTRADOR && !nit.isBlank()) throw new IllegalArgumentException(
                    "Deja nit_empresa vacío para el Administrador de la línea " + numero + ".");
            if (!correos.add(datos.correo())) throw new IllegalArgumentException(
                    "El mismo correo aparece más de una vez en el CSV (línea " + numero + ").");
            filas.add(new Fila(datos.nombre(), datos.correo(), rol, nit));
        }
        if (filas.isEmpty()) throw new IllegalArgumentException("El CSV no contiene cuentas para crear.");
        return List.copyOf(filas);
    }

    private static List<String> campos(String linea) {
        var valores = new ArrayList<String>();
        var valor = new StringBuilder();
        boolean comillas = false, cerrada = false;
        for (int i = 0; i < linea.length(); i++) {
            char elemento = linea.charAt(i);
            if (elemento == '"') {
                if (comillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    valor.append('"');
                    i++;
                } else if (comillas) {
                    comillas = false;
                    cerrada = true;
                } else if (valor.toString().isBlank() && !cerrada) {
                    valor.setLength(0);
                    comillas = true;
                } else throw new IllegalArgumentException(
                        "Revisa las comillas del CSV. Usa comillas dobles alrededor de un campo que c" +
                                "ontenga comas.");
            } else if (elemento == ',' && !comillas) {
                valores.add(valor.toString().strip());
                valor.setLength(0);
                cerrada = false;
            } else {
                if (cerrada && !Character.isWhitespace(elemento))
                    throw new IllegalArgumentException("Hay texto después de cerrar un campo entre comillas.");
                valor.append(elemento);
            }
        }
        if (comillas) throw new IllegalArgumentException(
                "Falta cerrar una comilla del CSV. Cada cuenta debe estar en una sola línea.");
        valores.add(valor.toString().strip());
        return valores;
    }

    /**
     * Dieciséis caracteres legibles, siempre con letras y números; bcrypt lo cifra en Usuarios.crear. RNF-011.
     */
    public static String temporal() {
        char[] clave = new char[16];
        clave[0] = LETRAS.charAt(AZAR.nextInt(LETRAS.length()));
        clave[1] = NUMEROS.charAt(AZAR.nextInt(NUMEROS.length()));
        String todos = LETRAS + NUMEROS;
        for (int i = 2; i < clave.length; i++) clave[i] = todos.charAt(AZAR.nextInt(todos.length()));
        for (int i = clave.length - 1; i > 0; i--) {
            int j = AZAR.nextInt(i + 1);
            char elemento = clave[i];
            clave[i] = clave[j];
            clave[j] = elemento;
        }
        return new String(clave);
    }

    /**
     * Tabla única: cada clave se imprime justo después de crear su cuenta y nunca se vuelve a imprimir. RNF-011.
     */
    public static int crear(Usuarios usuarios, Empresas empresas, Cuenta actor, List<Fila> filas,
            Function<Fila, Contacto> preguntar, PrintWriter salida) throws SQLException {
        if (actor != null && actor.rol() == Rol.ADMINISTRADOR && actor.temporal()) throw new IllegalArgumentException(
                "Primero cambia tu contraseña temporal desde la app y vuelve a ejecutar la he" + "rramienta.");
        Permisos.administrador(actor);
        var existentes = new HashSet<String>();
        for (var cuenta : usuarios.listar(actor)) existentes.add(cuenta.correo().toLowerCase(Locale.ROOT));
        var porNit = new HashMap<String, Integer>();
        for (var empresa2 : empresas.listar(actor))
            if (empresa2.activo()) porNit.put(empresa2.nit(), empresa2.id());

        // Recoge datos obligatorios antes de imprimir la tabla, sin inventar teléfonos o cargos.
        var datos = new LinkedHashMap<Fila, Usuarios.Datos>();
        var errores = new HashMap<Fila, String>();
        for (var elemento : filas) {
            if (existentes.contains(elemento.correo())) continue;
            var asignadas = elemento.rol() == Rol.ADMINISTRADOR ||
                    elemento.nit().isBlank() ? List.<Integer>of() : Arrays.stream(elemento.nit().split(";", -1))
                                                                    .map(String::strip).map(porNit::get).toList();
            Integer empresa = asignadas.size() == 1 ? asignadas.get(0) : null;
            if (asignadas.stream().anyMatch(Objects::isNull)) {
                errores.put(elemento, "no creada: la empresa no existe o está inactiva");
                continue;
            }
            try {
                var contacto = preguntar.apply(elemento);
                if (contacto == null) throw new IllegalArgumentException("Faltan teléfono y cargo.");
                datos.put(elemento,
                        new Usuarios.Datos(elemento.nombre(), elemento.correo(), contacto.telefono(), contacto.cargo(),
                                elemento.rol(), empresa, asignadas));
            } catch (IllegalArgumentException error) {
                errores.put(elemento, "no creada: " + error.getMessage());
            }
        }
        salida.println("Correo | Contraseña temporal / resultado");
        salida.println("-------|-------------------------------");
        int fallos = 0;
        for (var elemento2 : filas) {
            if (existentes.contains(elemento2.correo())) {
                salida.println(elemento2.correo() + " | ya existe");
                continue;
            }
            if (errores.containsKey(elemento2)) {
                salida.println(elemento2.correo() + " | " + errores.get(elemento2));
                fallos++;
                continue;
            }
            String clave = temporal();
            try {
                usuarios.crear(actor, datos.get(elemento2), clave);
                existentes.add(elemento2.correo());
                salida.println(elemento2.correo() + " | " + clave);
                salida.flush();
            } catch (IllegalArgumentException error2) {
                boolean existe = usuarios.listar(actor).stream()
                        .anyMatch(cuenta2 -> cuenta2.correo().equalsIgnoreCase(elemento2.correo()));
                salida.println(
                        elemento2.correo() + " | " + (existe ? "ya existe" : "no creada: " + error2.getMessage()));
                if (!existe) fallos++;
            } catch (SQLException error3) {
                salida.println(elemento2.correo() + " | no creada: no pudimos guardar; revisa MySQL");
                fallos++;
            }
        }
        salida.flush();
        return fallos;
    }
}
