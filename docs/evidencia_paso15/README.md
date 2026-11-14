# Evidencia · Paso 15

Datos ficticios en rula_ergonomia_pruebas. Los servidores de ensayo se cerraron con ROLLBACK. Ninguna captura corresponde a una valoración clínica real.

## Vistas del tamaño solicitado

- [iPad vertical · 834×1194](vista-834x1194.jpg)
- [Laptop · 1366×768](vista-1366x768.jpg)
- [Celular · 390×844](vista-390x844.jpg)

El resto de archivos `*-834.jpg`, `*-1366.jpg` y `*-390.jpg` son capturas **de página completa**, tomadas con esas ventanas CSS; su altura de archivo puede ser mayor porque muestran toda la conversación. Hay 18 estados por tamaño: permiso; inicial abierta/respondida; condiciones; lado; perfil sin foto/medido; brazo abierto/respondido; detalle; cuello abierto/respondido; carga abierta/respondida; resultado; dos lados; carga recordada; resultado Oficina.

`medidas.json`: 54 registros. Cero desbordamiento horizontal y cero diálogos; como máximo una lista abierta. Campos visibles de al menos 52 px. `minCampo=null` significa que esa pantalla no contiene un campo del componente (por ejemplo Resultado), no que mida cero. `qa-resumen.json` calcula el mínimo excluyendo esos casos.

## Safari nativo del Mac

- `safari-deteccion.png`: modelo ligero sobre la muestra, detectó persona en 2,73 s; diagnóstico capturó cero errores JavaScript.
- `safari-app-perfil-antes-del-ajuste-de-barra.png`: puntos y ángulos en la app real de QA, antes de corregir el azul nativo de progress.
- `safari-progreso-marfil.png`: comprobación posterior del avance marfil en Safari.

Safari 26.6.2 de escritorio. No es un iPad físico ni emulación de toque. No se descargaron navegadores.

`muneca-tres-puntos.jpg`: elección explícita de la medición actual de muñeca en el perfil (2,35°); no corresponde a una foto de detalle.
