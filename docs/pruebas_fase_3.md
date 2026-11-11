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
