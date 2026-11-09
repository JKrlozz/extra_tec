package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.HistorialEvidencia_ModeloVista
import itsm.extra_tec.databinding.HistorialEvidenciaVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class HistorialEvidencia_Vista : Base_Vista<HistorialEvidenciaVistaBinding>() {
    private val modelo: HistorialEvidencia_ModeloVista by viewModels()

    private val idActividad: Int? get() =
        arguments?.getInt(ARG_ID_ACTIVIDAD, -1)?.takeIf { it > 0 } ?: sesion.claseId

    private val nombreActividad: String get() =
        arguments?.getString(ARG_NOMBRE).orEmpty().ifBlank { sesion.claseNombre }

    private val pantallaEvidencia: Pantalla_Vista get() =
        if (sesion.rol == "admin") Pantalla_Vista.EVIDENCIA_ADMIN else Pantalla_Vista.EVIDENCIA

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        HistorialEvidenciaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val idClase = idActividad ?: return navegador.irAlInicio()
        binding.tvSubtitulo.text = nombreActividad
        modelo.cargar(idClase)

        modelo.evidencias.observe(viewLifecycleOwner) { evidencias ->
            estadoLista(binding.tvVacio, evidencias, "Aún no hay evidencias registradas.")
            binding.listaContenido.removeAllViews()
            evidencias?.forEach { evidencia ->
                val fotos = if (evidencia.fotos.size == 1) {
                    "1 fotografía"
                } else {
                    "${evidencia.fotos.size} fotografías"
                }
                val detalles = buildList {
                    add("Estado" to evidencia.estadoEtiqueta)
                    add("Fotografías" to fotos)
                    add("Registrada" to evidencia.marcaTiempo)
                    add("Descripción" to evidencia.descripcion.ifBlank { "Sin descripción" })
                    if (evidencia.editada) {
                        add("Última edición" to evidencia.actualizadaEn.orEmpty().take(16))
                    }
                }
                agregarTarjeta(
                    binding.listaContenido,
                    "Evidencia del ${fechaLegible(evidencia.fecha)}",
                    detalles = detalles,
                    b = Accion_Vista("Editar") {
                        navegador.ir(
                            pantallaEvidencia,
                            Evidencia_Vista.argumentos(
                                idEvidencia = evidencia.id,
                                idActividad = idClase,
                                nombreActividad = nombreActividad,
                            ),
                        )
                    },
                    c = Accion_Vista("Eliminar", peligrosa = true) {
                        confirmar("¿Eliminar la evidencia del ${evidencia.fecha}?") {
                            modelo.eliminar(evidencia.id)
                        }
                    },
                )
            }
        }

        binding.btnNuevaEvidencia.setOnClickListener {
            navegador.ir(
                pantallaEvidencia,
                Evidencia_Vista.argumentos(
                    idActividad = idClase,
                    nombreActividad = nombreActividad,
                ),
            )
        }
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
                val entrada = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val salida = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
                salida.format(entrada.parse(iso)!!)
            }.getOrDefault(iso)
    }
}
