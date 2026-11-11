package co.aplomo.rula;

import java.util.*;
import co.aplomo.rula.MedidorAngulos.Punto;
import co.aplomo.rula.MedidorAngulos.Lado;
import co.aplomo.rula.MedidorAngulos.Angulos;
import co.aplomo.rula.PuntuadorSegmentos.Ajustes;
import co.aplomo.rula.PuntuadorSegmentos.Puntos;
import co.aplomo.rula.TablasRula.NivelRiesgo;

/** RF-018, RF-019, RF-020, RNF-010: cálculo puro; no guarda análisis ni altera MySQL. */
public final class MotorRula {
    private final TablasRula tablas;

    /** Recibe las tablas previamente cargadas desde MySQL (RF-027). */
    public MotorRula(TablasRula tablas) {
        this.tablas = Objects.requireNonNull(tablas, "Faltan las tablas RULA.");
    }

    /** Las seis cargas autorizadas, con los mismos nombres del enum de analisis_rula (RF-019). */
    public enum Carga {
        MENOS_2KG_INTERMITENTE(0), DE_2_A_10KG_INTERMITENTE(1),
        DE_2_A_10KG_ESTATICA_O_REPETITIVA(2), MAS_10KG_INTERMITENTE(2),
        MAS_10KG_ESTATICA_O_REPETITIVA(3), GOLPES_O_FUERZAS_BRUSCAS(3);

        private final int puntos;
        Carga(int puntos) { this.puntos = puntos; }

        /** Devuelve el incremento que se aplica a ambos grupos (RF-019). */
        public int puntos() { return puntos; }
    }

    /** Desglose previo a guardar; puntajeA y B conservan el total sin limitar a la Tabla C. */
    public record Resultado(Puntos segmentos, int tablaA, int tablaB, int actividad, int carga,
                             int puntajeA, int puntajeB, int puntajeFinal, NivelRiesgo nivel) {}

    /** Los cinco segmentos medidos en una foto (RF-017). */
    public enum Segmento { BRAZO, ANTEBRAZO, MUNECA, CUELLO, TRONCO }
    /** Tres puntos normalizados; la corrección manual es opcional (RF-017). */
    public record Medicion(List<Punto> puntos, Double manual) {
        /** Conserva una copia de los puntos recibidos, también si se corrigió el ángulo. */
        public Medicion { puntos = puntos == null ? List.of() : List.copyOf(puntos); }
    }
    /** Datos de una foto; los grados y puntajes del navegador nunca son resultados confiables. */
    public record Entrada(Lado lado, int ancho, int alto, Map<Segmento,Medicion> mediciones,
                          Ajustes ajustes, boolean actividad, Carga carga) {}
    /** Ángulo calculado y puntos para persistir una medición (RF-017, RF-021). */
    public record Medida(Segmento segmento, String puntosMarcados, double grados, int puntos, boolean manual) {}
    /** Resultado completo, listo para mostrar y guardar sin duplicar las reglas en la interfaz. */
    public record Calculo(Entrada entrada, List<Medida> mediciones, Resultado resultado) {}

