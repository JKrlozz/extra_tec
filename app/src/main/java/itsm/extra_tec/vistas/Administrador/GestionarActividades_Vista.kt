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

class GestionarActividades_Vista : Base_Vista<GestionarActividadesVistaBinding>() {
    private val modelo: GestionarActividades_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        GestionarActividadesVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnNuevaActividad.setOnClickListener { navegador.ir(Pantalla_Vista.FORMULARIO_ACTIVIDAD) }
        modelo.actividades.observe(viewLifecycleOwner) { actividades ->
            estadoLista(binding.tvVacio, actividades, "No hay actividades registradas.")
            binding.listaContenido.removeAllViews()
            actividades?.forEach { a ->
                agregarTarjeta(
                    binding.listaContenido,
                    a.nombre,
                    a.descripcion.ifBlank { null },
                    detalles = listOf(
                        "Horario" to a.horario.ifBlank { "Por definir" },
                        "Lugar" to a.lugar.ifBlank { "Por definir" },
                        "Docente" to a.docente.ifBlank { "Por asignar" },
                        "Cupo" to "${a.inscritos} de ${a.cupo} inscritos",
                    ),
                    progreso = if (a.cupo > 0) (a.inscritos * 100) / a.cupo else 0,
                    a = Accion_Vista("Editar") {
                        navegador.ir(Pantalla_Vista.FORMULARIO_ACTIVIDAD, FormularioActividad_Vista.argumentos(a.id))
                    },
                    b = Accion_Vista("Evidencias") {
                        navegador.ir(
                            Pantalla_Vista.HISTORIAL_EVIDENCIA_ADMIN,
                            HistorialEvidencia_Vista.argumentos(a.id, a.nombre),
                        )
                    },
                    c = Accion_Vista("Asistencias") {
                        navegador.ir(
                            Pantalla_Vista.HISTORIAL_ASISTENCIA_ADMIN,
                            HistorialAsistencia_Vista.argumentos(a.id, a.nombre),
                        )
                    },
                    d = Accion_Vista("Eliminar", peligrosa = true) {
                        confirmar("¿Eliminar la actividad ${a.nombre}? Se conserva su historial.") { modelo.eliminar(a.id) }
                    },
                )
            }
        }
    }
}
