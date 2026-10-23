package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.FormularioActividad_ModeloVista
import itsm.extra_tec.databinding.FormularioActividadVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.texto

class FormularioActividad_Vista : Base_Vista<FormularioActividadVistaBinding>() {
    private val modelo: FormularioActividad_ModeloVista by viewModels()

    // Sin argumento id el formulario crea una actividad nueva
    private val idActividad: Int? get() = arguments?.getInt(ARG_ID, -1)?.takeIf { it >= 0 }

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        FormularioActividadVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.tvTituloForm.text = if (idActividad == null) "Nueva actividad" else "Editar actividad"
        modelo.iniciar(idActividad)

        modelo.docentes.observe(viewLifecycleOwner) { llenarSpinner(binding.spinnerDocente, it) { d -> d.nombre } }
        modelo.detalle.observe(viewLifecycleOwner) { detalle ->
            if (detalle == null) return@observe
            val a = detalle.actividad
            binding.etNombre.setText(a.nombre)
            binding.etDescripcion.setText(a.descripcion)
            binding.etCupo.setText(a.cupo.toString())
            binding.etHorario.setText(detalle.horarios.joinToString(", ") { "${it.diaSemana} ${it.horaInicio}-${it.horaFin}" })
            val posicion = modelo.docentes.value.orEmpty().indexOfFirst { it.id == a.idDocente }
            if (posicion >= 0) binding.spinnerDocente.setSelection(posicion)
        }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.ir(Pantalla_Vista.GESTIONAR_ACTIVIDADES) }
        }

        binding.btnGuardar.setOnClickListener {
            modelo.guardar(
                idActividad,
                binding.etNombre.texto(),
                binding.etDescripcion.texto(),
                binding.etCupo.texto(),
                binding.etHorario.texto(),
                binding.spinnerDocente.selectedItemPosition,
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_ACTIVIDADES) }
    }

    companion object {
        private const val ARG_ID = "id"

        fun argumentos(id: Int) = Bundle().apply { putInt(ARG_ID, id) }
    }
}
