package itsm.extra_tec.ModeloVista

import java.text.SimpleDateFormat
import java.util.Locale

// cambia aaaa-mm-dd a dd/mm/aaaa
object Fechas_ModeloVista {
    fun legible(iso: String): String {
        val partes = iso.take(10).split("-")
        if (partes.size != 3) {
            return iso
        }
        return partes[2] + "/" + partes[1] + "/" + partes[0]
    }

    // fecha desde el calendario
    fun desdeCalendario(anio: Int, mes: Int, dia: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, dia)
    }

    fun hoy(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
    }
}
