package itsm.extra_tec.vistas.Administrador

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import itsm.extra_tec.ModeloVista.Personas_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.databinding.PersonasVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

// alumnos y docentes en una pantalla
class Personas_Vista : Base_Vista() {
    private var _binding: PersonasVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: Personas_ModeloVista by viewModels()

    private var esAlumno = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PersonasVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        esAlumno = arguments?.getString(ARG_TIPO) != "docente"

        if (esAlumno) {
            binding.grupoTipo.check(R.id.btnTabAlumnos)
        } else {
            binding.grupoTipo.check(R.id.btnTabDocentes)
        }
        binding.grupoTipo.addOnButtonCheckedListener { _, id, marcado ->
            if (marcado) {
                esAlumno = id == R.id.btnTabAlumnos
                actualizarEncabezado()
                pintarLista()
            }
        }

        modelo.alumnos.observe(viewLifecycleOwner) { alumnos ->
            binding.tvTotalAlumnos.text = (alumnos?.size ?: 0).toString()
            if (esAlumno) pintarLista()
        }
        modelo.docentes.observe(viewLifecycleOwner) { docentes ->
            binding.tvTotalDocentes.text = (docentes?.size ?: 0).toString()
            if (!esAlumno) pintarLista()
        }

        alEscribir(binding.etBuscar) { texto -> modelo.buscar(texto) }
        binding.btnNuevo.setOnClickListener {
            if (esAlumno) {
                navegador.ir(Pantalla_Vista.FORMULARIO_ALUMNO, null)
            } else {
                navegador.ir(Pantalla_Vista.FORMULARIO_DOCENTE, null)
            }
        }

        actualizarEncabezado()
        pintarLista()
    }

    private fun actualizarEncabezado() {
        pintarPestana(binding.btnTabAlumnos, esAlumno)
        pintarPestana(binding.btnTabDocentes, !esAlumno)
        if (esAlumno) {
            binding.tilBuscar.hint = "Buscar por nombre o número de control"
            binding.btnNuevo.text = "Nuevo alumno"
        } else {
            binding.tilBuscar.hint = "Buscar por nombre o correo"
            binding.btnNuevo.text = "Nuevo docente"
        }
    }

    // la pestana elegida va verde
    private fun pintarPestana(boton: MaterialButton, elegida: Boolean) {
        val verde = ContextCompat.getColor(requireContext(), R.color.itsm_primary)
        val verdeClaro = ContextCompat.getColor(requireContext(), R.color.itsm_primary_light)
        val blanco = ContextCompat.getColor(requireContext(), R.color.white)
        if (elegida) {
            boton.backgroundTintList = ColorStateList.valueOf(verde)
            boton.setTextColor(blanco)
        } else {
            boton.backgroundTintList = ColorStateList.valueOf(verdeClaro)
            boton.setTextColor(verde)
        }
    }

    private fun pintarLista() {
        binding.listaContenido.removeAllViews()
        if (esAlumno) {
            val alumnos = modelo.alumnos.value
            estadoLista(binding.tvVacio, alumnos, "No hay alumnos registrados.")
            if (alumnos == null) return
            for (alumno in alumnos) {
                var semestre = alumno.semestre.toString()
                if (alumno.semestre >= 1 && alumno.semestre <= semestres.size) {
                    semestre = semestres[alumno.semestre - 1]
                }
                agregarTarjeta(
                    binding.listaContenido,
                    alumno.nombre,
                    detalles = listOf(
                        "Número de control" to alumno.matricula,
                        "Carrera" to alumno.carrera,
                        "Semestre" to semestre,
                        "Correo" to alumno.correo,
                    ),
                    a = Accion_Vista("Editar") {
                        navegador.ir(Pantalla_Vista.FORMULARIO_ALUMNO, FormularioAlumno_Vista.argumentos(alumno))
                    },
                    b = Accion_Vista("Eliminar", true, true) {
                        confirmar("¿Dar de baja a ${alumno.nombre}?") { modelo.eliminarAlumno(alumno.id) }
                    },
                )
            }
        } else {
            val docentes = modelo.docentes.value
            estadoLista(binding.tvVacio, docentes, "No hay docentes registrados.")
            if (docentes == null) return
            for (docente in docentes) {
                agregarTarjeta(
                    binding.listaContenido,
                    docente.nombre,
                    detalles = listOf(
                        "Especialidad" to docente.especialidad.ifBlank { "Sin especialidad" },
                        "Teléfono" to docente.telefono.ifBlank { "Sin teléfono" },
                        "Correo" to docente.correo,
                    ),
                    a = Accion_Vista("Editar") {
                        navegador.ir(Pantalla_Vista.FORMULARIO_DOCENTE, FormularioDocente_Vista.argumentos(docente))
                    },
                    b = Accion_Vista("Eliminar", true, true) {
                        confirmar("¿Dar de baja a ${docente.nombre}?") { modelo.eliminarDocente(docente.id) }
                    },
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TIPO = "tipo"

        fun argumentos(tipo: String): Bundle {
            val datos = Bundle()
            datos.putString(ARG_TIPO, tipo)
            return datos
        }
    }
}
