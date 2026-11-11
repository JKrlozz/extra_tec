package itsm.extra_tec.Modelo

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors

// error con mensaje para el usuario
class ErrorDatos(mensaje: String) : Exception(mensaje)

// base de datos local del telefono
object BaseDatos {
    private const val NOMBRE = "extra_tec.db"
    private const val VERSION = 5

    private var ayudante: Ayudante? = null
    private var aplicacion: Context? = null

    // las consultas van en otro hilo
    private val hilo = Executors.newSingleThreadExecutor()
    private val pantalla = Handler(Looper.getMainLooper())

    val contexto: Context
        get() {
            val actual = aplicacion
            if (actual == null) {
                throw IllegalStateException("La base de datos no se ha iniciado")
            }
            return actual
        }

    fun iniciar(contexto: Context) {
        if (ayudante == null) {
            aplicacion = contexto.applicationContext
            ayudante = Ayudante(contexto.applicationContext)
        }
    }

    // solo lectura
    fun <T> consultar(bloque: (SQLiteDatabase) -> T, alTerminar: (Resultado_Modelo<T>) -> Unit) {
        ejecutar(false, bloque, alTerminar)
    }

    // se guarda todo o nada
    fun <T> escribir(bloque: (SQLiteDatabase) -> T, alTerminar: (Resultado_Modelo<T>) -> Unit) {
        ejecutar(true, bloque, alTerminar)
    }

    private fun <T> ejecutar(
        conTransaccion: Boolean,
        bloque: (SQLiteDatabase) -> T,
        alTerminar: (Resultado_Modelo<T>) -> Unit,
    ) {
        hilo.execute {
            val db = ayudante!!.writableDatabase
            var resultado: Resultado_Modelo<T>
            try {
                if (conTransaccion) {
                    db.beginTransaction()
                    try {
                        val valor = bloque(db)
                        db.setTransactionSuccessful()
                        resultado = Resultado_Modelo.Exito(valor)
                    } finally {
                        db.endTransaction()
                    }
                } else {
                    resultado = Resultado_Modelo.Exito(bloque(db))
                }
            } catch (e: Exception) {
                Log.e("extra_tec", "Error de base de datos", e)
                resultado = Resultado_Modelo.Fallo(mensaje(e))
            }
            // el resultado regresa a pantalla
            pantalla.post { alTerminar(resultado) }
        }
    }

    // errores de sqlite a mensajes claros
    private fun mensaje(e: Exception): String {
        if (e is ErrorDatos) {
            return e.message ?: "No se pudo completar la operación."
        }
        val texto = e.message ?: ""
        if (texto.contains("usuarios.correo")) return "Ese correo ya está registrado."
        if (texto.contains("alumnos.matricula")) return "Ese número de control ya está registrado."
        if (texto.contains("actividades.nombre")) return "Ya existe una actividad con ese nombre en el periodo."
        if (texto.contains("El alumno ya tiene una actividad inscrita")) return "El alumno ya tiene una actividad inscrita."
        if (texto.contains("Cupo lleno para esta actividad")) return "Cupo lleno para esta actividad."
        if (texto.contains("El alumno no esta inscrito")) return "El alumno no está inscrito en la actividad."
        if (texto.contains("Maximo 10 fotografias")) return "Puedes guardar hasta 10 fotografías por evidencia."
        if (texto.contains("no coincide con el numero")) return "El correo del alumno debe ser su número de control@itsm.edu.mx."
        if (texto.contains("no tiene rol")) return "El tipo de usuario no coincide."
        if (texto.contains("periodos.activo")) return "Solo puede haber un periodo activo."
        if (texto.contains("UNIQUE constraint failed")) return "Ya existe un registro con esos datos."
        if (texto.contains("CHECK constraint failed")) return "Los datos no cumplen el formato permitido."
        if (texto.contains("FOREIGN KEY constraint failed")) return "El registro depende de otro que ya no existe."
        return "No se pudo guardar la información."
    }

    private class Ayudante(contexto: Context) : SQLiteOpenHelper(contexto, NOMBRE, null, VERSION) {
        override fun onConfigure(db: SQLiteDatabase) {
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            for (sentencia in ESQUEMA) {
                db.execSQL(sentencia.trimIndent())
            }
            Sembrado.crear(db)
        }

        // solo hay datos de prueba
        // otra version se crea de nuevo
        override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
            for (vista in VISTAS) {
                db.execSQL("DROP VIEW IF EXISTS $vista")
            }
            for (tabla in TABLAS) {
                db.execSQL("DROP TABLE IF EXISTS $tabla")
            }
            onCreate(db)
        }
    }
}

// atajos para leer columnas por nombre
internal fun Cursor.texto(columna: String): String {
    val indice = getColumnIndex(columna)
    if (indice < 0 || isNull(indice)) return ""
    return getString(indice)
}

internal fun Cursor.entero(columna: String): Int {
    val indice = getColumnIndex(columna)
    if (indice < 0 || isNull(indice)) return 0
    return getInt(indice)
}

internal fun Cursor.enteroONulo(columna: String): Int? {
    val indice = getColumnIndex(columna)
    if (indice < 0 || isNull(indice)) return null
    return getInt(indice)
}

internal fun Cursor.real(columna: String): Double {
    val indice = getColumnIndex(columna)
    if (indice < 0 || isNull(indice)) return 0.0
    return getDouble(indice)
}
