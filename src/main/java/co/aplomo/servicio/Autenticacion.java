package co.aplomo.servicio;

import co.aplomo.dao.CuentaDao;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.seguridad.Contrasenas;
import java.sql.SQLException;
import java.time.*;
import java.util.Locale;

/** RF-001a, RF-001c: ingreso y cambios persistentes; tiempos comprobables sin esperar minutos. */
public final class Autenticacion {
    public static final int MAX_INTENTOS = 5;
    public static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final String HASH_FICTICIO = Contrasenas.cifrar("NoEsUnaCuenta2026");
    private final Transacciones tx;
    private final CuentaDao dao;
    private final Clock reloj;

    /** Usa MySQL y la hora local del servidor para el bloqueo (RF-001a). */
    public Autenticacion() { this(new Transacciones(),new CuentaDao(),Clock.systemDefaultZone()); }
    /** Permite controlar conexiones y reloj en las comprobaciones (RNF-011). */
    public Autenticacion(Transacciones tx, CuentaDao dao, Clock reloj) { this.tx=tx; this.dao=dao; this.reloj=reloj; }

    /** Devuelve la cuenta o null con el mismo resultado para correo, clave o estado incorrectos. */
    public Cuenta ingresar(String correo, String clave) throws SQLException {
        String normalizado = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        return tx.ejecutar(con -> {
            Cuenta c = dao.porCorreo(con,normalizado);
            if (c == null) { Contrasenas.verificar(clave,HASH_FICTICIO); return null; }
            var ahora = LocalDateTime.now(reloj);
            boolean bloqueada = c.bloqueadoHasta() != null && ahora.isBefore(c.bloqueadoHasta());
            if (c.estado() == Estado.INACTIVA || bloqueada || (c.estado() == Estado.BLOQUEADA && c.bloqueadoHasta() == null)) {
                Contrasenas.verificar(clave,HASH_FICTICIO); return null;
            }
            int anteriores = c.bloqueadoHasta() != null ? 0 : c.intentos();
            if (!Contrasenas.verificar(clave,c.hash())) {
                int intentos = anteriores + 1;
                dao.acceso(con,c.id(),intentos,intentos >= MAX_INTENTOS ? ahora.plus(BLOQUEO) : null,
                        intentos >= MAX_INTENTOS ? Estado.BLOQUEADA : Estado.ACTIVA);
                return null;
            }
            dao.acceso(con,c.id(),0,null,Estado.ACTIVA);
            return dao.porId(con,c.id(),false);
        });
    }

    /** Relee el estado y los permisos vigentes para cada petición autenticada (RF-002). */
    public Cuenta actual(int id) throws SQLException { return tx.ejecutar(con -> dao.porId(con,id,false)); }

    /** Comprueba actual y confirmación, cambia el hash y quita la obligación temporal (RF-001c). */
    public Cuenta cambiar(Cuenta sesion, String actual, String nueva, String confirmacion) throws SQLException {
        if (nueva == null || !nueva.equals(confirmacion)) throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña.");
        Contrasenas.validar(nueva);
        return tx.ejecutar(con -> {
            Cuenta c = dao.porId(con,sesion.id(),true);
            if (c == null || c.estado() != Estado.ACTIVA || !c.hash().equals(sesion.hash())) throw new SecurityException("La sesión ya no está disponible. Vuelve a ingresar.");
            if (!Contrasenas.verificar(actual,c.hash())) throw new IllegalArgumentException("La contraseña actual no coincide.");
            if (Contrasenas.verificar(nueva,c.hash())) throw new IllegalArgumentException("Elige una contraseña diferente de la actual.");
            dao.contrasena(con,c.id(),Contrasenas.cifrar(nueva),false);
            return dao.porId(con,c.id(),false);
        });
    }
}
