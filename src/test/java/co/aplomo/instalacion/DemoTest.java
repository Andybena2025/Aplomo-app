package co.aplomo.instalacion;

import co.aplomo.db.Sql;
import co.aplomo.soporte.BasePrueba;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Datos reales de MySQL solo en la base de pruebas, revertidos por BasePrueba.
 */
class DemoTest extends BasePrueba {
    @TempDir
    Path archivos;

    private void cargar() throws Exception {
        Demo.cargar(con, Path.of("docs/base_de_datos/datos_demo.sql"), archivos);
    }

    @Test
    void nitCoincidenteSinMarcaNoPermiteCargarNiReiniciarLaDemo() throws Exception {
        Sql.actualizar(con, "UPDATE empresa SET nit=? WHERE id_empresa=?", DemoIdentidades.NITS.get(0), empresa);
        assertThrows(IllegalArgumentException.class, this::cargar);
        assertThrows(IllegalArgumentException.class, () -> Demo.vaciar(con, Demo.CONFIRMACION));
        assertNotNull(new co.aplomo.dao.EmpresaDao().buscar(con, empresa, 0, false));
    }

    @Test
    void nombresSectoresYHorariosSonPresentablesYLasClavesExistentesSeConservan() throws Exception {
        cargar();
        var andres = dao.porCorreo(con, "andres.evaluador@rula.local");
        String hash = andres.hash();
        cargar();
        assertEquals(hash, dao.porCorreo(con, andres.correo()).hash());
        assertEquals("#FF4F9A", dao.porCorreo(con, andres.correo()).colorAgenda());
        for (String nit : DemoIdentidades.NITS) {
            var empresaDemo = Sql.uno(con, "SELECT id_empresa,nombre,sector FROM empresa WHERE nit=?",
                    fila -> List.of(fila.getString(1), fila.getString(2), fila.getString(3)), nit);
            assertFalse(empresaDemo.get(2).isBlank());
            assertFalse(empresaDemo.get(1).contains("DEMO"));
            int id = Integer.parseInt(empresaDemo.get(0));
            int horariosInvalidos = Sql.uno(con, "SELECT COUNT(*) FROM reserva r JOIN salon l USING(id_salon) " +
                            "JOIN sede s USING(id_sede) WHERE s.id_empresa=? AND r.estado<>'CANCELADA' AND " +
                            "(WEEKDAY(r.fecha)>4 OR r.hora_inicio<'08:00' OR r.hora_fin>'16:00' OR " +
                            "MINUTE(r.hora_inicio) NOT IN(0,30) OR MINUTE(r.hora_fin) NOT IN(0,30))",
                    fila -> fila.getInt(1),
                    id);
            assertEquals(0, horariosInvalidos, nit);
            int textosAntiguos = Sql.uno(con, "SELECT COUNT(*) FROM reserva r JOIN salon l USING(id_salon) " +
                    "JOIN sede s USING(id_sede) WHERE s.id_empresa=? AND " +
                    "(r.descripcion LIKE '%Demostración%' OR r.descripcion LIKE '%APLOMO%' " +
                    "OR r.descripcion LIKE 'DEMO%' OR l.nombre LIKE 'DEMO%')", fila -> fila.getInt(1), id);
            assertEquals(0, textosAntiguos, nit);
            int posturasAntiguas = Sql.uno(con, "SELECT COUNT(*) FROM evaluacion e JOIN visita v USING(id_visita) " +
                    "JOIN reserva r USING(id_reserva) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede)" + " " +
                    "WHERE s.id_empresa=? AND (e.postura LIKE 'DEMO%' " + "OR e.postura LIKE '%Demostración " +
                    "ALINEO.%')", fila -> fila.getInt(1), id);
            assertEquals(0, posturasAntiguas, nit);
        }
        assertEquals("Educación",
                Sql.uno(con, "SELECT sector FROM empresa WHERE nit='012345'", fila -> fila.getString(1)));
    }

