-- Esquema de ITSM Extraescolares para Azure SQL (T-SQL)
-- Lo ejecuta api/scripts/crear_bd.js. Borra y crea todo de nuevo.
-- Los lotes se separan con una linea que solo dice GO.

IF OBJECT_ID('v_asistencia_alumno', 'V') IS NOT NULL DROP VIEW v_asistencia_alumno;
GO
IF OBJECT_ID('v_actividades', 'V') IS NOT NULL DROP VIEW v_actividades;
GO
IF OBJECT_ID('v_periodos', 'V') IS NOT NULL DROP VIEW v_periodos;
GO
DROP TABLE IF EXISTS constancias;
DROP TABLE IF EXISTS asistencias;
DROP TABLE IF EXISTS sesiones;
DROP TABLE IF EXISTS inscripciones;
DROP TABLE IF EXISTS evidencia_fotos;
DROP TABLE IF EXISTS evidencias;
DROP TABLE IF EXISTS horarios;
DROP TABLE IF EXISTS actividades;
DROP TABLE IF EXISTS alumnos;
DROP TABLE IF EXISTS docentes;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS especialidades;
DROP TABLE IF EXISTS lugares;
DROP TABLE IF EXISTS dias_semana;
DROP TABLE IF EXISTS periodos;
DROP TABLE IF EXISTS carreras;
GO

CREATE TABLE carreras (
  id     INT IDENTITY(1,1) CONSTRAINT pk_carreras PRIMARY KEY,
  nombre NVARCHAR(80) NOT NULL CONSTRAINT uq_carreras_nombre UNIQUE
         CONSTRAINT ck_carreras_nombre CHECK (LEN(nombre) BETWEEN 3 AND 80)
);

CREATE TABLE periodos (
  id     INT IDENTITY(1,1) CONSTRAINT pk_periodos PRIMARY KEY,
  inicio DATE NOT NULL,
  fin    DATE NOT NULL,
  activo TINYINT NOT NULL CONSTRAINT df_periodos_activo DEFAULT 0
         CONSTRAINT ck_periodos_activo CHECK (activo IN (0, 1)),
  -- cuando se cierra el semestre sus registros quedan archivados
  cerrado_en DATETIME2(0) NULL,
  CONSTRAINT ck_periodos_fechas CHECK (fin > inicio)
);
-- solo puede haber un periodo activo
CREATE UNIQUE INDEX ux_periodo_activo ON periodos (activo) WHERE activo = 1;

CREATE TABLE dias_semana (
  id     INT CONSTRAINT pk_dias PRIMARY KEY,
  nombre NVARCHAR(15) NOT NULL CONSTRAINT uq_dias_nombre UNIQUE,
  orden  INT NOT NULL CONSTRAINT uq_dias_orden UNIQUE
         CONSTRAINT ck_dias_orden CHECK (orden BETWEEN 1 AND 7)
);

CREATE TABLE lugares (
  id     INT IDENTITY(1,1) CONSTRAINT pk_lugares PRIMARY KEY,
  nombre NVARCHAR(60) NOT NULL CONSTRAINT uq_lugares_nombre UNIQUE
         CONSTRAINT ck_lugares_nombre CHECK (LEN(nombre) BETWEEN 2 AND 60)
);

CREATE TABLE especialidades (
  id     INT IDENTITY(1,1) CONSTRAINT pk_especialidades PRIMARY KEY,
  nombre NVARCHAR(50) NOT NULL CONSTRAINT uq_especialidades_nombre UNIQUE
         CONSTRAINT ck_especialidades_nombre CHECK (LEN(nombre) BETWEEN 2 AND 50)
);

