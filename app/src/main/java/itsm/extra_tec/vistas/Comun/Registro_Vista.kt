package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Registro_ModeloVista
import itsm.extra_tec.databinding.RegistroVistaBinding

class Registro_Vista : Base_Vista<RegistroVistaBinding>() {
    private val modelo: Registro_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        RegistroVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        modelo.carreras.observe(viewLifecycleOwner) { llenarSpinner(binding.spinnerCarrera, it) { carrera -> carrera.nombre } }
        modelo.registrado.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.ir(Pantalla_Vista.INICIO_SESION) }
        }

        binding.btnRegistrarse.setOnClickListener {
            modelo.registrar(
                nombre = binding.etNombre.texto(),
                correo = binding.etCorreo.texto(),
                contrasena = binding.etContrasena.text.toString(),
                matricula = binding.etMatricula.texto(),
                semestreTexto = binding.etSemestre.texto(),
                posicionCarrera = binding.spinnerCarrera.selectedItemPosition,
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.ir(Pantalla_Vista.INICIO_SESION) }
    }
}
