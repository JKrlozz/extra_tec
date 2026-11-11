package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

// tablas de la base en 3fn
// los check siguen a limites
internal val ESQUEMA = listOf(
    """
    CREATE TABLE carreras (
      id     INTEGER PRIMARY KEY AUTOINCREMENT,
      nombre TEXT NOT NULL UNIQUE CHECK (length(nombre) BETWEEN 3 AND 80)
    )
    """,
    """
    CREATE TABLE periodos (
      id     INTEGER PRIMARY KEY AUTOINCREMENT,
      inicio TEXT NOT NULL CHECK (inicio GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
      fin    TEXT NOT NULL CHECK (fin GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
      activo INTEGER NOT NULL DEFAULT 0 CHECK (activo IN (0, 1)),
      CHECK (fin > inicio)
    )
    """,
    // solo puede haber un periodo activo
    "CREATE UNIQUE INDEX idx_periodo_activo ON periodos (activo) WHERE activo = 1",
    """
    CREATE TABLE dias_semana (
      id     INTEGER PRIMARY KEY,
      nombre TEXT NOT NULL UNIQUE CHECK (length(nombre) BETWEEN 5 AND 15),
      orden  INTEGER NOT NULL UNIQUE CHECK (orden BETWEEN 1 AND 7)
    )
    """,
    """
    CREATE TABLE lugares (
      id     INTEGER PRIMARY KEY AUTOINCREMENT,
      nombre TEXT NOT NULL UNIQUE COLLATE NOCASE CHECK (length(nombre) BETWEEN 2 AND 60)
    )
    """,
    """
    CREATE TABLE especialidades (
      id     INTEGER PRIMARY KEY AUTOINCREMENT,
      nombre TEXT NOT NULL UNIQUE COLLATE NOCASE CHECK (length(nombre) BETWEEN 2 AND 50)
    )
    """,
    """
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
    )
    """,
    "CREATE INDEX idx_usuarios_nombre ON usuarios (apellido_paterno, apellido_materno, nombres)",
    """
    CREATE TABLE alumnos (
      id_usuario INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
      matricula  TEXT NOT NULL UNIQUE
                 CHECK (matricula GLOB '[0-9][0-9][0-9]T[0-9][0-9][0-9][0-9]'),
      id_carrera INTEGER NOT NULL REFERENCES carreras(id),
      semestre   INTEGER NOT NULL CHECK (semestre BETWEEN 1 AND 8)
    )
    """,
    """
    CREATE TABLE docentes (
      id_usuario      INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
      id_especialidad INTEGER REFERENCES especialidades(id),
      telefono        TEXT CHECK (telefono IS NULL OR
                                  (length(telefono) = 10 AND telefono NOT GLOB '*[^0-9]*'))
    )
    """,
    """
    CREATE TABLE actividades (
      id          INTEGER PRIMARY KEY AUTOINCREMENT,
      nombre      TEXT NOT NULL CHECK (length(nombre) BETWEEN 3 AND 50),
      descripcion TEXT CHECK (descripcion IS NULL OR length(descripcion) <= 200),
      cupo        INTEGER NOT NULL CHECK (cupo BETWEEN 1 AND 100),
      id_docente  INTEGER REFERENCES docentes(id_usuario) ON DELETE SET NULL,
      id_periodo  INTEGER NOT NULL REFERENCES periodos(id),
      activa      INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0, 1)),
      UNIQUE (nombre, id_periodo)
    )
    """,
    """
    CREATE TABLE horarios (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      id_actividad INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
      id_dia       INTEGER NOT NULL REFERENCES dias_semana(id),
      hora_inicio  TEXT NOT NULL CHECK (hora_inicio GLOB '[0-2][0-9]:[0-5][0-9]'),
      hora_fin     TEXT NOT NULL CHECK (hora_fin GLOB '[0-2][0-9]:[0-5][0-9]'),
      id_lugar     INTEGER NOT NULL REFERENCES lugares(id),
      UNIQUE (id_actividad, id_dia, hora_inicio),
      CHECK (hora_fin > hora_inicio)
    )
    """,
    """
    CREATE TABLE inscripciones (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      id_alumno    INTEGER NOT NULL REFERENCES alumnos(id_usuario) ON DELETE CASCADE,
      id_actividad INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
      fecha        TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
      estado       TEXT NOT NULL DEFAULT 'activa' CHECK (estado IN ('activa', 'baja')),
      UNIQUE (id_alumno, id_actividad)
    )
    """,
    """
    CREATE TABLE sesiones (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      id_actividad INTEGER NOT NULL REFERENCES actividades(id) ON DELETE CASCADE,
      fecha        TEXT NOT NULL CHECK (fecha GLOB '[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]'),
      UNIQUE (id_actividad, fecha)
    )
    """,
    """
    CREATE TABLE asistencias (
      id        INTEGER PRIMARY KEY AUTOINCREMENT,
      id_sesion INTEGER NOT NULL REFERENCES sesiones(id) ON DELETE CASCADE,
      id_alumno INTEGER NOT NULL REFERENCES alumnos(id_usuario) ON DELETE CASCADE,
      presente  INTEGER NOT NULL DEFAULT 0 CHECK (presente IN (0, 1)),
      UNIQUE (id_sesion, id_alumno)
    )
    """,
    // una evidencia es un dia
    """
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
    )
    """,
    "CREATE INDEX idx_evidencias_actividad_fecha ON evidencias (id_actividad, fecha)",
    // solo se guarda el archivo
    """
    CREATE TABLE evidencia_fotos (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      id_evidencia INTEGER NOT NULL REFERENCES evidencias(id) ON DELETE CASCADE,
      archivo      TEXT NOT NULL UNIQUE CHECK (length(archivo) BETWEEN 5 AND 100),
      orden        INTEGER NOT NULL DEFAULT 0 CHECK (orden >= 0)
    )
    """,
    """
    CREATE TABLE constancias (
      id             INTEGER PRIMARY KEY AUTOINCREMENT,
      id_inscripcion INTEGER NOT NULL UNIQUE REFERENCES inscripciones(id) ON DELETE CASCADE,
      folio          TEXT NOT NULL UNIQUE CHECK (length(folio) BETWEEN 5 AND 20),
      emitida_en     TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
    )
    """,

    // nombre del periodo con fechas
    """
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
    FROM periodos
    """,

    // actividad con docente periodo e inscritos
    """
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
    LEFT JOIN usuarios u ON u.id = a.id_docente
    """,

    // porcentaje de asistencia de cada alumno
    """
    CREATE VIEW v_asistencia_alumno AS
    SELECT i.id AS id_inscripcion, i.id_alumno, i.id_actividad,
           COUNT(s.id) AS sesiones,
           COALESCE(SUM(asi.presente), 0) AS asistencias,
           ROUND(100.0 * COALESCE(SUM(asi.presente), 0) / NULLIF(COUNT(s.id), 0), 2) AS porcentaje
    FROM inscripciones i
    LEFT JOIN sesiones s      ON s.id_actividad = i.id_actividad
    LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
    WHERE i.estado = 'activa'
    GROUP BY i.id, i.id_alumno, i.id_actividad
    """,

    // no deja inscribir con cupo lleno
    """
    CREATE TRIGGER trg_inscripcion_cupo BEFORE INSERT ON inscripciones
    FOR EACH ROW
    BEGIN
      SELECT CASE WHEN
        (SELECT COUNT(*) FROM inscripciones
          WHERE id_actividad = NEW.id_actividad AND estado = 'activa')
        >= (SELECT cupo FROM actividades WHERE id = NEW.id_actividad)
      THEN RAISE(ABORT, 'Cupo lleno para esta actividad') END;
    END
    """,

    // una actividad activa por alumno
    """
    CREATE TRIGGER trg_inscripcion_unica_alta BEFORE INSERT ON inscripciones
    FOR EACH ROW WHEN NEW.estado = 'activa'
    BEGIN
      SELECT CASE WHEN
        (SELECT COUNT(*) FROM inscripciones
          WHERE id_alumno = NEW.id_alumno AND estado = 'activa') > 0
      THEN RAISE(ABORT, 'El alumno ya tiene una actividad inscrita') END;
    END
    """,
    """
    CREATE TRIGGER trg_inscripcion_unica_cambio BEFORE UPDATE ON inscripciones
    FOR EACH ROW WHEN NEW.estado = 'activa'
    BEGIN
      SELECT CASE WHEN
        (SELECT COUNT(*) FROM inscripciones
          WHERE id_alumno = NEW.id_alumno AND estado = 'activa' AND id <> NEW.id) > 0
      THEN RAISE(ABORT, 'El alumno ya tiene una actividad inscrita') END;
    END
    """,

    // asistencia solo de alumnos inscritos
    """
    CREATE TRIGGER trg_asistencia_inscrito BEFORE INSERT ON asistencias
    FOR EACH ROW
    BEGIN
      SELECT CASE WHEN NOT EXISTS
        (SELECT 1 FROM inscripciones i JOIN sesiones s ON s.id_actividad = i.id_actividad
          WHERE s.id = NEW.id_sesion AND i.id_alumno = NEW.id_alumno)
      THEN RAISE(ABORT, 'El alumno no esta inscrito en la actividad') END;
    END
    """,

    // el rol debe coincidir
    """
    CREATE TRIGGER trg_alumno_rol BEFORE INSERT ON alumnos
    FOR EACH ROW
    BEGIN
      SELECT CASE WHEN (SELECT rol FROM usuarios WHERE id = NEW.id_usuario) <> 'alumno'
      THEN RAISE(ABORT, 'El usuario no tiene rol de alumno') END;
      SELECT CASE WHEN (SELECT lower(correo) FROM usuarios WHERE id = NEW.id_usuario) <> lower(NEW.matricula) || '@itsm.edu.mx'
      THEN RAISE(ABORT, 'El correo no coincide con el numero de control') END;
    END
    """,
    // al editar tambien se revisa el correo
    """
    CREATE TRIGGER trg_alumno_correo BEFORE UPDATE ON alumnos
    FOR EACH ROW
    BEGIN
      SELECT CASE WHEN (SELECT lower(correo) FROM usuarios WHERE id = NEW.id_usuario) <> lower(NEW.matricula) || '@itsm.edu.mx'
      THEN RAISE(ABORT, 'El correo no coincide con el numero de control') END;
    END
    """,
    """
    CREATE TRIGGER trg_docente_rol BEFORE INSERT ON docentes
    FOR EACH ROW
    BEGIN
      SELECT CASE WHEN (SELECT rol FROM usuarios WHERE id = NEW.id_usuario) <> 'docente'
      THEN RAISE(ABORT, 'El usuario no tiene rol de docente') END;
    END
    """,

    // maximo diez fotos por evidencia
    """
    CREATE TRIGGER trg_fotos_maximo BEFORE INSERT ON evidencia_fotos
    FOR EACH ROW
    BEGIN
      SELECT CASE WHEN
        (SELECT COUNT(*) FROM evidencia_fotos WHERE id_evidencia = NEW.id_evidencia) >= 10
      THEN RAISE(ABORT, 'Maximo 10 fotografias por evidencia') END;
    END
    """,
)

