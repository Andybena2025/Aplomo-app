package co.aplomo.web;

import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.EmpresaDao;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.servicio.Usuarios;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-007: simula un fallo SQL al guardar sin perder el formulario ni revelar detalles.
 */
class HttpErrorGuardadoTest extends HttpPrueba {
    @Override
    protected Usuarios servicioUsuarios() {
        CuentaDao fallaAlCrear = new CuentaDao() {
            @Override
            public int crear(Connection conexion, String nombre, String correo, String telefono, String cargo, Rol rol,
                    Integer empresa, String hash) throws SQLException {
                throw new SQLException("detalle SQL privado que nunca debe aparecer");
            }
        };
        return new Usuarios(tx, fallaAlCrear, new EmpresaDao());
    }

    @Test
    void conservaCamposYNoExponeClaveNiDetallesDelError() throws Exception {
        entrar(admin);
        var pagina = get("/usuarios/nuevo");
        var respuesta = post("/usuarios",
                Map.of("csrf", csrf(pagina), "nombre", "Nombre conservado", "correo", "fallo@prueba.invalid",
                        "telefono", "123", "cargo", "Contacto", "rol", "USUARIO", "empresa", Integer.toString(empresa),
                        "temporal", "Temporal2026"));
        assertEquals(503, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Nombre conservado"));
        assertTrue(respuesta.body().contains("fallo@prueba.invalid"));
        assertTrue(respuesta.body().contains("Tus datos siguen en el formulario"));
        assertFalse(respuesta.body().contains("detalle SQL privado"));
        assertFalse(respuesta.body().contains("value=\"Temporal2026\""));
        assertNull(dao.porCorreo(con, "fallo@prueba.invalid"));
    }
}
