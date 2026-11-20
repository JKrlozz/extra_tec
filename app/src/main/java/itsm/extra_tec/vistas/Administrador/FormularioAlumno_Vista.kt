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

class FormularioAlumno_Vista : Base_Vista() {
    private var _binding: FormularioAlumnoVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: FormularioAlumno_ModeloVista by viewModels()

    // sin id es alumno nuevo
    private var idAlumno: Int? = null
    private var actividadElegida = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FormularioAlumnoVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        filtrarNumeroControl(binding.etMatricula)
        llenarSpinner(binding.spinnerSemestre, semestres)
        val datos = arguments
        if (datos != null && datos.getInt(ARG_ID, -1) > 0) {
            idAlumno = datos.getInt(ARG_ID)
            binding.tvTituloForm.text = "Editar alumno"
            // al editar no cambia la clave
            binding.tilContrasena.visibility = View.GONE
            binding.grupoActividad.visibility = View.VISIBLE
            modelo.cargarActividad(idAlumno!!)
            if (savedInstanceState == null) {
                binding.etNombres.setText(datos.getString(ARG_NOMBRES))
                binding.etApellidoPaterno.setText(datos.getString(ARG_PATERNO))
                binding.etApellidoMaterno.setText(datos.getString(ARG_MATERNO))
                binding.etCorreo.setText(datos.getString(ARG_CORREO))
                binding.etMatricula.setText(datos.getString(ARG_MATRICULA))
                val semestre = datos.getInt(ARG_SEMESTRE, 1)
                if (semestre >= 1 && semestre <= semestres.size) {
                    binding.spinnerSemestre.setSelection(semestre - 1)
                }
            }
        } else {
            binding.tvTituloForm.text = "Agregar alumno"
        }

        modelo.carreras.observe(viewLifecycleOwner) { carreras ->
            val nombres = mutableListOf<String>()
            for (carrera in carreras) {
                nombres.add(carrera.nombre)
            }
            llenarSpinner(binding.spinnerCarrera, nombres)
            val idCarrera = arguments?.getInt(ARG_CARRERA, -1) ?: -1
            for (i in carreras.indices) {
                if (carreras[i].id == idCarrera) binding.spinnerCarrera.setSelection(i)
            }
        }
        modelo.actividades.observe(viewLifecycleOwner) { actividades ->
            val nombres = mutableListOf("Sin actividad")
            for (actividad in actividades) {
                nombres.add(actividad.nombre)
            }
            llenarSpinner(binding.spinnerActividad, nombres)
            elegirActividadActual()
        }
        modelo.actividadActual.observe(viewLifecycleOwner) { elegirActividadActual() }
        modelo.guardado.observe(viewLifecycleOwner) { evento ->
            val guardado = evento.obtener()
            if (guardado != null) {
                mostrarClaveInicial(guardado.claveInicial) {
                    navegador.irRaiz(Pantalla_Vista.GESTIONAR_ALUMNOS, Personas_Vista.argumentos("alumno"))
                }
            }
        }

        binding.btnGuardar.setOnClickListener {
            val aviso = if (idAlumno == null) null else modelo.avisoDeCambio(binding.spinnerActividad.selectedItemPosition)
            if (aviso == null) {
                guardar()
            } else {
                confirmar(aviso) { guardar() }
            }
        }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    // marca la actividad que ya tiene el alumno
    private fun elegirActividadActual() {
        if (actividadElegida) return
        val actividades = modelo.actividades.value ?: return
        if (binding.spinnerActividad.adapter == null || !modelo.actividadCargada()) return
        val actual = modelo.actividadActual.value
        actividadElegida = true
        for (i in actividades.indices) {
            if (actividades[i].id == actual) binding.spinnerActividad.setSelection(i + 1)
        }
    }

    private fun guardar() {
        modelo.guardar(
            idAlumno,
            binding.etNombres.texto(),
            binding.etApellidoPaterno.texto(),
            binding.etApellidoMaterno.texto(),
            binding.etCorreo.texto(),
            binding.etMatricula.texto(),
            binding.spinnerSemestre.selectedItemPosition,
            binding.spinnerCarrera.selectedItemPosition,
            binding.etContrasena.text.toString(),
            binding.spinnerActividad.selectedItemPosition,
        )
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
        private const val ARG_MATRICULA = "matricula"
        private const val ARG_SEMESTRE = "semestre"
        private const val ARG_CARRERA = "carrera"

        fun argumentos(alumno: Alumno): Bundle {
            val datos = Bundle()
            datos.putInt(ARG_ID, alumno.id)
            datos.putString(ARG_NOMBRES, alumno.nombres)
            datos.putString(ARG_PATERNO, alumno.apellidoPaterno)
            datos.putString(ARG_MATERNO, alumno.apellidoMaterno)
            datos.putString(ARG_CORREO, alumno.correo)
            datos.putString(ARG_MATRICULA, alumno.matricula)
            datos.putInt(ARG_SEMESTRE, alumno.semestre)
            datos.putInt(ARG_CARRERA, alumno.idCarrera)
            return datos
        }
    }
}
