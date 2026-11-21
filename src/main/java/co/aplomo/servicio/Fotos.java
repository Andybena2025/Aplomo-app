package co.aplomo.servicio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * RF-016, RNF-006: comprueba imágenes reales y guarda archivos con identidad aleatoria.
 */
public final class Fotos {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final Path raiz;

    /**
     * Una foto recibida; el nombre original no se utiliza para guardar ni abrir archivos. RNF-011.
     */
    public record Entrada(byte[] bytes, String vista, String descripcion) {
    }

    /**
     * Imagen validada, lista para almacenar con extensión obtenida de su contenido. RNF-011.
     */
    public record Imagen(byte[] bytes, String extension, String vista, String descripcion) {
    }

    /**
     * Usa la carpeta del servidor, configurable para aislar los archivos de las pruebas. RNF-011.
     */
    public Fotos() {
        this(co.aplomo.config.Config.carpetaFotos());
    }

    /**
     * Recibe una carpeta aislada para las pruebas. RNF-011.
     */
    public Fotos(Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
    }

    /**
     * Valida cantidad, tamaño, firma, decodificación, vista y descripción antes de escribir. RNF-011.
     */
    public List<Imagen> validar(List<Entrada> entradas) {
        if (entradas == null || entradas.isEmpty() || entradas.size() > 10)
            throw new IllegalArgumentException("Agrega de 1 a 10 fotos JPG o PNG, de hasta 5 MB cada una.");
        List<Imagen> imagenes = new ArrayList<>();
        for (Entrada elemento : entradas) {
            if (elemento.bytes() == null || elemento.bytes().length == 0 || elemento.bytes().length > MAX_BYTES)
                throw new IllegalArgumentException("Cada foto debe tener contenido y pesar como máximo 5 MB.");
            byte[] elemento2 = elemento.bytes();
            boolean png = elemento2.length >= 8 &&
                    Arrays.equals(Arrays.copyOf(elemento2, 8), new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            boolean jpg = elemento2.length >= 3 && elemento2[0] == (byte) 255 && elemento2[1] == (byte) 216 &&
                    elemento2[2] == (byte) 255;
            if (!png && !jpg) throw formato();
            try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(elemento2))) {
                Iterator<ImageReader> lectores = ImageIO.getImageReaders(in);
                if (!lectores.hasNext()) throw formato();
                ImageReader lector = lectores.next();
                try {
                    lector.setInput(in, true, true);
                    String tipo = lector.getFormatName();
                    if (!(png && tipo.equalsIgnoreCase("png") || jpg && tipo.equalsIgnoreCase("jpeg"))) throw formato();
                    int ancho = lector.getWidth(0), alto = lector.getHeight(0);
                    if (ancho <= 0 || alto <= 0) throw formato();

                    // Decodifica una muestra acotada para comprobar el contenido sin expandir fotos enormes en
                    // memoria.
                    var parametros = lector.getDefaultReadParam();
                    parametros.setSourceSubsampling((int) Math.max(1L, (ancho + 511L) / 512),
                            (int) Math.max(1L, (alto + 511L) / 512), 0, 0);
                    if (lector.read(0, parametros) == null) throw formato();
                } finally {
                    lector.dispose();
                }
            } catch (IOException | IndexOutOfBoundsException ex) {
                throw formato();
            }
            String vista = Validacion.opcion(elemento.vista(), "vista", "PERFIL", "FRENTE", "OTRA");
            String descripcion = Validacion.texto(elemento.descripcion(), "descripción de la foto", 200);
            imagenes.add(new Imagen(elemento2, png ? "png" : "jpg", vista, descripcion));
        }
        return List.copyOf(imagenes);
    }

    /**
     * Escribe una imagen nueva sin reutilizar nombres originales ni sobrescribir archivos. RNF-011.
     */
    public String guardar(Imagen imagen) throws IOException {
        Files.createDirectories(raiz);
        String nombre = UUID.randomUUID() + "." + imagen.extension();
        Path destino = raiz.resolve(nombre);

        // Si la escritura falla, solo se retira el archivo creado por esta operación.
        OutputStream salida = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW);
        try (salida) {
            salida.write(imagen.bytes());
        } catch (IOException fallo) {
            try {
                Files.deleteIfExists(destino);
            } catch (IOException limpieza) {
                fallo.addSuppressed(limpieza);
            }
            throw fallo;
        }
        return nombre;
    }

    /**
     * Lee únicamente nombres generados por la aplicación, dentro de la carpeta protegida. RNF-011.
     */
    public byte[] leer(String nombre) throws IOException {
        Path archivo = ruta(nombre);
        if (Files.isSymbolicLink(archivo) || !archivo.toRealPath().startsWith(raiz.toRealPath()))
            throw new IOException("Foto no disponible.");
        return Files.readAllBytes(archivo);
    }

    /**
     * Lee dimensiones de la imagen protegida para medir usando su proporción real (RF-017).
     */
    public int[] dimensiones(String nombre) throws IOException {
        byte[] bytes = leer(nombre);
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var lectores = ImageIO.getImageReaders(in);
            if (!lectores.hasNext()) throw new IOException("Foto no disponible.");
            var lector = lectores.next();
            try {
                lector.setInput(in);
                int ancho = lector.getWidth(0), alto = lector.getHeight(0);
                return orientacion(bytes) >= 5 ? new int[]{alto, ancho} : new int[]{ancho, alto};
            } finally {
                lector.dispose();
            }
        }
    }

    /**
     * Retira solo archivos nuevos de una operación fallida; nunca fotografías de registros guardados. RNF-011.
     */
    public void revertirNuevos(List<String> nombres) throws IOException {
        IOException fallo = null;
        for (String nombre : nombres)
            try {
                Files.deleteIfExists(ruta(nombre));
            } catch (IOException error) {
                fallo = error;
            }
        if (fallo != null) throw fallo;
    }

    // EXIF no cambia los bytes: corrige la proporción que muestra el navegador al girar fotos del teléfono.
    private static int orientacion(byte[] elemento) {
        if (elemento.length > 3 && (elemento[0] & 255) == 255 && (elemento[1] & 255) == 216) {
            int i = 2;
            while (i + 4 <= elemento.length && (elemento[i] & 255) == 255) {
                int marca = elemento[i + 1] & 255;
                if (marca == 218 || marca == 217) break;
                int cantidad2 = ((elemento[i + 2] & 255) << 8) | (elemento[i + 3] & 255);
                if (cantidad2 < 2 || i + 2L + cantidad2 > elemento.length) break;
                if (marca == 225 && cantidad2 >= 8 && elemento[i + 4] == 'E' && elemento[i + 5] == 'x' &&
                        elemento[i + 6] == 'i' && elemento[i + 7] == 'f' && elemento[i + 8] == 0 &&
                        elemento[i + 9] == 0) return orientacionTiff(elemento, i + 10, cantidad2 - 8);
                i += cantidad2 + 2;
            }
        } else if (elemento.length >= 8 && (elemento[0] & 255) == 137 && elemento[1] == 'P' && elemento[2] == 'N' &&
                elemento[3] == 'G') {
            int i = 8;
            while (i + 12 <= elemento.length) {
                long cantidad3 = ((long) (elemento[i] & 255) << 24) | ((long) (elemento[i + 1] & 255) << 16) |
                        ((long) (elemento[i + 2] & 255) << 8) | (elemento[i + 3] & 255);
                if (cantidad3 > elemento.length - i - 12) break;
                if (elemento[i + 4] == 'e' && elemento[i + 5] == 'X' && elemento[i + 6] == 'I' &&
                        elemento[i + 7] == 'f') return orientacionTiff(elemento, i + 8, (int) cantidad3);
                i += (int) cantidad3 + 12;
            }
        }
        return 1;
    }

    private static int orientacionTiff(byte[] elemento, int base, int longitud) {
        if (longitud < 8) return 1;
        boolean le = elemento[base] == 'I' && elemento[base + 1] == 'I';
        if (!le && !(elemento[base] == 'M' && elemento[base + 1] == 'M')) return 1;
        var elemento2 = java.nio.ByteBuffer.wrap(elemento, base, longitud).slice()
                .order(le ? java.nio.ByteOrder.LITTLE_ENDIAN : java.nio.ByteOrder.BIG_ENDIAN);
        if (Short.toUnsignedInt(elemento2.getShort(2)) != 42) return 1;
        long offset = Integer.toUnsignedLong(elemento2.getInt(4));
        if (offset > longitud - 2) return 1;
        int inicio = (int) offset, cantidad = Short.toUnsignedInt(elemento2.getShort(inicio));
        for (int j = 0; j < cantidad; j++) {
            long pos = inicio + 2L + j * 12L;
            if (pos > longitud - 12) return 1;
            int cantidad2 = (int) pos;
            if (Short.toUnsignedInt(elemento2.getShort(cantidad2)) == 274 &&
                    Short.toUnsignedInt(elemento2.getShort(cantidad2 + 2)) == 3 &&
                    elemento2.getInt(cantidad2 + 4) == 1) {
                int cantidad3 = Short.toUnsignedInt(elemento2.getShort(cantidad2 + 8));
                return cantidad3 >= 1 && cantidad3 <= 8 ? cantidad3 : 1;
            }
        }
        return 1;
    }

    private Path ruta(String nombre) throws IOException {
        if (nombre == null || !nombre.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(jpg|png)"))
            throw new IOException("Foto no disponible.");
        return raiz.resolve(nombre);
    }

    private static IllegalArgumentException formato() {
        return new IllegalArgumentException(
                "El archivo no es una imagen JPG o PNG válida. No basta con cambiar su extensión.");
    }
}
