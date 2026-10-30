package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.Carrera

class Registro_ModeloVista : Base_ModeloVista() {
    private val _carreras = MutableLiveData<List<Carrera>>(emptyList())
    val carreras: LiveData<List<Carrera>> = _carreras

    private val _registrado = MutableLiveData<Evento_ModeloVista<Unit>>()
    val registrado: LiveData<Evento_ModeloVista<Unit>> = _registrado

    init {
        Autenticacion_Modelo.carreras { resultado -> atender(resultado) { _carreras.value = it } }
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
        val carrera = _carreras.value.orEmpty().getOrNull(posicionCarrera)
        when {
            nombres.isEmpty() || apellidoPaterno.isEmpty() || correo.isEmpty() || matricula.isEmpty() ->
                return avisar("Completa nombres, apellido paterno, correo y número de control.")
            contrasena.length < 8 -> return avisar("La contraseña debe tener al menos 8 caracteres.")
            semestre !in 1..9 -> return avisar("Elige un semestre de primero a noveno.")
            carrera == null -> return avisar("Aún no se cargan las carreras.")
        }
        Autenticacion_Modelo.registrar(
            nombres, apellidoPaterno, apellidoMaterno, correo, contrasena, matricula, carrera!!.id, semestre,
        ) { resultado ->
            atender(resultado) {
                avisar("Registro realizado. Ya puedes iniciar sesión.")
                _registrado.value = Evento_ModeloVista(Unit)
            }
        }
    }
}
