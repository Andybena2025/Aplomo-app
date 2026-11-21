-- Paso 27A: aditivo e idempotente. Requiere respaldo y administrador de MySQL.
SET NAMES utf8mb4;
SET @existe = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='empresa' AND column_name='id_creador');
SET @orden = IF(@existe=0, 'ALTER TABLE empresa ADD COLUMN id_creador INT NULL', 'SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
SET @existe = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='empresa' AND column_name='sector');
SET @orden = IF(@existe=0, 'ALTER TABLE empresa ADD COLUMN sector VARCHAR(60) NULL', 'SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
SET @existe = (SELECT COUNT(*) FROM information_schema.table_constraints
 WHERE constraint_schema=DATABASE() AND table_name='empresa' AND constraint_name='fk_empresa_creador');
SET @orden = IF(@existe=0,
 'ALTER TABLE empresa ADD CONSTRAINT fk_empresa_creador FOREIGN KEY (id_creador) REFERENCES cuenta(id_cuenta)',
 'SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
UPDATE empresa SET id_creador=id_administrador WHERE id_creador IS NULL;
UPDATE cuenta SET color_agenda='#FF4F9A' WHERE rol='EVALUADOR' AND color_agenda='#FF8FB8';
UPDATE cuenta SET color_agenda='#3D7BFF' WHERE rol='EVALUADOR' AND color_agenda='#6FA8FF';
UPDATE cuenta SET color_agenda='#16BFC9' WHERE rol='EVALUADOR' AND color_agenda='#B9A7FF';
UPDATE cuenta SET color_agenda='#9B6BFF' WHERE rol='EVALUADOR' AND color_agenda='#7FD1E0';
UPDATE cuenta SET color_agenda='#FF8FD0' WHERE rol='EVALUADOR' AND color_agenda='#F4B393';
UPDATE cuenta SET color_agenda='#C9CCD2' WHERE rol='EVALUADOR' AND color_agenda='#C9CCD2';
UPDATE cuenta SET color_agenda='#FF4F9A' WHERE correo='andres.evaluador@rula.local' AND rol='EVALUADOR';
UPDATE cuenta SET color_agenda='#3D7BFF' WHERE correo='johan.evaluador@rula.local' AND rol='EVALUADOR';
UPDATE cuenta SET color_agenda='#16BFC9' WHERE correo='gabriella.evaluadora@rula.local' AND rol='EVALUADOR';

-- Paso 27B: cada cita conserva su ingeniero; no se cambian asignaciones existentes.
SET @existe=(SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='reserva' AND column_name='id_evaluador');
SET @orden=IF(@existe=0,'ALTER TABLE reserva ADD COLUMN id_evaluador INT NULL','SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
SET @existe=(SELECT COUNT(*) FROM information_schema.table_constraints
 WHERE constraint_schema=DATABASE() AND table_name='reserva' AND constraint_name='fk_reserva_evaluador');
SET @orden=IF(@existe=0,
 'ALTER TABLE reserva ADD CONSTRAINT fk_reserva_evaluador FOREIGN KEY(id_evaluador) REFERENCES cuenta(id_cuenta)',
 'SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
ALTER TABLE reserva MODIFY id_administrador INT NOT NULL COMMENT 'Cuenta que registró: Evaluador o Usuario; legado Administrador';
UPDATE reserva r JOIN salon l USING(id_salon) JOIN sede s USING(id_sede)
SET r.id_evaluador=(SELECT q.id_cuenta FROM cuenta_empresa ce JOIN cuenta q USING(id_cuenta)
 WHERE ce.id_empresa=s.id_empresa AND ce.activo=TRUE AND q.rol='EVALUADOR' AND q.estado='ACTIVA'
 ORDER BY (q.id_cuenta=r.id_administrador) DESC,q.id_cuenta LIMIT 1)
WHERE r.id_evaluador IS NULL;
CREATE TABLE IF NOT EXISTS mensaje (
  id_mensaje INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  id_reserva INT NULL,
  id_remitente INT NOT NULL,
  id_destinatario INT NOT NULL,
  tipo ENUM('NOTA_ADMIN','SOLICITUD_VISITA','PEDIR_LLAMADA','PEDIR_INFORMACION','RESPUESTA','SOLICITUD_ACCESO','AVISO') NOT NULL,
  texto VARCHAR(500) NOT NULL,
  creado DATETIME NOT NULL,
  leido BOOLEAN NOT NULL DEFAULT FALSE,
  INDEX ix_mensaje_destinatario (id_destinatario,leido,creado),
  CONSTRAINT fk_mensaje_reserva FOREIGN KEY (id_reserva) REFERENCES reserva(id_reserva),
  CONSTRAINT fk_mensaje_remitente FOREIGN KEY (id_remitente) REFERENCES cuenta(id_cuenta),
  CONSTRAINT fk_mensaje_destinatario FOREIGN KEY (id_destinatario) REFERENCES cuenta(id_cuenta)
) ENGINE=InnoDB;

-- Las notas anteriores también llegan a la bandeja; un segundo pase no las duplica ni reinicia el acuse.
INSERT INTO mensaje(id_reserva,id_remitente,id_destinatario,tipo,texto,creado,leido)
SELECT r.id_reserva,COALESCE((SELECT b.id_cuenta FROM bitacora b
 WHERE b.tabla='reserva' AND b.id_registro=r.id_reserva AND b.detalle='Nota del Administrador'
 ORDER BY b.id_bitacora DESC LIMIT 1),r.id_administrador),
 r.id_evaluador,'NOTA_ADMIN',r.nota_administrador,
 COALESCE((SELECT MAX(b.fecha_hora) FROM bitacora b WHERE b.tabla='reserva'
 AND b.id_registro=r.id_reserva AND b.detalle='Nota del Administrador'),TIMESTAMP(r.fecha,r.hora_inicio)),r.nota_vista
FROM reserva r WHERE r.id_evaluador IS NOT NULL AND r.nota_administrador IS NOT NULL
 AND TRIM(r.nota_administrador)<>'' AND NOT EXISTS(SELECT 1 FROM mensaje m
 WHERE m.id_reserva=r.id_reserva AND m.tipo='NOTA_ADMIN' AND m.texto=r.nota_administrador);
