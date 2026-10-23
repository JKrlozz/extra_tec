package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo

class GestionarActividades_ModeloVista : Base_ModeloVista() {
    private val _actividades = MutableLiveData<List<Actividad>?>(null)
    val actividades: LiveData<List<Actividad>?> = _actividades

    init {
        cargar()
    }

    fun cargar() {
        Actividades_Modelo.listar { resultado -> atender(resultado) { _actividades.value = it } }
    }

    fun eliminar(id: Int) {
        Actividades_Modelo.eliminar(id) { resultado ->
            atender(resultado) {
                avisar("Actividad eliminada.")
                cargar()
            }
        }
    }
}
