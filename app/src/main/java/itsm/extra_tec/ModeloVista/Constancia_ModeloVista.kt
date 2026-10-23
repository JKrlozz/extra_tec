package itsm.extra_tec.ModeloVista

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.Archivos_Modelo
import itsm.extra_tec.Modelo.Constancia

class Constancia_ModeloVista : Base_ModeloVista() {
    private val _constancias = MutableLiveData<List<Constancia>?>(null)
    val constancias: LiveData<List<Constancia>?> = _constancias

    init {
        Actividades_Modelo.constancias { resultado -> atender(resultado) { _constancias.value = it } }
    }

    fun descargar(idActividad: Int, contexto: Context) {
        Actividades_Modelo.descargarConstancia(idActividad) { resultado ->
            atender(resultado) { archivo ->
                runCatching { Archivos_Modelo.guardarEnDescargas(contexto, archivo.nombre, "application/pdf", archivo.bytes) }
                    .onSuccess { avisar("Constancia guardada en $it") }
                    .onFailure { avisar("No se pudo guardar el PDF: ${it.message}") }
            }
        }
    }
}
