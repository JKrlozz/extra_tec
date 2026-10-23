package itsm.extra_tec.Modelo

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress

class Descubrimiento_ModeloTest {
    private var servidor: HttpServer? = null

    @After
    fun cerrar() {
        servidor?.stop(0)
    }

    private fun levantar(respuesta: String, codigo: Int = 200): Int {
        val http = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        http.createContext("/api/salud") { intercambio ->
            val cuerpo = respuesta.toByteArray()
            intercambio.sendResponseHeaders(codigo, cuerpo.size.toLong())
            intercambio.responseBody.use { it.write(cuerpo) }
        }
        http.start()
        servidor = http
        return http.address.port
    }

    private fun ip(a: Int, b: Int, c: Int, d: Int) = byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte())

    @Test
    fun subredDe24RecorreDel1Al254SinLaPropia() {
        val hosts = Descubrimiento_Modelo.hostsDeLaSubred(ip(192, 168, 1, 50), 24)
        assertEquals(253, hosts.size)
        assertEquals((1..254).filter { it != 50 }.map { "192.168.1.$it" }.toSet(), hosts.toSet())
        assertFalse(hosts.contains("192.168.1.50"))
    }

    @Test
    fun puntoDeAccesoDelCelularUsaSuSubredPequena() {
        val hosts = Descubrimiento_Modelo.hostsDeLaSubred(ip(172, 20, 10, 3), 28)
        assertEquals(13, hosts.size)
        assertTrue(hosts.contains("172.20.10.1"))
        assertTrue(hosts.contains("172.20.10.14"))
        assertFalse(hosts.contains("172.20.10.0"))
        assertFalse(hosts.contains("172.20.10.15"))
        assertFalse(hosts.contains("172.20.10.3"))
    }

    @Test
    fun redDeLaEscuelaCubreTodaLaSubredYNoSoloUnBloque() {
        // La red ITSM-WiFi es 192.168.202.157/21: de 192.168.200.0 a 192.168.207.255
        val hosts = Descubrimiento_Modelo.hostsDeLaSubred(ip(192, 168, 203, 45), 21)
        assertEquals(2045, hosts.size)
        assertTrue(hosts.contains("192.168.202.157"))
        assertTrue(hosts.contains("192.168.200.1"))
        assertTrue(hosts.contains("192.168.207.254"))
        assertFalse(hosts.contains("192.168.203.45"))
        assertFalse(hosts.contains("192.168.208.1"))
    }

    @Test
    fun lasDireccionesMasCercanasSeProbanPrimero() {
        val hosts = Descubrimiento_Modelo.hostsDeLaSubred(ip(192, 168, 202, 100), 21)
        assertTrue(hosts.take(4).all { it.startsWith("192.168.202.") })
    }

    @Test
    fun redesMuyGrandesSeLimitanAUna20() {
        val hosts = Descubrimiento_Modelo.hostsDeLaSubred(ip(10, 0, 5, 20), 16)
        assertEquals(4093, hosts.size)
        assertTrue(hosts.all { it.startsWith("10.0.") && it.split('.')[2].toInt() in 0..15 })
    }

    @Test
    fun reconoceLaApiPorSuRespuesta() {
        val puerto = levantar("""{"ok":true,"servicio":"extra_tec"}""")
        assertTrue(Descubrimiento_Modelo.responde("http://127.0.0.1:$puerto"))
    }

    @Test
    fun ignoraOtroServicioEnElMismoPuerto() {
        val puerto = levantar("""{"ok":true,"servicio":"otra_cosa"}""")
        assertFalse(Descubrimiento_Modelo.responde("http://127.0.0.1:$puerto"))
    }

    @Test
    fun ignoraRespuestasQueNoSonJson() {
        val puerto = levantar("<html>hola</html>")
        assertFalse(Descubrimiento_Modelo.responde("http://127.0.0.1:$puerto"))
    }

    @Test
    fun ignoraErroresHttp() {
        val puerto = levantar("""{"ok":true,"servicio":"extra_tec"}""", codigo = 500)
        assertFalse(Descubrimiento_Modelo.responde("http://127.0.0.1:$puerto"))
    }

    @Test
    fun puertoCerradoNoResponde() {
        val puerto = levantar("""{"ok":true,"servicio":"extra_tec"}""")
        servidor?.stop(0)
        assertFalse(Descubrimiento_Modelo.responde("http://127.0.0.1:$puerto"))
    }

    @Test
    fun encuentraLaApiEntreVariosHosts() {
        val puerto = levantar("""{"ok":true,"servicio":"extra_tec"}""")
        val encontrada = Descubrimiento_Modelo.encontrarEnHosts(listOf("127.0.0.2", "127.0.0.3", "127.0.0.1"), puerto)
        assertEquals("http://127.0.0.1:$puerto", encontrada)
    }

    @Test
    fun sinApiDevuelveNulo() {
        val puerto = levantar("""{"ok":true,"servicio":"extra_tec"}""")
        servidor?.stop(0)
        assertNull(Descubrimiento_Modelo.encontrarEnHosts(listOf("127.0.0.1", "127.0.0.2"), puerto))
        assertNull(Descubrimiento_Modelo.encontrarEnHosts(emptyList(), puerto))
    }
}
