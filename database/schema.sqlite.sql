-- esquema local de sqlite version 5
-- copia de BaseDatos_Esquema.kt
-- la app crea esto al abrirse
PRAGMA foreign_keys = ON;

CREATE TABLE carreras (
  id     INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL UNIQUE CHECK (length(nombre) BETWEEN 3 AND 80)
);

CREATE TABLE periodos (
  id     INTEGER PRIMARY KEY AUTOINCREMENT,
  inicio TEXT NOT NULL CHECK (inicio GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
  fin    TEXT NOT NULL CHECK (fin GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
  activo INTEGER NOT NULL DEFAULT 0 CHECK (activo IN (0, 1)),
  CHECK (fin > inicio)
);

CREATE UNIQUE INDEX idx_periodo_activo ON periodos (activo) WHERE activo = 1;

CREATE TABLE dias_semana (
  id     INTEGER PRIMARY KEY,
  nombre TEXT NOT NULL UNIQUE CHECK (length(nombre) BETWEEN 5 AND 15),
  orden  INTEGER NOT NULL UNIQUE CHECK (orden BETWEEN 1 AND 7)
);

CREATE TABLE lugares (
  id     INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL UNIQUE COLLATE NOCASE CHECK (length(nombre) BETWEEN 2 AND 60)
);

CREATE TABLE especialidades (
  id     INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre TEXT NOT NULL UNIQUE COLLATE NOCASE CHECK (length(nombre) BETWEEN 2 AND 50)
);

CREATE TABLE usuarios (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  nombres          TEXT NOT NULL CHECK (length(nombres) BETWEEN 2 AND 50),
  apellido_paterno TEXT NOT NULL CHECK (length(apellido_paterno) BETWEEN 2 AND 40),
  apellido_materno TEXT CHECK (apellido_materno IS NULL OR length(apellido_materno) BETWEEN 2 AND 40),
  correo           TEXT NOT NULL UNIQUE COLLATE NOCASE
                   CHECK (length(correo) <= 100 AND correo LIKE '_%@itsm.edu.mx'),
  contrasena_hash  TEXT NOT NULL CHECK (length(contrasena_hash) <= 200),
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
  semestre   INTEGER NOT NULL CHECK (semestre BETWEEN 1 AND 8)
);

CREATE TABLE docentes (
  id_usuario      INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
  id_especialidad INTEGER REFERENCES especialidades(id),
  telefono        TEXT CHECK (telefono IS NULL OR
                              (length(telefono) = 10 AND telefono NOT GLOB '*[^0-9]*'))
);

CREATE TABLE actividades (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre      TEXT NOT NULL CHECK (length(nombre) BETWEEN 3 AND 50),
  descripcion TEXT CHECK (descripcion IS NULL OR length(descripcion) <= 200),
  cupo        INTEGER NOT NULL CHECK (cupo BETWEEN 1 AND 100),
  id_docente  INTEGER REFERENCES docentes(id_usuario) ON DELETE SET NULL,
  id_periodo  INTEGER NOT NULL REFERENCES periodos(id),
  activa      INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0, 1)),
  UNIQUE (nombre, id_periodo)
);

CREATE TABLE horarios (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  id_actividad INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
  id_dia       INTEGER NOT NULL REFERENCES dias_semana(id),
  hora_inicio  TEXT NOT NULL CHECK (hora_inicio GLOB '[0-2][0-9]:[0-5][0-9]'),
  hora_fin     TEXT NOT NULL CHECK (hora_fin GLOB '[0-2][0-9]:[0-5][0-9]'),
  id_lugar     INTEGER NOT NULL REFERENCES lugares(id),
  UNIQUE (id_actividad, id_dia, hora_inicio),
  CHECK (hora_fin > hora_inicio)
);

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
  fecha        TEXT NOT NULL CHECK (fecha GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
  UNIQUE (id_actividad, fecha)
);

CREATE TABLE asistencias (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  id_sesion INTEGER NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE,
  id_alumno INTEGER NOT NULL REFERENCES alumnos(id_usuario) ON DELETE CASCADE,
  presente  INTEGER NOT NULL DEFAULT 0 CHECK (presente IN (0, 1)),
  UNIQUE (id_sesion, id_alumno)
);