CREATE TABLE usuarios (
  id               INT IDENTITY(1,1) CONSTRAINT pk_usuarios PRIMARY KEY,
  nombres          NVARCHAR(50) NOT NULL CONSTRAINT ck_usuarios_nombres CHECK (LEN(nombres) BETWEEN 2 AND 50),
  apellido_paterno NVARCHAR(40) NOT NULL CONSTRAINT ck_usuarios_paterno CHECK (LEN(apellido_paterno) BETWEEN 2 AND 40),
  apellido_materno NVARCHAR(40) NULL CONSTRAINT ck_usuarios_materno CHECK (apellido_materno IS NULL OR LEN(apellido_materno) BETWEEN 2 AND 40),
  correo           NVARCHAR(100) NOT NULL CONSTRAINT uq_usuarios_correo UNIQUE
                   CONSTRAINT ck_usuarios_correo CHECK (correo LIKE '_%@itsm.edu.mx'),
  contrasena_hash  VARCHAR(200) NOT NULL,
  rol              VARCHAR(10) NOT NULL CONSTRAINT ck_usuarios_rol CHECK (rol IN ('alumno', 'docente', 'admin')),
  activo           TINYINT NOT NULL CONSTRAINT df_usuarios_activo DEFAULT 1
                   CONSTRAINT ck_usuarios_activo CHECK (activo IN (0, 1)),
  creado_en        DATETIME2(0) NOT NULL CONSTRAINT df_usuarios_creado
                   DEFAULT (CONVERT(DATETIME2(0), SYSDATETIMEOFFSET() AT TIME ZONE 'Central Standard Time (Mexico)'))
);
CREATE INDEX idx_usuarios_nombre ON usuarios (apellido_paterno, apellido_materno, nombres);

CREATE TABLE alumnos (
  id_usuario INT CONSTRAINT pk_alumnos PRIMARY KEY
             CONSTRAINT fk_alumnos_usuario REFERENCES usuarios(id) ON DELETE CASCADE,
  matricula  VARCHAR(8) NOT NULL CONSTRAINT uq_alumnos_matricula UNIQUE
             CONSTRAINT ck_alumnos_matricula CHECK (matricula LIKE '[0-9][0-9][0-9]T[0-9][0-9][0-9][0-9]'),
  id_carrera INT NOT NULL CONSTRAINT fk_alumnos_carrera REFERENCES carreras(id),
  semestre   INT NOT NULL CONSTRAINT ck_alumnos_semestre CHECK (semestre BETWEEN 1 AND 8)
);

CREATE TABLE docentes (
  id_usuario      INT CONSTRAINT pk_docentes PRIMARY KEY
                  CONSTRAINT fk_docentes_usuario REFERENCES usuarios(id) ON DELETE CASCADE,
  id_especialidad INT NULL CONSTRAINT fk_docentes_especialidad REFERENCES especialidades(id),
  telefono        VARCHAR(10) NULL CONSTRAINT ck_docentes_telefono
                  CHECK (telefono IS NULL OR (LEN(telefono) = 10 AND telefono NOT LIKE '%[^0-9]%'))
);

CREATE TABLE actividades (
  id          INT IDENTITY(1,1) CONSTRAINT pk_actividades PRIMARY KEY,
  nombre      NVARCHAR(50) NOT NULL CONSTRAINT ck_actividades_nombre CHECK (LEN(nombre) BETWEEN 3 AND 50),
  descripcion NVARCHAR(200) NULL,
  cupo        INT NOT NULL CONSTRAINT ck_actividades_cupo CHECK (cupo BETWEEN 1 AND 100),
  id_docente  INT NULL CONSTRAINT fk_actividades_docente REFERENCES docentes(id_usuario) ON DELETE SET NULL,
  id_periodo  INT NOT NULL CONSTRAINT fk_actividades_periodo REFERENCES periodos(id),
  activa      TINYINT NOT NULL CONSTRAINT df_actividades_activa DEFAULT 1
              CONSTRAINT ck_actividades_activa CHECK (activa IN (0, 1)),
  -- fecha en que se archivo al cerrar el semestre
  archivada_en DATETIME2(0) NULL,
  CONSTRAINT uq_actividades_nombre UNIQUE (nombre, id_periodo)
);

