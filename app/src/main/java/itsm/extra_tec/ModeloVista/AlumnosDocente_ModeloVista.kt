package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.AlumnoInscrito

class AlumnosDocente_ModeloVista : Base_ModeloVista() {
    private val _visibles = MutableLiveData<List<AlumnoInscrito>?>(null)
    val visibles: LiveData<List<AlumnoInscrito>?> = _visibles

    private var todos = listOf<AlumnoInscrito>()
    private var filtro = ""
    private var idActividad = -1

    fun cargar(id: Int) {
        if (idActividad == id) {
            return
        }
        idActividad = id
        Actividades_Modelo.alumnosDeActividad(id) { resultado ->
            atender(resultado) { lista ->
                todos = lista
                filtrar()
            }
        }
    }

    fun buscar(texto: String) {
        filtro = texto.trim().lowercase()
        filtrar()
    }

    // busca por nombre o control
    private fun filtrar() {
        val lista = mutableListOf<AlumnoInscrito>()
        for (alumno in todos) {
            val coincide = alumno.nombre.lowercase().contains(filtro) ||
                alumno.matricula.lowercase().contains(filtro)
            if (filtro.isEmpty() || coincide) {
                lista.add(alumno)
            }
        }
        _visibles.value = lista
    }
}
