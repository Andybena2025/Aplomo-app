# Paso 24 · Tres roles, agenda, historial propio y notas

Se agregan Evaluador, asignaciones múltiples, agenda de semana/mes, notas privadas con acuse y Mis evaluaciones. Administrador conserva exclusividad de cuentas, empresas nuevas y activación/desactivación. Usuario consulta sus empresas y solo resultados Finalizados, sin fotos. El alcance se renueva desde cuenta_empresa en cada petición; id_empresa legado se conserva y no autoriza.

| Requisito | Archivo / método | Comprobación |
|---|---|---|
| RF-002/003 | AccesoDatos.Alcance, CuentaEmpresaDao.validar/sincronizar, Usuarios | HttpTresRolesTest: alcance, revocación, Evaluador único, roles y bitácora; UsuariosTest |
| RF-014/016/024, RNF-006/009 | Empresas, Reservas, Visitas, Evaluaciones, Analisis, FotosEvaluacion y DAO con Alcance | Empresas ajenas bloqueadas por ID, foto y POST; evaluación completa del Evaluador |
| RF-033 (propuesta) | Agenda.consultar, PaginasAgenda.pagina, Router | Bordes de año/mes, 42 días, canceladas, cruces, filtro inactivo; agenda A sin altas |
| RNF-005 | Agenda.guardarNota/entender, ConsultaDao.propias | CSRF, texto escapado, acuse reiniciado, Usuario excluido, historial propio sin editar tras retiro |
| RNF-001/009 | migracion_paso24.sql, Esquema.verificar, scripts/migrar_paso24 | Dos migraciones aditivas, 21→22 tablas, todas las filas anteriores conservadas |
| RNF-002/007/011 | PaginasAgenda, PaginasUsuarios, aplomo.css; CrearCuentas | CSV E con varios NIT, componente actual y medidas DOM |

Migración ensayada en copia `rula_ergonomia_pruebas_historia24`, con empresa/puesto inactivos, Usuario anterior, visita Finalizada y evaluación. Dos ejecuciones, misma relación Usuario–empresa y una sola asignación. Filas antes/después: cuenta 3/3, empresa 1/1, sede 1/1, salón 1/1, puesto 1/1, reserva 1/1, visita 1/1, evaluación 1/1, condiciones 1/1, bitácora 1/1, nivel_riesgo 4/4, valor_tabla_rula 272/272; las otras nueve tablas existentes 0/0. cuenta_empresa nueva: 1 fila. También se migró la base de pruebas habitual con respaldo previo. No se ejecutó el script destructivo de instalación sobre ninguna base existente.

Pruebas dirigidas: 265 casos distintos de servicios, HTTP, cuentas, demo y sesiones comprobados; fallos iniciales corregidos y repetidos solo los casos afectados. La suite conjunta Maven/JS se reserva una sola vez para el final del Paso 25. Nuevo código no cambia co.aplomo.rula, el bloque POST /evaluaciones/calcular, deteccion.js ni galería. La autorización exterior de EvaluacionesWeb admite E, según este encargo.

Medidas DOM (navegador integrado, sin capturas): agenda semana, mes, Mis evaluaciones, reserva con nota e historial del puesto a 360/390/820/1024/1366 px reales: 25 combinaciones con 0 px de desborde. Botones y pestañas medidos: mínimo 44 px. La matriz de Mis evaluaciones utilizó su estado vacío; el contenido con filas y retiro se comprobó por HTTP. No se afirma prueba táctil física.

Cuentas demo, solo al cargar explícitamente la demostración: evaluador1@rula.local (E1/E2), evaluador2@rula.local (E3), usuario1@rula.local (E1/E2), usuario2@rula.local (E3). Cuatro cuentas temporales; repetición sin duplicados; las cuentas existentes del equipo no se modifican. Agenda de esta semana/próxima, cruce, nota y tres evaluaciones atribuidas a los Evaluadores. Las nuevas visitas con resultado se finalizan mediante los servicios, con reloj estable a segundos.

Pruebas antiguas ajustadas: BasePrueba inserta la nueva asignación del fixture; UsuariosTest conserva id_empresa al cambiar rol; VisitasTest retira/asigna mediante cuenta_empresa; SesionesTest ya no usa id_empresa para autorizar; DemoTest espera las cuatro cuentas demo nuevas. Ninguna expectativa del motor cambió.

Rutas de A a A/E: crear/editar sedes, salones, áreas, cargos y puestos; reservas y sus transiciones; visitas; evaluaciones/fotos/análisis; pendientes/sugerencias; informe y práctica. /usuarios, crear empresa, activar/desactivar y escribir nota siguen A. Agenda A/E; Entendido solo E asignado; Mis evaluaciones solo E. U conserva reservas de todos sus estados y solo resultados Finalizados.

Diagramas pendientes de actualizar (no editados): roles/casos de uso, clases/dominio, E-R, secuencias y actividades de cuentas/evaluación. Windows, iPad físico, otros navegadores y concurrencia de carga real no comprobados. No se descargaron navegadores, tomaron capturas, borraron datos ni generaron carpetas de evidencia. Suite final, cifras finales y estado de publicación se registrarán en el informe del Paso 25.

Base local rula_ergonomia migrada con el script Mac después de crear respaldo privado en config/respaldos. El respaldo queda ignorado por Git. No se cargó la demo ni se crearon cuentas en la base real.
