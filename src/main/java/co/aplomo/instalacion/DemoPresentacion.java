package co.aplomo.instalacion;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.VisitaDao;
import co.aplomo.db.Sql;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Visitas;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Paso 26: mundo ficticio coherente; los puntajes se obtienen de los servicios RULA existentes.
 */
final class DemoPresentacion {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private DemoPresentacion() {
    }

    static List<Integer> empresas(Connection conexion, int administrador) throws SQLException {
        String[] nombres = {"Ensambles del Quindío S.A.S.", "Muebles Cafeteros S.A.S.", "Corporación Universitaria " +
                "Empresarial Alexander von Humboldt", "Textiles del Eje", "Café de Origen Calarcá",
                "Lácteos La " + "Sabana", "Confecciones Armenia"};
        List<Integer> empresas = new ArrayList<>();
        for (int indice = 0; indice < nombres.length; indice++) {
            String nit = DemoIdentidades.NITS.get(indice);
            Integer empresa = Sql.uno(conexion, "SELECT id_empresa FROM empresa WHERE nit=?", fila -> fila.getInt(1),
                    nit);
            if (empresa == null) empresa = Sql.insertar(conexion,
                    "INSERT INTO empresa(id_administrador,nombre,nit,descripcion,telefono,correo," +
                            "persona_contacto) " + "VALUES(?,?,?,?,?,?,?)", administrador, nombres[indice], nit,
                    "Demostración ALINEO. Información ficticia.", "0000000000", "demo@ejemplo.invalid",
                    "Contacto ficticio");
            empresas.add(empresa);
            estructura(conexion, empresa, DemoIdentidades.SEDES.get(indice), indice);
        }
        actualizarMarca(conexion);
        return List.copyOf(empresas);
    }

    private static void estructura(Connection conexion, int empresa, String nombreSede,
            int indice) throws SQLException {
        Integer sede = Sql.uno(conexion, "SELECT id_sede FROM sede WHERE id_empresa=? AND nombre=?",
                fila -> fila.getInt(1), empresa, nombreSede);
        if (sede == null) sede = Sql.insertar(conexion,
                "INSERT INTO sede(id_empresa,nombre,direccion,ciudad,telefono,horario,descrip" +
                        "cion) VALUES(?,?,?,?,?,?,?)", empresa, nombreSede, "Dirección ficticia", "Armenia",
                "0000000000", "8:00 a 16:00", "Demo ALINEO.");
        int area = catalogo(conexion, "area", empresa, "Operaciones");
        int cargo = catalogo(conexion, "cargo", empresa, "Operario de producción");
        String[] salones = {DemoIdentidades.PLANTA.get(indice), DemoIdentidades.OFICINA.get(indice)};
        for (int tipo = 0; tipo < salones.length; tipo++) {
            Integer salon = Sql.uno(conexion, "SELECT id_salon FROM salon WHERE id_sede=? AND nombre=?",
                    fila -> fila.getInt(1), sede, salones[tipo]);
            if (salon == null) salon = Sql.insertar(conexion,
                    "INSERT INTO salon(id_sede,nombre,descripcion,piso,capacidad_puestos) VALUES(" + "?,?,?,?,?)", sede,
                    salones[tipo], indice == 2 &&
                            tipo == 0 ? "Se exige calzado antideslizante" : "Espacio ficticio para la presentación", 1,
                    8);
            if (Sql.<Integer>uno(conexion, "SELECT COUNT(*) FROM puesto_trabajo WHERE id_salon=?",
                    fila -> fila.getInt(1), salon) == 0) {
                Sql.insertar(conexion, "INSERT INTO puesto_trabajo(id_salon,id_area,id_cargo,numero,descripcion,tipo" +
                                ",ubicacion) " + "VALUES(?,?,?,?,?,?,?)", salon, area, cargo, 1,
                        tipo == 0 ? "Manipulación de piezas de demostración" : "Revisión de documentos",
                        tipo == 0 ? "MESA" : "ESCRITORIO", "Demostración ALINEO.");
            }
        }
    }

