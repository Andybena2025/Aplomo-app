package co.aplomo.web;

import co.aplomo.servicio.Ayuda.Contenido;
import co.aplomo.config.Contacto;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.modelo.Cuenta.Rol;

import static co.aplomo.web.Html.escapar;

/**
 * Paso 25, RNF-002/007: respuestas breves, HTML accesible sin depender de JavaScript.
 */
public final class PaginasAyuda {
    private PaginasAyuda() {
    }

    /**
     * Centro de ayuda, con preguntas filtrables y contenidos autorizados por el servicio. RNF-011.
     */
    public static String pagina(Sesion sesion, Contenido elemento) {
        return CentroAyuda.pagina(sesion, elemento);
    }

    /**
     * RF-002, RNF-007: el tema pedido se comprueba contra los artículos permitidos para la sesión.
     */
    public static String pagina(Sesion sesion, Contenido elemento, java.util.Map<String, String> parametros) {
        return CentroAyuda.pagina(sesion, elemento, parametros.getOrDefault("tema", ""));
    }

    private static void tema(int numero, Sesion sesion, Contenido elemento, StringBuilder contenido2) {
        switch (numero) {
            case 1 -> temaEntrar(contenido2);
            case 2 -> temaRoles(contenido2);
            case 3 -> temaEvaluar(contenido2);
            case 4 -> temaAgenda(contenido2);
            case 5 -> temaRiesgos(contenido2, elemento);
            case 6 -> temaConsultar(contenido2, sesion);
            case 7 -> temaFotos(contenido2);
            case 8 -> temaProblemas(contenido2);
            case 9 -> temaCuentas(contenido2);
            case 11 -> temaDurante(contenido2);
            case 12 -> temaArchivo(contenido2);
            case 13 -> temaAcerca(contenido2);
            case 10 -> temaContacto(contenido2, elemento);
            default -> throw new IllegalArgumentException("Tema de ayuda no disponible.");
        }
    }

    private static void temaEntrar(StringBuilder contenido2) {

        contenido2.append(pregunta("¿Cómo entro y cambio mi contraseña?",
                "<p>Abre <a href=\"/login\">Iniciar sesión</a> con tu correo y contraseña. Si" +
                        " es temporal, debes cambiarla antes de continuar. Para cambiarla después, ab" +
                        "re tu avatar y elige <a href=\"/cuenta\">Mi cuenta</a>.</p>"));
        contenido2.append(pregunta("¿Por qué no puedo entrar o mi sesión se cerró?",
                "<p>Tras cinco intentos fallidos, espera 15 minutos antes de volver a intenta" +
                        "r. La sesión se cierra después de 30 minutos sin actividad. Si olvidaste la " +
                        "contraseña, pídele al Administrador una temporal.</p>"));

    }

    private static void temaRoles(StringBuilder contenido2) {
        contenido2.append(pregunta("¿Qué puede hacer cada rol?",
                "<p>El Administrador gestiona todas las empresas, las cuentas y las asignacio" +
                        "nes. El Evaluador trabaja en las empresas que le asignaron, con agenda y eva" +
                        "luaciones propias. El Usuario consulta sus empresas y los resultados de visi" +
                        "tas Finalizadas, sin editar ni ver fotos.</p>"));
    }

    private static void temaEvaluar(StringBuilder contenido2) {
        contenido2.append(pregunta("¿Cómo hago una evaluación o pruebo el recorrido?",
                "<ol><li>Crea una <a href=\"/reservas/nueva\">reserva</a>, acéptala e inicia " +
                        "la visita.</li><li>Elige un puesto activo y completa el contexto y el consen" +
                        "timiento físico.</li><li>Sube las fotos: revisa los puntos de la IA o elige " +
                        "«Calcular manualmente».</li><li>Confirma el resultado y finaliza la visita p" +
                        "ara que la empresa consulte los resultados. Para practicar, usa <a href=\"/d" +
                        "emo/evaluacion\">Probar evaluación</a>; sus intentos quedan en una empresa f" +
                        "icticia.</li></ol>"));
    }

    private static void temaAgenda(StringBuilder contenido2) {
        contenido2.append(pregunta("¿Cómo consulto mi agenda?",
                "<p>Abre <a href=\"/agenda\">la agenda</a> y elige Semana o Mes. Usa Anterior" +
                        ", Hoy o Siguiente para moverte entre fechas; en el mes, «+N más» abre la sem" +
                        "ana. No se permite guardar dos citas que se crucen para un mismo Evaluador. " +
                        "Las notas del Administrador se leen en la reserva y se confirman con Entendi" + "do.</p>"));
    }

