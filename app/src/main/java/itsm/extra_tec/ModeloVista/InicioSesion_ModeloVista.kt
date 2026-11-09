package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.SesionIniciada

class InicioSesion_ModeloVista : Base_ModeloVista() {
    private val _sesionIniciada = MutableLiveData<Evento_ModeloVista<SesionIniciada>>()
    val sesionIniciada: LiveData<Evento_ModeloVista<SesionIniciada>> = _sesionIniciada

    fun iniciar(correo: String, contrasena: String, rol: String) {
        if (correo.isEmpty() || contrasena.isEmpty()) return avisar("Escribe tu correo y contraseña.")
        marcarCargando(true)
        Autenticacion_Modelo.iniciarSesion(correo, contrasena, rol) { resultado ->
            marcarCargando(false)
            atender(resultado) { _sesionIniciada.value = Evento_ModeloVista(it) }
        }
    }
}
