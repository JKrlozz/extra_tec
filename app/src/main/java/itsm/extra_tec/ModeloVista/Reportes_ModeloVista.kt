package itsm.extra_tec.ModeloVista

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Archivos_Modelo
import itsm.extra_tec.Modelo.BloquePdf
import itsm.extra_tec.Modelo.Reportes_Modelo
import java.text.SimpleDateFormat
import java.util.Locale

data class Reporte(
    val tipo: String,
    val etiqueta: String,
    val rango: String,
    val filas: List<Map<String, String>>,
) {
    val registros: Int get() = filas.size
}

class Reportes_ModeloVista : Base_ModeloVista() {
    // Tipos de reporte y sus etiquetas
    val tipos = listOf("actividad" to "Por actividad", "alumno" to "Por alumno", "docente" to "Por docente")

    private val _reporte = MutableLiveData<Reporte?>(null)
    val reporte: LiveData<Reporte?> = _reporte

    fun generar(posicionTipo: Int, desde: String, hasta: String) {
        val formato = Regex("\\d{4}-\\d{2}-\\d{2}")
        if ((desde.isNotEmpty() && !formato.matches(desde)) || (hasta.isNotEmpty() && !formato.matches(hasta))) {
            return avisar("Las fechas deben tener el formato AAAA-MM-DD.")
        }
        val tipo = tipos[posicionTipo]
        Reportes_Modelo.generar(tipo.first, desde, hasta) { resultado ->
            atender(resultado) { filas ->
                val rango = when {
                    desde.isEmpty() && hasta.isEmpty() -> "Todo el historial"
                    desde.isEmpty() -> "Hasta el ${fechaLegible(hasta)}"
                    hasta.isEmpty() -> "Desde el ${fechaLegible(desde)}"
                    else -> "Del ${fechaLegible(desde)} al ${fechaLegible(hasta)}"
                }
                _reporte.value = Reporte(tipo.first, tipo.second, rango, filas)
            }
        }
    }

    fun exportar(contexto: Context) {
        val actual = _reporte.value ?: return avisar("Primero genera un reporte.")
        if (actual.filas.isEmpty()) return avisar("El reporte no tiene registros para exportar.")
        runCatching {
            val pdf = Archivos_Modelo.reportePdf(
                "Reporte ${actual.etiqueta.lowercase()}",
                "${actual.rango}\n${actual.registros} registros",
                bloques(actual),
            )
            Archivos_Modelo.guardarEnDescargas(
                contexto,
                "reporte-${actual.tipo}-${System.currentTimeMillis() / 1000}.pdf",
                "application/pdf",
                pdf,
            )
        }.onSuccess { avisar("Reporte guardado en $it") }
            .onFailure { avisar("No se pudo exportar: ${it.message}") }
    }

    // Los mismos datos se usan en las tarjetas y en el PDF
    fun bloques(actual: Reporte): List<BloquePdf> = actual.filas.map { fila ->
        fun dato(clave: String) = fila[clave].orEmpty()
        val asistencia = dato("asistencia_pct").let { if (it.isEmpty()) "Sin datos" else "$it %" }
        when (actual.tipo) {
            "actividad" -> BloquePdf(
                dato("actividad").ifBlank { "Actividad sin nombre" },
                listOf(
                    "Docente" to dato("docente").ifBlank { "Sin docente" },
                    "Inscritos" to "${dato("inscritos")} de ${dato("cupo")}",
                    "Sesiones" to dato("sesiones"),
                    "Asistencia" to asistencia,
                ),
            )
            "alumno" -> BloquePdf(
                dato("alumno").ifBlank { "Alumno sin nombre" },
                listOf(
                    "Número de control" to dato("matricula"),
                    "Actividades" to dato("actividades"),
                    "Asistencia" to asistencia,
                ),
            )
            else -> BloquePdf(
                dato("docente").ifBlank { "Docente sin nombre" },
                listOf(
                    "Actividades" to dato("actividades"),
                    "Sesiones impartidas" to dato("sesiones_impartidas"),
                    "Evidencias" to dato("evidencias"),
                ),
            )
        }
    }

    private fun fechaLegible(iso: String): String = runCatching {
        val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val salida = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        salida.format(entrada.parse(iso)!!)
    }.getOrDefault(iso)
}
