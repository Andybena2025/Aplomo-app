package co.aplomo.rula;

import java.sql.SQLException;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** RF-027, RNF-010: tablas incompletas nunca pueden producir resultados aparentemente válidos. */
class TablasRulaTest {
    @Test
    void cargaUnaSolaVezCierraRecursosYConservaConexionAjena() throws Exception {
        var jdbc = new JdbcDePrueba();
        var tablas = new TablasRula();
        var con = jdbc.conexion();
        tablas.cargar(con);
        tablas.cargar(con);
        assertEquals(4,jdbc.consultas);
        assertEquals(4,jdbc.sentenciasCerradas);
        assertEquals(4,jdbc.resultadosCerrados);
        assertFalse(con.isClosed());
        for (var c : DatosPrueba.celdas()) assertEquals(c.valor(),DatosPrueba.consultar(tablas,c));
    }

    @Test
    void nivelesYTextosSeObtienenDeLosDatosNoDelCodigo() throws Exception {
        var jdbc = new JdbcDePrueba();
        jdbc.niveles.get(0).put("nombre","Nombre de prueba");
        jdbc.niveles.get(0).put("accion","Acción de prueba");
        jdbc.niveles.get(0).put("puntaje_maximo",3);
        jdbc.niveles.get(1).put("puntaje_minimo",4);
        var tablas = new TablasRula();
        tablas.cargar(jdbc.conexion());
        assertEquals("Nombre de prueba",tablas.nivelDe(3).nombre());
        assertEquals("Acción de prueba",tablas.nivelDe(3).accion());
        assertEquals("VERDE",tablas.nivelDe(3).color());
    }

    @ParameterizedTest
    @ValueSource(strings={"A","B","C"})
    void rechazaTablasIncompletasYPermiteReintento(String nombre) throws Exception {
        var jdbc = new JdbcDePrueba();
        var celda = jdbc.valores.stream().filter(v -> v.get("tabla").equals(nombre)).findFirst().orElseThrow();
        jdbc.valores.remove(celda);
        var tablas = new TablasRula();
        assertThrows(SQLException.class, () -> tablas.cargar(jdbc.conexion()));
        assertThrows(IllegalStateException.class, () -> tablas.buscarA(1,1,1,1));
        assertThrows(IllegalStateException.class, () -> tablas.nivelDe(1));
        assertEquals(jdbc.consultas,jdbc.sentenciasCerradas);
        assertEquals(jdbc.consultas,jdbc.resultadosCerrados);
        jdbc.valores.add(celda);
        tablas.cargar(jdbc.conexion());
        assertEquals(1,tablas.buscarA(1,1,1,1));
    }

    @ParameterizedTest
    @ValueSource(strings={"filaCero","filaExcesiva","columnaCero","columnaExcesiva","valorCero","valorExcesivo","duplicado","cMayor7"})
    void rechazaCeldasInvalidas(String fallo) throws Exception {
        var jdbc = new JdbcDePrueba();
        var celda = jdbc.valores.get(0);
        switch (fallo) {
            case "filaCero" -> celda.put("fila",0);
            case "filaExcesiva" -> celda.put("fila",19);
            case "columnaCero" -> celda.put("columna",0);
            case "columnaExcesiva" -> celda.put("columna",9);
            case "valorCero" -> celda.put("valor",0);
            case "valorExcesivo" -> celda.put("valor",10);
            case "duplicado" -> jdbc.valores.add(new HashMap<>(celda));
            case "cMayor7" -> jdbc.valores.stream().filter(v -> v.get("tabla").equals("C"))
                    .findFirst().orElseThrow().put("valor",8);
            default -> throw new AssertionError();
        }
        assertThrows(SQLException.class, () -> new TablasRula().cargar(jdbc.conexion()));
    }

    @ParameterizedTest
    @ValueSource(strings={"sinNiveles","hueco","superpuesto","minimoCero","maximoOcho","invertido","oficialCero","oficialCinco","color","nombre","accion","id"})
    void rechazaNivelesInvalidos(String fallo) throws Exception {
        var jdbc = new JdbcDePrueba();
        var n = jdbc.niveles.get(0);
        switch (fallo) {
            case "sinNiveles" -> jdbc.niveles.clear();
            case "hueco" -> n.put("puntaje_maximo",1);
            case "superpuesto" -> n.put("puntaje_maximo",3);
            case "minimoCero" -> n.put("puntaje_minimo",0);
            case "maximoOcho" -> n.put("puntaje_maximo",8);
            case "invertido" -> n.put("puntaje_minimo",3);
            case "oficialCero" -> n.put("nivel_oficial_rula",0);
            case "oficialCinco" -> n.put("nivel_oficial_rula",5);
            case "color" -> n.put("color","AZUL");
            case "nombre" -> n.put("nombre","");
            case "accion" -> n.put("accion",null);
            case "id" -> n.put("id_nivel",0);
            default -> throw new AssertionError();
        }
        var tablas = new TablasRula();
        assertThrows(SQLException.class, () -> tablas.cargar(jdbc.conexion()));
        assertThrows(IllegalStateException.class, () -> tablas.buscarC(1,1));
    }

    @Test
    void validaEntradasAntesDeConsultarIndices() throws Exception {
        var tablas = new TablasRula();
        assertThrows(IllegalStateException.class, () -> tablas.buscarB(1,1,1));
        tablas.cargar(new JdbcDePrueba().conexion());
        for (int fuera : new int[]{-1,0,Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(fuera,1,1,1));
            assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(1,fuera,1,1));
            assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(1,1,fuera,1));
            assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(1,1,1,fuera));
            assertThrows(IllegalArgumentException.class, () -> tablas.buscarB(fuera,1,1));
            assertThrows(IllegalArgumentException.class, () -> tablas.buscarB(1,fuera,1));
            assertThrows(IllegalArgumentException.class, () -> tablas.buscarB(1,1,fuera));
            assertThrows(IllegalArgumentException.class, () -> tablas.nivelDe(fuera));
        }
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(7,1,1,1));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(1,4,1,1));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(1,1,5,1));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarA(1,1,1,3));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarB(7,1,1));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarB(1,7,1));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarB(1,1,3));
        assertThrows(IllegalArgumentException.class, () -> tablas.nivelDe(8));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarC(0,1));
        assertThrows(IllegalArgumentException.class, () -> tablas.buscarC(1,0));
        assertEquals(tablas.buscarC(8,7),tablas.buscarC(Integer.MAX_VALUE,Integer.MAX_VALUE));
        assertEquals(tablas.buscarC(8,1),tablas.buscarC(9,1));
        assertEquals(tablas.buscarC(1,7),tablas.buscarC(1,8));
    }
}
