package co.aplomo.servicio;

import co.aplomo.dao.AreaDao;
import co.aplomo.dao.CargoDao;
import co.aplomo.dao.ConsultaDao;
import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.dao.EmpresaDao;
import co.aplomo.dao.PuestoDao;
import co.aplomo.dao.SalonDao;
import co.aplomo.dao.SedeDao;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Area;
import co.aplomo.modelo.Cargo;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Objects;

/**
 * RF-004 a RF-010, RF-028: configuración de empresas con autorización y conservación del historial.
 */
public final class Empresas {
    private final Transacciones tx;
    private final EmpresaDao empresas = new EmpresaDao();
    private final SedeDao sedes = new SedeDao();
    private final SalonDao salones = new SalonDao();
    private final AreaDao areas = new AreaDao();
    private final CargoDao cargos = new CargoDao();
    private final PuestoDao puestos = new PuestoDao();

    /**
     * Resultado persistido; un nombre similar avisa sin impedir una empresa con NIT diferente. RNF-011.
     */
    public record Guardado(int id, String aviso) {
    }

    /**
     * Usa la conexión configurada de la aplicación. RNF-011.
     */
    public Empresas() {
        this(new Transacciones());
    }

    /**
     * Permite probar con una transacción revertida y la base MySQL real. RNF-011.
     */
    public Empresas(Transacciones tx) {
        this.tx = tx;
    }

    /**
     * RNF-009, RF-028/024/020: datos y resultados históricos, solo lectura.
     */
    public record HistorialPuesto(Puesto puesto, Empresa empresa, Sede sede, Salon salon, Area area, Cargo cargo,
                                  List<ConsultaDao.Fila> evaluaciones) {
    }

