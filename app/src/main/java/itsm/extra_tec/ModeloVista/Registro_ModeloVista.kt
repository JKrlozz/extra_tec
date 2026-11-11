package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.Carrera
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.NumeroControl

class Registro_ModeloVista : Base_ModeloVista() {
    private val _carreras = MutableLiveData<List<Carrera>>(listOf())
    val carreras: LiveData<List<Carrera>> = _carreras

    private val _registrado = MutableLiveData<Evento_ModeloVista<Boolean>>()
    val registrado: LiveData<Evento_ModeloVista<Boolean>> = _registrado

    init {
        Autenticacion_Modelo.carreras { resultado ->
            atender(resultado) { lista -> _carreras.value = lista }
        }
    }

    fun registrar(
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        contrasena: String,
        matricula: String,
        posicionSemestre: Int,
        posicionCarrera: Int,
    ) {
        val semestre = posicionSemestre + 1
        val carreras = _carreras.value ?: listOf()
        var error = Limites.revisarPersona(nombres, apellidoPaterno, apellidoMaterno, correo)
        if (error == null) {
            error = Limites.revisarClave(contrasena, false)
        }
        if (error == null && !NumeroControl.esValido(matricula)) {
            error = NumeroControl.MENSAJE
        }
        if (error == null) {
            error = Limites.revisarCorreoAlumno(correo, matricula)
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
        Autenticacion_Modelo.registrar(
            nombres, apellidoPaterno, apellidoMaterno, correo, contrasena, matricula,
            carreras[posicionCarrera].id, semestre,
        ) { resultado ->
            atender(resultado) {
                avisar("Registro realizado. Ya puedes iniciar sesión.")
                _registrado.value = Evento_ModeloVista(true)
            }
        }
    }
}
