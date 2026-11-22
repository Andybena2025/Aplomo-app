package co.aplomo.servicio;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.ReservaDao;
import co.aplomo.db.Transacciones;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Permisos;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * RF-033 (propuesta), RF-002, RNF-005: agenda y notas privadas con alcance renovado por petición.
 */
public final class Agenda {
    private final Transacciones tx;
    private final Clock reloj;
    private final co.aplomo.dao.AgendaDao dao;

    /**
     * Usa las mismas transacciones y fecha de negocio que Reservas. RNF-011.
     */
    public Agenda(Transacciones tx, Clock reloj) {
        this(tx, reloj, new co.aplomo.dao.AgendaDao());
    }

    /**
     * RNF-011: permite sustituir el acceso JDBC sin mezclarlo con el calendario.
     */
    public Agenda(Transacciones tx, Clock reloj, co.aplomo.dao.AgendaDao dao) {
        this.tx = tx;
        this.reloj = reloj;
        this.dao = dao;
    }

    /**
     * Evaluador en el filtro, incluidas cuentas inactivas. RNF-011.
     */
    public record Evaluador(int id, String nombre, boolean activo, String color) {
        /**
         * RNF-011: operación del componente.
         */
        public Evaluador(int id, String nombre, boolean activo) {
            this(id, nombre, activo, null);
        }
    }

    /**
     * Cita autorizada con su nota y visita; sin controles de modificación. RNF-011.
     */
    public record Cita(ReservaDao.Ficha ficha, Integer evaluador, String nombreEvaluador, Nota nota, boolean cruce,
                       String color, int nuevos) {
        /**
         * RF-033: compatibilidad de citas sin avisos nuevos.
         */
        public Cita(ReservaDao.Ficha ficha, Integer evaluador, String nombreEvaluador, Nota nota, boolean cruce,
                String color) {
            this(ficha, evaluador, nombreEvaluador, nota, cruce, color, 0);
        }

        /**
         * RNF-011: operación del componente.
         */
        public Cita(ReservaDao.Ficha ficha, Integer evaluador, String nombreEvaluador, Nota nota, boolean cruce) {
            this(ficha, evaluador, nombreEvaluador, nota, cruce, null);
        }
    }

    /**
     * Texto privado visible únicamente a Administrador y Evaluador asignado. RNF-011.
     */
    public record Nota(String texto, boolean vista) {
    }

    /**
     * Rango normalizado; la semana comienza el lunes. RNF-011.
     */
    public record Vista(boolean mes, LocalDate fecha, LocalDate desde, LocalDate hasta, List<Cita> citas,
                        List<Evaluador> evaluadores, String filtro, boolean canceladas) {
    }

    /**
     * Normaliza fechas y vistas inválidas al mes actual de Bogotá. RNF-011.
     */
    public Vista consultar(Cuenta actor, Map<String, String> parametros) throws SQLException {
        return tx.ejecutar(conexion -> {
            Cuenta cuenta = AccesoDatos.vigente(conexion, actor);
            Permisos.trabajo(cuenta);
            Periodo periodo = periodo(parametros);
            var evaluadores = dao.evaluadores(conexion);
            String filtro = filtro(cuenta, parametros, evaluadores);
            if (cuenta.rol() == Cuenta.Rol.EVALUADOR) filtro = Integer.toString(cuenta.id());
            var bases = new ReservaDao().listar(conexion, AccesoDatos.empresaPermitida(cuenta));
            var nuevos = new co.aplomo.dao.MensajeDao().listar(conexion, cuenta.id()).stream()
                    .filter(m -> !m.leido() && m.reserva() != null).collect(
                            java.util.stream.Collectors.groupingBy(co.aplomo.dao.MensajeDao.Mensaje::reserva,
                                    java.util.stream.Collectors.counting()));
            List<Cita> citas = citas(conexion, bases, periodo, filtro, evaluadores, nuevos);
            citas.sort(Comparator.comparing((Cita cita) -> cita.ficha().reserva().fecha())
                    .thenComparing(cita -> cita.ficha().reserva().inicio())
                    .thenComparingInt(cita -> cita.ficha().reserva().id()));
            return new Vista(periodo.mes(), periodo.fecha(), periodo.desde(), periodo.hasta(), List.copyOf(citas),
                    cuenta.rol() == Cuenta.Rol.ADMINISTRADOR ? evaluadores : List.of(), filtro, periodo.canceladas());
        });
    }

