-- APLOMO: datos de demostración opcionales. No se ejecutan desde la app ni los instaladores.

-- Elegir la base al invocar mysql. Requiere un Administrador activo y las fotos indicadas en INSTALAR.md.

-- Nombres y sedes del mock. Puestos, contactos, fechas y resultados son ficticios.

-- Solo inserta lo que falta. No modifica ni borra cuentas o registros existentes.

-- Resultados 1, 3 y 7 comprobados con MotorRula y tablas MySQL: casos 4, 2 y 8 de docs/03.

START TRANSACTION;

SET @demo_admin = (SELECT id_cuenta FROM cuenta WHERE rol='ADMINISTRADOR' AND estado='ACTIVA' ORDER BY id_cuenta LIMIT 1);

INSERT INTO empresa (id_administrador,nombre,nit,descripcion,telefono,correo,persona_contacto)
SELECT @demo_admin,'Ensambles del Quindío S.A.S.','DEMO-APLOMO-E1','Demostración APLOMO. Manufactura de componentes. Información ficticia para presentar el producto.','0000000000','e1@ejemplo.invalid','Contacto de demostración'
WHERE NOT EXISTS (SELECT 1 FROM empresa WHERE nit='DEMO-APLOMO-E1');

SET @e1 = (SELECT id_empresa FROM empresa WHERE nit='DEMO-APLOMO-E1' LIMIT 1);

INSERT INTO empresa (id_administrador,nombre,nit,descripcion,telefono,correo,persona_contacto)
SELECT @demo_admin,'Muebles Cafeteros S.A.S.','DEMO-APLOMO-E2','Demostración APLOMO. Fabricación de muebles. Información ficticia para presentar el producto.','0000000000','e2@ejemplo.invalid','Contacto de demostración'
WHERE NOT EXISTS (SELECT 1 FROM empresa WHERE nit='DEMO-APLOMO-E2');

SET @e2 = (SELECT id_empresa FROM empresa WHERE nit='DEMO-APLOMO-E2' LIMIT 1);

INSERT INTO empresa (id_administrador,nombre,nit,descripcion,telefono,correo,persona_contacto)
SELECT @demo_admin,'Corporación Universitaria Empresarial Alexander von Humboldt','DEMO-APLOMO-E3','Demostración APLOMO. Educación superior. Información ficticia para presentar el producto.','0000000000','e3@ejemplo.invalid','Contacto de demostración'
WHERE NOT EXISTS (SELECT 1 FROM empresa WHERE nit='DEMO-APLOMO-E3');

SET @e3 = (SELECT id_empresa FROM empresa WHERE nit='DEMO-APLOMO-E3' LIMIT 1);

INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion)
SELECT @e1,'Planta Armenia','Km 3 vía al Edén','Armenia','0000000000','Lunes a viernes, 08:00 a 17:00','Sede de demostración. Dirección tomada del mock, sin verificación de vigencia.'
WHERE NOT EXISTS (SELECT 1 FROM sede WHERE id_empresa=@e1 AND nombre='Planta Armenia');

SET @s1 = (SELECT id_sede FROM sede WHERE id_empresa=@e1 AND nombre='Planta Armenia' LIMIT 1);

INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion)
SELECT @e2,'Planta Dosquebradas','Zona industrial La Badea','Dosquebradas','0000000000','Lunes a viernes, 08:00 a 17:00','Sede de demostración. Dirección tomada del mock, sin verificación de vigencia.'
WHERE NOT EXISTS (SELECT 1 FROM sede WHERE id_empresa=@e2 AND nombre='Planta Dosquebradas');

SET @s2 = (SELECT id_sede FROM sede WHERE id_empresa=@e2 AND nombre='Planta Dosquebradas' LIMIT 1);

INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion)
SELECT @e3,'Sede principal','Avenida Bolívar N° 1-189, Armenia','Armenia','0000000000','Lunes a viernes, 08:00 a 17:00','Sede de demostración. Dirección tomada del mock, sin verificación de vigencia.'
WHERE NOT EXISTS (SELECT 1 FROM sede WHERE id_empresa=@e3 AND nombre='Sede principal');

SET @s3 = (SELECT id_sede FROM sede WHERE id_empresa=@e3 AND nombre='Sede principal' LIMIT 1);

INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion)
SELECT @e3,'Sede Alcázar','Calle 4N # 13-05, Armenia','Armenia','0000000000','Lunes a viernes, 08:00 a 17:00','Sede de demostración. Dirección tomada del mock, sin verificación de vigencia.'
WHERE NOT EXISTS (SELECT 1 FROM sede WHERE id_empresa=@e3 AND nombre='Sede Alcázar');

SET @s4 = (SELECT id_sede FROM sede WHERE id_empresa=@e3 AND nombre='Sede Alcázar' LIMIT 1);

INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion)
SELECT @e3,'Sede Anova (Medicina)','Carrera 13 # 15N-46, Edificio Anova, Armenia','Armenia','0000000000','Lunes a viernes, 08:00 a 17:00','Sede de demostración. Dirección tomada del mock, sin verificación de vigencia.'
WHERE NOT EXISTS (SELECT 1 FROM sede WHERE id_empresa=@e3 AND nombre='Sede Anova (Medicina)');

SET @s5 = (SELECT id_sede FROM sede WHERE id_empresa=@e3 AND nombre='Sede Anova (Medicina)' LIMIT 1);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos)
SELECT @s1,'Línea de ensamble A','Salón de demostración APLOMO.',1,12
WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s1 AND nombre='Línea de ensamble A');

SET @l1 = (SELECT id_salon FROM salon WHERE id_sede=@s1 AND nombre='Línea de ensamble A' LIMIT 1);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos)
SELECT @s2,'Taller de lijado','Salón de demostración APLOMO.',1,12
WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s2 AND nombre='Taller de lijado');

SET @l2 = (SELECT id_salon FROM salon WHERE id_sede=@s2 AND nombre='Taller de lijado' LIMIT 1);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos)
SELECT @s3,'Laboratorio de Métodos y Movimientos','Salón de demostración APLOMO.',1,12
WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s3 AND nombre='Laboratorio de Métodos y Movimientos');

SET @l3 = (SELECT id_salon FROM salon WHERE id_sede=@s3 AND nombre='Laboratorio de Métodos y Movimientos' LIMIT 1);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos)
SELECT @s4,'Mantenimiento','Salón de demostración APLOMO.',1,12
WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s4 AND nombre='Mantenimiento');

SET @l4 = (SELECT id_salon FROM salon WHERE id_sede=@s4 AND nombre='Mantenimiento' LIMIT 1);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos)
SELECT @s5,'Laboratorio de simulación clínica','Salón de demostración APLOMO.',1,12
WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s5 AND nombre='Laboratorio de simulación clínica');

SET @l5 = (SELECT id_salon FROM salon WHERE id_sede=@s5 AND nombre='Laboratorio de simulación clínica' LIMIT 1);

INSERT INTO area (id_empresa,nombre)
SELECT @e3,'Docencia y laboratorio'
WHERE NOT EXISTS (SELECT 1 FROM area WHERE id_empresa=@e3 AND nombre='Docencia y laboratorio');

SET @area = (SELECT id_area FROM area WHERE id_empresa=@e3 AND nombre='Docencia y laboratorio' LIMIT 1);

INSERT INTO cargo (id_empresa,nombre,tipo)
SELECT @e3,'Practicante de laboratorio','OPERATIVO'
WHERE NOT EXISTS (SELECT 1 FROM cargo WHERE id_empresa=@e3 AND nombre='Practicante de laboratorio');

SET @cargo = (SELECT id_cargo FROM cargo WHERE id_empresa=@e3 AND nombre='Practicante de laboratorio' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion,ultimo_puntaje)
SELECT @l3,@area,@cargo,1,'Ensamble de piezas en prácticas de laboratorio','MESA','Demostración',1
WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@l3 AND numero=1);

SET @p1 = (SELECT id_puesto FROM puesto_trabajo WHERE id_salon=@l3 AND numero=1 LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion,ultimo_puntaje)
SELECT @l3,@area,@cargo,2,'Medición de piezas con calibrador','MESA','Demostración',3
WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@l3 AND numero=2);

SET @p2 = (SELECT id_puesto FROM puesto_trabajo WHERE id_salon=@l3 AND numero=2 LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion,ultimo_puntaje)
SELECT @l3,@area,@cargo,3,'Ensamble didáctico en mesa alta','MESA','Demostración',7
WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@l3 AND numero=3);

