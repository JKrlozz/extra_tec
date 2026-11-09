package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.InicioSesion_ModeloVista
import itsm.extra_tec.databinding.InicioSesionVistaBinding

class InicioSesion_Vista : Base_Vista<InicioSesionVistaBinding>() {
    private val modelo: InicioSesion_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        InicioSesionVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        llenarSpinner(binding.spinnerRol, listOf("alumno", "docente", "admin")) { it }

        binding.btnIniciarSesion.setOnClickListener {
            modelo.iniciar(
                binding.etCorreo.texto(),
                binding.etContrasena.text.toString(),
                binding.spinnerRol.selectedItem.toString(),
            )
        }
        binding.btnIrRegistro.setOnClickListener { navegador.ir(Pantalla_Vista.REGISTRO) }

        modelo.cargando.observe(viewLifecycleOwner) { binding.btnIniciarSesion.isEnabled = !it }
        modelo.sesionIniciada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let {
                sesion.iniciar(it)
                navegador.irAlInicio()
            }
        }
    }
}
