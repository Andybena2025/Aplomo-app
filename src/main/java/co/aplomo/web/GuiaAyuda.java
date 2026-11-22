package co.aplomo.web;

import co.aplomo.modelo.Cuenta.Rol;

import java.util.List;

/**
 * RF-002, RNF-007: tareas, orientación y consejos específicos; no calcula ni redefine reglas RULA.
 */
final class GuiaAyuda {
    static final List<String> SECCIONES = List.of("Tu trabajo", "Agenda y visitas", "Resultados y seguimiento",
            "Tu cuenta");

    private GuiaAyuda() {
    }

    static String seccion(String id) {
        return switch (id) {
            case "agenda", "notas", "solicitudes", "proximas", "iniciar" -> SECCIONES.get(1);
            case "resultados", "semaforo", "pendientes", "archivo" -> SECCIONES.get(2);
            case "cuenta" -> SECCIONES.get(3);
            default -> SECCIONES.get(0);
        };
    }

    static List<String> populares(Rol rol) {
        return switch (rol) {
            case ADMINISTRADOR -> List.of("empresas", "cuentas", "agenda", "resultados");
            case EVALUADOR -> List.of("evaluacion", "agenda", "durante", "resultados");
            case USUARIO -> List.of("resultados", "semaforo", "solicitudes", "proximas");
        };
    }

    static String[][] tareas(Rol rol) {
        return switch (rol) {
            case ADMINISTRADOR ->
                    new String[][]{{"empresas", "Crear una empresa", "Organiza sus sedes, salones y puestos."}, {
                        "cuentas", "Crear una cuenta", "Entrega un acceso personal al equipo."}, {"asignaciones",
                            "Asignar evaluador", "Relaciona su cuenta con las empresas."}, {"agenda", "Ver la agenda"
                            , "Consulta las citas de tu equipo."}, {"resultados", "Informe de empresa", "Revisa " +
                            "resultados y guarda el PDF."}, {"pendientes", "Pendientes", "Da seguimiento a las " +
                            "mejoras."}};
            case EVALUADOR ->
                    new String[][]{{"agenda", "Reservar una cita", "Elige un lugar y horario disponible."}, {"iniciar"
                            , "Iniciar una visita", "Continúa desde una reserva aceptada."}, {"evaluacion", "Evaluar " +
                            "un puesto", "Sigue el recorrido y revisa las respuestas."}, {"durante", "Marcar los " +
                            "puntos", "Corrige o mide a mano sobre la foto."}, {"proximas", "Mis reservas", "Revisa " +
                            "tus próximas citas y respuestas."}, {"pendientes", "Pendientes", "Consulta las mejoras " +
                            "de tus empresas."}};
            case USUARIO ->
                    new String[][]{{"resultados", "Ver resultados", "Consulta las visitas Finalizadas."}, {"semaforo"
                            , "Leer el semáforo", "Entiende el color y el nivel escrito."}, {"solicitudes",
                            "Solicitar una visita", "Propón una sede, una fecha y una hora."}, {"proximas", "Mis " +
                            "reservas", "Revisa las citas de tu empresa."}, {"pendientes", "Pendientes", "Consulta " +
                            "las mejoras y su seguimiento."}, {"cuenta", "Mi cuenta", "Actualiza tus datos y " +
                            "contraseña."}};
        };
    }

    static String consejo(String id) {
        return switch (id) {
            case "cuentas" ->
                    "Entrega la contraseña temporal por un canal privado; cada persona debe cambiarla al entrar.";
            case "asignaciones" ->
                    "Revisa la empresa elegida: quitar una asignación retira el acceso en la siguiente petición.";
            case "empresas" -> "Usa nombres de puestos que tu equipo reconozca en el lugar de trabajo.";
            case "estados" -> "Desactivar conserva el historial; comprueba primero que abriste el registro correcto.";
            case "agenda" ->
                    "Revisa la fecha y el lugar antes de reservar; no se aceptan citas cruzadas para el mismo " +
                            "Evaluador.";
            case "notas" -> "Escribe una indicación breve sobre esa cita; Entendido confirma que se leyó.";
            case "acceso" -> "Comprueba el correo del contacto antes de aprobar: allí se identifica su nueva cuenta.";
            case "resultados" ->
                    "Para imprimir, desmarca Encabezados y pies de página; conserva siempre el nivel escrito.";
            case "solicitudes" -> "Si un horario no está disponible, elige otra hora antes de enviar la solicitud.";
            case "evaluacion" -> "Observa varios ciclos y confirma las propuestas de la foto; la IA no decide por ti.";
            case "durante" -> "Marca cada punto donde indica la guía; puedes arrastrarlo y revisar de nuevo el ángulo.";
            case "fotos" -> "Usa una foto de perfil clara y completa de la misma postura que estás observando.";
            case "practicar" ->
                    "Ensaya en Probar evaluación: los intentos de práctica quedan separados de tus resultados reales.";
            case "semaforo" ->
                    "Lee color y nivel juntos: el resultado orienta la revisión del puesto, además del número.";
            case "proximas" ->
                    "Una reserva confirmada no significa que la visita ya esté Finalizada; revisa su estado.";
            case "archivo" ->
                    "El historial conserva evaluaciones anteriores; revisa la fecha antes de comparar resultados.";
            case "acerca" ->
                    "ALINEO acompaña la observación; el Evaluador sigue revisando las propuestas y las mejoras.";
            case "pendientes" ->
                    "Comprueba el plazo y el estado del pendiente; el historial no se borra al arreglarlo.";
            case "cuenta" ->
                    "Usa una contraseña de al menos ocho caracteres, con una letra y un número; no la compartas.";
            case "iniciar" ->
                    "Inicia la visita cuando estés en el lugar y confirma que corresponde a la reserva correcta.";
            default -> throw new IllegalArgumentException("Falta el consejo específico del tema.");
        };
    }

