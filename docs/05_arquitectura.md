# Arquitectura (propuesta para Java sin frameworks)

## Tecnología

- **Java 17+**, proyecto **Maven** abierto en IntelliJ IDEA.
- **Servidor web:** `com.sun.net.httpserver.HttpServer` (viene con el JDK). Puerto 8080.
- **Base de datos:** MySQL 8 con el script `docs/base_de_datos/Script_MySQL_RULA.sql`. Acceso con **JDBC** y `PreparedStatement`.
- **Dependencias** (las únicas): `com.mysql:mysql-connector-j`, `org.mindrot:jbcrypt` y `org.junit.jupiter:junit-jupiter` (solo pruebas).
- **Interfaz:** páginas HTML generadas por el servidor + un CSS propio (copiado del mock) + JavaScript puro para el canvas de puntos, los filtros de tablas y los mensajes.

## Capas (RNF-011: datos, cálculo RULA e interfaz separados)

```
src/main/java/co/aplomo/
  App.java                  arranque: lee config, crea el servidor y registra las rutas
  config/Config.java        lee config/db.properties
  db/Conexion.java          entrega conexiones JDBC (una por petición)
  modelo/                   clases simples (Cuenta, Empresa, Sede, Salon, Area, Cargo, Puesto, Reserva,
                            Visita, Evaluacion, CondicionesPuesto, Fotografia, AnalisisRula,
                            MedicionAngulo, Sugerencia, PendienteArreglo, NivelRiesgo...) = diagrama de clases
  dao/                      un DAO por tabla, solo SQL con PreparedStatement (sin DELETE)
  rula/                     EL ÚNICO lugar del cálculo (RF-018 a RF-021, RF-027, RNF-010)
    MedidorAngulos.java     3 puntos → grados y sentido
    PuntuadorSegmentos.java grados + ajustes → puntos por segmento
    TablasRula.java         carga valor_tabla_rula y nivel_riesgo una vez
    MotorRula.java          segmentos + actividad + carga → A, B, C, final, nivel
  servicio/                 reglas de negocio (duplicados RF-010, estados RF-012, finalizar visita,
                            pendientes, sugerencias, calificaciones, bitácora RNF-005)
  seguridad/                Contrasenas (bcrypt), Sesiones (cookie + 30 min), Permisos (RF-002), intentos
  web/                      Router, manejadores por sección, Html (plantillas y escape), Multipart (fotos)
src/main/resources/
  static/css/aplomo.css     tokens y componentes del mock (docs/07_estilo_visual.md)
  static/js/                lienzo.js (puntos), tablas.js (ordenar y filtrar), avisos.js
  plantillas/               HTML base con huecos {{titulo}}, {{contenido}}...
src/test/java/co/aplomo/rula/   pruebas de tablas y de los 10 casos a mano (RNF-010)
config/db.properties.ejemplo    url, usuario y clave de MySQL (la real va en db.properties, ignorada por git)
fotos/                          carpeta de fotos subidas (ignorada por git)
```

## Seguridad (RF-001a a RF-003, RNF-004, RNF-006)

- **Sesión:** id aleatorio (`SecureRandom`, 32 bytes) en una cookie `HttpOnly`, `SameSite=Strict`. En memoria: id → cuenta, rol, empresa y último acceso. Expira a los **30 minutos** sin actividad (RF-001b).
- **Ingreso:** mensaje genérico si falla. Después de **5 intentos** seguidos se bloquea **15 minutos** (`intentos_fallidos`, `bloqueado_hasta`). Si la contraseña es temporal, se obliga a cambiarla antes de entrar a cualquier otra página.
- **Permisos:** cada ruta declara qué rol puede usarla. El Usuario solo ve los registros cuyo `id_empresa` es el de su cuenta (se filtra en el SQL, no en la pantalla).
- **Fotos:** se sirven desde una ruta que revisa la sesión y el rol; un Usuario o alguien sin sesión ve "Acceso denegado". Validar tipo (JPG/PNG por sus primeros bytes), tamaño (5 MB) y cantidad (10).
- **Entradas:** validar todo en el servidor; escapar el HTML al mostrar texto; formularios POST con un token anti-CSRF guardado en la sesión.
- **bcrypt y el admin inicial:** el script crea `admin@rula.local` con la contraseña temporal `Cambiar2026` (hash `$2b$10$...`, ya verificado). Ojo: **jBCrypt 0.4 solo acepta el prefijo `$2a$`** y lanza "Invalid salt revision" con `$2b$`. Son equivalentes: antes de `BCrypt.checkpw`, cambia `$2b$` por `$2a$` en el hash leído (y genera los nuevos con `BCrypt.gensalt(10)`).
- **MySQL:** la app usa el usuario `app_rula` (solo SELECT, INSERT y UPDATE), creado por el script. La clave se cambia y va en `config/db.properties`.

## Rutas sugeridas

```
GET  /login                 POST /login              POST /logout
GET  /cuenta/contrasena     POST /cuenta/contrasena
GET  /                      inicio por rol
GET  /usuarios              POST /usuarios ...       (Administrador)
GET  /empresas              GET /empresas/{id}       POST /empresas ... (sedes, salones, áreas, cargos, puestos)
GET  /reservas              POST /reservas           POST /reservas/{id}/aceptar | /cancelar
GET  /visitas/{id}          POST /visitas/{id}/iniciar | /finalizar
GET  /evaluaciones/nueva?visita={id}   pasos 1-6     POST /evaluaciones/...
GET  /fotos/{id}            (con permiso)
GET  /pendientes            POST /pendientes/{id}/arreglado
GET  /sugerencias           POST /sugerencias/...
GET  /calificaciones?empresa={id}
GET  /informes/empresa/{id} (vista para imprimir)
GET  /static/...            CSS, JS e imágenes
```

## Bitácora (RNF-005)

Cada creación y cada cambio de estado de reserva, visita, evaluación y análisis escribe en `bitacora` quién, cuándo y qué cambió.
