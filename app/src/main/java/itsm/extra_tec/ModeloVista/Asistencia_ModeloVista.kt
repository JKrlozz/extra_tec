package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.Asistencia_Modelo
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.ListaAsistencia
import itsm.extra_tec.Modelo.MarcaAsistencia
import itsm.extra_tec.Modelo.Sesion_Actual

class Asistencia_ModeloVista : Base_ModeloVista() {
    private val _lista = MutableLiveData<ListaAsistencia?>(null)
    val lista: LiveData<ListaAsistencia?> = _lista

    private val _clases = MutableLiveData<List<Actividad>>(listOf())
    val clases: LiveData<List<Actividad>> = _clases

    private val _guardada = MutableLiveData<Evento_ModeloVista<Boolean>>()
    val guardada: LiveData<Evento_ModeloVista<Boolean>> = _guardada

    private var idActividad = -1

    init {
        // el docente solo ve las suyas
        if (Sesion_Actual.rol() == "admin") {
            Actividades_Modelo.todas { resultado -> atender(resultado) { lista -> _clases.value = lista } }
        } else {
            Actividades_Modelo.misClases { resultado -> atender(resultado) { lista -> _clases.value = lista } }
        }
    }

    fun cargar(id: Int, fecha: String) {
        val error = Limites.revisarFecha(fecha)
        if (error != null) {
            avisar(error)
            return
        }
        idActividad = id
        _lista.value = null
        Asistencia_Modelo.lista(id, fecha) { resultado ->
            atender(resultado) { lista -> _lista.value = lista }
        }
    }

    fun marcar(idAlumno: Int, presente: Boolean) {
        val actual = _lista.value
        if (actual == null) {
            return
        }
        val alumnos = mutableListOf<MarcaAsistencia>()
        for (alumno in actual.alumnos) {
            if (alumno.idAlumno == idAlumno) {
                alumnos.add(alumno.copy(presente = presente))
            } else {
                alumnos.add(alumno)
            }
        }
        _lista.value = ListaAsistencia(actual.fecha, alumnos)
    }

    fun guardar() {
        val actual = _lista.value
        if (actual == null || actual.alumnos.isEmpty()) {
            avisar("No hay alumnos para guardar.")
            return
        }
        Asistencia_Modelo.guardar(idActividad, actual.fecha, actual.alumnos) { resultado ->
            atender(resultado) {
                avisar("Asistencia guardada.")
                _guardada.value = Evento_ModeloVista(true)
            }
        }
    }
}
