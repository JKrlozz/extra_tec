package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.AlumnoInscrito

class AlumnosDocente_ModeloVista : Base_ModeloVista() {
    private val _visibles = MutableLiveData<List<AlumnoInscrito>?>(null)
    val visibles: LiveData<List<AlumnoInscrito>?> = _visibles

    private var todos: List<AlumnoInscrito> = emptyList()
    private var filtro = ""
    private var idActividad = -1

    fun cargar(id: Int) {
        if (idActividad == id) return
        idActividad = id
        Actividades_Modelo.alumnosDeActividad(id) { resultado ->
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

    private fun aplicarFiltro() {
        _visibles.value = todos.filter {
            filtro.isEmpty() || it.nombre.lowercase().contains(filtro) || it.matricula.contains(filtro)
        }
    }
}
