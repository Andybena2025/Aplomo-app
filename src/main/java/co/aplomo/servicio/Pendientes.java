package co.aplomo.servicio;

import co.aplomo.dao.*;import co.aplomo.db.*;import co.aplomo.modelo.*;import co.aplomo.seguridad.*;
import java.sql.*;import java.time.*;import java.time.format.DateTimeParseException;import java.util.*;

/** RF-022, RNF-004/005/009: seguimiento autorizado y automático en la transacción del análisis. */
public final class Pendientes {
    private final Transacciones tx;private final Clock reloj;private final BitacoraDao bitacora;private final PendienteDao dao=new PendienteDao();
    /** Usa MySQL, auditoría y hora de Colombia. */
    public Pendientes(){this(new Transacciones(),Clock.system(ZoneId.of("America/Bogota")),new BitacoraDao());}
    /** Comparte transacciones y reloj para comprobar sin confirmar datos de prueba. */
    public Pendientes(Transacciones tx,Clock reloj,BitacoraDao bitacora){this.tx=tx;this.reloj=reloj;this.bitacora=bitacora;}
    /** Lista por rol, empresa y visita; solo permite filtros y órdenes documentados. */
    public List<PendienteDao.Ficha> listar(Cuenta actor,String estado,String orden)throws SQLException{
        String e=Validacion.opcion(estado,"estado","ABIERTO","ARREGLADO","TODOS"),o=Validacion.opcion(orden,"orden","prioridad","fecha","limite");
        return tx.ejecutar(c->{var actual=AccesoDatos.vigente(c,actor);return dao.listar(c,AccesoDatos.empresaPermitida(actual),actual.rol()==Cuenta.Rol.USUARIO,e,o);});
    }
    /** Abre un pendiente únicamente dentro del alcance vigente de la cuenta. */
    public PendienteDao.Ficha consultar(Cuenta actor,int id)throws SQLException{return tx.ejecutar(c->visible(c,actor,id,false));}
    /** Conserva la bitácora visible con fecha, responsable y acciones del pendiente. */
    public List<PendienteDao.Cambio> historial(Cuenta actor,int id)throws SQLException{return tx.ejecutar(c->{visible(c,actor,id,false);return dao.historial(c,id);});}
    /** Guarda seguimiento opcional sin imponer fechas futuras que no exigen los documentos. */
    public void guardar(Cuenta actor,int id,String descripcion,String prioridad,String fecha)throws SQLException{
        tx.ejecutar(c->{var actual=admin(c,actor);var f=exigir(dao.buscar(c,id,0,false,true));
            String texto=Validacion.notas(descripcion);if(texto.codePointCount(0,texto.length())>255)throw new IllegalArgumentException("La descripción permite hasta 255 caracteres.");
            String p=prioridad==null||prioridad.isBlank()?null:Validacion.opcion(prioridad,"prioridad","ALTA","MEDIA","BAJA");LocalDate limite=fecha(fecha);
            var anterior=f.pendiente();if(Objects.equals(anterior.descripcion(),texto.isEmpty()?null:texto)&&Objects.equals(anterior.prioridad(),p)&&Objects.equals(anterior.limite(),limite))return null;
            dao.editar(c,id,texto.isEmpty()?null:texto,p,limite);
            bitacora.registrar(c,actual.id(),LocalDateTime.now(reloj),"pendiente_arreglo",id,"EDITAR",null,null,"Se actualizaron los datos de seguimiento: descripción, prioridad o fecha límite.");return null;});
    }
    /** Marca solo este registro; un segundo envío no duplica el cambio ni su auditoría. */
    public boolean arreglar(Cuenta actor,int id)throws SQLException{
        return tx.ejecutar(c->{var actual=admin(c,actor);var f=exigir(dao.buscar(c,id,0,false,true));if(f.pendiente().estado().equals("ARREGLADO"))return false;
            dao.arreglar(c,id);bitacora.registrar(c,actual.id(),LocalDateTime.now(reloj),"pendiente_arreglo",id,"CAMBIAR_ESTADO","ABIERTO","ARREGLADO","El Administrador confirmó que la empresa arregló el puesto.");return true;});
    }
    /** RF-022: se invoca con un resultado Rojo calculado por el servidor, bajo el bloqueo de la visita. */
    void registrarRojo(Connection c,Cuenta actor,int evaluacion)throws SQLException{
        Integer maximo=Sql.uno(c,"SELECT MAX(a.puntaje_final) FROM fotografia f JOIN analisis_rula a USING(id_foto) JOIN nivel_riesgo n USING(id_nivel) WHERE f.id_evaluacion=? AND n.color=?",r->r.getInt(1),evaluacion,"ROJO");
        if(maximo==null||maximo==0)return;var anterior=dao.porEvaluacion(c,evaluacion);
        if(anterior==null){int id=dao.crear(c,evaluacion,maximo,LocalDate.now(reloj));bitacora.registrar(c,actor.id(),LocalDateTime.now(reloj),"pendiente_arreglo",id,"CREAR",null,"ABIERTO","Pendiente creado automáticamente por un resultado Rojo.");}
        else if(maximo>anterior.puntaje()){dao.puntaje(c,anterior.id(),maximo);bitacora.registrar(c,actor.id(),LocalDateTime.now(reloj),"pendiente_arreglo",anterior.id(),"EDITAR",null,null,"El puntaje máximo de la evaluación pasó de "+anterior.puntaje()+" a "+maximo+"; se conservó su seguimiento y estado.");}
    }
    private PendienteDao.Ficha visible(Connection c,Cuenta actor,int id,boolean bloqueo)throws SQLException{var actual=AccesoDatos.vigente(c,actor);return exigir(dao.buscar(c,id,AccesoDatos.empresaPermitida(actual),actual.rol()==Cuenta.Rol.USUARIO,bloqueo));}
    private static PendienteDao.Ficha exigir(PendienteDao.Ficha f){if(f==null)throw new SecurityException("Acceso denegado. El pendiente no está disponible para tu cuenta.");return f;}
    private static Cuenta admin(Connection c,Cuenta a)throws SQLException{var actual=AccesoDatos.vigente(c,a);Permisos.administrador(actual);return actual;}
    private static LocalDate fecha(String texto){if(texto==null||texto.isBlank())return null;try{var d=LocalDate.parse(texto);if(d.getYear()<1000||d.getYear()>9999)throw new DateTimeParseException("Fuera de rango",texto,0);return d;}catch(DateTimeParseException x){throw new IllegalArgumentException("Revisa la fecha límite: escribe una fecha válida.");}}
}
