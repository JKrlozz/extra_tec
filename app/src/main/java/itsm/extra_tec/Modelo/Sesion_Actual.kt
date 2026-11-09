package itsm.extra_tec.Modelo

// Usuario autenticado en este momento
// La sesion vive solo en este telefono
object Sesion_Actual {
    @Volatile
    var usuario: Usuario? = null

    fun id(): Int = usuario?.id ?: throw ErrorDatos("La sesión terminó. Inicia sesión de nuevo.")

    fun rol(): String = usuario?.rol.orEmpty()
}
