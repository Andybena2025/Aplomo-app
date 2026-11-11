# APLOMO.

**Ergonomia con precision** — Sistema web para evaluar posturas en puestos de trabajo con el metodo RULA.

Proyecto Nuclear de Ingenieria de Software, 3.er semestre, Corporacion Universitaria Empresarial Alexander von Humboldt, periodo 2026-2.

## Tecnologias

- **Java 17+** con Maven
- **MySQL 8** (21 tablas, tablas RULA precargadas)
- Servidor HTTP del JDK (`com.sun.net.httpserver.HttpServer`)
- JDBC con `PreparedStatement` (sin ORM)
- bcrypt para contrasenas (`org.mindrot:jbcrypt`)
- JUnit 5 para pruebas
- HTML, CSS y JavaScript puro (sin frameworks)

## Como correrlo

1. **Solo para instalación inicial, si la base no existe**, crear la base de datos en MySQL:
   ```bash
   mysql -u root -p < docs/base_de_datos/Script_MySQL_RULA.sql
   ```
   Si `rula_ergonomia` ya existe, omitir este paso: el script empieza borrando esa base.

2. **Configurar la conexion:**
   ```bash
   cp -n config/db.properties.ejemplo config/db.properties
   ```
   Editar `config/db.properties` con tu usuario y clave de MySQL.

   Si el archivo ya existe, conservarlo; `cp -n` evita reemplazarlo.

3. **Compilar y arrancar:**
   ```bash
   mvn compile
   mvn exec:java
   ```

4. **Abrir** http://localhost:8080 en Chrome, Edge o Firefox.

## Estado de implementación y pruebas

Fase 2 completa: conexión JDBC, lectura de tablas y niveles desde MySQL, medición de ángulos y motor RULA. Corresponde al paso 1 del plan. La fase 3 (pasos 2 y 3) está completa: ingreso, salida, contraseñas, bloqueo, inactividad, permisos y administración de cuentas.

La fase 4 (pasos 4, 5 y 6) está completa: empresas y su estructura, reservas con estados y bitácora, e inicio y finalización de visitas. El Paso 7 de la fase 5 está implementado: puesto y postura, seis condiciones, consentimiento y fotos privadas. El Paso 8 está implementado: asistente completo con puntos, ángulos, ajustes, resultados y análisis independientes por foto. El Paso 9 está implementado: pendientes automáticos al guardar un Rojo, seguimiento, estado Arreglado y consulta por empresa. El Paso 10 está implementado: sugerencias automáticas y manuales, edición, quitar sin borrar, estados y acceso a reservar una Reevaluación. El Paso 11 está implementado: once tablas con búsqueda, filtros combinables y orden, con permisos por empresa y resultados de visitas finalizadas para el Usuario. Espera la aprobación de Andy antes del Paso 12. Una visita nueva no se puede cerrar sin evaluaciones.

Con MySQL y la conexión configurados, ejecutar `mvn test`: **686 pruebas aprobadas**, sin fallos, errores ni omisiones, incluidas las 389 de la fase 2 (272 combinaciones de tablas y 10 casos manuales). Instrucciones y desglose: [Pruebas de la fase 2](docs/pruebas_fase_2.md).

Las instrucciones de acceso y sus pruebas están en [Pruebas de la fase 3](docs/pruebas_fase_3.md).

El recorrido de empresas, reservas y visitas está en [Pruebas de la fase 4](docs/pruebas_fase_4.md). Para usar el puerto de la revisión actual: `mvn -Daplomo.puerto=8081 compile exec:java` y abrir `http://localhost:8081`.

El informe del Paso 7, las comprobaciones de permisos y las capturas por tamaño están en [Informe del Paso 7](docs/informe_paso7.md). Las pruebas y la revisión visual utilizan únicamente `rula_ergonomia_pruebas`.

El [Informe del Paso 8](docs/informe_paso8.md) contiene la tabla de requisitos, los diez casos en pantalla, pruebas de permisos y capturas a cinco tamaños. El puntaje guardado lo calcula el servidor con las tablas de MySQL y NEUTRO = 5°.

El [Informe del Paso 9](docs/informe_paso9.md) contiene requisitos, permisos, decisión de máximo por evaluación, pruebas y capturas de lista, detalle y edición en cinco tamaños. Los pendientes anteriores y su estado Arreglado se conservan.

El [Informe del Paso 10](docs/informe_paso10.md) documenta las correcciones visuales previas, las decisiones de Andy del 11 nov 2026 y las pruebas de Sugerencias. Se accede desde **Pendientes → Sugerencias** o desde una evaluación con análisis guardado. El Usuario solo consulta su empresa y visitas finalizadas.

El [Informe del Paso 11](docs/informe_paso11.md) incluye requisitos, decisión de Andy del 11 nov 2026, pruebas de una visita En curso con Rojo y capturas a cinco tamaños. Entra desde **Inicio → Evaluaciones**, las secciones habituales o **Ver otras secciones**. El Usuario consulta todas sus reservas y los motivos de cancelación, pero solo resultados de visitas finalizadas, sin fotos ni controles de edición.

Para una instalación existente nueva, cargar **solo** `docs/base_de_datos/datos_sugerencias.sql` en la base elegida: añade las 11 reglas iniciales sin modificar ni duplicar las existentes, sin borrar y sin cambiar tablas. En el entorno local de Andy ya se cargó ese catálogo en `rula_ergonomia`. Las pruebas lo cargan en su propia transacción en `rula_ergonomia_pruebas` y lo revierten. Nunca ejecutar de nuevo el script SQL inicial sobre una base existente.

## Prototipo (referencia visual)

El prototipo funcional que se presento antes de la implementacion esta en linea:
https://andybena2025.github.io/Aplomo-Proyecto-Nuclear-/

Su codigo fuente esta en `docs/referencia/mock_app/` como referencia de estilo y flujo.

## Equipo

- Laura Gabriella Romero Gomez
- Johan Andres Zapata Quintero
- Andres Eduardo Benavides

## Licencia

[MIT](LICENSE)
