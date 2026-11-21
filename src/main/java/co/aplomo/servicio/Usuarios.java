package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.dao.EmpresaDao.Opcion;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import co.aplomo.seguridad.*;
import java.sql.*;
import java.util.*;

/** RF-003, RF-010: gestión de cuentas autorizada en el servidor y protegida por transacciones. */
public final class Usuarios {
    private final Transacciones tx;
    private final CuentaDao dao;
    private final EmpresaDao empresas;

    /** Datos editables; no incluye ID, estado, intentos ni hash (RF-003). */
    public record Datos(String nombre, String correo, String telefono, String cargo, Rol rol, Integer empresa, List<Integer> empresas, String colorAgenda) {
        /** RF-003: conserva clientes de asignación múltiple con color automático. */
        public Datos(String nombre, String correo, String telefono, String cargo, Rol rol,
                     Integer empresa, List<Integer> empresas) {
            this(nombre, correo, telefono, cargo, rol, empresa, empresas, null);
        }
        /** Compatibilidad de clientes de una sola empresa; autoriza mediante cuenta_empresa. */
        public Datos(String n,String c,String t,String cargo,Rol r,Integer empresa){this(n,c,t,cargo,r,empresa,empresa==null?List.of():List.of(empresa));}
        /** Valida y normaliza los campos también en el servidor (RNF-004). */
        public Datos {
            nombre=texto(nombre,100,"nombre"); correo=texto(correo,120,"correo").toLowerCase(Locale.ROOT);
            telefono=texto(telefono,20,"teléfono"); cargo=texto(cargo,80,"cargo");
            if(!correo.matches("[^\\s@<>\\\"]+@[^\\s@<>\\\"]+\\.[^\\s@<>\\\"]+")) throw new IllegalArgumentException("Escribe un correo válido.");
            if(rol==null) throw new IllegalArgumentException("Elige un rol válido.");
            colorAgenda = ColoresAgenda.validar(colorAgenda);
            empresas=empresas==null?List.of():List.copyOf(new TreeSet<>(empresas));
            if(empresas.stream().anyMatch(id->id==null||id<1))throw new IllegalArgumentException("Elige empresas válidas.");
            if(rol==Rol.USUARIO && empresas.isEmpty()) throw new IllegalArgumentException("Elige la empresa del Usuario.");
            if(rol==Rol.ADMINISTRADOR && !empresas.isEmpty()) throw new IllegalArgumentException("El Administrador es independiente y no se asigna a una empresa.");
        }
        private static String texto(String valor,int maximo,String campo) {
            String limpio=valor==null ? "" : valor.strip().replaceAll("\\s+"," ");
            if(limpio.isEmpty() || limpio.codePointCount(0,limpio.length())>maximo || limpio.indexOf('\0')>=0)
                throw new IllegalArgumentException("Revisa el campo "+campo+": es obligatorio y permite hasta "+maximo+" caracteres.");
            return limpio;
        }
    }

    /** Usa los DAO JDBC con la configuración existente (RNF-011). */
    public Usuarios() { this(new Transacciones(),new CuentaDao(),new EmpresaDao()); }
    /** Recibe conexiones y DAO para verificar operaciones sin confirmar datos de prueba (RNF-011). */
    public Usuarios(Transacciones tx, CuentaDao dao, EmpresaDao empresas) { this.tx=tx; this.dao=dao; this.empresas=empresas; }

    /** RF-001c/003: edición del perfil propio con la misma conexión. */
    public CuentaPropia perfil() {
        return new CuentaPropia(tx, java.time.Clock.system(co.aplomo.web.Html.ZONA));
    }

