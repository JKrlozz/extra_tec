package itsm.extra_tec.vistas.Administrador

import android.app.DatePickerDialog
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
import java.util.Calendar
import java.util.Locale

class Reportes_Vista : Base_Vista<ReportesVistaBinding>() {
    private val modelo: Reportes_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        ReportesVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        llenarSpinner(binding.spinnerTipo, modelo.tipos) { it.second }

        modelo.reporte.observe(viewLifecycleOwner) { reporte ->
            binding.encabezadoReporte.visibility = if (reporte == null) View.GONE else View.VISIBLE
            binding.listaReporte.removeAllViews()
            if (reporte == null) {
                binding.tvVacioReporte.visibility = View.VISIBLE
                binding.tvVacioReporte.text = "Genera un reporte para ver los resultados."
                return@observe
            }

            binding.tvTituloReporte.text = "Reporte ${reporte.etiqueta.lowercase()}"
            binding.tvResumenReporte.text = "${reporte.rango}\n${reporte.registros} registros"
            binding.tvVacioReporte.visibility = if (reporte.filas.isEmpty()) View.VISIBLE else View.GONE
            binding.tvVacioReporte.text = "No hay registros en ese periodo."

            modelo.bloques(reporte).forEach { bloque ->
                val porcentaje = bloque.detalles
                    .firstOrNull { it.first == "Asistencia" }
                    ?.second
                    ?.substringBefore(" ")
                    ?.toIntOrNull()
                agregarTarjeta(
                    binding.listaReporte,
                    bloque.titulo,
                    detalles = bloque.detalles,
                    progreso = porcentaje,
                )
            }
        }

        binding.etDesde.setOnClickListener { elegirFecha(binding.etDesde) }
        binding.etHasta.setOnClickListener { elegirFecha(binding.etHasta) }
        binding.tilDesde.setEndIconOnClickListener { elegirFecha(binding.etDesde) }
        binding.tilHasta.setEndIconOnClickListener { elegirFecha(binding.etHasta) }
        binding.btnLimpiarFechas.setOnClickListener {
            binding.etDesde.setText("")
            binding.etHasta.setText("")
        }

        binding.btnGenerarReporte.setOnClickListener {
            modelo.generar(binding.spinnerTipo.selectedItemPosition, binding.etDesde.texto(), binding.etHasta.texto())
        }
        binding.btnExportarPdf.setOnClickListener { modelo.exportar(requireContext().applicationContext) }
    }

    private fun elegirFecha(campo: EditText) {
        val partes = campo.texto().split("-")
        val calendario = Calendar.getInstance().apply {
            if (partes.size == 3) {
                runCatching { set(partes[0].toInt(), partes[1].toInt() - 1, partes[2].toInt()) }
            }
        }
        DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                campo.setText(String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, dia))
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH),
        ).show()
    }
}
