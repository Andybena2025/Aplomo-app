package co.aplomo.soporte;

import co.aplomo.db.Sql;
import java.sql.*;
import java.time.*;
import java.util.*;

/** RNF-003: carga reversible hasta 1.000 filas por tabla operativa; no cambia catálogos RULA ni esquema. */
public final class CargaMilRegistros {
    private CargaMilRegistros(){}
    /** Rellena únicamente con datos nuevos y devuelve los tamaños exactos para documentar la medición. */
    public static Map<String,Integer> preparar(BasePrueba b)throws SQLException {
        String prefijo="QA"+UUID.randomUUID().toString().substring(0,8);
        for(int n=cuenta(b.con,"empresa");n<1000;n++)Sql.insertar(b.con,"INSERT INTO empresa(id_administrador,nombre,nit) VALUES(?,?,?)",b.admin,prefijo+" Empresa "+n,prefijo+n);
        String hash=b.cuenta(b.admin).hash();
        for(int n=cuenta(b.con,"cuenta");n<1000;n++)Sql.insertar(b.con,"INSERT INTO cuenta(id_empresa,nombre,correo,telefono,cargo,contrasena_hash,contrasena_temporal,rol) VALUES(?,?,?,'123','QA',?,TRUE,'USUARIO')",b.empresa,prefijo+" Contacto "+n,prefijo+n+"@prueba.invalid",hash);
        List<Integer> sedes=llenar(b,"sede",n->Sql.insertar(b.con,"INSERT INTO sede(id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion) VALUES(?,?,?,?,?,?,?)",b.empresa,prefijo+" Sede "+n,"Calle de pruebas","Armenia","123","8 a 5","Carga reversible"));
        List<Integer> salones=llenar(b,"salon",n->Sql.insertar(b.con,"INSERT INTO salon(id_sede,nombre,descripcion,piso,capacidad_puestos) VALUES(?,?,?,1,10)",sedes.get(n%sedes.size()),prefijo+" Salón "+n,"Carga reversible"));
        List<Integer> areas=llenar(b,"area",n->Sql.insertar(b.con,"INSERT INTO area(id_empresa,nombre) VALUES(?,?)",b.empresa,prefijo+" Área "+n));
        List<Integer> cargos=llenar(b,"cargo",n->Sql.insertar(b.con,"INSERT INTO cargo(id_empresa,nombre,tipo) VALUES(?,?,'OPERATIVO')",b.empresa,prefijo+" Cargo "+n));
        List<Integer> puestos=llenar(b,"puesto_trabajo",n->Sql.insertar(b.con,"INSERT INTO puesto_trabajo(id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion) VALUES(?,?,?,1,?,'MESA',?)",salones.get(n%salones.size()),areas.get(n%areas.size()),cargos.get(n%cargos.size()),"Puesto de carga","Pruebas"));
        List<Integer> reservas=llenar(b,"reserva",n->Sql.insertar(b.con,"INSERT INTO reserva(id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,estado) VALUES(?,?,?,?,'09:00','10:00','EVALUACION_INICIAL',?,'FINALIZADA')",salones.get(n%salones.size()),b.admin,prefijo.substring(0,6)+"R"+n,LocalDate.now(b.reloj),"Carga reversible"));
        List<Integer> visitas=llenar(b,"visita",n->Sql.insertar(b.con,"INSERT INTO visita(id_reserva,hora_real_inicio,hora_real_fin,calificacion_final,estado) VALUES(?,?,?,?, 'FINALIZADA')",reservas.get(n%reservas.size()),LocalDateTime.now(b.reloj),LocalDateTime.now(b.reloj),7));
        List<Integer> evaluaciones=llenar(b,"evaluacion",n->Sql.insertar(b.con,"INSERT INTO evaluacion(id_visita,id_puesto,fecha,postura,criterio,motivo,minutos_jornada,persona_acepto,registro_fotografico) VALUES(?,?,?,?,'MAS_INADECUADA',?,120,TRUE,'CON_FOTOS')",visitas.get(n%visitas.size()),puestos.get(n%puestos.size()),LocalDate.now(b.reloj),"Ensamble lateral","Carga reversible"));
        llenar(b,"condiciones_puesto",n->{Sql.actualizar(b.con,"INSERT INTO condiciones_puesto VALUES(?,TRUE,FALSE,TRUE,FALSE,TRUE,TRUE,?)",evaluaciones.get(n%evaluaciones.size()),"Carga reversible");return evaluaciones.get(n%evaluaciones.size());});
        List<Integer> fotos=llenar(b,"fotografia",n->Sql.insertar(b.con,"INSERT INTO fotografia(id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,descripcion) VALUES(?,?,?,1,'PERFIL',?)",evaluaciones.get(n%evaluaciones.size()),prefijo+n+".png","qa-carga/"+prefijo+n+".png","Metadatos ficticios, sin archivo"));
        llenar(b,"analisis_rula",n->b.analisis(fotos.get(n%fotos.size()),7));
        llenar(b,"pendiente_arreglo",n->Sql.insertar(b.con,"INSERT INTO pendiente_arreglo(id_evaluacion,descripcion,prioridad,puntaje,fecha_registro,estado) VALUES(?,?,'ALTA',7,?,'ABIERTO')",evaluaciones.get(n%evaluaciones.size()),"Carga reversible",LocalDate.now(b.reloj)));
        llenar(b,"sugerencia",n->Sql.insertar(b.con,"INSERT INTO sugerencia(id_evaluacion,titulo,descripcion,tipo,prioridad,estado,origen) VALUES(?,?,?,'POSTURA','ALTA','PENDIENTE','ADMINISTRADOR')",evaluaciones.get(n%evaluaciones.size()),"Acercar las piezas","Carga reversible"));
        llenar(b,"bitacora",n->Sql.insertar(b.con,"INSERT INTO bitacora(id_cuenta,fecha_hora,tabla,id_registro,accion,detalle) VALUES(?,?,'empresa',?,'CREAR',?)",b.admin,LocalDateTime.now(b.reloj),b.empresa,"Carga reversible"));
        Map<String,Integer> tamanos=new TreeMap<>();for(String tabla:TABLAS)tamanos.put(tabla,cuenta(b.con,tabla));return Collections.unmodifiableMap(tamanos);
    }
    private static final List<String> TABLAS=List.of("cuenta","empresa","sede","salon","area","cargo","puesto_trabajo","reserva","visita","evaluacion","condiciones_puesto","fotografia","analisis_rula","pendiente_arreglo","sugerencia","bitacora");
    @FunctionalInterface private interface Crear{int insertar(int numero)throws SQLException;}
    private static List<Integer> llenar(BasePrueba b,String tabla,Crear crear)throws SQLException {
        int actual=cuenta(b.con,tabla);if(actual>1000)throw new IllegalStateException("La base de pruebas supera la carga prevista; conserva sus datos y revisa el escenario.");
        List<Integer> ids=new ArrayList<>();for(int n=actual;n<1000;n++)ids.add(crear.insertar(n-actual));
        if(ids.isEmpty())throw new IllegalStateException("Se necesitan registros nuevos para mantener esta carga aislada.");return ids;
    }
    private static int cuenta(Connection c,String tabla)throws SQLException{
        if(!TABLAS.contains(tabla))throw new IllegalArgumentException("Tabla no permitida para la carga.");return Sql.uno(c,"SELECT COUNT(*) FROM "+tabla,r->r.getInt(1));
    }
}
