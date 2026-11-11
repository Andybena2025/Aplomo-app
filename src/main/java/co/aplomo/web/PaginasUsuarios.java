package co.aplomo.web;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.*;
import co.aplomo.dao.EmpresaDao.Opcion;
import co.aplomo.seguridad.Sesiones.Sesion;
import java.util.*;

/** RF-003, RNF-002: lista y formularios del Administrador con componentes del mock. */
public final class PaginasUsuarios {
    private PaginasUsuarios() {}

    /** Lista cuentas sin mostrar hashes, claves ni contadores internos (RF-003, RNF-004). */
    public static String lista(Sesion s,List<Cuenta> cuentas,List<Opcion> empresas,String aviso) {
        Map<Integer,String> nombres=new HashMap<>(); empresas.forEach(e->nombres.put(e.id(),e.nombre()));
        StringBuilder contenido=new StringBuilder(Html.aviso(aviso,true));
        contenido.append("<div class=\"row between\"><p class=\"muted\">").append(Html.plural(cuentas.size(), "cuenta"))
                .append(" registrada").append(cuentas.size() == 1 ? "" : "s").append("</p><a class=\"btn p\" href=\"/usuarios/nuevo\">Nuevo usuario</a></div><div class=\"grid2\">");
        for(Cuenta c:cuentas) {
            contenido.append("<article class=\"card stack\"><div class=\"row between\"><h2>").append(Html.escapar(c.nombre()))
                    .append("</h2><span class=\"").append(claseEstado(c.estado())).append("\">").append(estado(c.estado())).append("</span></div><dl class=\"datos-cuenta\">")
                    .append(dato("Correo",c.correo())).append(dato("Teléfono",c.telefono())).append(dato("Cargo",c.cargo()))
                    .append(dato("Rol",c.rol()==Rol.ADMINISTRADOR ? "Administrador" : "Usuario"))
                    .append(dato("Empresa",c.empresa()==null ? "Independiente" : nombres.getOrDefault(c.empresa(),"Empresa asignada")))
                    .append(dato("Contraseña",c.temporal() ? "Cambio obligatorio al ingresar" : "Definitiva"))
                    .append("</dl><div class=\"row\"><a class=\"btn sm\" href=\"/usuarios/").append(c.id()).append("/editar\">Editar</a>")
                    .append("<form method=\"post\" action=\"/usuarios/").append(c.id())
                    .append(c.estado()==Estado.INACTIVA ? "/activar\">" : "/desactivar\">")
                    .append(Html.csrf(s)).append("<button class=\"btn sm\">")
                    .append(c.estado()==Estado.INACTIVA ? "Activar" : "Desactivar").append("</button></form></div></article>");
        }
        contenido.append("</div>"); return Html.pagina("Usuarios",contenido.toString(),s);
    }

    private static String dato(String titulo,String valor) { return "<div><dt>"+Html.escapar(titulo)+"</dt><dd>"+Html.escapar(valor)+"</dd></div>"; }
    private static String estado(Estado estado) { return switch(estado) { case ACTIVA->"Activa"; case INACTIVA->"Inactiva"; case BLOQUEADA->"Bloqueada temporalmente"; }; }
    private static String claseEstado(Estado estado) { return switch(estado) { case ACTIVA->"tag good"; case INACTIVA->"tag"; case BLOQUEADA->"tag warn"; }; }

    /** Conserva los campos no secretos cuando falla la validación; una clave nunca se vuelve a mostrar. */
    public static String formulario(Sesion s,Cuenta c,List<Opcion> empresas,Map<String,String> enviados,String error) {
        boolean nuevo=c==null;
        Map<String,String> valores=new HashMap<>();
        if(c!=null) {
            valores.put("nombre",c.nombre()); valores.put("correo",c.correo()); valores.put("telefono",c.telefono()); valores.put("cargo",c.cargo());
            valores.put("rol",c.rol().name()); valores.put("empresa",c.empresa()==null ? "" : c.empresa().toString());
        } else valores.put("rol","USUARIO");
        valores.putAll(enviados);
        String rol=valores.get("rol");
        StringBuilder form=new StringBuilder(Html.aviso(error,false));
        form.append("<form class=\"card stack cuenta-form\" method=\"post\" action=\"")
                .append(nuevo ? "/usuarios" : "/usuarios/"+c.id()+"/editar").append("\">").append(Html.csrf(s))
                .append(Html.campo("nombre","Nombre","text",valores.get("nombre"),100))
                .append(Html.campo("correo","Correo","email",valores.get("correo"),120))
                .append("<div class=\"grid2\">").append(Html.campo("telefono","Teléfono","tel",valores.get("telefono"),20))
                .append(Html.campo("cargo","Cargo","text",valores.get("cargo"),80)).append("</div>")
                .append("<div class=\"field\"><label for=\"rol\">Rol</label><select class=\"input\" id=\"rol\" name=\"rol\" required>")
                .append(opcion("ADMINISTRADOR","Administrador","ADMINISTRADOR".equals(rol)))
                .append(opcion("USUARIO","Usuario","USUARIO".equals(rol))).append("</select></div>")
                .append("<div class=\"field\" data-empresa><label for=\"empresa\">Empresa del Usuario</label><select class=\"input\" id=\"empresa\" name=\"empresa\">")
                .append(opcion("","Selecciona una empresa",valores.getOrDefault("empresa","").isEmpty()));
        for(Opcion e:empresas) {
            boolean asignada = c != null && Objects.equals(c.empresa(),e.id());
            if (e.activa() || asignada) form.append(opcion(Integer.toString(e.id()),e.nombre()+(e.activa() ? "" : " (inactiva)"),Integer.toString(e.id()).equals(valores.get("empresa"))));
        }
        form.append("</select><span class=\"hint\">El Administrador es independiente. El Usuario consulta únicamente su empresa.</span></div>");
        if(empresas.isEmpty()) form.append(Html.aviso("Todavía no hay empresas registradas. Para crear un Usuario se necesita una empresa existente.",false));
        String clave=Html.campo("temporal",nuevo ? "Contraseña temporal" : "Nueva contraseña temporal (opcional)","password","",100);
        form.append(nuevo ? clave : clave.replace(" required",""))
                .append("<p class=\"hint\">Al menos 8 caracteres, una letra y un número. Comunícala a la persona por un canal privado. ")
                .append(nuevo ? "Se exigirá cambiarla al ingresar." : "Deja el campo vacío para conservar la contraseña actual.")
                .append("</p><div class=\"row\"><button class=\"btn p\">").append(nuevo ? "Crear usuario" : "Guardar cambios")
                .append("</button><a class=\"btn\" href=\"/usuarios\">Volver a usuarios</a></div></form>");
        return Html.pagina(nuevo ? "Nuevo usuario" : "Editar usuario",form.toString(),s);
    }

    private static String opcion(String valor,String texto,boolean seleccionada) {
        return "<option value=\""+Html.escapar(valor)+"\""+(seleccionada ? " selected" : "")+">"+Html.escapar(texto)+"</option>";
    }
}
