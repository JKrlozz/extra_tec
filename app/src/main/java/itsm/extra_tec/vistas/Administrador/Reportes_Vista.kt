package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.FiltrosReporte
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
        llenarSpinner(binding.spinnerSemestreReporte, listOf("Cualquier semestre") + (1..8).map { "Semestre $it" })
        llenarSpinner(binding.spinnerCarreraReporte, listOf("Cualquier carrera"))
        llenarSpinner(binding.spinnerActividadReporte, listOf("Cualquier actividad"))
        modelo.carreras.observe(viewLifecycleOwner) { lista ->
            llenarSpinner(binding.spinnerCarreraReporte, listOf("Cualquier carrera") + lista.map { it.nombre })
        }
        modelo.actividades.observe(viewLifecycleOwner) { lista ->
            llenarSpinner(binding.spinnerActividadReporte, listOf("Cualquier actividad") + lista.map { it.nombre })
        }
        modelo.cargarCatalogos()

        // los filtros cambian segun el tipo
        binding.spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(padre: AdapterView<*>?, vista: View?, posicion: Int, id: Long) {
                mostrarFiltros(modelo.tipos.getOrNull(posicion) ?: "actividad")
            }

            override fun onNothingSelected(padre: AdapterView<*>?) {
                mostrarFiltros("actividad")
            }
        }

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
            binding.tvResumenReporte.text = modelo.encabezado(reporte)
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
            modelo.generar(
                binding.spinnerTipo.selectedItemPosition,
                binding.etDesde.texto(),
                binding.etHasta.texto(),
                leerFiltros(),
            )
        }
        binding.btnExportarPdf.setOnClickListener { modelo.exportar(requireContext().applicationContext) }
    }

    private fun mostrarFiltros(tipo: String) {
        binding.grupoFiltros.visibility = if (tipo == "actividad") View.GONE else View.VISIBLE
        binding.grupoFiltrosAlumno.visibility = if (tipo == "alumno") View.VISIBLE else View.GONE
        binding.tilDocenteReporte.visibility = if (tipo == "docente") View.VISIBLE else View.GONE
        binding.tvAyudaFiltros.text = if (tipo == "docente") {
            "Todos son opcionales. Cada docente muestra el detalle de sus actividades."
        } else {
            "Todos son opcionales. Con número de control o nombre se muestra el detalle por sesión."
        }
    }

    // la posicion 0 de cada lista es sin filtro
    private fun leerFiltros(): FiltrosReporte {
        val posCarrera = binding.spinnerCarreraReporte.selectedItemPosition
        val posActividad = binding.spinnerActividadReporte.selectedItemPosition
        val carreras = modelo.carreras.value.orEmpty()
        val actividades = modelo.actividades.value.orEmpty()
        return FiltrosReporte(
            matricula = binding.etMatriculaReporte.texto(),
            nombre = binding.etNombreReporte.texto(),
            idCarrera = if (posCarrera > 0) carreras.getOrNull(posCarrera - 1)?.id ?: 0 else 0,
            semestre = binding.spinnerSemestreReporte.selectedItemPosition.coerceAtLeast(0),
            idActividad = if (posActividad > 0) actividades.getOrNull(posActividad - 1)?.id ?: 0 else 0,
            docente = binding.etDocenteReporte.texto(),
        )
    }

    private fun pedirFecha(campo: EditText) {
        elegirFecha(campo.texto(), false) { nueva -> campo.setText(nueva) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
