package itsm.extra_tec.ModeloVista

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.Evidencia
import itsm.extra_tec.Modelo.EvidenciaFoto
import itsm.extra_tec.Modelo.Evidencias_Modelo
import itsm.extra_tec.Modelo.FotoNueva
import itsm.extra_tec.Modelo.Sesion_Actual
import java.util.UUID

class Evidencia_ModeloVista : Base_ModeloVista() {
    // Foto del formulario ya guardada o recien elegida
    data class FotoFormulario(
        val clave: String,
        val id: Int? = null,
        val uri: Uri? = null,
        val ruta: String? = null,
    )

    private val _clases = MutableLiveData<List<Actividad>>(emptyList())
    val clases: LiveData<List<Actividad>> = _clases

    private val _fotos = MutableLiveData<List<FotoFormulario>>(emptyList())
    val fotos: LiveData<List<FotoFormulario>> = _fotos

    private val _evidencia = MutableLiveData<Evidencia?>(null)
    val evidencia: LiveData<Evidencia?> = _evidencia

    private val _guardada = MutableLiveData<Evento_ModeloVista<Unit>>()
    val guardada: LiveData<Evento_ModeloVista<Unit>> = _guardada

    private var existentes: List<EvidenciaFoto> = emptyList()
    private var idEvidencia: Int? = null
    private var iniciado = false

    init {
        // El administrador puede registrar evidencias de cualquier actividad
        // El docente solo de sus actividades
        val cargar = if (Sesion_Actual.rol() == "admin") {
            Actividades_Modelo::todas
        } else {
            Actividades_Modelo::misClases
        }
        cargar { resultado -> atender(resultado) { _clases.value = it } }
    }

    fun cargar(id: Int?) {
        if (iniciado) return
        iniciado = true
        idEvidencia = id
        if (id == null) return
        Evidencias_Modelo.detalle(id) { resultado ->
            atender(resultado) { evidencia ->
                existentes = evidencia.fotos
                _evidencia.value = evidencia
                _fotos.value = evidencia.fotos.map {
                    FotoFormulario("e:${it.id}", id = it.id, ruta = it.ruta)
                }
            }
        }
    }

    fun agregarFotos(uris: List<Uri>) {
        val actuales = _fotos.value.orEmpty()
        val yaElegidas = actuales.mapNotNull { it.uri }.toSet()
        val nuevas = uris.filter { it !in yaElegidas }
        if (actuales.size + nuevas.size > Evidencias_Modelo.MAX_FOTOS) {
            return avisar("Puedes seleccionar hasta ${Evidencias_Modelo.MAX_FOTOS} fotografías.")
        }
        _fotos.value = actuales + nuevas.map {
            FotoFormulario("n:${UUID.randomUUID()}", uri = it)
        }
    }

    fun quitarFoto(clave: String) {
        _fotos.value = _fotos.value.orEmpty().filterNot { it.clave == clave }
    }

    fun guardar(
        lector: ContentResolver,
        posicionClase: Int,
        fecha: String,
        estado: String,
        descripcion: String,
    ) {
        val clase = _clases.value.orEmpty().getOrNull(posicionClase)
            ?: return avisar("No hay actividades disponibles.")
        val actuales = _fotos.value.orEmpty()
        val nuevas = mutableListOf<FotoNueva>()
        for (foto in actuales.filter { it.uri != null }) {
            val uri = foto.uri!!
            val mime = lector.getType(uri) ?: "image/jpeg"
            val bytes = runCatching { lector.openInputStream(uri)!!.use { it.readBytes() } }.getOrNull()
                ?: return avisar("No se pudo leer una de las fotografías.")
            val extension = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                else -> "jpg"
            }
            nuevas.add(FotoNueva("evidencia.$extension", mime, bytes))
        }
        val conservadas = actuales.mapNotNull { it.id }.toSet()
        val quitar = existentes.map { it.id }.filterNot { it in conservadas }
        Evidencias_Modelo.guardar(
            clase.id, idEvidencia, fecha, estado, descripcion, nuevas, quitar,
        ) { resultado ->
            atender(resultado) {
                avisar("Evidencia guardada.")
                _guardada.value = Evento_ModeloVista(Unit)
            }
        }
    }
}
