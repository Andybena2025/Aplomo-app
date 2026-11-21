package co.aplomo.web;

import co.aplomo.soporte.CasosRulaPrueba;
import co.aplomo.soporte.EscenarioConsultas;
import co.aplomo.soporte.HttpPrueba;
import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.CuentaEmpresaDao;
import co.aplomo.dao.EvaluacionDao;
import co.aplomo.db.Esquema;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Empresa;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.servicio.Agenda;
import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Usuarios;
import co.aplomo.servicio.Visitas;
import co.aplomo.seguridad.AccesoDatos;
import co.aplomo.seguridad.Sesiones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paso 24, RF-002/003/024/033: ataques por ID y transacciones sin confirmar.
 */
class HttpTresRolesTest extends HttpPrueba {
    EscenarioConsultas escenario;
    int ev1, ev2;
    Usuarios us;
    CuentaEmpresaDao ce = new CuentaEmpresaDao();

    @BeforeEach
    void escenario() throws Exception {
        escenario = new EscenarioConsultas(this, carpetaFotos);
        us = servicioUsuarios();
        ev1 = crear(Cuenta.Rol.EVALUADOR, List.of(empresa));
        ev2 = crear(Cuenta.Rol.EVALUADOR, List.of(escenario.empresaB));
        Sql.actualizar(con, "UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) " +
                        "SET r.id_evaluador=CASE WHEN s.id_empresa=? THEN ? ELSE ? END " + "WHERE s.id_empresa IN (?,"
                        + "?)",
                empresa, ev1, ev2, empresa, escenario.empresaB);
        entrar(ev1);
    }

    void cambiarCuenta(int id) throws Exception {
        post("/logout", Map.of("csrf", csrf(get("/"))));
        entrar(id);
    }

    int crear(Cuenta.Rol rol, List<Integer> empresas) throws Exception {
        int id = us.crear(cuenta(admin),
                new Usuarios.Datos("Persona " + UUID.randomUUID(), UUID.randomUUID() + "@prueba.invalid", "123",
                        "Pruebas", rol, null, empresas), clave);
        dao.contrasena(con, id, cuenta(id).hash(), false);
        return id;
    }

    @ParameterizedTest
    @ValueSource(strings = {"empresas", "reservas", "visitas", "evaluaciones", "pendientes", "sugerencias", "puestos"})
    void listasSoloMuestranEmpresasAsignadas(String ruta) throws Exception {
        var respuesta = get("/" + ruta);
        assertEquals(200, respuesta.statusCode());
        assertFalse(respuesta.body().contains("Empresa ajena QA"));
        assertEquals(403, get("/" + ruta + "?empresa=" + escenario.empresaB).statusCode());
    }

    @Test
    void detallesFotosInformeYPostAjenosEstanBloqueados() throws Exception {
        int foto = new EvaluacionDao().fotos(con, escenario.evalB).get(0).id();
        for (String ruta : List.of("/empresas/" + escenario.empresaB, "/sedes/" + escenario.sedeB,
                "/salones/" + escenario.salonB, "/puestos/" + escenario.puestoB, "/reservas/" + escenario.reservaB,
                "/visitas/" + escenario.visitaB, "/evaluaciones/" + escenario.evalB, "/fotos/" + foto,
                "/pendientes/" + escenario.pendienteB, "/sugerencias/" + escenario.sugerenciaB,
                "/informes/empresa/" + escenario.empresaB))
            assertEquals(403, get(ruta).statusCode(), ruta);
        String csrf = csrf(get("/"));
        assertEquals(403, post("/reservas/" + escenario.reservaB + "/cancelar",
                Map.of("csrf", csrf, "motivo", "No permitido")).statusCode());
        assertThrows(SecurityException.class,
                () -> escenario.evaluaciones.analisis().autorizarParcial(cuenta(ev1), escenario.visitaB));
    }

    @Test
    void evaluadorNoAdministraEmpresasEstadosNiCuentas() throws Exception {
        String csrf = csrf(get("/"));
        for (String ruta : List.of("/usuarios", "/usuarios/nuevo", "/empresas/" + empresa + "/editar"))
            assertEquals(403, get(ruta).statusCode(), ruta);
        for (String ruta : List.of("/usuarios", "/empresas/" + empresa + "/desactivar",
                "/puestos/" + escenario.puesto1 + "/desactivar"))
            assertEquals(403, post(ruta, Map.of("csrf", csrf)).statusCode(), ruta);
        var em = new Empresas(tx);
        assertThrows(SecurityException.class, () -> em.estadoEmpresa(cuenta(ev1), empresa, false));
        int creada = em.guardar(cuenta(ev1), new Empresa(0, ev1, "Empresa propia", "OTRA", "", null, null, null, true))
                .id();
        assertEquals(ev1, em.empresa(cuenta(ev1), creada).creador());
        assertFalse(get("/empresas/" + empresa).body().contains("/desactivar"));
    }

