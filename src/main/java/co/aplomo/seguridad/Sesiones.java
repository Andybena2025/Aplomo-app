package co.aplomo.seguridad;

import co.aplomo.modelo.Cuenta;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** RF-001b, RNF-004: sesiones del servidor, IDs y CSRF independientes de 32 bytes aleatorios. */
public final class Sesiones {
    public static final Duration INACTIVIDAD = Duration.ofMinutes(30);
    private final Map<String,Sesion> sesiones = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock reloj;

    /** Datos exclusivamente del servidor; cuenta null representa el formulario de ingreso. */
    public record Sesion(String id, String csrf, Cuenta cuenta, Instant ultimoAcceso) {}

    /** Usa el reloj real (RF-001b). */
    public Sesiones() { this(Clock.systemUTC()); }
    /** Recibe un reloj controlable para comprobar los 30 minutos (RNF-011). */
    public Sesiones(Clock reloj) { this.reloj=reloj; }

    /** Crea una sesión nueva y elimina sesiones caducadas; al ingresar se rota el ID (RNF-004). */
    public synchronized Sesion crear(Cuenta cuenta) {
        var ahora = reloj.instant();
        sesiones.values().removeIf(s -> !ahora.isBefore(s.ultimoAcceso().plus(INACTIVIDAD)));
        var s = new Sesion(token(),token(),cuenta,ahora);
        sesiones.put(s.id(),s);
        return s;
    }

    /** Recupera y renueva actividad; exactamente a los 30 minutos ya no permite entrar. */
    public synchronized Sesion obtener(String id) {
        var s = sesiones.get(id);
        if (s == null) return null;
        var ahora = reloj.instant();
        if (!ahora.isBefore(s.ultimoAcceso().plus(INACTIVIDAD))) { sesiones.remove(id); return null; }
        var actualizada = new Sesion(s.id(),s.csrf(),s.cuenta(),ahora);
        sesiones.put(id,actualizada);
        return actualizada;
    }

    /** Cierra la sesión; una cookie antigua ya no abre rutas internas (RF-001b). */
    public synchronized void cerrar(String id) { sesiones.remove(id); }

    /** Invalida todas las sesiones de la cuenta tras cambiar contraseña o permisos (RNF-004). */
    public synchronized void invalidarCuenta(int id) {
        sesiones.values().removeIf(s -> s.cuenta() != null && s.cuenta().id() == id);
    }

    /** Compara el token del formulario con el guardado en el servidor (RNF-004). */
    public static boolean csrfValido(Sesion sesion, String recibido) {
        return sesion != null && recibido != null && MessageDigest.isEqual(
                sesion.csrf().getBytes(StandardCharsets.UTF_8),recibido.getBytes(StandardCharsets.UTF_8));
    }

    private String token() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
