package itsm.extra_tec

import android.app.Application
import itsm.extra_tec.Modelo.BaseDatos

// Prepara la base local al iniciar
class ExtraTec_App : Application() {
    override fun onCreate() {
        super.onCreate()
        BaseDatos.iniciar(this)
    }
}
