package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.EstadoSemestre
import itsm.extra_tec.Modelo.Semestre_Modelo

class Semestre_ModeloVista : Base_ModeloVista() {
    private val _estado = MutableLiveData<EstadoSemestre?>(null)
    val estado: LiveData<EstadoSemestre?> = _estado

    init {
        Semestre_Modelo.estado { resultado ->
            atender(resultado) { _estado.value = it }
        }
    }

    fun guardar(inicio: String, fin: String) {
        val habiaActivo = _estado.value?.activo != null
        marcarCargando(true)
        Semestre_Modelo.guardar(inicio, fin) { resultado ->
            marcarCargando(false)
            atender(resultado) { nuevo ->
                _estado.value = nuevo
                if (habiaActivo) {
                    avisar("Fechas del semestre guardadas.")
                } else {
                    avisar("Semestre iniciado. Actividades copiadas del semestre anterior: " + nuevo.copiadas + ".")
                }
            }
        }
    }

    fun cerrar() {
        marcarCargando(true)
        Semestre_Modelo.cerrar { resultado ->
            marcarCargando(false)
            atender(resultado) { nuevo ->
                _estado.value = nuevo
                avisar("Semestre cerrado y archivado.")
            }
        }
    }
}