    private static void temaRiesgos(StringBuilder contenido2, Contenido elemento) {

        StringBuilder riesgo = new StringBuilder(
                "<p>El puntaje RULA va de 1 a 7; un valor más alto requiere más atención. El " +
                        "semáforo y el nivel escrito indican cómo actuar.</p><dl class=\"ayuda-riesgo" + "s\">");
        for (var empresa : elemento.riesgos())
            riesgo.append("<div><dt>").append(Html.riesgo(empresa.minimo(), empresa.color(), empresa.nivel()))
                    .append(" — ").append(empresa.minimo() == empresa.maximo() ? empresa.minimo() :
                            empresa.minimo() + " a " + empresa.maximo()).append("</dt><dd>")
                    .append(escapar(empresa.nombre()))
                    .append(". ").append(escapar(empresa.accion())).append("</dd></div>");
        contenido2.append(
                pregunta("¿Qué significan el puntaje, el color y el nivel?", riesgo.append("</dl>").toString()));

    }

    private static void temaConsultar(StringBuilder contenido2, Sesion sesion) {

        boolean usuario = sesion != null && sesion.cuenta().rol() == Rol.USUARIO;
        contenido2.append(pregunta("¿Cómo busco resultados y consulto el informe?",
                "<p>En cada consulta, usa Buscar, los filtros y los títulos de las columnas p" +
                        "ara ordenar. Abre <a href=\"/empresas\">Empresas</a> y elige Ver calificacio" +
                        "nes para consultar sus resultados. " + (usuario ?
                        "Pídele el informe imprimible al Administrador o al Evaluador de " + "tu empresa." :
                        "Desde la empresa, elige Generar informe y luego Imprimir o guardar " + "PDF; en e" +
                        "l cuadro de imprimir, desmarca Encabezados y pies de página.") + "</p>"));

    }

    private static void temaFotos(StringBuilder contenido2) {
        contenido2.append(pregunta("¿Cómo se guardan las fotos y quién puede verlas?",
                "<p>Confirma primero que la persona firmó el consentimiento físico y quedó ar" +
                        "chivado. Con registro fotográfico se admiten de 1 a 10 fotos JPG o PNG, de h" +
                        "asta 5 MB cada una, con su vista y descripción. «Sin registro fotográfico» c" +
                        "onserva el contexto sin fotos ni puntaje. Solo el Administrador y el Evaluad" +
                        "or autorizado pueden verlas; el Usuario nunca.</p>"));
    }

    private static void temaProblemas(StringBuilder contenido2) {

        contenido2.append(pregunta("¿Por qué no veo una empresa o cambió mi acceso?",
                "<p>Puede que no esté asignada a tu cuenta o que el Administrador haya cambia" +
                        "do tus permisos. Vuelve a iniciar sesión y, si sigue igual, pídele que revis" +
                        "e tus asignaciones.</p>"));
        contenido2.append(pregunta("¿Qué hago si la cámara o la IA no funcionan?",
                "<p>Si la cámara no abre, usa Subir foto. Puedes volver a intentar Detectar p" +
                        "untos; si no los encuentra, elige Calcular manualmente y sigue las instrucci" +
                        "ones sobre la foto. Revisa siempre los puntos y las respuestas antes de guar" + "dar.</p>"));
        contenido2.append(pregunta("¿Cómo quito los encabezados del navegador en el PDF?",
                "<p>En el cuadro de imprimir, desmarca Encabezados y pies de página. El nivel" +
                        " de riesgo queda escrito para que el informe se entienda también en blanco y" +
                        " negro.</p>"));

    }

    private static void temaCuentas(StringBuilder contenido2) {
        contenido2.append(pregunta("¿Cómo creo cuentas y asigno empresas?",
                "<p>Abre <a href=\"/usuarios\">Usuarios</a> y elige Nuevo usuario. Elige el r" +
                        "ol y las empresas: Usuario necesita al menos una y Evaluador puede quedar si" +
                        "n ninguna; Administrador tiene acceso a todas. Comunica la contraseña tempor" +
                        "al por un canal privado y la persona deberá cambiarla al entrar. Desactivar " +
                        "conserva el historial; no puedes dejar al sistema sin ningún Administrador a" + "ctivo.</p>"));
    }

    private static void temaDurante(StringBuilder contenido2) {
        contenido2.append(durante());
    }

    private static void temaArchivo(StringBuilder contenido2) {
        contenido2.append(pregunta("¿Qué encuentro en Archivo?",
                "<p>Elige una empresa y despliega su sede y salón. Ver historial reúne las ev" +
                        "aluaciones del puesto. El Evaluador conserva su autoría aunque cambie la asi" +
                        "gnación; el Usuario solo ve visitas Finalizadas.</p>"));
    }

    private static void temaAcerca(StringBuilder contenido2) {
        contenido2.append(acerca());
    }

    private static void temaContacto(StringBuilder contenido2, Contenido elemento) {
        contenido2.append("<div data-ayuda-pregunta>").append(contacto(elemento.contacto())).append("</div>");
    }