    private record Periodo(boolean mes, LocalDate fecha, LocalDate desde, LocalDate hasta, boolean canceladas) {
    }

    private Periodo periodo(Map<String, String> parametros) {
        LocalDate fecha = LocalDate.now(reloj);
        boolean mes = true;
        try {
            String texto3 = parametros.getOrDefault("vista", "mes");
            if (!Set.of("semana", "mes").contains(texto3)) throw new IllegalArgumentException();
            fecha = LocalDate.parse(parametros.getOrDefault("fecha", fecha.toString()));
            if (fecha.getYear() < 1900 || fecha.getYear() > 9998) throw new IllegalArgumentException();
            mes = texto3.equals("mes");
        } catch (RuntimeException ex) {
            fecha = LocalDate.now(reloj);
            mes = true;
        }
        LocalDate desde = mes ? fecha.withDayOfMonth(1) : fecha.with(
                java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Periodo(mes, fecha, desde, mes ? desde.plusMonths(1) : desde.plusWeeks(1),
                parametros.getOrDefault("canceladas", "").equals("1"));
    }

    private static String filtro(Cuenta cuenta, Map<String, String> parametros, List<Evaluador> evaluadores) {
        String filtro = parametros.getOrDefault("evaluador", "TODOS");
        if (cuenta.rol() != Cuenta.Rol.ADMINISTRADOR && parametros.containsKey("evaluador"))
            throw new SecurityException("Acceso denegado.");
        if (!filtro.equals("TODOS") && !filtro.equals("SIN_ASIGNAR") &&
                evaluadores.stream().noneMatch(evaluador -> Integer.toString(evaluador.id()).equals(filtro)))
            throw new IllegalArgumentException("Elige un evaluador de la lista.");
        return filtro;
    }

    private List<Cita> citas(Connection conexion2, List<ReservaDao.Ficha> bases, Periodo periodo, String filtro,
            List<Evaluador> evaluadores, Map<Integer, Long> nuevos) throws SQLException {
        List<Cita> citas = new ArrayList<>();
        for (var reserva2 : bases) {
            var res = reserva2.reserva();
            if (res.fecha().isBefore(periodo.desde()) || !res.fecha().isBefore(periodo.hasta()) ||
                    !periodo.canceladas() && res.estado().equals("CANCELADA")) continue;
            var responsable = dao.responsableReserva(conexion2, res.id());
            Integer id = responsable == null ? null : responsable.getKey();
            if (filtro.equals("SIN_ASIGNAR") && id != null ||
                    !Set.of("TODOS", "SIN_ASIGNAR").contains(filtro) && !Objects.equals(id, Integer.valueOf(filtro)))
                continue;
            citas.add(new Cita(reserva2, id, responsable == null ? "Sin evaluador asignado" : responsable.getValue(),
                    notaEn(conexion2, res.id()), false,
                    evaluadores.stream().filter(elemento2 -> Objects.equals(elemento2.id(), id)).map(Evaluador::color)
                            .filter(Objects::nonNull).findFirst().orElse(null),
                    Math.toIntExact(nuevos.getOrDefault(res.id(), 0L))));
        }
        return citas;
    }

    /**
     * Consulta la nota solo tras autorizar la reserva; el Usuario nunca la recibe. RNF-011.
     */
    public Nota nota(Cuenta actor, int reserva) throws SQLException {
        return tx.ejecutar(conexion2 -> {
            var cuenta2 = AccesoDatos.vigente(conexion2, actor);
            if (cuenta2.rol() == Cuenta.Rol.USUARIO) return null;
            Permisos.trabajo(cuenta2);
            var ficha = new ReservaDao().buscar(conexion2, reserva, AccesoDatos.empresaPermitida(cuenta2), false);
            if (ficha == null) throw new SecurityException("Acceso denegado.");
            if (cuenta2.rol() == Cuenta.Rol.EVALUADOR && !Objects.equals(ficha.reserva().evaluador(), cuenta2.id()))
                return null;
            return notaEn(conexion2, reserva);
        });
    }

    /**
     * RF-033: guarda nota privada y reinicia el acuse aun si su texto es igual. RNF-011.
     */
    public String guardarNota(Cuenta actor, int reserva, String texto) throws SQLException {
        return tx.ejecutar(con -> guardarNotaEn(con, AccesoDatos.vigente(con, actor), reserva, texto));
    }

    String guardarNotaEn(Connection con, Cuenta cuenta, int reserva, String texto) throws SQLException {
        Permisos.administrador(cuenta);
        var ficha = exigir(con, cuenta, reserva);
        String nota = Validacion.opcional(texto, "nota para el evaluador", 500);
        Cuenta destinatario = ficha.reserva().evaluador() == null ? null : new co.aplomo.dao.CuentaDao().porId(con,
                ficha.reserva().evaluador(), true);
        if (!nota.isBlank() && (destinatario == null || destinatario.rol() != Cuenta.Rol.EVALUADOR ||
                destinatario.estado() != Cuenta.Estado.ACTIVA)) {
            throw new IllegalArgumentException("Asigna un evaluador activo a la cita antes de enviarle un mensaje.");
        }
        dao.guardarNota(con, reserva, nota);
        if (destinatario != null && !nota.isBlank()) {
            int mensaje = new co.aplomo.dao.MensajeDao().enviar(con, reserva, cuenta.id(), destinatario.id(),
                    "NOTA_ADMIN", nota, LocalDateTime.now(reloj));
            new BitacoraDao().registrar(con, cuenta.id(), LocalDateTime.now(reloj), "mensaje", mensaje, "CREAR", null,
                    null, "Nota del Administrador");
        }
        new BitacoraDao().registrar(con, cuenta.id(), LocalDateTime.now(reloj), "reserva", reserva, "EDITAR", null,
                null, "Nota del Administrador");
        return destinatario == null ? "el evaluador" : destinatario.nombre();
    }

    /**
     * RF-033: datos y conversación de una cita autorizada; el Usuario y otro Evaluador no acceden.
     */
    public record Conversacion(ReservaDao.Ficha ficha, Cuenta evaluador, String areas,
                               List<co.aplomo.dao.MensajeDao.Mensaje> mensajes) {
    }

    /**
     * RF-033: consulta sin marcar leído; el acuse se realiza después de abrir con POST y CSRF.
     */
    public Conversacion conversacion(Cuenta actor, int reserva) throws SQLException {
        return tx.ejecutar(con -> {
            Cuenta cuenta = AccesoDatos.vigente(con, actor);
            Permisos.trabajo(cuenta);
            var ficha = exigir(con, cuenta, reserva);
            Cuenta evaluador = ficha.reserva().evaluador() == null ? null : new co.aplomo.dao.CuentaDao().porId(con,
                    ficha.reserva().evaluador(), false);
            String areas = String.join(" · ", co.aplomo.db.Sql.listar(con,
                    "SELECT DISTINCT a.nombre FROM puesto_trabajo p JOIN area a ON a.id_area=p.id_area " +
                            "WHERE p.id_salon=? AND a.id_empresa=? ORDER BY a.nombre", f -> f.getString(1),
                    ficha.reserva().salon(), ficha.empresa()));
            var mensajes = evaluador ==
                    null ? List.<co.aplomo.dao.MensajeDao.Mensaje>of() : new co.aplomo.dao.MensajeDao().conversacion(
                    con, reserva, cuenta.id(), evaluador.id());
            return new Conversacion(ficha, evaluador, areas, mensajes);
        });
    }

    /**
     * RF-033: el panel acusa solo mensajes dirigidos al actor, dentro de su cita vigente.
     */
    public void leerConversacion(Cuenta actor, int reserva) throws SQLException {
        tx.ejecutar(con -> {
            Cuenta cuenta = AccesoDatos.vigente(con, actor);
            Permisos.trabajo(cuenta);
            var ficha = exigir(con, cuenta, reserva);
            if (ficha.reserva().evaluador() != null) {
                var mensajes = new co.aplomo.dao.MensajeDao();
                for (var mensaje : mensajes.conversacion(con, reserva, cuenta.id(), ficha.reserva().evaluador())) {
                    if (mensaje.destinatario() == cuenta.id()) mensajes.entender(con, mensaje);
                }
            }
            return null;
        });
    }

    /**
     * RF-033: el Evaluador responde al último Administrador de la cita; no recibe destinatario del navegador.
     */
    public void responder(Cuenta actor, int reserva, String texto) throws SQLException {
        tx.ejecutar(con -> {
            responderEn(con, AccesoDatos.vigente(con, actor), reserva, texto);
            return null;
        });
    }

    void responderEn(Connection con, Cuenta cuenta, int reserva, String texto) throws SQLException {
        if (cuenta.rol() != Cuenta.Rol.EVALUADOR) throw new SecurityException("Acceso denegado.");
        exigir(con, cuenta, reserva);
        Integer destino = co.aplomo.db.Sql.uno(con,
                "SELECT m.id_remitente FROM mensaje m JOIN cuenta a ON a.id_cuenta=m.id_remitente " +
                        "WHERE m.id_reserva=? AND m.id_destinatario=? AND m.tipo='NOTA_ADMIN' " +
                        "AND a.rol='ADMINISTRADOR' ORDER BY m.creado DESC,m.id_mensaje DESC LIMIT 1", f -> f.getInt(1),
                reserva, cuenta.id());
        if (destino == null) destino = Avisos.principalEn(con).id();
        int id = new co.aplomo.dao.MensajeDao().enviar(con, reserva, cuenta.id(), destino, "RESPUESTA",
                Validacion.texto(texto, "mensaje", 500), LocalDateTime.now(reloj));
        new BitacoraDao().registrar(con, cuenta.id(), LocalDateTime.now(reloj), "mensaje", id, "CREAR", null, null,
                "Respuesta de la cita");
    }

    /**
     * Acuse con permiso de Evaluador asignado; el Administrador no lo marca por él. RNF-011.
     */
    public void entender(Cuenta actor, int reserva) throws SQLException {
        tx.ejecutar(conexion2 -> {
            var cuenta2 = AccesoDatos.vigente(conexion2, actor);
            if (cuenta2.rol() != Cuenta.Rol.EVALUADOR) throw new SecurityException("Acceso denegado.");
            exigir(conexion2, cuenta2, reserva);
            dao.entender(conexion2, reserva);
            co.aplomo.db.Sql.actualizar(conexion2,
                    "UPDATE mensaje SET leido=TRUE WHERE id_reserva=? AND id_destinatario=? AND tipo='NOTA_ADMIN'",
                    reserva, cuenta2.id());
            return null;
        });
    }

    /**
     * Cantidad y primera reserva con nota pendiente, solo dentro del alcance vigente. RNF-011.
     */
    public List<Integer> nuevas(Cuenta actor) throws SQLException {
        return tx.ejecutar(conexion2 -> {
            var cuenta2 = AccesoDatos.vigente(conexion2, actor);
            if (cuenta2.rol() != Cuenta.Rol.EVALUADOR) return List.of();
            var al = AccesoDatos.empresaPermitida(cuenta2);
            return dao.nuevas(conexion2, al);
        });
    }

    /**
     * RF-033: datos mínimos del aviso privado.
     */
    public record Aviso(int reserva, String fecha, String hora, String empresa, String texto) {
    }

    /**
     * RF-002/033: notas pendientes, incluidas las de otros meses, solo para su Evaluador.
     */
    public List<Aviso> avisos(Cuenta actor) throws SQLException {
        return tx.ejecutar(conexion -> {
            Cuenta cuenta = AccesoDatos.vigente(conexion, actor);
            if (cuenta.rol() != Cuenta.Rol.EVALUADOR) throw new SecurityException("Acceso denegado.");
            var alcance = AccesoDatos.empresaPermitida(cuenta);
            return dao.avisos(conexion, alcance);
        });
    }

    private static ReservaDao.Ficha exigir(Connection conexion2, Cuenta cuenta2, int id) throws SQLException {
        var ficha = new ReservaDao().buscar(conexion2, id, AccesoDatos.empresaPermitida(cuenta2), true);
        if (ficha == null ||
                cuenta2.rol() == Cuenta.Rol.EVALUADOR && !Objects.equals(ficha.reserva().evaluador(), cuenta2.id())) {
            throw new SecurityException("La reserva no está disponible para tu cuenta.");
        }
        return ficha;
    }

    private Nota notaEn(Connection conexion2, int id) throws SQLException {
        return dao.nota(conexion2, id);
    }
}
