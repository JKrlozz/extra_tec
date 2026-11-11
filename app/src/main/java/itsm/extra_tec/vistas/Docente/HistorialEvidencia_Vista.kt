package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Evidencia
import itsm.extra_tec.ModeloVista.Fechas_ModeloVista
import itsm.extra_tec.ModeloVista.HistorialEvidencia_ModeloVista
import itsm.extra_tec.databinding.HistorialEvidenciaVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class HistorialEvidencia_Vista : Base_Vista() {
    private var _binding: HistorialEvidenciaVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: HistorialEvidencia_ModeloVista by viewModels()

    private var idActividad = -1
    private var nombreActividad = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = HistorialEvidenciaVistaBinding.inflate(inflater, container, false)
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

        modelo.evidencias.observe(viewLifecycleOwner) { evidencias ->
            estadoLista(binding.tvVacio, evidencias, "Aún no hay evidencias registradas.")
            binding.listaContenido.removeAllViews()
            if (evidencias != null) {
                for (evidencia in evidencias) {
                    agregarEvidencia(evidencia)
                }
            }
        }

        binding.btnNuevaEvidencia.setOnClickListener {
            navegador.ir(pantallaEvidencia(), Evidencia_Vista.argumentos(null, idActividad, nombreActividad))
        }
    }

    private fun agregarEvidencia(evidencia: Evidencia) {
        var textoFotos = "${evidencia.fotos.size} fotografías"
        if (evidencia.fotos.size == 1) {
            textoFotos = "1 fotografía"
        }
        val detalles = mutableListOf(
            "Estado" to evidencia.estadoEtiqueta,
            "Fotografías" to textoFotos,
            "Registrada" to evidencia.marcaTiempo,
            "Descripción" to evidencia.descripcion.ifBlank { "Sin descripción" },
        )
        if (evidencia.editada) {
            detalles.add("Última edición" to (evidencia.actualizadaEn ?: "").take(16))
        }

        // rutas para el visor
        val fotos = mutableListOf<String>()
        for (foto in evidencia.fotos) {
            fotos.add(foto.ruta)
        }

        agregarTarjeta(
            binding.listaContenido,
            "Evidencia del " + Fechas_ModeloVista.legible(evidencia.fecha),
            detalles = detalles,
            fotos = fotos,
            alTocarFoto = { posicion -> verFotos(fotos, posicion) },
            a = Accion_Vista("Editar") {
                navegador.ir(pantallaEvidencia(), Evidencia_Vista.argumentos(evidencia.id, idActividad, nombreActividad))
            },
            b = Accion_Vista("Eliminar", true, true) {
                confirmar("¿Eliminar la evidencia del " + Fechas_ModeloVista.legible(evidencia.fecha) + "?") {
                    modelo.eliminar(evidencia.id)
                }
            },
        )
    }

    private fun pantallaEvidencia(): Pantalla_Vista {
        if (sesion.rol == "admin") {
            return Pantalla_Vista.EVIDENCIA_ADMIN
        }
        return Pantalla_Vista.EVIDENCIA
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
