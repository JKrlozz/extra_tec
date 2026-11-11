package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Registro_ModeloVista
import itsm.extra_tec.databinding.RegistroVistaBinding

class Registro_Vista : Base_Vista() {
    private var _binding: RegistroVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: Registro_ModeloVista by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = RegistroVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        filtrarNumeroControl(binding.etMatricula)
        llenarSpinner(binding.spinnerSemestre, semestres)
        modelo.carreras.observe(viewLifecycleOwner) { carreras ->
            val nombres = mutableListOf<String>()
            for (carrera in carreras) {
                nombres.add(carrera.nombre)
            }
            llenarSpinner(binding.spinnerCarrera, nombres)
        }
        modelo.registrado.observe(viewLifecycleOwner) { evento ->
            if (evento.obtener() != null) {
                navegador.irRaiz(Pantalla_Vista.INICIO_SESION, null)
            }
        }

        binding.btnRegistrarse.setOnClickListener {
            modelo.registrar(
                binding.etNombres.texto(),
                binding.etApellidoPaterno.texto(),
                binding.etApellidoMaterno.texto(),
                binding.etCorreo.texto(),
                binding.etContrasena.text.toString(),
                binding.etMatricula.texto(),
                binding.spinnerSemestre.selectedItemPosition,
                binding.spinnerCarrera.selectedItemPosition,
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
