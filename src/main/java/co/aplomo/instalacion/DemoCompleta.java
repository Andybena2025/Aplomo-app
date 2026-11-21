package co.aplomo.instalacion;

import co.aplomo.rula.MotorRula.Carga;
import co.aplomo.rula.MotorRula.Entrada;
import co.aplomo.rula.MotorRula.Medicion;
import co.aplomo.rula.MotorRula.Segmento;
import co.aplomo.rula.MedidorAngulos.Lado;
import co.aplomo.rula.PuntuadorSegmentos.Ajustes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.EnumMap;
import java.util.List;

/**
 * Paso 17, RF-017/021/022/023/025: casos ficticios calculados por los servicios reales.
 */
final class DemoCompleta {
    private DemoCompleta() {
    }

    /**
     * Entradas manuales de los casos 4, 2 y 8 de docs/03; nunca se asigna un puntaje final.
     */
    static Entrada entrada(int caso) {
        double[] grados = caso == 0 || caso == 3 ? new double[]{0, 90, 0, 0, 0} : new double[]{61, 47, 0, 37, 17};
        var mediciones = new EnumMap<Segmento, Medicion>(Segmento.class);
        for (Segmento segmento : Segmento.values()) {
            mediciones.put(segmento, new Medicion(List.of(), grados[segmento.ordinal()]));
        }
        Carga carga = caso == 2 ? Carga.DE_2_A_10KG_ESTATICA_O_REPETITIVA :
                caso == 5 ? Carga.MAS_10KG_INTERMITENTE : Carga.MENOS_2KG_INTERMITENTE;
        return new Entrada(Lado.DERECHO, 200, 400, mediciones, Ajustes.sinAjustes(),
                caso == 3 || caso == 4 || caso == 5, carga);
    }

    /**
     * Integra los servicios en la transacción externa, sin confirmar ni cerrar la conexión del cargador.
     */
    static Connection sinConfirmar(Connection conexion) throws SQLException {
        Savepoint sp = conexion.setSavepoint();
        return (Connection) Proxy.newProxyInstance(DemoCompleta.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, metodo, args) -> {
                    switch (metodo.getName()) {
                        case "setAutoCommit", "commit", "close":
                            return null;
                        case "rollback":
                            conexion.rollback(sp);
                            return null;
                        default:
                            try {
                                return metodo.invoke(conexion, args);
                            } catch (InvocationTargetException error) {
                                throw error.getCause();
                            }
                    }
                });
    }
}
