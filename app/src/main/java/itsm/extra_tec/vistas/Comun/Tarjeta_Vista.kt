package itsm.extra_tec.vistas.Comun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import itsm.extra_tec.R
import itsm.extra_tec.databinding.ItemDetalleTarjetaBinding
import itsm.extra_tec.databinding.ItemTarjetaVistaBinding

// boton de una tarjeta
class Accion_Vista(
    val texto: String,
    val activa: Boolean = true,
    val peligrosa: Boolean = false,
    val alPulsar: () -> Unit,
)

// tarjeta con etiqueta y valor
// lleva fotos y cuatro botones
fun agregarTarjeta(
    contenedor: ViewGroup,
    titulo: String,
    linea1: String? = null,
    linea2: String? = null,
    progreso: Int? = null,
    detalles: List<Pair<String, String>> = listOf(),
    fotos: List<String> = listOf(),
    alTocarFoto: ((Int) -> Unit)? = null,
    a: Accion_Vista? = null,
    b: Accion_Vista? = null,
    c: Accion_Vista? = null,
    d: Accion_Vista? = null,
) {
    val inflador = LayoutInflater.from(contenedor.context)
    val tarjeta = ItemTarjetaVistaBinding.inflate(inflador, contenedor, false)
    tarjeta.tvTitulo.text = titulo
    ponerTexto(tarjeta.tvLinea1, linea1)
    ponerTexto(tarjeta.tvLinea2, linea2)

    // solo datos con valor
    for (detalle in detalles) {
        if (detalle.second.isNotBlank()) {
            val fila = ItemDetalleTarjetaBinding.inflate(inflador, tarjeta.contenedorDetalles, false)
            fila.tvEtiqueta.text = detalle.first
            fila.tvValor.text = detalle.second
            tarjeta.contenedorDetalles.addView(fila.root)
            tarjeta.contenedorDetalles.visibility = View.VISIBLE
        }
    }

    // miniaturas de las fotos
    if (fotos.isNotEmpty()) {
        tarjeta.scrollFotos.visibility = View.VISIBLE
        val tamano = contenedor.resources.getDimensionPixelSize(R.dimen.tamano_miniatura)
        for (i in fotos.indices) {
            val miniatura = inflador.inflate(R.layout.item_miniatura_foto, tarjeta.contenedorFotos, false) as ImageView
            Fotos_Vista.mostrar(miniatura, fotos[i], tamano)
            miniatura.setOnClickListener {
                if (alTocarFoto != null) {
                    alTocarFoto(i)
                }
            }
            tarjeta.contenedorFotos.addView(miniatura)
        }
    }

    if (progreso != null) {
        tarjeta.pbProgreso.progress = progreso
        tarjeta.pbProgreso.visibility = View.VISIBLE
    }

    val acciones = listOfNotNull(a, b, c, d)
    configurar(tarjeta.btnA, acciones.getOrNull(0), true)
    configurar(tarjeta.btnB, acciones.getOrNull(1), false)
    configurar(tarjeta.btnC, acciones.getOrNull(2), false)
    configurar(tarjeta.btnD, acciones.getOrNull(3), false)
    if (acciones.isNotEmpty()) {
        tarjeta.filaBotones1.visibility = View.VISIBLE
    }
    if (acciones.size > 2) {
        tarjeta.filaBotones2.visibility = View.VISIBLE
    }
    contenedor.addView(tarjeta.root)
}

private fun ponerTexto(vista: android.widget.TextView, texto: String?) {
    if (texto.isNullOrBlank()) {
        vista.visibility = View.GONE
    } else {
        vista.visibility = View.VISIBLE
        vista.text = texto
    }
}

// primero verde y peligroso rojo
private fun configurar(boton: MaterialButton, accion: Accion_Vista?, principal: Boolean) {
    if (accion == null) {
        boton.visibility = View.GONE
        return
    }
    boton.text = accion.texto
    boton.isEnabled = accion.activa
    boton.visibility = View.VISIBLE
    val contexto = boton.context
    if (accion.peligrosa) {
        boton.setTextColor(ContextCompat.getColorStateList(contexto, R.color.boton_peligro_texto))
        boton.backgroundTintList = ContextCompat.getColorStateList(contexto, R.color.boton_peligro_fondo)
    } else if (principal) {
        boton.setTextColor(ContextCompat.getColorStateList(contexto, R.color.boton_primario_texto))
        boton.backgroundTintList = ContextCompat.getColorStateList(contexto, R.color.boton_primario_fondo)
    }
    boton.setOnClickListener { accion.alPulsar() }
}