    @Test
    void cadaEmpresaTieneSedesConAmbosTiposYUnaVisitaListaSinDuplicar() throws Exception {
        cargar();
        cargar();
        for (int i = 1; i <= 3; i++) {
            int empresaDemo = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit=?", fila2 -> fila2.getInt(1),
                    java.util.List.of("900.512.384-1", "901.638.205-4", "012345").get(i - 1));
            var sedes = Sql.listar(con, "SELECT id_sede FROM sede WHERE id_empresa=?", fila3 -> fila3.getInt(1),
                    empresaDemo);
            assertTrue(sedes.size() >= 2);
            for (int sede : sedes) {
                var tipos = Sql.listar(con,
                        "SELECT DISTINCT p.tipo FROM puesto_trabajo p JOIN salon l USING(id_salon) WH" +
                                "ERE l.id_sede=? AND p.activo=TRUE", fila4 -> fila4.getString(1), sede);
                assertTrue(tipos.containsAll(List.of("ESCRITORIO", "MESA")));
            }
            var visitas = Sql.listar(con,
                    "SELECT v.id_visita,r.id_salon FROM visita v JOIN reserva r USING(id_reserva)" +
                            " JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE s.id_empresa=" +
                            "? AND v.estado='EN_CURSO'", fila5 -> new int[]{fila5.getInt(1), fila5.getInt(2)},
                    empresaDemo);
            assertEquals(1, visitas.size());
            assertEquals(2, Sql.<Integer>uno(con,
                    "SELECT COUNT(*) FROM puesto_trabajo p WHERE p.id_salon=? AND p.activo=TRUE A" +
                            "ND NOT EXISTS(SELECT 1 FROM evaluacion e WHERE e.id_visita=? AND e.id_puesto" +
                            "=p.id_puesto)", fila6 -> fila6.getInt(1), visitas.get(0)[1], visitas.get(0)[0]));
        }
    }

    @Test
    void demoCompletaTieneTresColoresDosPosturasYEsIdempotente() throws Exception {
        int cuentas = Sql.uno(con, "SELECT COUNT(*) FROM cuenta", fila2 -> fila2.getInt(1));
        cargar();
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM evaluacion", fila3 -> fila3.getInt(1));
        cargar();
        assertEquals(antes, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM evaluacion", fila4 -> fila4.getInt(1)));
        assertEquals(cuentas + 9, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM cuenta", fila5 -> fila5.getInt(1)));
        for (int i = 1; i <= 3; i++) {
            String nit = java.util.List.of("900.512.384-1", "901.638.205-4", "012345").get(i - 1);
            var colores = Sql.listar(con,
                    "SELECT DISTINCT n.color FROM analisis_rula a JOIN fotografia f USING(id_foto" +
                            ") JOIN evaluacion e USING(id_evaluacion) JOIN puesto_trabajo p USING(id_pues" +
                            "to) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa em " +
                            "USING(id_empresa) JOIN nivel_riesgo n USING(id_nivel) WHERE em.nit=?",
                    fila6 -> fila6.getString(1), nit);
            assertTrue(colores.containsAll(List.of("VERDE", "AMARILLO", "ROJO")), colores.toString());
            assertTrue(Sql.<Integer>uno(con,
                    "SELECT COUNT(*) FROM visita v JOIN reserva r USING(id_reserva) JOIN salon l " +
                            "USING(id_salon) JOIN sede s USING(id_sede) JOIN empresa em USING(id_empresa)" +
                            " WHERE em.nit=? AND v.estado='FINALIZADA' AND v.calificacion_final IS NOT NU" + "LL",
                    fila7 -> fila7.getInt(1), nit) > 0);
        }
        int dobles = Sql.uno(con, "SELECT COUNT(*) FROM (SELECT p.id_puesto FROM evaluacion e JOIN puesto_traba" +
                "jo p USING(id_puesto) JOIN salon l USING(id_salon) JOIN sede s USING(id_sede" +
                ") JOIN empresa em USING(id_empresa) WHERE em.nit IN ('900.512.384-1','901.638.205-4','012345') GROUP" +
                " BY p.id_puesto HAVING COUNT(DISTINCT e.cr" + "iterio)=2) q", fila8 -> fila8.getInt(1));
        assertTrue(dobles >= 3);
        assertTrue(Files.list(archivos).count() >= 18);
    }

    @Test
    void sinConfirmacionNoHaceNingunaConsultaDeBorrado() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Demo.vaciar(con, "sí"));
        assertNotNull(cuenta(admin));
        assertNotNull(cuenta(usuario));
    }

    @Test
    void cuentaVinculadaCancelaAntesDeBorrarYNoTocaCuentas() throws Exception {
        cargar();
        int demo = Sql.uno(con, "SELECT id_empresa FROM empresa WHERE nit='900.512.384-1'", fila2 -> fila2.getInt(1));
        Sql.actualizar(con, "UPDATE cuenta SET id_empresa=? WHERE id_cuenta=?", demo, usuario);
        int antes = Sql.uno(con, "SELECT COUNT(*) FROM empresa", fila3 -> fila3.getInt(1));
        var error = assertThrows(IllegalArgumentException.class, () -> Demo.vaciar(con, Demo.CONFIRMACION));
        assertTrue(error.getMessage().contains("cuentas vinculadas"));
        assertEquals(antes, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa", fila4 -> fila4.getInt(1)));
        assertEquals(demo, cuenta(usuario).empresa());
        assertNotNull(cuenta(admin));
    }

    @Test
    void usuarioDeAppNoEsAdministradorMysql() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Demo.verificarAdministrador(con));
    }

    @Test
    void limpiezaSoloRetiraFotosEnumeradasSinReferenciasYDentroDeLaCarpeta() throws Exception {
        Path fotos = Files.createDirectory(archivos.resolve("fotos"));
        for (String nombre : List.of("vieja.jpg", "compartida.jpg", "ajena.jpg"))
            Files.writeString(fotos.resolve(nombre), "ficticio");
        Path fuera = archivos.resolve("fuera.jpg");
        Files.writeString(fuera, "ajena");
        Files.createSymbolicLink(fotos.resolve("enlace.jpg"), fuera);
        var salon = crearSalon();
        int reserva = new co.aplomo.servicio.Reservas(tx, reloj).guardar(evaluadorTrabajo((solicitud(salon)).empresa()),
                0, solicitud(salon));
        Sql.actualizar(con, "UPDATE reserva SET estado='EN_COLA' WHERE id_reserva=?", reserva);
        int visita = Sql.insertar(con, "INSERT INTO visita(id_reserva,hora_real_inicio) VALUES (?,NOW())", reserva);
        int ev = evaluacion(visita, salon.id(), 1);
        int foto = foto(ev);
        Sql.actualizar(con, "UPDATE fotografia SET ruta=? WHERE id_foto=?", "compartida.jpg", foto);
        assertEquals(1, Demo.limpiarFotos(con, fotos,
                List.of("vieja.jpg", "compartida.jpg", "../fuera.jpg", fuera.toString(), "enlace.jpg")));
        assertFalse(Files.exists(fotos.resolve("vieja.jpg")));
        assertTrue(Files.exists(fotos.resolve("compartida.jpg")));
        assertTrue(Files.exists(fotos.resolve("ajena.jpg")));
        assertTrue(Files.exists(fuera));
        assertTrue(Files.isSymbolicLink(fotos.resolve("enlace.jpg")));
    }

    @Test
    void codigoReservadoEnEmpresaAjenaSeRechazaSinCargarDemo() throws Exception {
        var salon = crearSalon();
        int reserva = new co.aplomo.servicio.Reservas(tx, reloj).guardar(evaluadorTrabajo((solicitud(salon)).empresa()),
                0, solicitud(salon));
        Sql.actualizar(con, "UPDATE reserva SET codigo='DEMO-FIN-01' WHERE id_reserva=?", reserva);
        assertThrows(IllegalArgumentException.class, this::cargar);
        assertEquals(0, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM empresa WHERE nit='900.512.384-1'",
                fila2 -> fila2.getInt(1)));
    }

    @Test
    void rolesDemoAsignacionesNotasYResultadosPropiosNoSeDuplican() throws Exception {
        cargar();
        var ce = new co.aplomo.dao.CuentaEmpresaDao();
        var consultas = new co.aplomo.servicio.Consultas(tx);
        int total = Sql.uno(con, "SELECT COUNT(*) FROM cuenta_empresa", fila2 -> fila2.getInt(1));
        cargar();
        assertEquals(total, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM cuenta_empresa", fila3 -> fila3.getInt(1)));
        for (String prefijo : List.of("andres", "johan", "gabriella")) {
            String correo =
                    prefijo + (prefijo.equals("gabriella") ? ".evaluadora@rula.local" : ".evaluador@rula.local");
            var evaluador = dao.porCorreo(con, correo);
            assertEquals(co.aplomo.modelo.Cuenta.Rol.EVALUADOR, evaluador.rol());
            assertTrue(evaluador.temporal());
            assertTrue(co.aplomo.seguridad.Contrasenas.verificar("Cambiar2026", evaluador.hash()));
            assertEquals(2, ce.ids(con, evaluador.id()).size());
            dao.contrasena(con, evaluador.id(), evaluador.hash(), false);
            assertFalse(consultas.propias(cuenta(evaluador.id()), java.util.Map.of()).filas().isEmpty());
        }
        assertEquals(1, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM reserva WHERE codigo='V2620261127' " +
                "AND nota_administrador IS NULL AND estado='EN_COLA'", fila -> fila.getInt(1)));
        assertEquals(0, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM cuenta_empresa e JOIN cuenta_empresa u " +
                "ON e.id_empresa=u.id_empresa JOIN cuenta ce ON ce.id_cuenta=e.id_cuenta " +
                "JOIN cuenta cu ON cu.id_cuenta=u.id_cuenta WHERE e.activo=TRUE AND u.activo=TRUE " +
                "AND ce.rol='EVALUADOR' AND cu.rol='USUARIO' " +
                "AND SUBSTRING_INDEX(ce.correo,'.',1)=SUBSTRING_INDEX(cu.correo,'.',1)", fila -> fila.getInt(1)));
        assertEquals(0, Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM reserva a JOIN salon la ON la.id_salon=a.id_salon " +
                        "JOIN sede sa ON sa.id_sede=la.id_sede JOIN cuenta_empresa ca ON ca.id_empres" +
                        "a=sa.id_empresa " +
                        "AND ca.activo=TRUE JOIN cuenta qa ON qa.id_cuenta=ca.id_cuenta AND qa.rol='E" + "VALUADOR' " +
                        "AND qa.estado='ACTIVA' JOIN cuenta_empresa cb ON cb.id_cuenta=ca.id_cuenta A" +
                        "ND cb.activo=TRUE " +
                        "JOIN sede sb ON sb.id_empresa=cb.id_empresa JOIN salon lb ON lb.id_sede=sb.i" + "d_sede " +
                        "JOIN reserva b ON b.id_salon=lb.id_salon AND b.id_reserva>a.id_reserva AND b" +
                        ".fecha=a.fecha " + "AND b.hora_inicio<a.hora_fin AND b.hora_fin>a.hora_inicio " +
                        "WHERE a.estado<>'CANCELADA' AND b.estado<>'CANCELADA'", fila -> fila.getInt(1)));
        assertEquals(0, Sql.<Integer>uno(con, "SELECT COUNT(*) FROM reserva a JOIN reserva b " +
                        "ON a.id_administrador=b.id_administrador AND a.fecha=b.fecha AND a.id_reserv" + "a<b" +
                        ".id_reserva " + "AND a.hora_inicio<b.hora_fin AND b.hora_inicio<a.hora_fin WHERE a.codigo " + "LIK" +
                        "E 'D26%' " + "AND b.codigo LIKE 'D26%' AND a.estado<>'CANCELADA' AND b.estado<>'CANCELADA'",
                fila -> fila.getInt(1)));
    }

    @Test
    void demoDesactivaInicialSoloConTresAdminsActivosYConservaSuClave() throws Exception {
        var existente = dao.porCorreo(con, "admin@rula.local");
        int inicial =
                existente == null ? dao.crear(con, "Respaldo inicial", "admin@rula.local", "3005556789", "Instalación",
                        co.aplomo.modelo.Cuenta.Rol.ADMINISTRADOR, null,
                        co.aplomo.seguridad.Contrasenas.cifrar(clave)) : existente.id();
        Sql.actualizar(con, "UPDATE cuenta SET estado='ACTIVA' WHERE id_cuenta=?", inicial);
        String hash = cuenta(inicial).hash();
        cargar();
        assertEquals(co.aplomo.modelo.Cuenta.Estado.INACTIVA, cuenta(inicial).estado());
        assertEquals(hash, cuenta(inicial).hash());
        int mensajes = Sql.uno(con, "SELECT COUNT(*) FROM mensaje", f -> f.getInt(1));
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", admin);
        cargar();
        assertEquals(Integer.valueOf(mensajes), Sql.<Integer>uno(con, "SELECT COUNT(*) FROM mensaje", f -> f.getInt(1)));
        assertEquals(co.aplomo.modelo.Cuenta.Estado.INACTIVA, cuenta(inicial).estado());
        for (String correo : List.of("andres@rula.local", "johan@rula.local", "gabby@rula.local")) {
            assertEquals(co.aplomo.modelo.Cuenta.Estado.ACTIVA, dao.porCorreo(con, correo).estado());
        }
        int gabby = dao.porCorreo(con, "gabby@rula.local").id();
        Sql.actualizar(con, "UPDATE cuenta SET estado='INACTIVA' WHERE id_cuenta=?", gabby);
        Sql.actualizar(con, "UPDATE cuenta SET estado='ACTIVA' WHERE id_cuenta=?", inicial);
        assertThrows(IllegalArgumentException.class, this::cargar);
        assertEquals(co.aplomo.modelo.Cuenta.Estado.ACTIVA, cuenta(inicial).estado());
        assertEquals(hash, cuenta(inicial).hash());
    }

}
