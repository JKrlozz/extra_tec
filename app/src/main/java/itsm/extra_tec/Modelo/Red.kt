package itsm.extra_tec.Modelo

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import itsm.extra_tec.BuildConfig
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

// error con mensaje para el usuario
class ErrorDatos(mensaje: String) : Exception(mensaje)

// conexion con la api que guarda todo en Azure SQL
object Red {
    private const val SIN_RED = "No se pudo conectar con el servidor. Revisa tu internet e intenta de nuevo."
    private const val SESION_TERMINO = "La sesión terminó. Inicia sesión de nuevo."
    private const val FALLO_GENERAL = "No se pudo completar la operación."

    private var aplicacion: Context? = null

    // las llamadas van en otro hilo
    private val hilos = Executors.newFixedThreadPool(4)
    private val pantalla = Handler(Looper.getMainLooper())

    val contexto: Context
        get() {
            val actual = aplicacion
            if (actual == null) {
                throw IllegalStateException("La red no se ha iniciado")
            }
            return actual
        }

    fun iniciar(contexto: Context) {
        if (aplicacion == null) {
            aplicacion = contexto.applicationContext
        }
    }

    // corre el bloque fuera de pantalla y regresa el resultado en pantalla
    fun <T> trabajo(bloque: () -> T, alTerminar: (Resultado_Modelo<T>) -> Unit) {
        hilos.execute {
            var resultado: Resultado_Modelo<T>
            try {
                resultado = Resultado_Modelo.Exito(bloque())
            } catch (e: ErrorDatos) {
                resultado = Resultado_Modelo.Fallo(e.message ?: FALLO_GENERAL)
            } catch (e: IOException) {
                Log.e("extra_tec", "Sin conexión", e)
                resultado = Resultado_Modelo.Fallo(SIN_RED)
            } catch (e: Exception) {
                Log.e("extra_tec", "Error inesperado", e)
                resultado = Resultado_Modelo.Fallo(FALLO_GENERAL)
            }
            pantalla.post { alTerminar(resultado) }
        }
    }

    // pide a la api y regresa JSONObject, JSONArray o null
    fun solicitud(metodo: String, ruta: String, cuerpo: JSONObject? = null): Any? {
        val conexion = abrir(metodo, ruta)
        try {
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            }
            val texto = leer(conexion).toString(Charsets.UTF_8)
            if (texto.isBlank()) {
                return null
            }
            return JSONTokener(texto).nextValue()
        } finally {
            conexion.disconnect()
        }
    }

    // datos y fotos en una sola peticion
    fun multiparte(metodo: String, ruta: String, datos: JSONObject, fotos: List<FotoNueva>) {
        val limite = "----extratec" + UUID.randomUUID().toString().replace("-", "")
        val conexion = abrir(metodo, ruta)
        try {
            conexion.doOutput = true
            conexion.setChunkedStreamingMode(0)
            conexion.setRequestProperty("Content-Type", "multipart/form-data; boundary=$limite")
            conexion.outputStream.use { salida ->
                val cabeza = "--$limite\r\nContent-Disposition: form-data; name=\"datos\"\r\n\r\n"
                salida.write(cabeza.toByteArray(Charsets.UTF_8))
                salida.write(datos.toString().toByteArray(Charsets.UTF_8))
                salida.write("\r\n".toByteArray(Charsets.UTF_8))
                for (i in fotos.indices) {
                    val foto = fotos[i]
                    val parte = "--$limite\r\nContent-Disposition: form-data; name=\"fotos\"; filename=\"foto$i\"\r\n" +
                        "Content-Type: ${foto.tipoMime}\r\n\r\n"
                    salida.write(parte.toByteArray(Charsets.UTF_8))
                    salida.write(foto.datos)
                    salida.write("\r\n".toByteArray(Charsets.UTF_8))
                }
                salida.write("--$limite--\r\n".toByteArray(Charsets.UTF_8))
            }
            leer(conexion)
        } finally {
            conexion.disconnect()
        }
    }

    // ruta de la foto en el telefono, se descarga una vez
    fun archivoFoto(id: Int): String {
        val carpeta = carpetaFotos()
        val archivo = File(carpeta, "foto-$id.img")
        if (!archivo.exists() || archivo.length() == 0L) {
            val conexion = abrir("GET", "/fotos/$id")
            try {
                val bytes = leer(conexion)
                val temporal = File(carpeta, "foto-$id.tmp")
                temporal.writeBytes(bytes)
                temporal.renameTo(archivo)
            } finally {
                conexion.disconnect()
            }
        }
        return archivo.absolutePath
    }

    // al cerrar sesion no queda nada de otro usuario
    fun limpiarFotos() {
        val carpeta = File(contexto.cacheDir, "evidencias")
        carpeta.listFiles()?.forEach { it.delete() }
    }

    private fun carpetaFotos(): File {
        val carpeta = File(contexto.cacheDir, "evidencias")
        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }
        return carpeta
    }

    private fun abrir(metodo: String, ruta: String): HttpURLConnection {
        val conexion = URL(BuildConfig.API_URL.trimEnd('/') + "/api" + ruta).openConnection() as HttpURLConnection
        conexion.requestMethod = metodo
        // la base gratuita puede tardar en despertar
        conexion.connectTimeout = 30000
        conexion.readTimeout = 90000
        conexion.setRequestProperty("Accept", "application/json")
        val token = Sesion_Actual.token
        if (token != null) {
            conexion.setRequestProperty("Authorization", "Bearer $token")
        }
        return conexion
    }

    // cuerpo de la respuesta o el error que mando la api
    private fun leer(conexion: HttpURLConnection): ByteArray {
        val codigo = conexion.responseCode
        val flujo = if (codigo < 400) conexion.inputStream else conexion.errorStream
        val bytes = ByteArrayOutputStream()
        flujo?.use { it.copyTo(bytes) }
        if (codigo >= 400) {
            throw ErrorDatos(mensajeError(codigo, bytes.toString("UTF-8")))
        }
        return bytes.toByteArray()
    }

    private fun mensajeError(codigo: Int, cuerpo: String): String {
        try {
            val error = JSONObject(cuerpo).optString("error", "")
            if (error.isNotEmpty()) {
                return error
            }
        } catch (e: Exception) {
            // la respuesta no era de la api
        }
        if (codigo == 401) {
            return SESION_TERMINO
        }
        if (codigo >= 500) {
            return "El servidor no está disponible. Intenta de nuevo en un minuto."
        }
        return FALLO_GENERAL
    }
}
