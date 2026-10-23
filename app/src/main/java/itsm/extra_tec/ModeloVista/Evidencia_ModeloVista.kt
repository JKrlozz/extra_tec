package itsm.extra_tec.ModeloVista

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.Asistencia_Modelo

class Evidencia_ModeloVista : Base_ModeloVista() {
    private val _clases = MutableLiveData<List<Actividad>>(emptyList())
    val clases: LiveData<List<Actividad>> = _clases

    private val _foto = MutableLiveData<Uri?>(null)
    val foto: LiveData<Uri?> = _foto

    private val _guardada = MutableLiveData<Evento_ModeloVista<Unit>>()
    val guardada: LiveData<Evento_ModeloVista<Unit>> = _guardada

    init {
        Actividades_Modelo.misClases { resultado -> atender(resultado) { _clases.value = it } }
    }

    fun elegirFoto(uri: Uri) {
        _foto.value = uri
    }

    fun guardar(lector: ContentResolver, posicionClase: Int, descripcion: String) {
        val clase = _clases.value.orEmpty().getOrNull(posicionClase) ?: return avisar("No tienes clases asignadas.")
        val uri = _foto.value ?: return avisar("Selecciona una fotografía.")
        val mime = lector.getType(uri) ?: "image/jpeg"
        val bytes = runCatching { lector.openInputStream(uri)!!.use { it.readBytes() } }.getOrNull()
            ?: return avisar("No se pudo leer la fotografía.")
        if (bytes.size > LIMITE_BYTES) return avisar("La fotografía supera 8 MB.")
        val extension = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        Asistencia_Modelo.subirEvidencia(clase.id, descripcion, "evidencia.$extension", mime, bytes) { resultado ->
            atender(resultado) {
                avisar("Evidencia guardada.")
                _guardada.value = Evento_ModeloVista(Unit)
            }
        }
    }

    private companion object {
        const val LIMITE_BYTES = 8 * 1024 * 1024
    }
}
