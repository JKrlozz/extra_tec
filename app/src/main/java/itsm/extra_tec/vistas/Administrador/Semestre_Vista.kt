package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Fechas_ModeloVista
import itsm.extra_tec.ModeloVista.Semestre_ModeloVista
import itsm.extra_tec.databinding.SemestreVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta
import itsm.extra_tec.vistas.Comun.texto

// fechas del semestre y semestres archivados
class Semestre_Vista : Base_Vista() {
    private var _binding: SemestreVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: Semestre_ModeloVista by viewModels()
    private var camposLlenos = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SemestreVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)

        modelo.estado.observe(viewLifecycleOwner) { estado ->
            if (estado == null) {
                return@observe
            }
            val activo = estado.activo
            if (activo != null) {
                binding.tvEstadoSemestre.text = activo.nombre + "\n" +
                    Fechas_ModeloVista.legible(activo.inicio) + " al " + Fechas_ModeloVista.legible(activo.fin)
                binding.tvAyudaSemestre.text = "Al llegar la fecha de fin, las listas, asistencias y evidencias " +
                    "se archivan solas: se guardan, pero ya no aparecen en la aplicación."
                binding.btnGuardarSemestre.text = "Guardar fechas"
                binding.btnCerrarSemestre.visibility = View.VISIBLE
                if (!camposLlenos) {
                    binding.etInicio.setText(activo.inicio)
                    binding.etFin.setText(activo.fin)
                    camposLlenos = true
                }
            } else {
                binding.tvEstadoSemestre.text = "Sin semestre activo. La aplicación no permite inscribir " +
                    "ni registrar asistencias hasta iniciar uno."
                binding.tvAyudaSemestre.text = "Al iniciar un semestre se copian las actividades del anterior " +
                    "(con horarios y docentes), sin alumnos ni registros."
                binding.btnGuardarSemestre.text = "Iniciar semestre"
                binding.btnCerrarSemestre.visibility = View.GONE
                binding.etInicio.setText("")
                binding.etFin.setText("")
                camposLlenos = false
            }

            binding.listaArchivados.removeAllViews()
            binding.tvVacioArchivados.visibility = if (estado.archivados.isEmpty()) View.VISIBLE else View.GONE
            for (s in estado.archivados) {
                agregarTarjeta(
                    binding.listaArchivados,
                    s.nombre,
                    detalles = listOf(
                        Pair("Fechas", Fechas_ModeloVista.legible(s.inicio) + " al " + Fechas_ModeloVista.legible(s.fin)),
                        Pair("Archivado", Fechas_ModeloVista.legible(s.cerrado)),
                        Pair("Actividades", s.actividades.toString()),
                        Pair("Inscripciones", s.inscripciones.toString()),
                        Pair("Sesiones", s.sesiones.toString()),
                        Pair("Evidencias", s.evidencias.toString()),
                    ),
                )
            }
        }

        binding.etInicio.setOnClickListener { pedirFecha(binding.etInicio) }
        binding.etFin.setOnClickListener { pedirFecha(binding.etFin) }
        binding.tilInicio.setEndIconOnClickListener { pedirFecha(binding.etInicio) }
        binding.tilFin.setEndIconOnClickListener { pedirFecha(binding.etFin) }
        binding.btnGuardarSemestre.setOnClickListener {
            modelo.guardar(binding.etInicio.texto(), binding.etFin.texto())
        }
        binding.btnCerrarSemestre.setOnClickListener {
            confirmar(
                "Se archivarán las listas, asistencias y evidencias de este semestre. " +
                    "Quedarán guardadas pero ya no se verán en la aplicación. ¿Cerrar el semestre ahora?",
            ) { modelo.cerrar() }
        }
    }

    private fun pedirFecha(campo: EditText) {
        elegirFecha(campo.texto(), false) { nueva -> campo.setText(nueva) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