    @Test
    void evaluadorCreaSedePuestoReservaYEvaluacion() throws Exception {
        var em = new Empresas(tx);
        int sede = em.guardar(cuenta(ev1),
                new Sede(0, empresa, "Nueva sede E", "Calle 1", "Armenia", "123", "8 a 5", "Prueba", true)).id();
        int salon = em.guardar(cuenta(ev1), new Salon(0, sede, "Sala E", "Prueba", 1, 5, true)).id();
        int puesto = em.guardar(cuenta(ev1),
                new Puesto(0, salon, null, null, 1, "Puesto E", "MESA", "Centro", null, true)).id();
        var res = new Reservas(tx, reloj);
        int id = res.guardar(cuenta(ev1), 0,
                new Reservas.Datos(empresa, sede, salon, LocalDate.now(reloj), LocalTime.of(18, 0), LocalTime.of(19, 0),
                        "EVALUACION_INICIAL", "Prueba", ""));
        res.aceptar(cuenta(ev1), id);
        int visita = new Visitas(tx, reloj).iniciar(cuenta(ev1), id);
        int evaluacion = evaluar(visita, puesto);
        assertEquals(1, escenario.evaluaciones.analisis().listar(cuenta(ev1), evaluacion).size());
        assertEquals(cuenta(ev1).nombre(), new EvaluacionDao().consultar(con, evaluacion).responsable());
        int cancelar = res.guardar(cuenta(ev1), 0,
                new Reservas.Datos(empresa, sede, salon, LocalDate.now(reloj), LocalTime.of(20, 0), LocalTime.of(21, 0),
                        "SEGUIMIENTO", "Prueba", ""));
        res.cancelar(cuenta(ev1), cancelar, "Prueba cancelada");
        assertEquals("CANCELADA", res.consultar(cuenta(ev1), cancelar).reserva().estado());
    }

