package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.Carrera
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.NumeroControl
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioAlumno_ModeloVista : Base_ModeloVista() {
    private val _carreras = MutableLiveData<List<Carrera>>(listOf())
    val carreras: LiveData<List<Carrera>> = _carreras

    private val _guardado = MutableLiveData<Evento_ModeloVista<Guardado_ModeloVista>>()
    val guardado: LiveData<Evento_ModeloVista<Guardado_ModeloVista>> = _guardado

    init {
        Autenticacion_Modelo.carreras { resultado ->
            atender(resultado) { lista -> _carreras.value = lista }
        }
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
        val carreras = _carreras.value ?: listOf()
        var error = Limites.revisarPersona(nombres, apellidoPaterno, apellidoMaterno, correo)
        if (error == null && !NumeroControl.esValido(matricula)) {
            error = NumeroControl.MENSAJE
        }
        if (error == null) {
            error = Limites.revisarCorreoAlumno(correo, matricula)
        }
        // al editar no cambia la clave
        if (error == null && id == null) {
            error = Limites.revisarClave(contrasena, true)
        }
        if (error == null && (semestre < 1 || semestre > Limites.SEMESTRE_MAX)) {
            error = "Elige un semestre de primero a octavo."
        }
        if (error == null && (posicionCarrera < 0 || posicionCarrera >= carreras.size)) {
            error = "Aún no se cargan las carreras."
        }
        if (error != null) {
            avisar(error)
            return
        }
        Personas_Modelo.guardarAlumno(
            id, nombres, apellidoPaterno, apellidoMaterno, correo, matricula,
            carreras[posicionCarrera].id, semestre, contrasena,
        ) { resultado ->
            atender(resultado) { clave ->
                avisar("Alumno guardado.")
                _guardado.value = Evento_ModeloVista(Guardado_ModeloVista(clave))
            }
        }
    }
}
