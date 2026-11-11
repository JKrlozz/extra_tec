package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.DiaCalendario
import itsm.extra_tec.ModeloVista.HorarioAlumno_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.databinding.HorarioAlumnoVistaBinding
import itsm.extra_tec.databinding.ItemBloqueHorarioBinding
import itsm.extra_tec.databinding.ItemDiaCalendarioBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class HorarioAlumno_Vista : Base_Vista() {
    private var _binding: HorarioAlumnoVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: HorarioAlumno_ModeloVista by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = HorarioAlumnoVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnAgregarActividad.setOnClickListener { navegador.ir(Pantalla_Vista.ACTIVIDADES_ALUMNO, null) }
        modelo.horario.observe(viewLifecycleOwner) { dias ->
            binding.calendarioContenido.removeAllViews()
            if (dias == null) {
                binding.tvVacio.visibility = View.VISIBLE
                binding.tvVacio.text = "Cargando"
                return@observe
            }
            var total = 0
            for (dia in dias) {
                total += dia.bloques.size
            }
            if (total == 0) {
                binding.tvVacio.visibility = View.VISIBLE
                binding.tvVacio.text = "Aún no estás inscrito en ninguna actividad."
                binding.calendarioContenido.visibility = View.GONE
                return@observe
            }
            binding.tvVacio.visibility = View.GONE
            binding.calendarioContenido.visibility = View.VISIBLE
            for (dia in dias) {
                agregarDia(dia)
            }
        }
    }

    private fun agregarDia(dia: DiaCalendario) {
        val tarjeta = ItemDiaCalendarioBinding.inflate(layoutInflater, binding.calendarioContenido, false)
        tarjeta.tvDia.text = dia.nombre
        if (dia.esHoy) {
            tarjeta.tvEtiquetaHoy.visibility = View.VISIBLE
        } else {
            tarjeta.tvEtiquetaHoy.visibility = View.GONE
        }
        if (dia.bloques.isEmpty()) {
            tarjeta.tvSinClases.visibility = View.VISIBLE
        } else {
            tarjeta.tvSinClases.visibility = View.GONE
        }

        // hoy se marca en verde
        if (dia.esHoy) {
            tarjeta.root.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.itsm_primary_light))
            tarjeta.root.strokeColor = ContextCompat.getColor(requireContext(), R.color.itsm_primary)
            tarjeta.root.strokeWidth = resources.getDimensionPixelSize(R.dimen.borde_hoy)
        }

        for (bloque in dia.bloques) {
            val fila = ItemBloqueHorarioBinding.inflate(layoutInflater, tarjeta.contenedorBloques, false)
            fila.tvHoraInicio.text = bloque.horaInicio
            fila.tvHoraFin.text = bloque.horaFin
            fila.tvActividad.text = bloque.nombre
            fila.tvLugar.text = bloque.lugar
            tarjeta.contenedorBloques.addView(fila.root)
        }
        binding.calendarioContenido.addView(tarjeta.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
