-- Esquema MySQL de la aplicacion
-- Ejecutar antes de los datos de prueba
DROP DATABASE IF EXISTS extra_tec;
CREATE DATABASE extra_tec CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE extra_tec;

-- Catalogos

CREATE TABLE carreras (
  id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- El nombre del periodo se calcula con las fechas
CREATE TABLE periodos (
  id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  inicio  DATE NOT NULL,
  fin     DATE NOT NULL,
  activo  TINYINT(1) NOT NULL DEFAULT 0,
  CHECK (fin > inicio)
) ENGINE=InnoDB;

-- Dias de la semana
CREATE TABLE dias_semana (
  id     TINYINT UNSIGNED PRIMARY KEY,
  nombre VARCHAR(10) NOT NULL UNIQUE,
  orden  TINYINT UNSIGNED NOT NULL UNIQUE
) ENGINE=InnoDB;

-- Lugares de las actividades
CREATE TABLE lugares (
  id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- Personas

-- El nombre se guarda en columnas separadas
-- El nombre completo se calcula
CREATE TABLE usuarios (
  id                INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombres           VARCHAR(60) NOT NULL,
  apellido_paterno  VARCHAR(40) NOT NULL,
  apellido_materno  VARCHAR(40) NULL,
  nombre_completo   VARCHAR(150) GENERATED ALWAYS AS (
                      CONCAT_WS(' ', nombres, apellido_paterno, apellido_materno)
                    ) STORED,
  correo            VARCHAR(120) NOT NULL UNIQUE,
  contrasena_hash   VARCHAR(255) NOT NULL, -- clave cifrada
  rol               ENUM('alumno','docente','admin') NOT NULL,
  activo            TINYINT(1) NOT NULL DEFAULT 1,
  creado_en         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_usuarios_rol (rol),
  INDEX idx_usuarios_nombre (apellido_paterno, apellido_materno, nombres)
) ENGINE=InnoDB;

-- El numero de control tiene tres digitos una T y cuatro digitos
-- El semestre va de primero a noveno
CREATE TABLE alumnos (
  id_usuario  INT UNSIGNED PRIMARY KEY,
  matricula   CHAR(8) NOT NULL UNIQUE,
  id_carrera  INT UNSIGNED NOT NULL,
  semestre    TINYINT UNSIGNED NOT NULL,
  CHECK (matricula REGEXP '^[0-9]{3}T[0-9]{4}$'),
  CHECK (semestre BETWEEN 1 AND 9),
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE,
  FOREIGN KEY (id_carrera) REFERENCES carreras(id)
) ENGINE=InnoDB;

CREATE TABLE docentes (
  id_usuario    INT UNSIGNED PRIMARY KEY,
  especialidad  VARCHAR(80),
  telefono      VARCHAR(20),
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Actividades y horarios

CREATE TABLE actividades (
  id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre      VARCHAR(80) NOT NULL,
  descripcion VARCHAR(255),
  cupo        SMALLINT UNSIGNED NOT NULL,
  id_docente  INT UNSIGNED NULL,
  id_periodo  INT UNSIGNED NOT NULL,
  activa      TINYINT(1) NOT NULL DEFAULT 1,
  UNIQUE KEY uq_actividad_periodo (nombre, id_periodo),
  CHECK (cupo > 0),
  FOREIGN KEY (id_docente) REFERENCES docentes(id_usuario) ON DELETE SET NULL,
  FOREIGN KEY (id_periodo) REFERENCES periodos(id)
) ENGINE=InnoDB;

-- Cada fila es un bloque de horario
-- El dia y el lugar se guardan como claves
CREATE TABLE horarios (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_actividad  INT UNSIGNED NOT NULL,
  id_dia        TINYINT UNSIGNED NOT NULL,
  hora_inicio   TIME NOT NULL,
  hora_fin      TIME NOT NULL,
  id_lugar      INT UNSIGNED NOT NULL,
  UNIQUE KEY uq_horario (id_actividad, id_dia, hora_inicio),
  CHECK (hora_fin > hora_inicio),
  FOREIGN KEY (id_actividad) REFERENCES actividades(id) ON DELETE CASCADE,
  FOREIGN KEY (id_dia)       REFERENCES dias_semana(id),
  FOREIGN KEY (id_lugar)     REFERENCES lugares(id)
) ENGINE=InnoDB;

-- Inscripciones asistencia y evidencias

CREATE TABLE inscripciones (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_alumno     INT UNSIGNED NOT NULL,
  id_actividad  INT UNSIGNED NOT NULL,
  fecha         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado        ENUM('activa','baja') NOT NULL DEFAULT 'activa',
  UNIQUE KEY uq_inscripcion (id_alumno, id_actividad),
  FOREIGN KEY (id_alumno)    REFERENCES alumnos(id_usuario) ON DELETE CASCADE,
  FOREIGN KEY (id_actividad) REFERENCES actividades(id)     ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE sesiones (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_actividad  INT UNSIGNED NOT NULL,
  fecha         DATE NOT NULL,
  UNIQUE KEY uq_sesion (id_actividad, fecha),
  FOREIGN KEY (id_actividad) REFERENCES actividades(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE asistencias (
  id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_sesion  INT UNSIGNED NOT NULL,
  id_alumno  INT UNSIGNED NOT NULL,
  presente   TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uq_asistencia (id_sesion, id_alumno),
  FOREIGN KEY (id_sesion) REFERENCES sesiones(id) ON DELETE CASCADE,
  FOREIGN KEY (id_alumno) REFERENCES alumnos(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- El docente registra quien subio la evidencia
-- El titular puede cambiar despues
CREATE TABLE evidencias (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_actividad  INT UNSIGNED NOT NULL,
  id_docente    INT UNSIGNED NULL,
  descripcion   VARCHAR(255),
  ruta_foto     VARCHAR(255) NOT NULL, -- ruta de la imagen
  subida_en     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_actividad) REFERENCES actividades(id) ON DELETE CASCADE,
  FOREIGN KEY (id_docente)   REFERENCES docentes(id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Constancias

-- El porcentaje se calcula al consultar
CREATE TABLE constancias (
  id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_inscripcion  INT UNSIGNED NOT NULL UNIQUE,
  folio           VARCHAR(30) NOT NULL UNIQUE,
  emitida_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_inscripcion) REFERENCES inscripciones(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Vistas de datos calculados

-- Nombre del periodo
CREATE VIEW v_periodos AS
SELECT id, inicio, fin, activo,
       CONCAT(ELT(MONTH(inicio),'Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'),
              '/',
              ELT(MONTH(fin),'Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'),
              ' ', YEAR(inicio)) AS nombre
FROM periodos;

-- Cupo horario y docente de cada actividad
CREATE VIEW v_actividades AS
SELECT a.id, a.nombre, a.descripcion, a.cupo, a.activa, p.nombre AS periodo,
       u.nombre_completo AS docente, a.id_docente,
       (SELECT COUNT(*) FROM inscripciones i
         WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos,
       (SELECT GROUP_CONCAT(CONCAT(LEFT(d.nombre,3),' ',TIME_FORMAT(h.hora_inicio,'%H:%i'))
                            ORDER BY d.orden SEPARATOR ', ')
          FROM horarios h JOIN dias_semana d ON d.id = h.id_dia
         WHERE h.id_actividad = a.id) AS horario
FROM actividades a
JOIN v_periodos p ON p.id = a.id_periodo
LEFT JOIN usuarios u ON u.id = a.id_docente;

-- Porcentaje de asistencia
CREATE VIEW v_asistencia_alumno AS
SELECT i.id AS id_inscripcion, i.id_alumno, i.id_actividad,
       COUNT(s.id) AS sesiones,
       COALESCE(SUM(asi.presente),0) AS asistencias,
       ROUND(100 * COALESCE(SUM(asi.presente),0) / NULLIF(COUNT(s.id),0), 2) AS porcentaje
FROM inscripciones i
LEFT JOIN sesiones s      ON s.id_actividad = i.id_actividad
LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
WHERE i.estado = 'activa'
GROUP BY i.id, i.id_alumno, i.id_actividad;

-- Bloquea inscripciones con cupo lleno
DELIMITER $$
CREATE TRIGGER trg_inscripcion_cupo BEFORE INSERT ON inscripciones
FOR EACH ROW
BEGIN
  DECLARE v_cupo INT;
  DECLARE v_ins INT;
  SELECT cupo INTO v_cupo FROM actividades WHERE id = NEW.id_actividad;
  SELECT COUNT(*) INTO v_ins FROM inscripciones
   WHERE id_actividad = NEW.id_actividad AND estado = 'activa';
  IF v_ins >= v_cupo THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cupo lleno para esta actividad';
  END IF;
END$$

-- Un alumno solo puede tener una actividad activa
-- Al darse de baja queda libre para otra
CREATE TRIGGER trg_inscripcion_unica_alta BEFORE INSERT ON inscripciones
FOR EACH ROW
BEGIN
  DECLARE v_otras INT;
  IF NEW.estado = 'activa' THEN
    SELECT COUNT(*) INTO v_otras FROM inscripciones
     WHERE id_alumno = NEW.id_alumno AND estado = 'activa';
    IF v_otras > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El alumno ya tiene una actividad inscrita';
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_inscripcion_unica_cambio BEFORE UPDATE ON inscripciones
FOR EACH ROW
BEGIN
  DECLARE v_otras INT;
  IF NEW.estado = 'activa' THEN
    SELECT COUNT(*) INTO v_otras FROM inscripciones
     WHERE id_alumno = NEW.id_alumno AND estado = 'activa' AND id <> NEW.id;
    IF v_otras > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El alumno ya tiene una actividad inscrita';
    END IF;
  END IF;
END$$
DELIMITER ;
