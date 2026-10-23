package itsm.extra_tec.Modelo

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

object Api_Modelo {
    // 10.0.2.2 es el equipo anfitrión visto desde el emulador
    const val URL_EMULADOR = "http://10.0.2.2:3000"
    var baseUrl: String = URL_EMULADOR
    var token: String? = null

    // Se invoca cuando la API responde 401 con una sesión abierta
    var alExpirarSesion: (() -> Unit)? = null

    // Se invoca cuando no hay conexión con el servidor, para volver a buscarlo en la red
    var alPerderConexion: (() -> Unit)? = null

    private val hilos = Executors.newFixedThreadPool(4)
    private val ui = Handler(Looper.getMainLooper())

    private class ErrorApi(val codigo: Int, mensaje: String) : Exception(mensaje)

    // Devuelve JSONObject, JSONArray o null si el cuerpo está vacío
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

    // Convierte la respuesta con "convertir"; un fallo de conversión se reporta como error
    fun <T> consultar(
        metodo: String,
        ruta: String,
        cuerpo: JSONObject? = null,
        convertir: (Any?) -> T,
        alTerminar: (Resultado_Modelo<T>) -> Unit,
    ) = pedir(metodo, ruta, cuerpo,
        alExito = { json ->
            val resultado: Resultado_Modelo<T> = runCatching<Resultado_Modelo<T>> {
                Resultado_Modelo.Exito(convertir(json))
            }.getOrElse { Resultado_Modelo.Fallo("Respuesta inesperada del servidor") }
            alTerminar(resultado)
        },
        alError = { alTerminar(Resultado_Modelo.Fallo(it)) })

    fun descargar(ruta: String, alExito: (ByteArray, String) -> Unit, alError: (String) -> Unit) =
        ejecutar(alError) {
            val con = abrir("GET", ruta)
            val bytes = leer(con)
            val disp = con.getHeaderField("Content-Disposition").orEmpty()
            val nombre = Regex("filename=\"?([^\";]+)\"?").find(disp)?.groupValues?.get(1) ?: "archivo.pdf"
            ui.post { alExito(bytes, nombre) }
        }

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

    private fun abrir(metodo: String, ruta: String): HttpURLConnection {
        val con = URL(baseUrl.trimEnd('/') + "/api" + ruta).openConnection() as HttpURLConnection
        con.requestMethod = metodo
        con.connectTimeout = 8000
        con.readTimeout = 20000
        con.setRequestProperty("Accept", "application/json")
        token?.let { con.setRequestProperty("Authorization", "Bearer $it") }
        return con
    }

    // Lanza ErrorApi con el mensaje del servidor si el código no es 2xx
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
                ui.post {
                    alError("No se pudo conectar con el servidor. Verifica que la API esté encendida y que estés en la misma red Wi-Fi.")
                    alPerderConexion?.invoke()
                }
            }
        }
    }
}