    private static String indice(Contenido contenido) {
        StringBuilder html = new StringBuilder("<nav class=\"ayuda-indice\" aria-label=\"Temas de ayuda\">");
        String[] nombres = {"", "Entrar", "Roles", "Evaluación", "Agenda", "Resultados", "Consultas", "Fotos",
                "Problemas", "Cuentas", "Contacto", "Durante la evaluación", "Archivo", "Acerca de ALINEO."};
        for (int tema : java.util.List.of(1, 2, 3, 11, 4, 5, 12, 7, 8, 9, 13, 10)) {
            if (!contenido.temas().contains(tema)) continue;
            html.append("<a class=\"card\" href=\"#ayuda-").append(tema)
                    .append("\"><svg width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" ")
                    .append("stroke=\"currentColor\" stroke-width=\"1.4\" aria-hidden=\"true\">")
                    .append("<path d=\"M4 4h16v16H4z M8 8h8 M8 12h6 M8 16h4\"/></svg>")
                    .append(Html.escapar(nombres[tema])).append("</a>");
        }
        return html.append("</nav>").toString();
    }

    private static String durante() {
        return pregunta("¿No hay internet?",
                "<p>La IA necesita internet para descargar su modelo. Si la app sigue accesib" +
                        "le, usa Calcular manualmente. Las respuestas sin guardar no se conservan si " +
                        "cierras la página.</p>") + pregunta("¿La IA no detecta los puntos?",
                "<p>Usa Calcular manualmente. En el recorrido actual marca hombro, codo, muñe" +
                        "ca, coronilla y cadera en el orden indicado; después revisa los ángulos. Pue" +
                        "des arrastrar un punto para corregirlo.</p>") + pregunta("¿La foto salió oscura o cortada?",
                "<p>Cambia la foto por una clara y completa, de perfil. No confirmes puntos q" +
                        "ue no puedas revisar.</p>") + pregunta("¿El trabajador no quiere fotos?",
                "<p>Elige Sin registro fotográfico. Se conserva el contexto, sin fotos ni pun" + "taje.</p>") +
                pregunta("¿Se cerró la sesión en plena visita?",
                        "<p>Vuelve a ingresar y abre la visita En curso. Las evaluaciones guardadas s" +
                                "e conservan; el formulario sin guardar debe completarse otra vez.</p>");
    }

    private static String acerca() {
        return pregunta("¿Qué es ALINEO.?",
                "<p>Evaluamos posturas de trabajo con el método RULA. La app acompaña la rese" +
                        "rva, las fotos, el cálculo y las sugerencias.</p>") + pregunta("¿Por qué importa?",
                "<p>Medir una postura permite reconocer riesgos y decidir mejoras. El resulta" +
                        "do orienta la revisión del puesto; el evaluador confirma siempre las observa" +
                        "ciones.</p>") + pregunta("¿Quiénes lo hacemos?",
                "<p>Laura Gabriella Romero Gómez, Johan Andrés Zapata Quintero y Andrés Eduar" +
                        "do Benavides. Estudiantes de Ingeniería de Software de la Corporación Univer" +
                        "sitaria Empresarial Alexander von Humboldt. Proyecto Nuclear 2026-2.</p>");
    }

    private static String pregunta(String titulo, String respuesta) {
        return "<details class=\"guia\" data-ayuda-pregunta><summary>" + escapar(titulo) +
                "</summary><div class=\"ayuda-respuesta\">" + respuesta + "</div></details>";
    }

    private static String titulo(int cantidad) {
        return switch (cantidad) {
            case 1 -> "Entrar a tu cuenta";
            case 2 -> "Qué puede hacer cada rol";
            case 3 -> "Hacer una evaluación paso a paso";
            case 4 -> "Mi agenda";
            case 5 -> "Entender el resultado";
            case 6 -> "Consultar, buscar y filtrar";
            case 7 -> "Fotos y privacidad";
            case 8 -> "Problemas comunes";
            case 9 -> "Solo Administrador";
            case 10 -> "Contacto";
            case 11 -> "Durante la evaluación";
            case 12 -> "Archivo";
            case 13 -> "Acerca de ALINEO.";
            default -> "";
        };
    }

    private static String contacto(Contacto elemento) {
        var contenido2 = new StringBuilder(
                "<h3>¿Necesitas ayuda de una persona?</h3><p class=\"marca-contacto\">ALINEO.</p>");
        contenido2.append("<p>Laura Gabriella Romero Gómez · Administradora principal de ALINEO.</p>");
        if (!elemento.correo().isBlank())
            contenido2.append("<p><a href=\"mailto:").append(escapar(elemento.correo())).append("\">")
                    .append(escapar(elemento.correo())).append("</a></p>");
        if (!elemento.telefono().isBlank())
            contenido2.append("<p>").append(escapar(elemento.telefono())).append("</p>");
        if (!elemento.whatsapp().isBlank())
            contenido2.append("<a class=\"btn\" href=\"https://wa.me/").append(elemento.whatsapp())
                    .append("\" rel=\"noopener noreferrer\">Escribir por WhatsApp</a>");
        if (elemento.incompleto()) contenido2.append("<p class=\"hint\">Escríbele al Administrador de tu cuenta.</p>");
        return contenido2.toString();
    }
}
