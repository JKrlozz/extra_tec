package itsm.extra_tec.Modelo

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// la contrasena nunca va directa
// se guarda con pbkdf2
object Clave {
    private const val ITERACIONES = 120000
    private const val LARGO_BITS = 256
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"
    private val azar = SecureRandom()

    // queda como pbkdf2$iteraciones$sal$hash
    fun generar(contrasena: String): String {
        val sal = ByteArray(16)
        azar.nextBytes(sal)
        val hash = derivar(contrasena, sal, ITERACIONES)
        return "pbkdf2$" + ITERACIONES + "$" + base64(sal) + "$" + base64(hash)
    }

    fun verificar(contrasena: String, guardado: String): Boolean {
        val partes = guardado.split("$")
        if (partes.size != 4 || partes[0] != "pbkdf2") {
            return false
        }
        val iteraciones = partes[1].toIntOrNull()
        if (iteraciones == null) {
            return false
        }
        try {
            val sal = Base64.decode(partes[2], Base64.NO_WRAP)
            val esperado = Base64.decode(partes[3], Base64.NO_WRAP)
            return MessageDigest.isEqual(esperado, derivar(contrasena, sal, iteraciones))
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

    // clave temporal de diez letras
    fun inicial(): String {
        val letras = "abcdefghjkmnpqrstuvwxyz23456789"
        var clave = ""
        for (i in 1..10) {
            clave += letras[azar.nextInt(letras.length)]
        }
        return clave
    }

    private fun derivar(contrasena: String, sal: ByteArray, iteraciones: Int): ByteArray {
        val especificacion = PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, LARGO_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
        }
    }

    private fun base64(datos: ByteArray): String {
        return Base64.encodeToString(datos, Base64.NO_WRAP)
    }
}
