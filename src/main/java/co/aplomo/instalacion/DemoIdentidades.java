package co.aplomo.instalacion;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Paso 27: identidades estables de la demo, sin confundir un NIT real con un registro ficticio.
 */
final class DemoIdentidades {
    static final List<String> NITS = List.of("900.512.384-1", "901.638.205-4", "012345", "900.782.156-2",
            "901.428.603-7", "900.315.928-5", "901.267.840-3");
    static final List<String> SECTORES = List.of("Manufactura", "Carpintería", "Educación", "Textil", "Alimentos",
            "Alimentos", "Confección");
    static final List<String> SEDES = List.of("Planta La Tebaida", "Planta Calarcá", "Sede Alcázar", "Planta Armenia",
            "Centro de acopio Calarcá", "Planta Circasia", "Taller Centro");
    static final List<String> PLANTA = List.of("Línea de ensamble", "Taller de corte", "Taller de diseño",
            "Taller de confección", "Bodega de café", "Laboratorio de calidad", "Taller de costura");
    static final List<String> OFICINA = List.of("Oficina de logística", "Oficina de compras", "Oficina de admisiones",
            "Sala de patronaje", "Control de compras", "Gestión de calidad", "Administración");

    private DemoIdentidades() {
    }

    static void preparar(Connection conexion, int administrador) throws SQLException {
        for (int indice = 0; indice < NITS.size(); indice++) {
            String anterior = "DEMO-APLOMO-E" + (indice + 1);
            String nuevo = NITS.get(indice);
            Integer legado = Sql.uno(conexion, "SELECT id_empresa FROM empresa WHERE nit=? FOR UPDATE",
                    fila -> fila.getInt(1), anterior);
            Integer existente = Sql.uno(conexion, "SELECT id_empresa FROM empresa WHERE nit=? FOR UPDATE",
                    fila -> fila.getInt(1), nuevo);
            if (existente != null && (legado != null || !identificada(conexion, existente, anterior))) {
                throw new IllegalArgumentException("Un NIT de la demo ya pertenece a otra empresa. No se cambió nada.");
            }
            if (legado != null) {
                marcar(conexion, administrador, legado, anterior);
                Sql.actualizar(conexion, "UPDATE empresa SET nit=? WHERE id_empresa=?", nuevo, legado);
            }
        }
        estructuraExistente(conexion);
    }

    private static void estructuraExistente(Connection conexion) throws SQLException {
        List<String> antiguos = List.of("Ensamble", "Carpintería", "Taller de mantenimiento", "Confección",
                "Bodega de café", "Laboratorio de calidad", "Taller histórico");
        for (int indice = 0; indice < NITS.size(); indice++) {
            Integer empresa = Sql.uno(conexion, "SELECT id_empresa FROM empresa WHERE nit=?", fila -> fila.getInt(1),
                    NITS.get(indice));
            if (empresa == null) continue;
            renombrar(conexion, "cargo", "id_empresa", empresa, "Operario de demostración", "Operario de producción");
            renombrar(conexion, "sede", "id_empresa", empresa, indice == 2 ? "Sede Alcázar" : "Sede de producción",
                    SEDES.get(indice));
            renombrar(conexion, "sede", "id_empresa", empresa, "Oficina administrativa de demostración",
                    indice == 0 ? "Sede de coordinación" : "Sede comercial Armenia");
            for (var lugar : Sql.listar(conexion, "SELECT id_sede,nombre FROM sede WHERE id_empresa=?",
                    fila -> java.util.Map.entry(fila.getInt(1), fila.getString(2)), empresa)) {
                int sede = lugar.getKey();
                String espacio = switch (indice) {
                    case 0 -> lugar.getValue()
                            .equals("Planta Armenia") ? "Centro de operaciones" : "Oficina de logística";
                    case 1 -> lugar.getValue().equals("Planta Dosquebradas") ? "Taller de corte" : "Ensamble";
                    case 2 -> lugar.getValue().equals("Sede principal") ? "Oficina de admisiones" : lugar.getValue()
                            .equals("Sede Alcázar") ? "Taller de diseño" : "Laboratorio de simulación";
                    default -> PLANTA.get(indice);
                };
                renombrar(conexion, "salon", "id_sede", sede, "DEMO-ALINEO · Oficina y planta", espacio);
                renombrar(conexion, "salon", "id_sede", sede, antiguos.get(indice), PLANTA.get(indice));
                renombrar(conexion, "salon", "id_sede", sede, "Oficina administrativa", OFICINA.get(indice));
                renombrar(conexion, "salon", "id_sede", sede, "Demostración de resultados", "Área de trabajo");
            }
        }
    }