    static String veras(String id, int paso) {
        List<String> resultado = switch (id) {
            case "cuentas" -> List.of("La lista de cuentas y Nuevo usuario.", "Los datos, el rol y las empresas.",
                    "La cuenta creada con cambio de contraseña obligatorio.");
            case "asignaciones" ->
                    List.of("El formulario de la cuenta elegida.", "Las empresas asignadas a esa cuenta.",
                            "El equipo que tiene acceso a la empresa.");
            case "empresas" -> List.of("El formulario de empresa.", "La lista de sedes de esa empresa.",
                    "Los salones y sus puestos registrados.");
            case "estados" ->
                    List.of("El detalle y su estado actual.", "El estado actualizado, con el historial conservado.",
                            "Las acciones que permite tu cuenta.");
            case "agenda" -> List.of("El calendario y sus citas.", "El lugar, la fecha y la hora elegidos.",
                    "El detalle de la cita y las acciones de tu rol.");
            case "notas" -> List.of("La reserva con sus datos.", "La confirmación de que se guardó la nota.",
                    "La lectura confirmada en el seguimiento de la nota.");
            case "acceso" -> List.of("Las solicitudes pendientes.", "Los datos declarados por la empresa.",
                    "La respuesta y su estado actualizado.");
            case "resultados" -> List.of("Los resultados autorizados.", "El puntaje, el nivel y las mejoras.",
                    "El cuadro de impresión del navegador.");
            case "solicitudes" -> List.of("El formulario de solicitud.", "Las opciones de fecha, lugar y Evaluador.",
                    "El estado de la solicitud y sus avisos.");
            case "evaluacion" -> List.of("Los puestos de la visita.", "El contexto y el permiso para las fotos.",
                    "El resumen del resultado antes de guardarlo.");
            case "durante" ->
                    List.of("La opción Calcular manualmente.", "Las opciones para cambiar la foto o medir a mano.",
                            "Los puntos y las respuestas para revisar.");
            case "fotos" -> List.of("La foto de perfil para medir.", "Las fotos opcionales de la misma postura.",
                    "El contexto conservado sin foto ni puntaje.");
            case "practicar" -> List.of("La entrada a Probar evaluación.", "El mismo recorrido guiado de evaluación.",
                    "Los intentos de práctica separados.");
            case "semaforo" -> List.of("Resultados de una visita Finalizada.", "El color y el nivel escrito.",
                    "Las mejoras y su seguimiento.");
            case "proximas" -> List.of("Las próximas reservas.", "La fecha, el lugar y el estado.",
                    "Las acciones y resultados disponibles para tu rol.");
            case "archivo" ->
                    List.of("La lista de empresas del Archivo.", "El historial organizado por lugar y puesto.",
                            "Las evaluaciones autorizadas y sus fechas.");
            case "acerca" ->
                    List.of("El propósito de ALINEO.", "La revisión del Evaluador y el resultado del servidor.",
                            "El equipo del Proyecto Nuclear.");
            case "pendientes" -> List.of("Los estados y sus contadores.", "El puesto y su seguimiento.",
                    "Los datos que tu rol puede consultar o actualizar.");
            case "cuenta" -> List.of("Tus datos y accesos.", "La confirmación del dato guardado.",
                    "El formulario para cambiar tu contraseña.");
            case "iniciar" -> List.of("Los datos de la reserva Aceptada.", "El lugar de la cita.",
                    "La visita En curso con sus puestos.");
            default -> throw new IllegalArgumentException("Falta la orientación del tema.");
        };
        return resultado.get(paso);
    }
}
