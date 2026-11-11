package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.*;
import co.aplomo.seguridad.*;
import java.sql.*;
import java.util.*;

/** RF-004 a RF-010, RF-028: configuración de empresas con autorización y conservación del historial. */
public final class Empresas {
    private final Transacciones tx;
    private final EmpresaDao empresas = new EmpresaDao();
    private final SedeDao sedes = new SedeDao();
    private final SalonDao salones = new SalonDao();
    private final AreaDao areas = new AreaDao();
    private final CargoDao cargos = new CargoDao();
    private final PuestoDao puestos = new PuestoDao();

    /** Resultado persistido; un nombre similar avisa sin impedir una empresa con NIT diferente. */
    public record Guardado(int id, String aviso) {}

    /** Usa la conexión configurada de la aplicación. */
    public Empresas() { this(new Transacciones()); }

    /** Permite probar con una transacción revertida y la base MySQL real. */
    public Empresas(Transacciones tx) { this.tx = tx; }

    /** Lista todas las empresas para Administrador o únicamente la empresa del Usuario. */
    public List<Empresa> listar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> empresas.buscarTodos(con, 0, alcance(con, actor)));
    }

    /** Consulta una empresa sin revelar datos de otra empresa al Usuario. */
    public Empresa empresa(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(empresas.buscar(con, id, alcance(con, actor), false)));
    }

    /** Consulta una sede dentro de la empresa autorizada. */
    public Sede sede(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(sedes.buscar(con, id, alcance(con, actor), false)));
    }

    /** Consulta un salón dentro de la empresa autorizada. */
    public Salon salon(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(salones.buscar(con, id, alcance(con, actor), false)));
    }

    /** Consulta un área dentro de la empresa autorizada. */
    public Area area(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(areas.buscar(con, id, alcance(con, actor), false)));
    }

    /** Consulta un cargo dentro de la empresa autorizada. */
    public Cargo cargo(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(cargos.buscar(con, id, alcance(con, actor), false)));
    }

    /** Consulta un puesto dentro de la empresa autorizada. */
    public Puesto puesto(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(puestos.buscar(con, id, alcance(con, actor), false)));
    }

    /** Lista sedes, incluidas las inactivas para conservar el acceso a su historial. */
    public List<Sede> sedes(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(con -> {
            int filtro = alcance(con, actor);
            exigir(empresas.buscar(con, empresa, filtro, false));
            return sedes.buscarTodos(con, empresa, filtro);
        });
    }

    /** Lista los salones de una sede autorizada. */
    public List<Salon> salones(Cuenta actor, int sede) throws SQLException {
        return tx.ejecutar(con -> {
            int filtro = alcance(con, actor);
            exigir(sedes.buscar(con, sede, filtro, false));
            return salones.buscarTodos(con, sede, filtro);
        });
    }

    /** Lista las áreas de una empresa autorizada. */
    public List<Area> areas(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(con -> {
            int filtro = alcance(con, actor);
            exigir(empresas.buscar(con, empresa, filtro, false));
            return areas.buscarTodos(con, empresa, filtro);
        });
    }

    /** Lista los cargos de una empresa autorizada. */
    public List<Cargo> cargos(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(con -> {
            int filtro = alcance(con, actor);
            exigir(empresas.buscar(con, empresa, filtro, false));
            return cargos.buscarTodos(con, empresa, filtro);
        });
    }

    /** Lista los puestos de un salón autorizado. */
    public List<Puesto> puestos(Cuenta actor, int salon) throws SQLException {
        return tx.ejecutar(con -> {
            int filtro = alcance(con, actor);
            exigir(salones.buscar(con, salon, filtro, false));
            return puestos.buscarTodos(con, salon, filtro);
        });
    }

    /** Guarda una empresa; el NIT duplicado impide guardar y un nombre similar solo avisa. */
    public Guardado guardar(Cuenta actor, Empresa datos) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta administrador = administrador(con, actor);
            Empresa anterior = datos.id() == 0 ? null : exigir(empresas.buscar(con, datos.id(), 0, true));
            Empresa dato = new Empresa(datos.id(), anterior == null ? administrador.id() : anterior.administrador(),
                    Validacion.texto(datos.nombre(), "nombre", 120),
                    Validacion.texto(datos.nit(), "NIT", 20).replaceAll("\\s+", ""),
                    Validacion.opcional(datos.descripcion(), "descripción", 255),
                    Validacion.opcional(datos.telefono(), "teléfono", 20), Validacion.correo(datos.correo()),
                    Validacion.opcional(datos.contacto(), "persona de contacto", 100), anterior == null || anterior.activo());
            String aviso = null;
            for (Empresa otra : empresas.buscarTodos(con, 0, 0)) {
                if (otra.id() == dato.id()) continue;
                if (Validacion.nombre(otra.nit()).replaceAll("\\s+", "").equals(Validacion.nombre(dato.nit()))) {
                    throw new IllegalArgumentException("El NIT ya pertenece a " + otra.nombre() + (otra.nombre().endsWith(".") ? "" : ".") + " Edita o reactiva esa empresa.");
                }
                if (Validacion.nombreEmpresa(otra.nombre()).equals(Validacion.nombreEmpresa(dato.nombre()))) {
                    aviso = "Existe otra empresa con nombre similar: " + otra.nombre() + (otra.nombre().endsWith(".") ? "" : ".") + " Revisa que sean empresas diferentes.";
                }
            }
            try { return new Guardado(empresas.guardar(con, dato), aviso); }
            catch (SQLIntegrityConstraintViolationException e) {
                throw new IllegalArgumentException("Ese NIT ya está registrado. Revisa la empresa existente.");
            }
        });
    }

    /** Guarda una sede en su empresa y evita nombres repetidos dentro de ella. */
    public Guardado guardar(Cuenta actor, Sede datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Sede anterior = datos.id() == 0 ? null : exigir(sedes.buscar(con, datos.id(), 0, true));
            int empresa = anterior == null ? datos.empresa() : anterior.empresa();
            Empresa padre = exigir(empresas.buscar(con, empresa, 0, true));
            if (anterior == null) activo(padre.activo(), "la empresa");
            Sede dato = new Sede(datos.id(), empresa, Validacion.texto(datos.nombre(), "nombre", 100),
                    Validacion.texto(datos.direccion(), "dirección", 150), Validacion.texto(datos.ciudad(), "ciudad", 60),
                    Validacion.texto(datos.telefono(), "teléfono", 20), Validacion.texto(datos.horario(), "horario", 100),
                    Validacion.texto(datos.descripcion(), "descripción", 200), anterior == null || anterior.activo());
            for (Sede otra : sedes.buscarTodos(con, empresa, 0)) {
                duplicado(dato.id(), dato.nombre(), otra.id(), otra.nombre());
            }
            return guardarSede(con, dato);
        });
    }

    /** Guarda un salón y valida los límites de piso y capacidad establecidos por MySQL. */
    public Guardado guardar(Cuenta actor, Salon datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Salon anterior = datos.id() == 0 ? null : exigir(salones.buscar(con, datos.id(), 0, true));
            int sede = anterior == null ? datos.sede() : anterior.sede();
            Sede padre = exigir(sedes.buscar(con, sede, 0, true));
            Empresa empresa = exigir(empresas.buscar(con, padre.empresa(), 0, true));
            if (anterior == null) { activo(padre.activo(), "la sede"); activo(empresa.activo(), "la empresa"); }
            Salon dato = new Salon(datos.id(), sede, Validacion.texto(datos.nombre(), "nombre", 100),
                    Validacion.texto(datos.descripcion(), "descripción", 200),
                    Validacion.entero(Integer.toString(datos.piso()), "piso", -128, 127),
                    Validacion.entero(Integer.toString(datos.capacidad()), "capacidad de puestos", 1, 32767),
                    anterior == null || anterior.activo());
            for (Salon otro : salones.buscarTodos(con, sede, 0)) {
                duplicado(dato.id(), dato.nombre(), otro.id(), otro.nombre());
            }
            try { return new Guardado(salones.guardar(con, dato), null); }
            catch (SQLIntegrityConstraintViolationException e) { throw repetido(); }
        });
    }

    /** Guarda un área sin repetir el nombre dentro de la empresa. */
    public Guardado guardar(Cuenta actor, Area datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Area anterior = datos.id() == 0 ? null : exigir(areas.buscar(con, datos.id(), 0, true));
            int empresa = anterior == null ? datos.empresa() : anterior.empresa();
            Empresa padre = exigir(empresas.buscar(con, empresa, 0, true));
            if (anterior == null) activo(padre.activo(), "la empresa");
            Area dato = new Area(datos.id(), empresa, Validacion.texto(datos.nombre(), "nombre", 100),
                    anterior == null || anterior.activo());
            for (Area otra : areas.buscarTodos(con, empresa, 0)) {
                duplicado(dato.id(), dato.nombre(), otra.id(), otra.nombre());
            }
            try { return new Guardado(areas.guardar(con, dato), null); }
            catch (SQLIntegrityConstraintViolationException e) { throw repetido(); }
        });
    }

    /** Guarda un cargo con uno de los tres tipos aprobados. */
    public Guardado guardar(Cuenta actor, Cargo datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Cargo anterior = datos.id() == 0 ? null : exigir(cargos.buscar(con, datos.id(), 0, true));
            int empresa = anterior == null ? datos.empresa() : anterior.empresa();
            Empresa padre = exigir(empresas.buscar(con, empresa, 0, true));
            if (anterior == null) activo(padre.activo(), "la empresa");
            Cargo dato = new Cargo(datos.id(), empresa, Validacion.texto(datos.nombre(), "nombre", 100),
                    Validacion.opcion(datos.tipo(), "tipo de cargo", "OPERATIVO", "ADMINISTRATIVO", "OTRO"),
                    anterior == null || anterior.activo());
            for (Cargo otro : cargos.buscarTodos(con, empresa, 0)) {
                duplicado(dato.id(), dato.nombre(), otro.id(), otro.nombre());
            }
            try { return new Guardado(cargos.guardar(con, dato), null); }
            catch (SQLIntegrityConstraintViolationException e) { throw repetido(); }
        });
    }

    /** Guarda un puesto, conservando su puntaje y comprobando empresa y estado de sus relaciones. */
    public Guardado guardar(Cuenta actor, Puesto datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Puesto anterior = datos.id() == 0 ? null : exigir(puestos.buscar(con, datos.id(), 0, true));
            int salon = anterior == null ? datos.salon() : anterior.salon();
            Salon padre = exigir(salones.buscar(con, salon, 0, true));
            Sede sede = exigir(sedes.buscar(con, padre.sede(), 0, true));
            Empresa empresa = exigir(empresas.buscar(con, sede.empresa(), 0, true));
            if (anterior == null) {
                activo(padre.activo(), "el salón"); activo(sede.activo(), "la sede"); activo(empresa.activo(), "la empresa");
            }
            if (datos.area() != null) {
                Area area = exigir(areas.buscar(con, datos.area(), empresa.id(), true));
                if (anterior == null || !Objects.equals(anterior.area(), area.id())) activo(area.activo(), "el área");
            }
            if (datos.cargo() != null) {
                Cargo cargo = exigir(cargos.buscar(con, datos.cargo(), empresa.id(), true));
                if (anterior == null || !Objects.equals(anterior.cargo(), cargo.id())) activo(cargo.activo(), "el cargo");
            }
            Puesto dato = new Puesto(datos.id(), salon, datos.area(), datos.cargo(),
                    Validacion.entero(Integer.toString(datos.numero()), "número del puesto", 1, 32767),
                    Validacion.texto(datos.descripcion(), "descripción", 255),
                    Validacion.opcion(datos.tipo(), "tipo de puesto", "ESCRITORIO", "MESA", "MOSTRADOR", "OTRO"),
                    Validacion.texto(datos.ubicacion(), "ubicación", 120), anterior == null ? null : anterior.puntaje(),
                    anterior == null || anterior.activo());
            for (Puesto otro : puestos.buscarTodos(con, salon, 0)) {
                if (otro.id() != dato.id() && otro.numero() == dato.numero()) {
                    throw new IllegalArgumentException("El puesto " + otro.numero() + " ya existe en este salón. Edítalo o reactívalo.");
                }
            }
            try { return new Guardado(puestos.guardar(con, dato), null); }
            catch (SQLIntegrityConstraintViolationException e) { throw repetido(); }
        });
    }

    /** Cambia el estado de una empresa y conserva sus relaciones y su historial. */
    public void estadoEmpresa(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            administrador(con, actor);
            exigir(empresas.buscar(con, id, 0, true));
            empresas.estado(con, id, activo);
            return null;
        });
    }

    /** Cambia el estado de una sede sin borrar sus salones. */
    public void estadoSede(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            administrador(con, actor);
            exigir(sedes.buscar(con, id, 0, true));
            sedes.estado(con, id, activo);
            return null;
        });
    }

    /** Cambia el estado de un salón sin borrar sus puestos. */
    public void estadoSalon(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            administrador(con, actor);
            exigir(salones.buscar(con, id, 0, true));
            salones.estado(con, id, activo);
            return null;
        });
    }

    /** Cambia el estado de un área conservando las asignaciones existentes. */
    public void estadoArea(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            administrador(con, actor);
            exigir(areas.buscar(con, id, 0, true));
            areas.estado(con, id, activo);
            return null;
        });
    }

    /** Cambia el estado de un cargo conservando las asignaciones existentes. */
    public void estadoCargo(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            administrador(con, actor);
            exigir(cargos.buscar(con, id, 0, true));
            cargos.estado(con, id, activo);
            return null;
        });
    }

    /** Cambia el estado de un puesto y conserva su puntaje y evaluaciones. */
    public void estadoPuesto(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            administrador(con, actor);
            exigir(puestos.buscar(con, id, 0, true));
            puestos.estado(con, id, activo);
            return null;
        });
    }

    private Guardado guardarSede(Connection con, Sede dato) throws SQLException {
        try { return new Guardado(sedes.guardar(con, dato), null); }
        catch (SQLIntegrityConstraintViolationException e) { throw repetido(); }
    }

    private static int alcance(Connection con, Cuenta actor) throws SQLException {
        return AccesoDatos.empresaPermitida(AccesoDatos.vigente(con, actor));
    }

    private static Cuenta administrador(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        Permisos.administrador(actual);
        return actual;
    }

    private static <T> T exigir(T valor) {
        if (valor == null) throw new SecurityException("El registro no está disponible para tu cuenta.");
        return valor;
    }

    private static void activo(boolean activo, String nombre) {
        if (!activo) throw new IllegalArgumentException("Primero activa " + nombre + " para registrar o asignar datos nuevos.");
    }

    private static void duplicado(int id, String nombre, int otroId, String otroNombre) {
        if (id != otroId && Validacion.nombre(nombre).equals(Validacion.nombre(otroNombre))) {
            throw new IllegalArgumentException("Ya existe " + otroNombre + (otroNombre.endsWith(".") ? "" : ".") + " Edita o reactiva ese registro.");
        }
    }

    private static IllegalArgumentException repetido() {
        return new IllegalArgumentException("Ese registro ya existe. Revisa el listado y edítalo o reactívalo.");
    }
}
