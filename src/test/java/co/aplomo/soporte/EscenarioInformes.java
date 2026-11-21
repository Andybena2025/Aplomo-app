package co.aplomo.soporte;

import co.aplomo.dao.SedeDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Area;
import co.aplomo.modelo.Cargo;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.modelo.Sede;
import co.aplomo.servicio.Calificaciones;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * RF-025/026: tres sedes, dos calificadas, reevaluación Verde y resultados En curso; todo se revierte.
 */
public final class EscenarioInformes {
    public final int sedeCentro, sedeSur, sedeNorte, salonCentro, puesto1, puesto2, puestoSur;
    public final int visitaAnterior, visitaReevaluacion, visitaSur, visitaAbierta, evalAnterior, evalActual,
            evalAbierta;
    public final int empresaAjena, empresaSola, empresaVacia;
    public final Calificaciones calificaciones;
    private final BasePrueba elemento;

    /**
     * Prepara cifras conocidas sin tocar registros existentes ni archivos.
     */
    public EscenarioInformes(BasePrueba base) throws Exception {
        elemento = base;
        calificaciones = new Calificaciones(elemento.tx, elemento.reloj);
        elemento.dao.contrasena(elemento.con, elemento.usuario, elemento.cuenta(elemento.usuario).hash(), false);
        var empresas = new Empresas(elemento.tx);
        var visitas = new Visitas(elemento.tx, elemento.reloj);
        var centro = salon(elemento.empresa, "Sede Centro");
        sedeCentro = centro.sede();
        salonCentro = centro.id();
        var sur = salon(elemento.empresa, "Sede Sur");
        sedeSur = sur.sede();
        sedeNorte = salon(elemento.empresa, "Sede Norte").sede();
        int area = empresas.guardar(elemento.cuenta(elemento.admin), new Area(0, elemento.empresa, "Producción", true))
                .id();
        int cargo = empresas.guardar(elemento.cuenta(elemento.admin),
                new Cargo(0, elemento.empresa, "Operario", "OPERATIVO", true)).id();
        puesto1 = puesto(centro, 1, area, cargo);
        puesto2 = puesto(centro, 2, area, cargo);
        puestoSur = puesto(sur, 1, null, null);
        visitaAnterior = visita(centro);
        evalAnterior = evaluacion(visitaAnterior, puesto1, 7, 5);
        int e2 = evaluacion(visitaAnterior, puesto2, 5);
        pendiente(evalAnterior, 7, "ARREGLADO");
        pendiente(e2, 5, "ABIERTO");
        sugerencia(evalAnterior, "MOBILIARIO", "HECHA", true, "Ajustar la silla");
        sugerencia(e2, "POSTURA", "EN_PROCESO", true, "Acercar las piezas");
        sugerencia(e2, "POSTURA", "PENDIENTE", true, "Revisar apoyo");
        sugerencia(e2, "OTRA", "PENDIENTE", false, "Sugerencia quitada invisible");
        visitas.finalizar(elemento.cuenta(elemento.admin), visitaAnterior, "Primera visita: promedio seis.");
        elemento.reloj.avanzar(Duration.ofDays(1));
        visitaReevaluacion = visita(centro);
        evalActual = evaluacion(visitaReevaluacion, puesto1, 1, 2);
        evaluacion(visitaReevaluacion, puesto2, 4);
        visitas.finalizar(elemento.cuenta(elemento.admin), visitaReevaluacion, "Reevaluación: promedio tres.");
        visitaSur = visita(sur);
        evaluacion(visitaSur, puestoSur, 3);
        visitas.finalizar(elemento.cuenta(elemento.admin), visitaSur, "Promedio tres.");
        elemento.reloj.avanzar(Duration.ofDays(1));
        visitaAbierta = visita(centro);
        evalAbierta = evaluacion(visitaAbierta, puesto1, 7);
        pendiente(evalAbierta, 7, "ABIERTO");
        sugerencia(evalAbierta, "OTRA", "PENDIENTE", true, "En curso invisible");
        empresaAjena = empresa("Empresa ajena para informe");
        var ajena = salon(empresaAjena, "Sede ajena");
        int pa = puesto(ajena, 1, null, null);
        int va = visita(ajena);
        evaluacion(va, pa, 7);
        visitas.finalizar(elemento.cuenta(elemento.admin), va, "");
        empresaSola = empresa("Empresa de una sola sede");
        var sola = salon(empresaSola, "Sede única");
        int pu = puesto(sola, 1, null, null);
        int vu = visita(sola);
        evaluacion(vu, pu, 4);
        visitas.finalizar(elemento.cuenta(elemento.admin), vu, "");
        empresaVacia = empresa("Empresa sin visitas");
        salon(empresaVacia, "Sede pendiente");
    }