    /** Lista las cuentas solo tras comprobar el permiso actual en MySQL (RF-002, RF-003). */
    public List<Cuenta> listar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> { autorizar(con,actor); return dao.listar(con); });
    }

    /** Consulta una cuenta editable sin confiar en IDs enviados por el navegador (RF-003). */
    public Cuenta consultar(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> { autorizar(con,actor); return exigirCuenta(con,id,false); });
    }

    /** RF-003: empresas de cada cuenta; solo para la gestión del Administrador. */
    public Map<Integer,List<Integer>> asignaciones(Cuenta actor)throws SQLException{return tx.ejecutar(c->{autorizar(c,actor);return new CuentaEmpresaDao().todas(c);});}

    /** Lee el catálogo existente para asignar empresa al Usuario (RF-003). */
    public List<Opcion> empresas(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> { autorizar(con,actor); return empresas.listar(con); });
    }

    /** Crea una cuenta con clave temporal cifrada; los correos existentes no se reutilizan (RF-003, RF-010). */
    public int crear(Cuenta actor, Datos datos, String temporal) throws SQLException {
        Permisos.administrador(actor); String hash=Contrasenas.cifrar(temporal);
        return tx.ejecutar(con -> {
            dao.bloquearAdministradores(con); autorizar(con,actor);
            new CuentaEmpresaDao().validar(con,0,datos.rol(),datos.empresas(),true); duplicados(con,datos.correo(),0);
            try { int id=dao.crear(con,datos.nombre(),datos.correo(),datos.telefono(),datos.cargo(),datos.rol(),datos.empresa(),hash);new CuentaEmpresaDao().sincronizar(con,actor.id(),id,datos.empresas(),false);
                if(datos.rol()==Rol.EVALUADOR) {
                    ColoresAgenda.guardar(con,id,ColoresAgenda.elegir(con,id,datos.colorAgenda()));
                }
                return id; }
            catch(SQLIntegrityConstraintViolationException e) { throw new IllegalArgumentException("El correo ya está registrado. Revisa la cuenta existente."); }
        });
    }

    /** Edita datos y opcionalmente asigna una nueva contraseña temporal (RF-003, RF-001c). */
    public void editar(Cuenta actor,int id,Datos datos,String temporal) throws SQLException {
        Permisos.administrador(actor);
        String hash=temporal==null || temporal.isEmpty() ? null : Contrasenas.cifrar(temporal);
        tx.ejecutar(con -> {
            List<Cuenta> administradores=dao.bloquearAdministradores(con); autorizar(con,actor);
            Cuenta anterior=exigirCuenta(con,id,true);
            conservarAdministrador(anterior,datos.rol(),anterior.estado(),activos(administradores));
            new CuentaEmpresaDao().validar(con,id,datos.rol(),datos.empresas(),anterior.estado()!=Estado.INACTIVA); duplicados(con,datos.correo(),id);
            try { dao.editar(con,id,datos.nombre(),datos.correo(),datos.telefono(),datos.cargo(),datos.rol(),datos.empresa()); }
            catch(SQLIntegrityConstraintViolationException e) { throw new IllegalArgumentException("El correo ya está registrado. Revisa la cuenta existente."); }
            new CuentaEmpresaDao().sincronizar(con,actor.id(),id,datos.empresas(),anterior.rol()!=datos.rol());
            if(datos.rol()==Rol.EVALUADOR) {
                ColoresAgenda.guardar(con,id,ColoresAgenda.elegir(con,id,datos.colorAgenda()));
            }
            if(hash!=null) dao.contrasena(con,id,hash,true);
            return null;
        });
    }

    /** Activa o desactiva sin borrar y reinicia el bloqueo al activar (RF-003). */
    public void estado(Cuenta actor,int id,boolean activar) throws SQLException {
        Permisos.administrador(actor);
        tx.ejecutar(con -> {
            List<Cuenta> administradores=dao.bloquearAdministradores(con); autorizar(con,actor);
            Cuenta anterior=exigirCuenta(con,id,true);
            Estado nuevo=activar ? Estado.ACTIVA : Estado.INACTIVA;
            conservarAdministrador(anterior,anterior.rol(),nuevo,activos(administradores));
            if(activar)new CuentaEmpresaDao().validar(con,id,anterior.rol(),new CuentaEmpresaDao().ids(con,id),true);
            dao.acceso(con,id,0,null,nuevo); return null;
        });
    }

    /** Impide perder al último Administrador activo por desactivación o cambio de rol (RF-003). */
    public static void conservarAdministrador(Cuenta anterior,Rol nuevoRol,Estado nuevoEstado,long activos) {
        if(anterior.rol()==Rol.ADMINISTRADOR && anterior.estado()==Estado.ACTIVA
                && (nuevoRol!=Rol.ADMINISTRADOR || nuevoEstado!=Estado.ACTIVA) && activos<=1)
            throw new IllegalArgumentException("No puedes desactivar ni cambiar el rol del último Administrador activo.");
    }

    private static long activos(List<Cuenta> cuentas) { return cuentas.stream().filter(c->c.estado()==Estado.ACTIVA).count(); }

    private void autorizar(Connection con,Cuenta actor) throws SQLException {
        Permisos.administrador(actor);
        Cuenta actual=dao.porId(con,actor.id(),false); Permisos.administrador(actual);
        if(!actual.hash().equals(actor.hash())) throw new SecurityException("La sesión cambió. Vuelve a ingresar.");
    }

    private Cuenta exigirCuenta(Connection con,int id,boolean bloquear) throws SQLException {
        Cuenta c=dao.porId(con,id,bloquear);
        if(c==null) throw new IllegalArgumentException("La cuenta no existe. Vuelve a la lista de usuarios.");
        return c;
    }

    private void duplicados(Connection con,String correo,int mismoId) throws SQLException {
        Cuenta repetida=dao.porCorreo(con,correo);
        if(repetida!=null && repetida.id()!=mismoId) throw new IllegalArgumentException("El correo ya pertenece a "+repetida.nombre()+(repetida.nombre().endsWith(".") ? "" : ".")+" Edita o reactiva esa cuenta.");
    }
}
