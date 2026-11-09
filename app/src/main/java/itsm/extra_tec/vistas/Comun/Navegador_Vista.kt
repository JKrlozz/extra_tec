package itsm.extra_tec.vistas.Comun

import android.os.Bundle

interface Navegador_Vista {
    // Avanza y guarda la pantalla en el historial
    fun ir(pantalla: Pantalla_Vista, argumentos: Bundle? = null)

    // Abre una pantalla de inicio y limpia el historial
    fun irRaiz(pantalla: Pantalla_Vista, argumentos: Bundle? = null)

    // Vuelve a la pantalla anterior o al inicio del rol
    fun atras()

    // Abre el inicio del rol
    fun irAlInicio()
}
