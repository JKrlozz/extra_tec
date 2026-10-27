package itsm.extra_tec.Modelo

import android.content.Context
import org.json.JSONObject

// el archivo sesion se excluye de los respaldos (res/xml)
class Sesion_Modelo(contexto: Context) {
    private val sesion = contexto.getSharedPreferences("sesion", Context.MODE_PRIVATE)
    private val config = contexto.getSharedPreferences("config", Context.MODE_PRIVATE)

    fun guardar(sesionIniciada: SesionIniciada) {
        sesion.edit()
            .putString("token", sesionIniciada.token)
            .putString("usuario", sesionIniciada.usuario.aJson().toString())
            .apply()
    }

    fun leer(): SesionIniciada? {
        val token = sesion.getString("token", null) ?: return null
        val usuario = runCatching { Usuario.desdeJson(JSONObject(sesion.getString("usuario", "")!!)) }.getOrNull()
        if (usuario == null) {
            borrar()
            return null
        }
        return SesionIniciada(token, usuario)
    }

    fun borrar() {
        sesion.edit().clear().apply()
    }

    fun urlServidor(): String? = config.getString("url", null)

    fun guardarUrlServidor(url: String) {
        config.edit().putString("url", url).apply()
    }
}