SET @p3 = (SELECT id_puesto FROM puesto_trabajo WHERE id_salon=@l3 AND numero=3 LIMIT 1);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado)
SELECT @l3,@demo_admin,'DEMO-FIN-01','2026-11-10','08:00','10:00','EVALUACION_INICIAL','Reserva de demostración APLOMO.','Datos ficticios. No representa una visita real.','FINALIZADA'
WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-FIN-01');

SET @r1 = (SELECT id_reserva FROM reserva WHERE codigo='DEMO-FIN-01' LIMIT 1);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado)
SELECT @l1,@demo_admin,'DEMO-COL-01','2026-11-16','09:00','10:00','EVALUACION_INICIAL','Reserva de demostración APLOMO.','Datos ficticios. No representa una visita real.','EN_COLA'
WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-COL-01');

SET @r2 = (SELECT id_reserva FROM reserva WHERE codigo='DEMO-COL-01' LIMIT 1);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado)
SELECT @l2,@demo_admin,'DEMO-ESP-01','2026-11-17','14:00','15:00','EVALUACION_INICIAL','Reserva de demostración APLOMO.','Datos ficticios. No representa una visita real.','ESPERANDO'
WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-ESP-01');

SET @r3 = (SELECT id_reserva FROM reserva WHERE codigo='DEMO-ESP-01' LIMIT 1);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado)
SELECT @l4,@demo_admin,'DEMO-ESP-02','2026-11-18','08:30','09:30','SEGUIMIENTO','Reserva de demostración APLOMO.','Datos ficticios. No representa una visita real.','ESPERANDO'
WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-ESP-02');

SET @r4 = (SELECT id_reserva FROM reserva WHERE codigo='DEMO-ESP-02' LIMIT 1);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado)
SELECT @l5,@demo_admin,'DEMO-COL-02','2026-11-19','10:00','11:00','EVALUACION_INICIAL','Reserva de demostración APLOMO.','Datos ficticios. No representa una visita real.','EN_COLA'
WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-COL-02');

SET @r5 = (SELECT id_reserva FROM reserva WHERE codigo='DEMO-COL-02' LIMIT 1);

INSERT INTO visita (id_reserva,hora_real_inicio,hora_real_fin,notas,calificacion_final,estado)
SELECT @r1,'2026-11-10 08:00:00','2026-11-10 10:00:00','Visita ficticia para demostrar el informe.',3.7,'FINALIZADA'
WHERE NOT EXISTS (SELECT 1 FROM visita WHERE id_reserva=@r1);

SET @v1 = (SELECT id_visita FROM visita WHERE id_reserva=@r1 LIMIT 1);

INSERT INTO evaluacion (id_visita,id_puesto,fecha,postura,criterio,motivo,minutos_jornada,persona_acepto,registro_fotografico)
SELECT @v1,@p1,'2026-11-10','Ensamble de piezas en prácticas de laboratorio','MAS_INADECUADA','Caso ficticio de demostración, con ángulos manuales del plan de pruebas.',120,TRUE,'CON_FOTOS'
WHERE NOT EXISTS (SELECT 1 FROM evaluacion WHERE id_visita=@v1 AND id_puesto=@p1);

SET @ev1 = (SELECT id_evaluacion FROM evaluacion WHERE id_visita=@v1 AND id_puesto=@p1 LIMIT 1);

INSERT INTO condiciones_puesto (id_evaluacion,escritorio_muy_alto,pantalla_muy_alta,pies_colgando,tiene_apoyapies,silla_con_apoyabrazos,silla_regulable,otras_observaciones)
SELECT @ev1,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,'Demostración: no es una valoración de una persona real.'
WHERE NOT EXISTS (SELECT 1 FROM condiciones_puesto WHERE id_evaluacion=@ev1);

INSERT INTO fotografia (id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,descripcion)
SELECT @ev1,'de100001-493b-4bc9-a64e-d51b4ea3c901.jpg','de100001-493b-4bc9-a64e-d51b4ea3c901.jpg',60,'PERFIL','Imagen de muestra generada con IA. Resultado ficticio del caso manual, no una medición de esta imagen.'
WHERE NOT EXISTS (SELECT 1 FROM fotografia WHERE id_evaluacion=@ev1 AND nombre_archivo='de100001-493b-4bc9-a64e-d51b4ea3c901.jpg');

