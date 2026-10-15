package itsm.extra_tec

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/** Cliente mínimo de la API REST (carpeta api/). Todas las llamadas corren fuera del hilo principal. */
object Api {
    // 10.0.2.2 es el PC anfitrión visto desde el emulador. En un teléfono real se cambia desde el login.
    const val URL_EMULADOR = "http://10.0.2.2:3000"
    var baseUrl: String = URL_EMULADOR
    var token: String? = null

    /** Se invoca cuando la API responde 401 con una sesión abierta (token vencido). */
    var alExpirarSesion: (() -> Unit)? = null

    private val hilos = Executors.newFixedThreadPool(4)
    private val ui = Handler(Looper.getMainLooper())

    private class ErrorApi(val codigo: Int, mensaje: String) : Exception(mensaje)

    /** JSON de respuesta: JSONObject, JSONArray o null si el cuerpo viene vacío. */
    fun pedir(
        metodo: String,
        ruta: String,
        cuerpo: JSONObject? = null,
        alExito: (Any?) -> Unit,
        alError: (String) -> Unit,
    ) = ejecutar(alError) {
        val con = abrir(metodo, ruta)
        if (cuerpo != null) {
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            con.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
        }
        val texto = String(leer(con), Charsets.UTF_8).trim()
        val json: Any? = when {
            texto.startsWith("[") -> JSONArray(texto)
            texto.startsWith("{") -> JSONObject(texto)
            else -> null
        }
        ui.post { alExito(json) }
    }

    /** Descarga un archivo (PDF). Devuelve los bytes y el nombre sugerido por el servidor. */
    fun descargar(ruta: String, alExito: (ByteArray, String) -> Unit, alError: (String) -> Unit) =
        ejecutar(alError) {
            val con = abrir("GET", ruta)
            val bytes = leer(con)
            val disp = con.getHeaderField("Content-Disposition").orEmpty()
            val nombre = Regex("filename=\"?([^\";]+)\"?").find(disp)?.groupValues?.get(1) ?: "archivo.pdf"
            ui.post { alExito(bytes, nombre) }
        }

    /** POST multipart con un campo de archivo y varios campos de texto. */
    fun subirArchivo(
        ruta: String,
        campos: Map<String, String>,
        campoArchivo: String,
        nombreArchivo: String,
        tipoMime: String,
        datos: ByteArray,
        alExito: () -> Unit,
        alError: (String) -> Unit,
    ) = ejecutar(alError) {
        val limite = "----extratec" + UUID.randomUUID().toString()
        val con = abrir("POST", ruta)
        con.doOutput = true
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=$limite")
        val salida = ByteArrayOutputStream()
        fun texto(s: String) = salida.write(s.toByteArray(Charsets.UTF_8))
        for ((k, v) in campos) {
            texto("--$limite\r\nContent-Disposition: form-data; name=\"$k\"\r\n\r\n$v\r\n")
        }
        texto("--$limite\r\nContent-Disposition: form-data; name=\"$campoArchivo\"; filename=\"$nombreArchivo\"\r\n")
        texto("Content-Type: $tipoMime\r\n\r\n")
        salida.write(datos)
        texto("\r\n--$limite--\r\n")
        con.setFixedLengthStreamingMode(salida.size())
        con.outputStream.use { salida.writeTo(it) }
        leer(con)
        ui.post { alExito() }
    }

    // ---------- internos ----------

    private fun abrir(metodo: String, ruta: String): HttpURLConnection {
        val con = URL(baseUrl.trimEnd('/') + "/api" + ruta).openConnection() as HttpURLConnection
        con.requestMethod = metodo
        con.connectTimeout = 8000
        con.readTimeout = 20000
        con.setRequestProperty("Accept", "application/json")
        token?.let { con.setRequestProperty("Authorization", "Bearer $it") }
        return con
    }

    /** Lee la respuesta; si el código no es 2xx lanza ErrorApi con el mensaje de la API. */
    private fun leer(con: HttpURLConnection): ByteArray {
        val codigo = con.responseCode
        if (codigo in 200..299) return con.inputStream.use { it.readBytes() }
        val cuerpo = con.errorStream?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty()
        val mensaje = runCatching { JSONObject(cuerpo).getString("error") }.getOrNull() ?: "Error del servidor ($codigo)"
        throw ErrorApi(codigo, mensaje)
    }

    private fun ejecutar(alError: (String) -> Unit, bloque: () -> Unit) {
        hilos.execute {
            try {
                bloque()
            } catch (e: ErrorApi) {
                ui.post {
                    if (e.codigo == 401 && token != null) {
                        token = null
                        alExpirarSesion?.invoke()
                    } else {
                        alError(e.message ?: "Error")
                    }
                }
            } catch (e: Exception) {
                ui.post { alError("No se pudo conectar con el servidor ($baseUrl). ¿Está encendida la API?") }
            }
        }
    }
}
