-- =====================================================================
-- Sistema RULA – Evaluación Ergonómica · Script de base de datos MySQL
-- Proyecto Nuclear 6.0 · Matriz de requisitos v6 · Fase de Diseño
-- Basado en el Modelo E-R v6 (21 tablas, 3FN) y el Diccionario de datos v6
-- Motor: MySQL 8.0.16 o superior · InnoDB · utf8mb4 (RNF-001)
-- utf8mb4_unicode_ci compara sin distinguir mayúsculas ni tildes (RF-010)
-- Nada se borra: las llaves foráneas usan ON DELETE RESTRICT (RF-028, RNF-009)
-- =====================================================================

SET NAMES utf8mb4;
DROP DATABASE IF EXISTS rula_ergonomia;
CREATE DATABASE rula_ergonomia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rula_ergonomia;

-- ---------------------------------------------------------------------
-- Acceso y seguridad
-- ---------------------------------------------------------------------

-- cuenta: Cuentas de las personas que entran al sistema. Administrador y Usuario viven en la misma tabla y se distinguen por rol (herencia de Cuenta). (RF-001a, RF-001c, RF-002, RF-003, RNF-004)
CREATE TABLE cuenta (
  id_cuenta INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la cuenta.',
  id_empresa INT NULL COMMENT 'Empresa del Usuario. Vacío para el Administrador.',
  nombre VARCHAR(100) NOT NULL COMMENT 'Nombre de la persona.',
  correo VARCHAR(120) NOT NULL COMMENT 'Correo con el que entra. No se repite.',
  telefono VARCHAR(20) NOT NULL COMMENT 'Teléfono de contacto.',
  cargo VARCHAR(80) NOT NULL COMMENT 'Cargo de la persona (texto).',
  contrasena_hash CHAR(60) NOT NULL COMMENT 'Contraseña cifrada con bcrypt (costo mínimo 10). Nadie puede leerla.',
  contrasena_temporal BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Verdadero si la contraseña es temporal y debe cambiarse al entrar.',
  intentos_fallidos TINYINT NOT NULL DEFAULT 0 COMMENT 'Intentos fallidos seguidos de inicio de sesión.',
  bloqueado_hasta DATETIME NULL COMMENT 'Fecha y hora hasta la que el ingreso queda bloqueado (5 intentos = 15 minutos).',
  estado ENUM('ACTIVA', 'INACTIVA', 'BLOQUEADA') NOT NULL DEFAULT 'ACTIVA' COMMENT 'Estado de la cuenta.',
  rol ENUM('ADMINISTRADOR', 'USUARIO') NOT NULL COMMENT 'Rol de la cuenta.',
  CONSTRAINT pk_cuenta PRIMARY KEY (id_cuenta),
  CONSTRAINT uq_cuenta_correo UNIQUE (correo),
  CONSTRAINT chk_cuenta_intentos CHECK (intentos_fallidos >= 0)
) ENGINE=InnoDB COMMENT='Cuentas de las personas que entran al sistema. Administrador y Usuario viven en la misma tabla y se distinguen por rol (herencia de Cuenta).';

