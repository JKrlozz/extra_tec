package itsm.extra_tec.vistas.Docente

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Evidencias_Modelo
import itsm.extra_tec.ModeloVista.Evidencia_ModeloVista
import itsm.extra_tec.databinding.EvidenciaVistaBinding
import itsm.extra_tec.databinding.ItemFotoEvidenciaVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.texto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Evidencia_Vista : Base_Vista<EvidenciaVistaBinding>() {
    private val modelo: Evidencia_ModeloVista by viewModels()

    private var fecha = hoy()
    private var precargada = false
    private var idActividadEvidencia: Int? = null

    private val idEvidencia: Int? get() = argumento(ARG_ID)
    private val idActividadFija: Int? get() = argumento(ARG_ID_ACTIVIDAD)
    private val nombreActividad: String get() = arguments?.getString(ARG_NOMBRE).orEmpty()

    private val elegirFotos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) modelo.agregarFotos(uris)
    }

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        EvidenciaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        modelo.cargar(idEvidencia)

        binding.tvTitulo.text = if (idEvidencia == null) "Subir evidencia" else "Editar evidencia"
        binding.tvSubtitulo.text = nombreActividad.ifBlank {
            "Selecciona la actividad, la fecha y las fotografías"
        }
        binding.btnGuardarEvidencia.text = if (idEvidencia == null) "Guardar evidencia" else "Guardar cambios"
        // Al editar la evidencia conserva su actividad
        binding.spinnerActividad.isEnabled = idEvidencia == null
        binding.spinnerActividad.alpha = if (idEvidencia == null) 1f else 0.6f

        llenarSpinner(binding.spinnerEstado, Evidencias_Modelo.ESTADOS) { it.second }
        actualizarFecha()

        modelo.clases.observe(viewLifecycleOwner) { clases ->
            llenarSpinner(binding.spinnerActividad, clases) { it.nombre }
            val deseada = idActividadFija ?: idActividadEvidencia ?: sesion.claseId
            if (deseada != null) {
                val actual = clases.indexOfFirst { it.id == deseada }
                if (actual >= 0) binding.spinnerActividad.setSelection(actual)
            }
        }
        modelo.evidencia.observe(viewLifecycleOwner) { evidencia ->
            if (evidencia == null) return@observe
            idActividadEvidencia = evidencia.idActividad
            val clases = modelo.clases.value.orEmpty()
            val posicionClase = clases.indexOfFirst { it.id == evidencia.idActividad }
            if (posicionClase >= 0) binding.spinnerActividad.setSelection(posicionClase)
            if (precargada) return@observe
            precargada = true
            fecha = evidencia.fecha
            actualizarFecha()
            binding.etDescripcion.setText(evidencia.descripcion)
            val posicion = Evidencias_Modelo.ESTADOS.indexOfFirst { it.first == evidencia.estado }
            if (posicion >= 0) binding.spinnerEstado.setSelection(posicion)
        }
        modelo.fotos.observe(viewLifecycleOwner) { mostrarFotos(it) }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.atras() }
        }

        binding.btnFecha.setOnClickListener { elegirFecha() }
        binding.btnElegirFotos.setOnClickListener { elegirFotos.launch("image/*") }
        binding.btnGuardarEvidencia.setOnClickListener {
            modelo.guardar(
                requireContext().contentResolver,
                binding.spinnerActividad.selectedItemPosition,
                fecha,
                Evidencias_Modelo.ESTADOS[binding.spinnerEstado.selectedItemPosition].first,
                binding.etDescripcion.texto(),
            )
        }
    }

    private fun actualizarFecha() {
        binding.btnFecha.text = "Fecha: ${fechaLegible(fecha)}"
    }

    private fun elegirFecha() {
        val partes = fecha.split("-")
        val calendario = Calendar.getInstance().apply {
            if (partes.size == 3) {
                set(partes[0].toInt(), partes[1].toInt() - 1, partes[2].toInt())
            }
        }
        DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                fecha = String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, dia)
                actualizarFecha()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun mostrarFotos(fotos: List<Evidencia_ModeloVista.FotoFormulario>) {
        binding.listaFotos.removeAllViews()
        binding.tvSinFotos.visibility = if (fotos.isEmpty()) View.VISIBLE else View.GONE
        fotos.forEach { foto ->
            val fila = ItemFotoEvidenciaVistaBinding.inflate(layoutInflater, binding.listaFotos, false)
            fila.tvNombre.text = if (foto.id != null) "Fotografía guardada" else "Fotografía nueva"
            if (foto.uri != null) {
                fila.ivFoto.setImageURI(foto.uri)
            } else if (foto.ruta != null) {
                val opciones = BitmapFactory.Options().apply { inSampleSize = 4 }
                fila.ivFoto.setImageBitmap(BitmapFactory.decodeFile(foto.ruta, opciones))
            }
            fila.btnQuitar.setOnClickListener { modelo.quitarFoto(foto.clave) }
            binding.listaFotos.addView(fila.root)
        }
    }

    private fun argumento(clave: String): Int? =
        arguments?.getInt(clave, -1)?.takeIf { it > 0 }

    companion object {
        private const val ARG_ID = "id_evidencia"
        private const val ARG_ID_ACTIVIDAD = "id_actividad"
        private const val ARG_NOMBRE = "nombre_actividad"

        fun argumentos(
            idEvidencia: Int? = null,
            idActividad: Int? = null,
            nombreActividad: String = "",
        ) = Bundle().apply {
            if (idEvidencia != null) putInt(ARG_ID, idEvidencia)
            if (idActividad != null) putInt(ARG_ID_ACTIVIDAD, idActividad)
            if (nombreActividad.isNotBlank()) putString(ARG_NOMBRE, nombreActividad)
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
