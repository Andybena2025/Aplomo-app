package co.aplomo.web;
import co.aplomo.servicio.Ayuda.Contenido;import co.aplomo.config.Contacto;import co.aplomo.seguridad.Sesiones.Sesion;import co.aplomo.modelo.Cuenta.Rol;import static co.aplomo.web.Html.escapar;
/** Paso 25, RNF-002/007: respuestas breves, HTML accesible sin depender de JavaScript. */
public final class PaginasAyuda {
 private PaginasAyuda(){}
 /** Centro de ayuda, con preguntas filtrables y contenidos autorizados por el servicio. */
 public static String pagina(Sesion s,Contenido c){
  StringBuilder h=new StringBuilder(Html.encabezado("Ayuda · Centro de ayuda","Centro de ayuda",s==null?"<a class=\"btn\" href=\"/login\">Volver a iniciar sesión</a>":""));
  h.append("<p class=\"muted\">Encuentra una respuesta y sigue con tu trabajo.</p><div class=\"card field ayuda-buscador\"><label for=\"ayuda-buscar\">¿Qué necesitas?</label><input class=\"input\" type=\"search\" id=\"ayuda-buscar\" placeholder=\"Busca una pregunta\" data-ayuda-buscar></div><p class=\"aviso\" role=\"status\" data-ayuda-vacia hidden>No encontramos esa pregunta. Pregúntale al Administrador de tu cuenta.</p><div class=\"stack ayuda-temas\">");
  h.append(indice(c));
  for(int n=1;n<=13;n++)if(c.temas().contains(n)){
   h.append("<section class=\"card stack\" data-ayuda-grupo id=\"ayuda-"+n+"\" data-tema=\"").append(n).append("\"><h2>").append(titulo(n)).append("</h2>");
   switch(n){
    case 1->{h.append(pregunta("¿Cómo entro y cambio mi contraseña?","<p>Abre <a href=\"/login\">Iniciar sesión</a> con tu correo y contraseña. Si es temporal, debes cambiarla antes de continuar. Para cambiarla después, abre tu avatar y elige <a href=\"/cuenta\">Mi cuenta</a>.</p>"));h.append(pregunta("¿Por qué no puedo entrar o mi sesión se cerró?","<p>Tras cinco intentos fallidos, espera 15 minutos antes de volver a intentar. La sesión se cierra después de 30 minutos sin actividad. Si olvidaste la contraseña, pídele al Administrador una temporal.</p>"));}
    case 2->h.append(pregunta("¿Qué puede hacer cada rol?","<p>El Administrador gestiona todas las empresas, las cuentas y las asignaciones. El Evaluador trabaja en las empresas que le asignaron, con agenda y evaluaciones propias. El Usuario consulta sus empresas y los resultados de visitas Finalizadas, sin editar ni ver fotos.</p>"));
    case 3->h.append(pregunta("¿Cómo hago una evaluación o pruebo el recorrido?","<ol><li>Crea una <a href=\"/reservas/nueva\">reserva</a>, acéptala e inicia la visita.</li><li>Elige un puesto activo y completa el contexto y el consentimiento físico.</li><li>Sube las fotos: revisa los puntos de la IA o elige «Calcular manualmente».</li><li>Confirma el resultado y finaliza la visita para que la empresa consulte los resultados. Para practicar, usa <a href=\"/demo/evaluacion\">Probar evaluación</a>; sus intentos quedan en una empresa ficticia.</li></ol>"));
    case 4->h.append(pregunta("¿Cómo consulto mi agenda?","<p>Abre <a href=\"/agenda\">la agenda</a> y elige Semana o Mes. Usa Anterior, Hoy o Siguiente para moverte entre fechas; en el mes, «+N más» abre la semana. No se permite guardar dos citas que se crucen para un mismo Evaluador. Las notas del Administrador se leen en la reserva y se confirman con Entendido.</p>"));
    case 5->{StringBuilder riesgo=new StringBuilder("<p>El puntaje RULA va de 1 a 7; un valor más alto requiere más atención. El semáforo y el nivel escrito indican cómo actuar.</p><dl class=\"ayuda-riesgos\">");for(var v:c.riesgos())riesgo.append("<div><dt>").append(Html.riesgo(v.minimo(),v.color(),v.nivel())).append(" — ").append(v.minimo()==v.maximo()?v.minimo():v.minimo()+" a "+v.maximo()).append("</dt><dd>").append(escapar(v.nombre())).append(". ").append(escapar(v.accion())).append("</dd></div>");h.append(pregunta("¿Qué significan el puntaje, el color y el nivel?",riesgo.append("</dl>").toString()));}
    case 6->{boolean usuario=s!=null&&s.cuenta().rol()==Rol.USUARIO;h.append(pregunta("¿Cómo busco resultados y consulto el informe?","<p>En cada consulta, usa Buscar, los filtros y los títulos de las columnas para ordenar. Abre <a href=\"/empresas\">Empresas</a> y elige Ver calificaciones para consultar sus resultados. "+(usuario?"Pídele el informe imprimible al Administrador o al Evaluador de tu empresa.":"Desde la empresa, elige Generar informe y luego Imprimir o guardar PDF; en el cuadro de imprimir, desmarca Encabezados y pies de página.")+"</p>"));}
    case 7->h.append(pregunta("¿Cómo se guardan las fotos y quién puede verlas?","<p>Confirma primero que la persona firmó el consentimiento físico y quedó archivado. Con registro fotográfico se admiten de 1 a 10 fotos JPG o PNG, de hasta 5 MB cada una, con su vista y descripción. «Sin registro fotográfico» conserva el contexto sin fotos ni puntaje. Solo el Administrador y el Evaluador autorizado pueden verlas; el Usuario nunca.</p>"));
    case 8->{h.append(pregunta("¿Por qué no veo una empresa o cambió mi acceso?","<p>Puede que no esté asignada a tu cuenta o que el Administrador haya cambiado tus permisos. Vuelve a iniciar sesión y, si sigue igual, pídele que revise tus asignaciones.</p>"));h.append(pregunta("¿Qué hago si la cámara o la IA no funcionan?","<p>Si la cámara no abre, usa Subir foto. Puedes volver a intentar Detectar puntos; si no los encuentra, elige Calcular manualmente y sigue las instrucciones sobre la foto. Revisa siempre los puntos y las respuestas antes de guardar.</p>"));h.append(pregunta("¿Cómo quito los encabezados del navegador en el PDF?","<p>En el cuadro de imprimir, desmarca Encabezados y pies de página. El nivel de riesgo queda escrito para que el informe se entienda también en blanco y negro.</p>"));}
    case 9->h.append(pregunta("¿Cómo creo cuentas y asigno empresas?","<p>Abre <a href=\"/usuarios\">Usuarios</a> y elige Nuevo usuario. Elige el rol y las empresas: Usuario necesita al menos una y Evaluador puede quedar sin ninguna; Administrador tiene acceso a todas. Comunica la contraseña temporal por un canal privado y la persona deberá cambiarla al entrar. Desactivar conserva el historial; no puedes dejar al sistema sin ningún Administrador activo.</p>"));
    case 11->h.append(durante());
    case 12->h.append(pregunta("¿Qué encuentro en Archivo?", "<p>Elige una empresa y despliega su sede y salón. Ver historial reúne las evaluaciones del puesto. El Evaluador conserva su autoría aunque cambie la asignación; el Usuario solo ve visitas Finalizadas.</p>"));
    case 13->h.append(acerca());
    case 10->h.append("<div data-ayuda-pregunta>").append(contacto(c.contacto())).append("</div>");
    default->throw new IllegalArgumentException("Tema de ayuda no disponible.");
   }
   h.append("</section>");
  }
  h.append("</div><p class=\"muted\">¿No encontraste tu respuesta? Escríbele al Administrador de tu cuenta o usa el contacto de arriba.</p><script src=\"/static/js/ayuda.js\" defer></script>");return Html.pagina("Ayuda",h.toString(),s,false,false);
 }
 private static String indice(Contenido contenido) {
  StringBuilder html=new StringBuilder("<nav class=\"ayuda-indice\" aria-label=\"Temas de ayuda\">");
  String[] nombres={"", "Entrar", "Roles", "Evaluación", "Agenda", "Resultados", "Consultas",
      "Fotos", "Problemas", "Cuentas", "Contacto", "Durante la evaluación", "Archivo", "Acerca de ALINEO."};
  for(int tema:java.util.List.of(1,2,3,11,4,5,12,7,8,9,13,10)) {
   if(!contenido.temas().contains(tema))continue;
   html.append("<a class=\"card\" href=\"#ayuda-").append(tema)
       .append("\"><svg width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" ")
       .append("stroke=\"currentColor\" stroke-width=\"1.4\" aria-hidden=\"true\">")
       .append("<path d=\"M4 4h16v16H4z M8 8h8 M8 12h6 M8 16h4\"/></svg>")
       .append(Html.escapar(nombres[tema])).append("</a>");
  }
  return html.append("</nav>").toString();
 }
 private static String durante() {
  return pregunta("¿No hay internet?", "<p>La IA necesita internet para descargar su modelo. Si la app sigue accesible, usa Calcular manualmente. Las respuestas sin guardar no se conservan si cierras la página.</p>")
      +pregunta("¿La IA no detecta los puntos?", "<p>Usa Calcular manualmente. En el recorrido actual marca hombro, codo, muñeca, coronilla y cadera en el orden indicado; después revisa los ángulos. Puedes arrastrar un punto para corregirlo.</p>")
      +pregunta("¿La foto salió oscura o cortada?", "<p>Cambia la foto por una clara y completa, de perfil. No confirmes puntos que no puedas revisar.</p>")
      +pregunta("¿El trabajador no quiere fotos?", "<p>Elige Sin registro fotográfico. Se conserva el contexto, sin fotos ni puntaje.</p>")
      +pregunta("¿Se cerró la sesión en plena visita?", "<p>Vuelve a ingresar y abre la visita En curso. Las evaluaciones guardadas se conservan; el formulario sin guardar debe completarse otra vez.</p>");
 }
 private static String acerca() {
  return pregunta("¿Qué es ALINEO.?", "<p>Evaluamos posturas de trabajo con el método RULA. La app acompaña la reserva, las fotos, el cálculo y las sugerencias.</p>")
      +pregunta("¿Por qué importa?", "<p>Medir una postura permite reconocer riesgos y decidir mejoras. El resultado orienta la revisión del puesto; el evaluador confirma siempre las observaciones.</p>")
      +pregunta("¿Quiénes lo hacemos?", "<p>Laura Gabriella Romero Gómez, Johan Andrés Zapata Quintero y Andrés Eduardo Benavides. Estudiantes de Ingeniería de Software de la Corporación Universitaria Empresarial Alexander von Humboldt. Proyecto Nuclear 2026-2.</p>");
 }
 private static String pregunta(String titulo,String respuesta){return "<details class=\"guia\" data-ayuda-pregunta><summary>"+escapar(titulo)+"</summary><div class=\"ayuda-respuesta\">"+respuesta+"</div></details>";}
 private static String titulo(int n){return switch(n){case 1->"Entrar a tu cuenta";case 2->"Qué puede hacer cada rol";case 3->"Hacer una evaluación paso a paso";case 4->"Mi agenda";case 5->"Entender el resultado";case 6->"Consultar, buscar y filtrar";case 7->"Fotos y privacidad";case 8->"Problemas comunes";case 9->"Solo Administrador";case 10->"Contacto";case 11->"Durante la evaluación";case 12->"Archivo";case 13->"Acerca de ALINEO.";default->"";};}
 private static String contacto(Contacto c){var h=new StringBuilder("<h3>¿Necesitas ayuda de una persona?</h3><p class=\"marca-contacto\">ALINEO.</p>");h.append("<p>Laura Gabriella Romero Gómez · Administradora principal de ALINEO.</p>");if(!c.correo().isBlank())h.append("<p><a href=\"mailto:").append(escapar(c.correo())).append("\">").append(escapar(c.correo())).append("</a></p>");if(!c.telefono().isBlank())h.append("<p>").append(escapar(c.telefono())).append("</p>");if(!c.whatsapp().isBlank())h.append("<a class=\"btn\" href=\"https://wa.me/").append(c.whatsapp()).append("\" rel=\"noopener noreferrer\">Escribir por WhatsApp</a>");if(c.incompleto())h.append("<p class=\"hint\">Escríbele al Administrador de tu cuenta.</p>");return h.toString();}
}