CREATE TABLE evidencias (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  id_actividad   INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
  id_docente     INTEGER REFERENCES docentes(id_usuario) ON DELETE SET NULL,
  fecha          TEXT NOT NULL CHECK (fecha GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
  estado         TEXT NOT NULL DEFAULT 'realizada'
                 CHECK (estado IN ('realizada', 'suspendida', 'no_realizada')),
  descripcion    TEXT CHECK (descripcion IS NULL OR length(descripcion) <= 300),
  creada_en      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
  actualizada_en TEXT
);

CREATE INDEX idx_evidencias_actividad_fecha ON evidencias (id_actividad, fecha);

CREATE TABLE evidencia_fotos (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  id_evidencia INTEGER NOT NULL REFERENCES evidencias(id) ON DELETE CASCADE,
  archivo      TEXT NOT NULL UNIQUE CHECK (length(archivo) BETWEEN 5 AND 100),
  orden        INTEGER NOT NULL DEFAULT 0 CHECK (orden >= 0)
);

CREATE TABLE constancias (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  id_inscripcion INTEGER NOT NULL UNIQUE REFERENCES inscripciones(id) ON DELETE CASCADE,
  folio          TEXT NOT NULL UNIQUE CHECK (length(folio) BETWEEN 5 AND 20),
  emitida_en     TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

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

CREATE TRIGGER trg_inscripcion_cupo BEFORE INSERT ON inscripciones
FOR EACH ROW
BEGIN
  SELECT CASE WHEN
    (SELECT COUNT(*) FROM inscripciones
      WHERE id_actividad = NEW.id_actividad AND estado = 'activa')
    >= (SELECT cupo FROM actividades WHERE id = NEW.id_actividad)
  THEN RAISE(ABORT, 'Cupo lleno para esta actividad') END;
END;

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

CREATE TRIGGER trg_asistencia_inscrito BEFORE INSERT ON asistencias
FOR EACH ROW
BEGIN
  SELECT CASE WHEN NOT EXISTS
    (SELECT 1 FROM inscripciones i JOIN sesiones s ON s.id_actividad = i.id_actividad
      WHERE s.id = NEW.id_sesion AND i.id_alumno = NEW.id_alumno)
  THEN RAISE(ABORT, 'El alumno no esta inscrito en la actividad') END;
END;

CREATE TRIGGER trg_alumno_rol BEFORE INSERT ON alumnos
FOR EACH ROW
BEGIN
  SELECT CASE WHEN (SELECT rol FROM usuarios WHERE id = NEW.id_usuario) <> 'alumno'
  THEN RAISE(ABORT, 'El usuario no tiene rol de alumno') END;
  SELECT CASE WHEN (SELECT lower(correo) FROM usuarios WHERE id = NEW.id_usuario) <> lower(NEW.matricula) || '@itsm.edu.mx'
  THEN RAISE(ABORT, 'El correo no coincide con el numero de control') END;
END;

CREATE TRIGGER trg_alumno_correo BEFORE UPDATE ON alumnos
FOR EACH ROW
BEGIN
  SELECT CASE WHEN (SELECT lower(correo) FROM usuarios WHERE id = NEW.id_usuario) <> lower(NEW.matricula) || '@itsm.edu.mx'
  THEN RAISE(ABORT, 'El correo no coincide con el numero de control') END;
END;

CREATE TRIGGER trg_docente_rol BEFORE INSERT ON docentes
FOR EACH ROW
BEGIN
  SELECT CASE WHEN (SELECT rol FROM usuarios WHERE id = NEW.id_usuario) <> 'docente'
  THEN RAISE(ABORT, 'El usuario no tiene rol de docente') END;
END;

CREATE TRIGGER trg_fotos_maximo BEFORE INSERT ON evidencia_fotos
FOR EACH ROW
BEGIN
  SELECT CASE WHEN
    (SELECT COUNT(*) FROM evidencia_fotos WHERE id_evidencia = NEW.id_evidencia) >= 10
  THEN RAISE(ABORT, 'Maximo 10 fotografias por evidencia') END;
END;