    int evaluar(int visita, int puesto) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(12, 12, 1), "png", bytes);
        return escenario.evaluaciones.guardar(cuenta(ev1), visita,
                new Evaluaciones.Datos(puesto, "Postura E", "MAS_INADECUADA", "", 120, false, false, false, false,
                        false, false, "", true, false),
                List.of(new Fotos.Entrada(bytes.toByteArray(), "PERFIL", "Prueba")),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(0).entrada())));
    }

    @Test
    void retirarAsignacionRevocaSiguientePeticionYConservaHistoriaPropia() throws Exception {
        int propia = evaluar(escenario.visitaAbierta, escenario.puesto2);
        var datos = new Usuarios.Datos(cuenta(ev1).nombre(), cuenta(ev1).correo(), "123", "Pruebas",
                Cuenta.Rol.EVALUADOR, null, List.of());
        us.editar(cuenta(admin), ev1, datos, "");
        assertEquals(403, get("/empresas/" + empresa).statusCode());
        assertEquals(200, get("/evaluaciones/" + propia).statusCode());
        assertFalse(get("/evaluaciones/" + propia).body().contains("/analizar"));
        assertThrows(SecurityException.class,
                () -> escenario.evaluaciones.analisis().autorizarParcial(cuenta(ev1), escenario.visitaAbierta));
        assertEquals(303, get("/mis-evaluaciones").statusCode());
        String texto = get("/archivo?empresa=" + empresa).body();
        assertTrue(texto.contains("Ver historial"));
        assertEquals(List.of(escenario.puesto2),
                escenario.consultas.propias(cuenta(ev1), Map.of()).catalogos().get("puesto").stream()
                        .map(co.aplomo.dao.ConsultaDao.Opcion::id).toList());
        assertTrue(get("/").body().contains("Hoy no tienes citas"));
        assertTrue(get("/puestos/" + escenario.puesto2).body().contains("/evaluaciones/" + propia));
        assertFalse(texto.contains("/evaluaciones/" + escenario.evalRojo + "\""));
        assertTrue(Sql.<Integer>uno(con,
                "SELECT COUNT(*) FROM bitacora WHERE tabla='cuenta_empresa' AND accion='DESAC" +
                        "TIVAR' AND id_cuenta=?", fila -> fila.getInt(1), admin) > 0);
    }

    @Test
    void variosEvaluadoresActivosYUsuarioDeVariasEmpresas() throws Exception {
        int adicional = crear(Cuenta.Rol.EVALUADOR, List.of(empresa));
        assertTrue(new CuentaEmpresaDao().ids(con, adicional).contains(empresa));
        int cantidad = crear(Cuenta.Rol.USUARIO, List.of(empresa, escenario.empresaB));
        cambiarCuenta(cantidad);
        String texto = get("/empresas").body();
        assertTrue(texto.contains("Empresa ajena QA"));
        assertEquals(403, get("/agenda").statusCode());
        assertEquals(403, get("/evaluaciones/" + escenario.evalAbierta).statusCode());
        assertEquals(200, get("/evaluaciones/" + escenario.evalB).statusCode());
        assertEquals(403, get("/fotos/" + new EvaluacionDao().fotos(con, escenario.evalB).get(0).id()).statusCode());
        assertEquals(List.of(empresa, escenario.empresaB), ce.ids(con, cantidad));
    }

    @Test
    void notasConPermisosCsrfYAcuseRenovado() throws Exception {
        var ag = new Reservas(tx, reloj).agenda();
        ag.guardarNota(cuenta(admin), escenario.reservaAbierta, "<script>No ejecutar</script>");
        String texto = get("/reservas/" + escenario.reservaAbierta).body();
        assertTrue(texto.contains("&lt;script&gt;No ejecutar&lt;/script&gt;"));
        assertTrue(texto.contains(">Entendido</button>"));
        assertEquals(403, post("/reservas/" + escenario.reservaAbierta + "/entendido", Map.of()).statusCode());
        assertEquals(303, post("/reservas/" + escenario.reservaAbierta + "/entendido",
                Map.of("csrf", csrf(get("/")))).statusCode());
        assertTrue(ag.nota(cuenta(ev1), escenario.reservaAbierta).vista());
        ag.guardarNota(cuenta(admin), escenario.reservaAbierta, "Nueva nota");
        assertFalse(ag.nota(cuenta(ev1), escenario.reservaAbierta).vista());
        assertTrue(ag.nuevas(cuenta(ev1)).contains(escenario.reservaAbierta));
        assertThrows(SecurityException.class, () -> ag.entender(cuenta(ev2), escenario.reservaAbierta));
        assertThrows(SecurityException.class, () -> ag.guardarNota(cuenta(ev1), escenario.reservaAbierta, "No"));
        cambiarCuenta(usuario);
        assertFalse(get("/reservas/" + escenario.reservaAbierta).body().contains("Nueva nota"));
        assertThrows(IllegalArgumentException.class,
                () -> ag.guardarNota(cuenta(admin), escenario.reservaAbierta, "x".repeat(501)));
    }

    @Test
    void agendaSoloLecturaAdminYFiltroInactivo() throws Exception {
        cambiarCuenta(admin);
        var ag = new Reservas(tx, reloj).agenda();
        String texto = get("/agenda?fecha=2026-11-11").body();
        assertFalse(texto.contains("Nueva reserva"));
        assertTrue(texto.contains("Empresa ajena QA"));
        dao.acceso(con, ev2, 0, null, Cuenta.Estado.INACTIVA);
        assertTrue(ag.consultar(cuenta(admin), Map.of()).evaluadores().stream()
                .anyMatch(item -> item.id() == ev2 && !item.activo()));
        assertEquals(400, get("/agenda?evaluador=99999999").statusCode());
        assertTrue(ag.nuevas(cuenta(usuario)).isEmpty());
    }

    @Test
    void agendaFechasBordesCanceladasCrucesYSeisSemanas() throws Exception {
        var ag = new Reservas(tx, reloj).agenda();
        var elemento = ag.consultar(cuenta(ev1), Map.of("fecha", "2026-11-11", "vista", "semana"));
        assertEquals(DayOfWeek.MONDAY, elemento.desde().getDayOfWeek());
        assertTrue(elemento.citas().stream().noneMatch(cita -> cita.ficha().reserva().id() == escenario.reservaB ||
                cita.ficha().reserva().id() == escenario.reservaCancelada));
        assertTrue(ag.consultar(cuenta(ev1), Map.of("fecha", "2026-11-11", "canceladas", "1")).citas().stream()
                .anyMatch(cita2 -> cita2.ficha().reserva().id() == escenario.reservaCancelada));
        assertEquals(LocalDate.of(2026, 12, 28),
                ag.consultar(cuenta(ev1), Map.of("fecha", "2027-01-01", "vista", "semana")).desde());
        assertTrue(ag.consultar(cuenta(ev1), Map.of("vista", "no", "fecha", "mal")).mes());
        var mes = ag.consultar(cuenta(ev1), Map.of("vista", "mes", "fecha", "2026-08-01"));
        assertEquals(42,
                PaginasAgenda.pagina(new Sesiones(reloj).crear(cuenta(ev1)), mes).split("data-dia=", -1).length - 1);
        Sql.actualizar(con, "UPDATE reserva SET hora_inicio='11:30:00',hora_fin='12:30:00' WHERE id_reserva=?",
                escenario.reservaEsperando);
        assertTrue(ag.consultar(cuenta(ev1), Map.of("fecha", "2026-11-11")).citas().stream()
                .noneMatch(Agenda.Cita::cruce));
    }

    @Test
    void practicaPrivadaPorEvaluador() throws Exception {
        var evaluacion2 = new Visitas(tx, reloj).practica();
        var evaluacion3 = evaluacion2.abrir(cuenta(ev1), "PLANTA");
        var evaluacion4 = evaluacion2.abrir(cuenta(ev2), "OFICINA");
        assertNotEquals(evaluacion3.visita(), evaluacion4.visita());
        assertEquals(evaluacion3.visita(), evaluacion2.enCurso(cuenta(ev1)));
        assertThrows(SecurityException.class, () -> escenario.evaluaciones.contexto(cuenta(ev1), evaluacion4.visita()));
        assertNotNull(escenario.evaluaciones.contexto(cuenta(ev1), evaluacion3.visita()));
        assertTrue(escenario.consultas.propias(cuenta(ev1), Map.of()).filas().isEmpty());
    }

    @Test
    void cambiosDeRolConservanCampoAnteriorYRetiranAsignaciones() throws Exception {
        Integer antes = cuenta(usuario).empresa();
        us.editar(cuenta(admin), usuario,
                new Usuarios.Datos("Usuario convertido", cuenta(usuario).correo(), "123", "Pruebas",
                        Cuenta.Rol.ADMINISTRADOR, null), "");
        assertEquals(antes, cuenta(usuario).empresa());
        assertTrue(ce.ids(con, usuario).isEmpty());
    }

    @Test
    void arranqueCompruebaEsquemaActual() throws Exception {
        assertDoesNotThrow(() -> Esquema.verificar(con));
        assertEquals(23,
                Sql.<Integer>uno(con, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()",
                        rs -> rs.getInt(1)));
    }

    @Test
    void evaluadorAbreFotosYCalculoParcialConElMismoContratoYAjenaNo() throws Exception {
        assertEquals(200, get("/evaluaciones/" + escenario.evalAbierta).statusCode());
        int foto = new EvaluacionDao().fotos(con, escenario.evalAbierta).get(0).id();
        assertEquals(200, get("/fotos/" + foto).statusCode());
        String token = csrf(get("/"));
        assertEquals(403, post("/evaluaciones/calcular",
                Map.of("csrf", token, "visita", "" + escenario.visitaB, "segmento", "BRAZO", "angulo",
                        "0")).statusCode());
        assertEquals(200, get("/evaluaciones/nueva?visita=" + escenario.visitaAbierta + "&puesto=" +
                escenario.puesto2).statusCode());
        assertFalse(get("/agenda").body().contains("aria-label=\"Atrás\""));
        assertFalse(get("/mis-evaluaciones").body().contains("aria-label=\"Atrás\""));
    }

    @Test
    void historialPropioOrdenaEmpatesPorIdDescendenteYUsaPrimerAutor() throws Exception {
        int primero = evaluar(escenario.visitaAbierta, escenario.puesto2), segundo = evaluar(escenario.visitaAbierta,
                escenario.puesto2);
        var filas = escenario.consultas.propias(cuenta(ev1), Map.of()).filas();
        assertEquals(List.of(segundo, primero), filas.stream().map(co.aplomo.dao.ConsultaDao.Fila::id).toList());
        new BitacoraDao().registrar(con, ev2, java.time.LocalDateTime.now(reloj), "evaluacion", primero, "CREAR", null,
                null, "Registro posterior de QA");
        assertFalse(AccesoDatos.propia(con, cuenta(ev2), primero));
    }

    @Test
    void empresaInactivaDejaHistoriaPropiaEnSoloLectura() throws Exception {
        int id = evaluar(escenario.visitaAbierta, escenario.puesto2);
        new Empresas(tx).estadoEmpresa(cuenta(admin), empresa, false);
        assertEquals(200, get("/evaluaciones/" + id).statusCode());
        assertFalse(escenario.evaluaciones.consultar(cuenta(ev1), id).enCurso());
        assertFalse(escenario.evaluaciones.permiteTrabajo(cuenta(ev1), id));
        assertThrows(SecurityException.class, () -> escenario.evaluaciones.analisis().guardar(cuenta(ev1), id,
                List.of(new Analisis.Solicitud(new EvaluacionDao().fotos(con, id).get(0).id(),
                        CasosRulaPrueba.todos().get(0).entrada()))));
    }

    @Test
    void agendaPrefiereEvaluadorActivoCuandoOtroAsignadoEstaInactivo() throws Exception {
        dao.acceso(con, ev1, 0, null, Cuenta.Estado.INACTIVA);
        int nuevo = crear(Cuenta.Rol.EVALUADOR, List.of(empresa));
        var agenda = new Reservas(tx, reloj).agenda().consultar(cuenta(nuevo), Map.of("fecha", "2026-11-11"));
        assertTrue(agenda.citas().stream().allMatch(cita -> Objects.equals(cita.evaluador(), nuevo)));
    }
}
