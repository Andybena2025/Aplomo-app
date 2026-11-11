package co.aplomo.modelo;
import java.util.*;
/** RF-024: secciones, columnas y filtros permitidos; nunca se usan identificadores SQL recibidos del navegador. */
public enum SeccionConsulta {
    EMPRESAS("Empresas","empresa","nombre:Nombre|nit:NIT|estado:Estado","empresa,estado","ACTIVO,INACTIVO"),
    AREAS("Áreas","área","nombre:Área|empresa_nombre:Empresa|estado:Estado","empresa,area,estado","ACTIVO,INACTIVO"),
    CARGOS("Cargos","cargo","nombre:Cargo|tipo:Tipo|empresa_nombre:Empresa|estado:Estado","empresa,cargo,estado","ACTIVO,INACTIVO"),
    SEDES("Sedes","sede","nombre:Sede|empresa_nombre:Empresa|ciudad:Ciudad|estado:Estado","empresa,sede,estado","ACTIVO,INACTIVO"),
    SALONES("Salones","salón","nombre:Salón|ubicacion:Empresa y sede|piso:Piso|capacidad:Capacidad|estado:Estado","empresa,sede,salon,estado","ACTIVO,INACTIVO"),
    PUESTOS("Puestos","puesto","numero:Puesto|ubicacion:Empresa y lugar|asignacion:Área y cargo|puntaje:Resultado|estado:Estado","empresa,sede,salon,area,cargo,numero,puntaje,color,nivel,estado","ACTIVO,INACTIVO"),
    RESERVAS("Reservas","reserva","codigo:Reserva|ubicacion:Empresa y lugar|fecha:Fecha y horario|tipo:Categoría|estado:Estado","empresa,sede,salon,desde,hasta,estado","ESPERANDO,EN_COLA,FINALIZADA,CANCELADA"),
    VISITAS("Visitas","visita","codigo:Reserva|ubicacion:Empresa y lugar|fecha:Inicio real|estado:Estado|calificacion:Calificación","empresa,sede,salon,desde,hasta,estado","EN_CURSO,FINALIZADA"),
    EVALUACIONES("Evaluaciones","evaluación","fecha:Fecha|ubicacion:Empresa y lugar|numero:Puesto|puntaje:Resultado|estado:Visita","empresa,sede,salon,area,cargo,numero,desde,hasta,puntaje,color,nivel,estado","EN_CURSO,FINALIZADA"),
    PENDIENTES("Pendientes de arreglo","pendiente","numero:Puesto y ubicación|puntaje:Resultado|seguimiento:Seguimiento|estado:Estado","empresa,sede,salon,area,cargo,numero,desde,hasta,puntaje,color,nivel,estado","ABIERTO,ARREGLADO"),
    SUGERENCIAS("Sugerencias","sugerencia","numero:Puesto|nombre:Sugerencia|origen:Origen|prioridad:Prioridad|estado:Estado","empresa,sede,salon,area,cargo,numero,desde,hasta,puntaje,color,nivel,estado","PENDIENTE,EN_PROCESO,HECHA");
    /** Una columna y su clave permitida de ordenación. */
    public record Columna(String clave,String titulo) {}
    private final String titulo,singular;private final List<Columna> columnas;private final List<String> filtros,estados;
    SeccionConsulta(String titulo,String singular,String columnas,String filtros,String estados){this.titulo=titulo;this.singular=singular;this.columnas=Arrays.stream(columnas.split("\\|")).map(x->x.split(":",2)).map(x->new Columna(x[0],x[1])).toList();this.filtros=List.of(filtros.split(","));this.estados=List.of(estados.split(","));}
    /** Título visible de la sección. */ public String titulo(){return titulo;}
    /** Nombre singular para el contador. */ public String singular(){return singular;}
    /** Ruta estable que conserva los manejadores existentes de creación y edición. */ public String ruta(){return "/"+name().toLowerCase(Locale.ROOT);}
    /** Columnas de la tabla. */ public List<Columna> columnas(){return columnas;}
    /** Filtros aplicables a los datos de esta sección. */ public List<String> filtros(){return filtros;}
    /** Estados del esquema válidos en esta sección. */ public List<String> estados(){return estados;}
    /** Identifica únicamente las rutas de lista aprobadas, sin aceptar nombres arbitrarios. */
    public static SeccionConsulta deRuta(String ruta){return Arrays.stream(values()).filter(x->x.ruta().equals(ruta)).findFirst().orElse(null);}
}
