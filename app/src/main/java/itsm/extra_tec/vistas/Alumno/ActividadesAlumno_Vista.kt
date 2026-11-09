package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.ActividadesAlumno_ModeloVista
import itsm.extra_tec.databinding.ActividadesAlumnoVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class ActividadesAlumno_Vista : Base_Vista<ActividadesAlumnoVistaBinding>() {
    private val modelo: ActividadesAlumno_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        ActividadesAlumnoVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        modelo.actividades.observe(viewLifecycleOwner) { actividades ->
            estadoLista(binding.tvVacio, actividades, "No hay actividades disponibles.")
            binding.listaContenido.removeAllViews()
            // El alumno solo puede tener una actividad
            // Las demas quedan bloqueadas hasta darse de baja
            val yaInscrito = actividades?.any { it.inscrito } == true
            actividades?.forEach { a ->
                val detalles = listOf(
                    "Horario" to a.horario.ifBlank { "Por definir" },
                    "Lugar" to a.lugar.ifBlank { "Por definir" },
                    "Docente" to a.docente.ifBlank { "Por asignar" },
                    "Cupo" to "${a.inscritos} de ${a.cupo} inscritos",
                    "Estado" to when {
                        a.inscrito -> "Inscrito"
                        a.lleno -> "Cupo lleno"
                        yaInscrito -> "Ya tienes otra actividad"
                        else -> "Disponible"
                    },
                )
                val progreso = if (a.cupo > 0) (a.inscritos * 100) / a.cupo else 0
                if (a.inscrito) {
                    agregarTarjeta(
                        binding.listaContenido,
                        a.nombre,
                        a.descripcion.ifBlank { null },
                        detalles = detalles,
                        progreso = progreso,
                        a = Accion_Vista("Darme de baja", peligrosa = true) {
                            confirmar("¿Darte de baja de ${a.nombre}?") { modelo.darDeBaja(a.id) }
                        },
                    )
                } else {
                    val disponible = !a.lleno && !yaInscrito
                    val etiqueta = when {
                        yaInscrito -> "Ya tienes una actividad"
                        a.lleno -> "Cupo lleno"
                        else -> "Inscribirme"
                    }
                    agregarTarjeta(
                        binding.listaContenido,
                        a.nombre,
                        a.descripcion.ifBlank { null },
                        detalles = detalles,
                        progreso = progreso,
                        a = Accion_Vista(etiqueta, disponible) { modelo.inscribir(a.id) },
                    )
                }
            }
        }
    }
}
