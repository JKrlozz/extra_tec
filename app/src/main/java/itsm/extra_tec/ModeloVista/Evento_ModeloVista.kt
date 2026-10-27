package itsm.extra_tec.ModeloVista

// valor que se entrega a la vista una sola vez
class Evento_ModeloVista<out T>(private val contenido: T) {
    private var consumido = false

    fun obtener(): T? {
        if (consumido) return null
        consumido = true
        return contenido
    }
}
