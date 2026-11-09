package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.Carrera
import itsm.extra_tec.Modelo.NumeroControl
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
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        matricula: String,
        posicionSemestre: Int,
        posicionCarrera: Int,
        contrasena: String,
    ) {
        val semestre = posicionSemestre + 1
        val carrera = _carreras.value.orEmpty().getOrNull(posicionCarrera)
        val numeroControl = NumeroControl.normalizar(matricula)
        when {
            nombres.isEmpty() || apellidoPaterno.isEmpty() || correo.isEmpty() || numeroControl.isEmpty() ->
                return avisar("Completa nombres, apellido paterno, correo y número de control.")
            !NumeroControl.esValido(numeroControl) -> return avisar(NumeroControl.MENSAJE)
            semestre !in 1..9 -> return avisar("Elige un semestre de primero a noveno.")
            carrera == null -> return avisar("Aún no se cargan las carreras.")
        }
        Personas_Modelo.guardarAlumno(
            id, nombres, apellidoPaterno, apellidoMaterno, correo, numeroControl, carrera!!.id, semestre, contrasena,
        ) { resultado ->
            atender(resultado) {
                avisar("Alumno guardado.")
                _guardado.value = Evento_ModeloVista(Guardado_ModeloVista(it))
            }
        }
    }
}
