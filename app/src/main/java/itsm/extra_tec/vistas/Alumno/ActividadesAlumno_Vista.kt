package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.ModeloVista.ActividadesAlumno_ModeloVista
import itsm.extra_tec.databinding.ActividadesAlumnoVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class ActividadesAlumno_Vista : Base_Vista() {
    private var _binding: ActividadesAlumnoVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: ActividadesAlumno_ModeloVista by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActividadesAlumnoVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        modelo.actividades.observe(viewLifecycleOwner) { actividades ->
            estadoLista(binding.tvVacio, actividades, "No hay actividades disponibles.")
            binding.listaContenido.removeAllViews()
            if (actividades != null) {
                pintar(actividades)
            }
        }
    }

    // solo una actividad por alumno
    // las demas quedan bloqueadas
    private fun pintar(actividades: List<Actividad>) {
        var yaInscrito = false
        for (actividad in actividades) {
            if (actividad.inscrito) yaInscrito = true
        }

        for (a in actividades) {
            var estado = "Disponible"
            if (a.inscrito) {
                estado = "Inscrito"
            } else if (a.lleno) {
                estado = "Cupo lleno"
            } else if (yaInscrito) {
                estado = "Ya tienes otra actividad"
            }
            val detalles = listOf(
                "Horario" to a.horario.ifBlank { "Por definir" },
                "Lugar" to a.lugar.ifBlank { "Por definir" },
                "Docente" to a.docente.ifBlank { "Por asignar" },
                "Cupo" to "${a.inscritos} de ${a.cupo} inscritos",
                "Estado" to estado,
            )
            var progreso = 0
            if (a.cupo > 0) {
                progreso = a.inscritos * 100 / a.cupo
            }

            var accion: Accion_Vista
            if (a.inscrito) {
                // la baja solo la hace el administrador
                accion = Accion_Vista("Inscrito este semestre", false) {}
            } else {
                var texto = "Inscribirme"
                if (yaInscrito) {
                    texto = "Ya tienes una actividad"
                } else if (a.lleno) {
                    texto = "Cupo lleno"
                }
                val disponible = !a.lleno && !yaInscrito
                accion = Accion_Vista(texto, disponible) {
                    confirmar(
                        "¿Inscribirte a ${a.nombre}?\n\n" +
                            "Solo puedes inscribirte a una actividad extraescolar y la inscripción " +
                            "se mantiene durante todo el semestre."
                    ) { modelo.inscribir(a.id) }
                }
            }
            agregarTarjeta(
                binding.listaContenido,
                a.nombre,
                linea1 = a.descripcion,
                detalles = detalles,
                progreso = progreso,
                a = accion,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
