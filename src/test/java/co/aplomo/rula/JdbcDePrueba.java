package co.aplomo.rula;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

/** Doble JDBC pequeño hecho con el JDK: permite simular datos dañados sin escribir en MySQL. */
final class JdbcDePrueba {
    final List<Map<String, Object>> valores = new ArrayList<>();
    final List<Map<String, Object>> niveles = new ArrayList<>();
    int consultas, sentenciasCerradas, resultadosCerrados;
    boolean conexionCerrada;

    JdbcDePrueba() throws Exception {
        for (var c : DatosPrueba.celdas()) {
            valores.add(new HashMap<>(Map.of("tabla",c.tabla(),"fila",c.fila(),"columna",c.columna(),"valor",c.valor())));
        }
        nivel(1,"VERDE",1,2,"Verde","No necesita cambios",1);
        nivel(2,"AMARILLO",3,4,"Amarillo","Hacer una revisión",2);
        nivel(3,"ROJO",5,6,"Rojo","Intervención inmediata",3);
        nivel(4,"ROJO",7,7,"Rojo","Intervención inmediata",4);
    }

    private void nivel(int id, String color, int min, int max, String nombre, String accion, int oficial) {
        niveles.add(new HashMap<>(Map.of("id_nivel",id,"color",color,"puntaje_minimo",min,"puntaje_maximo",max,
                "nombre",nombre,"accion",accion,"nivel_oficial_rula",oficial)));
    }

    Connection conexion() {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (p,m,args) -> switch (m.getName()) {
                    case "prepareStatement" -> sentencia((String) args[0]);
                    case "close" -> { conexionCerrada = true; yield null; }
                    case "isClosed" -> conexionCerrada;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }

    private PreparedStatement sentencia(String sql) {
        if (!sql.startsWith("SELECT ")) throw new AssertionError("Las pruebas solo permiten SELECT.");
        String[] parametro = new String[1];
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (p,m,args) -> switch (m.getName()) {
                    case "setString" -> {
                        if (!args[0].equals(1)) throw new AssertionError("Índice de parámetro incorrecto.");
                        parametro[0] = (String) args[1]; yield null;
                    }
                    case "executeQuery" -> {
                        consultas++;
                        if (sql.contains("valor_tabla_rula")) {
                            if (parametro[0] == null) throw new AssertionError("Falta parametrizar la tabla.");
                            yield resultado(valores.stream().filter(v -> v.get("tabla").equals(parametro[0])).toList());
                        }
                        yield resultado(niveles);
                    }
                    case "close" -> { sentenciasCerradas++; yield null; }
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }

    private ResultSet resultado(List<Map<String,Object>> filas) {
        int[] indice = {-1};
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                (p,m,args) -> switch (m.getName()) {
                    case "next" -> ++indice[0] < filas.size();
                    case "getInt", "getString" -> filas.get(indice[0]).get((String) args[0]);
                    case "close" -> { resultadosCerrados++; yield null; }
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }
}
