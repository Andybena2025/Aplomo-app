package co.aplomo.instalacion;

import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * RNF-011: estructura explícita de la demo; nunca borra ni combina empresas por nombre.
 */
final class EstructuraDemo {
    private static final String[] NOMBRES = {"Ensambles del Quindío S.A.S.", "Muebles Cafeteros S.A.S.", "Corporación" +
            " Universitaria Empresarial Alexander von Humboldt", "Textiles del Eje", "Café de Origen Calarcá", "L" +
            "ácteos La Sabana", "Logística del Quindío"};
    private static final String[][] CARGOS = {{"Operario de ensamble", "Inspector de calidad"}, {"Operario de corte",
            "Lijador"}, {"Técnico de mantenimiento", "Auxiliar de laboratorio"}, {"Patronista", "Operaria de " +
            "confección"}, {"Recibidor de café", "Catador"}, {"Operario de envasado", "Auxiliar de calidad"}, {
        "Auxiliar de bodega", "Operario de despacho"}};
    private static final String[][] PUESTOS = {{"Banco de ensamble", "Mesa de revisión"}, {"Sierra de banco", "Banco " +
            "de lijado"}, {"Banco de herramientas", "Mesa de prácticas"}, {"Mesa de corte", "Máquina plana"}, {"B" +
            "áscula de piso", "Mesa de catación"}, {"Línea de llenado", "Mesa de muestras"}, {"Estantería alta",
            "Mesa de embalaje"}};

    private EstructuraDemo() {
    }

    static List<Integer> crear(Connection con, int admin) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        for (int indice = 0; indice < NOMBRES.length; indice++) {
            Integer id = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit=?", f -> f.getInt(1),
                    DemoIdentidades.NITS.get(indice));
            if (id == null) id = Sql.insertar(con, "INSERT INTO empresa " +
                            "(id_administrador,id_creador,nombre,nit,descripcion,sector,telefono,correo," +
                            "persona_contacto) " +
                            "VALUES(?,?,?,?,?,?,'0000000000','demo@ejemplo.invalid','Recepción')", admin, admin,
                    NOMBRES[indice], DemoIdentidades.NITS.get(indice), "Empresa de demostración",
                    DemoIdentidades.SECTORES.get(indice));
            ids.add(id);
            for (int sede = 0; sede < (indice < 3 ? 2 : 1); sede++) estructura(con, id, indice, sede);
        }
        return List.copyOf(ids);
    }

    private static void estructura(Connection con, int empresa, int indice, int orden) throws SQLException {
        String nombre = orden == 0 ? DemoIdentidades.SEDES.get(indice) :
                indice == 2 ? "Sede principal" : "Sede de coordinación";
        Integer sede = Sql.uno(con, "SELECT id_sede FROM sede WHERE id_empresa=? AND nombre=?", f -> f.getInt(1),
                empresa, nombre);
        if (sede != null) return;
        sede = Sql.insertar(con, "INSERT INTO sede(id_empresa,nombre,direccion,ciudad,descripcion,telefono,horario) " +
                        "VALUES(?,?,?,?,?,'0000000000','7:00 a 18:00')", empresa, nombre, "Zona industrial", "Armenia",
                "Espacio de trabajo");
        int salon = Sql.insertar(con, "INSERT INTO salon(id_sede,nombre,capacidad_puestos,descripcion,piso) " +
                "VALUES(?,?,4,'Zona de trabajo',1)", sede, DemoIdentidades.PLANTA.get(indice));
        int area = catalogo(con, "area", empresa, "Operaciones", false);
        for (int numero = 1; numero <= 4; numero++) {
            boolean oficina = numero > 2;
            String cargo = oficina ? (
                    numero == 3 ? "Auxiliar administrativo" : "Asistente de recepción") : CARGOS[indice][numero - 1];
            String descripcion = oficina ? (
                    numero == 3 ? "Escritorio con pantalla" : "Punto de atención") : PUESTOS[indice][numero - 1];
            int idCargo = catalogo(con, "cargo", empresa, cargo, oficina);
            Sql.insertar(con,
                    "INSERT INTO puesto_trabajo(id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion) " +
                            "VALUES(?,?,?,?,?,?,'Zona de trabajo')", salon, area, idCargo, numero, descripcion,
                    oficina ? "ESCRITORIO" : "MESA");
        }
    }

    private static int catalogo(Connection con, String tabla, int empresa, String nombre,
            boolean oficina) throws SQLException {
        Integer id = Sql.uno(con, "SELECT id_" + tabla + " FROM " + tabla + " WHERE id_empresa=? AND nombre=?",
                f -> f.getInt(1), empresa, nombre);
        if (id != null) return id;
        return tabla.equals("area") ? Sql.insertar(con, "INSERT INTO area(id_empresa,nombre) VALUES(?,?)", empresa,
                nombre) : Sql.insertar(con, "INSERT INTO cargo(id_empresa,nombre,tipo) VALUES(?,?,?)", empresa, nombre,
                oficina ? "ADMINISTRATIVO" : "OPERATIVO");
    }
}
