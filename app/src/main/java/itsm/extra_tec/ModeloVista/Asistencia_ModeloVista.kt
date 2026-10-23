package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Asistencia_Modelo
import itsm.extra_tec.Modelo.ListaAsistencia

class Asistencia_ModeloVista : Base_ModeloVista() {
    private val _lista = MutableLiveData<ListaAsistencia?>(null)
    val lista: LiveData<ListaAsistencia?> = _lista

    private val _guardada = MutableLiveData<Evento_ModeloVista<Unit>>()
    val guardada: LiveData<Evento_ModeloVista<Unit>> = _guardada

    private var idActividad: Int = -1

    fun cargar(id: Int) {
        if (idActividad == id && _lista.value != null) return
        idActividad = id
        Asistencia_Modelo.lista(id) { resultado -> atender(resultado) { _lista.value = it } }
    }

    fun marcar(idAlumno: Int, presente: Boolean) {
        val actual = _lista.value ?: return
        _lista.value = actual.copy(alumnos = actual.alumnos.map {
            if (it.idAlumno == idAlumno) it.copy(presente = presente) else it
        })
    }

    fun guardar() {
        val actual = _lista.value
        if (actual == null || actual.alumnos.isEmpty()) return avisar("No hay alumnos para guardar.")
        Asistencia_Modelo.guardar(idActividad, actual.fecha, actual.alumnos) { resultado ->
            atender(resultado) {
                avisar("Asistencia guardada.")
                _guardada.value = Evento_ModeloVista(Unit)
            }
        }
    }
}
