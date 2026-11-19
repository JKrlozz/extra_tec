package itsm.extra_tec

import android.app.Application
import itsm.extra_tec.Modelo.Red

// prepara la conexion al iniciar
class ExtraTec_App : Application() {
    override fun onCreate() {
        super.onCreate()
        Red.iniciar(this)
    }
}
