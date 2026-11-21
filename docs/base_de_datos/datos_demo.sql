-- Paso 24: Demo.cargar también crea 2 Evaluadores y 2 Usuarios demo, asignaciones, agenda y resultados propios.
-- Paso 17: estructura ficticia e idempotente. Resultados y fotos se crean con Demo.cargar y el motor real.
-- No ejecutar este SQL solo para obtener evaluaciones: usar scripts/cargar_demo o el perfil Docker demo.

SET NAMES utf8mb4;

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

INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion)
SELECT @l3,@area,@cargo,1,'Ensamble de piezas en prácticas de laboratorio','MESA','Demostración'
WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@l3 AND numero=1);

SET @p1 = (SELECT id_puesto FROM puesto_trabajo WHERE id_salon=@l3 AND numero=1 LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion)
SELECT @l3,@area,@cargo,2,'Medición de piezas con calibrador','MESA','Demostración'
WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@l3 AND numero=2);

SET @p2 = (SELECT id_puesto FROM puesto_trabajo WHERE id_salon=@l3 AND numero=2 LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion)
SELECT @l3,@area,@cargo,3,'Ensamble didáctico en mesa alta','MESA','Demostración'
WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@l3 AND numero=3);

SET @p3 = (SELECT id_puesto FROM puesto_trabajo WHERE id_salon=@l3 AND numero=3 LIMIT 1);

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

INSERT INTO puesto_trabajo (id_salon,id_area,id_cargo,numero,descripcion,tipo,ubicacion)
SELECT @l4,@area,@cargo,1,'Revisión de piezas de demostración','MESA','Demostración'
WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@l4 AND numero=1);

SET @p4 = (SELECT id_puesto FROM puesto_trabajo WHERE id_salon=@l4 AND numero=1 LIMIT 1);

INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion) SELECT @e1,'Oficina administrativa de demostración','Dirección ficticia','Armenia','0000000000','Lunes a viernes, 08:00 a 17:00','DEMO-APLOMO: sede para practicar.' WHERE NOT EXISTS (SELECT 1 FROM sede WHERE id_empresa=@e1 AND nombre='Oficina administrativa de demostración');

SET @s6=(SELECT id_sede FROM sede WHERE id_empresa=@e1 AND nombre='Oficina administrativa de demostración' LIMIT 1);

INSERT INTO sede (id_empresa,nombre,direccion,ciudad,telefono,horario,descripcion) SELECT @e2,'Oficina administrativa de demostración','Dirección ficticia','Armenia','0000000000','Lunes a viernes, 08:00 a 17:00','DEMO-APLOMO: sede para practicar.' WHERE NOT EXISTS (SELECT 1 FROM sede WHERE id_empresa=@e2 AND nombre='Oficina administrativa de demostración');

SET @s7=(SELECT id_sede FROM sede WHERE id_empresa=@e2 AND nombre='Oficina administrativa de demostración' LIMIT 1);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) SELECT @s1,'DEMO-APLOMO · Oficina y planta','Práctica guiada, datos ficticios.',1,2 WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s1 AND nombre='DEMO-APLOMO · Oficina y planta');

SET @listo1=(SELECT id_salon FROM salon WHERE id_sede=@s1 AND nombre='DEMO-APLOMO · Oficina y planta' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo1,1,'DEMO-APLOMO · Trabajo en computador','ESCRITORIO','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo1 AND numero=1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo1,2,'DEMO-APLOMO · Levantamiento de piezas','MESA','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo1 AND numero=2);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) SELECT @s2,'DEMO-APLOMO · Oficina y planta','Práctica guiada, datos ficticios.',1,2 WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s2 AND nombre='DEMO-APLOMO · Oficina y planta');

SET @listo2=(SELECT id_salon FROM salon WHERE id_sede=@s2 AND nombre='DEMO-APLOMO · Oficina y planta' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo2,1,'DEMO-APLOMO · Trabajo en computador','ESCRITORIO','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo2 AND numero=1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo2,2,'DEMO-APLOMO · Levantamiento de piezas','MESA','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo2 AND numero=2);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) SELECT @s3,'DEMO-APLOMO · Oficina y planta','Práctica guiada, datos ficticios.',1,2 WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s3 AND nombre='DEMO-APLOMO · Oficina y planta');

SET @listo3=(SELECT id_salon FROM salon WHERE id_sede=@s3 AND nombre='DEMO-APLOMO · Oficina y planta' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo3,1,'DEMO-APLOMO · Trabajo en computador','ESCRITORIO','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo3 AND numero=1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo3,2,'DEMO-APLOMO · Levantamiento de piezas','MESA','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo3 AND numero=2);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) SELECT @s4,'DEMO-APLOMO · Oficina y planta','Práctica guiada, datos ficticios.',1,2 WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s4 AND nombre='DEMO-APLOMO · Oficina y planta');

