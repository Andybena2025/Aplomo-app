package co.aplomo.rula;

import java.util.Objects;

/** RF-017, RNF-010: geometría en coordenadas de imagen (x derecha, y abajo), sin interfaz. */
public final class MedidorAngulos {
    private MedidorAngulos() {}

    /** Lado evaluado: en perfil DERECHO mira a la derecha, IZQUIERDO a la izquierda. */
    public enum Lado { DERECHO, IZQUIERDO }

    /** Punto en píxeles de la foto original; no mezclar escalas distintas para x e y. */
    public record Punto(double x, double y) {
        /** Rechaza coordenadas no finitas (RF-017). */
        public Punto {
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                throw new IllegalArgumentException("Las coordenadas deben ser números finitos.");
            }
        }
    }

    /** Grados: positivo = flexión, negativo = extensión; antebrazo siempre entre 0 y 180. */
    public record Angulos(double brazo, double antebrazo, double muneca, double cuello, double tronco) {
        /** Valida también los ángulos escritos a mano; solo cero es neutro (RF-017). */
        public Angulos {
            validar(brazo, -180, 180);
            validar(antebrazo, 0, 180);
            validar(muneca, -180, 180);
            validar(cuello, -180, 180);
            validar(tronco, -180, 180);
        }
    }

    /** Mide los cinco segmentos a partir de los seis puntos anatómicos (RF-017). */
    public static Angulos medir(Punto cadera, Punto hombro, Punto codo, Punto muneca,
                                 Punto nudillos, Punto coronilla, Lado lado) {
        return new Angulos(brazo(cadera, hombro, codo, lado), antebrazo(hombro, codo, muneca),
                muneca(codo, muneca, nudillos), cuello(cadera, hombro, coronilla, lado),
                tronco(cadera, hombro, lado));
    }

    /** Mide brazo respecto al tronco y distingue flexión de extensión (RF-017). */
    public static double brazo(Punto cadera, Punto hombro, Punto codo, Lado lado) {
        Punto u = restar(cadera, hombro), v = restar(codo, hombro);
        return conSentido(angulo(u, v), cruz(u, v) * direccion(lado) <= 0);
    }

    /** Mide la flexión del codo: 0° con el brazo estirado (RF-017). */
    public static double antebrazo(Punto hombro, Punto codo, Punto muneca) {
        return 180 - angulo(restar(hombro, codo), restar(muneca, codo));
    }

    /** Mide la desviación de la muñeca: 0° cuando está recta (RF-017). */
    public static double muneca(Punto codo, Punto muneca, Punto nudillos) {
        return 180 - angulo(restar(codo, muneca), restar(nudillos, muneca));
    }

    /** Mide cuello respecto al tronco y distingue extensión (RF-017). */
    public static double cuello(Punto cadera, Punto hombro, Punto coronilla, Lado lado) {
        Punto u = restar(hombro, cadera), v = restar(coronilla, hombro);
        return conSentido(angulo(u, v), cruz(u, v) * direccion(lado) >= 0);
    }

    /** Mide tronco respecto a la vertical ascendente (RF-017). */
    public static double tronco(Punto cadera, Punto hombro, Lado lado) {
        Punto u = restar(hombro, cadera);
        return conSentido(angulo(u, new Punto(0, -1)), u.x() * direccion(lado) >= 0);
    }

    private static int direccion(Lado lado) {
        return Objects.requireNonNull(lado, "Elige el lado evaluado.") == Lado.DERECHO ? 1 : -1;
    }

    private static Punto restar(Punto a, Punto b) {
        Objects.requireNonNull(a, "Falta un punto de la foto.");
        Objects.requireNonNull(b, "Falta un punto de la foto.");
        return new Punto(a.x() - b.x(), a.y() - b.y());
    }

    private static double angulo(Punto u, Punto v) {
        double nu = Math.hypot(u.x(), u.y()), nv = Math.hypot(v.x(), v.y());
        if (nu == 0 || nv == 0 || !Double.isFinite(nu) || !Double.isFinite(nv)) {
            throw new IllegalArgumentException("Separa los puntos coincidentes para medir el ángulo.");
        }
        double coseno = (u.x() / nu) * (v.x() / nv) + (u.y() / nu) * (v.y() / nv);
        if (cruz(u, v) == 0) return coseno >= 0 ? 0 : 180;
        double grados = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, coseno))));
        // RNF-010: elimina ruido como 20.00000000000001 sin redondear a grados enteros
        // ni aplicar la tolerancia de 5° del mock. Los valores manuales no se redondean.
        return Math.rint(grados * 1e10) / 1e10;
    }

    private static double cruz(Punto u, Punto v) {
        // Normalizar evita desbordamientos sin cambiar el signo del producto cruz.
        double nu = Math.hypot(u.x(), u.y()), nv = Math.hypot(v.x(), v.y());
        return (u.x() / nu) * (v.y() / nv) - (u.y() / nu) * (v.x() / nv);
    }

    private static double conSentido(double grados, boolean flexion) {
        return grados == 0 || flexion ? grados : -grados;
    }

    static void validar(double grados, double minimo, double maximo) {
        if (!Double.isFinite(grados) || grados < minimo || grados > maximo) {
            throw new IllegalArgumentException("El ángulo debe estar entre " + minimo + " y " + maximo + " grados.");
        }
    }
}
