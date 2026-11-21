package co.aplomo.web;

import co.aplomo.config.Contacto;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Ayuda.Contenido;

import java.util.ArrayList;
import java.util.List;

/**
 * RF-002/027, RNF-007: artículos por rol, sin acordeones ni datos de otros perfiles.
 */
final class CentroAyuda {
    private CentroAyuda() {
    }

    private record Articulo(String id, String titulo, String intro, List<String> pasos, String consejo) {
    }

    static String pagina(Sesion sesion, Contenido contenido) {
        List<Articulo> articulos = articulos(sesion);
        StringBuilder html = new StringBuilder("<div class=\"ayuda-layout\"><aside><nav class=\"ayuda-navegacion\" " +
                "aria-label=\"Temas de ayuda\">");
        for (Articulo articulo : articulos) {
            html.append("<a href=\"#").append(articulo.id()).append("\" data-articulo-enlace>")
                    .append(Html.escapar(articulo.titulo())).append("</a>");
        }
        html.append("</nav>");
        html.append(contacto(contenido.contacto()));
        html.append("</aside><div>");
        for (Articulo articulo : articulos) {
            html.append("<article class=\"ayuda-articulo\" id=\"").append(articulo.id())
                    .append("\" data-articulo><p class=\"migas\">Centro de ayuda › ")
                    .append(Html.escapar(articulo.titulo())).append("</p><h1>").append(Html.escapar(articulo.titulo()))
                    .append("</h1><p class=\"muted\">").append(Html.escapar(articulo.intro())).append("</p><ol>");
            for (String paso : articulo.pasos()) html.append("<li>").append(paso).append("</li>");
            html.append("</ol>");
            if (articulo.id().equals("resultados") || articulo.id().equals("semaforo")) {
                html.append("<dl class=\"ayuda-riesgos\">");
                for (var nivel : contenido.riesgos()) {
                    html.append("<div><dt>").append(Html.riesgo(nivel.minimo(), nivel.color(), nivel.nivel()))
                            .append(" · ").append(Html.escapar(nivel.nombre())).append("</dt><dd>")
                            .append(Html.escapar(nivel.accion())).append("</dd></div>");
                }
                html.append("</dl>");
            }
            html.append("<p class=\"ayuda-consejo\"><strong>Consejo</strong> ").append(Html.escapar(articulo.consejo()))
                    .append("</p><h2>Relacionado</h2><div class=\"row\">");
            articulos.stream().filter(otro -> !otro.id().equals(articulo.id())).limit(2).forEach(
                    otro -> html.append("<a href=\"#").append(otro.id()).append("\" data-articulo-enlace>")
                            .append(Html.escapar(otro.titulo())).append("</a>"));
            html.append("</div><div class=\"ayuda-util row\"><span>¿Te sirvió?</span>" +
                    "<button class=\"btn\" type=\"button\" data-util aria-pressed=\"false\">Sí</button>" +
                    "<button class=\"btn\" type=\"button\" data-util " +
                    "aria-pressed=\"false\">No</button></div></article>");
        }
        html.append("</div></div><script src=\"/static/js/centro-ayuda.js\" defer></script>");
        return Html.pagina("Centro de ayuda", html.toString(), sesion, false, false);
    }

