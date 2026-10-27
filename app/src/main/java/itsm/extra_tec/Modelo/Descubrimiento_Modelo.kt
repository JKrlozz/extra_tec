package itsm.extra_tec.Modelo

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// busca la api en la red wi-fi sin configurar direcciones
object Descubrimiento_Modelo {
    const val PUERTO = 3000
    private const val SERVICIO = "extra_tec"

    private val hilo = Executors.newSingleThreadExecutor()
    // perezoso para que las pruebas no necesiten android
    private val ui by lazy { Handler(Looper.getMainLooper()) }

    fun buscar(contexto: Context, urlActual: String?, alTerminar: (Resultado_Modelo<String>) -> Unit) {
        val aplicacion = contexto.applicationContext
        hilo.execute {
            val busqueda = buscarSincrono(aplicacion, urlActual)
            ui.post {
                alTerminar(
                    if (busqueda.url != null) Resultado_Modelo.Exito(busqueda.url)
                    else Resultado_Modelo.Fallo(
                        "No se encontró el servidor. ${busqueda.detalle} Verifica que la API esté encendida y que el teléfono y la computadora estén en la misma red Wi-Fi."
                    )
                )
            }
        }
    }

    // explica qué se revisó cuando falla la búsqueda
    private class Busqueda(val url: String?, val detalle: String)

    private fun buscarSincrono(contexto: Context, urlActual: String?): Busqueda {
        val gestor = contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = redWifi(gestor)
        // con wi-fi sin internet, android usa datos móviles; se fuerza la red local
        if (wifi != null) gestor.bindProcessToNetwork(wifi)

        // direcciones conocidas: la última, el túnel usb y el emulador
        val conocidas = listOfNotNull(urlActual, "http://localhost:$PUERTO") +
            (if (esEmulador()) listOf("http://10.0.2.2:$PUERTO") else emptyList())
        conocidas.distinct().firstOrNull { responde(it) }?.let { return Busqueda(it, "") }

        if (wifi == null) return Busqueda(null, "El teléfono no está conectado a una red Wi-Fi.")
        val direccion = direccionIPv4(gestor, wifi)
            ?: return Busqueda(null, "No se pudo leer la dirección IP del teléfono.")
        val hosts = hostsDeLaSubred(direccion.first, direccion.second)
        val ip = direccion.first.joinToString(".") { (it.toInt() and 0xFF).toString() }
        val url = encontrarEnHosts(hosts, PUERTO)
        return Busqueda(url, "IP del teléfono: $ip/${direccion.second}; se revisaron ${hosts.size} direcciones.")
    }

    private fun redWifi(gestor: ConnectivityManager): Network? {
        @Suppress("DEPRECATION")
        val redes = gestor.allNetworks.filter {
            val capacidades = gestor.getNetworkCapabilities(it)
            capacidades?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                capacidades?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        }
        return redes.firstOrNull()
    }

    private fun direccionIPv4(gestor: ConnectivityManager, red: Network): Pair<ByteArray, Int>? {
        val enlace = gestor.getLinkProperties(red) ?: return null
        val direccion = enlace.linkAddresses.firstOrNull {
            it.address is Inet4Address && !it.address.isLoopbackAddress
        } ?: return null
        return direccion.address.address to direccion.prefixLength
    }

    private fun esEmulador(): Boolean =
        Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.contains("emulator") ||
            Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish") || Build.MODEL.contains("Emulator")

    // la api se reconoce por su respuesta, no solo por el puerto
    internal fun responde(base: String, esperaMs: Int = 700): Boolean {
        return try {
            val conexion = URL(base.trimEnd('/') + "/api/salud").openConnection() as HttpURLConnection
            conexion.connectTimeout = esperaMs
            conexion.readTimeout = esperaMs
            val cuerpo = conexion.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
            conexion.disconnect()
            Regex("\"ok\"\\s*:\\s*true").containsMatchIn(cuerpo) &&
                Regex("\"servicio\"\\s*:\\s*\"$SERVICIO\"").containsMatchIn(cuerpo)
        } catch (e: Exception) {
            false
        }
    }

    // prueba los hosts en paralelo y devuelve la primera api
    internal fun encontrarEnHosts(hosts: List<String>, puerto: Int, esperaMs: Int = 400): String? {
        if (hosts.isEmpty()) return null
        val hilos = Executors.newFixedThreadPool(minOf(128, hosts.size))
        return try {
            val tareas = hosts.map { host ->
                Callable {
                    val url = "http://$host:$puerto"
                    if (responde(url, esperaMs)) url else throw IllegalStateException("sin respuesta")
                }
            }
            hilos.invokeAny(tareas, 45, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        } finally {
            hilos.shutdownNow()
        }
    }

    // subred sin la propia, las más cercanas primero; máximo una /20
    internal fun hostsDeLaSubred(ip: ByteArray, prefijo: Int): List<String> {
        val bits = prefijo.coerceIn(20, 30)
        val propia = ip.fold(0L) { acumulado, byte -> (acumulado shl 8) or (byte.toLong() and 0xFF) }
        val mascara = (0xFFFFFFFFL shl (32 - bits)) and 0xFFFFFFFFL
        val red = propia and mascara
        val difusion = red or (mascara.inv() and 0xFFFFFFFFL)
        return (red + 1 until difusion)
            .filter { it != propia }
            .sortedBy { kotlin.math.abs(it - propia) }
            .map { "${(it shr 24) and 255}.${(it shr 16) and 255}.${(it shr 8) and 255}.${it and 255}" }
    }
}