SET @listo4=(SELECT id_salon FROM salon WHERE id_sede=@s4 AND nombre='DEMO-APLOMO · Oficina y planta' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo4,1,'DEMO-APLOMO · Trabajo en computador','ESCRITORIO','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo4 AND numero=1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo4,2,'DEMO-APLOMO · Levantamiento de piezas','MESA','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo4 AND numero=2);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) SELECT @s5,'DEMO-APLOMO · Oficina y planta','Práctica guiada, datos ficticios.',1,2 WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s5 AND nombre='DEMO-APLOMO · Oficina y planta');

SET @listo5=(SELECT id_salon FROM salon WHERE id_sede=@s5 AND nombre='DEMO-APLOMO · Oficina y planta' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo5,1,'DEMO-APLOMO · Trabajo en computador','ESCRITORIO','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo5 AND numero=1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo5,2,'DEMO-APLOMO · Levantamiento de piezas','MESA','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo5 AND numero=2);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) SELECT @s6,'DEMO-APLOMO · Oficina y planta','Práctica guiada, datos ficticios.',1,2 WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s6 AND nombre='DEMO-APLOMO · Oficina y planta');

SET @listo6=(SELECT id_salon FROM salon WHERE id_sede=@s6 AND nombre='DEMO-APLOMO · Oficina y planta' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo6,1,'DEMO-APLOMO · Trabajo en computador','ESCRITORIO','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo6 AND numero=1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo6,2,'DEMO-APLOMO · Levantamiento de piezas','MESA','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo6 AND numero=2);

INSERT INTO salon (id_sede,nombre,descripcion,piso,capacidad_puestos) SELECT @s7,'DEMO-APLOMO · Oficina y planta','Práctica guiada, datos ficticios.',1,2 WHERE NOT EXISTS (SELECT 1 FROM salon WHERE id_sede=@s7 AND nombre='DEMO-APLOMO · Oficina y planta');

SET @listo7=(SELECT id_salon FROM salon WHERE id_sede=@s7 AND nombre='DEMO-APLOMO · Oficina y planta' LIMIT 1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo7,1,'DEMO-APLOMO · Trabajo en computador','ESCRITORIO','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo7 AND numero=1);

INSERT INTO puesto_trabajo (id_salon,numero,descripcion,tipo,ubicacion) SELECT @listo7,2,'DEMO-APLOMO · Levantamiento de piezas','MESA','Demostración' WHERE NOT EXISTS (SELECT 1 FROM puesto_trabajo WHERE id_salon=@listo7 AND numero=2);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado) SELECT @listo1,@demo_admin,'DEMO-LST-E1',CURRENT_DATE(),'08:00','09:00','EVALUACION_INICIAL','DEMO-APLOMO · Lista para evaluar','Práctica ficticia, Oficina y Planta.','EN_COLA' WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-LST-E1');

INSERT INTO visita (id_reserva,hora_real_inicio,notas,estado) SELECT r.id_reserva,NOW(),'DEMO-APLOMO · Lista para practicar','EN_CURSO' FROM reserva r WHERE r.codigo='DEMO-LST-E1' AND NOT EXISTS (SELECT 1 FROM visita WHERE id_reserva=r.id_reserva);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado) SELECT @listo2,@demo_admin,'DEMO-LST-E2',CURRENT_DATE(),'08:00','09:00','EVALUACION_INICIAL','DEMO-APLOMO · Lista para evaluar','Práctica ficticia, Oficina y Planta.','EN_COLA' WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-LST-E2');

INSERT INTO visita (id_reserva,hora_real_inicio,notas,estado) SELECT r.id_reserva,NOW(),'DEMO-APLOMO · Lista para practicar','EN_CURSO' FROM reserva r WHERE r.codigo='DEMO-LST-E2' AND NOT EXISTS (SELECT 1 FROM visita WHERE id_reserva=r.id_reserva);

INSERT INTO reserva (id_salon,id_administrador,codigo,fecha,hora_inicio,hora_fin,categoria,descripcion,notas,estado) SELECT @listo3,@demo_admin,'DEMO-LST-E3',CURRENT_DATE(),'08:00','09:00','EVALUACION_INICIAL','DEMO-APLOMO · Lista para evaluar','Práctica ficticia, Oficina y Planta.','EN_COLA' WHERE NOT EXISTS (SELECT 1 FROM reserva WHERE codigo='DEMO-LST-E3');

INSERT INTO visita (id_reserva,hora_real_inicio,notas,estado) SELECT r.id_reserva,NOW(),'DEMO-APLOMO · Lista para practicar','EN_CURSO' FROM reserva r WHERE r.codigo='DEMO-LST-E3' AND NOT EXISTS (SELECT 1 FROM visita WHERE id_reserva=r.id_reserva);

COMMIT;
