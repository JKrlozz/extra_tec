package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.ListaAsistencia
import itsm.extra_tec.ModeloVista.Asistencia_ModeloVista
import itsm.extra_tec.ModeloVista.Fechas_ModeloVista
import itsm.extra_tec.databinding.AsistenciaVistaBinding
import itsm.extra_tec.databinding.ItemAlumnoAsistenciaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista

class Asistencia_Vista : Base_Vista() {
    private var _binding: AsistenciaVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: Asistencia_ModeloVista by viewModels()

    private var fecha = Fechas_ModeloVista.hoy()
    private var nombreActividad = ""
    private var idActividad = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AsistenciaVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        // con argumentos se edita
        val idEdicion = arguments?.getInt(ARG_ID_ACTIVIDAD, -1) ?: -1
        if (idEdicion > 0) {
            prepararEdicion(idEdicion)
        } else {
            prepararSeleccion()
        }

        modelo.lista.observe(viewLifecycleOwner) { lista -> mostrarLista(lista) }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            if (evento.obtener() != null) {
                navegador.atras()
            }
        }
        binding.btnGuardarAsistencia.setOnClickListener { modelo.guardar() }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    private fun prepararEdicion(id: Int) {
        idActividad = id
        fecha = arguments?.getString(ARG_FECHA) ?: Fechas_ModeloVista.hoy()
        nombreActividad = arguments?.getString(ARG_NOMBRE) ?: ""
        binding.contenedorSeleccion.visibility = View.GONE
        binding.tvTituloAsistencia.text = "Editar asistencia"
        cargarLista()
    }

    private fun prepararSeleccion() {
        binding.tvSubtitulo.text = "Elige la actividad y la fecha"
        actualizarFecha()
        binding.btnFechaAsistencia.setOnClickListener {
            elegirFecha(fecha, true) { nueva ->
                fecha = nueva
                actualizarFecha()
                cargarLista()
            }
        }

        modelo.clases.observe(viewLifecycleOwner) { clases ->
            val nombres = mutableListOf<String>()
            for (clase in clases) {
                nombres.add(clase.nombre)
            }
            llenarSpinner(binding.spinnerActividadAsistencia, nombres)
            if (clases.isEmpty()) {
                binding.contenedorLista.visibility = View.GONE
                binding.tvVacio.visibility = View.VISIBLE
                binding.tvVacio.text = "No tienes clases asignadas."
                return@observe
            }
            binding.contenedorLista.visibility = View.VISIBLE

            // se elige la clase abierta
            var posicion = 0
            for (i in clases.indices) {
                if (clases[i].id == sesion.claseId) posicion = i
            }
            binding.spinnerActividadAsistencia.setSelection(posicion)
            elegirClase(clases[posicion])

            binding.spinnerActividadAsistencia.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(padre: AdapterView<*>?, vista: View?, posicionNueva: Int, id: Long) {
                    if (posicionNueva >= 0 && posicionNueva < clases.size) {
                        elegirClase(clases[posicionNueva])
                    }
                }

                override fun onNothingSelected(padre: AdapterView<*>?) {}
            }
        }
    }

    private fun elegirClase(clase: Actividad) {
        idActividad = clase.id
        nombreActividad = clase.nombre
        sesion.claseId = clase.id
        sesion.claseNombre = clase.nombre
        cargarLista()
    }

    private fun cargarLista() {
        if (idActividad <= 0) {
            return
        }
        binding.listaContenido.removeAllViews()
        binding.tvVacio.visibility = View.GONE
        modelo.cargar(idActividad, fecha)
    }

    private fun mostrarLista(lista: ListaAsistencia?) {
        estadoLista(binding.tvVacio, lista?.alumnos, "No hay alumnos inscritos.")
        if (lista == null) {
            binding.listaContenido.removeAllViews()
            return
        }
        binding.tvSubtitulo.text = nombreActividad + "\n" + Fechas_ModeloVista.legible(lista.fecha)
        // al marcar no se redibuja
        if (binding.listaContenido.childCount > 0) {
            return
        }
        for (alumno in lista.alumnos) {
            val fila = ItemAlumnoAsistenciaBinding.inflate(layoutInflater, binding.listaContenido, false)
            fila.tvNombreAlumno.text = alumno.nombre
            fila.tvMatriculaAlumno.text = "Número de control " + alumno.matricula
            fila.cbPresente.isChecked = alumno.presente
            fila.cbPresente.setOnCheckedChangeListener { _, marcada -> modelo.marcar(alumno.idAlumno, marcada) }
            fila.root.setOnClickListener { fila.cbPresente.toggle() }
            binding.listaContenido.addView(fila.root)
        }
    }

    private fun actualizarFecha() {
        binding.btnFechaAsistencia.text = "Fecha: " + Fechas_ModeloVista.legible(fecha)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID_ACTIVIDAD = "id_actividad"
        private const val ARG_FECHA = "fecha"
        private const val ARG_NOMBRE = "nombre_actividad"

        fun argumentos(idActividad: Int, nombre: String, fecha: String): Bundle {
            val datos = Bundle()
            datos.putInt(ARG_ID_ACTIVIDAD, idActividad)
            datos.putString(ARG_NOMBRE, nombre)
            datos.putString(ARG_FECHA, fecha)
            return datos
        }
    }
}
