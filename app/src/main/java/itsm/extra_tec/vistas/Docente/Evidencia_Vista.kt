package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Evidencia_ModeloVista
import itsm.extra_tec.databinding.EvidenciaVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.texto

class Evidencia_Vista : Base_Vista<EvidenciaVistaBinding>() {
    private val modelo: Evidencia_ModeloVista by viewModels()

    private val elegirFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) modelo.elegirFoto(uri)
    }

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        EvidenciaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.tvSubtitulo.text = "Selecciona la actividad y la fotografía"

        modelo.clases.observe(viewLifecycleOwner) { clases ->
            llenarSpinner(binding.spinnerActividad, clases) { it.nombre }
            val actual = clases.indexOfFirst { it.id == sesion.claseId }
            if (actual >= 0) binding.spinnerActividad.setSelection(actual)
        }
        modelo.foto.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.ivVistaPrevia.setImageURI(uri)
                binding.ivVistaPrevia.visibility = View.VISIBLE
            }
        }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.ir(Pantalla_Vista.INICIO_DOCENTE) }
        }

        binding.btnElegirFoto.setOnClickListener { elegirFoto.launch("image/*") }
        binding.btnGuardarEvidencia.setOnClickListener {
            modelo.guardar(requireContext().contentResolver, binding.spinnerActividad.selectedItemPosition, binding.etDescripcion.texto())
        }
    }
}
