package itsm.extra_tec.vistas.Docente

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.ListaAsistencia
import itsm.extra_tec.ModeloVista.Asistencia_ModeloVista
import itsm.extra_tec.databinding.AsistenciaVistaBinding
import itsm.extra_tec.databinding.ItemAlumnoAsistenciaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Asistencia_Vista : Base_Vista<AsistenciaVistaBinding>() {
    private val modelo: Asistencia_ModeloVista by viewModels()

    private val idEdicion: Int? get() = argumento(ARG_ID_ACTIVIDAD)
    private val fechaEdicion: String? get() = arguments?.getString(ARG_FECHA)
    private val nombreEdicion: String get() = arguments?.getString(ARG_NOMBRE).orEmpty()
    private val modoSeleccion: Boolean get() = idEdicion == null

    private var fecha = hoy()
    private var nombreActividad = ""

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        AsistenciaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        if (modoSeleccion) prepararSeleccion() else prepararEdicion()

        modelo.lista.observe(viewLifecycleOwner) { lista -> mostrarLista(lista) }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.atras() }
        }

        binding.btnGuardarAsistencia.setOnClickListener { modelo.guardar() }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    private fun prepararEdicion() {
        val id = idEdicion ?: return navegador.irAlInicio()
        fecha = fechaEdicion ?: hoy()
        nombreActividad = nombreEdicion
        binding.contenedorSeleccion.visibility = View.GONE
        binding.tvTituloAsistencia.text = "Editar asistencia"
        binding.tvSubtitulo.text = if (nombreEdicion.isBlank()) {
            "Edición de asistencia"
        } else {
            nombreEdicion
        }
        modelo.cargar(id, fecha)
    }

    private fun prepararSeleccion() {
        binding.tvSubtitulo.text = "Elige la actividad y la fecha"
        actualizarFecha()
        binding.btnFechaAsistencia.setOnClickListener { elegirFecha() }

        modelo.clases.observe(viewLifecycleOwner) { clases ->
            llenarSpinner(binding.spinnerActividadAsistencia, clases) { it.nombre }
            if (clases.isEmpty()) {
                binding.contenedorLista.visibility = View.GONE
                binding.tvVacio.visibility = View.VISIBLE
                binding.tvVacio.text = "No tienes clases asignadas."
                return@observe
            }

            binding.contenedorLista.visibility = View.VISIBLE
            val posicion = clases.indexOfFirst { it.id == sesion.claseId }.takeIf { it >= 0 } ?: 0
            binding.spinnerActividadAsistencia.setSelection(posicion)
            nombreActividad = clases[posicion].nombre
            cargarLista(clases[posicion].id)

            binding.spinnerActividadAsistencia.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long,
                    ) {
                        val clase = clases.getOrNull(position) ?: return
                        nombreActividad = clase.nombre
                        sesion.claseId = clase.id
                        sesion.claseNombre = clase.nombre
                        cargarLista(clase.id)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    private fun cargarLista(idActividad: Int) {
        binding.listaContenido.removeAllViews()
        binding.tvVacio.visibility = View.GONE
        modelo.cargar(idActividad, fecha)
    }

    private fun mostrarLista(lista: ListaAsistencia?) {
        estadoLista(binding.tvVacio, lista?.alumnos, "No hay alumnos inscritos.")
        if (lista == null) return
        binding.tvSubtitulo.text = if (nombreActividad.isBlank()) {
            fechaLegible(lista.fecha)
        } else {
            "$nombreActividad\n${fechaLegible(lista.fecha)}"
        }
        if (binding.listaContenido.childCount > 0) return

        lista.alumnos.forEach { alumno ->
            val fila = ItemAlumnoAsistenciaBinding.inflate(
                layoutInflater, binding.listaContenido, false,
            )
            fila.tvNombreAlumno.text = alumno.nombre
            fila.tvMatriculaAlumno.text = "Número de control ${alumno.matricula}"
            fila.cbPresente.isChecked = alumno.presente
            fila.cbPresente.setOnCheckedChangeListener { _, marcada ->
                modelo.marcar(alumno.idAlumno, marcada)
            }
            fila.root.setOnClickListener { fila.cbPresente.toggle() }
            binding.listaContenido.addView(fila.root)
        }
    }

    private fun elegirFecha() {
        val partes = fecha.split("-")
        val calendario = Calendar.getInstance().apply {
            if (partes.size == 3) {
                runCatching { set(partes[0].toInt(), partes[1].toInt() - 1, partes[2].toInt()) }
            }
        }
        DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                fecha = String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, dia)
                actualizarFecha()
                if (modoSeleccion) {
                    val clases = modelo.clases.value.orEmpty()
                    val clase = clases.getOrNull(binding.spinnerActividadAsistencia.selectedItemPosition)
                    if (clase != null) {
                        nombreActividad = clase.nombre
                        cargarLista(clase.id)
                    }
                }
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun actualizarFecha() {
        binding.btnFechaAsistencia.text = "Fecha: ${fechaLegible(fecha)}"
    }

    private fun argumento(clave: String): Int? =
        arguments?.getInt(clave, -1)?.takeIf { it > 0 }

    companion object {
        private const val ARG_ID_ACTIVIDAD = "id_actividad"
        private const val ARG_FECHA = "fecha"
        private const val ARG_NOMBRE = "nombre_actividad"

        fun argumentos(idActividad: Int, nombre: String, fecha: String? = null) = Bundle().apply {
            putInt(ARG_ID_ACTIVIDAD, idActividad)
            putString(ARG_NOMBRE, nombre)
            if (fecha != null) putString(ARG_FECHA, fecha)
        }

        private fun hoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        private fun fechaLegible(iso: String): String =
            runCatching {
                val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val salida = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                salida.format(entrada.parse(iso)!!)
            }.getOrDefault(iso)
    }
}
