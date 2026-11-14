package co.aplomo.servicio;

import co.aplomo.dao.*;
import co.aplomo.db.*;
import co.aplomo.modelo.Cuenta;
import co.aplomo.seguridad.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Entorno de práctica voluntario: conserva intentos y no modifica empresas existentes. */
public final class PracticaEvaluacion {
    public static final String NIT="DEMO-APLOMO-PRACTICA", NOMBRE="APLOMO · Práctica de evaluación";
    private final Transacciones tx;
    private final Clock reloj;
    public record Destino(int visita,int puesto) {}
    public PracticaEvaluacion(Transacciones tx,Clock reloj){this.tx=tx;this.reloj=reloj;}

    /** Reutiliza incluso un puesto evaluado; si finalizan la visita, inicia otra sin borrar intentos. */
    public Destino abrir(Cuenta actor,String tipo)throws SQLException {
        if(!Set.of("OFICINA","PLANTA").contains(tipo==null?"":tipo))throw new IllegalArgumentException("Elige Oficina o Planta para practicar.");
        return tx.ejecutar(con->{
            Cuenta admin=AccesoDatos.vigente(con,actor);Permisos.administrador(admin);
            // El NIT único y el bloqueo de empresa serializan también dos clics simultáneos.
            Sql.actualizar(con,"INSERT INTO empresa (id_administrador,nombre,nit,descripcion) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE id_empresa=id_empresa",admin.id(),NOMBRE,NIT,"Solo demostración. Las evaluaciones de práctica se conservan aquí.");
            int empresa=Sql.uno(con,"SELECT id_empresa FROM empresa WHERE nit=? FOR UPDATE",r->r.getInt(1),NIT);
            if(!NOMBRE.equals(Sql.uno(con,"SELECT nombre FROM empresa WHERE id_empresa=?",r->r.getString(1),empresa)))throw new IllegalArgumentException("El identificador de práctica ya está en uso. No se cambió esa empresa.");
            if(!Boolean.TRUE.equals(Sql.uno(con,"SELECT activo FROM empresa WHERE id_empresa=?",r->r.getBoolean(1),empresa)))throw new IllegalArgumentException("La empresa de práctica está desactivada. Actívala en Empresas para continuar.");
            var bitacora=new BitacoraDao();var ahora=LocalDateTime.now(reloj);
            if(Sql.<Integer>uno(con,"SELECT COUNT(*) FROM bitacora WHERE tabla='empresa' AND id_registro=? AND accion='CREAR'",r->r.getInt(1),empresa)==0)bitacora.registrar(con,admin.id(),ahora,"empresa",empresa,"CREAR",null,null,"Empresa ficticia de práctica");
            Integer sede=Sql.uno(con,"SELECT id_sede FROM sede WHERE id_empresa=? AND nombre=?",r->r.getInt(1),empresa,"Sede de práctica");
            if(sede==null){sede=Sql.insertar(con,"INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion) VALUES (?,?,?,?,?,?,?)",empresa,"Sede de práctica","Dirección ficticia","Demostración","0000000000","Solo para pruebas","Datos ficticios para recorrer la evaluación.");bitacora.registrar(con,admin.id(),ahora,"sede",sede,"CREAR",null,null,"Sede de práctica");}
            Integer salon=Sql.uno(con,"SELECT id_salon FROM salon WHERE id_sede=? AND nombre=?",r->r.getInt(1),sede,"Sala de práctica");
            if(salon==null){salon=Sql.insertar(con,"INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) VALUES (?,?,?,?,?)",sede,"Sala de práctica","Puestos ficticios de Oficina y Planta",1,2);bitacora.registrar(con,admin.id(),ahora,"salon",salon,"CREAR",null,null,"Salón de práctica");}
            if(!Boolean.TRUE.equals(Sql.uno(con,"SELECT (s.activo AND l.activo) FROM salon l JOIN sede s USING(id_sede) WHERE l.id_salon=?",r->r.getBoolean(1),salon)))throw new IllegalArgumentException("La sede o el salón de práctica está desactivado. Actívalo en Empresas para continuar.");
            int elegido=0;
            for(int numero:List.of(1,2)){
                Integer puesto=Sql.uno(con,"SELECT id_puesto FROM puesto_trabajo WHERE id_salon=? AND numero=?",r->r.getInt(1),salon,numero);
                if(puesto==null){puesto=Sql.insertar(con,"INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) VALUES (?,?,?,?,?)",salon,numero,numero==1?"DEMO · Trabajo en computador":"DEMO · Levantamiento de piezas",numero==1?"ESCRITORIO":"MESA","Sala de práctica");bitacora.registrar(con,admin.id(),ahora,"puesto_trabajo",puesto,"CREAR",null,null,"Puesto de práctica");}
                if(numero==(tipo.equals("OFICINA")?1:2))elegido=puesto;
            }
            if(!Boolean.TRUE.equals(Sql.uno(con,"SELECT activo FROM puesto_trabajo WHERE id_puesto=?",r->r.getBoolean(1),elegido)))throw new IllegalArgumentException("El puesto de práctica está desactivado. Actívalo en Empresas para continuar.");
            Integer visita=Sql.uno(con,"SELECT v.id_visita FROM visita v JOIN reserva r USING(id_reserva) WHERE r.id_salon=? AND v.estado='EN_CURSO' AND r.estado='EN_COLA' ORDER BY v.id_visita DESC LIMIT 1 FOR UPDATE",r->r.getInt(1),salon);
            if(visita==null){
                LocalDate fecha=LocalDate.now(reloj);LocalTime inicio=LocalTime.of(8,0);
                while(Sql.<Integer>uno(con,"SELECT COUNT(*) FROM reserva WHERE id_salon=? AND fecha=? AND hora_inicio=?",r->r.getInt(1),salon,fecha,inicio)>0){inicio=inicio.plusMinutes(1);if(!inicio.isBefore(LocalTime.of(23,58))){fecha=fecha.plusDays(1);inicio=LocalTime.of(8,0);}}
                var reservas=new ReservaDao();int reserva=Sql.insertar(con,"INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,estado) VALUES (?,?,?,?,?,?,?,?,?)",salon,admin.id(),UUID.randomUUID().toString().replace("-","").substring(0,12),fecha,inicio,inicio.plusMinutes(1),"EVALUACION_INICIAL","DEMO · Visita para practicar sin usar una empresa real","EN_COLA");
                reservas.codigo(con,reserva,"R-"+String.format(Locale.ROOT,"%04d",reserva));bitacora.registrar(con,admin.id(),ahora,"reserva",reserva,"CREAR",null,"EN_COLA","Reserva de práctica");
                visita=new VisitaDao().iniciar(con,reserva,ahora);bitacora.registrar(con,admin.id(),ahora,"visita",visita,"CREAR",null,"EN_CURSO","Visita de práctica reutilizable");
            }
            return new Destino(visita,elegido);
        });
    }
}
