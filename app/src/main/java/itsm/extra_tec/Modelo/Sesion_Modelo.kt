package itsm.extra_tec.Modelo

import android.content.Context

// La sesion se guarda solo en este telefono
class Sesion_Modelo(contexto: Context) {
    private val sesion = contexto.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    fun guardar(sesionIniciada: SesionIniciada) {
        val usuario = sesionIniciada.usuario
        sesion.edit()
            .putInt("id", usuario.id)
            .putString("nombres", usuario.nombres)
            .putString("apellido_paterno", usuario.apellidoPaterno)
            .putString("apellido_materno", usuario.apellidoMaterno)
            .putString("correo", usuario.correo)
            .putString("rol", usuario.rol)
            .apply()
    }

    fun leer(): SesionIniciada? {
        val id = sesion.getInt("id", 0)
        if (id == 0) return null
        val rol = sesion.getString("rol", "").orEmpty()
        if (rol !in setOf("alumno", "docente", "admin")) {
            borrar()
            return null
        }
        return SesionIniciada(
            Usuario(
                id,
                sesion.getString("nombres", "").orEmpty(),
                sesion.getString("apellido_paterno", "").orEmpty(),
                sesion.getString("apellido_materno", "").orEmpty(),
                sesion.getString("correo", "").orEmpty(),
                rol,
            )
        )
    }

    fun borrar() {
        sesion.edit().clear().apply()
    }
}