SET @f1 = (SELECT id_foto FROM fotografia WHERE id_evaluacion=@ev1 AND nombre_archivo='de100001-493b-4bc9-a64e-d51b4ea3c901.jpg' LIMIT 1);

SET @nivel1 = (SELECT id_nivel FROM nivel_riesgo WHERE 1 BETWEEN puntaje_minimo AND puntaje_maximo LIMIT 1);

INSERT INTO analisis_rula (id_foto,id_nivel,fecha_hora,lado,hombro_elevado,brazo_separado,brazo_apoyado,antebrazo_cruza_linea_media,muneca_desviada,giro_muneca,cuello_girado,cuello_inclinado,tronco_girado,tronco_inclinado,piernas_apoyadas,postura_estatica_o_repetitiva,carga,puntaje_a,puntaje_b,puntaje_final)
SELECT @f1,@nivel1,'2026-11-10 09:00:00','DERECHO',FALSE,FALSE,FALSE,FALSE,FALSE,'MEDIO',FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,'MENOS_2KG_INTERMITENTE',1,1,1
WHERE NOT EXISTS (SELECT 1 FROM analisis_rula WHERE id_foto=@f1);

SET @a1 = (SELECT id_analisis FROM analisis_rula WHERE id_foto=@f1 LIMIT 1);

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a1,'BRAZO','[]',0,1,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a1 AND segmento='BRAZO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a1,'ANTEBRAZO','[]',90,1,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a1 AND segmento='ANTEBRAZO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a1,'MUNECA','[]',0,1,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a1 AND segmento='MUNECA');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a1,'CUELLO','[]',0,1,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a1 AND segmento='CUELLO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a1,'TRONCO','[]',0,1,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a1 AND segmento='TRONCO');

INSERT INTO bitacora (id_cuenta,fecha_hora,tabla,id_registro,accion,detalle)
SELECT @demo_admin,'2026-11-10 09:00:00','evaluacion',@ev1,'CREAR','Demostración APLOMO: registro ficticio.'
WHERE NOT EXISTS (SELECT 1 FROM bitacora WHERE tabla='evaluacion' AND id_registro=@ev1 AND accion='CREAR');

INSERT INTO bitacora (id_cuenta,fecha_hora,tabla,id_registro,accion,detalle)
SELECT @demo_admin,'2026-11-10 09:00:00','analisis_rula',@a1,'CREAR','Demostración APLOMO: registro ficticio.'
WHERE NOT EXISTS (SELECT 1 FROM bitacora WHERE tabla='analisis_rula' AND id_registro=@a1 AND accion='CREAR');

INSERT INTO evaluacion (id_visita,id_puesto,fecha,postura,criterio,motivo,minutos_jornada,persona_acepto,registro_fotografico)
SELECT @v1,@p2,'2026-11-10','Medición de piezas con calibrador','MAS_INADECUADA','Caso ficticio de demostración, con ángulos manuales del plan de pruebas.',120,TRUE,'CON_FOTOS'
WHERE NOT EXISTS (SELECT 1 FROM evaluacion WHERE id_visita=@v1 AND id_puesto=@p2);

SET @ev2 = (SELECT id_evaluacion FROM evaluacion WHERE id_visita=@v1 AND id_puesto=@p2 LIMIT 1);

INSERT INTO condiciones_puesto (id_evaluacion,escritorio_muy_alto,pantalla_muy_alta,pies_colgando,tiene_apoyapies,silla_con_apoyabrazos,silla_regulable,otras_observaciones)
SELECT @ev2,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,'Demostración: no es una valoración de una persona real.'
WHERE NOT EXISTS (SELECT 1 FROM condiciones_puesto WHERE id_evaluacion=@ev2);

INSERT INTO fotografia (id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,descripcion)
SELECT @ev2,'de100002-493b-4bc9-a64e-d51b4ea3c902.jpg','de100002-493b-4bc9-a64e-d51b4ea3c902.jpg',60,'PERFIL','Imagen de muestra generada con IA. Resultado ficticio del caso manual, no una medición de esta imagen.'
WHERE NOT EXISTS (SELECT 1 FROM fotografia WHERE id_evaluacion=@ev2 AND nombre_archivo='de100002-493b-4bc9-a64e-d51b4ea3c902.jpg');

SET @f2 = (SELECT id_foto FROM fotografia WHERE id_evaluacion=@ev2 AND nombre_archivo='de100002-493b-4bc9-a64e-d51b4ea3c902.jpg' LIMIT 1);

