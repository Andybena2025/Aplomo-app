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

    /** Lee dimensiones de la imagen protegida para medir usando su proporción real (RF-017). */
    public int[] dimensiones(String nombre) throws IOException {
        byte[] bytes=leer(nombre);
        try(ImageInputStream in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var lectores=ImageIO.getImageReaders(in);if(!lectores.hasNext())throw new IOException("Foto no disponible.");
            var lector=lectores.next();try{lector.setInput(in);int ancho=lector.getWidth(0),alto=lector.getHeight(0);return orientacion(bytes)>=5?new int[]{alto,ancho}:new int[]{ancho,alto};}finally{lector.dispose();}
        }
    }

    /** Retira solo archivos nuevos de una operación fallida; nunca fotografías de registros guardados. */
    public void revertirNuevos(List<String> nombres) throws IOException {
        IOException fallo = null;
        for (String nombre : nombres) try { Files.deleteIfExists(ruta(nombre)); }
        catch (IOException e) { fallo = e; }
        if (fallo != null) throw fallo;
    }

    // EXIF no cambia los bytes: corrige la proporción que muestra el navegador al girar fotos del teléfono.
    private static int orientacion(byte[] b) {
        if(b.length>3&&(b[0]&255)==255&&(b[1]&255)==216) {
            int i=2;
            while(i+4<=b.length&&(b[i]&255)==255) {
                int marca=b[i+1]&255;if(marca==218||marca==217)break;
                int n=((b[i+2]&255)<<8)|(b[i+3]&255);if(n<2||i+2L+n>b.length)break;
                if(marca==225&&n>=8&&b[i+4]=='E'&&b[i+5]=='x'&&b[i+6]=='i'&&b[i+7]=='f'&&b[i+8]==0&&b[i+9]==0)return orientacionTiff(b,i+10,n-8);
                i+=n+2;
            }
        }else if(b.length>=8&&(b[0]&255)==137&&b[1]=='P'&&b[2]=='N'&&b[3]=='G') {
            int i=8;
            while(i+12<=b.length) {
                long n=((long)(b[i]&255)<<24)|((long)(b[i+1]&255)<<16)|((long)(b[i+2]&255)<<8)|(b[i+3]&255);
                if(n>b.length-i-12)break;
                if(b[i+4]=='e'&&b[i+5]=='X'&&b[i+6]=='I'&&b[i+7]=='f')return orientacionTiff(b,i+8,(int)n);
                i+=(int)n+12;
            }
        }
        return 1;
    }
    private static int orientacionTiff(byte[] b,int base,int longitud) {
        if(longitud<8)return 1;boolean le=b[base]=='I'&&b[base+1]=='I';
        if(!le&&!(b[base]=='M'&&b[base+1]=='M'))return 1;
        var t=java.nio.ByteBuffer.wrap(b,base,longitud).slice().order(le?java.nio.ByteOrder.LITTLE_ENDIAN:java.nio.ByteOrder.BIG_ENDIAN);
        if(Short.toUnsignedInt(t.getShort(2))!=42)return 1;
        long offset=Integer.toUnsignedLong(t.getInt(4));if(offset>longitud-2)return 1;int inicio=(int)offset,cantidad=Short.toUnsignedInt(t.getShort(inicio));
        for(int j=0;j<cantidad;j++) {
            long pos=inicio+2L+j*12L;if(pos>longitud-12)return 1;int p=(int)pos;
            if(Short.toUnsignedInt(t.getShort(p))==274&&Short.toUnsignedInt(t.getShort(p+2))==3&&t.getInt(p+4)==1){int o=Short.toUnsignedInt(t.getShort(p+8));return o>=1&&o<=8?o:1;}
        }
        return 1;
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
