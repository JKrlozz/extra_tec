package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.GestionarActividades_ModeloVista
import itsm.extra_tec.databinding.GestionarActividadesVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta
import itsm.extra_tec.vistas.Docente.HistorialAsistencia_Vista
import itsm.extra_tec.vistas.Docente.HistorialEvidencia_Vista

class GestionarActividades_Vista : Base_Vista() {
    private var _binding: GestionarActividadesVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: GestionarActividades_ModeloVista by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = GestionarActividadesVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnNuevaActividad.setOnClickListener { navegador.ir(Pantalla_Vista.FORMULARIO_ACTIVIDAD, null) }
        modelo.actividades.observe(viewLifecycleOwner) { actividades ->
            estadoLista(binding.tvVacio, actividades, "No hay actividades registradas.")
            binding.listaContenido.removeAllViews()
            if (actividades == null) {
                return@observe
            }
            for (a in actividades) {
                var progreso = 0
                if (a.cupo > 0) {
                    progreso = a.inscritos * 100 / a.cupo
                }
                agregarTarjeta(
                    binding.listaContenido,
                    a.nombre,
                    linea1 = a.descripcion,
                    detalles = listOf(
                        "Horario" to a.horario.ifBlank { "Por definir" },
                        "Lugar" to a.lugar.ifBlank { "Por definir" },
                        "Docente" to a.docente.ifBlank { "Por asignar" },
                        "Cupo" to "${a.inscritos} de ${a.cupo} inscritos",
                    ),
                    progreso = progreso,
                    a = Accion_Vista("Editar") {
                        navegador.ir(Pantalla_Vista.FORMULARIO_ACTIVIDAD, FormularioActividad_Vista.argumentos(a.id))
                    },
                    b = Accion_Vista("Evidencias") {
                        navegador.ir(Pantalla_Vista.HISTORIAL_EVIDENCIA_ADMIN, HistorialEvidencia_Vista.argumentos(a.id, a.nombre))
                    },
                    c = Accion_Vista("Asistencias") {
                        navegador.ir(Pantalla_Vista.HISTORIAL_ASISTENCIA_ADMIN, HistorialAsistencia_Vista.argumentos(a.id, a.nombre))
                    },
                    d = Accion_Vista("Eliminar", true, true) {
                        confirmar("¿Eliminar la actividad ${a.nombre}? Se conserva su historial.") { modelo.eliminar(a.id) }
                    },
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
