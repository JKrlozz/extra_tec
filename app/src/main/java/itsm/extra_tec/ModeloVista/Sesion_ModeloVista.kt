package itsm.extra_tec.ModeloVista

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import itsm.extra_tec.Modelo.SesionIniciada
import itsm.extra_tec.Modelo.Sesion_Actual
import itsm.extra_tec.Modelo.Sesion_Modelo
import itsm.extra_tec.Modelo.Usuario

// Usuario actual y clase elegida para toda la app
class Sesion_ModeloVista(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val almacen = Sesion_Modelo(aplicacion)

    var usuario: Usuario? = null
        private set
    val rol: String get() = usuario?.rol.orEmpty()

    var claseId: Int? = null
    var claseNombre: String = ""

    fun restaurar(): Boolean {
        val guardada = almacen.leer() ?: return false
        usuario = guardada.usuario
        Sesion_Actual.usuario = guardada.usuario
        return true
    }

    fun iniciar(sesion: SesionIniciada) {
        usuario = sesion.usuario
        Sesion_Actual.usuario = sesion.usuario
        almacen.guardar(sesion)
    }

    fun cerrar() {
        almacen.borrar()
        Sesion_Actual.usuario = null
        usuario = null
        claseId = null
        claseNombre = ""
    }
}