SET @nivel2 = (SELECT id_nivel FROM nivel_riesgo WHERE 3 BETWEEN puntaje_minimo AND puntaje_maximo LIMIT 1);

INSERT INTO analisis_rula (id_foto,id_nivel,fecha_hora,lado,hombro_elevado,brazo_separado,brazo_apoyado,antebrazo_cruza_linea_media,muneca_desviada,giro_muneca,cuello_girado,cuello_inclinado,tronco_girado,tronco_inclinado,piernas_apoyadas,postura_estatica_o_repetitiva,carga,puntaje_a,puntaje_b,puntaje_final)
SELECT @f2,@nivel2,'2026-11-10 09:00:00','DERECHO',FALSE,FALSE,FALSE,FALSE,FALSE,'MEDIO',FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,'MENOS_2KG_INTERMITENTE',3,3,3
WHERE NOT EXISTS (SELECT 1 FROM analisis_rula WHERE id_foto=@f2);

SET @a2 = (SELECT id_analisis FROM analisis_rula WHERE id_foto=@f2 LIMIT 1);

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a2,'BRAZO','[]',61,3,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a2 AND segmento='BRAZO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a2,'ANTEBRAZO','[]',47,2,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a2 AND segmento='ANTEBRAZO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a2,'MUNECA','[]',0,1,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a2 AND segmento='MUNECA');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a2,'CUELLO','[]',37,3,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a2 AND segmento='CUELLO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a2,'TRONCO','[]',17,2,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a2 AND segmento='TRONCO');

INSERT INTO bitacora (id_cuenta,fecha_hora,tabla,id_registro,accion,detalle)
SELECT @demo_admin,'2026-11-10 09:00:00','evaluacion',@ev2,'CREAR','Demostración APLOMO: registro ficticio.'
WHERE NOT EXISTS (SELECT 1 FROM bitacora WHERE tabla='evaluacion' AND id_registro=@ev2 AND accion='CREAR');

INSERT INTO bitacora (id_cuenta,fecha_hora,tabla,id_registro,accion,detalle)
SELECT @demo_admin,'2026-11-10 09:00:00','analisis_rula',@a2,'CREAR','Demostración APLOMO: registro ficticio.'
WHERE NOT EXISTS (SELECT 1 FROM bitacora WHERE tabla='analisis_rula' AND id_registro=@a2 AND accion='CREAR');

INSERT INTO evaluacion (id_visita,id_puesto,fecha,postura,criterio,motivo,minutos_jornada,persona_acepto,registro_fotografico)
SELECT @v1,@p3,'2026-11-10','Ensamble didáctico en mesa alta','MAS_INADECUADA','Caso ficticio de demostración, con ángulos manuales del plan de pruebas.',120,TRUE,'CON_FOTOS'
WHERE NOT EXISTS (SELECT 1 FROM evaluacion WHERE id_visita=@v1 AND id_puesto=@p3);

SET @ev3 = (SELECT id_evaluacion FROM evaluacion WHERE id_visita=@v1 AND id_puesto=@p3 LIMIT 1);

INSERT INTO condiciones_puesto (id_evaluacion,escritorio_muy_alto,pantalla_muy_alta,pies_colgando,tiene_apoyapies,silla_con_apoyabrazos,silla_regulable,otras_observaciones)
SELECT @ev3,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,'Demostración: no es una valoración de una persona real.'
WHERE NOT EXISTS (SELECT 1 FROM condiciones_puesto WHERE id_evaluacion=@ev3);

INSERT INTO fotografia (id_evaluacion,nombre_archivo,ruta,tamano_kb,vista,descripcion)
SELECT @ev3,'de100003-493b-4bc9-a64e-d51b4ea3c903.jpg','de100003-493b-4bc9-a64e-d51b4ea3c903.jpg',60,'PERFIL','Imagen de muestra generada con IA. Resultado ficticio del caso manual, no una medición de esta imagen.'
WHERE NOT EXISTS (SELECT 1 FROM fotografia WHERE id_evaluacion=@ev3 AND nombre_archivo='de100003-493b-4bc9-a64e-d51b4ea3c903.jpg');

SET @f3 = (SELECT id_foto FROM fotografia WHERE id_evaluacion=@ev3 AND nombre_archivo='de100003-493b-4bc9-a64e-d51b4ea3c903.jpg' LIMIT 1);

