package itsm.extra_tec.Modelo

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Guarda las contrasenas con PBKDF2 y una sal por cuenta
// Nunca se guarda la contrasena en claro
object Clave {
    private const val ITERACIONES = 120_000
    private const val LARGO_BITS = 256
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"
    private val azar = SecureRandom()

    fun generar(contrasena: String): String {
        val sal = ByteArray(16).also { azar.nextBytes(it) }
        val hash = derivar(contrasena, sal, ITERACIONES)
        return listOf("pbkdf2", ITERACIONES.toString(), base64(sal), base64(hash)).joinToString("\$")
    }

    fun verificar(contrasena: String, guardado: String): Boolean {
        val partes = guardado.split("\$")
        if (partes.size != 4 || partes[0] != "pbkdf2") return false
        val iteraciones = partes[1].toIntOrNull() ?: return false
        val sal = runCatching { Base64.decode(partes[2], Base64.NO_WRAP) }.getOrNull() ?: return false
        val esperado = runCatching { Base64.decode(partes[3], Base64.NO_WRAP) }.getOrNull() ?: return false
        return MessageDigest.isEqual(esperado, derivar(contrasena, sal, iteraciones))
    }

    // Contrasena temporal para las cuentas nuevas
    fun inicial(): String = ByteArray(5).also { azar.nextBytes(it) }
        .joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    private fun derivar(contrasena: String, sal: ByteArray, iteraciones: Int): ByteArray {
        val especificacion = PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, LARGO_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
        }
    }

    private fun base64(datos: ByteArray): String = Base64.encodeToString(datos, Base64.NO_WRAP)
}
