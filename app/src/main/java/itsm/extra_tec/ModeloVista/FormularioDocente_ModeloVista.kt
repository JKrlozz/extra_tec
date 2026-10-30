package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioDocente_ModeloVista : Base_ModeloVista() {
    private val _guardado = MutableLiveData<Evento_ModeloVista<Guardado_ModeloVista>>()
    val guardado: LiveData<Evento_ModeloVista<Guardado_ModeloVista>> = _guardado

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
        if (nombres.isEmpty() || apellidoPaterno.isEmpty() || correo.isEmpty()) {
            return avisar("Escribe nombres, apellido paterno y correo.")
        }
        Personas_Modelo.guardarDocente(
            id, nombres, apellidoPaterno, apellidoMaterno, correo, especialidad, telefono, contrasena,
        ) { resultado ->
            atender(resultado) {
                avisar("Docente guardado.")
                _guardado.value = Evento_ModeloVista(Guardado_ModeloVista(it))
            }
        }
    }
}
