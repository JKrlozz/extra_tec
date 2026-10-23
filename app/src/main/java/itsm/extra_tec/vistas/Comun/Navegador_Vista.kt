package itsm.extra_tec.vistas.Comun

import android.os.Bundle

interface Navegador_Vista {
    fun ir(pantalla: Pantalla_Vista, argumentos: Bundle? = null)

    // Abre el inicio que corresponde al rol de la sesión actual
    fun irAlInicio()
}
