package itsm.extra_tec.vistas.Comun

import android.os.Bundle

interface Navegador_Vista {
    // avanza y guarda la pantalla anterior
    fun ir(pantalla: Pantalla_Vista, argumentos: Bundle?)

    // abre y borra el historial
    fun irRaiz(pantalla: Pantalla_Vista, argumentos: Bundle?)

    // regresa a la pantalla anterior
    fun atras()

    // abre el inicio segun el rol
    fun irAlInicio()
}
