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
        return pagina(sesion, contenido, "");
    }

    static String pagina(Sesion sesion, Contenido contenido, String tema) {
        if (sesion == null || sesion.cuenta() == null || sesion.cuenta().temporal()) {
            if (!tema.isBlank() && !List.of("entrar", "acerca").contains(tema))
                throw new SecurityException("Tema no disponible.");
            return AyudaPublica.pagina(sesion, contenido);
        }
        List<Articulo> articulos = articulos(sesion);
        if (tema.isBlank()) return inicio(sesion, contenido, articulos);
        Articulo elegido = articulos.stream().filter(a -> a.id().equals(tema)).findFirst()
                .orElseThrow(() -> new SecurityException("Este tema no está disponible para tu cuenta."));
        StringBuilder html = new StringBuilder("<div class=\"ayuda-layout ayuda-articulo-layout\"><aside>").append(
                        indice(articulos, tema)).append("</aside><article class=\"ayuda-articulo\" data-articulo id=\"")
                .append(elegido.id()).append("\"><a href=\"/ayuda\">‹ Centro de ayuda</a><h2>")
                .append(Html.escapar(elegido.titulo())).append("</h2><p class=\"muted\">")
                .append(Html.escapar(elegido.intro())).append("</p><ol class=\"ayuda-pasos\">");
        for (int i = 0; i < elegido.pasos().size(); i++)
            html.append("<li><p>").append(Html.escapar(elegido.pasos().get(i)))
                    .append("</p><p class=\"muted ayuda-veras\">Verás: ")
                    .append(Html.escapar(GuiaAyuda.veras(elegido.id(), i))).append("</p></li>");
        html.append("</ol>");
        if (List.of("resultados", "semaforo").contains(tema)) html.append(riesgos(contenido));
        html.append("<aside class=\"ayuda-consejo\"><strong>Consejo</strong><p>")
                .append(Html.escapar(elegido.consejo())).append("</p></aside>")
                .append("<div class=\"ayuda-util row\"><span>¿Te sirvió?</span><button class=\"btn\" type=\"button\" " +
                        "data-util aria-pressed=\"false\">Sí</button>")
                .append("<button class=\"btn\" type=\"button\" data-util " +
                        "aria-pressed=\"false\">No</button></div></article>")
                .append("<aside class=\"ayuda-relacionados stack\"><section class=\"card stack\"><h2>Temas " +
                        "relacionados</h2>");
        articulos.stream().filter(a -> !a.id().equals(tema)).sorted(java.util.Comparator.comparingInt(
                        a -> GuiaAyuda.seccion(a.id()).equals(GuiaAyuda.seccion(tema)) ? 0 : 1)).limit(3)
                .forEach(a -> html.append(enlace(a, false)));
        html.append("</section>").append(ContactoAyuda.tarjeta(contenido.contacto(), sesion))
                .append("</aside></div><script src=\"/static/js/centro-ayuda.js\" defer></script>");
        return Html.pagina("Centro de ayuda", html.toString(), sesion, "Ayuda · Artículo", "", false);
    }

    private static String inicio(Sesion sesion, Contenido contenido, List<Articulo> articulos) {
        String nombre = Html.nombreSinTitulo(sesion.cuenta().nombre()).split("\\s+")[0];
        StringBuilder html = new StringBuilder(
                "<section class=\"ayuda-inicio\"><div class=\"ayuda-superior\"><div>" +
                        "<label class=\"ayuda-buscador\">").append(
                        "<span class=\"sr-only\">Buscar ayuda</span><input type=\"search\" class=\"input " + "ayuda" +
                                "-buscar\" ")
                .append("data-ayuda-buscar aria-label=\"Buscar ayuda\" placeholder=\"¿Qué necesitas hacer?\"></label>")
                .append("<nav class=\"chips ayuda-populares\" aria-label=\"Temas populares\">");
        for (String id : GuiaAyuda.populares(sesion.cuenta().rol())) {
            Articulo a = articulos.stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
            html.append("<a class=\"chip\" href=\"/ayuda?tema=").append(id).append("\">")
                    .append(Html.escapar(a.titulo())).append("</a>");
        }
        html.append("</nav></div>").append(ContactoAyuda.tarjeta(contenido.contacto(), sesion))
                .append("</div><section data-ayuda-grupo><h2>¿Qué quieres hacer?</h2><div class=\"ayuda-tareas\">");
        for (String[] tarea : GuiaAyuda.tareas(sesion.cuenta().rol()))
            html.append("<a class=\"card ayuda-tarea\" data-ayuda-pregunta href=\"/ayuda?tema=").append(tarea[0])
                    .append("\"><svg width=\"26\" height=\"26\" viewBox=\"0 0 24 24\" fill=\"none\" " +
                            "stroke=\"currentColor\" stroke-width=\"1.5\" aria-hidden=\"true\">")
                    .append("<path d=\"M4 3h12l4 4v14H4z M8 9h8 M8 13h8 M8 17h5\"/></svg><strong>")
                    .append(Html.escapar(tarea[1])).append("</strong><span class=\"muted\" title=\"")
                    .append(Html.escapar(tarea[2])).append("\">").append(Html.escapar(tarea[2])).append("</span></a>");
        html.append("</div></section><h2 class=\"ayuda-todos-titulo\">Todos los temas</h2><div class=\"ayuda-todos\">");
        for (String grupo : GuiaAyuda.SECCIONES) {
            html.append("<section data-ayuda-grupo><h3>").append(Html.escapar(nombreGrupo(grupo, articulos))).append(
                    "</h3>");
            for (Articulo articulo : articulos)
                if (GuiaAyuda.seccion(articulo.id()).equals(grupo)) html.append(enlace(articulo, true));
            html.append("</section>");
        }
        html.append("</div><p data-ayuda-vacia hidden role=\"status\">No encontramos temas con esas palabras.</p>")
                .append("</section>")
                .append("<script src=\"/static/js/ayuda.js\" defer></script><script src=\"/static/js/centro-ayuda" +
                        ".js\" defer></script>");
        return Html.pagina("¿En qué te ayudamos, " + nombre + "?", html.toString(), sesion, "Ayuda · Inicio", "",
                false);
    }

    private static String nombreGrupo(String grupo, List<Articulo> articulos) {
        boolean soloAcerca = articulos.stream().filter(a -> GuiaAyuda.seccion(a.id()).equals(grupo))
                .allMatch(a -> a.id().equals("acerca"));
        return grupo.equals("Tu trabajo") && soloAcerca ? "Sobre ALINEO" : grupo;
    }

    private static String indice(List<Articulo> articulos, String actual) {
        StringBuilder html = new StringBuilder("<nav class=\"ayuda-navegacion\" aria-label=\"Temas de ayuda\">");
        for (String seccion : GuiaAyuda.SECCIONES) {
            html.append("<section><h3>").append(Html.escapar(nombreGrupo(seccion, articulos))).append("</h3>");
            for (Articulo articulo : articulos)
                if (GuiaAyuda.seccion(articulo.id()).equals(seccion)) {
                    html.append("<a href=\"/ayuda?tema=").append(articulo.id()).append("\"")
                            .append(articulo.id().equals(actual) ? " aria-current=\"page\"" : "").append(">")
                            .append(Html.escapar(articulo.titulo())).append("</a>");
                }
            html.append("</section>");
        }
        return html.append("</nav>").toString();
    }

    private static String enlace(Articulo articulo, boolean buscar) {
        return "<a href=\"/ayuda?tema=" + articulo.id() + "\" data-tema-id=\"" + articulo.id() + "\" id=\"" +
                articulo.id() + "\"" + (buscar ? " data-ayuda-pregunta" : "") + ">" + Html.escapar(articulo.titulo()) +
                "</a>";
    }

    private static String riesgos(Contenido contenido) {
        StringBuilder html = new StringBuilder("<dl class=\"ayuda-riesgos\">");
        for (var nivel : contenido.riesgos()) {
            String color = Formularios.etiqueta(nivel.color());
            html.append("<div><dt>").append(Html.escapar(color)).append(" · Nivel ").append(nivel.nivel())
                    .append("</dt><dd>");
            if (!nivel.nombre().equalsIgnoreCase(color)) html.append(Html.escapar(nivel.nombre())).append(". ");
            html.append(Html.escapar(nivel.accion())).append("</dd></div>");
        }
        return html.append("</dl>").toString();
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
        if (rol == null || rol == Cuenta.Rol.USUARIO) {
            lista.add(articulo("acerca", "Acerca de ALINEO", "Ergonomía con precisión · Método RULA.",
                    "ALINEO acompaña la observación y evaluación de posturas de trabajo.",
                    "El Evaluador revisa las propuestas de la foto; el servidor calcula el resultado RULA.",
                    "Proyecto Nuclear 2026-2 · Andrés Benavides, Johan Zapata y Laura Gabriella Romero."));
        }
        lista.add(articulo("pendientes", "Pendientes", "Revisa las mejoras que siguen abiertas.",
                "Abre Pendientes y elige Abiertos, Arreglados o Todos.", "Busca el puesto y abre su seguimiento.",
                rol == Cuenta.Rol.USUARIO ? "Consulta los datos de las visitas Finalizadas de tu empresa." :
                        "Revisa" + " prioridad, plazo y acciones del pendiente antes de actualizarlo."));
        lista.add(articulo("cuenta", "Mi cuenta", "Mantén tus datos y tu contraseña al día.",
                "Abre tu avatar y entra en Mi cuenta.", "Elige el dato que quieres editar y guarda.",
                "Para cambiar la contraseña, abre Seguridad y elige Cambiar contraseña."));
        if (rol == Cuenta.Rol.EVALUADOR) {
            lista.add(articulo("iniciar", "Iniciar una visita", "Empieza al llegar al lugar reservado.",
                    "Abre una reserva Aceptada de tu agenda.", "Revisa empresa, sede, salón y horario.",
                    "Pulsa Iniciar visita y elige un puesto pendiente para evaluar."));
            lista.add(
                    articulo("proximas", "Mis reservas", "Consulta tus citas y responde las que esperan confirmación.",
                            "Abre Reservas y revisa Tu próxima cita.", "Usa los chips para consultar otros estados.",
                            "Abre una reserva para revisar sus datos y las acciones disponibles."));
        }
        return List.copyOf(lista);
    }

    private static Articulo articulo(String id, String titulo, String intro, String... pasos) {
        return new Articulo(id, titulo, intro, List.of(pasos), GuiaAyuda.consejo(id));
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

}
