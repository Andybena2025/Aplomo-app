package co.aplomo.soporte;

import co.aplomo.dao.CuentaDao;
import co.aplomo.db.*;
import co.aplomo.modelo.Cuenta;
import co.aplomo.servicio.Autenticacion;
import co.aplomo.seguridad.Contrasenas;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.UUID;

/** Datos nuevos, nunca cuentas reales: al terminar se revierte todo sin DELETE ni DDL. */
public abstract class BasePrueba {
    protected Connection con;
    protected Transacciones tx;
    protected CuentaDao dao;
    protected Autenticacion auth;
    protected RelojPrueba reloj;
    protected String clave;
    protected int admin, usuario, empresa;

    @BeforeEach
    void preparar() throws Exception {
        con=Conexion.obtener(); con.setAutoCommit(false);
        tx=new Transacciones(() -> (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    Savepoint inicio;
                    public Object invoke(Object proxy, Method metodo, Object[] args) throws Throwable {
                        switch (metodo.getName()) {
                            case "setAutoCommit": inicio=con.setSavepoint(); return null;
                            case "commit", "close": return null;
                            case "rollback": con.rollback(inicio); return null;
                            default:
                                try { return metodo.invoke(con,args); }
                                catch (InvocationTargetException e) { throw e.getCause(); }
                        }
                    }
                }));
        reloj=new RelojPrueba(); dao=new CuentaDao(); auth=new Autenticacion(tx,dao,reloj);
        clave="Prueba7"+UUID.randomUUID().toString().substring(0,12);
        String hash=Contrasenas.cifrar(clave);
        admin=insertar("ADMINISTRADOR",null,hash,false);
        try (var ps=con.prepareStatement("INSERT INTO empresa (id_administrador,nombre,nit) VALUES (?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,admin); ps.setString(2,"Empresa de prueba sin confirmar"); ps.setString(3,UUID.randomUUID().toString().substring(0,20)); ps.executeUpdate();
            try (var rs=ps.getGeneratedKeys()) { rs.next(); empresa=rs.getInt(1); }
        }
        usuario=insertar("USUARIO",empresa,hash,true);
    }

    protected int insertar(String rol, Integer empresa, String hash, boolean temporal) throws SQLException {
        try (var ps=con.prepareStatement("INSERT INTO cuenta (id_empresa,nombre,correo,telefono,cargo,contrasena_hash,contrasena_temporal,rol) VALUES (?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1,empresa); ps.setString(2,"Prueba <script>sin ejecutar</script>");
            ps.setString(3,UUID.randomUUID()+"@prueba.invalid"); ps.setString(4,"123"); ps.setString(5,"Pruebas");
            ps.setString(6,hash); ps.setBoolean(7,temporal); ps.setString(8,rol); ps.executeUpdate();
            try (var rs=ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Crea un salón dentro de la empresa fixture, sin confirmar sus datos. */
    protected co.aplomo.modelo.Salon crearSalon() throws Exception {
        var empresas = new co.aplomo.servicio.Empresas(tx);
        int sede = empresas.guardar(cuenta(admin), new co.aplomo.modelo.Sede(0, empresa,
                "Sede de prueba", "Calle 1", "Armenia", "123", "8 a 5", "Sede", true)).id();
        int salon = empresas.guardar(cuenta(admin), new co.aplomo.modelo.Salon(0, sede,
                "Salón de prueba", "Salón", 1, 20, true)).id();
        return empresas.salon(cuenta(admin), salon);
    }

    /** Construye un horario válido con la misma fecha controlada de la prueba. */
    protected co.aplomo.servicio.Reservas.Datos solicitud(co.aplomo.modelo.Salon salon) {
        return new co.aplomo.servicio.Reservas.Datos(empresa, salon.sede(), salon.id(),
                java.time.LocalDate.now(reloj), java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0),
                "EVALUACION_INICIAL", "Descripción", "Primera línea\nSegunda línea");
    }

    protected Cuenta cuenta(int id) throws SQLException { return dao.porId(con,id,false); }

    @AfterEach
    void revertir() throws Exception {
        if (con!=null) { try { con.rollback(); } finally { con.close(); } }
    }
}
