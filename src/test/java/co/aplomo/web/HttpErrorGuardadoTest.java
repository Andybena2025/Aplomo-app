package co.aplomo.web;

import co.aplomo.dao.*;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.servicio.Usuarios;
import co.aplomo.soporte.HttpPrueba;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** RNF-007: simula un fallo SQL al guardar sin perder el formulario ni revelar detalles. */
class HttpErrorGuardadoTest extends HttpPrueba {
    @Override protected Usuarios servicioUsuarios() {
        CuentaDao fallaAlCrear=new CuentaDao() {
            @Override public int crear(Connection c,String nombre,String correo,String telefono,String cargo,Rol rol,Integer empresa,String hash) throws SQLException {
                throw new SQLException("detalle SQL privado que nunca debe aparecer");
            }
        };
        return new Usuarios(tx,fallaAlCrear,new EmpresaDao());
    }

    @Test void conservaCamposYNoExponeClaveNiDetallesDelError() throws Exception {
        entrar(admin); var pagina=get("/usuarios/nuevo");
        var r=post("/usuarios",Map.of("csrf",csrf(pagina),"nombre","Nombre conservado","correo","fallo@prueba.invalid",
                "telefono","123","cargo","Contacto","rol","USUARIO","empresa",Integer.toString(empresa),"temporal","Temporal2026"));
        assertEquals(503,r.statusCode()); assertTrue(r.body().contains("Nombre conservado"));
        assertTrue(r.body().contains("fallo@prueba.invalid")); assertTrue(r.body().contains("Tus datos siguen en el formulario"));
        assertFalse(r.body().contains("detalle SQL privado")); assertFalse(r.body().contains("value=\"Temporal2026\""));
        assertNull(dao.porCorreo(con,"fallo@prueba.invalid"));
    }
}
