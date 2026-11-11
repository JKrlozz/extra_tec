package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Alumno
import itsm.extra_tec.Modelo.Docente
import itsm.extra_tec.Modelo.Personas_Modelo

// alumnos y docentes en una pantalla
class Personas_ModeloVista : Base_ModeloVista() {
    private val _alumnos = MutableLiveData<List<Alumno>?>(null)
    val alumnos: LiveData<List<Alumno>?> = _alumnos

    private val _docentes = MutableLiveData<List<Docente>?>(null)
    val docentes: LiveData<List<Docente>?> = _docentes

    private var filtro = ""

    init {
        recargar()
    }

    fun buscar(texto: String) {
        if (texto.trim() == filtro) {
            return
        }
        filtro = texto.trim()
        recargar()
    }

    fun eliminarAlumno(id: Int) {
        Personas_Modelo.eliminarAlumno(id) { resultado ->
            atender(resultado) {
                avisar("Alumno dado de baja.")
                recargar()
            }
        }
    }

    fun eliminarDocente(id: Int) {
        Personas_Modelo.eliminarDocente(id) { resultado ->
            atender(resultado) {
                avisar("Docente dado de baja.")
                recargar()
            }
        }
    }

    private fun recargar() {
        Personas_Modelo.alumnos(filtro) { resultado ->
            atender(resultado) { lista -> _alumnos.value = lista }
        }
        Personas_Modelo.docentes(filtro) { resultado ->
            atender(resultado) { lista -> _docentes.value = lista }
        }
    }
}
