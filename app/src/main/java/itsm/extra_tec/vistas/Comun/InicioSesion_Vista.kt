package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Api_Modelo
import itsm.extra_tec.ModeloVista.InicioSesion_ModeloVista
import itsm.extra_tec.databinding.InicioSesionVistaBinding

class InicioSesion_Vista : Base_Vista<InicioSesionVistaBinding>() {
    private val modelo: InicioSesion_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        InicioSesionVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        llenarSpinner(binding.spinnerRol, listOf("alumno", "docente", "admin")) { it }

        binding.btnIniciarSesion.setOnClickListener {
            modelo.iniciar(
                binding.etCorreo.texto(),
                binding.etContrasena.text.toString(),
                binding.spinnerRol.selectedItem.toString(),
            )
        }
        binding.btnIrRegistro.setOnClickListener { navegador.ir(Pantalla_Vista.REGISTRO) }
        binding.btnServidor.setOnClickListener { configurarServidor() }

        // estado de la búsqueda automática; al tocarlo se repite
        sesion.servidor.observe(viewLifecycleOwner) { binding.tvEstadoServidor.text = it }
        binding.tvEstadoServidor.setOnClickListener { sesion.buscarServidor() }

        modelo.cargando.observe(viewLifecycleOwner) { binding.btnIniciarSesion.isEnabled = !it }
        modelo.sesionIniciada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let {
                sesion.iniciar(it)
                navegador.irAlInicio()
            }
        }
    }

    private fun configurarServidor() {
        val entrada = EditText(requireContext()).apply {
            setText(sesion.urlServidor())
            hint = "http://192.168.1.10:3000"
            setSingleLine()
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Dirección del servidor")
            .setMessage("Emulador: ${Api_Modelo.URL_EMULADOR}\nTeléfono real: la IP de tu PC, por ejemplo http://192.168.1.10:3000")
            .setView(entrada)
            .setPositiveButton("Guardar") { _, _ ->
                val url = entrada.texto().ifEmpty { Api_Modelo.URL_EMULADOR }
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    sesion.guardarUrl(url)
                    aviso("Servidor guardado: $url")
                } else {
                    aviso("La dirección debe empezar con http://")
                }
            }
            .setNeutralButton("Probar") { _, _ ->
                modelo.probarServidor(entrada.texto().ifEmpty { Api_Modelo.URL_EMULADOR })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
