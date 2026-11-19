package itsm.extra_tec.ModeloVista

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import itsm.extra_tec.Modelo.Red
import itsm.extra_tec.Modelo.SesionIniciada
import itsm.extra_tec.Modelo.Sesion_Actual
import itsm.extra_tec.Modelo.Sesion_Modelo
import itsm.extra_tec.Modelo.Usuario

// usuario actual y clase elegida
class Sesion_ModeloVista(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val almacen = Sesion_Modelo(aplicacion)

    var usuario: Usuario? = null
        private set

    var claseId: Int? = null
    var claseNombre = ""

    val rol: String
        get() {
            val actual = usuario
            if (actual == null) return ""
            return actual.rol
        }

    // regresa true si habia sesion guardada
    fun restaurar(): Boolean {
        val guardada = almacen.leer()
        if (guardada == null) {
            return false
        }
        usuario = guardada.usuario
        Sesion_Actual.usuario = guardada.usuario
        Sesion_Actual.token = guardada.token
        return true
    }

    fun iniciar(sesion: SesionIniciada) {
        usuario = sesion.usuario
        Sesion_Actual.usuario = sesion.usuario
        Sesion_Actual.token = sesion.token
        almacen.guardar(sesion)
    }

    fun cerrar() {
        almacen.borrar()
        Sesion_Actual.usuario = null
        Sesion_Actual.token = null
        Red.limpiarFotos()
        usuario = null
        claseId = null
        claseNombre = ""
    }
}
