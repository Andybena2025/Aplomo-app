package co.aplomo.soporte;

import co.aplomo.dao.BitacoraDao;
import co.aplomo.dao.PendienteDao;
import co.aplomo.db.Sql;
import co.aplomo.modelo.Area;
import co.aplomo.modelo.Cargo;
import co.aplomo.modelo.Puesto;
import co.aplomo.modelo.Salon;
import co.aplomo.servicio.Analisis;
import co.aplomo.servicio.Consultas;
import co.aplomo.servicio.Empresas;
import co.aplomo.servicio.Evaluaciones;
import co.aplomo.servicio.Fotos;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Visitas;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/**
 * RF-024: dos empresas, todos los estados de reserva y un Rojo En curso oculto al Usuario. Todo pertenece a la
 * transacción de BasePrueba.
 */
public final class EscenarioConsultas {
    public final int empresaA, empresaB, sedeA, sedeB, salonA, salonB, areaA, areaB, cargoA, cargoB, puesto1, puesto2
            , puesto3, puestoB, visitaFinal, visitaAbierta, visitaB, evalVerde, evalRojo, evalAbierta,
            evalNuevaAbierta, evalB, pendienteAbierto, pendienteB, sugerenciaAbierta, sugerenciaB, reservaFinal,
            reservaAbierta, reservaEsperando, reservaCancelada, reservaB;
    public final Evaluaciones evaluaciones;
    public final Consultas consultas;

