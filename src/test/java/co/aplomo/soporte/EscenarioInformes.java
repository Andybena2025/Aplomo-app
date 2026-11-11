package co.aplomo.soporte;

import co.aplomo.dao.*;
import co.aplomo.db.Sql;
import co.aplomo.modelo.*;
import co.aplomo.servicio.*;
import java.time.*;
import java.util.*;

/** RF-025/026: tres sedes, dos calificadas, reevaluación Verde y resultados En curso; todo se revierte. */
public final class EscenarioInformes {
    public final int sedeCentro,sedeSur,sedeNorte,salonCentro,puesto1,puesto2,puestoSur;
    public final int visitaAnterior,visitaReevaluacion,visitaSur,visitaAbierta,evalAnterior,evalActual,evalAbierta;
    public final int empresaAjena,empresaSola,empresaVacia;
    public final Calificaciones calificaciones;
    private final BasePrueba b;
    /** Prepara cifras conocidas sin tocar registros existentes ni archivos. */
    public EscenarioInformes(BasePrueba base)throws Exception{
        b=base;calificaciones=new Calificaciones(b.tx,b.reloj);
        b.dao.contrasena(b.con,b.usuario,b.cuenta(b.usuario).hash(),false);
        var empresas=new Empresas(b.tx);var visitas=new Visitas(b.tx,b.reloj);
        var centro=salon(b.empresa,"Sede Centro");sedeCentro=centro.sede();salonCentro=centro.id();
        var sur=salon(b.empresa,"Sede Sur");sedeSur=sur.sede();sedeNorte=salon(b.empresa,"Sede Norte").sede();
        int area=empresas.guardar(b.cuenta(b.admin),new Area(0,b.empresa,"Producción",true)).id();
        int cargo=empresas.guardar(b.cuenta(b.admin),new Cargo(0,b.empresa,"Operario","OPERATIVO",true)).id();
        puesto1=puesto(centro,1,area,cargo);puesto2=puesto(centro,2,area,cargo);puestoSur=puesto(sur,1,null,null);
        visitaAnterior=visita(centro);evalAnterior=evaluacion(visitaAnterior,puesto1,7,5);int e2=evaluacion(visitaAnterior,puesto2,5);
        pendiente(evalAnterior,7,"ARREGLADO");pendiente(e2,5,"ABIERTO");
        sugerencia(evalAnterior,"MOBILIARIO","HECHA",true,"Ajustar la silla");sugerencia(e2,"POSTURA","EN_PROCESO",true,"Acercar las piezas");
        sugerencia(e2,"POSTURA","PENDIENTE",true,"Revisar apoyo");sugerencia(e2,"OTRA","PENDIENTE",false,"Sugerencia quitada invisible");
        visitas.finalizar(b.cuenta(b.admin),visitaAnterior,"Primera visita: promedio seis.");
        b.reloj.avanzar(Duration.ofDays(1));visitaReevaluacion=visita(centro);evalActual=evaluacion(visitaReevaluacion,puesto1,1,2);
        evaluacion(visitaReevaluacion,puesto2,4);visitas.finalizar(b.cuenta(b.admin),visitaReevaluacion,"Reevaluación: promedio tres.");
        visitaSur=visita(sur);evaluacion(visitaSur,puestoSur,3);visitas.finalizar(b.cuenta(b.admin),visitaSur,"Promedio tres.");
        b.reloj.avanzar(Duration.ofDays(1));visitaAbierta=visita(centro);evalAbierta=evaluacion(visitaAbierta,puesto1,7);
        pendiente(evalAbierta,7,"ABIERTO");sugerencia(evalAbierta,"OTRA","PENDIENTE",true,"En curso invisible");
        empresaAjena=empresa("Empresa ajena para informe");var ajena=salon(empresaAjena,"Sede ajena");int pa=puesto(ajena,1,null,null);
        int va=visita(ajena);evaluacion(va,pa,7);visitas.finalizar(b.cuenta(b.admin),va,"");
        empresaSola=empresa("Empresa de una sola sede");var sola=salon(empresaSola,"Sede única");int pu=puesto(sola,1,null,null);
        int vu=visita(sola);evaluacion(vu,pu,4);visitas.finalizar(b.cuenta(b.admin),vu,"");
        empresaVacia=empresa("Empresa sin visitas");salon(empresaVacia,"Sede pendiente");
    }
    private int empresa(String nombre)throws Exception{return Sql.insertar(b.con,"INSERT INTO empresa(id_administrador,nombre,nit) VALUES(?,?,?)",b.admin,nombre,UUID.randomUUID().toString().substring(0,20));}
    private Salon salon(int empresa,String nombre)throws Exception{
        var e=new Empresas(b.tx);int sede=e.guardar(b.cuenta(b.admin),new Sede(0,empresa,nombre,"Calle 10","Armenia","123","8 a 5","Sede de QA",true)).id();
        int id=e.guardar(b.cuenta(b.admin),new Salon(0,sede,"Salón principal","Ensamble",1,20,true)).id();return e.salon(b.cuenta(b.admin),id);
    }
    private int puesto(Salon s,int numero,Integer area,Integer cargo)throws Exception{return new Empresas(b.tx).guardar(b.cuenta(b.admin),new Puesto(0,s.id(),area,cargo,numero,"Estación de ensamble "+numero,"MESA","Junto a ventana",null,true)).id();}
    private int visita(Salon s)throws Exception{
        int empresa=new SedeDao().buscar(b.con,s.sede(),0,false).empresa();var r=new Reservas(b.tx,b.reloj);
        int id=r.guardar(b.cuenta(b.admin),0,new Reservas.Datos(empresa,s.sede(),s.id(),LocalDate.now(b.reloj),LocalTime.of(9,0),LocalTime.of(10,0),"REEVALUACION","Visita de prueba",""));
        r.aceptar(b.cuenta(b.admin),id);return new Visitas(b.tx,b.reloj).iniciar(b.cuenta(b.admin),id);
    }
    private int evaluacion(int visita,int puesto,int... puntajes)throws Exception{
        int e=Sql.insertar(b.con,"INSERT INTO evaluacion(id_visita,id_puesto,fecha,postura,criterio,motivo,minutos_jornada,persona_acepto,registro_fotografico) VALUES(?,?,?,?,'MAS_INADECUADA',?,120,TRUE,'CON_FOTOS')",visita,puesto,LocalDate.now(b.reloj),"Ensamble con alcance lateral","Postura mantenida durante la tarea.");
        Sql.actualizar(b.con,"INSERT INTO condiciones_puesto VALUES(?,TRUE,FALSE,TRUE,FALSE,TRUE,TRUE,?)",e,"Revisar el apoyo de los pies.");
        int foto=b.foto(e);for(int p:puntajes)b.analisis(foto,p);return e;
    }
    private void pendiente(int evaluacion,int puntaje,String estado)throws Exception{Sql.actualizar(b.con,"INSERT INTO pendiente_arreglo(id_evaluacion,descripcion,prioridad,puntaje,fecha_registro,estado) VALUES(?,?,'ALTA',?,?,?)",evaluacion,"Ajustar el puesto",puntaje,LocalDate.now(b.reloj),estado);}
    private void sugerencia(int evaluacion,String tipo,String estado,boolean activo,String titulo)throws Exception{Sql.actualizar(b.con,"INSERT INTO sugerencia(id_evaluacion,titulo,descripcion,tipo,prioridad,estado,origen,activo) VALUES(?,?,?,?,'MEDIA',?,'ADMINISTRADOR',?)",evaluacion,titulo,"Mejora registrada por el Administrador.",tipo,estado,activo);}
}