    /**
     * RNF-009, RF-028/024: abre todas las evaluaciones del puesto, solo para Administrador vigente.
     */
    public HistorialPuesto historialPuesto(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            var elemento = exigir(puestos.buscar(con, id, alcance(con, actor), false));
            var elemento2 = exigir(salones.buscar(con, elemento.salon(), alcance(con, actor), false));
            var elemento3 = exigir(sedes.buscar(con, elemento2.sede(), alcance(con, actor), false));
            var empresa2 = exigir(empresas.buscar(con, elemento3.empresa(), alcance(con, actor), false));
            return new HistorialPuesto(elemento, empresa2, elemento3, elemento2,
                    elemento.area() == null ? null : areas.buscar(con, elemento.area(), alcance(con, actor), false),
                    elemento.cargo() == null ? null : cargos.buscar(con, elemento.cargo(), alcance(con, actor), false),
                    new ConsultaDao().historialPuesto(con, id));
        });
    }

    /**
     * RF-003: cuentas vinculadas, visibles solo al Administrador.
     */
    public List<Cuenta> vinculadas(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(conexion -> {
            Permisos.administrador(AccesoDatos.vigente(conexion, actor));
            exigir(empresas.buscar(conexion, empresa, 0, false));
            return new CuentaDao().listar(conexion).stream().filter(cuenta -> {
                try {
                    return new CuentaEmpresaDao().ids(conexion, cuenta.id()).contains(empresa);
                } catch (SQLException error) {
                    throw new IllegalStateException(error);
                }
            }).toList();
        });
    }

    /**
     * Lista todas las empresas para Administrador o únicamente la empresa del Usuario. RNF-011.
     */
    public List<Empresa> listar(Cuenta actor) throws SQLException {
        return tx.ejecutar(con -> empresas.buscarTodos(con, 0, alcance(con, actor)));
    }

    /**
     * Consulta una empresa sin revelar datos de otra empresa al Usuario. RNF-011.
     */
    public Empresa empresa(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(empresas.buscar(con, id, alcance(con, actor), false)));
    }

    /**
     * Consulta una sede dentro de la empresa autorizada. RNF-011.
     */
    public Sede sede(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(sedes.buscar(con, id, alcance(con, actor), false)));
    }

    /**
     * Consulta un salón dentro de la empresa autorizada. RNF-011.
     */
    public Salon salon(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(salones.buscar(con, id, alcance(con, actor), false)));
    }

    /**
     * Consulta un área dentro de la empresa autorizada. RNF-011.
     */
    public Area area(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(areas.buscar(con, id, alcance(con, actor), false)));
    }

    /**
     * Consulta un cargo dentro de la empresa autorizada. RNF-011.
     */
    public Cargo cargo(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(cargos.buscar(con, id, alcance(con, actor), false)));
    }

    /**
     * Consulta un puesto dentro de la empresa autorizada. RNF-011.
     */
    public Puesto puesto(Cuenta actor, int id) throws SQLException {
        return tx.ejecutar(con -> exigir(puestos.buscar(con, id, alcance(con, actor), false)));
    }

    /**
     * Lista sedes, incluidas las inactivas para conservar el acceso a su historial. RNF-011.
     */
    public List<Sede> sedes(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(con -> {
            var filtro = alcance(con, actor);
            exigir(empresas.buscar(con, empresa, filtro, false));
            return sedes.buscarTodos(con, empresa, filtro);
        });
    }

    /**
     * Lista los salones de una sede autorizada. RNF-011.
     */
    public List<Salon> salones(Cuenta actor, int sede) throws SQLException {
        return tx.ejecutar(con -> {
            var filtro = alcance(con, actor);
            exigir(sedes.buscar(con, sede, filtro, false));
            return salones.buscarTodos(con, sede, filtro);
        });
    }

    /**
     * Lista las áreas de una empresa autorizada. RNF-011.
     */
    public List<Area> areas(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(con -> {
            var filtro = alcance(con, actor);
            exigir(empresas.buscar(con, empresa, filtro, false));
            return areas.buscarTodos(con, empresa, filtro);
        });
    }

    /**
     * Lista los cargos de una empresa autorizada. RNF-011.
     */
    public List<Cargo> cargos(Cuenta actor, int empresa) throws SQLException {
        return tx.ejecutar(con -> {
            var filtro = alcance(con, actor);
            exigir(empresas.buscar(con, empresa, filtro, false));
            return cargos.buscarTodos(con, empresa, filtro);
        });
    }

    /**
     * Lista los puestos de un salón autorizado. RNF-011.
     */
    public List<Puesto> puestos(Cuenta actor, int salon) throws SQLException {
        return tx.ejecutar(con -> {
            var filtro = alcance(con, actor);
            exigir(salones.buscar(con, salon, filtro, false));
            return puestos.buscarTodos(con, salon, filtro);
        });
    }

    /**
     * Guarda una empresa; el NIT duplicado impide guardar y un nombre similar solo avisa. RNF-011.
     */
    public Guardado guardar(Cuenta actor, Empresa datos) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta administrador = administrador(con, actor);
            if (datos.id() == 0) Permisos.administrador(administrador);
            Empresa anterior =
                    datos.id() == 0 ? null : exigir(empresas.buscar(con, datos.id(), alcance(con, actor), true));
            Empresa dato = new Empresa(datos.id(), anterior == null ? administrador.id() : anterior.administrador(),
                    Validacion.texto(datos.nombre(), "nombre", 120),
                    Validacion.texto(datos.nit(), "NIT", 20).replaceAll("\\s+", ""),
                    Validacion.opcional(datos.descripcion(), "descripción", 255),
                    Validacion.opcional(datos.telefono(), "teléfono", 20), Validacion.correo(datos.correo()),
                    Validacion.opcional(datos.contacto(), "persona de contacto", 100),
                    anterior == null || anterior.activo());
            String aviso = null;
            for (Empresa otra : empresas.buscarTodos(con, 0, alcance(con, actor))) {
                if (otra.id() == dato.id()) continue;
                if (Validacion.nombre(otra.nit()).replaceAll("\\s+", "").equals(Validacion.nombre(dato.nit()))) {
                    throw new IllegalArgumentException(
                            "El NIT ya pertenece a " + otra.nombre() + (otra.nombre().endsWith(".") ? "" : ".") +
                                    " Edita o reactiva esa empresa.");
                }
                if (Validacion.nombreEmpresa(otra.nombre()).equals(Validacion.nombreEmpresa(dato.nombre()))) {
                    aviso = "Existe otra empresa con nombre similar: " + otra.nombre() +
                            (otra.nombre().endsWith(".") ? "" : ".") + " Revisa que sean empresas diferentes.";
                }
            }
            try {
                return new Guardado(empresas.guardar(con, dato), aviso);
            } catch (SQLIntegrityConstraintViolationException error) {
                throw new IllegalArgumentException("Ese NIT ya está registrado. Revisa la empresa existente.");
            }
        });
    }

    /**
     * Guarda una sede en su empresa y evita nombres repetidos dentro de ella. RNF-011.
     */
    public Guardado guardar(Cuenta actor, Sede datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Sede anterior = datos.id() == 0 ? null : exigir(sedes.buscar(con, datos.id(), alcance(con, actor), true));
            int empresa = anterior == null ? datos.empresa() : anterior.empresa();
            Empresa padre = exigir(empresas.buscar(con, empresa, alcance(con, actor), true));
            if (anterior == null) activo(padre.activo(), "la empresa");
            Sede dato = new Sede(datos.id(), empresa, Validacion.texto(datos.nombre(), "nombre", 100),
                    Validacion.texto(datos.direccion(), "dirección", 150),
                    Validacion.texto(datos.ciudad(), "ciudad", 60), Validacion.texto(datos.telefono(), "teléfono", 20),
                    Validacion.texto(datos.horario(), "horario", 100),
                    Validacion.texto(datos.descripcion(), "descripción", 200), anterior == null || anterior.activo());
            for (Sede otra : sedes.buscarTodos(con, empresa, alcance(con, actor))) {
                duplicado(dato.id(), dato.nombre(), otra.id(), otra.nombre());
            }
            return guardarSede(con, dato);
        });
    }

    /**
     * Guarda un salón y valida los límites de piso y capacidad establecidos por MySQL. RNF-011.
     */
    public Guardado guardar(Cuenta actor, Salon datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Salon anterior =
                    datos.id() == 0 ? null : exigir(salones.buscar(con, datos.id(), alcance(con, actor), true));
            int sede = anterior == null ? datos.sede() : anterior.sede();
            Sede padre = exigir(sedes.buscar(con, sede, alcance(con, actor), true));
            Empresa empresa = exigir(empresas.buscar(con, padre.empresa(), alcance(con, actor), true));
            if (anterior == null) {
                activo(padre.activo(), "la sede");
                activo(empresa.activo(), "la empresa");
            }
            Salon dato = new Salon(datos.id(), sede, Validacion.texto(datos.nombre(), "nombre", 100),
                    Validacion.texto(datos.descripcion(), "descripción", 200),
                    Validacion.entero(Integer.toString(datos.piso()), "piso", -128, 127),
                    Validacion.entero(Integer.toString(datos.capacidad()), "capacidad de puestos", 1, 32767),
                    anterior == null || anterior.activo());
            for (Salon otro : salones.buscarTodos(con, sede, alcance(con, actor))) {
                duplicado(dato.id(), dato.nombre(), otro.id(), otro.nombre());
            }
            try {
                return new Guardado(salones.guardar(con, dato), null);
            } catch (SQLIntegrityConstraintViolationException error) {
                throw repetido();
            }
        });
    }

    /**
     * Guarda un área sin repetir el nombre dentro de la empresa. RNF-011.
     */
    public Guardado guardar(Cuenta actor, Area datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Area anterior = datos.id() == 0 ? null : exigir(areas.buscar(con, datos.id(), alcance(con, actor), true));
            int empresa = anterior == null ? datos.empresa() : anterior.empresa();
            Empresa padre = exigir(empresas.buscar(con, empresa, alcance(con, actor), true));
            if (anterior == null) activo(padre.activo(), "la empresa");
            Area dato = new Area(datos.id(), empresa, Validacion.texto(datos.nombre(), "nombre", 100),
                    anterior == null || anterior.activo());
            for (Area otra : areas.buscarTodos(con, empresa, alcance(con, actor))) {
                duplicado(dato.id(), dato.nombre(), otra.id(), otra.nombre());
            }
            try {
                return new Guardado(areas.guardar(con, dato), null);
            } catch (SQLIntegrityConstraintViolationException error) {
                throw repetido();
            }
        });
    }

    /**
     * Guarda un cargo con uno de los tres tipos aprobados. RNF-011.
     */
    public Guardado guardar(Cuenta actor, Cargo datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Cargo anterior = datos.id() == 0 ? null : exigir(cargos.buscar(con, datos.id(), alcance(con, actor), true));
            int empresa = anterior == null ? datos.empresa() : anterior.empresa();
            Empresa padre = exigir(empresas.buscar(con, empresa, alcance(con, actor), true));
            if (anterior == null) activo(padre.activo(), "la empresa");
            Cargo dato = new Cargo(datos.id(), empresa, Validacion.texto(datos.nombre(), "nombre", 100),
                    Validacion.opcion(datos.tipo(), "tipo de cargo", "OPERATIVO", "ADMINISTRATIVO", "OTRO"),
                    anterior == null || anterior.activo());
            for (Cargo otro : cargos.buscarTodos(con, empresa, alcance(con, actor))) {
                duplicado(dato.id(), dato.nombre(), otro.id(), otro.nombre());
            }
            try {
                return new Guardado(cargos.guardar(con, dato), null);
            } catch (SQLIntegrityConstraintViolationException error) {
                throw repetido();
            }
        });
    }

    /**
     * Guarda un puesto, conservando su puntaje y comprobando empresa y estado de sus relaciones. RNF-011.
     */
    public Guardado guardar(Cuenta actor, Puesto datos) throws SQLException {
        return tx.ejecutar(con -> {
            administrador(con, actor);
            Puesto anterior =
                    datos.id() == 0 ? null : exigir(puestos.buscar(con, datos.id(), alcance(con, actor), true));
            int salon = anterior == null ? datos.salon() : anterior.salon();
            Salon padre = exigir(salones.buscar(con, salon, alcance(con, actor), true));
            Sede sede = exigir(sedes.buscar(con, padre.sede(), alcance(con, actor), true));
            Empresa empresa = exigir(empresas.buscar(con, sede.empresa(), alcance(con, actor), true));
            if (anterior == null) {
                activo(padre.activo(), "el salón");
                activo(sede.activo(), "la sede");
                activo(empresa.activo(), "la empresa");
            }
            if (datos.area() != null) {
                Area area = exigir(areas.buscar(con, datos.area(), empresa.id(), true));
                if (anterior == null || !Objects.equals(anterior.area(), area.id())) activo(area.activo(), "el área");
            }
            if (datos.cargo() != null) {
                Cargo cargo = exigir(cargos.buscar(con, datos.cargo(), empresa.id(), true));
                if (anterior == null || !Objects.equals(anterior.cargo(), cargo.id()))
                    activo(cargo.activo(), "el cargo");
            }
            Puesto dato = new Puesto(datos.id(), salon, datos.area(), datos.cargo(),
                    Validacion.entero(Integer.toString(datos.numero()), "número del puesto", 1, 32767),
                    Validacion.texto(datos.descripcion(), "descripción", 255),
                    Validacion.opcion(datos.tipo(), "tipo de puesto", "ESCRITORIO", "MESA", "MOSTRADOR", "OTRO"),
                    Validacion.texto(datos.ubicacion(), "ubicación", 120), anterior == null ? null : anterior.puntaje(),
                    anterior == null || anterior.activo());
            for (Puesto otro : puestos.buscarTodos(con, salon, alcance(con, actor))) {
                if (otro.id() != dato.id() && otro.numero() == dato.numero()) {
                    throw new IllegalArgumentException(
                            "El puesto " + otro.numero() + " ya existe en este salón. Edítalo o reactívalo.");
                }
            }
            try {
                return new Guardado(puestos.guardar(con, dato), null);
            } catch (SQLIntegrityConstraintViolationException error) {
                throw repetido();
            }
        });
    }

    /**
     * Cambia el estado de una empresa y conserva sus relaciones y su historial. RNF-011.
     */
    public void estadoEmpresa(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            Permisos.administrador(AccesoDatos.vigente(con, actor));
            exigir(empresas.buscar(con, id, 0, true));
            empresas.estado(con, id, activo);
            return null;
        });
    }

    /**
     * Cambia el estado de una sede sin borrar sus salones. RNF-011.
     */
    public void estadoSede(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            Permisos.administrador(AccesoDatos.vigente(con, actor));
            exigir(sedes.buscar(con, id, 0, true));
            sedes.estado(con, id, activo);
            return null;
        });
    }

    /**
     * Cambia el estado de un salón sin borrar sus puestos. RNF-011.
     */
    public void estadoSalon(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            Permisos.administrador(AccesoDatos.vigente(con, actor));
            exigir(salones.buscar(con, id, 0, true));
            salones.estado(con, id, activo);
            return null;
        });
    }

    /**
     * Cambia el estado de un área conservando las asignaciones existentes. RNF-011.
     */
    public void estadoArea(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            Permisos.administrador(AccesoDatos.vigente(con, actor));
            exigir(areas.buscar(con, id, 0, true));
            areas.estado(con, id, activo);
            return null;
        });
    }

    /**
     * Cambia el estado de un cargo conservando las asignaciones existentes. RNF-011.
     */
    public void estadoCargo(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            Permisos.administrador(AccesoDatos.vigente(con, actor));
            exigir(cargos.buscar(con, id, 0, true));
            cargos.estado(con, id, activo);
            return null;
        });
    }

    /**
     * Cambia el estado de un puesto y conserva su puntaje y evaluaciones. RNF-011.
     */
    public void estadoPuesto(Cuenta actor, int id, boolean activo) throws SQLException {
        tx.ejecutar(con -> {
            Permisos.administrador(AccesoDatos.vigente(con, actor));
            exigir(puestos.buscar(con, id, 0, true));
            puestos.estado(con, id, activo);
            return null;
        });
    }

    private Guardado guardarSede(Connection con, Sede dato) throws SQLException {
        try {
            return new Guardado(sedes.guardar(con, dato), null);
        } catch (SQLIntegrityConstraintViolationException error) {
            throw repetido();
        }
    }

    private static AccesoDatos.Alcance alcance(Connection con, Cuenta actor) throws SQLException {
        return AccesoDatos.empresaPermitida(AccesoDatos.vigente(con, actor));
    }

    private static Cuenta administrador(Connection con, Cuenta actor) throws SQLException {
        Cuenta actual = AccesoDatos.vigente(con, actor);
        Permisos.trabajo(actual);
        return actual;
    }

    private static <T> T exigir(T valor) {
        if (valor == null) throw new SecurityException("El registro no está disponible para tu cuenta.");
        return valor;
    }

    private static void activo(boolean activo, String nombre) {
        if (!activo)
            throw new IllegalArgumentException("Primero activa " + nombre + " para registrar o asignar datos nuevos.");
    }

    private static void duplicado(int id, String nombre, int otroId, String otroNombre) {
        if (id != otroId && Validacion.nombre(nombre).equals(Validacion.nombre(otroNombre))) {
            throw new IllegalArgumentException("Ya existe " + otroNombre + (otroNombre.endsWith(".") ? "" : ".") +
                    " Edita o reactiva ese registro.");
        }
    }

    private static IllegalArgumentException repetido() {
        return new IllegalArgumentException("Ese registro ya existe. Revisa el listado y edítalo o reactívalo.");
    }
}
