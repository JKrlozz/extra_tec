package itsm.extra_tec.vistas.Administrador

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Personas_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.databinding.PersonasVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

// Pantalla unica para alumnos y docentes
class Personas_Vista : Base_Vista<PersonasVistaBinding>() {
    private val modelo: Personas_ModeloVista by viewModels()

    private var esAlumno = true

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        PersonasVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        esAlumno = arguments?.getString(ARG_TIPO) != TIPO_DOCENTE

        binding.grupoTipo.check(if (esAlumno) R.id.btnTabAlumnos else R.id.btnTabDocentes)
        binding.grupoTipo.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            esAlumno = id == R.id.btnTabAlumnos
            actualizarEncabezado()
            pintarLista()
        }

        modelo.alumnos.observe(viewLifecycleOwner) { alumnos ->
            binding.tvTotalAlumnos.text = alumnos?.size?.toString() ?: "0"
            if (esAlumno) pintarLista()
        }
        modelo.docentes.observe(viewLifecycleOwner) { docentes ->
            binding.tvTotalDocentes.text = docentes?.size?.toString() ?: "0"
            if (!esAlumno) pintarLista()
        }

        alEscribir(binding.etBuscar) { modelo.buscar(it) }
        binding.btnNuevo.setOnClickListener {
            val destino = if (esAlumno) Pantalla_Vista.FORMULARIO_ALUMNO else Pantalla_Vista.FORMULARIO_DOCENTE
            navegador.ir(destino)
        }

        actualizarEncabezado()
        pintarLista()
    }

    private fun actualizarEncabezado() {
        pintarPestanas()
        binding.tilBuscar.hint =
            if (esAlumno) "Buscar por nombre o número de control" else "Buscar por nombre o correo"
        binding.btnNuevo.text = if (esAlumno) "Nuevo alumno" else "Nuevo docente"
    }

    // La pestana activa usa el color institucional
    private fun pintarPestanas() {
        val activo = ContextCompat.getColor(requireContext(), R.color.itsm_primary)
        val inactivo = ContextCompat.getColor(requireContext(), R.color.itsm_surface)
        listOf(
            binding.btnTabAlumnos to esAlumno,
            binding.btnTabDocentes to !esAlumno,
        ).forEach { (boton, seleccionado) ->
            boton.backgroundTintList = ColorStateList.valueOf(if (seleccionado) activo else inactivo)
            boton.setTextColor(if (seleccionado) Color.WHITE else activo)
        }
    }

    private fun pintarLista() {
        binding.listaContenido.removeAllViews()
        if (esAlumno) {
            val alumnos = modelo.alumnos.value
            estadoLista(binding.tvVacio, alumnos, "No hay alumnos registrados.")
            alumnos?.forEach { alumno ->
                agregarTarjeta(
                    binding.listaContenido,
                    alumno.nombre,
                    detalles = listOf(
                        "Número de control" to alumno.matricula,
                        "Carrera" to alumno.carrera,
                        "Semestre" to semestres.getOrElse(alumno.semestre - 1) { alumno.semestre.toString() },
                        "Correo" to alumno.correo,
                    ),
                    a = Accion_Vista("Editar") {
                        navegador.ir(
                            Pantalla_Vista.FORMULARIO_ALUMNO,
                            FormularioAlumno_Vista.argumentos(alumno),
                        )
                    },
                    b = Accion_Vista("Eliminar", peligrosa = true) {
                        confirmar("¿Dar de baja a ${alumno.nombre}?") { modelo.eliminarAlumno(alumno.id) }
                    },
                )
            }
        } else {
            val docentes = modelo.docentes.value
            estadoLista(binding.tvVacio, docentes, "No hay docentes registrados.")
            docentes?.forEach { docente ->
                agregarTarjeta(
                    binding.listaContenido,
                    docente.nombre,
                    detalles = listOf(
                        "Especialidad" to docente.especialidad.ifBlank { "Sin especialidad" },
                        "Teléfono" to docente.telefono.ifBlank { "Sin teléfono" },
                        "Correo" to docente.correo,
                    ),
                    a = Accion_Vista("Editar") {
                        navegador.ir(
                            Pantalla_Vista.FORMULARIO_DOCENTE,
                            FormularioDocente_Vista.argumentos(docente),
                        )
                    },
                    b = Accion_Vista("Eliminar", peligrosa = true) {
                        confirmar("¿Dar de baja a ${docente.nombre}?") { modelo.eliminarDocente(docente.id) }
                    },
                )
            }
        }
    }

    companion object {
        private const val ARG_TIPO = "tipo"
        private const val TIPO_DOCENTE = "docente"

        fun argumentos(tipo: String) = Bundle().apply { putString(ARG_TIPO, tipo) }
    }
}
