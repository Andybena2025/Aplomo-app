package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.*;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RF-016, RNF-005/006: reemplazo lógico, sin borrar fotos ni sus análisis históricos. */
public final class FotosEvaluacion {
    public static final String REEMPLAZADA="Reemplazada · ";
    public record Cambio(Integer foto,Integer anterior,String vista,String descripcion) {}
    private final Transacciones tx;private final Clock reloj;private final Fotos archivos;private final BitacoraDao bitacora;
    public FotosEvaluacion(Transacciones tx,Clock reloj,Fotos archivos,BitacoraDao bitacora){this.tx=tx;this.reloj=reloj;this.archivos=archivos;this.bitacora=bitacora;}
    /** Nueva imagen opcional: sin imagen, marca como reemplazada la anterior, conservando su archivo. */
    public Cambio cambiar(Cuenta actor,int evaluacion,Integer anterior,Fotos.Entrada entrada)throws SQLException,IOException {
        List<String> nuevos=new ArrayList<>();
        try{return tx.ejecutar(c->{
            Cuenta admin=AccesoDatos.vigente(c,actor);Permisos.trabajo(admin);var dao=new EvaluacionDao();AccesoDatos.exigirEvaluacion(c,actor,evaluacion,true);var e=dao.consultar(c,evaluacion);
            if(e==null)throw new SecurityException("La evaluación no está disponible.");
            var vd=new VisitaDao();var visita=vd.buscar(c,e.visita(),0,false,false);if(visita==null)throw new SecurityException("La visita no está disponible.");
            new ReservaDao().buscar(c,visita.visita().reserva(),0,true);visita=vd.buscar(c,e.visita(),0,false,true);
            if(!visita.visita().estado().equals("EN_CURSO"))throw new IllegalArgumentException("Solo puedes cambiar fotos de una visita En curso.");
            if(e.datos().sinFotos()||!e.datos().aceptaFotos())throw new IllegalArgumentException("Esta evaluación no tiene permiso para fotos.");
            EvaluacionDao.Foto previa=null;
            if(anterior!=null){Sql.uno(c,"SELECT id_foto FROM fotografia WHERE id_foto=? FOR UPDATE",r->r.getInt(1),anterior);previa=dao.foto(c,anterior);if(previa==null||previa.evaluacion()!=evaluacion)throw new SecurityException("La foto no pertenece a esta evaluación.");}
            if(entrada==null&&previa==null)throw new IllegalArgumentException("Elige la foto que quieres quitar.");
            if(entrada!=null&&previa!=null&&previa.descripcion().startsWith(REEMPLAZADA))throw new IllegalArgumentException("Esta foto ya fue reemplazada. Abre la foto actual.");
            Integer nueva=null;String vista="",descripcion="";
            if(entrada!=null){
                long activas=dao.fotos(c,evaluacion).stream().filter(f->!f.descripcion().startsWith(REEMPLAZADA)).count();
                if(activas-(previa==null?0:1)>=10)throw new IllegalArgumentException("Puedes guardar hasta 10 fotos activas.");
                var imagen=archivos.validar(List.of(entrada)).get(0);String nombre;try{nombre=archivos.guardar(imagen);}catch(IOException x){throw new UncheckedIOException(x);}nuevos.add(nombre);
                nueva=dao.foto(c,evaluacion,nombre,imagen.vista(),imagen.descripcion(),imagen.bytes().length);vista=imagen.vista();descripcion=imagen.descripcion();
                bitacora.registrar(c,admin.id(),LocalDateTime.now(reloj),"fotografia",nueva,"CREAR",null,null,"Foto nueva · Evaluación "+evaluacion);
            }
            if(previa!=null&&!previa.descripcion().startsWith(REEMPLAZADA)){
                String marca=REEMPLAZADA+previa.descripcion();marca=marca.codePoints().limit(200).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString();
                Sql.actualizar(c,"UPDATE fotografia SET descripcion=? WHERE id_foto=?",marca,previa.id());
                bitacora.registrar(c,admin.id(),LocalDateTime.now(reloj),"fotografia",previa.id(),"EDITAR",null,null,"Reemplazada"+(nueva==null?"":" por foto "+nueva)+" · "+previa.descripcion());
            }
            return new Cambio(nueva,anterior,vista,descripcion);
        });}catch(SQLException|RuntimeException x){archivos.revertirNuevos(nuevos);if(x instanceof UncheckedIOException io)throw io.getCause();throw x;}
    }
}
