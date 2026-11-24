package co.aplomo.db;

import co.aplomo.soporte.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** RF-014/RNF-009: repetición aditiva en pruebas; no elimina ni recrea tablas. */
class Migracion40Test extends BasePrueba {
    @org.junit.jupiter.api.io.TempDir Path fotos;

    @Test
    void dosEjecucionesConservanColumnasYReemplazanSoloTextoExacto() throws Exception {
        var e = new EscenarioConsultas(this, fotos);
        String viejo = "Usar herramientas y sujeciones que permitan mantener la muñeca recta.";
        String nuevo = "Usar herramientas y soportes que dejen la muñeca recta " +
                "(por ejemplo, una prensa para sujetar la pieza).";
        int regla = Sql.insertar(con, "INSERT INTO regla_sugerencia(segmento,puntaje_minimo,condicion," +
                "texto_sugerido) VALUES('MUNECA',99,'SIEMPRE',?)", viejo);
        int personalizada = Sql.insertar(con, "INSERT INTO regla_sugerencia(segmento,puntaje_minimo,condicion," +
                "texto_sugerido) VALUES('MUNECA',99,'SIEMPRE',?)", viejo + " Texto editado.");
        Sql.actualizar(con, "UPDATE sugerencia SET descripcion=? WHERE id_sugerencia=?", viejo, e.sugerenciaAbierta);
        String script = Files.readString(Path.of("docs/base_de_datos/migracion_paso40.sql"));
        int tablas = Sql.uno(con, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()",
                f -> f.getInt(1));
        for (int intento = 0; intento < 2; intento++) {
            for (String sql : script.split(";")) if (!sql.isBlank())
                try (var ps = con.prepareStatement(sql)) { ps.execute(); }
            assertEquals(nuevo, Sql.uno(con, "SELECT texto_sugerido FROM regla_sugerencia WHERE id_regla=?",
                    f -> f.getString(1), regla));
            assertEquals(nuevo, Sql.uno(con, "SELECT descripcion FROM sugerencia WHERE id_sugerencia=?",
                    f -> f.getString(1), e.sugerenciaAbierta));
        }
        assertEquals(viejo + " Texto editado.", Sql.uno(con,
                "SELECT texto_sugerido FROM regla_sugerencia WHERE id_regla=?", f -> f.getString(1), personalizada));
        assertThrows(java.sql.SQLException.class, () -> Sql.actualizar(con,
                "UPDATE evaluacion SET trabajador_edad=13 WHERE id_evaluacion=?", e.evalAbierta));
        assertEquals(tablas, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()", f -> f.getInt(1)));
        assertEquals(3, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema=DATABASE() AND table_name='evaluacion' AND column_name LIKE 'trabajador_%'",
                f -> f.getInt(1)));
        assertFalse(Files.readString(Path.of("docs/base_de_datos/datos_sugerencias.sql")).contains(viejo));
    }
}
