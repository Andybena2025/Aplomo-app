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

    /** Inserta una evaluación fixture sin archivos, siempre dentro de la transacción reversible. */
    protected int evaluacion(int visita, int salon, int numero) throws Exception {
        Integer puesto = Sql.uno(con, "SELECT id_puesto FROM puesto_trabajo WHERE id_salon = ? AND numero = ?",
                fila -> fila.getInt(1), salon, numero);
        if (puesto == null) puesto = new co.aplomo.servicio.Empresas(tx).guardar(cuenta(admin),
                new co.aplomo.modelo.Puesto(0, salon, null, null, numero, "Puesto de prueba", "ESCRITORIO", "Centro", null, true)).id();
        return Sql.insertar(con, "INSERT INTO evaluacion (id_visita,id_puesto,fecha,postura,criterio,motivo,minutos_jornada,registro_fotografico) VALUES (?,?,?,?,'MAS_INADECUADA',?,60,'SIN_REGISTRO_FOTOGRAFICO')",
                visita, puesto, java.time.LocalDate.now(reloj), "Postura <b>de prueba</b>", "Motivo de prueba");
    }

    /** Crea metadatos de foto ficticios; nunca escribe archivos ni confirma datos. */
    protected int foto(int evaluacion) throws SQLException {
        Sql.actualizar(con, "UPDATE evaluacion SET registro_fotografico = 'CON_FOTOS', persona_acepto = TRUE WHERE id_evaluacion = ?", evaluacion);
        return Sql.insertar(con, "INSERT INTO fotografia (id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,descripcion) VALUES (?,?,?,1,'PERFIL',?)",
                evaluacion, UUID.randomUUID() + ".png", "no-existe/prueba.png", "Metadatos de prueba sin archivo");
    }

    /** Guarda un resultado fixture para comprobar promedios, sin recalcular ni alterar tablas RULA. */
    protected int analisis(int foto, int puntaje) throws SQLException {
        int nivel = Sql.uno(con, "SELECT id_nivel FROM nivel_riesgo WHERE ? BETWEEN puntaje_minimo AND puntaje_maximo",
                fila -> fila.getInt(1), puntaje);
        return Sql.insertar(con, "INSERT INTO analisis_rula (id_foto,id_nivel,fecha_hora,lado,hombro_elevado,brazo_separado,brazo_apoyado,antebrazo_cruza_linea_media,muneca_desviada,giro_muneca,cuello_girado,cuello_inclinado,tronco_girado,tronco_inclinado,piernas_apoyadas,postura_estatica_o_repetitiva,carga,puntaje_a,puntaje_b,puntaje_final) VALUES (?,?,?,'DERECHO',FALSE,FALSE,FALSE,FALSE,FALSE,'MEDIO',FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,'MENOS_2KG_INTERMITENTE',1,1,?)",
                foto, nivel, java.time.LocalDateTime.now(reloj), puntaje);
    }

    protected Cuenta cuenta(int id) throws SQLException { return dao.porId(con,id,false); }

    @AfterEach
    void revertir() throws Exception {
        if (con!=null) { try { con.rollback(); } finally { con.close(); } }
    }
}
