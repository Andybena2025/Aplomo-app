-- Paso 26: campo aditivo. No borra ni recrea tablas o registros.
SET NAMES utf8mb4;
SET @paso26_sql = IF(
 EXISTS(SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='cuenta' AND column_name='color_agenda'),
 'SELECT 1',
 'ALTER TABLE cuenta ADD COLUMN color_agenda CHAR(7) NULL COMMENT ''Color del Evaluador en Agenda; no representa riesgo RULA.''');
PREPARE paso26_stmt FROM @paso26_sql;
EXECUTE paso26_stmt;
DEALLOCATE PREPARE paso26_stmt;
UPDATE cuenta SET color_agenda='#FF8FB8'
WHERE rol='EVALUADOR' AND color_agenda IS NULL
 AND (SELECT COUNT(*) FROM
      (SELECT color_agenda FROM cuenta WHERE rol='EVALUADOR' AND estado<>'INACTIVA'
       GROUP BY color_agenda) colores WHERE color_agenda='#FF8FB8')=0
ORDER BY id_cuenta LIMIT 1;
UPDATE cuenta SET color_agenda='#6FA8FF'
WHERE rol='EVALUADOR' AND color_agenda IS NULL
 AND (SELECT COUNT(*) FROM
      (SELECT color_agenda FROM cuenta WHERE rol='EVALUADOR' AND estado<>'INACTIVA'
       GROUP BY color_agenda) colores WHERE color_agenda='#6FA8FF')=0
ORDER BY id_cuenta LIMIT 1;
UPDATE cuenta SET color_agenda='#B9A7FF'
WHERE rol='EVALUADOR' AND color_agenda IS NULL
 AND (SELECT COUNT(*) FROM
      (SELECT color_agenda FROM cuenta WHERE rol='EVALUADOR' AND estado<>'INACTIVA'
       GROUP BY color_agenda) colores WHERE color_agenda='#B9A7FF')=0
ORDER BY id_cuenta LIMIT 1;
UPDATE cuenta SET color_agenda='#7FD1E0'
WHERE rol='EVALUADOR' AND color_agenda IS NULL
 AND (SELECT COUNT(*) FROM
      (SELECT color_agenda FROM cuenta WHERE rol='EVALUADOR' AND estado<>'INACTIVA'
       GROUP BY color_agenda) colores WHERE color_agenda='#7FD1E0')=0
ORDER BY id_cuenta LIMIT 1;
UPDATE cuenta SET color_agenda='#F4B393'
WHERE rol='EVALUADOR' AND color_agenda IS NULL
 AND (SELECT COUNT(*) FROM
      (SELECT color_agenda FROM cuenta WHERE rol='EVALUADOR' AND estado<>'INACTIVA'
       GROUP BY color_agenda) colores WHERE color_agenda='#F4B393')=0
ORDER BY id_cuenta LIMIT 1;
UPDATE cuenta SET color_agenda='#C9CCD2'
WHERE rol='EVALUADOR' AND color_agenda IS NULL
 AND (SELECT COUNT(*) FROM
      (SELECT color_agenda FROM cuenta WHERE rol='EVALUADOR' AND estado<>'INACTIVA'
       GROUP BY color_agenda) colores WHERE color_agenda='#C9CCD2')=0
ORDER BY id_cuenta LIMIT 1;
