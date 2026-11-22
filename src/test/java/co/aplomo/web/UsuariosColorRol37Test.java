package co.aplomo.web;

import co.aplomo.dao.EmpresaDao.Opcion;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Estado;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.seguridad.Sesiones.Sesion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-003, RNF-007: en Usuarios cada rol tiene su color y las cuentas salen agrupadas por rol.
 */
class UsuariosColorRol37Test {
    private static Cuenta cuenta(int id, String nombre, Rol rol) {
        return new Cuenta(id, null, nombre, "c" + id + "@rula.local", "", "", "x", false, 0, null, Estado.ACTIVA, rol);
    }

    @Test
    void cadaRolTieneSuPildoraDeColorYLasCuentasSalenAgrupadas() {
        var cuentas = List.of(cuenta(1, "Admin Uno", Rol.ADMINISTRADOR), cuenta(2, "Rep. Usuario", Rol.USUARIO),
                cuenta(3, "Ing. Evaluador", Rol.EVALUADOR), cuenta(4, "Admin Dos", Rol.ADMINISTRADOR));
        var sesion = new Sesion("s", "t", cuentas.get(0), Instant.now());
        String html = TablaUsuarios.pagina(sesion, cuentas, List.of(new Opcion(1, "Empresa", true)),
                Map.of(2, List.of(1), 3, List.of(1)), null);
        for (String clase : List.of("rol-pill-administrador", "rol-pill-evaluador", "rol-pill-usuario"))
            assertTrue(html.contains("class=\"rol-pill " + clase + "\""), clase);
        assertTrue(html.contains("<i class=\"rol-punto\" aria-hidden=\"true\"></i>Administrador"));
        int adminDos = html.indexOf("Admin Dos"), evaluador = html.indexOf("Ing. Evaluador"),
                usuario = html.indexOf("Rep. Usuario");
        assertTrue(adminDos < evaluador && evaluador < usuario, "Administradores, luego Evaluadores, luego Usuarios");
    }
}
