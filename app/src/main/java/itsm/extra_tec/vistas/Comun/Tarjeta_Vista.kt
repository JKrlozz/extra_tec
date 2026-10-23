package itsm.extra_tec.vistas.Comun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import itsm.extra_tec.databinding.ItemTarjetaVistaBinding

class Accion_Vista(val texto: String, val activa: Boolean = true, val alPulsar: () -> Unit)

fun agregarTarjeta(
    contenedor: ViewGroup,
    titulo: String,
    linea1: String? = null,
    linea2: String? = null,
    progreso: Int? = null,
    a: Accion_Vista? = null,
    b: Accion_Vista? = null,
    c: Accion_Vista? = null,
) {
    val tarjeta = ItemTarjetaVistaBinding.inflate(LayoutInflater.from(contenedor.context), contenedor, false)
    tarjeta.tvTitulo.text = titulo
    for ((vista, texto) in listOf(tarjeta.tvLinea1 to linea1, tarjeta.tvLinea2 to linea2)) {
        vista.visibility = if (texto.isNullOrBlank()) View.GONE else View.VISIBLE
        vista.text = texto
    }
    if (progreso != null) {
        tarjeta.pbProgreso.progress = progreso
        tarjeta.pbProgreso.visibility = View.VISIBLE
    }
    fun configurar(boton: MaterialButton, accion: Accion_Vista?) {
        if (accion == null) {
            boton.visibility = View.GONE
            return
        }
        boton.text = accion.texto
        boton.isEnabled = accion.activa
        boton.visibility = View.VISIBLE
        boton.setOnClickListener { accion.alPulsar() }
    }
    configurar(tarjeta.btnA, a)
    configurar(tarjeta.btnB, b)
    configurar(tarjeta.btnC, c)
    if (a != null || b != null || c != null) tarjeta.filaBotones.visibility = View.VISIBLE
    contenedor.addView(tarjeta.root)
}
