package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Evidencia
import itsm.extra_tec.Modelo.Evidencias_Modelo

class HistorialEvidencia_ModeloVista : Base_ModeloVista() {
    private val _evidencias = MutableLiveData<List<Evidencia>?>(null)
    val evidencias: LiveData<List<Evidencia>?> = _evidencias

    private var idActividad = -1

    fun cargar(id: Int) {
        idActividad = id
        Evidencias_Modelo.historial(id) { resultado -> atender(resultado) { _evidencias.value = it } }
    }

    fun eliminar(id: Int) {
        Evidencias_Modelo.eliminar(id) { resultado ->
            atender(resultado) {
                avisar("Evidencia eliminada.")
                cargar(idActividad)
            }
        }
    }
}