    /** Mide o valida cada segmento y delega el puntaje al mismo motor usado en las pruebas (RF-017 a RF-021). */
    public Calculo calcular(Entrada e) {
        if (e == null || e.lado() == null || e.ajustes() == null || e.carga() == null || e.mediciones() == null)
            throw new IllegalArgumentException("Completa el lado, los ángulos, los ajustes y la carga de la foto.");
        if (e.ancho() < 1 || e.alto() < 1)
            throw new IllegalArgumentException("No pudimos reconocer las dimensiones de la foto. Vuelve a seleccionarla.");
        double[] grados = new double[5]; List<String> puntos = new ArrayList<>();
        for (Segmento segmento : Segmento.values()) {
            Medicion m = e.mediciones().get(segmento);
            if (m == null || m.manual() == null && m.puntos().size() != 3)
                throw new IllegalArgumentException("Falta medir " + etiqueta(segmento) + ": marca los puntos o escribe el ángulo.");
            if (!m.puntos().isEmpty() && m.puntos().size() != 3)
                throw new IllegalArgumentException("Cada segmento necesita sus tres puntos completos.");
            StringJoiner coords = new StringJoiner(";", "[", "]");
            for (Punto p : m.puntos()) {
                if (p.x() < 0 || p.x() > 1 || p.y() < 0 || p.y() > 1)
                    throw new IllegalArgumentException("Los puntos deben estar dentro de la foto.");
                coords.add(String.format(Locale.ROOT,"%.6f,%.6f",p.x(),p.y()));
            }
            puntos.add(coords.toString());
            double g;
            if (m.manual() != null) g = m.manual();
            else {
                List<Punto> ps = m.puntos().stream().map(p -> new Punto(p.x()*e.ancho(),p.y()*e.alto())).toList();
                Punto a=ps.get(0),b=ps.get(1),c=ps.get(2);
                g = switch(segmento) {
                    case BRAZO -> MedidorAngulos.brazo(a,b,c,e.lado());
                    case ANTEBRAZO -> MedidorAngulos.antebrazo(a,b,c);
                    case MUNECA -> MedidorAngulos.muneca(a,b,c);
                    case CUELLO -> MedidorAngulos.cuello(a,b,c,e.lado());
                    case TRONCO -> MedidorAngulos.tronco(a,b,e.lado());
                };
            }
            MedidorAngulos.validar(g, segmento == Segmento.ANTEBRAZO ? 0 : -180, 180);
            grados[segmento.ordinal()] = g;
        }
        var r = calcular(new Angulos(grados[0],grados[1],grados[2],grados[3],grados[4]),e.ajustes(),e.actividad(),e.carga());
        int[] pts={r.segmentos().brazo(),r.segmentos().antebrazo(),r.segmentos().muneca(),r.segmentos().cuello(),r.segmentos().tronco()};
        List<Medida> medidas=new ArrayList<>();
        for(Segmento seg:Segmento.values()) medidas.add(new Medida(seg,puntos.get(seg.ordinal()),grados[seg.ordinal()],pts[seg.ordinal()],e.mediciones().get(seg).manual()!=null));
        return new Calculo(e,List.copyOf(medidas),r);
    }

    /** Restaura el desglose histórico usando los puntos y totales guardados, sin redondear otra vez el análisis. */
    public Calculo restaurar(Entrada e,List<Medida> medidas,int puntajeA,int puntajeB,int fin) {
        if(medidas.size()!=5)throw new IllegalArgumentException("El análisis no tiene sus cinco mediciones completas.");
        Map<Segmento,Integer> pts=new EnumMap<>(Segmento.class);for(var m:medidas)pts.put(m.segmento(),m.puntos());
        Puntos p=new Puntos(pts.get(Segmento.BRAZO),pts.get(Segmento.ANTEBRAZO),pts.get(Segmento.MUNECA),e.ajustes().giro()==PuntuadorSegmentos.Giro.MEDIO?1:2,pts.get(Segmento.CUELLO),pts.get(Segmento.TRONCO),e.ajustes().piernasApoyadas()?1:2);
        return new Calculo(e,medidas,new Resultado(p,tablas.buscarA(p.brazo(),p.antebrazo(),p.muneca(),p.giro()),tablas.buscarB(p.cuello(),p.tronco(),p.piernas()),e.actividad()?1:0,e.carga().puntos(),puntajeA,puntajeB,fin,tablas.nivelDe(fin)));
    }

    private static String etiqueta(Segmento s) {
        return switch(s) { case BRAZO -> "el brazo";case ANTEBRAZO -> "el antebrazo";case MUNECA -> "la muñeca";case CUELLO -> "el cuello";case TRONCO -> "el tronco"; };
    }

    /** Calcula segmentos, A y B, suma una actividad/carga común y consulta C y riesgo (RF-018 a RF-020). */
    public Resultado calcular(Angulos angulos, Ajustes ajustes, boolean estaticaORepetitiva, Carga carga) {
        Objects.requireNonNull(carga, "Elige la carga o fuerza.");
        Puntos p = PuntuadorSegmentos.puntuar(angulos, ajustes);
        int a = tablas.buscarA(p.brazo(), p.antebrazo(), p.muneca(), p.giro());
        int b = tablas.buscarB(p.cuello(), p.tronco(), p.piernas());
        int actividad = estaticaORepetitiva ? 1 : 0;
        int ajustadoA = a + actividad + carga.puntos();
        int ajustadoB = b + actividad + carga.puntos();
        int fin = tablas.buscarC(ajustadoA, ajustadoB);
        return new Resultado(p, a, b, actividad, carga.puntos(), ajustadoA, ajustadoB, fin, tablas.nivelDe(fin));
    }
}
