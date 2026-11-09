package itsm.extra_tec.vistas.Comun

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import itsm.extra_tec.R
import itsm.extra_tec.databinding.ItemDetalleTarjetaBinding
import itsm.extra_tec.databinding.ItemTarjetaVistaBinding

class Accion_Vista(
    val texto: String,
    val activa: Boolean = true,
    val peligrosa: Boolean = false,
    val alPulsar: () -> Unit,
)

// Tarjeta reutilizable con filas de etiqueta y valor
// Cada dato se muestra por separado
fun agregarTarjeta(
    contenedor: ViewGroup,
    titulo: String,
    linea1: String? = null,
    linea2: String? = null,
    progreso: Int? = null,
    detalles: List<Pair<String, String>> = emptyList(),
    a: Accion_Vista? = null,
    b: Accion_Vista? = null,
    c: Accion_Vista? = null,
    d: Accion_Vista? = null,
) {
    val tarjeta = ItemTarjetaVistaBinding.inflate(LayoutInflater.from(contenedor.context), contenedor, false)
    tarjeta.tvTitulo.text = titulo
    for ((vista, texto) in listOf(tarjeta.tvLinea1 to linea1, tarjeta.tvLinea2 to linea2)) {
        vista.visibility = if (texto.isNullOrBlank()) View.GONE else View.VISIBLE
        vista.text = texto
    }

    val visibles = detalles.filter { it.second.isNotBlank() }
    tarjeta.contenedorDetalles.visibility = if (visibles.isEmpty()) View.GONE else View.VISIBLE
    visibles.forEach { (etiqueta, valor) ->
        val fila = ItemDetalleTarjetaBinding.inflate(
            LayoutInflater.from(contenedor.context), tarjeta.contenedorDetalles, false,
        )
        fila.tvEtiqueta.text = etiqueta
        fila.tvValor.text = valor
        tarjeta.contenedorDetalles.addView(fila.root)
    }

    if (progreso != null) {
        tarjeta.pbProgreso.progress = progreso
        tarjeta.pbProgreso.visibility = View.VISIBLE
    }

    val acciones = listOfNotNull(a, b, c, d)
    val botones = listOf(tarjeta.btnA, tarjeta.btnB, tarjeta.btnC, tarjeta.btnD)
    botones.forEachIndexed { indice, boton ->
        configurar(boton, acciones.getOrNull(indice), indice == 0)
    }
    tarjeta.filaBotones1.visibility = if (acciones.isNotEmpty()) View.VISIBLE else View.GONE
    tarjeta.filaBotones2.visibility = if (acciones.size > 2) View.VISIBLE else View.GONE
    contenedor.addView(tarjeta.root)
}

private fun configurar(boton: MaterialButton, accion: Accion_Vista?, principal: Boolean) {
    if (accion == null) {
        boton.visibility = View.GONE
        return
    }
    boton.text = accion.texto
    boton.isEnabled = accion.activa
    boton.visibility = View.VISIBLE
    if (accion.peligrosa) {
        boton.setTextColor(ContextCompat.getColor(boton.context, R.color.itsm_danger))
        boton.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(boton.context, R.color.itsm_danger_light))
    } else if (principal) {
        boton.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(boton.context, R.color.itsm_primary))
        boton.setTextColor(ContextCompat.getColor(boton.context, R.color.white))
    }
    boton.setOnClickListener { accion.alPulsar() }
}