// se borran en orden inverso
internal val VISTAS = listOf("v_asistencia_alumno", "v_actividades", "v_periodos")

internal val TABLAS = listOf(
    "constancias", "asistencias", "sesiones", "inscripciones", "evidencia_fotos",
    "evidencias", "horarios", "actividades", "alumnos", "docentes", "usuarios",
    "especialidades", "lugares", "dias_semana", "periodos", "carreras",
)

// datos de prueba
// todas usan la misma contrasena
internal object Sembrado {
    private const val CLAVE_DEMO = "itsm1234"

    fun crear(db: SQLiteDatabase) {
        val carreras = listOf(
            "Ingeniería en Sistemas Computacionales",
            "Ingeniería Industrial",
            "Ingeniería en Gestión Empresarial",
            "Ingeniería Bioquímica",
            "Ingeniería Electromecánica",
            "Ingeniería Civil",
            "Ingeniería en Tecnologías de la Información y Comunicaciones",
            "Ingeniería Ambiental",
            "Ingeniería Petrolera",
            "Licenciatura en Gastronomía",
        )
        for (nombre in carreras) {
            insertarNombre(db, "carreras", nombre)
        }

        db.execSQL("INSERT INTO periodos (inicio, fin, activo) VALUES ('2026-08-17', '2026-12-18', 1)")

        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        for (i in dias.indices) {
            val valores = ContentValues()
            valores.put("id", i + 1)
            valores.put("nombre", dias[i])
            valores.put("orden", i + 1)
            db.insert("dias_semana", null, valores)
        }

        val lugares = listOf(
            "ITSM", "Cancha de fútbol", "Cancha de básquetbol",
            "Gimnasio", "Salón de usos múltiples", "Patio central",
        )
        for (nombre in lugares) {
            insertarNombre(db, "lugares", nombre)
        }

        val especialidades = listOf("Deportes", "Ajedrez", "Música")
        for (nombre in especialidades) {
            insertarNombre(db, "especialidades", nombre)
        }

        val hash = Clave.generar(CLAVE_DEMO)
        insertarUsuario(db, "Administrador", "ITSM", null, "admin@itsm.edu.mx", "admin", hash)
        insertarUsuario(db, "Carlos", "Pérez", "Domínguez", "cperez@itsm.edu.mx", "docente", hash)
        insertarUsuario(db, "Ana", "López", "Hernández", "alopez@itsm.edu.mx", "docente", hash)
        insertarUsuario(db, "Luis", "Gómez", "Ramos", "lgomez@itsm.edu.mx", "docente", hash)
        insertarUsuario(db, "María", "Hernández", "Cruz", "232t0182@itsm.edu.mx", "alumno", hash)
        insertarUsuario(db, "José", "Martínez", "Luna", "219t0382@itsm.edu.mx", "alumno", hash)
        insertarUsuario(db, "Diana", "Sánchez", "Rojas", "232t0183@itsm.edu.mx", "alumno", hash)
        insertarUsuario(db, "Pedro", "García", "Mendoza", "219t0383@itsm.edu.mx", "alumno", hash)

        insertarDocente(db, "cperez@itsm.edu.mx", "Deportes", "2351000001")
        insertarDocente(db, "alopez@itsm.edu.mx", "Ajedrez", "2351000002")
        insertarDocente(db, "lgomez@itsm.edu.mx", "Música", "2351000003")

        insertarAlumno(db, "232t0182@itsm.edu.mx", "232T0182")
        insertarAlumno(db, "219t0382@itsm.edu.mx", "219T0382")
        insertarAlumno(db, "232t0183@itsm.edu.mx", "232T0183")
        insertarAlumno(db, "219t0383@itsm.edu.mx", "219T0383")

        insertarActividad(db, "Fútbol", 25, "cperez@itsm.edu.mx")
        insertarActividad(db, "Ajedrez", 20, "alopez@itsm.edu.mx")
        insertarActividad(db, "Voleibol", 25, "lgomez@itsm.edu.mx")
        insertarActividad(db, "Baloncesto", 25, "cperez@itsm.edu.mx")
        insertarActividad(db, "Banda de guerra", 30, "lgomez@itsm.edu.mx")

        insertarHorario(db, "Fútbol", "Lunes", "16:00", "18:00", "Cancha de fútbol")
        insertarHorario(db, "Fútbol", "Miércoles", "16:00", "18:00", "Cancha de fútbol")
        insertarHorario(db, "Ajedrez", "Martes", "15:00", "17:00", "Salón de usos múltiples")
        insertarHorario(db, "Voleibol", "Jueves", "17:00", "19:00", "Gimnasio")
        insertarHorario(db, "Baloncesto", "Viernes", "17:00", "19:00", "Cancha de básquetbol")
        insertarHorario(db, "Banda de guerra", "Lunes", "18:00", "20:00", "Patio central")

        // todos los alumnos van a futbol
        val futbol = idDe(db, "actividades", "nombre", "Fútbol")
        db.execSQL(
            "INSERT INTO inscripciones (id_alumno, id_actividad) SELECT id_usuario, ? FROM alumnos",
            arrayOf(futbol),
        )

        // pedro falta a la segunda
        val fechas = listOf("2026-09-07", "2026-09-09")
        for (fecha in fechas) {
            val valores = ContentValues()
            valores.put("id_actividad", futbol)
            valores.put("fecha", fecha)
            val sesion = db.insert("sesiones", null, valores)

            val cursor = db.rawQuery("SELECT id_usuario, matricula FROM alumnos", null)
            while (cursor.moveToNext()) {
                var presente = 1
                if (cursor.getString(1) == "219T0383" && fecha == "2026-09-09") {
                    presente = 0
                }
                val asistencia = ContentValues()
                asistencia.put("id_sesion", sesion)
                asistencia.put("id_alumno", cursor.getInt(0))
                asistencia.put("presente", presente)
                db.insert("asistencias", null, asistencia)
            }
            cursor.close()
        }
    }