CREATE TABLE horarios (
  id           INT IDENTITY(1,1) CONSTRAINT pk_horarios PRIMARY KEY,
  id_actividad INT NOT NULL CONSTRAINT fk_horarios_actividad REFERENCES actividades(id) ON DELETE CASCADE,
  id_dia       INT NOT NULL CONSTRAINT fk_horarios_dia REFERENCES dias_semana(id),
  hora_inicio  CHAR(5) NOT NULL,
  hora_fin     CHAR(5) NOT NULL,
  id_lugar     INT NOT NULL CONSTRAINT fk_horarios_lugar REFERENCES lugares(id),
  CONSTRAINT uq_horarios UNIQUE (id_actividad, id_dia, hora_inicio),
  CONSTRAINT ck_horarios_horas CHECK (hora_fin > hora_inicio)
);

CREATE TABLE inscripciones (
  id           INT IDENTITY(1,1) CONSTRAINT pk_inscripciones PRIMARY KEY,
  id_alumno    INT NOT NULL CONSTRAINT fk_inscripciones_alumno REFERENCES alumnos(id_usuario) ON DELETE CASCADE,
  id_actividad INT NOT NULL CONSTRAINT fk_inscripciones_actividad REFERENCES actividades(id),
  fecha        DATETIME2(0) NOT NULL CONSTRAINT df_inscripciones_fecha
               DEFAULT (CONVERT(DATETIME2(0), SYSDATETIMEOFFSET() AT TIME ZONE 'Central Standard Time (Mexico)')),
  estado       VARCHAR(9) NOT NULL CONSTRAINT df_inscripciones_estado DEFAULT 'activa'
               CONSTRAINT ck_inscripciones_estado CHECK (estado IN ('activa', 'baja', 'archivada')),
  CONSTRAINT uq_inscripciones UNIQUE (id_alumno, id_actividad)
);
-- una actividad activa por alumno
CREATE UNIQUE INDEX ux_inscripcion_activa ON inscripciones (id_alumno) WHERE estado = 'activa';

CREATE TABLE sesiones (
  id           INT IDENTITY(1,1) CONSTRAINT pk_sesiones PRIMARY KEY,
  id_actividad INT NOT NULL CONSTRAINT fk_sesiones_actividad REFERENCES actividades(id),
  fecha        DATE NOT NULL,
  CONSTRAINT uq_sesiones UNIQUE (id_actividad, fecha)
);

CREATE TABLE asistencias (
  id        INT IDENTITY(1,1) CONSTRAINT pk_asistencias PRIMARY KEY,
  id_sesion INT NOT NULL CONSTRAINT fk_asistencias_sesion REFERENCES sesiones(id) ON DELETE CASCADE,
  id_alumno INT NOT NULL CONSTRAINT fk_asistencias_alumno REFERENCES alumnos(id_usuario),
  presente  TINYINT NOT NULL CONSTRAINT df_asistencias_presente DEFAULT 0
            CONSTRAINT ck_asistencias_presente CHECK (presente IN (0, 1)),
  CONSTRAINT uq_asistencias UNIQUE (id_sesion, id_alumno)
);

-- una evidencia es un dia
CREATE TABLE evidencias (
  id             INT IDENTITY(1,1) CONSTRAINT pk_evidencias PRIMARY KEY,
  id_actividad   INT NOT NULL CONSTRAINT fk_evidencias_actividad REFERENCES actividades(id),
  id_docente     INT NULL CONSTRAINT fk_evidencias_docente REFERENCES docentes(id_usuario),
  fecha          DATE NOT NULL,
  estado         VARCHAR(12) NOT NULL CONSTRAINT df_evidencias_estado DEFAULT 'realizada'
                 CONSTRAINT ck_evidencias_estado CHECK (estado IN ('realizada', 'suspendida', 'no_realizada')),
  descripcion    NVARCHAR(300) NULL,
  creada_en      DATETIME2(0) NOT NULL CONSTRAINT df_evidencias_creada
                 DEFAULT (CONVERT(DATETIME2(0), SYSDATETIMEOFFSET() AT TIME ZONE 'Central Standard Time (Mexico)')),
  actualizada_en DATETIME2(0) NULL
);
CREATE INDEX idx_evidencias_actividad_fecha ON evidencias (id_actividad, fecha);

