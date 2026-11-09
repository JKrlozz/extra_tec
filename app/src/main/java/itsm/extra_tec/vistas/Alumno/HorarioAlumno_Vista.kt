package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.HorarioAlumno_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.databinding.HorarioAlumnoVistaBinding
import itsm.extra_tec.databinding.ItemBloqueHorarioBinding
import itsm.extra_tec.databinding.ItemDiaCalendarioBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class HorarioAlumno_Vista : Base_Vista<HorarioAlumnoVistaBinding>() {
    private val modelo: HorarioAlumno_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        HorarioAlumnoVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnAgregarActividad.setOnClickListener { navegador.ir(Pantalla_Vista.ACTIVIDADES_ALUMNO) }
        modelo.horario.observe(viewLifecycleOwner) { dias ->
            val total = dias?.sumOf { it.bloques.size }
            binding.tvVacio.visibility = if (dias == null || total == 0) View.VISIBLE else View.GONE
            binding.tvVacio.text = when {
                dias == null -> "Cargando"
                else -> "Aún no estás inscrito en ninguna actividad."
            }
            binding.calendarioContenido.visibility =
                if (dias != null && total!! > 0) View.VISIBLE else View.GONE
            binding.calendarioContenido.removeAllViews()
            dias?.forEach { dia ->
                val tarjeta = ItemDiaCalendarioBinding.inflate(
                    layoutInflater, binding.calendarioContenido, false,
                )
                tarjeta.tvDia.text = dia.nombre
                tarjeta.tvEtiquetaHoy.visibility = if (dia.esHoy) View.VISIBLE else View.GONE
                tarjeta.tvSinClases.visibility = if (dia.bloques.isEmpty()) View.VISIBLE else View.GONE

                if (dia.esHoy) {
                    tarjeta.root.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.itsm_primary_light),
                    )
                    tarjeta.root.strokeColor =
                        ContextCompat.getColor(requireContext(), R.color.itsm_primary)
                    tarjeta.tvDia.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.itsm_primary_dark),
                    )
                }

                dia.bloques.forEach { bloque ->
                    val fila = ItemBloqueHorarioBinding.inflate(
                        layoutInflater, tarjeta.contenedorBloques, false,
                    )
                    fila.tvHoraInicio.text = bloque.horaInicio
                    fila.tvHoraFin.text = bloque.horaFin
                    fila.tvActividad.text = bloque.nombre
                    fila.tvLugar.text = bloque.lugar
                    tarjeta.contenedorBloques.addView(fila.root)
                }
                binding.calendarioContenido.addView(tarjeta.root)
            }
        }
    }
}
