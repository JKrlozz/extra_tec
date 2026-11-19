package itsm.extra_tec.Modelo

import android.content.Context

// la sesion queda en el telefono
class Sesion_Modelo(contexto: Context) {
    private val sesion = contexto.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    fun guardar(sesionIniciada: SesionIniciada) {
        val usuario = sesionIniciada.usuario
        val editor = sesion.edit()
        editor.putInt("id", usuario.id)
        editor.putString("nombres", usuario.nombres)
        editor.putString("apellido_paterno", usuario.apellidoPaterno)
        editor.putString("apellido_materno", usuario.apellidoMaterno)
        editor.putString("correo", usuario.correo)
        editor.putString("rol", usuario.rol)
        editor.putString("token", sesionIniciada.token)
        editor.apply()
    }

    fun leer(): SesionIniciada? {
        val id = sesion.getInt("id", 0)
        if (id == 0) {
            return null
        }
        val rol = sesion.getString("rol", "") ?: ""
        val token = sesion.getString("token", "") ?: ""
        if (token.isEmpty() || (rol != "alumno" && rol != "docente" && rol != "admin")) {
            borrar()
            return null
        }
        val usuario = Usuario(
            id,
            sesion.getString("nombres", "") ?: "",
            sesion.getString("apellido_paterno", "") ?: "",
            sesion.getString("apellido_materno", "") ?: "",
            sesion.getString("correo", "") ?: "",
            rol,
        )
        return SesionIniciada(usuario, token)
    }

    fun borrar() {
        sesion.edit().clear().apply()
    }
}
