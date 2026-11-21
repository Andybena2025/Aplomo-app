package co.aplomo.soporte;

import co.aplomo.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * RNF-003: carga reversible hasta 1.000 filas por tabla operativa; no cambia catálogos RULA ni esquema.
 */
public final class CargaMilRegistros {
    private CargaMilRegistros() {
    }

    /**
     * Rellena únicamente con datos nuevos y devuelve los tamaños exactos para documentar la medición.
     */
    public static Map<String, Integer> preparar(BasePrueba elemento) throws SQLException {
        String prefijo = "QA" + UUID.randomUUID().toString().substring(0, 8);
        for (int n = cuenta(elemento.con, "empresa"); n < 1000; n++)
            Sql.insertar(elemento.con, "INSERT INTO empresa(id_administrador,nombre,nit) VALUES(?,?,?)", elemento.admin,
                    prefijo + " Empresa " + n, prefijo + n);
        String hash = elemento.cuenta(elemento.admin).hash();
        for (int n = cuenta(elemento.con, "cuenta"); n < 1000; n++)
            Sql.insertar(elemento.con, "INSERT INTO cuenta(id_empresa,nombre,correo,telefono,cargo,contrasena_hash,c" +
                            "ontrasena_temporal,rol) VALUES(?,?,?,'123','QA',?,TRUE,'USUARIO')", elemento.empresa,
                    prefijo + " Contacto " + n, prefijo + n + "@prueba.invalid", hash);
        List<Integer> sedes = llenar(elemento, "sede", cantidad -> Sql.insertar(elemento.con,
                "INSERT INTO sede(id_empresa,nombre,direccion,ciudad,telefono,horario,descrip" +
                        "cion) VALUES(?,?,?,?,?,?,?)", elemento.empresa, prefijo + " Sede " + cantidad,
                "Calle de pruebas", "Armenia", "123", "8 a 5", "Carga reversible"));
        List<Integer> salones = llenar(elemento, "salon", cantidad2 -> Sql.insertar(elemento.con,
                "INSERT INTO salon(id_sede,nombre,descripcion,piso,capacidad_puestos) VALUES(" + "?,?,?,1,10)",
                sedes.get(cantidad2 % sedes.size()), prefijo + " Salón " + cantidad2, "Carga reversible"));
        List<Integer> areas = llenar(elemento, "area",
                cantidad3 -> Sql.insertar(elemento.con, "INSERT INTO area(id_empresa,nombre) VALUES(?,?)",
                        elemento.empresa, prefijo + " Área " + cantidad3));
        List<Integer> cargos = llenar(elemento, "cargo", cantidad4 -> Sql.insertar(elemento.con,
                "INSERT INTO cargo(id_empresa,nombre,tipo) VALUES(?,?,'OPERATIVO')", elemento.empresa,
                prefijo + " Cargo " + cantidad4));
        List<Integer> puestos = llenar(elemento, "puesto_trabajo", cantidad5 -> Sql.insertar(elemento.con,
                "INSERT INTO puesto_trabajo(id_salon,id_area,id_cargo,numero,descripcion,tipo" +
                        ",ubicacion) VALUES(?,?,?,1,?,'MESA',?)", salones.get(cantidad5 % salones.size()),
                areas.get(cantidad5 % areas.size()), cargos.get(cantidad5 % cargos.size()), "Puesto de carga",
                "Pruebas"));
        List<Integer> reservas = llenar(elemento, "reserva", cantidad6 -> Sql.insertar(elemento.con,
                "INSERT INTO reserva(id_salon,id_administrador,codigo,fecha,hora_inicio,hora_" +
                        "fin,categoria,descripcion,estado) VALUES(?,?,?,?,'09:00','10:00','EVALUACION" +
                        "_INICIAL',?,'FINALIZADA')", salones.get(cantidad6 % salones.size()), elemento.admin,
                prefijo.substring(0, 6) + "R" + cantidad6, LocalDate.now(elemento.reloj), "Carga reversible"));
        List<Integer> visitas = llenar(elemento, "visita", cantidad7 -> Sql.insertar(elemento.con,
                "INSERT INTO visita(id_reserva,hora_real_inicio,hora_real_fin,calificacion_fi" +
                        "nal,estado) VALUES(?,?,?,?, 'FINALIZADA')", reservas.get(cantidad7 % reservas.size()),
                LocalDateTime.now(elemento.reloj), LocalDateTime.now(elemento.reloj), 7));
        List<Integer> evaluaciones = llenar(elemento, "evaluacion", cantidad8 -> Sql.insertar(elemento.con,
                "INSERT INTO evaluacion(id_visita,id_puesto,fecha,postura,criterio,motivo,min" +
                        "utos_jornada,persona_acepto,registro_fotografico) VALUES(?,?,?,?,'MAS_INADEC" +
                        "UADA',?,120,TRUE,'CON_FOTOS')", visitas.get(cantidad8 % visitas.size()),
                puestos.get(cantidad8 % puestos.size()), LocalDate.now(elemento.reloj), "Ensamble lateral",
                "Carga reversible"));
        llenar(elemento, "condiciones_puesto", cantidad9 -> {
            Sql.actualizar(elemento.con, "INSERT INTO condiciones_puesto VALUES(?,TRUE,FALSE,TRUE,FALSE,TRUE,TRUE,?)",
                    evaluaciones.get(cantidad9 % evaluaciones.size()), "Carga reversible");
            return evaluaciones.get(cantidad9 % evaluaciones.size());
        });
        List<Integer> fotos = llenar(elemento, "fotografia", cantidad10 -> Sql.insertar(elemento.con,
                "INSERT INTO fotografia(id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,des" +
                        "cripcion) VALUES(?,?,?,1,'PERFIL',?)", evaluaciones.get(cantidad10 % evaluaciones.size()),
                prefijo + cantidad10 + ".png", "qa-carga/" + prefijo + cantidad10 + ".png",
                "Metadatos ficticios, sin archivo"));
        llenar(elemento, "analisis_rula", cantidad11 -> elemento.analisis(fotos.get(cantidad11 % fotos.size()), 7));
        llenar(elemento, "pendiente_arreglo", cantidad12 -> Sql.insertar(elemento.con,
                "INSERT INTO pendiente_arreglo(id_evaluacion,descripcion,prioridad,puntaje,fe" +
                        "cha_registro,estado) VALUES(?,?,'ALTA',7,?,'ABIERTO')",
                evaluaciones.get(cantidad12 % evaluaciones.size()), "Carga reversible", LocalDate.now(elemento.reloj)));
        llenar(elemento, "sugerencia", cantidad13 -> Sql.insertar(elemento.con,
                "INSERT INTO sugerencia(id_evaluacion,titulo,descripcion,tipo,prioridad,estad" +
                        "o,origen) VALUES(?,?,?,'POSTURA','ALTA','PENDIENTE','ADMINISTRADOR')",
                evaluaciones.get(cantidad13 % evaluaciones.size()), "Acercar las piezas", "Carga reversible"));
        llenar(elemento, "bitacora", cantidad14 -> Sql.insertar(elemento.con,
                "INSERT INTO bitacora(id_cuenta,fecha_hora,tabla,id_registro,accion,detalle) " +
                        "VALUES(?,?,'empresa',?,'CREAR',?)", elemento.admin, LocalDateTime.now(elemento.reloj),
                elemento.empresa, "Carga reversible"));
        Map<String, Integer> tamanos = new TreeMap<>();
        for (String tabla : TABLAS) tamanos.put(tabla, cuenta(elemento.con, tabla));
        return Collections.unmodifiableMap(tamanos);
    }

