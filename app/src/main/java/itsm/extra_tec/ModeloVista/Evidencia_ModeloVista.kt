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
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.Sesion_Actual

// foto del formulario guardada o nueva
// la guardada tiene id
data class FotoFormulario(
    val id: Int?,
    val uri: Uri?,
    val ruta: String?,
) {
    // texto para poder mostrarla
    val fuente: String
        get() {
            if (uri != null) return uri.toString()
            if (ruta != null) return ruta
            return ""
        }
}

class Evidencia_ModeloVista : Base_ModeloVista() {
    private val _clases = MutableLiveData<List<Actividad>>(listOf())
    val clases: LiveData<List<Actividad>> = _clases

    private val _fotos = MutableLiveData<List<FotoFormulario>>(listOf())
    val fotos: LiveData<List<FotoFormulario>> = _fotos

    private val _evidencia = MutableLiveData<Evidencia?>(null)
    val evidencia: LiveData<Evidencia?> = _evidencia

    private val _guardada = MutableLiveData<Evento_ModeloVista<Boolean>>()
    val guardada: LiveData<Evento_ModeloVista<Boolean>> = _guardada

    private var existentes = listOf<EvidenciaFoto>()
    private var idEvidencia: Int? = null
    private var iniciado = false

    init {
        // el admin puede usar cualquier actividad
        if (Sesion_Actual.rol() == "admin") {
            Actividades_Modelo.todas { resultado -> atender(resultado) { lista -> _clases.value = lista } }
        } else {
            Actividades_Modelo.misClases { resultado -> atender(resultado) { lista -> _clases.value = lista } }
        }
    }

    fun cargar(id: Int?) {
        if (iniciado) {
            return
        }
        iniciado = true
        idEvidencia = id
        if (id == null) {
            return
        }
        Evidencias_Modelo.detalle(id) { resultado ->
            atender(resultado) { evidencia ->
                existentes = evidencia.fotos
                _evidencia.value = evidencia
                val lista = mutableListOf<FotoFormulario>()
                for (foto in evidencia.fotos) {
                    lista.add(FotoFormulario(foto.id, null, foto.ruta))
                }
                _fotos.value = lista
            }
        }
    }

    fun agregarFotos(uris: List<Uri>) {
        val lista = mutableListOf<FotoFormulario>()
        lista.addAll(_fotos.value ?: listOf())
        for (uri in uris) {
            // no se repite la misma foto
            var repetida = false
            for (foto in lista) {
                if (foto.uri == uri) repetida = true
            }
            if (!repetida) {
                lista.add(FotoFormulario(null, uri, null))
            }
        }
        if (lista.size > Limites.FOTOS_MAX) {
            avisar("Puedes seleccionar hasta ${Limites.FOTOS_MAX} fotografías.")
            return
        }
        _fotos.value = lista
    }

    fun quitarFoto(posicion: Int) {
        val lista = mutableListOf<FotoFormulario>()
        lista.addAll(_fotos.value ?: listOf())
        if (posicion >= 0 && posicion < lista.size) {
            lista.removeAt(posicion)
        }
        _fotos.value = lista
    }

    fun guardar(
        lector: ContentResolver,
        posicionClase: Int,
        fecha: String,
        estado: String,
        descripcion: String,
    ) {
        val clases = _clases.value ?: listOf()
        if (posicionClase < 0 || posicionClase >= clases.size) {
            avisar("No hay actividades disponibles.")
            return
        }
        var error = Limites.revisarFecha(fecha)
        if (error == null) {
            error = Limites.revisarDescripcionEvidencia(descripcion)
        }
        if (error != null) {
            avisar(error)
            return
        }

        // se leen las fotos nuevas
        val actuales = _fotos.value ?: listOf()
        val nuevas = mutableListOf<FotoNueva>()
        val conservadas = mutableListOf<Int>()
        for (foto in actuales) {
            if (foto.id != null) {
                conservadas.add(foto.id)
            }
            if (foto.uri != null) {
                val tipo = lector.getType(foto.uri) ?: "image/jpeg"
                var bytes: ByteArray? = null
                try {
                    val entrada = lector.openInputStream(foto.uri)
                    if (entrada != null) {
                        bytes = entrada.readBytes()
                        entrada.close()
                    }
                } catch (e: Exception) {
                    bytes = null
                }
                if (bytes == null) {
                    avisar("No se pudo leer una de las fotografías.")
                    return
                }
                if (bytes.size > Limites.FOTO_BYTES_MAX) {
                    avisar("Una fotografía supera 8 MB.")
                    return
                }
                nuevas.add(FotoNueva("evidencia", tipo, bytes))
            }
        }
        val quitar = mutableListOf<Int>()
        for (foto in existentes) {
            if (!conservadas.contains(foto.id)) {
                quitar.add(foto.id)
            }
        }

        Evidencias_Modelo.guardar(
            clases[posicionClase].id, idEvidencia, fecha, estado, descripcion, nuevas, quitar,
        ) { resultado ->
            atender(resultado) {
                avisar("Evidencia guardada.")
                _guardada.value = Evento_ModeloVista(true)
            }
        }
    }
}
