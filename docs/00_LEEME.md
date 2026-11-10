# Documentación para programar APLOMO.

Esta carpeta es la **foto congelada del diseño aprobado** (Proyecto Nuclear 6.0, 10 de noviembre de 2026), para que quien programe, una persona o Claude en IntelliJ, no tenga que buscar en Notion ni en chats.

| Archivo | Para qué sirve |
|---|---|
| `../CLAUDE.md` | Reglas del proyecto. Claude Code lo lee solo al empezar. |
| `01_vision_y_alcance.md` | Problema, roles, términos, las 4 fases, pendientes con la clienta. |
| `02_requisitos_v6.md` | Los 44 requisitos completos (33 RF + 11 RNF) con su regla de negocio. |
| `03_metodo_rula.md` | Ángulos, puntos por segmento, tablas, carga, semáforo y pruebas. |
| `04_flujo_y_pantallas.md` | Qué ve cada rol y los 6 pasos de la evaluación. |
| `05_arquitectura.md` | Java sin frameworks: capas, paquetes, rutas y seguridad. |
| `06_plan_de_implementacion.md` | Los pasos en orden, cada uno con cómo probarlo. |
| `07_estilo_visual.md` | Colores, tipografías y componentes del mock. |
| `diagramas/` | Casos de uso (aprobados), clases A y B, objetos, dominio, actividades, estados, secuencia y E-R, más la especificación de los 29 casos de uso (PDF y texto). |
| `base_de_datos/` | `Script_MySQL_RULA.sql` (21 tablas, tablas RULA cargadas, admin inicial) y el diccionario de datos (PDF y texto). |
| `referencia/mock_app/` | Código del prototipo (`app.src.html`, sin fotos) y capturas. Referencia visual y de flujo. En línea: https://andybena2025.github.io/Aplomo-Proyecto-Nuclear-/ |

Si algo cambia en Notion, se actualiza aquí con un commit, para que el código y el diseño no se separen.
