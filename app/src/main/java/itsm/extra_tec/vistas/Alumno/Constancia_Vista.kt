package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Constancia_ModeloVista
import itsm.extra_tec.databinding.ConstanciaVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class Constancia_Vista : Base_Vista<ConstanciaVistaBinding>() {
    private val modelo: Constancia_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        ConstanciaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        modelo.constancias.observe(viewLifecycleOwner) { constancias ->
            estadoLista(binding.tvVacio, constancias, "Aún no estás inscrito en ninguna actividad.")
            binding.listaContenido.removeAllViews()
            constancias?.forEach { c ->
                val estado = if (c.disponible) "Constancia disponible" else "Constancia no disponible (se requiere 80 %)"
                agregarTarjeta(binding.listaContenido, c.nombre,
                    "${c.periodo} · $estado",
                    "Asistencia: ${c.porcentaje.toInt()} % (${c.asistencias} de ${c.sesiones} sesiones)",
                    progreso = c.porcentaje.toInt(),
                    a = if (c.disponible) {
                        Accion_Vista("Descargar PDF") { modelo.descargar(c.idActividad, requireContext().applicationContext) }
                    } else null)
            }
        }
    }
}
