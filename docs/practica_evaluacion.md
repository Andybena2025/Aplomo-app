# Práctica permanente de evaluación

Solicitada por Andy el 14 nov 2026. Inicio → Probar evaluación → Abrir evaluación de prueba → Planta u Oficina. El mismo recorrido y cálculo del servidor; incluye las fotos de ejemplo ya existentes.

| Requisito | Implementación | Comprobación |
|---|---|---|
| Acceso siempre visible al Administrador | InicioEvaluacion.practica y Router | HTTP: Inicio vacío y después de evaluar |
| Visita En curso reutilizable | PracticaEvaluacion.abrir | HTTP/servicio: repetición, nueva visita tras finalizar |
| Conservación de intentos y empresas ajenas | Empresa dedicada DEMO-APLOMO-PRACTICA, sin DELETE | Intento conservado, empresa ajena y desactivación respetadas |
| RF-002 / RNF-004: permisos y CSRF | Router y AccesoDatos/Permisos en servicio | Usuario por URL manual y POST; token inválido rechazado |
| RNF-005: quién/cuándo | BitacoraDao en la misma transacción | Auditoría del alta; reloj compartido de Visitas |

Los resultados de práctica **sí se guardan en la base configurada**, bajo la empresa ficticia; no es una simulación sin persistencia. No se carga el SQL demo completo, no se borran intentos ni se reabren visitas finalizadas. El primer clic prepara solo los registros que faltan; una consulta GET no crea nada. Si desactivan manualmente sus entidades, se pide reactivarlas desde Empresas en lugar de hacerlo sin permiso. Desactivable con la misma bandera aplomo.modoDemo de las fotos de ejemplo.

Validación: 8 pruebas nuevas en HttpPracticaEvaluacionTest; suite completa de 900 Maven y 41 JS. QA automatizado solo en rula_ergonomia_pruebas, con reversión. No se tomaron capturas ni se generaron artefactos de evidencia. Sin cambios de esquema, motor RULA o POST /evaluaciones/calcular. No se certifica una nueva prueba visual o táctil en dispositivo físico.

Para probar: entrar como Administrador, abrir el acceso, elegir Planta, usar una foto de ejemplo y guardar; regresar al mismo acceso y repetir. También elegir Oficina. Finalizar voluntariamente la visita y volver al acceso debe crear otra, conservando la anterior. No es necesario reiniciar datos ni abrir una terminal.

Por petición expresa de abrir una visita para Andy, tras las pruebas se preparó en la base local de la app (`rula_ergonomia`) la visita **61**, En curso, con puestos **89** (Oficina) y **90** (Planta). Es el alta voluntaria de la demostración, distinta del QA revertido. No se cargaron otras empresas demo, no se borró nada y no se cambió config/db.properties.
