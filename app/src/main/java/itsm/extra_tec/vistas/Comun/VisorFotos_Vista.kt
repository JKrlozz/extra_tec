package itsm.extra_tec.vistas.Comun

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import itsm.extra_tec.R
import itsm.extra_tec.databinding.VisorFotosVistaBinding

// ventana negra para ver fotos grandes
class VisorFotos_Vista(contexto: Context, private val fuentes: List<String>, inicio: Int) {
    private val ventana = Dialog(contexto, R.style.Itsm_Visor)
    private val binding = VisorFotosVistaBinding.inflate(LayoutInflater.from(ventana.context))
    private var actual = inicio

    fun mostrar() {
        ventana.setContentView(binding.root)
        // la ventana ocupa toda la pantalla
        val ventanaAndroid = ventana.window
        if (ventanaAndroid != null) {
            ventanaAndroid.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        binding.btnCerrar.setOnClickListener { ventana.dismiss() }
        binding.btnAnterior.setOnClickListener { cambiar(-1) }
        binding.btnSiguiente.setOnClickListener { cambiar(1) }
        pintar()
        ventana.show()
    }

    private fun cambiar(paso: Int) {
        val nueva = actual + paso
        if (nueva < 0 || nueva >= fuentes.size) {
            return
        }
        actual = nueva
        pintar()
    }

    private fun pintar() {
        if (actual < 0 || actual >= fuentes.size) {
            actual = 0
        }
        binding.ivFoto.setImageDrawable(null)
        binding.tvContador.text = "Foto " + (actual + 1) + " de " + fuentes.size
        binding.btnAnterior.isEnabled = actual > 0
        binding.btnSiguiente.isEnabled = actual < fuentes.size - 1
        // mas grande que la miniatura
        Fotos_Vista.mostrar(binding.ivFoto, fuentes[actual], 1400) {
            binding.ivFoto.reiniciarZoom()
        }
    }
}
