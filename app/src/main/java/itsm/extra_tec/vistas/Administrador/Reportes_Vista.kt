package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Reportes_ModeloVista
import itsm.extra_tec.databinding.ReportesVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.texto

class Reportes_Vista : Base_Vista<ReportesVistaBinding>() {
    private val modelo: Reportes_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        ReportesVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        llenarSpinner(binding.spinnerTipo, modelo.tipos) { it.second }
        modelo.texto.observe(viewLifecycleOwner) { binding.tvResultadoReporte.text = it }

        binding.btnGenerarReporte.setOnClickListener {
            modelo.generar(binding.spinnerTipo.selectedItemPosition, binding.etDesde.texto(), binding.etHasta.texto())
        }
        binding.btnExportarPdf.setOnClickListener { modelo.exportar(requireContext().applicationContext) }
    }
}