    private static List<Articulo> articulos(Sesion sesion) {
        List<Articulo> lista = new ArrayList<>();
        Cuenta.Rol rol = sesion == null || sesion.cuenta().temporal() ? null : sesion.cuenta().rol();
        if (rol == Cuenta.Rol.ADMINISTRADOR) {
            lista.add(articulo("cuentas", "Crear una cuenta", "Cada persona entra con su propio correo.",
                    "Abre Usuarios y pulsa Nuevo usuario.", "Completa sus datos y elige el rol.",
                    "Entrega la contraseña temporal; la persona deberá cambiarla al entrar."));
            lista.add(articulo("asignaciones", "Asignar empresas y evaluador", "El acceso se comprueba en el servidor.",
                    "Abre la cuenta del Evaluador desde Usuarios.", "Selecciona sus empresas y guarda los cambios.",
                    "Comprueba la asignación en el detalle de la empresa."));
            lista.add(articulo("empresas", "Crear empresa y sedes", "Organiza la empresa antes de evaluar.",
                    "En Empresas, pulsa Nueva empresa y completa los datos.", "Abre la empresa y agrega sus sedes.",
                    "Dentro de cada sede, crea salones y puestos con nombres reconocibles."));
            lista.add(articulo("estados", "Activar o desactivar", "El historial siempre se conserva.",
                    "Abre el registro que administras.", "Pulsa Desactivar o Reactivar según corresponda.",
                    "En una empresa creada por un Evaluador, solo su dueño modifica sus datos y espacios."));
            lista.add(articulo("agenda", "Agenda del equipo", "Consulta las citas con el color de cada Evaluador.",
                    "Abre Agenda.", "Filtra el equipo con los chips y elige Mes o Semana.",
                    "Abre una cita para consultar sus datos; el Administrador no inicia visitas."));
            lista.add(articulo("notas", "Dejar una nota", "Comunica una indicación relacionada con una cita.",
                    "Abre la cita desde Agenda.", "Escribe la Nota para el evaluador y guárdala.",
                    "El Evaluador puede confirmar su lectura con Entendido."));
            lista.add(articulo("acceso", "Solicitudes de acceso", "Revisa los datos antes de habilitar una cuenta.",
                    "Abre Avisos y elige una solicitud pendiente.", "Revisa empresa, contacto y correo.",
                    "Aprueba para crear su acceso o rechaza indicando el motivo."));
            lista.add(resultados("Resultados e informe PDF"));
        } else if (rol == Cuenta.Rol.EVALUADOR) {
            lista.add(articulo("agenda", "Agenda y reservas", "Prepara tu jornada con las citas asignadas.",
                    "Abre Mi agenda y elige la fecha.", "Crea una reserva para una empresa a la que tengas acceso.",
                    "Desde la reserva Aceptada, inicia la visita cuando estés en el lugar."));
            lista.add(solicitudes());
            lista.add(articulo("evaluacion", "Hacer una evaluación", "Una foto, una postura y una revisión cuidadosa.",
                    "Abre una visita En curso y elige un puesto pendiente.",
                    "Confirma permiso, contexto y fotos de la misma postura.",
                    "Revisa los puntos, los rangos y el esfuerzo antes de guardar el resultado."));
            lista.add(articulo("durante", "Durante la visita", "Puedes seguir midiendo a mano si la IA falla.",
                    "Sin internet, la IA no funciona: usa Calcular manualmente si la app sigue accesible.",
                    "Si la IA no detecta la persona, cambia la foto o usa Calcular manualmente.",
                    "Revisa cada punto y cada propuesta; la foto no reemplaza la observación del trabajo."));
            lista.add(articulo("fotos", "Fotos", "Fotografía solo con el permiso de la persona.",
                    "Usa una foto de perfil clara y completa para medir.",
                    "Agrega frente o detalle cuando ayuden a revisar la misma postura.",
                    "Si no acepta fotos, elige Sin registro fotográfico; no se calcula un puntaje."));
            lista.add(resultados("Resultados y semáforo"));
            lista.add(articulo("practicar", "Practicar", "Ensaya el recorrido antes de una visita real.",
                    "Desde Inicio abre Probar evaluación.", "Elige el puesto y completa el mismo recorrido.",
                    "El modo práctica no cuenta en tus resultados reales."));
        } else if (rol == Cuenta.Rol.USUARIO) {
            lista.add(resultados("Ver resultados"));
            lista.add(articulo("semaforo", "Semáforo", "El nivel escrito explica el resultado además del color.",
                    "Abre los resultados de una visita Finalizada.", "Lee el puntaje, el color y el nivel juntos.",
                    "Consulta las sugerencias y los pendientes para entender las mejoras propuestas."));
            lista.add(solicitudes());
            lista.add(articulo("proximas", "Próximas visitas", "Consulta las reservas de tus empresas.",
                    "Abre Reservas y elige Próximas.", "Abre una fila para ver fecha, lugar y estado.",
                    "Los resultados de una visita En curso aparecen solo cuando se finaliza."));
        }
        if (rol != null) lista.add(articulo("archivo", "Archivo", "Consulta el historial sin alterar los registros.",
                "Abre Archivo y elige una empresa.", "Despliega la sede, el salón y el puesto.", rol ==
                        Cuenta.Rol.EVALUADOR ? "El Archivo conserva tus evaluaciones aunque cambie tu asignación." :
                        "Abre una evaluación para consultar su resultado y seguimiento."));
        lista.add(
                articulo("entrar", "Entrar y contraseña", "Tu acceso es personal.", "Entra con tu correo y contraseña.",
                        "Si es temporal, cámbiala antes de continuar.",
                        "Después puedes cambiarla desde Mi cuenta. Si la olvidas, pide una temporal al Administrador" +
                                "."));
        if (rol == null || rol == Cuenta.Rol.USUARIO) {
            lista.add(articulo("acerca", "Acerca de ALINEO", "Ergonomía con precisión · Método RULA.",
                    "ALINEO acompaña la observación y evaluación de posturas de trabajo.",
                    "El Evaluador revisa las propuestas de la foto; el servidor calcula el resultado RULA.",
                    "Proyecto Nuclear 2026-2 · Andrés Benavides, Johan Zapata y Laura Gabriella Romero."));
        }
        return List.copyOf(lista);
    }

    private static Articulo articulo(String id, String titulo, String intro, String... pasos) {
        return new Articulo(id, titulo, intro, List.of(pasos),
                "Confirma los datos antes de guardar y conserva tu acceso personal.");
    }

    private static Articulo resultados(String titulo) {
        return articulo("resultados", titulo, "El puntaje orienta la revisión del puesto.",
                "Abre la visita o el resultado autorizado de tu empresa.",
                "Lee puntaje, color, nivel y las sugerencias; no te guíes solo por el color.",
                "En el informe, desmarca Encabezados y pies de página antes de imprimir o guardar PDF.");
    }

    private static Articulo solicitudes() {
        return articulo("solicitudes", "Solicitudes de visita", "Elige un lugar y un horario disponible.",
                "Desde Inicio solicita una visita para una sede de tu empresa.",
                "Elige Evaluador, fecha y hora; si están ocupados, busca otra hora u otro Evaluador.",
                "Consulta la respuesta en Avisos y en el estado de la reserva.");
    }

    private static String contacto(Contacto contacto) {
        StringBuilder html = new StringBuilder("<section class=\"ayuda-contacto\"><h2>Tu administradora</h2>" +
                "<p>Laura Gabriella Romero</p><p class=\"hint\">Administradora principal de ALINEO.</p>" +
                "<div class=\"row\">");
        if (!contacto.whatsapp().isBlank())
            html.append("<a class=\"btn\" href=\"https://wa.me/").append(Html.escapar(contacto.whatsapp()))
                    .append("\" target=\"_blank\" rel=\"noopener\">WhatsApp</a>");
        if (!contacto.correo().isBlank())
            html.append("<a class=\"btn\" href=\"mailto:").append(Html.escapar(contacto.correo()))
                    .append("\">Correo</a>");
        html.append("</div>");
        if (contacto.incompleto()) html.append("<p class=\"hint\">Escríbele al Administrador de tu cuenta.</p>");
        return html.append("</section>").toString();
    }
}
