package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Docente
import itsm.extra_tec.Modelo.Personas_Modelo

class GestionarDocentes_ModeloVista : Base_ModeloVista() {
    private val _visibles = MutableLiveData<List<Docente>?>(null)
    val visibles: LiveData<List<Docente>?> = _visibles

    private var todos: List<Docente> = emptyList()
    private var filtro = ""

    init {
        cargar()
    }

    fun cargar() {
        Personas_Modelo.docentes { resultado ->
            atender(resultado) {
                todos = it
                aplicarFiltro()
            }
        }
    }

    fun buscar(texto: String) {
        filtro = texto.trim().lowercase()
        aplicarFiltro()
    }

    fun eliminar(id: Int) {
        Personas_Modelo.eliminarDocente(id) { resultado ->
            atender(resultado) {
                avisar("Docente eliminado.")
                cargar()
            }
        }
    }

    private fun aplicarFiltro() {
        _visibles.value = todos.filter { filtro.isEmpty() || it.nombre.lowercase().contains(filtro) }
    }
}
