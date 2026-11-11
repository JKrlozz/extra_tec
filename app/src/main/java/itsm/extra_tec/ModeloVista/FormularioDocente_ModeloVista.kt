package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioDocente_ModeloVista : Base_ModeloVista() {
    private val _especialidades = MutableLiveData<List<String>>(listOf())
    val especialidades: LiveData<List<String>> = _especialidades

    private val _guardado = MutableLiveData<Evento_ModeloVista<Guardado_ModeloVista>>()
    val guardado: LiveData<Evento_ModeloVista<Guardado_ModeloVista>> = _guardado

    init {
        Personas_Modelo.especialidades { resultado ->
            atender(resultado) { lista -> _especialidades.value = lista }
        }
    }

    fun guardar(
        id: Int?,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        especialidad: String,
        telefono: String,
        contrasena: String,
    ) {
        var error = Limites.revisarPersona(nombres, apellidoPaterno, apellidoMaterno, correo)
        if (error == null) {
            error = Limites.revisarEspecialidad(especialidad)
        }
        if (error == null) {
            error = Limites.revisarTelefono(telefono)
        }
        if (error == null && id == null) {
            error = Limites.revisarClave(contrasena, true)
        }
        if (error != null) {
            avisar(error)
            return
        }
        Personas_Modelo.guardarDocente(
            id, nombres, apellidoPaterno, apellidoMaterno, correo, especialidad, telefono, contrasena,
        ) { resultado ->
            atender(resultado) { clave ->
                avisar("Docente guardado.")
                _guardado.value = Evento_ModeloVista(Guardado_ModeloVista(clave))
            }
        }
    }
}
