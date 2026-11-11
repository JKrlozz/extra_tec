package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.ModeloVista.InicioDocente_ModeloVista
import itsm.extra_tec.databinding.InicioDocenteVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class InicioDocente_Vista : Base_Vista() {
    private var _binding: InicioDocenteVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: InicioDocente_ModeloVista by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InicioDocenteVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val usuario = sesion.usuario
        if (usuario != null) {
            binding.tvNombre.text = usuario.nombre
        }
        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }

        modelo.clases.observe(viewLifecycleOwner) { clases ->
            estadoLista(binding.tvVacio, clases, "Aún no tienes clases asignadas.")
            binding.listaContenido.removeAllViews()
            if (clases == null) {
                return@observe
            }
            for (clase in clases) {
                var progreso = 0
                if (clase.cupo > 0) {
                    progreso = clase.inscritos * 100 / clase.cupo
                }
                agregarTarjeta(
                    binding.listaContenido,
                    clase.nombre,
                    linea1 = clase.descripcion,
                    detalles = listOf(
                        "Horario" to clase.horario.ifBlank { "Por definir" },
                        "Lugar" to clase.lugar.ifBlank { "Por definir" },
                        "Inscritos" to "${clase.inscritos} alumnos",
                        "Cupo" to "${clase.cupo} lugares",
                    ),
                    progreso = progreso,
                    a = Accion_Vista("Pase de lista") { abrir(clase, Pantalla_Vista.ASISTENCIA) },
                    b = Accion_Vista("Ver alumnos") { abrir(clase, Pantalla_Vista.ALUMNOS_DOCENTE) },
                    c = Accion_Vista("Historial de asistencias") { abrir(clase, Pantalla_Vista.HISTORIAL_ASISTENCIA) },
                    d = Accion_Vista("Historial de evidencias") { abrir(clase, Pantalla_Vista.HISTORIAL_EVIDENCIA) },
                )
            }
        }
    }

    // se guarda la clase elegida
    private fun abrir(clase: Actividad, destino: Pantalla_Vista) {
        sesion.claseId = clase.id
        sesion.claseNombre = clase.nombre
        navegador.ir(destino, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
