-- Esquema SQLite de la aplicacion
-- La app crea este esquema la primera vez
-- Este archivo es la referencia de la estructura

PRAGMA foreign_keys = ON;

-- Catalogos

CREATE TABLE carreras (
  id     INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL UNIQUE
);

-- El nombre del periodo se calcula con las fechas
CREATE TABLE periodos (
  id     INTEGER PRIMARY KEY AUTOINCREMENT,
  inicio TEXT NOT NULL,
  fin    TEXT NOT NULL,
  activo INTEGER NOT NULL DEFAULT 0 CHECK (activo IN (0, 1)),
  CHECK (fin > inicio)
);

-- Dias de la semana
CREATE TABLE dias_semana (
  id     INTEGER PRIMARY KEY,
  nombre TEXT NOT NULL UNIQUE,
  orden  INTEGER NOT NULL UNIQUE
);

-- Lugares de las actividades
CREATE TABLE lugares (
  id     INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL UNIQUE
);

-- Personas

CREATE TABLE usuarios (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  nombres          TEXT NOT NULL,
  apellido_paterno TEXT NOT NULL,
  apellido_materno TEXT,
  correo           TEXT NOT NULL UNIQUE,
  contrasena_hash  TEXT NOT NULL,
  rol              TEXT NOT NULL CHECK (rol IN ('alumno', 'docente', 'admin')),
  activo           INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1)),
  creado_en        TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX idx_usuarios_nombre ON usuarios (apellido_paterno, apellido_materno, nombres);

CREATE TABLE alumnos (
  id_usuario INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
  matricula  TEXT NOT NULL UNIQUE
             CHECK (matricula GLOB '[0-9][0-9][0-9]T[0-9][0-9][0-9][0-9]'),
  id_carrera INTEGER NOT NULL REFERENCES carreras(id),
  semestre   INTEGER NOT NULL CHECK (semestre BETWEEN 1 AND 9)
);

CREATE TABLE docentes (
  id_usuario   INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
  especialidad TEXT,
  telefono     TEXT
);

-- Actividades y horarios

CREATE TABLE actividades (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre      TEXT NOT NULL,
  descripcion TEXT,
  cupo        INTEGER NOT NULL CHECK (cupo > 0),
  id_docente  INTEGER REFERENCES docentes(id_usuario) ON DELETE SET NULL,
  id_periodo  INTEGER NOT NULL REFERENCES periodos(id),
  activa      INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0, 1)),
  UNIQUE (nombre, id_periodo)
);

-- Cada fila es un bloque de horario
CREATE TABLE horarios (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  id_actividad INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
  id_dia       INTEGER NOT NULL REFERENCES dias_semana(id),
  hora_inicio  TEXT NOT NULL,
  hora_fin     TEXT NOT NULL,
  id_lugar     INTEGER NOT NULL REFERENCES lugares(id),
  UNIQUE (id_actividad, id_dia, hora_inicio),
  CHECK (hora_fin > hora_inicio)
);

-- Inscripciones y asistencia

CREATE TABLE inscripciones (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  id_alumno    INTEGER NOT NULL REFERENCES alumnos(id_usuario) ON DELETE CASCADE,
  id_actividad INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
  fecha        TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
  estado       TEXT NOT NULL DEFAULT 'activa' CHECK (estado IN ('activa', 'baja')),
  UNIQUE (id_alumno, id_actividad)
);

CREATE TABLE sesiones (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  id_actividad INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
  fecha        TEXT NOT NULL,
  UNIQUE (id_actividad, fecha)
);

CREATE TABLE asistencias (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  id_sesion INTEGER NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE,
  id_alumno INTEGER NOT NULL REFERENCES alumnos(id_usuario) ON DELETE CASCADE,
  presente  INTEGER NOT NULL DEFAULT 0 CHECK (presente IN (0, 1)),
  UNIQUE (id_sesion, id_alumno)
);

-- Evidencias y constancias

-- Una evidencia corresponde a un dia de una actividad
-- Puede tener varias fotos y un estado
CREATE TABLE evidencias (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  id_actividad   INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
  id_docente     INTEGER REFERENCES docentes(id_usuario) ON DELETE SET NULL,
  fecha          TEXT NOT NULL,
  estado         TEXT NOT NULL DEFAULT 'realizada'
                 CHECK (estado IN ('realizada', 'suspendida', 'no_realizada')),
  descripcion    TEXT,
  creada_en      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
  actualizada_en TEXT
);

CREATE INDEX idx_evidencias_actividad_fecha ON evidencias (id_actividad, fecha);

