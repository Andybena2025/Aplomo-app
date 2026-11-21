-- Paso 24: migración aditiva e idempotente; sin borrar datos. Ejecutar en la base seleccionada.
SET NAMES utf8mb4;
ALTER TABLE cuenta MODIFY rol ENUM('ADMINISTRADOR','EVALUADOR','USUARIO') NOT NULL COMMENT 'Rol de la cuenta.';
ALTER TABLE cuenta MODIFY id_empresa INT NULL COMMENT 'Relación anterior conservada. La fuente de asignaciones es cuenta_empresa.';
ALTER TABLE empresa MODIFY id_administrador INT NOT NULL COMMENT 'Cuenta que registró (Administrador o Evaluador)';
ALTER TABLE reserva MODIFY id_administrador INT NOT NULL COMMENT 'Cuenta que registró (Administrador o Evaluador)';
CREATE TABLE IF NOT EXISTS cuenta_empresa (
 id_cuenta_empresa INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la asignación.',
 id_cuenta INT NOT NULL COMMENT 'Cuenta Evaluador o Usuario asignada.',
 id_empresa INT NOT NULL COMMENT 'Empresa dentro de su alcance.',
 activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso al retirar la asignación; conserva historial.',
 fecha_asignacion DATETIME NOT NULL COMMENT 'Fecha de la última asignación.',
 CONSTRAINT pk_cuenta_empresa PRIMARY KEY(id_cuenta_empresa),
 CONSTRAINT uq_cuenta_empresa UNIQUE(id_cuenta,id_empresa),
 CONSTRAINT fk_ce_cuenta FOREIGN KEY(id_cuenta) REFERENCES cuenta(id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_ce_empresa FOREIGN KEY(id_empresa) REFERENCES empresa(id_empresa) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='RF-002/003: empresas asignadas por el Administrador.';
INSERT IGNORE INTO cuenta_empresa(id_cuenta,id_empresa,activo,fecha_asignacion)
 SELECT id_cuenta,id_empresa,TRUE,NOW() FROM cuenta WHERE rol='USUARIO' AND id_empresa IS NOT NULL;
SET @paso24_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reserva' AND column_name='nota_administrador'), 'SELECT 1', 'ALTER TABLE reserva ADD COLUMN nota_administrador VARCHAR(500) NULL COMMENT ''Nota privada del Administrador para el Evaluador.''');
PREPARE paso24_stmt FROM @paso24_sql;
EXECUTE paso24_stmt;
DEALLOCATE PREPARE paso24_stmt;
SET @paso24_sql = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reserva' AND column_name='nota_vista'), 'SELECT 1', 'ALTER TABLE reserva ADD COLUMN nota_vista BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''El Evaluador confirmó que leyó la nota.''');
PREPARE paso24_stmt FROM @paso24_sql;
EXECUTE paso24_stmt;
DEALLOCATE PREPARE paso24_stmt;