    private static int catalogo(Connection conexion, String tabla, int empresa, String nombre) throws SQLException {
        if (!List.of("area", "cargo").contains(tabla)) throw new IllegalArgumentException("Catálogo inválido.");
        Integer existente = Sql.uno(conexion,
                "SELECT id_" + tabla + " FROM " + tabla + " WHERE id_empresa=? AND nombre=?", fila -> fila.getInt(1),
                empresa, nombre);
        return existente != null ? existente : Sql.insertar(conexion, "INSERT INTO " + tabla + (tabla.equals(
                        "cargo") ? "(id_empresa,nombre,tipo) VALUES(?,?,?)" : "(id_empresa,nombre) VALUES(?,?)"),
                tabla.equals("cargo") ? new Object[]{empresa, nombre, "OPERATIVO"} : new Object[]{empresa, nombre});
    }

    static void citas(Connection conexion, Path fotos, int administrador, List<Integer> empresas,
            List<Integer> evaluadores, LocalDate presentacion) throws SQLException, IOException {
        LocalDate lunes = presentacion.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int[][] asignaciones = {{2, 1}, {0, 3}, {4, 5}};
        for (int persona = 0; persona < evaluadores.size(); persona++) {
            for (int semana = -4; semana <= 3; semana++) {
                for (int turno = 0; turno < 2; turno++) {
                    LocalDate fecha = lunes.plusWeeks(semana).plusDays(turno == 0 ? 0 : 2);
                    int empresa = empresas.get(asignaciones[persona][turno]);
                    String codigo =
                            "D26" + presentacion.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")) +
                                    persona + (semana + 4) + turno;
                    cita(conexion, fotos, administrador, evaluadores.get(persona), empresa, fecha,
                            LocalTime.of(9 + persona * 2, 0), codigo, fecha.isBefore(presentacion),
                            persona == 1 && semana == 1 && turno == 0);
                }
            }
        }
        cita(conexion, fotos, administrador, evaluadores.get(0), empresas.get(2), presentacion, LocalTime.of(9, 0),
                "V26" + presentacion.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE), false, false);
        for (int persona = 1; persona < evaluadores.size(); persona++) {
            cita(conexion, fotos, administrador, evaluadores.get(persona), empresas.get(persona == 1 ? 0 : 4),
                    presentacion, LocalTime.of(9, 0),
                    "V27" + persona + presentacion.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE), false,
                    false);
        }
        historialInactivo(conexion, fotos, administrador, empresas.get(6), evaluadores.get(1));
        Sql.actualizar(conexion, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                        "JOIN empresa e USING(id_empresa) SET r.nota_administrador=?,r.nota_vista=TRUE " +
                        "WHERE e.nit='900.512.384-1' AND r.codigo=? AND r.nota_administrador IS NULL",
                "Revisa los apoyos durante la visita.",
                "D26" + presentacion.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")) + "140");
    }

    private static void cita(Connection conexion, Path fotos, int administrador, int evaluador, int empresa,
            LocalDate fecha, LocalTime hora, String codigo, boolean pasada,
            boolean cancelada) throws SQLException, IOException {
        Integer existe = Sql.uno(conexion, "SELECT id_reserva FROM reserva WHERE codigo=?", fila -> fila.getInt(1),
                codigo);
        if (existe != null) return;
        int salon = Sql.uno(conexion,
                "SELECT l.id_salon FROM salon l JOIN sede s USING(id_sede) WHERE s.id_empresa=? " +
                        "AND l.activo=TRUE AND EXISTS(SELECT 1 FROM puesto_trabajo p " +
                        "WHERE p.id_salon=l.id_salon AND p.activo=TRUE) " +
                        "ORDER BY (l.nombre='Taller de diseño') DESC,l.id_salon DESC LIMIT 1", fila -> fila.getInt(1),
                empresa);
        int reserva = Sql.insertar(conexion,
                "INSERT INTO reserva(id_salon,id_administrador,codigo,fecha,hora_inicio,hora_" +
                        "fin,categoria,descripcion,estado) " + "VALUES(?,?,?,?,?,?,?,?,?)", salon, evaluador, codigo,
                fecha, hora, hora.plusHours(1), "EVALUACION_INICIAL", "Demostración ALINEO.", "EN_COLA");
        if (cancelada) {
            Sql.actualizar(conexion, "UPDATE reserva SET estado='CANCELADA',motivo_cancelacion=? WHERE id_reserva=?",
                    "Cambio de turno ficticio", reserva);
        } else if (pasada) {
            finalizar(conexion, fotos, evaluador, reserva, salon, fecha, hora);
        }
    }

    private static void finalizar(Connection conexion, Path fotos, int evaluador, int reserva, int salon,
            LocalDate fecha, LocalTime hora) throws SQLException, IOException {
        CuentaDao cuentas = new CuentaDao();
        Cuenta original = cuentas.porId(conexion, evaluador, false);
        cuentas.contrasena(conexion, evaluador, original.hash(), false);
        Clock reloj = Clock.fixed(fecha.atTime(hora.plusMinutes(50)).atZone(ZONA).toInstant(), ZONA);
        int visita = new VisitaDao().iniciar(conexion, reserva, fecha.atTime(hora));
        Transacciones transacciones = new Transacciones(() -> DemoCompleta.sinConfirmar(conexion));
        try {
            Cuenta actor = cuentas.porId(conexion, evaluador, false);
            Evaluaciones servicio = new Evaluaciones(transacciones, reloj, new Fotos(fotos), new BitacoraDao());
            List<Integer> puestos = Sql.listar(conexion,
                    "SELECT id_puesto FROM puesto_trabajo WHERE id_salon=? AND activo=TRUE ORDER " + "BY numero",
                    fila -> fila.getInt(1), salon);
            byte[] imagen;
            try (var entrada = DemoPresentacion.class.getResourceAsStream(
                    "/static/img/demo/08_laboratorio_buena_postura_perfil.jpg")) {
                if (entrada == null) throw new IOException("Falta la foto de muestra.");
                imagen = entrada.readAllBytes();
            }
            for (int indice = 0; indice < puestos.size(); indice++) {
                int caso = Math.floorMod(fecha.getDayOfYear() + indice, 3);
                servicio.guardar(actor, visita,
                        new Evaluaciones.Datos(puestos.get(indice), "Demostración ALINEO. · Postura revisada",
                                "MAS_INADECUADA", "Postura de demostración", 120, false, false, false, false, false,
                                false, "Datos ficticios; caso manual simulado.", true, false),
                        List.of(new Fotos.Entrada(imagen, "PERFIL", "Demostración: no es una valoración real.")),
                        List.of(new Analisis.Solicitud(0, DemoCompleta.entrada(caso))));
            }
            new Visitas(transacciones, reloj).finalizar(actor, visita, "Demostración ALINEO.");
        } finally {
            cuentas.contrasena(conexion, evaluador, original.hash(), original.temporal());
        }
    }

    private static void historialInactivo(Connection conexion, Path fotos, int administrador, int empresa,
            int evaluador) throws SQLException, IOException {
        ajustarHistorial(conexion, administrador, empresa);
        int existentes = Sql.uno(conexion, "SELECT COUNT(*) FROM reserva WHERE codigo LIKE 'A26-2025-%'",
                fila -> fila.getInt(1));
        if (existentes == 3) return;

        // La asignación solo se usa dentro de esta transacción para crear el historial de 2025.
        Sql.actualizar(conexion, "UPDATE empresa SET activo=TRUE WHERE id_empresa=?", empresa);
        var asignadas = new co.aplomo.dao.CuentaEmpresaDao().ids(conexion, evaluador);
        List<Integer> temporal = new ArrayList<>(asignadas);
        temporal.add(empresa);
        new co.aplomo.dao.CuentaEmpresaDao().sincronizar(conexion, administrador, evaluador, temporal, false);
        for (int indice = 0; indice < 3; indice++) {
            cita(conexion, fotos, administrador, evaluador, empresa, diaHabil(LocalDate.of(2025, 8 + indice, 12)),
                    LocalTime.of(10, 0), "A26-2025-" + indice, true, false);
        }
        new co.aplomo.dao.CuentaEmpresaDao().sincronizar(conexion, administrador, evaluador, asignadas, false);
        Sql.actualizar(conexion, "UPDATE empresa SET activo=FALSE WHERE id_empresa=?", empresa);
    }

    private static LocalDate diaHabil(LocalDate fecha) {
        while (fecha.getDayOfWeek().getValue() > 5) fecha = fecha.plusDays(1);
        return fecha;
    }

    private static void ajustarHistorial(Connection conexion, int administrador, int empresa) throws SQLException {
        var citas = Sql.listar(conexion, "SELECT r.id_reserva,r.fecha FROM reserva r JOIN salon l USING(id_salon) " +
                        "JOIN sede s USING(id_sede) WHERE s.id_empresa=? AND r.codigo LIKE 'A26-2025-%' " +
                        "AND WEEKDAY(r.fecha)>4", fila -> java.util.Map.entry(fila.getInt(1),
                        fila.getDate(2).toLocalDate()),
                empresa);
        for (var cita : citas) {
            Sql.actualizar(conexion, "UPDATE reserva SET fecha=? WHERE id_reserva=?", diaHabil(cita.getValue()),
                    cita.getKey());
            Sql.actualizar(conexion, "UPDATE visita v JOIN reserva r USING(id_reserva) " +
                    "SET v.hora_real_inicio=TIMESTAMP(r.fecha,TIME(v.hora_real_inicio))," +
                    "v.hora_real_fin=TIMESTAMP(r.fecha,TIME(v.hora_real_fin)) WHERE r.id_reserva=?", cita.getKey());
            new BitacoraDao().registrar(conexion, administrador, java.time.LocalDateTime.now(ZONA), "reserva",
                    cita.getKey(), "EDITAR", null, null, "Demo: historial trasladado a día hábil");
        }
    }

    static void actualizarMarca(Connection conexion) throws SQLException {
        Sql.actualizar(conexion, "UPDATE empresa SET descripcion=REPLACE(descripcion,'APLOMO','ALINEO') " +
                "WHERE nit IN ('900.512.384-1','901.638.205-4','012345','900.782.156-2'," +
                "'901.428.603-7','900.315.928-5','901.267.840-3')");
        for (String tabla : List.of("sede", "salon", "puesto_trabajo", "visita", "evaluacion")) {
            String union = switch (tabla) {
                case "sede" -> "sede t JOIN empresa e USING(id_empresa)";
                case "salon" -> "salon t JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa)";
                case "puesto_trabajo" -> "puesto_trabajo t JOIN salon l USING(id_salon) " +
                        "JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa)";
                case "visita" -> "visita t JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) " +
                        "JOIN sede s USING(id_sede) JOIN empresa e USING(id_empresa)";
                default -> "evaluacion t JOIN visita v USING(id_visita) JOIN reserva r USING(id_reserva) " +
                        "JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa e USING" + "(id_empresa)";
            };
            String campos = switch (tabla) {
                case "sede", "salon" -> "t.nombre=REPLACE(t.nombre,'APLOMO','ALINEO')," +
                        "t.descripcion=REPLACE(t.descripcion,'APLOMO','ALINEO')";
                case "puesto_trabajo" -> "t.descripcion=REPLACE(t.descripcion,'APLOMO','ALINEO')";
                case "visita" -> "t.notas=REPLACE(t.notas,'APLOMO','ALINEO')";
                default -> "t.postura=REPLACE(t.postura,'APLOMO','ALINEO')," +
                        "t.motivo=REPLACE(t.motivo,'APLOMO','ALINEO')";
            };
            Sql.actualizar(conexion, "UPDATE " + union + " SET " + campos +
                    " WHERE e.nit IN ('900.512.384-1','901.638.205-4','012345','900.782.156-2','901.428.603-7','900" +
                    ".315.928-5','901.267.840-3')");
        }
        Sql.actualizar(conexion, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                "JOIN empresa e USING(id_empresa) SET r.descripcion=REPLACE(r.descripcion,'AP" + "LOMO','ALINEO')," +
                "r.notas=REPLACE(r.notas,'APLOMO','ALINEO') WHERE e.nit IN ('900.512.384-1','901.638.205-4','012345'," +
                "'900.782.156-2','901.428.603-7','900.315.928-5','901.267.840-3')");
    }
}
