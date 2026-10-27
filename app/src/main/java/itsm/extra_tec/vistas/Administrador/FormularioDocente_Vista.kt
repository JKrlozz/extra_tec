package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Docente
import itsm.extra_tec.ModeloVista.FormularioDocente_ModeloVista
import itsm.extra_tec.databinding.FormularioDocenteVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.texto

class FormularioDocente_Vista : Base_Vista<FormularioDocenteVistaBinding>() {
    private val modelo: FormularioDocente_ModeloVista by viewModels()

    // sin id, el formulario registra un docente nuevo
    private val idDocente: Int? get() = arguments?.getInt(ARG_ID, -1)?.takeIf { it >= 0 }

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        FormularioDocenteVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.tvTituloForm.text = if (idDocente == null) "Registrar docente" else "Editar docente"
        if (idDocente != null && savedInstanceState == null) {
            arguments?.let {
                binding.etNombre.setText(it.getString(ARG_NOMBRE))
                binding.etCorreo.setText(it.getString(ARG_CORREO))
                binding.etEspecialidad.setText(it.getString(ARG_ESPECIALIDAD))
                binding.etTelefono.setText(it.getString(ARG_TELEFONO))
            }
        }
        if (idDocente != null) binding.tilContrasena.visibility = View.GONE

        modelo.guardado.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { mostrarClaveInicial(it.claveInicial) { navegador.ir(Pantalla_Vista.GESTIONAR_DOCENTES) } }
        }
        binding.btnGuardar.setOnClickListener {
            modelo.guardar(
                idDocente,
                binding.etNombre.texto(),
                binding.etCorreo.texto(),
                binding.etEspecialidad.texto(),
                binding.etTelefono.texto(),
                binding.etContrasena.text.toString(),
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_DOCENTES) }
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_NOMBRE = "nombre"
        private const val ARG_CORREO = "correo"
        private const val ARG_ESPECIALIDAD = "especialidad"
        private const val ARG_TELEFONO = "telefono"

        fun argumentos(d: Docente) = Bundle().apply {
            putInt(ARG_ID, d.id)
            putString(ARG_NOMBRE, d.nombre)
            putString(ARG_CORREO, d.correo)
            putString(ARG_ESPECIALIDAD, d.especialidad)
            putString(ARG_TELEFONO, d.telefono)
        }
    }
}