-- la fotografia vive en la base de datos
CREATE TABLE evidencia_fotos (
  id           INT IDENTITY(1,1) CONSTRAINT pk_evidencia_fotos PRIMARY KEY,
  id_evidencia INT NOT NULL CONSTRAINT fk_fotos_evidencia REFERENCES evidencias(id) ON DELETE CASCADE,
  tipo         VARCHAR(20) NOT NULL CONSTRAINT ck_fotos_tipo CHECK (tipo IN ('image/jpeg', 'image/png', 'image/webp')),
  contenido    VARBINARY(MAX) NOT NULL,
  orden        INT NOT NULL CONSTRAINT df_fotos_orden DEFAULT 0 CONSTRAINT ck_fotos_orden CHECK (orden >= 0)
);
CREATE INDEX idx_fotos_evidencia ON evidencia_fotos (id_evidencia, orden);

CREATE TABLE constancias (
  id             INT IDENTITY(1,1) CONSTRAINT pk_constancias PRIMARY KEY,
  id_inscripcion INT NOT NULL CONSTRAINT uq_constancias_inscripcion UNIQUE
                 CONSTRAINT fk_constancias_inscripcion REFERENCES inscripciones(id) ON DELETE CASCADE,
  folio          VARCHAR(20) NOT NULL CONSTRAINT uq_constancias_folio UNIQUE,
  emitida_en     DATETIME2(0) NOT NULL CONSTRAINT df_constancias_emitida
                 DEFAULT (CONVERT(DATETIME2(0), SYSDATETIMEOFFSET() AT TIME ZONE 'Central Standard Time (Mexico)'))
);
GO

-- nombre del periodo con fechas, por ejemplo Ago/Dic 2026
CREATE VIEW v_periodos AS
SELECT id, inicio, fin, activo,
       CHOOSE(MONTH(inicio), 'Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic')
       + '/' +
       CHOOSE(MONTH(fin), 'Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic')
       + ' ' + CAST(YEAR(inicio) AS VARCHAR(4)) AS nombre
FROM periodos;
GO

-- actividad con docente, periodo e inscritos
CREATE VIEW v_actividades AS
SELECT a.id, a.nombre, a.descripcion, a.cupo, a.activa,
       p.nombre AS periodo,
       LTRIM(RTRIM(COALESCE(u.nombres, '') + ' ' + COALESCE(u.apellido_paterno, '') + ' ' +
                   COALESCE(u.apellido_materno, ''))) AS docente,
       a.id_docente,
       (SELECT COUNT(*) FROM inscripciones i
         WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos
FROM actividades a
JOIN v_periodos p ON p.id = a.id_periodo
LEFT JOIN usuarios u ON u.id = a.id_docente;
GO

-- porcentaje de asistencia de cada alumno inscrito
CREATE VIEW v_asistencia_alumno AS
SELECT i.id AS id_inscripcion, i.id_alumno, i.id_actividad,
       COUNT(s.id) AS sesiones,
       COALESCE(SUM(CAST(asi.presente AS INT)), 0) AS asistencias,
       ROUND(100.0 * COALESCE(SUM(CAST(asi.presente AS INT)), 0) / NULLIF(COUNT(s.id), 0), 2) AS porcentaje
FROM inscripciones i
LEFT JOIN sesiones s      ON s.id_actividad = i.id_actividad
LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
-- las archivadas siguen dando derecho a constancia
WHERE i.estado IN ('activa', 'archivada')
GROUP BY i.id, i.id_alumno, i.id_actividad;
GO
