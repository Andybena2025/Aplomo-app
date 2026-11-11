# Fase 3: acceso y cuentas (pasos 2 y 3)

## Paso 2: acceso

Implementa RF-001a, RF-001b, RF-001c, RF-002 y RNF-004 con Java/JDBC, bcrypt de costo 10, sesiones en memoria y páginas con el estilo del mock. Las 389 pruebas de la fase 2 se conservan sin cambios.

- `Autenticacion` bloquea la fila de la cuenta durante el ingreso: cinco errores consecutivos bloquean 15 minutos, sin ampliar el plazo por intentar entrar durante el bloqueo. Al vencer, se puede intentar otra vez; un ingreso correcto reinicia el contador. Inactiva nunca se reactiva automáticamente.
- `Contrasenas` admite los hashes `$2b$` existentes y genera `$2a$` compatibles con jBCrypt. Exige ocho caracteres, letra y número. Rechaza entradas de más de 72 bytes UTF-8 para evitar que bcrypt ignore el final de una clave.
- `Sesiones` usa IDs y tokens CSRF aleatorios de 32 bytes. La cookie es HttpOnly y SameSite=Strict; también Secure al servir con HTTPS. Exactamente a los 30 minutos sin peticiones autenticadas deja de permitir acceso. El próximo acceso lleva al ingreso con aviso. Cerrar sesión elimina el ID del servidor.
- La contraseña temporal solo permite cambiar contraseña o salir. El cambio comprueba actual, nueva y confirmación; no permite reutilizar la actual, quita la marca temporal, invalida las otras sesiones y rota la actual.
- Cada petición interna relee estado, hash, rol y empresa; los permisos no dependen de botones. No se muestran hashes ni se guardan contraseñas en el navegador. Todos los POST exigen CSRF, incluido el ingreso. Los errores de ingreso son genéricos.

### Comprobación del paso 2

`mvn compile` y `mvn test`: **415 ejecutadas, 415 aprobadas, 0 fallos, 0 errores y 0 omitidas**. Incluye HTTP real (cookies, CSRF, rutas, cambio obligatorio, roles, expiración), SQL real con transacciones revertidas y pruebas de tiempos con reloj controlado. La página de ingreso se abrió y verificó visualmente en Safari; la portada y su animación se conservaron.

Para probar manualmente:

1. Conservar la base y `config/db.properties`. No volver a ejecutar el script SQL completo ni sobrescribir la configuración.
2. Reiniciar el servidor de desarrollo y ejecutar `mvn compile exec:java`. Abrir `http://localhost:8080`. Si otro proceso ya ocupa ese puerto, se puede usar `mvn -Daplomo.puerto=8081 compile exec:java` y abrir `http://localhost:8081`.
3. Entrar con una cuenta existente. El administrador inicial es el indicado en `05_arquitectura.md`, siempre que aún no se haya cambiado. No se restableció ninguna clave existente.
4. Si la contraseña es temporal, se abre Cambiar contraseña. Intentar abrir `/` o `/usuarios`: vuelve al cambio obligatorio. Probar una actual errónea, una confirmación diferente y una nueva sin números: avisa sin guardar. Cambiarla correctamente: abre Inicio.
5. Usar Cambiar contraseña desde el menú para el cambio voluntario. Cerrar sesión y volver a una dirección interna: exige ingresar otra vez.
6. Con una cuenta de prueba, fallar cinco veces al ingresar. La correcta tampoco entra antes de 15 minutos. Al vencer el bloqueo, la correcta permite entrar. El mensaje no revela si falló correo, clave o estado.
7. Dejar una sesión 30 minutos sin hacer peticiones y volver a Inicio: exige ingresar y muestra un aviso. Las pruebas automatizadas verifican los límites sin esperar ese tiempo real.
8. Ingresar como Usuario con contraseña definitiva: no hay enlace Usuarios. Escribir `/usuarios`, enviar un POST a esa ruta o abrir `/fotos/1`: acceso denegado.

Las pruebas crean cuentas y una empresa exclusivamente dentro de transacciones no confirmadas y al terminar ejecutan ROLLBACK. No usan DELETE ni DDL ni modifican credenciales reales. Los contadores AUTO_INCREMENT pueden avanzar aunque las filas se reviertan, comportamiento normal de MySQL.

## Paso 3: administración de usuarios

Implementa RF-003 y RF-010, con permisos de RF-002 y validación de RNF-004. Solo el Administrador con contraseña definitiva puede listar, crear, editar, activar o desactivar cuentas. Las cuentas se conservan siempre; no hay ruta de borrado.

