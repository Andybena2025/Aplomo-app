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

1. **Crear la base de datos** en MySQL:
   ```bash
   mysql -u root -p < docs/base_de_datos/Script_MySQL_RULA.sql
   ```

2. **Configurar la conexion:**
   ```bash
   cp config/db.properties.ejemplo config/db.properties
   ```
   Editar `config/db.properties` con tu usuario y clave de MySQL.

3. **Compilar y arrancar:**
   ```bash
   mvn compile
   mvn exec:java
   ```

4. **Abrir** http://localhost:8080 en Chrome, Edge o Firefox.

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
