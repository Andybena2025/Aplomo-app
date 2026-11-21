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
