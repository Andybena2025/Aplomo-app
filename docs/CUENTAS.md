# Crear cuentas del equipo

La herramienta crea cuentas por el mismo servicio que la pantalla **Usuarios**. No borra, no reactiva ni modifica cuentas existentes. Usa la base configurada para esa instalación. No necesitas el administrador de MySQL: basta la cuenta de conexión habitual de APLOMO y un Administrador vigente de la app.

## 1. Cambia primero la contraseña del Administrador inicial

1. Abre la app e ingresa con **admin@rula.local** y la contraseña actual de esa instalación.
2. Si es el primer ingreso, la app abre obligatoriamente **Cambiar contraseña**. Escribe la actual, una nueva y su confirmación.
3. La nueva debe tener al menos ocho caracteres, una letra y un número; como todas las claves de APLOMO, admite hasta 72 bytes para bcrypt.
4. Si ya cambiaste la contraseña, usa la nueva. También puedes abrir **Cambiar contraseña** desde el avatar de la barra superior. Esta herramienta no restablece ni revela la clave del Administrador inicial.

## 2. Prepara la lista privada

Desde la raíz del proyecto, copia la plantilla:

Mac:

```sh
cp config/cuentas.csv.ejemplo config/cuentas.csv
```

Windows, en Símbolo del sistema:

```bat
copy config\cuentas.csv.ejemplo config\cuentas.csv
```

Edita **config/cuentas.csv**, en formato **UTF-8**, separado por comas. Tiene exactamente estas columnas:

| Columna | Qué poner |
|---|---|
| nombre | Nombre de la persona, hasta 100 caracteres. |
| correo | Correo válido y único; se normaliza a minúsculas. |
| rol | ADMINISTRADOR o USUARIO, escrito así. |
| nit_empresa | NIT exacto de una empresa activa existente para USUARIO; vacío para ADMINISTRADOR. |

La plantilla contiene nombres y correos ficticios: reemplázalos antes de usarla. **No agregues una columna de contraseña.** Si un nombre contiene comas, enciérralo entre comillas dobles; para una comilla dentro del nombre, escríbela dos veces. Una cuenta por línea, sin saltos dentro de un campo. Máximo 1000 cuentas; no repitas correos dentro del CSV.

**config/cuentas.csv está ignorado por Git**; solo se versiona la plantilla sin contraseñas. No envíes tu lista privada al repositorio. Los teléfonos y cargos no forman parte del CSV: la herramienta los pide por consola para cada cuenta nueva porque son obligatorios en Usuarios. No inventa esos datos ni guarda valores de relleno.

## 3. Ejecuta la herramienta

Necesitas Java 17 o superior, Maven Wrapper del proyecto y MySQL accesible con la configuración habitual. Desde una Terminal real, sin redirigir la entrada o salida:

Mac:

```sh
bash scripts/crear_cuentas.sh
```

Windows, en Símbolo del sistema:

```bat
scripts\crear_cuentas.bat
```

1. Escribe el correo de un **Administrador activo** de APLOMO y su contraseña actual. La contraseña de ingreso no se ve al escribirla y no se pasa como argumento del comando.
2. Completa teléfono y cargo de cada cuenta nueva. Las empresas deben existir y estar activas. La herramienta mantiene las mismas validaciones de Usuarios y vuelve a comprobar el permiso al crear.
3. Al terminar aparece **una sola tabla**. Cada cuenta creada muestra su correo y una contraseña temporal aleatoria de **16 caracteres**, con letras y números, sin 0/O/o ni l/1/I. Cada contraseña se imprime una sola vez, inmediatamente después de crear la cuenta.
4. Copia las claves desde esa tabla y entrégalas a sus titulares por un medio privado. No uses `>`, `tee`, grabación de sesión o registros de terminal para guardar la salida. La herramienta exige una consola y no escribe contraseñas en archivos ni en registros de la aplicación.
5. Si hay un error en una fila, se muestra «no creada». Las cuentas creadas en otras filas se conservan; el comando termina con error para que puedas revisarlo. Corrige esa fila y ejecuta de nuevo.

Los scripts compilan y preparan una lista de dependencias en `target/cuentas-classpath.txt`. Ese archivo contiene solo rutas de librerías; no contiene cuentas ni claves. La importación se ejecuta con Java directamente para disponer de una consola con contraseña oculta. No abre otra copia del servidor web.

## 4. Repetir y entrar

- Si el correo **ya existe**, la tabla dice **«ya existe»** y no pide sus datos ni cambia nombre, rol, empresa, estado o contraseña. Tampoco reactiva cuentas inactivas. Repetir el archivo válido no duplica cuentas.
- Cada cuenta nueva queda activa y con contraseña **temporal**, guardada únicamente como hash bcrypt de costo 10. El primer ingreso obliga a cambiarla antes de usar pantallas internas.
- Si perdiste una contraseña temporal, repetir la herramienta no la vuelve a mostrar. Un Administrador debe abrir **Usuarios → Editar** y asignar una nueva contraseña temporal mediante el formulario existente.
- **Administrador:** gestiona la aplicación; no se asigna a una empresa.
- **Usuario:** solo consulta los datos permitidos de su empresa. No crea ni edita y no ve fotos. Los resultados de visitas En curso permanecen ocultos hasta Finalizadas; las reservas de su empresa siguen siendo consultables según las reglas existentes.

## Comprobaciones

Las pruebas automatizadas importan dos cuentas ficticias en **rula_ergonomia_pruebas**, repiten el archivo, comprueban bcrypt/longitud, conservan una cuenta inactiva y verifican la redirección al cambio obligatorio de contraseña. Los datos se revierten al terminar. No se crean cuentas del equipo real durante esas pruebas.

La versión Windows se entrega con los mismos pasos; no se ejecutó en Windows en este Mac. Si quieres usarla contra otra instalación, revisa primero la configuración de conexión: no cambies la base accidentalmente.

## Tres roles · cambio Paso 24 (propuesta, Andy, 20 nov 2026)

- Administrador maestro: todas las empresas; único que crea empresas, administra cuentas, asigna empresas y activa/desactiva catálogos.
- Evaluador: trabajo completo en sus empresas asignadas (también historial inactivo), sin crear empresas ni activar/desactivar registros. Mi agenda y Mis evaluaciones; práctica privada.
- Usuario: consulta una o varias empresas asignadas; resultados solo de visitas Finalizadas y nunca fotos. Reservas en todos sus estados, sin editar.
- `cuenta_empresa` es la fuente de acceso. Quitar una asignación revoca el acceso en la siguiente petición. Una empresa admite un solo Evaluador activo. El historial de evaluaciones creadas por el Evaluador sigue disponible en solo lectura.
- RF-029 sigue fuera de alcance: no hay registro libre.

CSV: `rol` también admite EVALUADOR; `nit_empresa` admite varios NIT separados por `;`. Para Administrador queda vacío. La herramienta usa el mismo servicio y conserva la regla de un solo Evaluador activo.

| Correo demo | Rol | Qué ve |
|---|---|---|
| evaluador1@rula.local | Evaluador | Empresas E1/E2, su agenda y sus evaluaciones |
| evaluador2@rula.local | Evaluador | Empresa E3, su agenda y sus evaluaciones |
| usuario1@rula.local | Usuario | E1/E2, reservas y resultados Finalizados, sin fotos |
| usuario2@rula.local | Usuario | E3, reservas y resultados Finalizados, sin fotos |

La carga demo es explícita, con contraseña inicial de demostración temporal igual al patrón de instalación. No altera las cuentas del equipo. Cambiarla en el primer ingreso.
