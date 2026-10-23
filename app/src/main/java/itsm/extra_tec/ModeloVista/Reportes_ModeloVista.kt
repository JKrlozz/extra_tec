package itsm.extra_tec.ModeloVista

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Archivos_Modelo
import itsm.extra_tec.Modelo.Reportes_Modelo

class Reportes_ModeloVista : Base_ModeloVista() {
    // Pares de clave de la API y etiqueta para mostrar
    val tipos = listOf("actividad" to "Por actividad", "alumno" to "Por alumno", "docente" to "Por docente")

    private val _texto = MutableLiveData("")
    val texto: LiveData<String> = _texto

    private var titulo = ""

    fun generar(posicionTipo: Int, desde: String, hasta: String) {
        val formato = Regex("\\d{4}-\\d{2}-\\d{2}")
        if ((desde.isNotEmpty() && !formato.matches(desde)) || (hasta.isNotEmpty() && !formato.matches(hasta))) {
            return avisar("Las fechas deben tener el formato AAAA-MM-DD.")
        }
        val tipo = tipos[posicionTipo]
        Reportes_Modelo.generar(tipo.first, desde, hasta) { resultado ->
            atender(resultado) { filas ->
                val rango = if (desde.isEmpty() && hasta.isEmpty()) "todo el historial"
                else "${desde.ifEmpty { "inicio" }} a ${hasta.ifEmpty { "hoy" }}"
                titulo = "Reporte ${tipo.second.lowercase()} ($rango)"
                _texto.value = "$titulo\n${filas.size} registros\n\n" + filas.joinToString("\n") { describir(tipo.first, it) }
            }
        }
    }

    fun exportar(contexto: Context) {
        val contenido = _texto.value.orEmpty()
        if (contenido.isEmpty()) return avisar("Primero genera un reporte.")
        runCatching {
            val pdf = Archivos_Modelo.textoAPdf("Actividades extraescolares ITSM", contenido)
            Archivos_Modelo.guardarEnDescargas(contexto, "reporte-${System.currentTimeMillis() / 1000}.pdf", "application/pdf", pdf)
        }.onSuccess { avisar("Reporte guardado en $it") }
            .onFailure { avisar("No se pudo exportar: ${it.message}") }
    }

    private fun describir(tipo: String, fila: Map<String, String>): String {
        fun dato(clave: String) = fila[clave].orEmpty()
        val asistencia = dato("asistencia_pct").let { if (it.isEmpty()) "sin datos" else "$it %" }
        return when (tipo) {
            "actividad" -> "• ${dato("actividad")} (${dato("docente").ifEmpty { "sin docente" }}): " +
                "${dato("inscritos")}/${dato("cupo")} inscritos, ${dato("sesiones")} sesiones, asistencia $asistencia"
            "alumno" -> "• ${dato("alumno")} (${dato("matricula")}): ${dato("actividades")} actividades, asistencia $asistencia"
            else -> "• ${dato("docente")}: ${dato("actividades")} actividades, ${dato("sesiones_impartidas")} sesiones, ${dato("evidencias")} evidencias"
        }
    }
}
