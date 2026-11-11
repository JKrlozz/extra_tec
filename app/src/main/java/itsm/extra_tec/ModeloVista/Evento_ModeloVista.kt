package itsm.extra_tec.ModeloVista

// valor que se usa una vez
class Evento_ModeloVista<T>(private val contenido: T) {
    private var usado = false

    fun obtener(): T? {
        if (usado) {
            return null
        }
        usado = true
        return contenido
    }
}
