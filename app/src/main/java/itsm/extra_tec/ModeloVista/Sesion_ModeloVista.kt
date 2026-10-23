package itsm.extra_tec.ModeloVista

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Api_Modelo
import itsm.extra_tec.Modelo.Descubrimiento_Modelo
import itsm.extra_tec.Modelo.Resultado_Modelo
import itsm.extra_tec.Modelo.SesionIniciada
import itsm.extra_tec.Modelo.Sesion_Modelo
import itsm.extra_tec.Modelo.Usuario

// Compartido por toda la app: usuario actual y clase seleccionada por el docente
class Sesion_ModeloVista(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val almacen = Sesion_Modelo(aplicacion)

    var usuario: Usuario? = null
        private set
    val rol: String get() = usuario?.rol.orEmpty()

    var claseId: Int? = null
    var claseNombre: String = ""

    private val _servidor = MutableLiveData("")
    val servidor: LiveData<String> = _servidor
    private var buscando = false

    init {
        almacen.urlServidor()?.let { Api_Modelo.baseUrl = it }
    }

    // Busca la API en la red y guarda la dirección encontrada; ignora la llamada si ya hay una búsqueda
    fun buscarServidor() {
        if (buscando) return
        buscando = true
        _servidor.value = "Buscando el servidor en la red…"
        Descubrimiento_Modelo.buscar(getApplication(), Api_Modelo.baseUrl) { resultado ->
            buscando = false
            when (resultado) {
                is Resultado_Modelo.Exito -> {
                    guardarUrl(resultado.valor)
                    _servidor.value = "Conectado a ${resultado.valor}"
                }
                is Resultado_Modelo.Fallo -> _servidor.value = resultado.mensaje
            }
        }
    }

    fun restaurar(): Boolean {
        val guardada = almacen.leer() ?: return false
        Api_Modelo.token = guardada.token
        usuario = guardada.usuario
        return true
    }

    fun iniciar(sesion: SesionIniciada) {
        Api_Modelo.token = sesion.token
        usuario = sesion.usuario
        almacen.guardar(sesion)
    }

    fun cerrar() {
        almacen.borrar()
        Api_Modelo.token = null
        usuario = null
        claseId = null
        claseNombre = ""
    }

    fun urlServidor(): String = Api_Modelo.baseUrl

    fun guardarUrl(url: String) {
        Api_Modelo.baseUrl = url
        almacen.guardarUrlServidor(url)
    }
}
