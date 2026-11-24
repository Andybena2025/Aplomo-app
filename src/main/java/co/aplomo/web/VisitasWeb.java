package co.aplomo.web;

import co.aplomo.dao.VisitaDao.Evaluacion;
import co.aplomo.dao.VisitaDao.Ficha;
import co.aplomo.seguridad.Permisos;
import co.aplomo.seguridad.Sesiones.Sesion;
import co.aplomo.servicio.Reservas;
import co.aplomo.servicio.Validacion;
import co.aplomo.servicio.Visitas;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RF-013a, RF-013b: inicio desde reserva y consulta o finalización de visitas autorizadas.
 */
public final class VisitasWeb {
    private static final Pattern INICIAR = Pattern.compile("/reservas/([1-9][0-9]*)/iniciar-visita");
    private static final Pattern VISITA = Pattern.compile("/visitas/([1-9][0-9]*)(/finalizar)?");
    private final Visitas visitas;
    private final Reservas reservas;

    /**
     * Recibe los servicios para mantener el flujo entre reserva y visita. RNF-011.
     */
    public VisitasWeb(Visitas visitas, Reservas reservas) {
        this.visitas = visitas;
        this.reservas = reservas;
    }

    /**
     * Expone el servicio para que el Inicio consulte datos sin duplicar la lógica de acceso. RNF-011.
     */
    public Visitas servicio() {
        return visitas;
    }

    /**
     * Atiende las rutas con permiso y token ya comprobados por el Router. RNF-011.
     */
    public void atender(HttpExchange intercambio, Sesion sesion,
            Map<String, String> datos) throws IOException, SQLException {
        String ruta = intercambio.getRequestURI().getPath();
        boolean post = intercambio.getRequestMethod().equals("POST");
        var iniciar = INICIAR.matcher(ruta);
        if (iniciar.matches()) {
            if (!post) {
                noDisponible(intercambio, sesion);
                return;
            }
            Permisos.trabajo(sesion.cuenta());
            int reserva = id(iniciar.group(1));
            var anterior = reservas.consultar(sesion.cuenta(), reserva);
            try {
                int visita = visitas.iniciar(sesion.cuenta(), reserva);
                Router.redirigir(intercambio, "/visitas/" + visita + "?aviso=" + codificar("Visita iniciada."));
            } catch (IllegalArgumentException error) {
                Router.enviar(intercambio, 400, PaginasReservas.detalle(sesion, anterior, error.getMessage(), false));
            } catch (SQLException error2) {
                Router.enviar(intercambio, 503,
                        PaginasReservas.detalle(sesion, anterior, "No pudimos iniciar la visita. Inténtalo de nuevo.",
                                false));
            }
            return;
        }
        if (ruta.equals("/visitas") && !post) {
            Router.enviar(intercambio, 200, PaginasVisitas.lista(sesion, visitas.listar(sesion.cuenta())));
            return;
        }
        var registro = VISITA.matcher(ruta);
        if (!registro.matches()) {
            noDisponible(intercambio, sesion);
            return;
        }
        int id = id(registro.group(1));
        boolean finalizar = registro.group(2) != null;
        if (finalizar) Permisos.trabajo(sesion.cuenta());
        Ficha ficha = visitas.consultar(sesion.cuenta(), id);
        List<Evaluacion> evaluaciones = visitas.evaluaciones(sesion.cuenta(), id);
        if (!finalizar && !post) {
            Router.enviar(intercambio, 200, PaginasVisitas.detalle(sesion, ficha, evaluaciones,
                    sesion.cuenta().rol() != co.aplomo.modelo.Cuenta.Rol.USUARIO &&
                            ficha.visita().estado().equals("EN_CURSO") ? visitas.puestos(sesion.cuenta(),
                            id) : List.of(), Router.consulta(intercambio).get("aviso"), true,
                    visitas.practica().esEmpresa(ficha.empresa())));
            return;
        }
        if (!finalizar) {
            noDisponible(intercambio, sesion);
            return;
        }
        if (!ficha.visita().estado().equals("EN_CURSO")) {
            Router.enviar(intercambio, 400,
                    PaginasVisitas.detalle(sesion, ficha, evaluaciones, List.of(), "La visita ya está finalizada.",
                            false));
            return;
        }
        if (!post) {
            Router.enviar(intercambio, 200,
                    PaginasVisitas.finalizar(sesion, ficha, evaluaciones, ficha.visita().notas(), null));
            return;
        }
        try {
            visitas.finalizar(sesion.cuenta(), id, datos.get("notas"));
        } catch (IllegalArgumentException error3) {
            Router.enviar(intercambio, 400,
                    PaginasVisitas.finalizar(sesion, ficha, evaluaciones, datos.get("notas"), error3.getMessage()));
            return;
        } catch (SQLException error4) {
            Router.enviar(intercambio, 503, PaginasVisitas.finalizar(sesion, ficha, evaluaciones, datos.get("notas"),
                    "No pudimos finalizar la visita. Tus notas siguen escritas; inténtalo de nuevo."));
            return;
        }
        Router.redirigir(intercambio, "/visitas/" + id + "?aviso=" + codificar("Visita finalizada."));
    }

    private static int id(String valor) {
        return Validacion.entero(valor, "identificador", 1, Integer.MAX_VALUE);
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static void noDisponible(HttpExchange intercambio, Sesion sesion) throws IOException {
        Router.enviar(intercambio, 404,
                Html.pagina("Página no disponible", PaginasEmpresas.enlace("/visitas", "Volver a visitas", false),
                        sesion));
    }
}
