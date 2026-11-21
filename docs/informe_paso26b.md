# Paso 26B · código ordenado

- Ingreso, Inicio, Agenda y práctica delegan en AccesoWeb, InicioWeb, AgendaWeb y DemoWeb.
- AgendaDao separa JDBC de las reglas; Calendario y PaginasAgenda conservan la presentación.
- Inicio, Ayuda, Consultas y la cabecera usan fragmentos con nombre; los avisos se componen una sola vez.
- Imports explícitos, instrucciones separadas, nombres internos aclarados y comentarios de requisito.
- Líneas Java >120: 1.342 antes, 16 después; 0 fuera de las exclusiones aprobadas por Andy.
- LongitudFuentesTest deja explícito «código protegido, RNF-010»: paquete RULA y bloque POST calcular.
- Motor y bloque de cálculo idénticos byte por byte; SQL, esquema, rutas, IA y fotos de galería sin cambios.
- Suite completa única: 1.036 ejecutadas/aprobadas, 0 fallos/errores/omitidas; JS 122/122, 0 fallos/omitidas.

Las pruebas previas se conservaron: solo formato, imports y nombres internos en este paso;
se agregó la prueba de longitud. No se añadieron bibliotecas, frameworks ni dependencias Maven.
Se usó el formateador de IntelliJ ya instalado, sin descargar software.

## Resumen del cierre 26 + 26B

| Suite final | Ejecutadas | Aprobadas al cierre | Fallos pendientes |
|---|---:|---:|---:|
| Paso 26 Maven | 1035 únicas | 1035 verificadas | 0 |
| Paso 26 JS | 122 | 122 | 0 |
| Paso 26B Maven | 1036 | 1036 | 0 |
| Paso 26B JS | 122 | 122 | 0 |

La corrida completa del 26 señaló 7 fallos, corregidos y comprobados con ejecución dirigida;
no se repitió esa suite completa. El 26B pasó completo a la primera.
Migración real: 22 tablas, 449 filas antes/después, dos ejecuciones sin duplicados ni eliminaciones.
La carga posterior de la demo sí agrega sus propios datos. Las nueve cuentas y el guion están en CUENTAS.md.
La demo nueva tiene 17/19/16 citas por Andrés/Johan/Gabriella; Johan incluye 3 históricas de 2025
 y una cancelada futura. Los horarios ficticios anteriores se conservaron fuera del período de presentación.

| Pantalla | 360 px | 390 px | 820 px | 1366 px |
|---|---:|---:|---:|---:|
| /login | 0 | 0 | 0 | 0 |
| /cuenta | 0 | 0 | 0 | 0 |
| /archivo | 0 | 0 | 0 | 0 |
| /archivo?empresa | 0 | 0 | 0 | 0 |
| /agenda mes | 0 | 0 | 0 | 0 |
| /agenda semana | 0 | 0 | 0 | 0 |
| /ayuda | 0 | 0 | 0 | 0 |

Medidas DOM del Paso 26, sin capturas; el 26B conserva la presentación. También 0 px a 1024.
Pendientes de entorno: iPad físico, Windows y tres computadores en red. Contacto empresarial real pendiente
 del archivo local. Fuera de alcance: hosting, Spring Boot, logo nuevo, Notion y diagramas.
