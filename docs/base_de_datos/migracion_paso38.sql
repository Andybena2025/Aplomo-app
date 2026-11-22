-- Paso 38B: migración aditiva e idempotente. Respaldo previo; no borra datos.
SET NAMES utf8mb4;
SET @existe = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='solicitud_acceso' AND column_name='tipo');
SET @orden = IF(@existe=0,
 'ALTER TABLE solicitud_acceso ADD COLUMN tipo ENUM(''EMPRESA'',''EVALUADOR'') NOT NULL DEFAULT ''EMPRESA'' AFTER id_solicitud',
 'SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
SET @existe = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='solicitud_acceso' AND column_name='empresa' AND is_nullable='NO');
SET @orden = IF(@existe=1, 'ALTER TABLE solicitud_acceso MODIFY COLUMN empresa VARCHAR(120) NULL', 'SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
SET @existe = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='solicitud_acceso' AND column_name='nit' AND is_nullable='NO');
SET @orden = IF(@existe=1, 'ALTER TABLE solicitud_acceso MODIFY COLUMN nit VARCHAR(20) NULL', 'SELECT 1');
PREPARE migracion FROM @orden;
EXECUTE migracion;
DEALLOCATE PREPARE migracion;