    private fun insertarNombre(db: SQLiteDatabase, tabla: String, nombre: String) {
        val valores = ContentValues()
        valores.put("nombre", nombre)
        db.insert(tabla, null, valores)
    }

    private fun insertarUsuario(
        db: SQLiteDatabase,
        nombres: String,
        paterno: String,
        materno: String?,
        correo: String,
        rol: String,
        hash: String,
    ) {
        val valores = ContentValues()
        valores.put("nombres", nombres)
        valores.put("apellido_paterno", paterno)
        valores.put("apellido_materno", materno)
        valores.put("correo", correo)
        valores.put("contrasena_hash", hash)
        valores.put("rol", rol)
        db.insert("usuarios", null, valores)
    }

    private fun insertarDocente(db: SQLiteDatabase, correo: String, especialidad: String, telefono: String) {
        val valores = ContentValues()
        valores.put("id_usuario", idDe(db, "usuarios", "correo", correo))
        valores.put("id_especialidad", idDe(db, "especialidades", "nombre", especialidad))
        valores.put("telefono", telefono)
        db.insert("docentes", null, valores)
    }

    private fun insertarAlumno(db: SQLiteDatabase, correo: String, matricula: String) {
        val valores = ContentValues()
        valores.put("id_usuario", idDe(db, "usuarios", "correo", correo))
        valores.put("matricula", matricula)
        valores.put("id_carrera", 1)
        valores.put("semestre", 7)
        db.insert("alumnos", null, valores)
    }

    private fun insertarActividad(db: SQLiteDatabase, nombre: String, cupo: Int, correoDocente: String) {
        val valores = ContentValues()
        valores.put("nombre", nombre)
        valores.put("cupo", cupo)
        valores.put("id_docente", idDe(db, "usuarios", "correo", correoDocente))
        valores.put("id_periodo", 1)
        db.insert("actividades", null, valores)
    }

    private fun insertarHorario(
        db: SQLiteDatabase,
        actividad: String,
        dia: String,
        inicio: String,
        fin: String,
        lugar: String,
    ) {
        val valores = ContentValues()
        valores.put("id_actividad", idDe(db, "actividades", "nombre", actividad))
        valores.put("id_dia", idDe(db, "dias_semana", "nombre", dia))
        valores.put("hora_inicio", inicio)
        valores.put("hora_fin", fin)
        valores.put("id_lugar", idDe(db, "lugares", "nombre", lugar))
        db.insert("horarios", null, valores)
    }

    private fun idDe(db: SQLiteDatabase, tabla: String, columna: String, valor: String): Int {
        val cursor = db.rawQuery("SELECT id FROM $tabla WHERE $columna = ?", arrayOf(valor))
        var id = 0
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        return id
    }
}
