package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

// Una evidencia corresponde a un dia de una actividad
// Puede tener varias fotos y un estado
internal const val DDL_EVIDENCIAS = """
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
    )
"""

internal const val DDL_EVIDENCIA_FOTOS = """
    CREATE TABLE evidencia_fotos (
      id           INTEGER PRIMARY KEY AUTOINCREMENT,
      id_evidencia INTEGER NOT NULL REFERENCES evidencias(id) ON DELETE CASCADE,
      ruta_foto    TEXT NOT NULL,
      orden        INTEGER NOT NULL DEFAULT 0
    )
"""

// Esquema local equivalente al de MySQL
// Se aplica una sola vez al crear la base local
internal val ESQUEMA = listOf(
    """
    CREATE TABLE carreras (
      id     INTEGER PRIMARY KEY AUTOINCREMENT,
      nombre TEXT NOT NULL UNIQUE
    )
    """,
    """
    CREATE TABLE periodos (
      id     INTEGER PRIMARY KEY AUTOINCREMENT,
      inicio TEXT NOT NULL,
      fin    TEXT NOT NULL,
      activo INTEGER NOT NULL DEFAULT 0 CHECK (activo IN (0, 1)),
      CHECK (fin > inicio)
    )
    """,
    """
    CREATE TABLE dias_semana (
      id     INTEGER PRIMARY KEY,
      nombre TEXT NOT NULL UNIQUE,
      orden  INTEGER NOT NULL UNIQUE
    )
    """,
    """
    CREATE TABLE lugares (
      id     INTEGER PRIMARY KEY AUTOINCREMENT,
      nombre TEXT NOT NULL UNIQUE
    )
    """,
    """
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
    )
    """,
    "CREATE INDEX idx_usuarios_nombre ON usuarios (apellido_paterno, apellido_materno, nombres)",
    """
    CREATE TABLE alumnos (
      id_usuario INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
      matricula  TEXT NOT NULL UNIQUE
                 CHECK (matricula GLOB '[0-9][0-9][0-9]T[0-9][0-9][0-9][0-9]'),
      id_carrera INTEGER NOT NULL REFERENCES carreras(id),
      semestre   INTEGER NOT NULL CHECK (semestre BETWEEN 1 AND 9)
    )
    """,
    """
    CREATE TABLE docentes (
      id_usuario   INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
      especialidad TEXT,
      telefono     TEXT
    )
    """,
    """
    CREATE TABLE actividades (
      id          INTEGER PRIMARY KEY AUTOINCREMENT,
      nombre      TEXT NOT NULL,
      descripcion TEXT,
      cupo        INTEGER NOT NULL CHECK (cupo > 0),
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
      hora_inicio  TEXT NOT NULL,
      hora_fin     TEXT NOT NULL,
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
      fecha        TEXT NOT NULL,
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
    DDL_EVIDENCIAS,
    DDL_EVIDENCIA_FOTOS,
    "CREATE INDEX idx_evidencias_actividad_fecha ON evidencias (id_actividad, fecha)",
    """
    CREATE TABLE constancias (
      id             INTEGER PRIMARY KEY AUTOINCREMENT,
      id_inscripcion INTEGER NOT NULL UNIQUE REFERENCES inscripciones(id) ON DELETE CASCADE,
      folio          TEXT NOT NULL UNIQUE,
      emitida_en     TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
    )
    """,

    // Nombre del periodo calculado con las fechas
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

    // Cupo docente y periodo de cada actividad
    // El horario se arma en Kotlin para respetar el orden de los dias
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

    // Porcentaje de asistencia calculado al consultar
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

    // Bloquea inscripciones con cupo lleno
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

    // Un alumno solo puede tener una actividad activa
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
)

// Se borran en orden inverso a las dependencias
internal val VISTAS = listOf("v_asistencia_alumno", "v_actividades", "v_periodos")

internal val TABLAS = listOf(
    "constancias", "asistencias", "sesiones", "inscripciones", "evidencia_fotos",
    "evidencias", "horarios", "actividades", "alumnos", "docentes", "usuarios",
    "lugares", "dias_semana", "periodos", "carreras",
)

// Datos de prueba
// La contrasena de prueba es la misma para todas las cuentas
internal object Sembrado {
    private const val CLAVE_DEMO = "itsm1234"

    fun crear(db: SQLiteDatabase) {
        // Los tres primeros lugares conservan el id de las carreras anteriores
        listOf(
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
        ).forEach {
            db.insert("carreras", null, ContentValues().apply { put("nombre", it) })
        }
        db.execSQL("INSERT INTO periodos (inicio, fin, activo) VALUES ('2025-01-20', '2025-06-30', 1)")

        val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        dias.forEachIndexed { indice, nombre ->
            db.insert("dias_semana", null, ContentValues().apply {
                put("id", indice + 1)
                put("nombre", nombre)
                put("orden", indice + 1)
            })
        }
        listOf(
            "ITSM", "Cancha de fútbol", "Cancha de básquetbol",
            "Gimnasio", "Salón de usos múltiples", "Patio central",
        ).forEach {
            db.insert("lugares", null, ContentValues().apply { put("nombre", it) })
        }

        val personas = listOf(
            Persona("Administrador", "ITSM", null, "admin@itsm.edu.mx", "admin"),
            Persona("Carlos", "Pérez", "Domínguez", "cperez@itsm.edu.mx", "docente"),
            Persona("Ana", "López", "Hernández", "alopez@itsm.edu.mx", "docente"),
            Persona("Luis", "Gómez", "Ramos", "lgomez@itsm.edu.mx", "docente"),
            Persona("María", "Hernández", "Cruz", "mhernandez@itsm.edu.mx", "alumno"),
            Persona("José", "Martínez", "Luna", "jmartinez@itsm.edu.mx", "alumno"),
            Persona("Diana", "Sánchez", "Rojas", "dsanchez@itsm.edu.mx", "alumno"),
            Persona("Pedro", "García", "Mendoza", "pgarcia@itsm.edu.mx", "alumno"),
        )
        val hash = Clave.generar(CLAVE_DEMO)
        personas.forEach { p ->
            db.insert("usuarios", null, ContentValues().apply {
                put("nombres", p.nombres)
                put("apellido_paterno", p.paterno)
                if (p.materno == null) putNull("apellido_materno") else put("apellido_materno", p.materno)
                put("correo", p.correo)
                put("contrasena_hash", hash)
                put("rol", p.rol)
            })
        }

        listOf("cperez@itsm.edu.mx", "alopez@itsm.edu.mx", "lgomez@itsm.edu.mx").forEach { correo ->
            db.insert("docentes", null, ContentValues().apply {
                put("id_usuario", idDe(db, "usuarios", "correo", correo))
                put("especialidad", "Deportes")
                put("telefono", "2351000000")
            })
        }

        listOf(
            "mhernandez@itsm.edu.mx" to "232T0182",
            "jmartinez@itsm.edu.mx" to "219T0382",
            "dsanchez@itsm.edu.mx" to "232T0183",
            "pgarcia@itsm.edu.mx" to "219T0383",
        ).forEach { (correo, matricula) ->
            db.insert("alumnos", null, ContentValues().apply {
                put("id_usuario", idDe(db, "usuarios", "correo", correo))
                put("matricula", matricula)
                put("id_carrera", 1)
                put("semestre", 7)
            })
        }

        listOf(
            Triple("Fútbol", 25, "cperez@itsm.edu.mx"),
            Triple("Ajedrez", 20, "alopez@itsm.edu.mx"),
            Triple("Voleibol", 25, "lgomez@itsm.edu.mx"),
            Triple("Baloncesto", 25, "cperez@itsm.edu.mx"),
            Triple("Banda de guerra", 30, "lgomez@itsm.edu.mx"),
        ).forEach { (nombre, cupo, correo) ->
            db.insert("actividades", null, ContentValues().apply {
                put("nombre", nombre)
                put("cupo", cupo)
                put("id_docente", idDe(db, "usuarios", "correo", correo))
                put("id_periodo", 1)
            })
        }

        listOf(
            FilaHorario("Fútbol", "Lunes", "16:00", "18:00", "Cancha de fútbol"),
            FilaHorario("Fútbol", "Miércoles", "16:00", "18:00", "Cancha de fútbol"),
            FilaHorario("Ajedrez", "Martes", "15:00", "17:00", "Salón de usos múltiples"),
            FilaHorario("Voleibol", "Jueves", "17:00", "19:00", "Gimnasio"),
            FilaHorario("Baloncesto", "Viernes", "17:00", "19:00", "Cancha de básquetbol"),
            FilaHorario("Banda de guerra", "Lunes", "18:00", "20:00", "Patio central"),
        ).forEach { h ->
            db.insert("horarios", null, ContentValues().apply {
                put("id_actividad", idDe(db, "actividades", "nombre", h.actividad))
                put("id_dia", idDe(db, "dias_semana", "nombre", h.dia))
                put("hora_inicio", h.inicio)
                put("hora_fin", h.fin)
                put("id_lugar", idDe(db, "lugares", "nombre", h.lugar))
            })
        }

        val futbol = idDe(db, "actividades", "nombre", "Fútbol")
        db.execSQL("INSERT INTO inscripciones (id_alumno, id_actividad) SELECT id_usuario, ? FROM alumnos", arrayOf(futbol))

        listOf("2025-03-03", "2025-03-05").forEach { fecha ->
            db.insert("sesiones", null, ContentValues().apply {
                put("id_actividad", futbol)
                put("fecha", fecha)
            })
            val sesion = db.rawQuery(
                "SELECT id FROM sesiones WHERE id_actividad = ? AND fecha = ?",
                arrayOf(futbol.toString(), fecha),
            ).use { it.moveToFirst(); it.getInt(0) }
            db.rawQuery("SELECT id_usuario, matricula FROM alumnos", null).use { c ->
                while (c.moveToNext()) {
                    val falta = c.getString(1) == "219T0383" && fecha == "2025-03-05"
                    db.insert("asistencias", null, ContentValues().apply {
                        put("id_sesion", sesion)
                        put("id_alumno", c.getInt(0))
                        put("presente", if (falta) 0 else 1)
                    })
                }
            }
        }
    }

    private fun idDe(db: SQLiteDatabase, tabla: String, columna: String, valor: String): Int =
        db.rawQuery("SELECT id FROM $tabla WHERE $columna = ?", arrayOf(valor)).use {
            it.moveToFirst()
            it.getInt(0)
        }

    private data class Persona(
        val nombres: String,
        val paterno: String,
        val materno: String?,
        val correo: String,
        val rol: String,
    )

    private data class FilaHorario(
        val actividad: String,
        val dia: String,
        val inicio: String,
        val fin: String,
        val lugar: String,
    )
}
