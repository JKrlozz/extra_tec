package itsm.extra_tec.Modelo

sealed class Resultado_Modelo<out T> {
    data class Exito<T>(val valor: T) : Resultado_Modelo<T>()
    data class Fallo(val mensaje: String) : Resultado_Modelo<Nothing>()
}
