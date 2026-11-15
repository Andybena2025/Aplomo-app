package co.aplomo.instalacion;

import co.aplomo.modelo.Cuenta;
import co.aplomo.modelo.Cuenta.Rol;
import co.aplomo.servicio.*;
import co.aplomo.seguridad.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

/** Importación voluntaria por el servicio de Usuarios; nunca guarda claves en archivos. */
public final class CrearCuentas {
    private CrearCuentas() {}
    private static final SecureRandom AZAR=new SecureRandom();
    private static final String LETRAS="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz",NUMEROS="23456789";
    public record Fila(String nombre,String correo,Rol rol,String nit) {}
    public record Contacto(String telefono,String cargo) {}

    /** La terminal real mantiene la contraseña de ingreso oculta, fuera de argumentos y archivos. */
    public static void main(String[] args) {
        try {
            Console terminal=System.console();
            if(terminal==null)throw new IllegalArgumentException("Abre una Terminal y ejecuta el script sin redirigir su salida. Las claves se muestran solo allí.");
            List<Fila> filas;
            try(Reader r=Files.newBufferedReader(Path.of("config/cuentas.csv"),StandardCharsets.UTF_8)){filas=leer(r);}
            String correo=terminal.readLine("Correo del Administrador actual: ");
            char[] entrada=terminal.readPassword("Contraseña actual (no se muestra): ");
            if(correo==null||entrada==null)throw new IllegalArgumentException("Se canceló la lectura. No se crearon cuentas.");
            Cuenta actor;
            try{actor=new Autenticacion().ingresar(correo,new String(entrada));}
            finally{Arrays.fill(entrada,'\0');}
            if(actor==null)throw new IllegalArgumentException("No pudimos iniciar sesión. Revisa los datos o el bloqueo de la cuenta.");
            int errores=crear(new Usuarios(),new Empresas(),actor,filas,f->{
                terminal.printf("Datos de %s%n",f.correo());
                return new Contacto(terminal.readLine("Teléfono: "),terminal.readLine("Cargo: "));
            },terminal.writer());
            if(errores>0)System.exit(1);
        }catch(IllegalArgumentException|SecurityException e){System.err.println(e.getMessage());System.exit(1);}
        catch(IOException e){System.err.println("No pudimos leer config/cuentas.csv. Copia la plantilla y guárdala como CSV UTF-8.");System.exit(1);}
        catch(SQLException e){System.err.println("No pudimos completar la operación. Revisa MySQL y los permisos; las cuentas existentes se conservan.");System.exit(1);}
    }

