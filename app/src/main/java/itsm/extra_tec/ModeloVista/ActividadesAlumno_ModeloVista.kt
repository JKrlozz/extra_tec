package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo

class ActividadesAlumno_ModeloVista : Base_ModeloVista() {
    private val _actividades = MutableLiveData<List<Actividad>?>(null)
    val actividades: LiveData<List<Actividad>?> = _actividades

    init {
        cargar()
    }

    fun cargar() {
        Actividades_Modelo.listar { resultado -> atender(resultado) { _actividades.value = it } }
    }

    fun inscribir(idActividad: Int) {
        Actividades_Modelo.inscribir(idActividad) { resultado ->
            atender(resultado) {
                avisar("Inscripción realizada.")
                cargar()
            }
        }
    }

    fun darDeBaja(idActividad: Int) {
        Actividades_Modelo.darDeBaja(idActividad) { resultado ->
            atender(resultado) {
                avisar("Baja realizada.")
                cargar()
            }
        }
    }
}
