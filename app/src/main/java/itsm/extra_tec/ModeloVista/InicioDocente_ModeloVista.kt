package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo

class InicioDocente_ModeloVista : Base_ModeloVista() {
    private val _clases = MutableLiveData<List<Actividad>?>(null)
    val clases: LiveData<List<Actividad>?> = _clases

    init {
        Actividades_Modelo.misClases { resultado -> atender(resultado) { _clases.value = it } }
    }
}
