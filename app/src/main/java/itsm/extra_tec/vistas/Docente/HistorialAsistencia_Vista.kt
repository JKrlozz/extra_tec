package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.SesionAsistencia
import itsm.extra_tec.ModeloVista.HistorialAsistencia_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.databinding.HistorialAsistenciaVistaBinding
import itsm.extra_tec.databinding.ItemDetalleAsistenciaBinding
import itsm.extra_tec.databinding.ItemSesionAsistenciaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import java.text.SimpleDateFormat
import java.util.Locale

class HistorialAsistencia_Vista : Base_Vista<HistorialAsistenciaVistaBinding>() {
    private val modelo: HistorialAsistencia_ModeloVista by viewModels()

    private val idActividad: Int? get() =
        arguments?.getInt(ARG_ID_ACTIVIDAD, -1)?.takeIf { it > 0 } ?: sesion.claseId

    private val nombreActividad: String get() =
        arguments?.getString(ARG_NOMBRE).orEmpty().ifBlank { sesion.claseNombre }

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        HistorialAsistenciaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val idClase = idActividad ?: return navegador.irAlInicio()
        binding.tvSubtitulo.text = nombreActividad
        modelo.cargar(idClase)

        modelo.sesiones.observe(viewLifecycleOwner) { sesiones ->
            estadoLista(binding.tvVacio, sesiones, "Aún no hay asistencias registradas.")
            binding.listaContenido.removeAllViews()
            sesiones?.forEach { registro -> agregarSesion(registro, idClase) }
        }
    }

    private fun agregarSesion(registro: SesionAsistencia, idClase: Int) {
        val item = ItemSesionAsistenciaBinding.inflate(
            layoutInflater, binding.listaContenido, false,
        )
        item.tvFecha.text = fechaLegible(registro.fecha)
        item.tvResumen.text = "${registro.presentes} de ${registro.total} presentes"
        item.pbAsistencia.progress =
            if (registro.total > 0) (registro.presentes * 100) / registro.total else 0
        item.btnDetalle.text = "Expander"
        item.btnDetalle.setOnClickListener {
            val mostrar = item.contenedorDetalle.visibility != View.VISIBLE
            item.contenedorDetalle.visibility = if (mostrar) View.VISIBLE else View.GONE
            item.btnDetalle.text = if (mostrar) "Ocultar" else "Expander"
        }
        item.btnEditar.setOnClickListener {
            val destino = if (sesion.rol == "admin") {
                Pantalla_Vista.ASISTENCIA_ADMIN
            } else {
                Pantalla_Vista.ASISTENCIA
            }
            navegador.ir(
                destino,
                Asistencia_Vista.argumentos(idClase, nombreActividad, registro.fecha),
            )
        }

        item.tvSinDetalle.visibility =
            if (registro.alumnos.isEmpty()) View.VISIBLE else View.GONE
        registro.alumnos.forEach { alumno ->
            val fila = ItemDetalleAsistenciaBinding.inflate(
                layoutInflater, item.contenedorDetalle, false,
            )
            fila.tvNombreDetalle.text = alumno.nombre
            fila.tvMatriculaDetalle.text = "Número de control ${alumno.matricula}"
            fila.tvEstadoDetalle.text = if (alumno.presente) "Presente" else "Ausente"
            fila.tvEstadoDetalle.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (alumno.presente) R.color.itsm_success else R.color.itsm_danger,
                ),
            )
            item.contenedorDetalle.addView(fila.root)
        }
        binding.listaContenido.addView(item.root)
    }

    companion object {
        private const val ARG_ID_ACTIVIDAD = "id_actividad"
        private const val ARG_NOMBRE = "nombre_actividad"

        fun argumentos(idActividad: Int, nombre: String) = Bundle().apply {
            putInt(ARG_ID_ACTIVIDAD, idActividad)
            putString(ARG_NOMBRE, nombre)
        }

        private fun fechaLegible(iso: String): String =
            runCatching {
                val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val salida = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                salida.format(entrada.parse(iso)!!)
            }.getOrDefault(iso)
    }
}
