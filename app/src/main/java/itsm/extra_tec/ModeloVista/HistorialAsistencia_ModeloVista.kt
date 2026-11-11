package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Asistencia_Modelo
import itsm.extra_tec.Modelo.SesionAsistencia

class HistorialAsistencia_ModeloVista : Base_ModeloVista() {
    private val _sesiones = MutableLiveData<List<SesionAsistencia>?>(null)
    val sesiones: LiveData<List<SesionAsistencia>?> = _sesiones

    fun cargar(idActividad: Int) {
        Asistencia_Modelo.historial(idActividad) { resultado ->
            atender(resultado) { lista -> _sesiones.value = lista }
        }
    }
}