    static String adaptar(String texto) {
        for (int indice = 0; indice < NITS.size(); indice++) {
            texto = texto.replace("DEMO-APLOMO-E" + (indice + 1), NITS.get(indice));
        }
        return texto;
    }

    static void exigirIdentidad(Connection conexion, List<Integer> empresas) throws SQLException {
        for (int empresa : empresas) {
            String nit = Sql.uno(conexion, "SELECT nit FROM empresa WHERE id_empresa=?", fila -> fila.getString(1),
                    empresa);
            int indice = NITS.indexOf(nit);
            if (indice < 0 || !identificada(conexion, empresa, "DEMO-APLOMO-E" + (indice + 1))) {
                throw new IllegalArgumentException("Una empresa no pertenece a la demo. No se borró nada.");
            }
        }
    }

    static void completar(Connection conexion, int administrador) throws SQLException {
        for (int indice = 0; indice < NITS.size(); indice++) {
            int empresa = Sql.uno(conexion, "SELECT id_empresa FROM empresa WHERE nit=?", fila -> fila.getInt(1),
                    NITS.get(indice));
            marcar(conexion, administrador, empresa, "DEMO-APLOMO-E" + (indice + 1));
            Sql.actualizar(conexion, "UPDATE empresa SET sector=?,id_creador=COALESCE(id_creador,id_administrador)," +
                            "descripcion=?,persona_contacto='Recepción' WHERE id_empresa=?", SECTORES.get(indice),
                    "Empresa del sector " + SECTORES.get(indice).toLowerCase(java.util.Locale.ROOT) + ".", empresa);
            Sql.actualizar(conexion, "UPDATE salon l JOIN sede s USING(id_sede) SET l.descripcion=CONCAT('Zona de '," +
                    "LOWER(l.nombre)) WHERE s.id_empresa=? AND (l.descripcion LIKE '%fictici%' " +
                    "OR l.descripcion LIKE '%Demostración%' OR l.descripcion LIKE '%Paso 17%')", empresa);
            Sql.actualizar(conexion, "UPDATE visita v JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) " +
                    "JOIN sede s USING(id_sede) SET v.notas='Revisión de las tareas del turno.' WHERE s.id_empresa=? " +
                    "AND (v.notas LIKE '%Demostración%' OR v.notas LIKE 'DEMO%')", empresa);
            Sql.actualizar(conexion, "UPDATE sede SET descripcion='Espacio de trabajo y coordinación'," +
                            "direccion=CASE WHEN direccion='Dirección ficticia' THEN 'Zona industrial' ELSE direccion" +
                            " END " +
                            "WHERE id_empresa=? AND (descripcion LIKE '%Demo ALINEO%' " + "OR descripcion LIKE " +
                            "'%fictici%')",
                    empresa);
            Sql.actualizar(conexion, "UPDATE evaluacion e JOIN visita v USING(id_visita) " +
                            "JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede)" +
                            " " +
                            "SET e.postura=REPLACE(REPLACE(e.postura,'DEMO17 · ',''),'Demostración ALINEO. · ','') " +
                            "WHERE s.id_empresa=? AND (e.postura LIKE 'DEMO17%' OR e.postura LIKE 'Demostración " +
                            "ALINEO.%')",
                    empresa);
            Sql.actualizar(conexion, "UPDATE pendiente_arreglo pa JOIN evaluacion e USING(id_evaluacion) " +
                    "JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) " +
                    "JOIN sede s USING(id_sede) SET pa.descripcion=REPLACE(pa.descripcion,'DEMO17 · ','') " +
                    "WHERE s.id_empresa=? AND pa.descripcion LIKE 'DEMO17%'", empresa);
            Sql.actualizar(conexion, "UPDATE sugerencia su JOIN evaluacion ev USING(id_evaluacion) " +
                    "JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) " +
                    "JOIN sede s USING(id_sede) SET su.titulo=REPLACE(su.titulo,'DEMO17 · ','') " +
                    "WHERE s.id_empresa=? AND su.titulo LIKE 'DEMO17%'", empresa);
            String nombreAnterior = indice == 2 ? "Sede Alcázar" : "Sede de producción";
            renombrar(conexion, "sede", "id_empresa", empresa, nombreAnterior, SEDES.get(indice));
            List<Integer> sedes = Sql.listar(conexion, "SELECT id_sede FROM sede WHERE id_empresa=?",
                    fila -> fila.getInt(1), empresa);
            for (int sede : sedes) {
                renombrar(conexion, "salon", "id_sede", sede, "Taller de mantenimiento", PLANTA.get(indice));
                renombrar(conexion, "salon", "id_sede", sede, "Oficina administrativa", OFICINA.get(indice));
                renombrar(conexion, "salon", "id_sede", sede, "Demostración de resultados", "Área de trabajo");
            }
            Sql.actualizar(conexion,
                    "UPDATE puesto_trabajo p JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                            "SET p.descripcion=CASE WHEN p.tipo='ESCRITORIO' THEN 'Trabajo en computador' " +
                            "ELSE ? END,p.ubicacion='Zona de trabajo' WHERE s.id_empresa=? AND " +
                            "(p.descripcion LIKE '%demostración%' OR p.descripcion LIKE 'DEMO%')",
                    indice == 1 ? "Corte de tablones" : indice == 4 ? "Levantamiento de sacos" : "Revisión de piezas",
                    empresa);
            Sql.actualizar(conexion, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                    "SET r.descripcion='Evaluación de las tareas del turno',r.notas=CASE " +
                    "WHEN r.notas LIKE '%fictici%' OR r.notas LIKE '%Demostración%' THEN NULL ELSE r.notas END " +
                    "WHERE s.id_empresa=? AND (r.descripcion LIKE '%Demostración%' OR r.descripcion LIKE 'DEMO%' " +
                    "OR r.descripcion LIKE '%fictici%')", empresa);
        }
        int humboldt = Sql.uno(conexion, "SELECT id_empresa FROM empresa WHERE nit='012345'", fila -> fila.getInt(1));
        Sql.actualizar(conexion,
                "UPDATE empresa SET telefono=?,correo=?,persona_contacto=?,descripcion=? " + "WHERE id_empresa=?",
                "606 745 0025", "recepcion@cue.edu.co", "Recepción",
                "Institución de educación superior en Armenia, Quindío. Sitio: unihumboldt.edu.co", humboldt);
        Sql.actualizar(conexion, "UPDATE sede SET direccion=?,ciudad='Armenia',telefono=? " +
                "WHERE id_empresa=? AND nombre='Sede principal'", "Av. Bolívar # 1-189", "606 745 0025", humboldt);
    }

    private static void renombrar(Connection conexion, String tabla, String padre, int id, String viejo,
            String nuevo) throws SQLException {
        if (viejo.equals(nuevo)) return;
        Sql.actualizar(conexion, "UPDATE " + tabla + " a LEFT JOIN " + tabla + " b ON b." + padre + "=a." + padre +
                        " AND b.nombre=? SET a.nombre=? WHERE a." + padre + "=? AND a.nombre=? " +
                        "AND b.nombre IS NULL",
                nuevo, nuevo, id, viejo);
    }

    private static boolean identificada(Connection conexion, int empresa, String clave) throws SQLException {
        int cantidad = Sql.uno(conexion,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='empresa' AND id_registro=? " + "AND detalle=?",
                fila -> fila.getInt(1), empresa, clave);
        return cantidad > 0;
    }

    private static void marcar(Connection conexion, int administrador, int empresa, String clave) throws SQLException {
        if (identificada(conexion, empresa, clave)) return;
        new BitacoraDao().registrar(conexion, administrador, LocalDateTime.now(ZoneId.of("America/Bogota")), "empresa",
                empresa, "EDITAR", null, null, clave);
    }
}
