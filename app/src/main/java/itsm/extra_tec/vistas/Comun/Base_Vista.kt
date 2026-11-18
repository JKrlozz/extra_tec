package itsm.extra_tec.vistas.Comun

import android.app.DatePickerDialog
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import itsm.extra_tec.ModeloVista.Base_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.ModeloVista.Fechas_ModeloVista
import itsm.extra_tec.ModeloVista.Sesion_ModeloVista
import java.util.Calendar

// texto del campo sin espacios
fun EditText.texto(): String {
    return text.toString().trim()
}

// cosas que usan todas las pantallas
open class Base_Vista : Fragment() {
    protected val sesion: Sesion_ModeloVista by activityViewModels()

    protected val navegador: Navegador_Vista
        get() = requireActivity() as Navegador_Vista

    protected val semestres = listOf(
        "Primero", "Segundo", "Tercero", "Cuarto", "Quinto",
        "Sexto", "Séptimo", "Octavo",
    )

    // muestra los mensajes del viewmodel
    protected fun escuchar(modelo: Base_ModeloVista) {
        modelo.mensaje.observe(viewLifecycleOwner) { evento ->
            val texto = evento.obtener()
            if (texto != null) {
                aviso(texto)
            }
        }
    }

    protected fun aviso(texto: String) {
        Toast.makeText(requireContext(), texto, Toast.LENGTH_LONG).show()
    }

    protected fun cerrarSesion() {
        sesion.cerrar()
        navegador.irRaiz(Pantalla_Vista.INICIO_SESION, null)
    }

    protected fun confirmar(mensaje: String, alAceptar: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setMessage(mensaje)
            .setPositiveButton("Sí") { _, _ -> alAceptar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    protected fun llenarSpinner(spinner: Spinner, textos: List<String>) {
        val adaptador = ArrayAdapter(requireContext(), R.layout.item_spinner, textos)
        adaptador.setDropDownViewResource(R.layout.item_spinner)
        spinner.adapter = adaptador
    }

    protected fun alEscribir(campo: EditText, accion: (String) -> Unit) {
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, inicio: Int, cantidad: Int, despues: Int) {}
            override fun onTextChanged(s: CharSequence?, inicio: Int, antes: Int, cantidad: Int) {}
            override fun afterTextChanged(s: Editable?) {
                accion(s.toString())
            }
        })
    }

    // solo deja escribir la forma 000T0000
    protected fun filtrarNumeroControl(campo: EditText) {
        val forma = Regex("^[0-9]{0,3}$|^[0-9]{3}[Tt][0-9]{0,4}$")
        val filtro = InputFilter { fuente, inicio, fin, actual, desde, hasta ->
            val nuevo = actual.substring(0, desde) + fuente.substring(inicio, fin) + actual.substring(hasta)
            if (forma.matches(nuevo)) {
                null
            } else {
                ""
            }
        }
        campo.filters = arrayOf(filtro, InputFilter.AllCaps(), InputFilter.LengthFilter(8))
    }

    // lista cargando vacia o con datos
    protected fun estadoLista(texto: TextView, elementos: List<*>?, vacio: String) {
        if (elementos == null) {
            texto.visibility = View.VISIBLE
            texto.text = "Cargando"
        } else if (elementos.isEmpty()) {
            texto.visibility = View.VISIBLE
            texto.text = vacio
        } else {
            texto.visibility = View.GONE
        }
    }

    // la clave se muestra una vez
    protected fun mostrarClaveInicial(clave: String?, alCerrar: () -> Unit) {
        if (clave == null) {
            alCerrar()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Contraseña inicial")
            .setMessage("Anótala y entrégala al usuario; no se volverá a mostrar:\n\n$clave")
            .setPositiveButton("Entendido") { _, _ -> alCerrar() }
            .setCancelable(false)
            .show()
    }

    // calendario del telefono
    // hastaHoy bloquea dias futuros
    protected fun elegirFecha(fechaActual: String, hastaHoy: Boolean, alElegir: (String) -> Unit) {
        val calendario = Calendar.getInstance()
        val partes = fechaActual.split("-")
        if (partes.size == 3) {
            val anio = partes[0].toIntOrNull()
            val mes = partes[1].toIntOrNull()
            val dia = partes[2].toIntOrNull()
            if (anio != null && mes != null && dia != null) {
                calendario.set(anio, mes - 1, dia)
            }
        }
        val selector = DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia -> alElegir(Fechas_ModeloVista.desdeCalendario(anio, mes, dia)) },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH),
        )
        if (hastaHoy) {
            selector.datePicker.maxDate = System.currentTimeMillis()
        }
        selector.show()
    }

    // abre la foto en grande
    protected fun verFotos(fuentes: List<String>, inicio: Int) {
        if (fuentes.isEmpty()) {
            return
        }
        val visor = VisorFotos_Vista(requireContext(), fuentes, inicio)
        visor.mostrar()
    }
}
