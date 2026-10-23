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
        nombre: String,
        correo: String,
        contrasena: String,
        matricula: String,
        semestreTexto: String,
        posicionCarrera: Int,
    ) {
        val semestre = semestreTexto.toIntOrNull()
        val carrera = _carreras.value.orEmpty().getOrNull(posicionCarrera)
        when {
            nombre.isEmpty() || correo.isEmpty() || matricula.isEmpty() -> return avisar("Completa nombre, correo y matrícula.")
            contrasena.length < 8 -> return avisar("La contraseña debe tener al menos 8 caracteres.")
            semestre == null || semestre !in 1..12 -> return avisar("El semestre debe ser de 1 a 12.")
            carrera == null -> return avisar("Aún no se cargan las carreras.")
        }
        Autenticacion_Modelo.registrar(nombre, correo, contrasena, matricula, carrera!!.id, semestre!!) { resultado ->
            atender(resultado) {
                avisar("Registro realizado. Ya puedes iniciar sesión.")
                _registrado.value = Evento_ModeloVista(Unit)
            }
        }
    }
}
