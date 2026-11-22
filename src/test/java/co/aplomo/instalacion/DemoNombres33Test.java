package co.aplomo.instalacion;

import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;
import co.aplomo.servicio.Avisos;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class DemoNombres33Test extends BasePrueba {
    @TempDir
    Path fotos;

    @Test
    void nombresPorRolAceptanAliasAnterioresYSeSigueIngresandoConLaClaveDePrueba() throws Exception {
        Demo.cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"), fotos);
        int visitas = Sql.uno(con, "SELECT COUNT(*) FROM visita", f -> f.getInt(1));
        int evaluaciones = Sql.uno(con, "SELECT COUNT(*) FROM evaluacion", f -> f.getInt(1));
        Sql.actualizar(con, "UPDATE bitacora SET detalle='DEMO-32-PLAN' " +
                "WHERE tabla='empresa' AND detalle='DEMO-33-PLAN'");
        Demo.cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"), fotos);
        assertEquals(visitas, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM visita", f -> f.getInt(1)));
        assertEquals(evaluaciones, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion", f -> f.getInt(1)));
        String[] correos = {"gabby@rula.local", "gabriella.evaluadora@rula.local", "gabriella.usuaria@rula.local"};
        String[] nombres = {"Laura Romero", "Ing. Gabriela Romero", "Rep. Laura Gabriela Romero"};
        String[] viejos = {"Laura Gabriella Romero", "Ing. Laura Gabriella Romero Gómez", "Rep. Laura Romero"};
        for (int i = 0; i < correos.length; i++) {
            int id = auth.ingresar(correos[i], "Cambiar2026").id();
            assertEquals(nombres[i],
                    Sql.uno(con, "SELECT nombre FROM cuenta WHERE id_cuenta=?", f -> f.getString(1), id));
            Sql.actualizar(con, "UPDATE cuenta SET nombre=? WHERE id_cuenta=?", viejos[i], id);
            Demo.cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"), fotos);
            assertEquals(id, auth.ingresar(correos[i], "Cambiar2026").id());
            assertEquals(nombres[i],
                    Sql.uno(con, "SELECT nombre FROM cuenta WHERE id_cuenta=?", f -> f.getString(1), id));
        }
        assertEquals("Coordinadora de evaluaciones", Sql.uno(con,
                "SELECT cargo FROM cuenta WHERE correo=?", f -> f.getString(1), correos[0]));
        assertEquals("Ingeniera Industrial · Evaluadora RULA", Sql.uno(con,
                "SELECT cargo FROM cuenta WHERE correo=?", f -> f.getString(1), correos[1]));
        assertEquals("gabby@rula.local", new Avisos(tx, reloj).principal(cuenta(admin)).correo());
    }
}
