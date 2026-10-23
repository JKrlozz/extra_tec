-- Esquema MySQL 8.0 de extra_tec. Ejecutar antes de seed.sql.
DROP DATABASE IF EXISTS extra_tec;
CREATE DATABASE extra_tec CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE extra_tec;

-- Catálogos
CREATE TABLE carreras (
  id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE periodos (
  id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre  VARCHAR(40) NOT NULL UNIQUE,          -- 'Ene/Jun 2025'
  inicio  DATE NOT NULL,
  fin     DATE NOT NULL,
  activo  TINYINT(1) NOT NULL DEFAULT 0,
  CHECK (fin > inicio)
) ENGINE=InnoDB;

-- Usuarios y roles
-- Cuenta única de acceso: correo, contraseña y rol
CREATE TABLE usuarios (
  id               INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre           VARCHAR(100) NOT NULL,
  correo           VARCHAR(120) NOT NULL UNIQUE,
  contrasena_hash  VARCHAR(255) NOT NULL,        -- bcrypt/argon2 generado por la API
  rol              ENUM('alumno','docente','admin') NOT NULL,
  activo           TINYINT(1) NOT NULL DEFAULT 1,
  creado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_usuarios_rol (rol)
) ENGINE=InnoDB;

CREATE TABLE alumnos (
  id_usuario  INT UNSIGNED PRIMARY KEY,
  matricula   VARCHAR(15) NOT NULL UNIQUE,
  id_carrera  INT UNSIGNED NOT NULL,
  semestre    TINYINT UNSIGNED NOT NULL,
  CHECK (semestre BETWEEN 1 AND 12),
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE,
  FOREIGN KEY (id_carrera) REFERENCES carreras(id)
) ENGINE=InnoDB;

CREATE TABLE docentes (
  id_usuario    INT UNSIGNED PRIMARY KEY,
  especialidad  VARCHAR(80),
  telefono      VARCHAR(20),
  FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Actividades
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

-- Una actividad puede tener varios días
CREATE TABLE horarios (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_actividad  INT UNSIGNED NOT NULL,
  dia_semana    ENUM('Lunes','Martes','Miércoles','Jueves','Viernes','Sábado') NOT NULL,
  hora_inicio   TIME NOT NULL,
  hora_fin      TIME NOT NULL,
  lugar         VARCHAR(60) DEFAULT 'ITSM',
  UNIQUE KEY uq_horario (id_actividad, dia_semana, hora_inicio),
  CHECK (hora_fin > hora_inicio),
  FOREIGN KEY (id_actividad) REFERENCES actividades(id) ON DELETE CASCADE
) ENGINE=InnoDB;

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

-- Asistencia y evidencias
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

CREATE TABLE evidencias (
  id            INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_actividad  INT UNSIGNED NOT NULL,
  id_docente    INT UNSIGNED NULL,
  descripcion   VARCHAR(255),
  ruta_foto     VARCHAR(255) NOT NULL,           -- la imagen se guarda en disco; aquí solo la ruta
  subida_en     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_actividad) REFERENCES actividades(id) ON DELETE CASCADE,
  FOREIGN KEY (id_docente)   REFERENCES docentes(id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Constancias
CREATE TABLE constancias (
  id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  id_inscripcion  INT UNSIGNED NOT NULL UNIQUE,
  folio           VARCHAR(30) NOT NULL UNIQUE,
  porcentaje      DECIMAL(5,2) NOT NULL,
  emitida_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (id_inscripcion) REFERENCES inscripciones(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Vistas para pantallas y reportes
-- Cupo inscritos/total, horario resumido y docente
CREATE VIEW v_actividades AS
SELECT a.id, a.nombre, a.descripcion, a.cupo, a.activa, p.nombre AS periodo,
       u.nombre AS docente, a.id_docente,
       (SELECT COUNT(*) FROM inscripciones i
         WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos,
       (SELECT GROUP_CONCAT(CONCAT(LEFT(h.dia_semana,3),' ',TIME_FORMAT(h.hora_inicio,'%H:%i'))
                            ORDER BY FIELD(h.dia_semana,'Lunes','Martes','Miércoles','Jueves','Viernes','Sábado')
                            SEPARATOR ', ')
          FROM horarios h WHERE h.id_actividad = a.id) AS horario
FROM actividades a
JOIN periodos p ON p.id = a.id_periodo
LEFT JOIN usuarios u ON u.id = a.id_docente;

-- Porcentaje de asistencia por inscripción
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

-- Bloquea inscripciones cuando el cupo está lleno
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
DELIMITER ;