    /**
     * Crea únicamente datos nuevos dentro de la conexión reversible recibida.
     */
    public EscenarioConsultas(BasePrueba elemento, Path fotos) throws Exception {
        ReglasSugerenciasPrueba.cargar(elemento.con);
        empresaA = elemento.empresa;
        var empresas = new Empresas(elemento.tx);
        var reservas = new Reservas(elemento.tx, elemento.reloj);
        var visitas = new Visitas(elemento.tx, elemento.reloj);
        evaluaciones = new Evaluaciones(elemento.tx, elemento.reloj, new Fotos(fotos), new BitacoraDao());
        consultas = evaluaciones.consultas();
        elemento.dao.contrasena(elemento.con, elemento.usuario, elemento.cuenta(elemento.usuario).hash(), false);
        var salon = elemento.crearSalon();
        salonA = salon.id();
        sedeA = salon.sede();
        areaA = empresas.guardar(elemento.cuenta(elemento.admin), new Area(0, empresaA, "Producción y ensamble", true))
                .id();
        cargoA = empresas.guardar(elemento.cuenta(elemento.admin),
                new Cargo(0, empresaA, "Operario de precisión", "OPERATIVO", true)).id();
        puesto1 = puesto(elemento, empresas, salonA, areaA, cargoA, 1, "Estación de ensamble Alfa");
        puesto2 = puesto(elemento, empresas, salonA, areaA, cargoA, 2, "Estación nueva en curso");
        puesto3 = puesto(elemento, empresas, salonA, areaA, cargoA, 3, "Estación de ensamble Zeta");
        reservaFinal = reservas.guardar(elemento.cuenta(elemento.admin), 0, elemento.solicitud(salon));
        reservas.aceptar(elemento.cuenta(elemento.admin), reservaFinal);
        visitaFinal = visitas.iniciar(elemento.cuenta(elemento.admin), reservaFinal);
        evalVerde = evaluar(elemento, visitaFinal, puesto1, 1);
        evalRojo = evaluar(elemento, visitaFinal, puesto3, 10);
        visitas.finalizar(elemento.cuenta(elemento.admin), visitaFinal, "Visita finalizada de prueba");
        elemento.reloj.avanzar(Duration.ofDays(1));
        reservaAbierta = reservas.guardar(elemento.cuenta(elemento.admin), 0, solicitud(empresaA, salon, 11));
        reservas.aceptar(elemento.cuenta(elemento.admin), reservaAbierta);
        visitaAbierta = visitas.iniciar(elemento.cuenta(elemento.admin), reservaAbierta);
        evalAbierta = evaluar(elemento, visitaAbierta, puesto1, 10);
        evalNuevaAbierta = evaluar(elemento, visitaAbierta, puesto2, 10);
        reservaEsperando = reservas.guardar(elemento.cuenta(elemento.admin), 0, solicitud(empresaA, salon, 13));
        reservaCancelada = reservas.guardar(elemento.cuenta(elemento.admin), 0, solicitud(empresaA, salon, 15));
        reservas.cancelar(elemento.cuenta(elemento.admin), reservaCancelada, "La empresa solicitó otro horario.");
        empresaB = Sql.insertar(elemento.con, "INSERT INTO empresa (id_administrador,nombre,nit) VALUES (?,?,?)",
                elemento.admin, "Empresa ajena QA", UUID.randomUUID().toString().substring(0, 20));
        elemento.empresa = empresaB;
        var otro = elemento.crearSalon();
        salonB = otro.id();
        sedeB = otro.sede();
        areaB = empresas.guardar(elemento.cuenta(elemento.admin), new Area(0, empresaB, "Área ajena QA", true)).id();
        cargoB = empresas.guardar(elemento.cuenta(elemento.admin),
                new Cargo(0, empresaB, "Cargo ajeno QA", "ADMINISTRATIVO", true)).id();
        puestoB = puesto(elemento, empresas, salonB, areaB, cargoB, 1, "Puesto ajeno QA");
        reservaB = reservas.guardar(elemento.cuenta(elemento.admin), 0, elemento.solicitud(otro));
        reservas.aceptar(elemento.cuenta(elemento.admin), reservaB);
        visitaB = visitas.iniciar(elemento.cuenta(elemento.admin), reservaB);
        evalB = evaluar(elemento, visitaB, puestoB, 10);
        visitas.finalizar(elemento.cuenta(elemento.admin), visitaB, "");
        elemento.empresa = empresaA;
        pendienteAbierto = new PendienteDao().porEvaluacion(elemento.con, evalAbierta).id();
        pendienteB = new PendienteDao().porEvaluacion(elemento.con, evalB).id();
        sugerenciaAbierta = evaluaciones.sugerencias().listar(elemento.cuenta(elemento.admin), evalAbierta).get(0)
                .sugerencia().id();
        sugerenciaB = evaluaciones.sugerencias().listar(elemento.cuenta(elemento.admin), evalB).get(0).sugerencia()
                .id();
    }

    private int evaluar(BasePrueba elemento, int visita, int puesto, int caso) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(12, 12, 1), "png", bytes);
        return evaluaciones.guardar(elemento.cuenta(elemento.admin), visita,
                new Evaluaciones.Datos(puesto, "Postura de ensamble " + puesto, "MAS_INADECUADA",
                        "Postura mantenida al trabajar.", 120, true, true, true, true, true, true, "", true, false),
                List.of(new Fotos.Entrada(bytes.toByteArray(), "PERFIL", "Imagen sintética de prueba")),
                List.of(new Analisis.Solicitud(0, CasosRulaPrueba.todos().get(caso - 1).entrada())));
    }

    private static int puesto(BasePrueba elemento, Empresas empresa2, int salon, int area, int cargo, int numero,
            String nombre) throws Exception {
        return empresa2.guardar(elemento.cuenta(elemento.admin),
                new Puesto(0, salon, area, cargo, numero, nombre, "MESA", "Junto a la ventana", null, true)).id();
    }

    private Reservas.Datos solicitud(int empresa, Salon salon, int hora) {
        return new Reservas.Datos(empresa, salon.sede(), salon.id(), LocalDate.of(2026, 11, 11), LocalTime.of(hora, 0),
                LocalTime.of(hora + 1, 0), "SEGUIMIENTO", "Reserva de prueba", "");
    }
}