    private static final List<String> TABLAS = List.of("cuenta", "empresa", "sede", "salon", "area", "cargo",
            "puesto_trabajo", "reserva", "visita", "evaluacion", "condiciones_puesto", "fotografia", "analisis_rula",
            "pendiente_arreglo", "sugerencia", "bitacora");

    @FunctionalInterface
    private interface Crear {
        int insertar(int numero) throws SQLException;
    }

    private static List<Integer> llenar(BasePrueba elemento, String tabla, Crear crear) throws SQLException {
        int actual = cuenta(elemento.con, tabla);
        if (actual > 1000) throw new IllegalStateException(
                "La base de pruebas supera la carga prevista; conserva sus datos y revisa el " + "escenario.");
        List<Integer> ids = new ArrayList<>();
        for (int n = actual; n < 1000; n++) ids.add(crear.insertar(n - actual));
        if (ids.isEmpty())
            throw new IllegalStateException("Se necesitan registros nuevos para mantener esta carga aislada.");
        return ids;
    }

    private static int cuenta(Connection conexion, String tabla) throws SQLException {
        if (!TABLAS.contains(tabla)) throw new IllegalArgumentException("Tabla no permitida para la carga.");
        return Sql.uno(conexion, "SELECT COUNT(*) FROM " + tabla, fila -> fila.getInt(1));
    }
}