    /** Cuatro columnas; comillas dobles permiten comas en nombres. Sin contraseñas en el CSV. */
    public static List<Fila> leer(Reader entrada)throws IOException {
        var filas=new ArrayList<Fila>();var correos=new HashSet<String>();
        var lineas=new BufferedReader(entrada);String primera=lineas.readLine();
        if(primera==null||!campos(primera.replaceFirst("^\\uFEFF","")).equals(List.of("nombre","correo","rol","nit_empresa")))
            throw new IllegalArgumentException("La primera línea debe ser: nombre,correo,rol,nit_empresa.");
        String linea;int numero=1;
        while((linea=lineas.readLine())!=null){numero++;if(linea.isBlank())continue;
            if(linea.length()>4000||filas.size()>=1000)throw new IllegalArgumentException("El archivo admite hasta 1000 cuentas y líneas de hasta 4000 caracteres.");
            var c=campos(linea);if(c.size()!=4)throw new IllegalArgumentException("Revisa las cuatro columnas de la línea "+numero+".");
            Rol rol;try{rol=Rol.valueOf(c.get(2));}catch(IllegalArgumentException e){throw new IllegalArgumentException("El rol de la línea "+numero+" debe ser ADMINISTRADOR o USUARIO.");}
            String correo=c.get(1).toLowerCase(Locale.ROOT),nit=c.get(3);
            // Reutiliza las reglas del formulario antes de crear ninguna cuenta.
            var datos=new Usuarios.Datos(c.get(0),correo,"Pendiente","Pendiente",rol,rol==Rol.USUARIO?1:null);
            if(rol==Rol.USUARIO&&nit.isBlank())throw new IllegalArgumentException("El Usuario de la línea "+numero+" necesita nit_empresa.");
            if(rol==Rol.ADMINISTRADOR&&!nit.isBlank())throw new IllegalArgumentException("Deja nit_empresa vacío para el Administrador de la línea "+numero+".");
            if(!correos.add(datos.correo()))throw new IllegalArgumentException("El mismo correo aparece más de una vez en el CSV (línea "+numero+").");
            filas.add(new Fila(datos.nombre(),datos.correo(),rol,nit));
        }
        if(filas.isEmpty())throw new IllegalArgumentException("El CSV no contiene cuentas para crear.");
        return List.copyOf(filas);
    }
    private static List<String> campos(String linea){
        var valores=new ArrayList<String>();var valor=new StringBuilder();boolean comillas=false,cerrada=false;
        for(int i=0;i<linea.length();i++){char c=linea.charAt(i);
            if(c=='"'){
                if(comillas&&i+1<linea.length()&&linea.charAt(i+1)=='"'){valor.append('"');i++;}
                else if(comillas){comillas=false;cerrada=true;}
                else if(valor.toString().isBlank()&&!cerrada){valor.setLength(0);comillas=true;}
                else throw new IllegalArgumentException("Revisa las comillas del CSV. Usa comillas dobles alrededor de un campo que contenga comas.");
            }else if(c==','&&!comillas){valores.add(valor.toString().strip());valor.setLength(0);cerrada=false;}
            else {if(cerrada&&!Character.isWhitespace(c))throw new IllegalArgumentException("Hay texto después de cerrar un campo entre comillas.");valor.append(c);}
        }
        if(comillas)throw new IllegalArgumentException("Falta cerrar una comilla del CSV. Cada cuenta debe estar en una sola línea.");
        valores.add(valor.toString().strip());return valores;
    }
    /** Dieciséis caracteres legibles, siempre con letras y números; bcrypt lo cifra en Usuarios.crear. */
    public static String temporal(){
        char[] clave=new char[16];clave[0]=LETRAS.charAt(AZAR.nextInt(LETRAS.length()));clave[1]=NUMEROS.charAt(AZAR.nextInt(NUMEROS.length()));String todos=LETRAS+NUMEROS;
        for(int i=2;i<clave.length;i++)clave[i]=todos.charAt(AZAR.nextInt(todos.length()));
        for(int i=clave.length-1;i>0;i--){int j=AZAR.nextInt(i+1);char c=clave[i];clave[i]=clave[j];clave[j]=c;}
        return new String(clave);
    }
    /** Tabla única: cada clave se imprime justo después de crear su cuenta y nunca se vuelve a imprimir. */
    public static int crear(Usuarios usuarios,Empresas empresas,Cuenta actor,List<Fila> filas,
                            Function<Fila,Contacto> preguntar,PrintWriter salida)throws SQLException {
        if(actor!=null&&actor.rol()==Rol.ADMINISTRADOR&&actor.temporal())throw new IllegalArgumentException("Primero cambia tu contraseña temporal desde la app y vuelve a ejecutar la herramienta.");
        Permisos.administrador(actor);
        var existentes=new HashSet<String>();for(var c:usuarios.listar(actor))existentes.add(c.correo().toLowerCase(Locale.ROOT));
        var porNit=new HashMap<String,Integer>();for(var e:empresas.listar(actor))if(e.activo())porNit.put(e.nit(),e.id());
        // Recoge datos obligatorios antes de imprimir la tabla, sin inventar teléfonos o cargos.
        var datos=new LinkedHashMap<Fila,Usuarios.Datos>();var errores=new HashMap<Fila,String>();
        for(var f:filas){if(existentes.contains(f.correo()))continue;
            Integer empresa=f.rol()==Rol.USUARIO?porNit.get(f.nit()):null;
            if(f.rol()==Rol.USUARIO&&empresa==null){errores.put(f,"no creada: la empresa no existe o está inactiva");continue;}
            try{var contacto=preguntar.apply(f);if(contacto==null)throw new IllegalArgumentException("Faltan teléfono y cargo.");datos.put(f,new Usuarios.Datos(f.nombre(),f.correo(),contacto.telefono(),contacto.cargo(),f.rol(),empresa));}
            catch(IllegalArgumentException e){errores.put(f,"no creada: "+e.getMessage());}
        }
        salida.println("Correo | Contraseña temporal / resultado");salida.println("-------|-------------------------------");int fallos=0;
        for(var f:filas){
            if(existentes.contains(f.correo())){salida.println(f.correo()+" | ya existe");continue;}
            if(errores.containsKey(f)){salida.println(f.correo()+" | "+errores.get(f));fallos++;continue;}
            String clave=temporal();
            try{usuarios.crear(actor,datos.get(f),clave);existentes.add(f.correo());salida.println(f.correo()+" | "+clave);salida.flush();}
            catch(IllegalArgumentException e){
                boolean existe=usuarios.listar(actor).stream().anyMatch(c->c.correo().equalsIgnoreCase(f.correo()));
                salida.println(f.correo()+" | "+(existe?"ya existe":"no creada: "+e.getMessage()));if(!existe)fallos++;
            }catch(SQLException e){salida.println(f.correo()+" | no creada: no pudimos guardar; revisa MySQL");fallos++;}
        }
        salida.flush();return fallos;
    }
}
