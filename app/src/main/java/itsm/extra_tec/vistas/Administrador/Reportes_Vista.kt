package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Reportes_ModeloVista
import itsm.extra_tec.databinding.ReportesVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta
import itsm.extra_tec.vistas.Comun.texto

class Reportes_Vista : Base_Vista() {
    private var _binding: ReportesVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: Reportes_ModeloVista by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ReportesVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        llenarSpinner(binding.spinnerTipo, modelo.etiquetas)

        modelo.reporte.observe(viewLifecycleOwner) { reporte ->
            binding.listaReporte.removeAllViews()
            if (reporte == null) {
                binding.encabezadoReporte.visibility = View.GONE
                binding.tvVacioReporte.visibility = View.VISIBLE
                binding.tvVacioReporte.text = "Genera un reporte para ver los resultados."
                return@observe
            }
            binding.encabezadoReporte.visibility = View.VISIBLE
            binding.tvTituloReporte.text = "Reporte " + reporte.etiqueta.lowercase()
            binding.tvResumenReporte.text = reporte.rango + "\n" + reporte.filas.size + " registros"
            if (reporte.filas.isEmpty()) {
                binding.tvVacioReporte.visibility = View.VISIBLE
                binding.tvVacioReporte.text = "No hay registros en ese periodo."
            } else {
                binding.tvVacioReporte.visibility = View.GONE
            }

            for (bloque in modelo.bloques(reporte)) {
                // la barra muestra la asistencia
                var porcentaje: Int? = null
                for (detalle in bloque.detalles) {
                    if (detalle.first == "Asistencia") {
                        porcentaje = detalle.second.substringBefore(" ").toDoubleOrNull()?.toInt()
                    }
                }
                agregarTarjeta(
                    binding.listaReporte,
                    bloque.titulo,
                    detalles = bloque.detalles,
                    progreso = porcentaje,
                )
            }
        }

        binding.etDesde.setOnClickListener { pedirFecha(binding.etDesde) }
        binding.etHasta.setOnClickListener { pedirFecha(binding.etHasta) }
        binding.tilDesde.setEndIconOnClickListener { pedirFecha(binding.etDesde) }
        binding.tilHasta.setEndIconOnClickListener { pedirFecha(binding.etHasta) }
        binding.btnLimpiarFechas.setOnClickListener {
            binding.etDesde.setText("")
            binding.etHasta.setText("")
        }
        binding.btnGenerarReporte.setOnClickListener {
            modelo.generar(binding.spinnerTipo.selectedItemPosition, binding.etDesde.texto(), binding.etHasta.texto())
        }
        binding.btnExportarPdf.setOnClickListener { modelo.exportar(requireContext().applicationContext) }
    }

    private fun pedirFecha(campo: EditText) {
        elegirFecha(campo.texto(), false) { nueva -> campo.setText(nueva) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