-- Cada fila es una foto de la evidencia
CREATE TABLE evidencia_fotos (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  id_evidencia INTEGER NOT NULL REFERENCES evidencias(id) ON DELETE CASCADE,
  ruta_foto    TEXT NOT NULL,
  orden        INTEGER NOT NULL DEFAULT 0
);

-- El porcentaje se calcula al consultar
CREATE TABLE constancias (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  id_inscripcion INTEGER NOT NULL UNIQUE REFERENCES inscripciones(id) ON DELETE CASCADE,
  folio          TEXT NOT NULL UNIQUE,
  emitida_en     TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Vistas de datos calculados

CREATE VIEW v_periodos AS
SELECT id, inicio, fin, activo,
       (CASE CAST(strftime('%m', inicio) AS INTEGER)
          WHEN 1 THEN 'Ene' WHEN 2 THEN 'Feb' WHEN 3 THEN 'Mar' WHEN 4 THEN 'Abr'
          WHEN 5 THEN 'May' WHEN 6 THEN 'Jun' WHEN 7 THEN 'Jul' WHEN 8 THEN 'Ago'
          WHEN 9 THEN 'Sep' WHEN 10 THEN 'Oct' WHEN 11 THEN 'Nov' ELSE 'Dic' END)
       || '/' ||
       (CASE CAST(strftime('%m', fin) AS INTEGER)
          WHEN 1 THEN 'Ene' WHEN 2 THEN 'Feb' WHEN 3 THEN 'Mar' WHEN 4 THEN 'Abr'
          WHEN 5 THEN 'May' WHEN 6 THEN 'Jun' WHEN 7 THEN 'Jul' WHEN 8 THEN 'Ago'
          WHEN 9 THEN 'Sep' WHEN 10 THEN 'Oct' WHEN 11 THEN 'Nov' ELSE 'Dic' END)
       || ' ' || strftime('%Y', inicio) AS nombre
FROM periodos;

-- El horario se arma en Kotlin
CREATE VIEW v_actividades AS
SELECT a.id, a.nombre, a.descripcion, a.cupo, a.activa,
       p.nombre AS periodo,
       TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
            COALESCE(u.apellido_materno, '')) AS docente,
       a.id_docente,
       (SELECT COUNT(*) FROM inscripciones i
         WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos
FROM actividades a
JOIN v_periodos p ON p.id = a.id_periodo
LEFT JOIN usuarios u ON u.id = a.id_docente;

CREATE VIEW v_asistencia_alumno AS
SELECT i.id AS id_inscripcion, i.id_alumno, i.id_actividad,
       COUNT(s.id) AS sesiones,
       COALESCE(SUM(asi.presente), 0) AS asistencias,
       ROUND(100.0 * COALESCE(SUM(asi.presente), 0) / NULLIF(COUNT(s.id), 0), 2) AS porcentaje
FROM inscripciones i
LEFT JOIN sesiones s      ON s.id_actividad = i.id_actividad
LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
WHERE i.estado = 'activa'
GROUP BY i.id, i.id_alumno, i.id_actividad;

-- Reglas de negocio

-- Bloquea inscripciones con cupo lleno
CREATE TRIGGER trg_inscripcion_cupo BEFORE INSERT ON inscripciones
FOR EACH ROW
BEGIN
  SELECT CASE WHEN
    (SELECT COUNT(*) FROM inscripciones
      WHERE id_actividad = NEW.id_actividad AND estado = 'activa')
    >= (SELECT cupo FROM actividades WHERE id = NEW.id_actividad)
  THEN RAISE(ABORT, 'Cupo lleno para esta actividad') END;
END;

-- Un alumno solo puede tener una actividad activa
CREATE TRIGGER trg_inscripcion_unica_alta BEFORE INSERT ON inscripciones
FOR EACH ROW WHEN NEW.estado = 'activa'
BEGIN
  SELECT CASE WHEN
    (SELECT COUNT(*) FROM inscripciones
      WHERE id_alumno = NEW.id_alumno AND estado = 'activa') > 0
  THEN RAISE(ABORT, 'El alumno ya tiene una actividad inscrita') END;
END;

CREATE TRIGGER trg_inscripcion_unica_cambio BEFORE UPDATE ON inscripciones
FOR EACH ROW WHEN NEW.estado = 'activa'
BEGIN
  SELECT CASE WHEN
    (SELECT COUNT(*) FROM inscripciones
      WHERE id_alumno = NEW.id_alumno AND estado = 'activa' AND id <> NEW.id) > 0
  THEN RAISE(ABORT, 'El alumno ya tiene una actividad inscrita') END;
END;

-- Los datos de prueba se insertan desde la app
-- Las contrasenas se guardan con PBKDF2 y una sal por cuenta
