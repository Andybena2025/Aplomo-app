# Paso 26 · funciones y demo

Login compacto, títulos champán, perfil propio, Archivo con permisos renovados y calendario con colores personales.
Se rechazan citas cruzadas del mismo Evaluador; los avisos privados no prolongan la sesión.
La demo agrega seis cuentas, siete empresas y citas alrededor del 27/11/2026, sin cambiar claves existentes.
Migración aditiva: color_agenda; sin tablas nuevas, eliminaciones ni cambios de motor, cálculo, IA o galería.

## Comprobaciones

- Desborde horizontal: 0 px a 360, 390, 820, 1024 y 1366 en /cuenta, /archivo, /archivo?empresa,
  /agenda (mes y semana) y /ayuda: 30 medidas DOM, sin capturas.
- /login: 0 px en esas anchuras y en 430/1440; Entrar a 581/615/643 px en celulares de
  360×740/390×844/430×932. En 1366×768 y 1440×900, altura de documento igual a pantalla.
- Pruebas específicas de permisos, datos propios, notas, bloqueo, colores, cruces y demo idempotente.
- Pendiente de entorno: iPad físico, Windows y tres computadores en la misma red; sin descargas.
- Contacto empresarial real pendiente de configuración local. No se inventaron teléfono ni correo.
- Fuera de alcance: hosting, Spring Boot, logo nuevo, Notion y diagramas.

Los resultados finales de suites, migración y demo se agregan al cerrar la verificación.

## Cierre de pruebas y migración

Maven: 1.035 casos únicos. Corrida completa única: 1.028 aprobados, 7 fallidos, 0 errores/omitidos;
corregidos y verificados mediante ejecución dirigida de los métodos fallidos (incluye sus parámetros).
JavaScript: 122 ejecutadas y aprobadas, 0 fallos. No se repitió ninguna suite completa.
Contraste del título: 11,73:1 sobre #151517; 10,93:1 sobre #1C1C1F; 9,94:1 sobre #242429.
Migración real repetida dos veces: 22 tablas; 449 filas antes y 449 después, conteo exacto por tabla sin cambios.
Respaldo completo privado fuera de Git; el administrador de MySQL ejecutó la migración.

Archivos por parte: login/Html/CSS; CuentaPropia/CuentaWeb; Archivo/ArchivoWeb/PaginasArchivo;
Agenda/Calendario/PaginasAgenda/CrucesAgendaDao; NotasWeb/notas.js; Usuarios/ColoresAgenda/Cuenta/DAO;
DemoRoles/DemoPresentacion/Demo y scripts de carga/migración; ayuda/contacto; pruebas y documentos del paso.

La carga real se repitió sin duplicar cuentas ni citas. Los horarios de demos anteriores se conservan,
reubicados antes del período de presentación; así el nuevo período mantiene dos citas semanales
por Evaluador (Andrés tiene además la cita en vivo). Se conciliaron los cruces anteriores, sin borrar
registros ni modificar empresas reales. Las seis cuentas nuevas exigen cambio de clave.
Escenario 26: Andrés 17 citas (10 anteriores, 7 desde el 27/11); Johan 19 (13 anteriores,
5 futuras activas y 1 cancelada; incluye 3 históricas de 2025); Gabriella 16 (10 anteriores, 6 futuras).

Cierre posterior: Paso 26B aprobado por 1036 Maven y 122 JS; ver informe_paso26b.md.
