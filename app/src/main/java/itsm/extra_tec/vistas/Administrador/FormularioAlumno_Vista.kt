package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Alumno
import itsm.extra_tec.ModeloVista.FormularioAlumno_ModeloVista
import itsm.extra_tec.databinding.FormularioAlumnoVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.texto

class FormularioAlumno_Vista : Base_Vista<FormularioAlumnoVistaBinding>() {
    private val modelo: FormularioAlumno_ModeloVista by viewModels()

    // sin id, el formulario agrega un alumno nuevo
    private val idAlumno: Int? get() = arguments?.getInt(ARG_ID, -1)?.takeIf { it >= 0 }

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        FormularioAlumnoVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.tvTituloForm.text = if (idAlumno == null) "Agregar alumno" else "Editar alumno"
        if (idAlumno != null && savedInstanceState == null) {
            arguments?.let {
                binding.etNombre.setText(it.getString(ARG_NOMBRE))
                binding.etCorreo.setText(it.getString(ARG_CORREO))
                binding.etMatricula.setText(it.getString(ARG_MATRICULA))
                binding.etSemestre.setText(it.getInt(ARG_SEMESTRE).toString())
            }
        }
        if (idAlumno != null) binding.tilContrasena.visibility = View.GONE

        modelo.carreras.observe(viewLifecycleOwner) { carreras ->
            llenarSpinner(binding.spinnerCarrera, carreras) { it.nombre }
            val posicion = carreras.indexOfFirst { it.id == arguments?.getInt(ARG_CARRERA, -1) }
            if (posicion >= 0) binding.spinnerCarrera.setSelection(posicion)
        }
        modelo.guardado.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { mostrarClaveInicial(it.claveInicial) { navegador.ir(Pantalla_Vista.GESTIONAR_ALUMNOS) } }
        }

        binding.btnGuardar.setOnClickListener {
            modelo.guardar(
                idAlumno,
                binding.etNombre.texto(),
                binding.etCorreo.texto(),
                binding.etMatricula.texto(),
                binding.etSemestre.texto(),
                binding.spinnerCarrera.selectedItemPosition,
                binding.etContrasena.text.toString(),
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_ALUMNOS) }
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_NOMBRE = "nombre"
        private const val ARG_CORREO = "correo"
        private const val ARG_MATRICULA = "matricula"
        private const val ARG_SEMESTRE = "semestre"
        private const val ARG_CARRERA = "carrera"

        fun argumentos(a: Alumno) = Bundle().apply {
            putInt(ARG_ID, a.id)
            putString(ARG_NOMBRE, a.nombre)
            putString(ARG_CORREO, a.correo)
            putString(ARG_MATRICULA, a.matricula)
            putInt(ARG_SEMESTRE, a.semestre)
            putInt(ARG_CARRERA, a.idCarrera)
        }
    }
}
