package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import itsm.extra_tec.Modelo.Resultado_Modelo

// resultado de guardar; sin clave inicial si el administrador la escribió
data class Guardado_ModeloVista(val claveInicial: String?)

open class Base_ModeloVista : ViewModel() {
    private val _mensaje = MutableLiveData<Evento_ModeloVista<String>>()
    val mensaje: LiveData<Evento_ModeloVista<String>> = _mensaje

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    protected fun avisar(texto: String) {
        _mensaje.value = Evento_ModeloVista(texto)
    }

    protected fun marcarCargando(valor: Boolean) {
        _cargando.value = valor
    }

    // ejecuta la acción si hay éxito o muestra el error
    protected fun <T> atender(resultado: Resultado_Modelo<T>, alExito: (T) -> Unit) {
        when (resultado) {
            is Resultado_Modelo.Exito -> alExito(resultado.valor)
            is Resultado_Modelo.Fallo -> avisar(resultado.mensaje)
        }
    }
}
