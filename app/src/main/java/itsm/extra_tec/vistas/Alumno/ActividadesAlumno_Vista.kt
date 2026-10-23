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
            actividades?.forEach { a ->
                val docente = a.docente.ifEmpty { "Por asignar" }
                val cupo = "Cupo ${a.inscritos}/${a.cupo}"
                if (a.inscrito) {
                    agregarTarjeta(binding.listaContenido, a.nombre, "${a.horario} · Prof. $docente", "Inscrito · $cupo",
                        c = Accion_Vista("Darme de baja") {
                            confirmar("¿Darte de baja de ${a.nombre}?") { modelo.darDeBaja(a.id) }
                        })
                } else {
                    agregarTarjeta(binding.listaContenido, a.nombre, "${a.horario} · Prof. $docente", cupo,
                        a = Accion_Vista(if (a.lleno) "Cupo lleno" else "Inscribirme", !a.lleno) { modelo.inscribir(a.id) })
                }
            }
        }
    }
}
