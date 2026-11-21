package co.aplomo.seguridad;

import co.aplomo.modelo.Cuenta;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * RF-001b, RNF-004: sesiones del servidor, IDs y CSRF independientes de 32 bytes aleatorios.
 */
public final class Sesiones {
    public static final Duration INACTIVIDAD = Duration.ofMinutes(30);
    private final Map<String, Sesion> sesiones = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock reloj;

    /**
     * Datos exclusivamente del servidor; cuenta null representa el formulario de ingreso. RNF-011.
     */
    public record Sesion(String id, String csrf, Cuenta cuenta, Instant ultimoAcceso) {
    }

    /**
     * Usa el reloj real (RF-001b).
     */
    public Sesiones() {
        this(Clock.systemUTC());
    }

    /**
     * Recibe un reloj controlable para comprobar los 30 minutos (RNF-011).
     */
    public Sesiones(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Crea una sesión nueva y elimina sesiones caducadas; al ingresar se rota el ID (RNF-004).
     */
    public synchronized Sesion crear(Cuenta cuenta) {
        var ahora = reloj.instant();
        sesiones.values().removeIf(sesion2 -> !ahora.isBefore(sesion2.ultimoAcceso().plus(INACTIVIDAD)));
        var sesion3 = new Sesion(token(), token(), cuenta, ahora);
        sesiones.put(sesion3.id(), sesion3);
        return sesion3;
    }

    /**
     * Recupera y renueva actividad; exactamente a los 30 minutos ya no permite entrar. RNF-011.
     */
    public synchronized Sesion obtener(String id) {
        var sesion2 = sesiones.get(id);
        if (sesion2 == null) return null;
        var ahora = reloj.instant();
        if (!ahora.isBefore(sesion2.ultimoAcceso().plus(INACTIVIDAD))) {
            sesiones.remove(id);
            return null;
        }
        var actualizada = new Sesion(sesion2.id(), sesion2.csrf(), sesion2.cuenta(), ahora);
        sesiones.put(id, actualizada);
        return actualizada;
    }

    /**
     * RF-001b: la consulta automática no cuenta como actividad del evaluador.
     */
    public synchronized Sesion consultar(String id) {
        Sesion sesion = sesiones.get(id);
        if (sesion != null && !reloj.instant().isBefore(sesion.ultimoAcceso().plus(INACTIVIDAD))) {
            sesiones.remove(id);
            return null;
        }
        return sesion;
    }

    /**
     * Cierra la sesión; una cookie antigua ya no abre rutas internas (RF-001b).
     */
    public synchronized void cerrar(String id) {
        sesiones.remove(id);
    }

    /**
     * Invalida todas las sesiones de la cuenta tras cambiar contraseña o permisos (RNF-004).
     */
    public synchronized void invalidarCuenta(int id) {
        sesiones.values().removeIf(sesion2 -> sesion2.cuenta() != null && sesion2.cuenta().id() == id);
    }

    /**
     * Compara el token del formulario con el guardado en el servidor (RNF-004).
     */
    public static boolean csrfValido(Sesion sesion, String recibido) {
        return sesion != null && recibido != null &&
                MessageDigest.isEqual(sesion.csrf().getBytes(StandardCharsets.UTF_8),
                        recibido.getBytes(StandardCharsets.UTF_8));
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