- Nuevo usuario pide nombre, correo, teléfono, cargo, rol y contraseña temporal. Para el rol Usuario exige una empresa existente; el Administrador es independiente y no tiene empresa. El selector se lee de la base, sin crear empresas ni datos de demostración permanentes.
- Editar conserva ID, estado y contraseña. Permite cambiar rol y empresa de forma coherente. El campo de contraseña temporal es opcional: vacío conserva la actual; con valor asigna una temporal y obliga a cambiarla al ingresar. Las claves nunca se muestran otra vez en listas ni formularios.
- Los correos se normalizan y se comprueban con la comparación de MySQL. La restricción única del esquema se respeta también para cuentas inactivas: se ofrece editar o reactivar la existente, nunca reutilizar su correo en una fila nueva.
- Activar y desactivar son POST con CSRF. Desactivar impide el ingreso y revoca sesiones; activar reinicia intentos y bloqueo. Editar una cuenta invalida sus sesiones; si se edita la propia, hay que ingresar otra vez con los permisos actuales.
- La protección del último Administrador activo también cubre su cambio a Usuario. El servicio bloquea las filas de Administradores dentro de la transacción antes de contar y guardar, para que dos peticiones no puedan dejar la instalación sin Administrador por cambios concurrentes. El formulario no puede enviar un estado o ID alternativos para evitar esta regla.
- Si falla la validación o guardar una cuenta, los datos no secretos permanecen en el formulario. Las contraseñas se vuelven a escribir. Los avisos no exponen SQL ni trazas.

### Comprobación final del paso 3

`mvn compile` y `mvn test`: **437 ejecutadas, 437 aprobadas, 0 fallos, 0 errores y 0 omitidas**. Son las 389 anteriores de RULA/JDBC y 48 de acceso, cuentas y HTTP. La prueba de último Administrador usa únicamente las cuentas de prueba al simular el conjunto de Administradores, para no modificar ninguna cuenta previa. Las escrituras y la comprobación del motor usan MySQL real; los datos nuevos se revierten con ROLLBACK. Un fallo SQL al crear se simula para verificar que no se pierdan campos ni se revele información privada.

Se revisaron visualmente en Safari la lista y el formulario usando las mismas clases HTML/CSS y datos ficticios, en una vista local de solo lectura. El funcionamiento completo de las rutas se verificó con las pruebas HTTP. No se ejecutó todavía el recorrido en Chrome, Edge y Firefox; esa comprobación de compatibilidad sigue en el paso 13 del plan.

Para probar las funciones del paso 3:

1. Reiniciar el servidor con `mvn compile exec:java` y entrar como Administrador con contraseña definitiva. Si se usa el puerto alterno, añadir `-Daplomo.puerto=8081` y abrir ese puerto. El puerto 8080 puede seguir ocupado por un proceso previo: hay que detenerlo desde su terminal antes de arrancar allí la nueva versión.
2. Abrir **Usuarios → Nuevo usuario**. Crear un Administrador sin empresa con una contraseña temporal válida. Salir y entrar con esa cuenta: obliga a cambiar la contraseña.
3. Volver con un Administrador, crear una cuenta de rol **Usuario** y elegir una empresa existente. Si no hay empresas, el formulario lo avisa: su creación pertenece al paso 4 y no se adelantó en esta fase. El conjunto automático de pruebas sí utiliza una empresa temporal revertida para comprobar este recorrido.
4. En **Editar**, cambiar nombre, teléfono o cargo y guardar con contraseña temporal vacía. El ID y la clave se conservan. Probar correo repetido, correo inválido o rol Usuario sin empresa: no guarda y mantiene los campos no secretos.
5. Para restablecer una contraseña olvidada, abrir **Editar**, escribir una **Nueva contraseña temporal** y guardar. Comunicarla por un canal privado; la clave vieja ya no sirve y la nueva exige cambiarla al ingresar.
6. Pulsar **Desactivar** en una cuenta de prueba. Intentar entrar con ella: rechazo genérico. Si tenía una sesión abierta, su próxima petición también exige ingresar. Pulsar **Activar**: vuelve a permitir el ingreso.
7. Si solo existe un Administrador activo, intentar desactivarlo o cambiar su rol: aparece la explicación y no se guarda. Esta condición también se verifica automáticamente con `mvn -Dtest=UsuariosTest#ultimoAdministradorNoSeDesactivaNiSeDemueveEnTransaccion test`, sin tocar cuentas reales.
8. Entrar como Usuario con clave definitiva: no hay opción Usuarios. Abrir directamente `/usuarios`, `/usuarios/nuevo` o `/usuarios/1/editar`: acceso denegado. Cambiar contraseña y cerrar sesión siguen permitidos.
9. Ejecutar `mvn test` para repetir todo; los informes están en `target/surefire-reports/`. Solo las pruebas de acceso HTTP y cuentas se pueden ejecutar con `mvn -Dtest=HttpSeguridadTest,HttpUsuariosTest,HttpErrorGuardadoTest,UsuariosTest,AutenticacionTest,SesionesTest,ContrasenasTest test`.

## Punto de parada

Fase 3 completa (pasos 2 y 3). Quedan para la fase 4 las empresas, sedes, salones, áreas, cargos y puestos (paso 4), sin adelantarlos. La consulta de resultados y fotografías, evaluaciones y demás módulos siguen sus pasos originales. Se conserva la base, el esquema y las credenciales existentes. Se espera el **OK** de Andy antes de comenzar la fase 4.
