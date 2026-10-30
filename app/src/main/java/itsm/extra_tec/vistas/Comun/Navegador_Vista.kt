package itsm.extra_tec.vistas.Comun

import android.os.Bundle

interface Navegador_Vista {
    // avanza y guarda la pantalla actual en el historial
    fun ir(pantalla: Pantalla_Vista, argumentos: Bundle? = null)

    // abre una pantalla de inicio y limpia el historial
    fun irRaiz(pantalla: Pantalla_Vista, argumentos: Bundle? = null)

    // vuelve a la pantalla anterior; si no hay historial, va al inicio del rol
    fun atras()

    // abre el inicio que corresponde al rol de la sesión
    fun irAlInicio()
}
