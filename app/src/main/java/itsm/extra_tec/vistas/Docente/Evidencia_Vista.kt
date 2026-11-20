package itsm.extra_tec.vistas.Docente

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Evidencias_Modelo
import itsm.extra_tec.ModeloVista.Evidencia_ModeloVista
import itsm.extra_tec.ModeloVista.Fechas_ModeloVista
import itsm.extra_tec.ModeloVista.FotoFormulario
import itsm.extra_tec.R
import itsm.extra_tec.databinding.EvidenciaVistaBinding
import itsm.extra_tec.databinding.ItemFotoEvidenciaVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Fotos_Vista
import itsm.extra_tec.vistas.Comun.texto
import java.io.File

class Evidencia_Vista : Base_Vista() {
    private var _binding: EvidenciaVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: Evidencia_ModeloVista by viewModels()

    private var fecha = Fechas_ModeloVista.hoy()
    private var precargada = false
    private var idActividadEvidencia = -1

    // el selector de fotos del telefono
    private val elegirFotos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            modelo.agregarFotos(uris)
        }
    }

    // la foto que se esta tomando con la camara
    private var uriCamara: Uri? = null

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        val uri = uriCamara
        uriCamara = null
        if (tomada && uri != null) {
            modelo.agregarFotos(listOf(uri))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val guardada = savedInstanceState?.getString(ESTADO_URI_CAMARA)
        if (guardada != null) {
            uriCamara = Uri.parse(guardada)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        uriCamara?.let { outState.putString(ESTADO_URI_CAMARA, it.toString()) }
    }

    private fun abrirCamara() {
        if (!modelo.puedeAgregarFoto()) {
            return
        }
        // archivo temporal en cache para la foto
        val carpeta = File(requireContext().cacheDir, "camara")
        carpeta.mkdirs()
        val archivo = File(carpeta, "foto_" + System.currentTimeMillis() + ".jpg")
        val uri = FileProvider.getUriForFile(
            requireContext(), requireContext().packageName + ".archivos", archivo,
        )
        uriCamara = uri
        try {
            tomarFoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            uriCamara = null
            archivo.delete()
            modelo.avisarSinCamara()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = EvidenciaVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val idEvidencia = leerNumero(ARG_ID)
        val idActividadFija = leerNumero(ARG_ID_ACTIVIDAD)
        val nombreActividad = arguments?.getString(ARG_NOMBRE) ?: ""
        modelo.cargar(idEvidencia)

        if (idEvidencia == null) {
            binding.tvTitulo.text = "Subir evidencia"
            binding.btnGuardarEvidencia.text = "Guardar evidencia"
        } else {
            binding.tvTitulo.text = "Editar evidencia"
            binding.btnGuardarEvidencia.text = "Guardar cambios"
            // al editar no cambia actividad
            binding.spinnerActividad.isEnabled = false
            binding.spinnerActividad.alpha = 0.6f
        }
        if (nombreActividad.isBlank()) {
            binding.tvSubtitulo.text = "Selecciona la actividad, la fecha y las fotografías"
        } else {
            binding.tvSubtitulo.text = nombreActividad
        }

        val estados = mutableListOf<String>()
        for (estado in Evidencias_Modelo.ESTADOS) {
            estados.add(estado[1])
        }
        llenarSpinner(binding.spinnerEstado, estados)
        actualizarFecha()

        modelo.clases.observe(viewLifecycleOwner) { clases ->
            val nombres = mutableListOf<String>()
            for (clase in clases) {
                nombres.add(clase.nombre)
            }
            llenarSpinner(binding.spinnerActividad, nombres)
            // se busca la actividad que toca
            var deseada = sesion.claseId ?: -1
            if (idActividadEvidencia > 0) deseada = idActividadEvidencia
            if (idActividadFija != null) deseada = idActividadFija
            for (i in clases.indices) {
                if (clases[i].id == deseada) binding.spinnerActividad.setSelection(i)
            }
        }
        modelo.evidencia.observe(viewLifecycleOwner) { evidencia ->
            if (evidencia == null) {
                return@observe
            }
            idActividadEvidencia = evidencia.idActividad
            val clases = modelo.clases.value ?: listOf()
            for (i in clases.indices) {
                if (clases[i].id == evidencia.idActividad) binding.spinnerActividad.setSelection(i)
            }
            // los datos se ponen una vez
            if (precargada) {
                return@observe
            }
            precargada = true
            fecha = evidencia.fecha
            actualizarFecha()
            binding.etDescripcion.setText(evidencia.descripcion)
            for (i in Evidencias_Modelo.ESTADOS.indices) {
                if (Evidencias_Modelo.ESTADOS[i][0] == evidencia.estado) binding.spinnerEstado.setSelection(i)
            }
        }
        modelo.fotos.observe(viewLifecycleOwner) { fotos -> mostrarFotos(fotos) }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            if (evento.obtener() != null) {
                navegador.atras()
            }
        }

        binding.btnFecha.setOnClickListener {
            elegirFecha(fecha, true) { nueva ->
                fecha = nueva
                actualizarFecha()
            }
        }
        binding.btnElegirFotos.setOnClickListener { elegirFotos.launch("image/*") }
        binding.btnTomarFoto.setOnClickListener { abrirCamara() }
        binding.btnGuardarEvidencia.setOnClickListener {
            val posicionEstado = binding.spinnerEstado.selectedItemPosition
            modelo.guardar(
                requireContext().contentResolver,
                binding.spinnerActividad.selectedItemPosition,
                fecha,
                Evidencias_Modelo.ESTADOS[posicionEstado][0],
                binding.etDescripcion.texto(),
            )
        }
    }

    private fun actualizarFecha() {
        binding.btnFecha.text = "Fecha: " + Fechas_ModeloVista.legible(fecha)
    }

    private fun mostrarFotos(fotos: List<FotoFormulario>) {
        binding.listaFotos.removeAllViews()
        if (fotos.isEmpty()) {
            binding.tvSinFotos.visibility = View.VISIBLE
        } else {
            binding.tvSinFotos.visibility = View.GONE
        }
        // todas las fotos para el visor
        val fuentes = mutableListOf<String>()
        for (foto in fotos) {
            fuentes.add(foto.fuente)
        }
        val tamano = resources.getDimensionPixelSize(R.dimen.tamano_miniatura)
        for (i in fotos.indices) {
            val fila = ItemFotoEvidenciaVistaBinding.inflate(layoutInflater, binding.listaFotos, false)
            if (fotos[i].id != null) {
                fila.tvNombre.text = "Fotografía guardada"
            } else {
                fila.tvNombre.text = "Fotografía nueva"
            }
            Fotos_Vista.mostrar(fila.ivFoto, fuentes[i], tamano)
            fila.ivFoto.setOnClickListener { verFotos(fuentes, i) }
            fila.btnQuitar.setOnClickListener { modelo.quitarFoto(i) }
            binding.listaFotos.addView(fila.root)
        }
    }

    private fun leerNumero(clave: String): Int? {
        val numero = arguments?.getInt(clave, -1) ?: -1
        if (numero > 0) {
            return numero
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // las fotos temporales se borran al salir de la pantalla
        val actividad = activity
        if (isRemoving || actividad == null || actividad.isFinishing) {
            File(requireContext().cacheDir, "camara").deleteRecursively()
        }
    }

    companion object {
        private const val ESTADO_URI_CAMARA = "uri_camara"
        private const val ARG_ID = "id_evidencia"
        private const val ARG_ID_ACTIVIDAD = "id_actividad"
        private const val ARG_NOMBRE = "nombre_actividad"

        // sin id es evidencia nueva
        fun argumentos(idEvidencia: Int?, idActividad: Int, nombreActividad: String): Bundle {
            val datos = Bundle()
            if (idEvidencia != null) {
                datos.putInt(ARG_ID, idEvidencia)
            }
            datos.putInt(ARG_ID_ACTIVIDAD, idActividad)
            datos.putString(ARG_NOMBRE, nombreActividad)
            return datos
        }
    }
}
