package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.SesionAsistencia
import itsm.extra_tec.ModeloVista.Fechas_ModeloVista
import itsm.extra_tec.ModeloVista.HistorialAsistencia_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.databinding.HistorialAsistenciaVistaBinding
import itsm.extra_tec.databinding.ItemDetalleAsistenciaBinding
import itsm.extra_tec.databinding.ItemSesionAsistenciaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class HistorialAsistencia_Vista : Base_Vista() {
    private var _binding: HistorialAsistenciaVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: HistorialAsistencia_ModeloVista by viewModels()

    private var idActividad = -1
    private var nombreActividad = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = HistorialAsistenciaVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        // actividad del admin o sesion
        idActividad = arguments?.getInt(ARG_ID_ACTIVIDAD, -1) ?: -1
        nombreActividad = arguments?.getString(ARG_NOMBRE) ?: ""
        if (idActividad <= 0) {
            idActividad = sesion.claseId ?: -1
            nombreActividad = sesion.claseNombre
        }
        if (idActividad <= 0) {
            navegador.irAlInicio()
            return
        }
        binding.tvSubtitulo.text = nombreActividad
        modelo.cargar(idActividad)

        modelo.sesiones.observe(viewLifecycleOwner) { sesiones ->
            estadoLista(binding.tvVacio, sesiones, "Aún no hay asistencias registradas.")
            binding.listaContenido.removeAllViews()
            if (sesiones != null) {
                for (registro in sesiones) {
                    agregarSesion(registro)
                }
            }
        }
    }

    private fun agregarSesion(registro: SesionAsistencia) {
        val item = ItemSesionAsistenciaBinding.inflate(layoutInflater, binding.listaContenido, false)
        item.tvFecha.text = Fechas_ModeloVista.legible(registro.fecha)
        item.tvResumen.text = "${registro.presentes} de ${registro.total} presentes"
        if (registro.total > 0) {
            item.pbAsistencia.progress = registro.presentes * 100 / registro.total
        }
        item.btnDetalle.text = "Ver detalle"
        item.btnDetalle.setOnClickListener {
            if (item.contenedorDetalle.visibility == View.VISIBLE) {
                item.contenedorDetalle.visibility = View.GONE
                item.btnDetalle.text = "Ver detalle"
            } else {
                item.contenedorDetalle.visibility = View.VISIBLE
                item.btnDetalle.text = "Ocultar"
            }
        }
        item.btnEditar.setOnClickListener {
            var destino = Pantalla_Vista.ASISTENCIA
            if (sesion.rol == "admin") {
                destino = Pantalla_Vista.ASISTENCIA_ADMIN
            }
            navegador.ir(destino, Asistencia_Vista.argumentos(idActividad, nombreActividad, registro.fecha))
        }

        if (registro.alumnos.isEmpty()) {
            item.tvSinDetalle.visibility = View.VISIBLE
        } else {
            item.tvSinDetalle.visibility = View.GONE
        }
        for (alumno in registro.alumnos) {
            val fila = ItemDetalleAsistenciaBinding.inflate(layoutInflater, item.contenedorDetalle, false)
            fila.tvNombreDetalle.text = alumno.nombre
            fila.tvMatriculaDetalle.text = "Número de control " + alumno.matricula
            if (alumno.presente) {
                fila.tvEstadoDetalle.text = "Presente"
                fila.tvEstadoDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.itsm_success))
            } else {
                fila.tvEstadoDetalle.text = "Ausente"
                fila.tvEstadoDetalle.setTextColor(ContextCompat.getColor(requireContext(), R.color.itsm_danger))
            }
            item.contenedorDetalle.addView(fila.root)
        }
        binding.listaContenido.addView(item.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID_ACTIVIDAD = "id_actividad"
        private const val ARG_NOMBRE = "nombre_actividad"

        fun argumentos(idActividad: Int, nombre: String): Bundle {
            val datos = Bundle()
            datos.putInt(ARG_ID_ACTIVIDAD, idActividad)
            datos.putString(ARG_NOMBRE, nombre)
            return datos
        }
    }
}
