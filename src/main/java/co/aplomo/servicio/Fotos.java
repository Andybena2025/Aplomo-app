package co.aplomo.servicio;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

/** RF-016, RNF-006: comprueba imágenes reales y guarda archivos con identidad aleatoria. */
public final class Fotos {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final Path raiz;

    /** Una foto recibida; el nombre original no se utiliza para guardar ni abrir archivos. */
    public record Entrada(byte[] bytes, String vista, String descripcion) {}
    /** Imagen validada, lista para almacenar con extensión obtenida de su contenido. */
    public record Imagen(byte[] bytes, String extension, String vista, String descripcion) {}

    /** Usa la carpeta del servidor, configurable para aislar los archivos de las pruebas. */
    public Fotos() { this(Path.of(System.getProperty("aplomo.fotos", "fotos"))); }
    /** Recibe una carpeta aislada para las pruebas. */
    public Fotos(Path raiz) { this.raiz = raiz.toAbsolutePath().normalize(); }

    /** Valida cantidad, tamaño, firma, decodificación, vista y descripción antes de escribir. */
    public List<Imagen> validar(List<Entrada> entradas) {
        if (entradas == null || entradas.isEmpty() || entradas.size() > 10)
            throw new IllegalArgumentException("Agrega de 1 a 10 fotos JPG o PNG, de hasta 5 MB cada una.");
        List<Imagen> imagenes = new ArrayList<>();
        for (Entrada e : entradas) {
            if (e.bytes() == null || e.bytes().length == 0 || e.bytes().length > MAX_BYTES)
                throw new IllegalArgumentException("Cada foto debe tener contenido y pesar como máximo 5 MB.");
            byte[] b = e.bytes();
            boolean png = b.length >= 8 && Arrays.equals(Arrays.copyOf(b, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
            boolean jpg = b.length >= 3 && b[0] == (byte)255 && b[1] == (byte)216 && b[2] == (byte)255;
            if (!png && !jpg) throw formato();
            try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(b))) {
                Iterator<ImageReader> lectores = ImageIO.getImageReaders(in);
                if (!lectores.hasNext()) throw formato();
                ImageReader lector = lectores.next();
                try {
                    lector.setInput(in, true, true);
                    String tipo = lector.getFormatName();
                    if (!(png && tipo.equalsIgnoreCase("png") || jpg && tipo.equalsIgnoreCase("jpeg"))) throw formato();
                    int ancho = lector.getWidth(0), alto = lector.getHeight(0);
                    if (ancho <= 0 || alto <= 0) throw formato();
                    // Decodifica una muestra acotada para comprobar el contenido sin expandir fotos enormes en memoria.
                    var parametros = lector.getDefaultReadParam();
                    parametros.setSourceSubsampling((int)Math.max(1L, (ancho + 511L) / 512), (int)Math.max(1L, (alto + 511L) / 512), 0, 0);
                    if (lector.read(0, parametros) == null) throw formato();
                } finally { lector.dispose(); }
            } catch (IOException | IndexOutOfBoundsException ex) { throw formato(); }
            String vista = Validacion.opcion(e.vista(), "vista", "PERFIL", "FRENTE", "OTRA");
            String descripcion = Validacion.texto(e.descripcion(), "descripción de la foto", 200);
            imagenes.add(new Imagen(b, png ? "png" : "jpg", vista, descripcion));
        }
        return List.copyOf(imagenes);
    }

    /** Escribe una imagen nueva sin reutilizar nombres originales ni sobrescribir archivos. */
    public String guardar(Imagen imagen) throws IOException {
        Files.createDirectories(raiz);
        String nombre = UUID.randomUUID() + "." + imagen.extension();
        Path destino=raiz.resolve(nombre);
        // Si la escritura falla, solo se retira el archivo creado por esta operación.
        OutputStream salida=Files.newOutputStream(destino,StandardOpenOption.CREATE_NEW);
        try (salida) { salida.write(imagen.bytes()); }
        catch (IOException fallo) {
            try { Files.deleteIfExists(destino); } catch(IOException limpieza) { fallo.addSuppressed(limpieza); }
            throw fallo;
        }
        return nombre;
    }

    /** Lee únicamente nombres generados por la aplicación, dentro de la carpeta protegida. */
    public byte[] leer(String nombre) throws IOException {
        Path archivo = ruta(nombre);
        if (Files.isSymbolicLink(archivo) || !archivo.toRealPath().startsWith(raiz.toRealPath()))
            throw new IOException("Foto no disponible.");
        return Files.readAllBytes(archivo);
    }

    /** Retira solo archivos nuevos de una operación fallida; nunca fotografías de registros guardados. */
    public void revertirNuevos(List<String> nombres) throws IOException {
        IOException fallo = null;
        for (String nombre : nombres) try { Files.deleteIfExists(ruta(nombre)); }
        catch (IOException e) { fallo = e; }
        if (fallo != null) throw fallo;
    }

    private Path ruta(String nombre) throws IOException {
        if (nombre == null || !nombre.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(jpg|png)"))
            throw new IOException("Foto no disponible.");
        return raiz.resolve(nombre);
    }
    private static IllegalArgumentException formato() {
        return new IllegalArgumentException("El archivo no es una imagen JPG o PNG válida. No basta con cambiar su extensión.");
    }
}
