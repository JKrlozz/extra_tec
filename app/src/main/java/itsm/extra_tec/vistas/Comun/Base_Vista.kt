package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import itsm.extra_tec.ModeloVista.Base_ModeloVista
import itsm.extra_tec.ModeloVista.Sesion_ModeloVista

fun EditText.texto(): String = text.toString().trim()

abstract class Base_Vista<B : ViewBinding> : Fragment() {
    private var _binding: B? = null
    protected val binding: B get() = _binding!!

    protected val sesion: Sesion_ModeloVista by activityViewModels()
    protected val navegador: Navegador_Vista get() = requireActivity() as Navegador_Vista

    protected abstract fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?): B

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = crearBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // Muestra los mensajes que emite el ViewModel
    protected fun escuchar(modelo: Base_ModeloVista) {
        modelo.mensaje.observe(viewLifecycleOwner) { evento -> evento.obtener()?.let { aviso(it) } }
    }

    protected fun aviso(texto: String) {
        Toast.makeText(requireContext(), texto, Toast.LENGTH_LONG).show()
    }

    protected fun cerrarSesion() {
        sesion.cerrar()
        navegador.ir(Pantalla_Vista.INICIO_SESION)
    }

    protected fun confirmar(mensaje: String, alAceptar: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setMessage(mensaje)
            .setPositiveButton("Sí") { _, _ -> alAceptar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    protected fun <T> llenarSpinner(spinner: Spinner, elementos: List<T>, etiqueta: (T) -> String) {
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, elementos.map(etiqueta))
    }

    protected fun alEscribir(campo: EditText, accion: (String) -> Unit) {
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, inicio: Int, cantidad: Int, despues: Int) {}
            override fun onTextChanged(s: CharSequence?, inicio: Int, antes: Int, cantidad: Int) {}
            override fun afterTextChanged(s: Editable?) = accion(s.toString())
        })
    }

    // Texto de estado de una lista: carga, vacía o con elementos (null oculta el texto)
    protected fun estadoLista(texto: TextView, elementos: List<*>?, vacio: String) {
        val mensaje = when {
            elementos == null -> "Cargando…"
            elementos.isEmpty() -> vacio
            else -> null
        }
        texto.visibility = if (mensaje == null) View.GONE else View.VISIBLE
        texto.text = mensaje
    }

    // El servidor entrega la contraseña inicial una sola vez
    protected fun mostrarClaveInicial(clave: String?, alCerrar: () -> Unit) {
        if (clave == null) return alCerrar()
        AlertDialog.Builder(requireContext())
            .setTitle("Contraseña inicial")
            .setMessage("Anótala y entrégala al usuario; no se volverá a mostrar:\n\n$clave")
            .setPositiveButton("Entendido") { _, _ -> alCerrar() }
            .setCancelable(false)
            .show()
    }
}
