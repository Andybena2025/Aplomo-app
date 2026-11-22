package co.aplomo.web;

import co.aplomo.dao.ConsultaDao.Fila;
import co.aplomo.dao.ReservaDao.Ficha;
import co.aplomo.servicio.Avisos.ContactoEquipo;
import co.aplomo.servicio.SolicitudesVisita.Ingeniero;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RF-013a/027/033: presentación del Inicio con datos previamente autorizados por los servicios.
 */
final class InicioUsuario {
    private static final Locale CO = new Locale("es", "CO");

    private InicioUsuario() {
    }

    static String rotulo(LocalDateTime ahora) {
        String saludo = ahora.getHour() < 12 ? "Buenos días" : ahora.getHour() < 19 ? "Buenas tardes" : "Buenas noches";
        return (ahora.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", CO)) + " · " + saludo)
                .toUpperCase(CO);
    }

    static String faltan(LocalDate hoy, LocalDate dia) {
        long dias = ChronoUnit.DAYS.between(hoy, dia);
        return dias == 0 ? "Hoy" : dias == 1 ? "Mañana" : "Faltan " + dias + " días";
    }

    static String fecha(LocalDate dia) {
        String texto = dia.format(DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", CO));
        return texto.substring(0, 1).toUpperCase(CO) + texto.substring(1);
    }

    static void proxima(List<Ficha> futuras, Map<Integer, Ingeniero> personas, LocalDate hoy, StringBuilder html) {
        Ficha cita = futuras.stream().filter(f -> List.of("EN_COLA", "ESPERANDO").contains(f.reserva().estado()))
                .findFirst().orElse(null);
        html.append(
                "<section class=\"card stack usuario-proxima\"><span class=\"cita-check\" " +
                        "aria-hidden=\"true\">✓</span>");
        if (cita == null) html.append("<h2>Aún no tienes visitas programadas</h2>");
        else {
            var reserva = cita.reserva();
            html.append("<h2>").append(reserva.estado().equals("ESPERANDO") ?
                            "Tu solicitud está esperando respuesta" : "Tu próxima visita está confirmada")
                    .append("</h2><p>").append(fecha(reserva.fecha())).append(" · ")
                    .append(reserva.inicio()).append(" a ").append(reserva.fin()).append(" · ")
                    .append(Html.escapar(cita.sedeNombre()));
            Ingeniero persona = personas.get(reserva.evaluador());
            if (persona != null) html.append(", con ").append(TextoLista.persona(persona.nombre(), persona.color()));
            html.append(". ").append(faltan(hoy, reserva.fecha())).append(".</p><p class=\"muted small\">")
                    .append("Para preparar la visita: que tu equipo trabaje como un día normal y que el " +
                            "área esté despejada.")
                    .append("</p>");
        }
        html.append(
                "<div class=\"row proxima-acciones\"><a class=\"btn p\" " +
                        "href=\"/solicitar-visita\">Solicitar una visita</a>");
        if (cita != null) {
            Ingeniero persona = personas.get(cita.reserva().evaluador());
            if (persona != null && persona.asignado()) html.append("<a class=\"btn\" href=\"/avisos/nuevo?para=")
                    .append(persona.id()).append("\">Escribir a ")
                    .append(Html.escapar(Html.nombreSinTitulo(persona.nombre()).split("\\s+")[0])).append("</a>");
            html.append("<a class=\"btn\" href=\"/reservas/").append(cita.reserva().id()).append("\">Ver detalles</a>");
        }
        html.append("</div></section>");
    }

    static void tendencia(List<Fila> visitas, List<Fila> evaluaciones, StringBuilder html) {
        List<Fila> ultimas = visitas.stream().filter(f -> f.valor("estado").equals("FINALIZADA"))
                .filter(f -> !f.valor("puntaje").isBlank())
                .sorted(Comparator.comparing((Fila f) -> f.valor("fecha")).thenComparingInt(Fila::id).reversed())
                .limit(4).sorted(
                        Comparator.comparing((Fila f) -> f.valor("fecha")).thenComparingInt(Fila::id)).toList();
        html.append("<section class=\"card stack equipo-tendencia\"><div class=\"row between\">")
                .append("<h2>Así ha mejorado tu equipo</h2>");
        if (ultimas.size() >= 2) {
            Fila primera = ultimas.get(0), ultima = ultimas.get(ultimas.size() - 1);
            int antes = Integer.parseInt(primera.valor("puntaje")), despues = Integer.parseInt(ultima.valor("puntaje"));
            if (antes != despues) html.append("<span class=\"tendencia-cambio\" style=\"color:var(--")
                    .append(despues < antes ? "verde" : "rojo").append(")\">")
                    .append(despues < antes ? "▲" : "▼").append(" De ").append(antes).append(" a ").append(despues)
                    .append(despues < antes ? periodo(primera, ultima) : "").append("</span>");
        }
        html.append("</div><div class=\"tendencia-visitas\">");
        for (int i = 0; i < ultimas.size(); i++) {
            Fila fila = ultimas.get(i);
            String color = fila.valor("color").toLowerCase(Locale.ROOT);
            Fila destino = evaluaciones.stream().filter(e -> e.valor("visita").equals(fila.valor("id")))
                    .filter(e -> e.valor("estado").equals("FINALIZADA") && !e.valor("puntaje").isBlank())
                    .max(Comparator.comparingInt((Fila e) -> Integer.parseInt(e.valor("puntaje")))
                            .thenComparingInt(Fila::id)).orElse(null);
            if (destino == null) continue;
            html.append("<a class=\"tendencia-visita kpi-riesgo\" href=\"/evaluaciones/")
                    .append(destino.id()).append("\" style=\"--riesgo:var(--")
                    .append(List.of("verde", "amarillo", "rojo").contains(color) ? color : "muted").append(")\">")
                    .append("<strong>").append(Html.escapar(fila.valor("puntaje"))).append("</strong><span>")
                    .append(Html.escapar(Formularios.etiqueta(fila.valor("color")))).append("</span><small>")
                    .append(LocalDate.parse(fila.valor("fecha").substring(0, 10))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .append(i == ultimas.size() - 1 ? " · última" : "").append("</small></a>");
        }
        if (ultimas.isEmpty()) html.append("<p class=\"muted\">Aún no hay visitas Finalizadas.</p>");
        html.append("</div></section>");
    }

    private static String periodo(Fila primera, Fila ultima) {
        long dias = ChronoUnit.DAYS.between(LocalDate.parse(primera.valor("fecha").substring(0, 10)),
                LocalDate.parse(ultima.valor("fecha").substring(0, 10)));
        return dias >= 28 && dias <= 31 ? " en un mes" : dias == 1 ? " en un día" : " en " + dias + " días";
    }

    static void ayuda(List<ContactoEquipo> contactos, StringBuilder html) {
        html.append(ContactoInicio.tarjeta(contactos));
    }
}