-- bitacora: Registro de quién hizo cada acción, cuándo y qué estado cambió. (RNF-005, RF-012, RF-013a, RF-013b)
CREATE TABLE bitacora (
  id_bitacora INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del registro.',
  id_cuenta INT NOT NULL COMMENT 'Cuenta que hizo la acción.',
  fecha_hora DATETIME NOT NULL COMMENT 'Fecha y hora de la acción.',
  tabla VARCHAR(30) NOT NULL COMMENT 'Nombre de la tabla afectada (por ejemplo, reserva).',
  id_registro INT NOT NULL COMMENT 'Identificador de la fila afectada en esa tabla.',
  accion ENUM('CREAR', 'EDITAR', 'CAMBIAR_ESTADO', 'DESACTIVAR', 'REACTIVAR') NOT NULL COMMENT 'Tipo de acción.',
  estado_anterior VARCHAR(20) NULL COMMENT 'Estado antes del cambio (solo en cambios de estado).',
  estado_nuevo VARCHAR(20) NULL COMMENT 'Estado después del cambio (solo en cambios de estado).',
  detalle VARCHAR(255) NULL COMMENT 'Detalle opcional, por ejemplo el motivo de cancelación.',
  CONSTRAINT pk_bitacora PRIMARY KEY (id_bitacora),
  CONSTRAINT fk_bitacora_id_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuenta (id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Registro de quién hizo cada acción, cuándo y qué estado cambió.';

-- ---------------------------------------------------------------------
-- Fase 1 · Configuración de la empresa
-- ---------------------------------------------------------------------

-- empresa: Empresas cliente que evalúa el Ingeniero Industrial. No se borran: se desactivan. (RF-004, RF-010, RF-028)
CREATE TABLE empresa (
  id_empresa INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la empresa.',
  id_administrador INT NOT NULL COMMENT 'Administrador que registró la empresa.',
  nombre VARCHAR(120) NOT NULL COMMENT 'Nombre de la empresa (obligatorio).',
  nit VARCHAR(20) NOT NULL COMMENT 'NIT de la empresa (obligatorio, no se repite).',
  descripcion VARCHAR(255) NULL COMMENT 'Descripción opcional.',
  telefono VARCHAR(20) NULL COMMENT 'Teléfono opcional.',
  correo VARCHAR(120) NULL COMMENT 'Correo opcional.',
  persona_contacto VARCHAR(100) NULL COMMENT 'Persona de contacto opcional.',
  activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso si la empresa está desactivada.',
  CONSTRAINT pk_empresa PRIMARY KEY (id_empresa),
  CONSTRAINT uq_empresa_nit UNIQUE (nit),
  CONSTRAINT fk_empresa_id_administrador FOREIGN KEY (id_administrador) REFERENCES cuenta (id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Empresas cliente que evalúa el Ingeniero Industrial. No se borran: se desactivan.';

-- La cuenta del Usuario apunta a su empresa: se agrega después porque empresa necesita a cuenta (referencia circular).
ALTER TABLE cuenta ADD CONSTRAINT fk_cuenta_id_empresa FOREIGN KEY (id_empresa) REFERENCES empresa (id_empresa) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- sede: Sedes de cada empresa. (RF-005, RF-010, RF-028)
CREATE TABLE sede (
  id_sede INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la sede.',
  id_empresa INT NOT NULL COMMENT 'Empresa a la que pertenece.',
  nombre VARCHAR(100) NOT NULL COMMENT 'Nombre de la sede; no se repite en la empresa.',
  direccion VARCHAR(150) NOT NULL COMMENT 'Dirección.',
  ciudad VARCHAR(60) NOT NULL COMMENT 'Ciudad.',
  telefono VARCHAR(20) NOT NULL COMMENT 'Teléfono.',
  horario VARCHAR(100) NOT NULL COMMENT 'Horario de atención.',
  descripcion VARCHAR(200) NOT NULL COMMENT 'Descripción de máximo 200 caracteres.',
  activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso si la sede está desactivada.',
  CONSTRAINT pk_sede PRIMARY KEY (id_sede),
  CONSTRAINT uq_sede UNIQUE (id_empresa, nombre),
  CONSTRAINT fk_sede_id_empresa FOREIGN KEY (id_empresa) REFERENCES empresa (id_empresa) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Sedes de cada empresa.';

-- salon: Salones o áreas físicas de cada sede. (RF-006, RF-010, RF-028)
CREATE TABLE salon (
  id_salon INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del salón.',
  id_sede INT NOT NULL COMMENT 'Sede a la que pertenece.',
  nombre VARCHAR(100) NOT NULL COMMENT 'Nombre del salón; no se repite en la sede.',
  descripcion VARCHAR(200) NOT NULL COMMENT 'Descripción de máximo 200 caracteres.',
  piso TINYINT NOT NULL COMMENT 'Piso donde está.',
  capacidad_puestos SMALLINT NOT NULL COMMENT 'Cantidad de puestos que caben.',
  activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso si el salón está desactivado.',
  CONSTRAINT pk_salon PRIMARY KEY (id_salon),
  CONSTRAINT uq_salon UNIQUE (id_sede, nombre),
  CONSTRAINT chk_salon_capacidad CHECK (capacidad_puestos > 0),
  CONSTRAINT fk_salon_id_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Salones o áreas físicas de cada sede.';

-- area: Áreas de la empresa (por ejemplo, Contabilidad). (RF-007, RF-010, RF-028)
CREATE TABLE area (
  id_area INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del área.',
  id_empresa INT NOT NULL COMMENT 'Empresa a la que pertenece.',
  nombre VARCHAR(100) NOT NULL COMMENT 'Nombre del área; no se repite en la empresa.',
  activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso si el área está desactivada.',
  CONSTRAINT pk_area PRIMARY KEY (id_area),
  CONSTRAINT uq_area UNIQUE (id_empresa, nombre),
  CONSTRAINT fk_area_id_empresa FOREIGN KEY (id_empresa) REFERENCES empresa (id_empresa) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Áreas de la empresa (por ejemplo, Contabilidad).';

-- cargo: Cargos de la empresa con su tipo. No se guardan datos de personas, solo el cargo. (RF-007, RF-010, RF-028)
CREATE TABLE cargo (
  id_cargo INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del cargo.',
  id_empresa INT NOT NULL COMMENT 'Empresa a la que pertenece.',
  nombre VARCHAR(100) NOT NULL COMMENT 'Nombre del cargo; no se repite en la empresa.',
  tipo ENUM('OPERATIVO', 'ADMINISTRATIVO', 'OTRO') NOT NULL COMMENT 'Tipo de cargo.',
  activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso si el cargo está desactivado.',
  CONSTRAINT pk_cargo PRIMARY KEY (id_cargo),
  CONSTRAINT uq_cargo UNIQUE (id_empresa, nombre),
  CONSTRAINT fk_cargo_id_empresa FOREIGN KEY (id_empresa) REFERENCES empresa (id_empresa) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Cargos de la empresa con su tipo. No se guardan datos de personas, solo el cargo.';

-- puesto_trabajo: Puestos de trabajo dentro de un salón, con su número y su último puntaje RULA. (RF-008, RF-010, RF-028)
CREATE TABLE puesto_trabajo (
  id_puesto INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del puesto.',
  id_salon INT NOT NULL COMMENT 'Salón donde está el puesto.',
  id_area INT NULL COMMENT 'Área opcional del puesto.',
  id_cargo INT NULL COMMENT 'Cargo opcional del puesto.',
  numero SMALLINT NOT NULL COMMENT 'Número del puesto; no se repite en el salón.',
  descripcion VARCHAR(255) NOT NULL COMMENT 'Descripción del puesto.',
  tipo ENUM('ESCRITORIO', 'MESA', 'MOSTRADOR', 'OTRO') NOT NULL COMMENT 'Tipo de puesto.',
  ubicacion VARCHAR(120) NOT NULL COMMENT 'Ubicación dentro del salón.',
  ultimo_puntaje TINYINT NULL COMMENT 'Puntaje más alto de su última evaluación (1 a 7). Vacío si nunca se ha evaluado. El color sale de nivel_riesgo.',
  activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso si el puesto está desactivado.',
  CONSTRAINT pk_puesto_trabajo PRIMARY KEY (id_puesto),
  CONSTRAINT uq_puesto_trabajo UNIQUE (id_salon, numero),
  CONSTRAINT chk_puesto_ultimo CHECK (ultimo_puntaje IS NULL OR ultimo_puntaje BETWEEN 1 AND 7),
  CONSTRAINT fk_puesto_trabajo_id_salon FOREIGN KEY (id_salon) REFERENCES salon (id_salon) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_puesto_trabajo_id_area FOREIGN KEY (id_area) REFERENCES area (id_area) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_puesto_trabajo_id_cargo FOREIGN KEY (id_cargo) REFERENCES cargo (id_cargo) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Puestos de trabajo dentro de un salón, con su número y su último puntaje RULA.';

-- ---------------------------------------------------------------------
-- Fase 2 · Agenda de visitas
-- ---------------------------------------------------------------------

-- reserva: Reservas de visita a un salón, con su código y su estado. (RF-009b, RF-010, RF-011, RF-012)
CREATE TABLE reserva (
  id_reserva INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la reserva.',
  id_salon INT NOT NULL COMMENT 'Salón reservado; la sede y la empresa se obtienen por el salón.',
  id_administrador INT NOT NULL COMMENT 'Administrador que registró la reserva.',
  codigo VARCHAR(12) NOT NULL COMMENT 'Código único que asigna el sistema (por ejemplo, R-0001). No se cambia.',
  fecha DATE NOT NULL COMMENT 'Fecha de la visita (hoy o posterior).',
  hora_inicio TIME NOT NULL COMMENT 'Hora de inicio.',
  hora_fin TIME NOT NULL COMMENT 'Hora de fin (posterior a la de inicio).',
  categoria ENUM('EVALUACION_INICIAL', 'SEGUIMIENTO', 'REEVALUACION') NOT NULL COMMENT 'Categoría de la visita.',
  descripcion VARCHAR(255) NULL COMMENT 'Descripción opcional.',
  notas TEXT NULL COMMENT 'Notas opcionales.',
  estado ENUM('ESPERANDO', 'EN_COLA', 'FINALIZADA', 'CANCELADA') NOT NULL DEFAULT 'ESPERANDO' COMMENT 'Estado de la reserva.',
  motivo_cancelacion VARCHAR(255) NULL COMMENT 'Motivo obligatorio cuando la reserva se cancela.',
  CONSTRAINT pk_reserva PRIMARY KEY (id_reserva),
  CONSTRAINT uq_reserva UNIQUE (id_salon, fecha, hora_inicio),
  CONSTRAINT uq_reserva_codigo UNIQUE (codigo),
  CONSTRAINT chk_reserva_horas CHECK (hora_fin > hora_inicio),
  CONSTRAINT chk_reserva_motivo CHECK (estado <> 'CANCELADA' OR motivo_cancelacion IS NOT NULL),
  CONSTRAINT fk_reserva_id_salon FOREIGN KEY (id_salon) REFERENCES salon (id_salon) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_reserva_id_administrador FOREIGN KEY (id_administrador) REFERENCES cuenta (id_cuenta) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Reservas de visita a un salón, con su código y su estado.';

-- visita: Visita real hecha a partir de una reserva EN_COLA (una visita por reserva). (RF-013a, RF-013b)
CREATE TABLE visita (
  id_visita INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la visita.',
  id_reserva INT NOT NULL COMMENT 'Reserva de la que nace (una visita por reserva).',
  hora_real_inicio DATETIME NOT NULL COMMENT 'Fecha y hora real de inicio.',
  hora_real_fin DATETIME NULL COMMENT 'Fecha y hora real de fin; vacía mientras está EN_CURSO.',
  notas TEXT NULL COMMENT 'Notas al finalizar.',
  calificacion_final DECIMAL(3,1) NULL COMMENT 'Promedio de los puntajes de los puestos evaluados; se llena al finalizar.',
  estado ENUM('EN_CURSO', 'FINALIZADA') NOT NULL DEFAULT 'EN_CURSO' COMMENT 'Estado de la visita.',
  CONSTRAINT pk_visita PRIMARY KEY (id_visita),
  CONSTRAINT uq_visita_id_reserva UNIQUE (id_reserva),
  CONSTRAINT chk_visita_fin CHECK (estado <> 'FINALIZADA' OR hora_real_fin IS NOT NULL),
  CONSTRAINT fk_visita_id_reserva FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Visita real hecha a partir de una reserva EN_COLA (una visita por reserva).';

-- ---------------------------------------------------------------------
-- Fase 3 · Evaluación RULA
-- ---------------------------------------------------------------------

-- nivel_riesgo: Niveles del semáforo y nivel oficial de RULA. Tiene 4 filas fijas. (RF-020, RF-027)
CREATE TABLE nivel_riesgo (
  id_nivel INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del nivel.',
  color ENUM('VERDE', 'AMARILLO', 'ROJO') NOT NULL COMMENT 'Color del semáforo.',
  puntaje_minimo TINYINT NOT NULL COMMENT 'Puntaje final mínimo del rango.',
  puntaje_maximo TINYINT NOT NULL COMMENT 'Puntaje final máximo del rango.',
  nombre VARCHAR(40) NOT NULL COMMENT 'Nombre que se muestra con el color.',
  accion VARCHAR(150) NOT NULL COMMENT 'Acción que se recomienda.',
  nivel_oficial_rula TINYINT NOT NULL COMMENT 'Nivel de actuación oficial de RULA (1 a 4, Tabla 18 de Ergonautas).',
  CONSTRAINT pk_nivel_riesgo PRIMARY KEY (id_nivel),
  CONSTRAINT chk_nivel_rango CHECK (puntaje_minimo BETWEEN 1 AND 7 AND puntaje_maximo BETWEEN puntaje_minimo AND 7),
  CONSTRAINT chk_nivel_oficial CHECK (nivel_oficial_rula BETWEEN 1 AND 4)
) ENGINE=InnoDB COMMENT='Niveles del semáforo y nivel oficial de RULA. Tiene 4 filas fijas.';

-- valor_tabla_rula: Valores de las Tablas A, B y C de RULA (Ergonautas). Se cargan una vez y no cambian. (RF-027, RNF-010)
CREATE TABLE valor_tabla_rula (
  id_valor INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del valor.',
  tabla ENUM('A', 'B', 'C') NOT NULL COMMENT 'Tabla de RULA a la que pertenece el valor.',
  fila TINYINT NOT NULL COMMENT 'Fila de la tabla (ver la regla de lectura abajo).',
  columna TINYINT NOT NULL COMMENT 'Columna de la tabla (ver la regla de lectura abajo).',
  valor TINYINT NOT NULL COMMENT 'Valor publicado en la tabla.',
  CONSTRAINT pk_valor_tabla_rula PRIMARY KEY (id_valor),
  CONSTRAINT uq_valor_tabla_rula UNIQUE (tabla, fila, columna),
  CONSTRAINT chk_valor_rango CHECK (valor BETWEEN 1 AND 9)
) ENGINE=InnoDB COMMENT='Valores de las Tablas A, B y C de RULA (Ergonautas). Se cargan una vez y no cambian.';

-- evaluacion: Evaluación de un puesto dentro de una visita EN_CURSO: la postura elegida y por qué. (RF-014, RF-016)
CREATE TABLE evaluacion (
  id_evaluacion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la evaluación.',
  id_visita INT NOT NULL COMMENT 'Visita EN_CURSO donde se hace.',
  id_puesto INT NOT NULL COMMENT 'Puesto evaluado.',
  fecha DATE NOT NULL COMMENT 'Fecha de la evaluación.',
  postura VARCHAR(150) NOT NULL COMMENT 'Postura elegida para estudiar.',
  criterio ENUM('MAS_INADECUADA', 'MAYOR_EXPOSICION') NOT NULL COMMENT 'Criterio de elección de la postura.',
  motivo VARCHAR(255) NOT NULL COMMENT 'Motivo de la elección.',
  minutos_jornada SMALLINT NOT NULL COMMENT 'Minutos por jornada en esa postura (1 a 720).',
  metodologia ENUM('RULA') NOT NULL DEFAULT 'RULA' COMMENT 'Metodología usada.',
  persona_acepto BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Casilla «A la persona se le explicó el estudio y aceptó las fotos».',
  registro_fotografico ENUM('CON_FOTOS', 'SIN_REGISTRO_FOTOGRAFICO') NOT NULL COMMENT 'Indica si la evaluación tiene fotos o quedó sin registro fotográfico.',
  CONSTRAINT pk_evaluacion PRIMARY KEY (id_evaluacion),
  CONSTRAINT chk_eval_minutos CHECK (minutos_jornada BETWEEN 1 AND 720),
  CONSTRAINT fk_evaluacion_id_visita FOREIGN KEY (id_visita) REFERENCES visita (id_visita) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_evaluacion_id_puesto FOREIGN KEY (id_puesto) REFERENCES puesto_trabajo (id_puesto) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Evaluación de un puesto dentro de una visita EN_CURSO: la postura elegida y por qué.';

-- condiciones_puesto: Condiciones del puesto marcadas con Sí o No (una fila por evaluación). (RF-015)
CREATE TABLE condiciones_puesto (
  id_evaluacion INT NOT NULL COMMENT 'Evaluación a la que pertenecen las condiciones (llave y foránea a la vez).',
  escritorio_muy_alto BOOLEAN NOT NULL COMMENT 'La superficie queda por encima del codo con los hombros relajados.',
  pantalla_muy_alta BOOLEAN NOT NULL COMMENT 'El borde superior queda por encima de la línea de los ojos.',
  pies_colgando BOOLEAN NOT NULL COMMENT 'Los pies no apoyan completos en el piso.',
  tiene_apoyapies BOOLEAN NOT NULL COMMENT 'El puesto tiene apoyapiés.',
  silla_con_apoyabrazos BOOLEAN NOT NULL COMMENT 'La silla tiene apoyabrazos.',
  silla_regulable BOOLEAN NOT NULL COMMENT 'La silla es regulable.',
  otras_observaciones VARCHAR(255) NULL COMMENT 'Otras observaciones (opcional).',
  CONSTRAINT pk_condiciones_puesto PRIMARY KEY (id_evaluacion),
  CONSTRAINT fk_condiciones_puesto_id_evaluacion FOREIGN KEY (id_evaluacion) REFERENCES evaluacion (id_evaluacion) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Condiciones del puesto marcadas con Sí o No (una fila por evaluación).';

-- fotografia: Fotos de la postura. El archivo vive en una carpeta del servidor; aquí se guardan sus datos y su ruta. (RF-016, RNF-006)
CREATE TABLE fotografia (
  id_foto INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la foto.',
  id_evaluacion INT NOT NULL COMMENT 'Evaluación a la que pertenece.',
  nombre_archivo VARCHAR(120) NOT NULL COMMENT 'Nombre único del archivo en el servidor.',
  ruta VARCHAR(255) NOT NULL COMMENT 'Ruta de la carpeta del servidor donde está la foto.',
  tamano_kb INT NOT NULL COMMENT 'Tamaño en KB (máximo 5 MB = 5120 KB).',
  vista ENUM('FRENTE', 'PERFIL', 'OTRA') NOT NULL COMMENT 'Vista en que se tomó la foto.',
  descripcion VARCHAR(200) NOT NULL COMMENT 'Descripción de máximo 200 caracteres.',
  CONSTRAINT pk_fotografia PRIMARY KEY (id_foto),
  CONSTRAINT chk_foto_tamano CHECK (tamano_kb BETWEEN 1 AND 5120),
  CONSTRAINT fk_fotografia_id_evaluacion FOREIGN KEY (id_evaluacion) REFERENCES evaluacion (id_evaluacion) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Fotos de la postura. El archivo vive en una carpeta del servidor; aquí se guardan sus datos y su ruta.';

-- analisis_rula: Análisis RULA de una foto: lado, ajustes, actividad, carga y puntajes. Cada análisis es nuevo; nada se reemplaza. (RF-017, RF-018, RF-019, RF-020, RF-021)
CREATE TABLE analisis_rula (
  id_analisis INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del análisis.',
  id_foto INT NOT NULL COMMENT 'Foto analizada.',
  id_nivel INT NOT NULL COMMENT 'Nivel de riesgo que le corresponde al puntaje final.',
  fecha_hora DATETIME NOT NULL COMMENT 'Fecha y hora del análisis.',
  lado ENUM('DERECHO', 'IZQUIERDO') NOT NULL COMMENT 'Lado del cuerpo evaluado.',
  hombro_elevado BOOLEAN NOT NULL COMMENT 'Ajuste +1: hombro elevado.',
  brazo_separado BOOLEAN NOT NULL COMMENT 'Ajuste +1: brazo separado del cuerpo.',
  brazo_apoyado BOOLEAN NOT NULL COMMENT 'Ajuste −1: brazo apoyado.',
  antebrazo_cruza_linea_media BOOLEAN NOT NULL COMMENT 'Ajuste +1: antebrazo cruza la línea media o va hacia afuera.',
  muneca_desviada BOOLEAN NOT NULL COMMENT 'Ajuste +1: muñeca desviada.',
  giro_muneca ENUM('MEDIO', 'EXTREMO') NOT NULL COMMENT 'Giro de la muñeca (para la Tabla A).',
  cuello_girado BOOLEAN NOT NULL COMMENT 'Ajuste +1: cuello girado.',
  cuello_inclinado BOOLEAN NOT NULL COMMENT 'Ajuste +1: cuello inclinado lateralmente.',
  tronco_girado BOOLEAN NOT NULL COMMENT 'Ajuste +1: tronco girado.',
  tronco_inclinado BOOLEAN NOT NULL COMMENT 'Ajuste +1: tronco inclinado lateralmente.',
  piernas_apoyadas BOOLEAN NOT NULL COMMENT 'Piernas y pies bien apoyados (para la Tabla B).',
  postura_estatica_o_repetitiva BOOLEAN NOT NULL COMMENT 'Postura estática (más de 1 minuto) o repetitiva (más de 4 veces por minuto): +1.',
  carga ENUM('MENOS_2KG_INTERMITENTE', 'DE_2_A_10KG_INTERMITENTE', 'DE_2_A_10KG_ESTATICA_O_REPETITIVA', 'MAS_10KG_INTERMITENTE', 'MAS_10KG_ESTATICA_O_REPETITIVA', 'GOLPES_O_FUERZAS_BRUSCAS') NOT NULL COMMENT 'Carga o fuerza (suma de 0 a 3 puntos).',
  puntaje_a TINYINT NOT NULL COMMENT 'Puntaje del Grupo A con actividad y carga.',
  puntaje_b TINYINT NOT NULL COMMENT 'Puntaje del Grupo B con actividad y carga.',
  puntaje_final TINYINT NOT NULL COMMENT 'Puntaje final de 1 a 7 (Tabla C).',
  CONSTRAINT pk_analisis_rula PRIMARY KEY (id_analisis),
  CONSTRAINT chk_analisis_final CHECK (puntaje_final BETWEEN 1 AND 7),
  CONSTRAINT fk_analisis_rula_id_foto FOREIGN KEY (id_foto) REFERENCES fotografia (id_foto) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_analisis_rula_id_nivel FOREIGN KEY (id_nivel) REFERENCES nivel_riesgo (id_nivel) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Análisis RULA de una foto: lado, ajustes, actividad, carga y puntajes. Cada análisis es nuevo; nada se reemplaza.';

-- medicion_angulo: Ángulo medido de cada segmento del cuerpo dentro de un análisis. (RF-017, RF-018)
CREATE TABLE medicion_angulo (
  id_medicion INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la medición.',
  id_analisis INT NOT NULL COMMENT 'Análisis al que pertenece.',
  segmento ENUM('BRAZO', 'ANTEBRAZO', 'MUNECA', 'CUELLO', 'TRONCO') NOT NULL COMMENT 'Parte del cuerpo medida; una medición por segmento en cada análisis.',
  puntos_marcados VARCHAR(100) NOT NULL COMMENT 'Coordenadas de los 3 puntos marcados sobre la foto.',
  angulo_grados DECIMAL(5,2) NOT NULL COMMENT 'Ángulo calculado en grados.',
  puntos TINYINT NOT NULL COMMENT 'Puntos RULA del segmento según su ángulo.',
  corregido_a_mano BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Verdadero si el ángulo se escribió a mano.',
  CONSTRAINT pk_medicion_angulo PRIMARY KEY (id_medicion),
  CONSTRAINT uq_medicion_angulo UNIQUE (id_analisis, segmento),
  CONSTRAINT fk_medicion_angulo_id_analisis FOREIGN KEY (id_analisis) REFERENCES analisis_rula (id_analisis) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Ángulo medido de cada segmento del cuerpo dentro de un análisis.';

-- ---------------------------------------------------------------------
-- Fase 4 · Seguimiento e informes
-- ---------------------------------------------------------------------

-- regla_sugerencia: Reglas que usa el sistema para proponer sugerencias según el segmento, el puntaje y las condiciones. (RF-023a)
CREATE TABLE regla_sugerencia (
  id_regla INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la regla.',
  segmento ENUM('BRAZO', 'ANTEBRAZO', 'MUNECA', 'CUELLO', 'TRONCO') NOT NULL COMMENT 'Segmento al que aplica la regla.',
  puntaje_minimo TINYINT NOT NULL COMMENT 'Puntos mínimos del segmento para que la regla aplique.',
  condicion VARCHAR(120) NOT NULL COMMENT 'Condición del puesto que activa la regla (por ejemplo, pies_colgando).',
  texto_sugerido VARCHAR(255) NOT NULL COMMENT 'Texto de la sugerencia que propone el sistema.',
  CONSTRAINT pk_regla_sugerencia PRIMARY KEY (id_regla)
) ENGINE=InnoDB COMMENT='Reglas que usa el sistema para proponer sugerencias según el segmento, el puntaje y las condiciones.';

-- sugerencia: Sugerencias de mejora de una evaluación, propuestas por el sistema o escritas por el Administrador. (RF-023a, RF-023b)
CREATE TABLE sugerencia (
  id_sugerencia INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la sugerencia.',
  id_evaluacion INT NOT NULL COMMENT 'Evaluación a la que pertenece.',
  id_regla INT NULL COMMENT 'Regla que la originó; vacío si la escribió el Administrador.',
  titulo VARCHAR(120) NOT NULL COMMENT 'Título de la sugerencia.',
  descripcion TEXT NOT NULL COMMENT 'Descripción de la mejora.',
  tipo ENUM('MOBILIARIO', 'POSTURA', 'DESCANSOS', 'CARGA', 'OTRA') NOT NULL COMMENT 'Tipo de sugerencia.',
  prioridad ENUM('ALTA', 'MEDIA', 'BAJA') NOT NULL COMMENT 'Prioridad.',
  estado ENUM('PENDIENTE', 'EN_PROCESO', 'HECHA') NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Estado de la sugerencia.',
  fecha_limite DATE NULL COMMENT 'Fecha límite opcional.',
  origen ENUM('SISTEMA', 'ADMINISTRADOR') NOT NULL COMMENT 'Quién la propuso.',
  activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Falso si el Administrador la quitó.',
  CONSTRAINT pk_sugerencia PRIMARY KEY (id_sugerencia),
  CONSTRAINT fk_sugerencia_id_evaluacion FOREIGN KEY (id_evaluacion) REFERENCES evaluacion (id_evaluacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_sugerencia_id_regla FOREIGN KEY (id_regla) REFERENCES regla_sugerencia (id_regla) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Sugerencias de mejora de una evaluación, propuestas por el sistema o escritas por el Administrador.';

-- pendiente_arreglo: Puestos cuyo análisis quedó en Rojo y deben arreglarse. (RF-022)
CREATE TABLE pendiente_arreglo (
  id_pendiente INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del pendiente.',
  id_evaluacion INT NOT NULL COMMENT 'Evaluación que lo generó (el puesto se obtiene por ella). Un pendiente por evaluación.',
  descripcion VARCHAR(255) NULL COMMENT 'Qué hay que arreglar; se escribe después.',
  prioridad ENUM('ALTA', 'MEDIA', 'BAJA') NULL COMMENT 'Prioridad; se escribe después.',
  fecha_limite DATE NULL COMMENT 'Fecha límite; se escribe después.',
  puntaje TINYINT NOT NULL COMMENT 'Puntaje con el que quedó en Rojo.',
  fecha_registro DATE NOT NULL COMMENT 'Fecha en que entró a pendientes.',
  estado ENUM('ABIERTO', 'ARREGLADO') NOT NULL DEFAULT 'ABIERTO' COMMENT 'Estado del pendiente.',
  CONSTRAINT pk_pendiente_arreglo PRIMARY KEY (id_pendiente),
  CONSTRAINT uq_pendiente_arreglo_id_evaluacion UNIQUE (id_evaluacion),
  CONSTRAINT chk_pendiente_puntaje CHECK (puntaje BETWEEN 5 AND 7),
  CONSTRAINT fk_pendiente_arreglo_id_evaluacion FOREIGN KEY (id_evaluacion) REFERENCES evaluacion (id_evaluacion) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Puestos cuyo análisis quedó en Rojo y deben arreglarse.';

-- informe_empresa: Informes PDF generados para una empresa. (RF-025, RF-026)
CREATE TABLE informe_empresa (
  id_informe INT NOT NULL AUTO_INCREMENT COMMENT 'Identificador del informe.',
  id_empresa INT NOT NULL COMMENT 'Empresa del informe.',
  fecha_generacion DATETIME NOT NULL COMMENT 'Fecha y hora en que se generó.',
  ruta VARCHAR(255) NOT NULL COMMENT 'Ruta del PDF en el servidor.',
  calificacion_general DECIMAL(3,1) NULL COMMENT 'Calificación general entre sedes; vacía si la empresa tiene una sola sede.',
  CONSTRAINT pk_informe_empresa PRIMARY KEY (id_informe),
  CONSTRAINT fk_informe_empresa_id_empresa FOREIGN KEY (id_empresa) REFERENCES empresa (id_empresa) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Informes PDF generados para una empresa.';

-- ---------------------------------------------------------------------
-- Datos iniciales
-- ---------------------------------------------------------------------

-- Niveles del semáforo y nivel oficial RULA (RF-020): 4 filas
INSERT INTO nivel_riesgo (color, puntaje_minimo, puntaje_maximo, nombre, accion, nivel_oficial_rula) VALUES
  ('VERDE', 1, 2, 'Verde', 'No necesita cambios', 1),
  ('AMARILLO', 3, 4, 'Amarillo', 'Hacer una revisión', 2),
  ('ROJO', 5, 6, 'Rojo', 'Intervención inmediata', 3),
  ('ROJO', 7, 7, 'Rojo', 'Intervención inmediata', 4);

-- Tablas RULA de Ergonautas (RF-027, RNF-010)
-- Tabla A: fila = (brazo - 1) * 3 + antebrazo (1 a 18); columna = (muñeca - 1) * 2 + giro de muñeca (1 a 8)
INSERT INTO valor_tabla_rula (tabla, fila, columna, valor) VALUES
  ('A', 1, 1, 1), ('A', 1, 2, 2), ('A', 1, 3, 2), ('A', 1, 4, 2), ('A', 1, 5, 2), ('A', 1, 6, 3), ('A', 1, 7, 3), ('A', 1, 8, 3),
  ('A', 2, 1, 2), ('A', 2, 2, 2), ('A', 2, 3, 2), ('A', 2, 4, 2), ('A', 2, 5, 3), ('A', 2, 6, 3), ('A', 2, 7, 3), ('A', 2, 8, 3),
  ('A', 3, 1, 2), ('A', 3, 2, 3), ('A', 3, 3, 3), ('A', 3, 4, 3), ('A', 3, 5, 3), ('A', 3, 6, 3), ('A', 3, 7, 4), ('A', 3, 8, 4),
  ('A', 4, 1, 2), ('A', 4, 2, 3), ('A', 4, 3, 3), ('A', 4, 4, 3), ('A', 4, 5, 3), ('A', 4, 6, 4), ('A', 4, 7, 4), ('A', 4, 8, 4),
  ('A', 5, 1, 3), ('A', 5, 2, 3), ('A', 5, 3, 3), ('A', 5, 4, 3), ('A', 5, 5, 3), ('A', 5, 6, 4), ('A', 5, 7, 4), ('A', 5, 8, 4),
  ('A', 6, 1, 3), ('A', 6, 2, 4), ('A', 6, 3, 4), ('A', 6, 4, 4), ('A', 6, 5, 4), ('A', 6, 6, 4), ('A', 6, 7, 5), ('A', 6, 8, 5),
  ('A', 7, 1, 3), ('A', 7, 2, 3), ('A', 7, 3, 4), ('A', 7, 4, 4), ('A', 7, 5, 4), ('A', 7, 6, 4), ('A', 7, 7, 5), ('A', 7, 8, 5),
  ('A', 8, 1, 3), ('A', 8, 2, 4), ('A', 8, 3, 4), ('A', 8, 4, 4), ('A', 8, 5, 4), ('A', 8, 6, 4), ('A', 8, 7, 5), ('A', 8, 8, 5),
  ('A', 9, 1, 4), ('A', 9, 2, 4), ('A', 9, 3, 4), ('A', 9, 4, 4), ('A', 9, 5, 4), ('A', 9, 6, 5), ('A', 9, 7, 5), ('A', 9, 8, 5),
  ('A', 10, 1, 4), ('A', 10, 2, 4), ('A', 10, 3, 4), ('A', 10, 4, 4), ('A', 10, 5, 4), ('A', 10, 6, 5), ('A', 10, 7, 5), ('A', 10, 8, 5),
  ('A', 11, 1, 4), ('A', 11, 2, 4), ('A', 11, 3, 4), ('A', 11, 4, 4), ('A', 11, 5, 4), ('A', 11, 6, 5), ('A', 11, 7, 5), ('A', 11, 8, 5),
  ('A', 12, 1, 4), ('A', 12, 2, 4), ('A', 12, 3, 4), ('A', 12, 4, 5), ('A', 12, 5, 5), ('A', 12, 6, 5), ('A', 12, 7, 6), ('A', 12, 8, 6),
  ('A', 13, 1, 5), ('A', 13, 2, 5), ('A', 13, 3, 5), ('A', 13, 4, 5), ('A', 13, 5, 5), ('A', 13, 6, 6), ('A', 13, 7, 6), ('A', 13, 8, 7),
  ('A', 14, 1, 5), ('A', 14, 2, 6), ('A', 14, 3, 6), ('A', 14, 4, 6), ('A', 14, 5, 6), ('A', 14, 6, 7), ('A', 14, 7, 7), ('A', 14, 8, 7),
  ('A', 15, 1, 6), ('A', 15, 2, 6), ('A', 15, 3, 6), ('A', 15, 4, 7), ('A', 15, 5, 7), ('A', 15, 6, 7), ('A', 15, 7, 7), ('A', 15, 8, 8),
  ('A', 16, 1, 7), ('A', 16, 2, 7), ('A', 16, 3, 7), ('A', 16, 4, 7), ('A', 16, 5, 7), ('A', 16, 6, 8), ('A', 16, 7, 8), ('A', 16, 8, 9),
  ('A', 17, 1, 8), ('A', 17, 2, 8), ('A', 17, 3, 8), ('A', 17, 4, 8), ('A', 17, 5, 8), ('A', 17, 6, 9), ('A', 17, 7, 9), ('A', 17, 8, 9),
  ('A', 18, 1, 9), ('A', 18, 2, 9), ('A', 18, 3, 9), ('A', 18, 4, 9), ('A', 18, 5, 9), ('A', 18, 6, 9), ('A', 18, 7, 9), ('A', 18, 8, 9);
-- Tabla B: fila = cuello (1 a 6); columna = (tronco - 1) * 2 + piernas (1 a 12)
INSERT INTO valor_tabla_rula (tabla, fila, columna, valor) VALUES
  ('B', 1, 1, 1), ('B', 1, 2, 3), ('B', 1, 3, 2), ('B', 1, 4, 3), ('B', 1, 5, 3), ('B', 1, 6, 4),
  ('B', 1, 7, 5), ('B', 1, 8, 5), ('B', 1, 9, 6), ('B', 1, 10, 6), ('B', 1, 11, 7), ('B', 1, 12, 7),
  ('B', 2, 1, 2), ('B', 2, 2, 3), ('B', 2, 3, 2), ('B', 2, 4, 3), ('B', 2, 5, 4), ('B', 2, 6, 5),
  ('B', 2, 7, 5), ('B', 2, 8, 5), ('B', 2, 9, 6), ('B', 2, 10, 7), ('B', 2, 11, 7), ('B', 2, 12, 7),
  ('B', 3, 1, 3), ('B', 3, 2, 3), ('B', 3, 3, 3), ('B', 3, 4, 4), ('B', 3, 5, 4), ('B', 3, 6, 5),
  ('B', 3, 7, 5), ('B', 3, 8, 6), ('B', 3, 9, 6), ('B', 3, 10, 7), ('B', 3, 11, 7), ('B', 3, 12, 7),
  ('B', 4, 1, 5), ('B', 4, 2, 5), ('B', 4, 3, 5), ('B', 4, 4, 6), ('B', 4, 5, 6), ('B', 4, 6, 7),
  ('B', 4, 7, 7), ('B', 4, 8, 7), ('B', 4, 9, 7), ('B', 4, 10, 7), ('B', 4, 11, 8), ('B', 4, 12, 8),
  ('B', 5, 1, 7), ('B', 5, 2, 7), ('B', 5, 3, 7), ('B', 5, 4, 7), ('B', 5, 5, 7), ('B', 5, 6, 8),
  ('B', 5, 7, 8), ('B', 5, 8, 8), ('B', 5, 9, 8), ('B', 5, 10, 8), ('B', 5, 11, 8), ('B', 5, 12, 8),
  ('B', 6, 1, 8), ('B', 6, 2, 8), ('B', 6, 3, 8), ('B', 6, 4, 8), ('B', 6, 5, 8), ('B', 6, 6, 8),
  ('B', 6, 7, 8), ('B', 6, 8, 9), ('B', 6, 9, 9), ('B', 6, 10, 9), ('B', 6, 11, 9), ('B', 6, 12, 9);
-- Tabla C: fila = puntaje A (1 a 8; 8 = 8 o más); columna = puntaje B (1 a 7; 7 = 7 o más)
INSERT INTO valor_tabla_rula (tabla, fila, columna, valor) VALUES
  ('C', 1, 1, 1), ('C', 1, 2, 2), ('C', 1, 3, 3), ('C', 1, 4, 3), ('C', 1, 5, 4), ('C', 1, 6, 5), ('C', 1, 7, 5),
  ('C', 2, 1, 2), ('C', 2, 2, 2), ('C', 2, 3, 3), ('C', 2, 4, 4), ('C', 2, 5, 4), ('C', 2, 6, 5), ('C', 2, 7, 5),
  ('C', 3, 1, 3), ('C', 3, 2, 3), ('C', 3, 3, 3), ('C', 3, 4, 4), ('C', 3, 5, 4), ('C', 3, 6, 5), ('C', 3, 7, 6),
  ('C', 4, 1, 3), ('C', 4, 2, 3), ('C', 4, 3, 3), ('C', 4, 4, 4), ('C', 4, 5, 5), ('C', 4, 6, 6), ('C', 4, 7, 6),
  ('C', 5, 1, 4), ('C', 5, 2, 4), ('C', 5, 3, 4), ('C', 5, 4, 5), ('C', 5, 5, 6), ('C', 5, 6, 7), ('C', 5, 7, 7),
  ('C', 6, 1, 4), ('C', 6, 2, 4), ('C', 6, 3, 5), ('C', 6, 4, 6), ('C', 6, 5, 6), ('C', 6, 6, 7), ('C', 6, 7, 7),
  ('C', 7, 1, 5), ('C', 7, 2, 5), ('C', 7, 3, 6), ('C', 7, 4, 6), ('C', 7, 5, 7), ('C', 7, 6, 7), ('C', 7, 7, 7),
  ('C', 8, 1, 5), ('C', 8, 2, 5), ('C', 8, 3, 6), ('C', 8, 4, 7), ('C', 8, 5, 7), ('C', 8, 6, 7), ('C', 8, 7, 7);

-- Cuenta de Administrador inicial (RF-003). Contraseña temporal: Cambiar2026 (bcrypt, costo 10).
-- contrasena_temporal = TRUE obliga a cambiarla al entrar por primera vez (RF-001c).
INSERT INTO cuenta (id_empresa, nombre, correo, telefono, cargo, contrasena_hash, contrasena_temporal, rol) VALUES
  (NULL, 'Administrador inicial', 'admin@rula.local', '0000000000', 'Ingeniero Industrial', '$2b$10$61ckJOxWDjDbXztnXpRCje5Hgln1RcLFjrhM./0NPm.6t6OOpcYma', TRUE, 'ADMINISTRADOR');

-- ---------------------------------------------------------------------
-- Usuario de la aplicación con permisos mínimos (RNF-004): sin DELETE, DROP ni GRANT
-- Cambiar la contraseña antes de usarlo.
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'app_rula'@'localhost' IDENTIFIED BY 'CambiarEstaClave2026';
GRANT SELECT, INSERT, UPDATE ON rula_ergonomia.* TO 'app_rula'@'localhost';
FLUSH PRIVILEGES;

-- Fin del script
