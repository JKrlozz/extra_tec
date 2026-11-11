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
        Actividades_Modelo.constancias { resultado ->
            atender(resultado) { lista -> _constancias.value = lista }
        }
    }

    fun descargar(idActividad: Int, contexto: Context) {
        Actividades_Modelo.descargarConstancia(idActividad) { resultado ->
            atender(resultado) { archivo ->
                try {
                    val ruta = Archivos_Modelo.guardarEnDescargas(contexto, archivo.nombre, "application/pdf", archivo.bytes)
                    avisar("Constancia guardada en $ruta")
                } catch (e: Exception) {
                    avisar("No se pudo guardar el PDF: " + e.message)
                }
            }
        }
    }
}
