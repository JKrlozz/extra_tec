package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioDocente_ModeloVista : Base_ModeloVista() {
    private val _guardado = MutableLiveData<Evento_ModeloVista<Guardado_ModeloVista>>()
    val guardado: LiveData<Evento_ModeloVista<Guardado_ModeloVista>> = _guardado

    fun guardar(
        id: Int?,
        nombre: String,
        correo: String,
        especialidad: String,
        telefono: String,
        contrasena: String,
    ) {
        if (nombre.isEmpty() || correo.isEmpty()) return avisar("Escribe nombre y correo.")
        Personas_Modelo.guardarDocente(id, nombre, correo, especialidad, telefono, contrasena) { resultado ->
            atender(resultado) {
                avisar("Docente guardado.")
                _guardado.value = Evento_ModeloVista(Guardado_ModeloVista(it))
            }
        }
    }
}
