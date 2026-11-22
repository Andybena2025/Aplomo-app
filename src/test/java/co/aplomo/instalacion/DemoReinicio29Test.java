package co.aplomo.instalacion;

import co.aplomo.dao.CuentaDao;
import co.aplomo.db.Conexion;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.Contrasenas;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RNF-009/011: reinicio real con permisos de instalación, solo en la base de PRUEBAS y con rollback.
 */
class DemoReinicio29Test {
    @TempDir
    Path fotos;

    @Test
    void reinicioConservaUsuarioClaveYAsignacionPorNitConIdsNuevos() throws Exception {
        String url;
        try (var lectura = Conexion.obtener()) {
            url = lectura.getMetaData().getURL();
        }
        assertTrue(url.matches(".*[/]rula_ergonomia_pruebas(?:[?].*)?"), "Solo base de pruebas");
        try (var con = DriverManager.getConnection(url, "root", "")) {
            con.setAutoCommit(false);
            try {
                CuentaDao inicialDao = new CuentaDao();
                int inicial = inicialDao.crear(con, "Administrador de prueba", UUID.randomUUID() + "@prueba.invalid",
                        "123", "Pruebas", Cuenta.Rol.ADMINISTRADOR, null, Contrasenas.cifrar("Prueba292026"));
                inicialDao.contrasena(con, inicial, inicialDao.porId(con, inicial, false).hash(), false);
                Demo.cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"), fotos);
                int empresa = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit=?", f -> f.getInt(1),
                        DemoIdentidades.NITS.get(0));
                CuentaDao dao = new CuentaDao();
                String hash = Contrasenas.cifrar("PruebaDemo292026");
                int usuario = dao.crear(con, "Usuario vinculado", UUID.randomUUID() + "@prueba.invalid", "123",
                        "Representante", Cuenta.Rol.USUARIO, empresa, hash);
                Sql.insertar(con, "INSERT INTO cuenta_empresa(id_cuenta,id_empresa,activo,fecha_asignacion) " +
                        "VALUES(?,?,TRUE,NOW())", usuario, empresa);
                int equipo = dao.porCorreo(con, "andres.usuario@rula.local").id();
                String nitAjeno = UUID.randomUUID().toString().substring(0, 20);
                int ajena = Sql.insertar(con, "INSERT INTO empresa(id_administrador,nombre,nit) VALUES(?,?,?)", inicial,
                        "Ajena al reinicio", nitAjeno);
                Sql.insertar(con, "INSERT INTO cuenta_empresa(id_cuenta,id_empresa,activo,fecha_asignacion) " +
                        "VALUES(?,?,TRUE,NOW())", equipo, ajena);
                int cuentas = Sql.uno(con, "SELECT COUNT(*) FROM cuenta", f -> f.getInt(1));
                Demo.reiniciar(con, Demo.CONFIRMACION, Path.of("docs/base_de_datos/datos_demo.sql"), fotos);
                int nueva = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit=?", f -> f.getInt(1),
                        DemoIdentidades.NITS.get(0));
                assertNotEquals(empresa, nueva);
                assertEquals(nueva, dao.porId(con, usuario, false).empresa());
                assertEquals(hash, dao.porId(con, usuario, false).hash());
                assertEquals(cuentas, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM cuenta", f -> f.getInt(1)));
                assertEquals(1, Sql.<Integer>uno(con,
                        "SELECT COUNT(*) FROM cuenta_empresa " + "WHERE id_cuenta=? AND id_empresa=? AND activo=TRUE",
                        f -> f.getInt(1), equipo, ajena));
                assertEquals(1, Sql.<Integer>uno(con,
                        "SELECT COUNT(*) FROM cuenta_empresa " + "WHERE id_cuenta=? AND id_empresa=? AND activo=TRUE",
                        f -> f.getInt(1), usuario, nueva));
            } finally {
                con.rollback();
            }
        }
    }
}
