package itsm.extra_tec.Modelo

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors

// Error con un mensaje claro para el usuario
class ErrorDatos(mensaje: String) : Exception(mensaje)

// Base de datos local sin servidor ni conexion
object BaseDatos {
    private const val NOMBRE = "extra_tec.db"
    private const val VERSION = 3

    private var ayudante: Ayudante? = null
    private var aplicacion: Context? = null
    private val hilo = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())

    val contexto: Context get() = aplicacion ?: error("La base de datos no se ha iniciado")

    fun iniciar(contexto: Context) {
        if (ayudante == null) {
            aplicacion = contexto.applicationContext
            ayudante = Ayudante(contexto.applicationContext)
        }
    }

    // Lectura sin transaccion
    fun <T> consultar(bloque: (SQLiteDatabase) -> T, alTerminar: (Resultado_Modelo<T>) -> Unit) =
        ejecutar(false, bloque, alTerminar)

    // Escritura completa o nada
    fun <T> escribir(bloque: (SQLiteDatabase) -> T, alTerminar: (Resultado_Modelo<T>) -> Unit) =
        ejecutar(true, bloque, alTerminar)

    private fun <T> ejecutar(
        conTransaccion: Boolean,
        bloque: (SQLiteDatabase) -> T,
        alTerminar: (Resultado_Modelo<T>) -> Unit,
    ) {
        hilo.execute {
            val db = ayudante!!.writableDatabase
            val resultado: Resultado_Modelo<T> = try {
                if (conTransaccion) {
                    db.beginTransaction()
                    try {
                        val valor = bloque(db)
                        db.setTransactionSuccessful()
                        Resultado_Modelo.Exito(valor)
                    } finally {
                        db.endTransaction()
                    }
                } else {
                    Resultado_Modelo.Exito(bloque(db))
                }
            } catch (e: Exception) {
                Log.e("extra_tec", "Error de base de datos", e)
                Resultado_Modelo.Fallo(mensaje(e))
            }
            ui.post { alTerminar(resultado) }
        }
    }

    // Convierte los errores de SQLite en mensajes claros
    private fun mensaje(e: Exception): String {
        if (e is ErrorDatos) return e.message ?: "No se pudo completar la operación."
        val texto = e.message.orEmpty()
        return when {
            texto.contains("usuarios.correo") -> "Ese correo ya está registrado."
            texto.contains("alumnos.matricula") -> "Ese número de control ya está registrado."
            texto.contains("actividades.nombre") -> "Ya existe una actividad con ese nombre en el periodo."
            texto.contains("El alumno ya tiene una actividad inscrita") -> "El alumno ya tiene una actividad inscrita."
            texto.contains("Cupo lleno para esta actividad") -> "Cupo lleno para esta actividad."
            texto.contains("UNIQUE constraint failed") -> "Ya existe un registro con esos datos."
            texto.contains("CHECK constraint failed") -> "Los datos no cumplen el formato permitido."
            texto.contains("FOREIGN KEY constraint failed") -> "El registro depende de otro que ya no existe."
            else -> "No se pudo guardar la información."
        }
    }

    private class Ayudante(contexto: Context) : SQLiteOpenHelper(contexto, NOMBRE, null, VERSION) {
        override fun onConfigure(db: SQLiteDatabase) {
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            ESQUEMA.forEach { db.execSQL(it.trimIndent()) }
            Sembrado.crear(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
            var version = versionAnterior
            // La version 1 a 2 solo cambia la tabla de evidencias
            if (version == 1) {
                migrarEvidenciasV1(db)
                version = 2
            }
            // La version 2 a 3 actualiza el catalogo de carreras
            if (version == 2) {
                migrarCarrerasV3(db)
                version = 3
            }
            // Otro cambio de esquema recrea la base local
            if (version != versionNueva) {
                VISTAS.forEach { db.execSQL("DROP VIEW IF EXISTS $it") }
                TABLAS.forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
                onCreate(db)
            }
        }

        // Cambia los nombres anteriores por los oficiales y agrega las carreras nuevas
        private fun migrarCarrerasV3(db: SQLiteDatabase) {
            val cambios = listOf(
                "Ing. en Sistemas" to "Ingeniería en Sistemas Computacionales",
                "Ing. Industrial" to "Ingeniería Industrial",
                "Ing. en Gestión" to "Ingeniería en Gestión Empresarial",
            )
            cambios.forEach { (anterior, oficial) ->
                db.execSQL(
                    "UPDATE carreras SET nombre = ? WHERE nombre = ?",
                    arrayOf(oficial, anterior),
                )
            }
            listOf(
                "Ingeniería Bioquímica",
                "Ingeniería Electromecánica",
                "Ingeniería Civil",
                "Ingeniería en Tecnologías de la Información y Comunicaciones",
                "Ingeniería Ambiental",
                "Ingeniería Petrolera",
                "Licenciatura en Gastronomía",
            ).forEach { nombre ->
                db.execSQL(
                    "INSERT OR IGNORE INTO carreras (nombre) VALUES (?)",
                    arrayOf(nombre),
                )
            }
        }

        // Conserva las evidencias de la version anterior
        // Cada registro tenia una foto y no guardaba estado ni fecha
        private fun migrarEvidenciasV1(db: SQLiteDatabase) {
            db.execSQL("ALTER TABLE evidencias RENAME TO evidencias_v1")
            db.execSQL(DDL_EVIDENCIAS)
            db.execSQL(DDL_EVIDENCIA_FOTOS)
            db.execSQL("CREATE INDEX idx_evidencias_actividad_fecha ON evidencias (id_actividad, fecha)")
            db.execSQL(
                """
                INSERT INTO evidencias (id, id_actividad, id_docente, fecha, estado, descripcion, creada_en)
                SELECT id, id_actividad, id_docente, date(subida_en), 'realizada', descripcion, subida_en
                  FROM evidencias_v1
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO evidencia_fotos (id_evidencia, ruta_foto, orden)
                SELECT id, ruta_foto, 0 FROM evidencias_v1
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE evidencias_v1")
        }
    }
}

// Atajos para leer columnas por nombre
internal fun Cursor.texto(columna: String): String {
    val indice = getColumnIndex(columna)
    return if (indice < 0 || isNull(indice)) "" else getString(indice)
}

internal fun Cursor.entero(columna: String): Int {
    val indice = getColumnIndex(columna)
    return if (indice < 0 || isNull(indice)) 0 else getInt(indice)
}

internal fun Cursor.enteroONulo(columna: String): Int? {
    val indice = getColumnIndex(columna)
    return if (indice < 0 || isNull(indice)) null else getInt(indice)
}

internal fun Cursor.real(columna: String): Double {
    val indice = getColumnIndex(columna)
    return if (indice < 0 || isNull(indice)) 0.0 else getDouble(indice)
}
