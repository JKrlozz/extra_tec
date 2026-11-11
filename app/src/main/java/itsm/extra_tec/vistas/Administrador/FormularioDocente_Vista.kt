package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Docente
import itsm.extra_tec.ModeloVista.FormularioDocente_ModeloVista
import itsm.extra_tec.databinding.FormularioDocenteVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.texto

class FormularioDocente_Vista : Base_Vista() {
    private var _binding: FormularioDocenteVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: FormularioDocente_ModeloVista by viewModels()

    // sin id es docente nuevo
    private var idDocente: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FormularioDocenteVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val datos = arguments
        if (datos != null && datos.getInt(ARG_ID, -1) > 0) {
            idDocente = datos.getInt(ARG_ID)
            binding.tvTituloForm.text = "Editar docente"
            binding.tilContrasena.visibility = View.GONE
            if (savedInstanceState == null) {
                binding.etNombres.setText(datos.getString(ARG_NOMBRES))
                binding.etApellidoPaterno.setText(datos.getString(ARG_PATERNO))
                binding.etApellidoMaterno.setText(datos.getString(ARG_MATERNO))
                binding.etCorreo.setText(datos.getString(ARG_CORREO))
                binding.etEspecialidad.setText(datos.getString(ARG_ESPECIALIDAD))
                binding.etTelefono.setText(datos.getString(ARG_TELEFONO))
            }
        } else {
            binding.tvTituloForm.text = "Registrar docente"
        }

        // sugerencias del catalogo de especialidades
        modelo.especialidades.observe(viewLifecycleOwner) { lista ->
            binding.etEspecialidad.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, lista)
            )
        }
        modelo.guardado.observe(viewLifecycleOwner) { evento ->
            val guardado = evento.obtener()
            if (guardado != null) {
                mostrarClaveInicial(guardado.claveInicial) {
                    navegador.irRaiz(Pantalla_Vista.GESTIONAR_DOCENTES, Personas_Vista.argumentos("docente"))
                }
            }
        }
        binding.btnGuardar.setOnClickListener {
            modelo.guardar(
                idDocente,
                binding.etNombres.texto(),
                binding.etApellidoPaterno.texto(),
                binding.etApellidoMaterno.texto(),
                binding.etCorreo.texto(),
                binding.etEspecialidad.text.toString().trim(),
                binding.etTelefono.texto(),
                binding.etContrasena.text.toString(),
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_NOMBRES = "nombres"
        private const val ARG_PATERNO = "apellido_paterno"
        private const val ARG_MATERNO = "apellido_materno"
        private const val ARG_CORREO = "correo"
        private const val ARG_ESPECIALIDAD = "especialidad"
        private const val ARG_TELEFONO = "telefono"

        fun argumentos(docente: Docente): Bundle {
            val datos = Bundle()
            datos.putInt(ARG_ID, docente.id)
            datos.putString(ARG_NOMBRES, docente.nombres)
            datos.putString(ARG_PATERNO, docente.apellidoPaterno)
            datos.putString(ARG_MATERNO, docente.apellidoMaterno)
            datos.putString(ARG_CORREO, docente.correo)
            datos.putString(ARG_ESPECIALIDAD, docente.especialidad)
            datos.putString(ARG_TELEFONO, docente.telefono)
            return datos
        }
    }
}
