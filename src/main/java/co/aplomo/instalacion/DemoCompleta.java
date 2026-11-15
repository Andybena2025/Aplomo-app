package co.aplomo.instalacion;

import co.aplomo.dao.*;
import co.aplomo.db.*;
import co.aplomo.rula.*;
import co.aplomo.rula.MotorRula.*;
import co.aplomo.rula.MedidorAngulos.*;
import co.aplomo.rula.PuntuadorSegmentos.*;
import co.aplomo.servicio.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Paso 17, RF-017/021/022/023/025: casos ficticios calculados por los servicios reales. */
final class DemoCompleta {
    private static final String[] FOTOS={"08_laboratorio_buena_postura_perfil.jpg","02_calidad_cabeza_inclinada_perfil.jpg","07_bodega_levantando_caja_perfil.jpg"};
    private DemoCompleta(){}
    static void cargar(Connection c,Path carpeta,int administrador)throws SQLException,IOException {
        var tx=new Transacciones(()->sinConfirmar(c));var actor=new CuentaDao().porId(c,administrador,false);
        LocalDate dia=LocalDate.now(ZoneId.of("America/Bogota")).minusDays(2);
        Clock reloj=Clock.fixed(dia.atTime(11,0).atZone(ZoneId.of("America/Bogota")).toInstant(),ZoneId.of("America/Bogota"));
        var servicio=new Evaluaciones(tx,reloj,new Fotos(carpeta),new BitacoraDao());
        for(int bloque=1;bloque<=4;bloque++){int i=Math.min(bloque,3);boolean segunda=bloque==4;
            Integer empresa=Sql.uno(c,"SELECT id_empresa FROM empresa WHERE nit=?",rs->rs.getInt(1),"DEMO-APLOMO-E"+i);
            if(empresa==null)throw new IllegalArgumentException("Falta una empresa de demostración.");
            Integer area=Sql.uno(c,"SELECT id_area FROM area WHERE id_empresa=? AND activo=TRUE ORDER BY id_area LIMIT 1",rs->rs.getInt(1),empresa);
            Integer cargo=Sql.uno(c,"SELECT id_cargo FROM cargo WHERE id_empresa=? AND activo=TRUE ORDER BY id_cargo LIMIT 1",rs->rs.getInt(1),empresa);
            int sede=Sql.uno(c,"SELECT id_sede FROM sede WHERE id_empresa=? ORDER BY id_sede LIMIT 1 OFFSET "+(segunda?1:0),rs->rs.getInt(1),empresa);
            Integer salon=Sql.uno(c,"SELECT id_salon FROM salon WHERE id_sede=? AND nombre=?",rs->rs.getInt(1),sede,"Demostración de resultados");
            if(salon==null)salon=Sql.insertar(c,"INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) VALUES (?,?,?,?,?)",sede,"Demostración de resultados","Solo datos ficticios · Paso 17",1,12);
            String codigo=segunda?"DEMO17-E3B":"DEMO-17-E"+i;
            Integer reserva=Sql.uno(c,"SELECT r.id_reserva FROM reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede) WHERE codigo=? AND s.id_empresa=?",rs->rs.getInt(1),codigo,empresa);
            if(reserva==null){if(Sql.<Integer>uno(c,"SELECT COUNT(*) FROM reserva WHERE codigo=?",rs->rs.getInt(1),codigo)>0)throw new IllegalArgumentException("Un código de demostración está en otra empresa.");reserva=Sql.insertar(c,"INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,estado) VALUES (?,?,?,?,?,?,?,?,?)",salon,administrador,codigo,dia,LocalTime.of(9,0),LocalTime.of(11,0),"EVALUACION_INICIAL","DEMO17 · Resultados ficticios para presentar APLOMO","EN_COLA");}
            Integer visita=Sql.uno(c,"SELECT id_visita FROM visita WHERE id_reserva=?",rs->rs.getInt(1),reserva);
            if(visita==null)visita=new VisitaDao().iniciar(c,reserva,dia.atTime(9,0));
            boolean abierta="EN_CURSO".equals(Sql.<String>uno(c,"SELECT estado FROM visita WHERE id_visita=?",rs->rs.getString(1),visita));
            for(int color=0;color<3;color++){
                String descripcion="DEMO17 · "+List.of("Trabajo con buen apoyo","Alcance y revisión del puesto","Manipulación de carga").get(color);
                Integer puesto=Sql.uno(c,"SELECT id_puesto FROM puesto_trabajo WHERE id_salon=? AND numero=?",rs->rs.getInt(1),salon,color+1);
                if(puesto==null)puesto=Sql.insertar(c,"INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion) VALUES (?,?,?,?,?,?,?)",salon,area,cargo,color+1,descripcion,color==0?"ESCRITORIO":"MESA","Solo demostración");
                byte[] bytes;try(var in=DemoCompleta.class.getResourceAsStream("/static/img/demo/"+FOTOS[color])){if(in==null)throw new IOException("Falta una foto de muestra.");bytes=in.readAllBytes();}
                for(int postura=0;postura<2;postura++){
                    String criterio=postura==0?"MAS_INADECUADA":"MAYOR_EXPOSICION";
                    Integer ev=Sql.uno(c,"SELECT id_evaluacion FROM evaluacion WHERE id_visita=? AND id_puesto=? AND criterio=? ORDER BY id_evaluacion LIMIT 1",rs->rs.getInt(1),visita,puesto,criterio);
                    if(ev!=null){for(String ruta:Sql.listar(c,"SELECT ruta FROM fotografia WHERE id_evaluacion=?",rs->rs.getString(1),ev)){Path destino=carpeta.resolve(ruta).normalize();if(destino.startsWith(carpeta.normalize())&&!Files.exists(destino,LinkOption.NOFOLLOW_LINKS)){Files.createDirectories(carpeta);Files.write(destino,bytes,StandardOpenOption.CREATE_NEW);}}continue;}
                    if(!abierta)continue;
                    var datos=new Evaluaciones.Datos(puesto,(postura==0?"La más incómoda":"La que más tiempo mantiene")+" · "+descripcion,criterio,postura==0?"La postura más incómoda.":"La postura que más tiempo mantiene.",300,false,false,false,false,false,false,"Datos ficticios. Ángulos manuales de casos de docs/03; no son una medición automática de la imagen.",true,false);
                    ev=servicio.guardar(actor,visita,datos,List.of(new Fotos.Entrada(bytes,"PERFIL","Imagen de demostración · Caso manual simulado, no una valoración real.")),List.of(new Analisis.Solicitud(0,entrada(color))));
                    var sugerencias=servicio.sugerencias();int sug=sugerencias.guardar(actor,ev,0,"DEMO17 · Revisar el puesto","Demostración: revisar el alcance y conservar apoyos adecuados.",color==0?"MOBILIARIO":"POSTURA",color==2?"ALTA":"MEDIA",dia.plusDays(14).toString());
                    if(postura==0)sugerencias.estado(actor,sug,"HECHA");
                    var pendiente=new PendienteDao().porEvaluacion(c,ev);
                    if(pendiente!=null){servicio.pendientes().guardar(actor,pendiente.id(),"DEMO17 · Ajuste de puesto pendiente de comprobar","ALTA",dia.plusDays(7).toString());if(postura==0)servicio.pendientes().arreglar(actor,pendiente.id());}
                }
            }
            if(abierta)new Visitas(tx,reloj).finalizar(actor,visita,"DEMO17 · Visita ficticia con resultados calculados por APLOMO.");
            String futuro=segunda?"DEMO17-F3B":"DEMO-17-F"+i;
            if(Sql.<Integer>uno(c,"SELECT COUNT(*) FROM reserva WHERE codigo=?",rs->rs.getInt(1),futuro)==0)Sql.insertar(c,"INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,estado) VALUES (?,?,?,?,?,?,?,?,?)",salon,administrador,futuro,dia.plusDays(21),LocalTime.of(14,0),LocalTime.of(15,0),"SEGUIMIENTO","DEMO17 · Reserva futura ficticia","ESPERANDO");
        }
    }
    /** Entradas manuales de los casos 4, 2 y 8 de docs/03; nunca se asigna un puntaje final. */
    static Entrada entrada(int caso){double[] g=caso==0?new double[]{0,90,0,0,0}:new double[]{61,47,0,37,17};var m=new EnumMap<Segmento,Medicion>(Segmento.class);for(var s:Segmento.values())m.put(s,new Medicion(List.of(),g[s.ordinal()]));return new Entrada(Lado.DERECHO,200,400,m,Ajustes.sinAjustes(),caso==2,caso==2?Carga.MAS_10KG_INTERMITENTE:Carga.MENOS_2KG_INTERMITENTE);}
    /** Integra los servicios en la transacción externa, sin confirmar ni cerrar la conexión del cargador. */
    private static Connection sinConfirmar(Connection c)throws SQLException{Savepoint sp=c.setSavepoint();return (Connection)Proxy.newProxyInstance(DemoCompleta.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,metodo,args)->{switch(metodo.getName()){case "setAutoCommit","commit","close":return null;case "rollback":c.rollback(sp);return null;default:try{return metodo.invoke(c,args);}catch(InvocationTargetException e){throw e.getCause();}}});}
}