SET @nivel3 = (SELECT id_nivel FROM nivel_riesgo WHERE 7 BETWEEN puntaje_minimo AND puntaje_maximo LIMIT 1);

INSERT INTO analisis_rula (id_foto,id_nivel,fecha_hora,lado,hombro_elevado,brazo_separado,brazo_apoyado,antebrazo_cruza_linea_media,muneca_desviada,giro_muneca,cuello_girado,cuello_inclinado,tronco_girado,tronco_inclinado,piernas_apoyadas,postura_estatica_o_repetitiva,carga,puntaje_a,puntaje_b,puntaje_final)
SELECT @f3,@nivel3,'2026-11-10 09:00:00','DERECHO',FALSE,FALSE,FALSE,FALSE,FALSE,'MEDIO',FALSE,FALSE,FALSE,FALSE,TRUE,TRUE,'MAS_10KG_INTERMITENTE',6,6,7
WHERE NOT EXISTS (SELECT 1 FROM analisis_rula WHERE id_foto=@f3);

SET @a3 = (SELECT id_analisis FROM analisis_rula WHERE id_foto=@f3 LIMIT 1);

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a3,'BRAZO','[]',61,3,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a3 AND segmento='BRAZO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a3,'ANTEBRAZO','[]',47,2,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a3 AND segmento='ANTEBRAZO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a3,'MUNECA','[]',0,1,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a3 AND segmento='MUNECA');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a3,'CUELLO','[]',37,3,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a3 AND segmento='CUELLO');

INSERT INTO medicion_angulo (id_analisis,segmento,puntos_marcados,angulo_grados,puntos,corregido_a_mano)
SELECT @a3,'TRONCO','[]',17,2,TRUE
WHERE NOT EXISTS (SELECT 1 FROM medicion_angulo WHERE id_analisis=@a3 AND segmento='TRONCO');

INSERT INTO bitacora (id_cuenta,fecha_hora,tabla,id_registro,accion,detalle)
SELECT @demo_admin,'2026-11-10 09:00:00','evaluacion',@ev3,'CREAR','Demostración APLOMO: registro ficticio.'
WHERE NOT EXISTS (SELECT 1 FROM bitacora WHERE tabla='evaluacion' AND id_registro=@ev3 AND accion='CREAR');

INSERT INTO bitacora (id_cuenta,fecha_hora,tabla,id_registro,accion,detalle)
SELECT @demo_admin,'2026-11-10 09:00:00','analisis_rula',@a3,'CREAR','Demostración APLOMO: registro ficticio.'
WHERE NOT EXISTS (SELECT 1 FROM bitacora WHERE tabla='analisis_rula' AND id_registro=@a3 AND accion='CREAR');

INSERT INTO pendiente_arreglo (id_evaluacion,descripcion,prioridad,fecha_limite,puntaje,fecha_registro,estado)
SELECT @ev3,'Demostración: revisar la postura y la organización del puesto.','ALTA','2026-11-20',7,'2026-11-10','ABIERTO'
WHERE NOT EXISTS (SELECT 1 FROM pendiente_arreglo WHERE id_evaluacion=@ev3);

INSERT INTO sugerencia (id_evaluacion,titulo,descripcion,tipo,prioridad,estado,origen,activo)
SELECT @ev2,'Revisar la postura de trabajo','Sugerencia ficticia: revisar la postura y acercar las piezas antes de reevaluar.','POSTURA','MEDIA','PENDIENTE','ADMINISTRADOR',TRUE
WHERE NOT EXISTS (SELECT 1 FROM sugerencia WHERE id_evaluacion=@ev2 AND titulo='Revisar la postura de trabajo');

INSERT INTO sugerencia (id_evaluacion,titulo,descripcion,tipo,prioridad,estado,origen,activo)
SELECT @ev3,'Revisar la postura de trabajo','Sugerencia ficticia: revisar la postura y acercar las piezas antes de reevaluar.','POSTURA','ALTA','PENDIENTE','ADMINISTRADOR',TRUE
WHERE NOT EXISTS (SELECT 1 FROM sugerencia WHERE id_evaluacion=@ev3 AND titulo='Revisar la postura de trabajo');

COMMIT;

-- Fin. Volver a ejecutarlo no duplica filas ni restablece estados editados.
