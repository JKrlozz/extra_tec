package itsm.extra_tec.Modelo

// usuario que tiene la sesion abierta
object Sesion_Actual {
    @Volatile
    var usuario: Usuario? = null

    // llave que manda el servidor al iniciar sesion
    @Volatile
    var token: String? = null

    fun id(): Int {
        val actual = usuario
        if (actual == null) {
            throw ErrorDatos("La sesión terminó. Inicia sesión de nuevo.")
        }
        return actual.id
    }

    fun rol(): String {
        val actual = usuario
        if (actual == null) {
            return ""
        }
        return actual.rol
    }
}