    private int empresa(String nombre) throws Exception {
        return Sql.insertar(elemento.con, "INSERT INTO empresa(id_administrador,nombre,nit) VALUES(?,?,?)",
                elemento.admin, nombre, UUID.randomUUID().toString().substring(0, 20));
    }

    private Salon salon(int empresa, String nombre) throws Exception {
        var empresa2 = new Empresas(elemento.tx);
        int sede = empresa2.guardar(elemento.cuenta(elemento.admin),
                new Sede(0, empresa, nombre, "Calle 10", "Armenia", "123", "8 a 5", "Sede de QA", true)).id();
        int id = empresa2.guardar(elemento.cuenta(elemento.admin),
                new Salon(0, sede, "Salón principal", "Ensamble", 1, 20, true)).id();
        return empresa2.salon(elemento.cuenta(elemento.admin), id);
    }

    private int puesto(Salon elemento2, int numero, Integer area, Integer cargo) throws Exception {
        return new Empresas(elemento.tx).guardar(elemento.cuenta(elemento.admin),
                new Puesto(0, elemento2.id(), area, cargo, numero, "Estación de ensamble " + numero, "MESA",
                        "Junto a ventana", null, true)).id();
    }

    private int visita(Salon elemento2) throws Exception {
        int empresa = new SedeDao().buscar(elemento.con, elemento2.sede(), 0, false).empresa();
        var reserva = new Reservas(elemento.tx, elemento.reloj);
        int id = reserva.guardar(elemento.cuenta(elemento.admin), 0,
                new Reservas.Datos(empresa, elemento2.sede(), elemento2.id(), LocalDate.now(elemento.reloj),
                        LocalTime.of(9, 0), LocalTime.of(10, 0), "REEVALUACION", "Visita de prueba", ""));
        reserva.aceptar(elemento.cuenta(elemento.admin), id);
        return new Visitas(elemento.tx, elemento.reloj).iniciar(elemento.cuenta(elemento.admin), id);
    }

    private int evaluacion(int visita, int puesto, int... puntajes) throws Exception {
        int cantidad = Sql.insertar(elemento.con,
                "INSERT INTO evaluacion(id_visita,id_puesto,fecha,postura,criterio,motivo,min" +
                        "utos_jornada,persona_acepto,registro_fotografico) VALUES(?,?,?,?,'MAS_INADEC" +
                        "UADA',?,120,TRUE,'CON_FOTOS')", visita, puesto, LocalDate.now(elemento.reloj),
                "Ensamble con alcance lateral", "Postura mantenida durante la tarea.");
        Sql.actualizar(elemento.con, "INSERT INTO condiciones_puesto VALUES(?,TRUE,FALSE,TRUE,FALSE,TRUE,TRUE,?)",
                cantidad, "Revisar el apoyo de los pies.");
        int foto = elemento.foto(cantidad);
        for (int cantidad2 : puntajes) elemento.analisis(foto, cantidad2);
        return cantidad;
    }

    private void pendiente(int evaluacion, int puntaje, String estado) throws Exception {
        Sql.actualizar(elemento.con, "INSERT INTO pendiente_arreglo(id_evaluacion,descripcion,prioridad,puntaje,fe" +
                        "cha_registro,estado) VALUES(?,?,'ALTA',?,?,?)", evaluacion, "Ajustar el puesto", puntaje,
                LocalDate.now(elemento.reloj), estado);
    }

    private void sugerencia(int evaluacion, String tipo, String estado, boolean activo,
            String titulo) throws Exception {
        Sql.actualizar(elemento.con, "INSERT INTO sugerencia(id_evaluacion,titulo,descripcion,tipo,prioridad,estad" +
                        "o,origen,activo) VALUES(?,?,?,?,'MEDIA',?,'ADMINISTRADOR',?)", evaluacion, titulo,
                "Mejora registrada por el Administrador.", tipo, estado, activo);
    }
}
