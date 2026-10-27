package itsm.extra_tec.ModeloVista

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Alumno
import itsm.extra_tec.Modelo.Personas_Modelo

class GestionarAlumnos_ModeloVista : Base_ModeloVista() {
    private val _alumnos = MutableLiveData<List<Alumno>?>(null)
    val alumnos: LiveData<List<Alumno>?> = _alumnos

    private val ui = Handler(Looper.getMainLooper())
    private var pendiente: Runnable? = null
    private var filtro = ""

    init {
        cargar()
    }

    fun cargar() {
        Personas_Modelo.alumnos(filtro) { resultado -> atender(resultado) { _alumnos.value = it } }
    }

    // espera 300 ms tras la última tecla antes de consultar
    fun buscar(texto: String) {
        if (texto == filtro) return
        filtro = texto
        pendiente?.let { ui.removeCallbacks(it) }
        pendiente = Runnable { cargar() }.also { ui.postDelayed(it, 300) }
    }

    fun eliminar(id: Int) {
        Personas_Modelo.eliminarAlumno(id) { resultado ->
            atender(resultado) {
                avisar("Alumno eliminado.")
                cargar()
            }
        }
    }

    override fun onCleared() {
        pendiente?.let { ui.removeCallbacks(it) }
    }
}
