package co.aplomo.instalacion;

import co.aplomo.dao.CuentaDao;
import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Contrasenas;
import co.aplomo.servicio.ColoresAgenda;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-002/033: seis cuentas ficticias del equipo, sin alterar sus administradores reales.
 */
final class DemoRoles {
    private DemoRoles() {
    }

    static void cargar(Connection conexion, Path fotos, int administrador) throws SQLException, IOException {
        LocalDate presentacion = LocalDate.parse(System.getProperty("aplomo.demo.fecha", "2026-11-27"));
        List<Integer> empresas = DemoPresentacion.empresas(conexion, administrador);
        desactivarGenericas(conexion, administrador);
        List<Integer> evaluadores = new ArrayList<>();
        String[] nombres = {"Andrés Benavides", "Johan Zapata", "Laura Gabriella Romero"};
        String[] correos = {"andres", "johan", "gabriella"};
        int[][] asignaciones = {{2, 1}, {0, 3}, {4, 5}};
        int[] empresasUsuario = {4, 2, 0};
        for (int indice = 0; indice < nombres.length; indice++) {
            String sufijo = indice == 2 ? ".evaluadora@rula.local" : ".evaluador@rula.local";
            int evaluador = cuenta(conexion, nombres[indice], correos[indice] + sufijo, Cuenta.Rol.EVALUADOR);
            List<Integer> elegidas = List.of(empresas.get(asignaciones[indice][0]),
                    empresas.get(asignaciones[indice][1]));
            new CuentaEmpresaDao().validar(conexion, evaluador, Cuenta.Rol.EVALUADOR, elegidas, true);
            new CuentaEmpresaDao().sincronizar(conexion, administrador, evaluador, elegidas, false);
            ColoresAgenda.guardar(conexion, evaluador, ColoresAgenda.PALETA.get(indice));
            evaluadores.add(evaluador);
            String usuario = correos[indice] + (indice == 2 ? ".usuaria@rula.local" : ".usuario@rula.local");
            int cuenta = cuenta(conexion, nombres[indice], usuario, Cuenta.Rol.USUARIO);
            new CuentaEmpresaDao().sincronizar(conexion, administrador, cuenta,
                    List.of(empresas.get(empresasUsuario[indice])), false);
        }
        DemoPresentacion.citas(conexion, fotos, administrador, empresas, evaluadores, presentacion);
        HorariosDemo.conciliar(conexion, administrador);
    }

    private static int cuenta(Connection conexion, String nombre, String correo, Cuenta.Rol rol) throws SQLException {
        CuentaDao cuentas = new CuentaDao();
        Cuenta existente = cuentas.porCorreo(conexion, correo);
        if (existente != null) {
            if (!existente.nombre().equals(nombre) || existente.rol() != rol) {
                throw new IllegalArgumentException(
                        "Un correo de demostración pertenece a otra cuenta. No se modificó.");
            }
            return existente.id();
        }
        return cuentas.crear(conexion, nombre, correo, "0000000000", "Demostración", rol, null,
                Contrasenas.cifrar("Cambiar2026"));
    }

    private static void desactivarGenericas(Connection conexion, int administrador) throws SQLException {
        for (String correo : List.of("evaluador1", "evaluador2", "usuario1", "usuario2")) {
            Cuenta cuenta = new CuentaDao().porCorreo(conexion, correo + "@rula.local");
            if (cuenta == null) continue;
            if (!cuenta.cargo().equals("Demostración") || cuenta.rol() == Cuenta.Rol.ADMINISTRADOR) {
                throw new IllegalArgumentException("Una cuenta genérica ya tiene otro uso. No se cambió.");
            }
            Sql.actualizar(conexion, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", cuenta.id());
            new CuentaEmpresaDao().sincronizar(conexion, administrador, cuenta.id(), List.of(), false);
        }
    }
}
