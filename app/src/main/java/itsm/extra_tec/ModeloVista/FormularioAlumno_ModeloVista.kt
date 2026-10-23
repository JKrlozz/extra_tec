package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.Carrera
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioAlumno_ModeloVista : Base_ModeloVista() {
    private val _carreras = MutableLiveData<List<Carrera>>(emptyList())
    val carreras: LiveData<List<Carrera>> = _carreras

    private val _guardado = MutableLiveData<Evento_ModeloVista<Guardado_ModeloVista>>()
    val guardado: LiveData<Evento_ModeloVista<Guardado_ModeloVista>> = _guardado

    init {
        Autenticacion_Modelo.carreras { resultado -> atender(resultado) { _carreras.value = it } }
    }

    fun guardar(
        id: Int?,
        nombre: String,
        correo: String,
        matricula: String,
        semestreTexto: String,
        posicionCarrera: Int,
        contrasena: String,
    ) {
        val semestre = semestreTexto.toIntOrNull()
        val carrera = _carreras.value.orEmpty().getOrNull(posicionCarrera)
        when {
            nombre.isEmpty() || correo.isEmpty() || matricula.isEmpty() -> return avisar("Completa nombre, correo y matrícula.")
            semestre == null || semestre !in 1..12 -> return avisar("El semestre debe ser de 1 a 12.")
            carrera == null -> return avisar("Aún no se cargan las carreras.")
        }
        Personas_Modelo.guardarAlumno(id, nombre, correo, matricula, carrera!!.id, semestre!!, contrasena) { resultado ->
            atender(resultado) {
                avisar("Alumno guardado.")
                _guardado.value = Evento_ModeloVista(Guardado_ModeloVista(it))
            }
        }
    }
}
