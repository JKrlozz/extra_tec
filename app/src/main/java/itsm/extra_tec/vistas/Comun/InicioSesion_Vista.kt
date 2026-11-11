package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.InicioSesion_ModeloVista
import itsm.extra_tec.databinding.InicioSesionVistaBinding

class InicioSesion_Vista : Base_Vista() {
    private var _binding: InicioSesionVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: InicioSesion_ModeloVista by viewModels()

    // valor guardado y texto visible
    private val roles = listOf("alumno", "docente", "admin")
    private val nombresRoles = listOf("Alumno", "Docente", "Administrador")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InicioSesionVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        llenarSpinner(binding.spinnerRol, nombresRoles)

        binding.btnIniciarSesion.setOnClickListener {
            val rol = roles[binding.spinnerRol.selectedItemPosition]
            modelo.iniciar(binding.etCorreo.texto(), binding.etContrasena.text.toString(), rol)
        }
        binding.btnIrRegistro.setOnClickListener { navegador.ir(Pantalla_Vista.REGISTRO, null) }

        modelo.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.btnIniciarSesion.isEnabled = !cargando
        }
        modelo.sesionIniciada.observe(viewLifecycleOwner) { evento ->
            val nueva = evento.obtener()
            if (nueva != null) {
                sesion.iniciar(nueva)
                navegador.irAlInicio()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
