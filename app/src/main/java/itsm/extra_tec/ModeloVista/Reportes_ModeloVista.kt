package itsm.extra_tec.ModeloVista

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Archivos_Modelo
import itsm.extra_tec.Modelo.BloquePdf
import itsm.extra_tec.Modelo.Reportes_Modelo

data class Reporte(
    val tipo: String,
    val etiqueta: String,
    val rango: String,
    val filas: List<Map<String, String>>,
)

class Reportes_ModeloVista : Base_ModeloVista() {
    // valor del reporte y su texto
    val tipos = listOf("actividad", "alumno", "docente")
    val etiquetas = listOf("Por actividad", "Por alumno", "Por docente")

    private val _reporte = MutableLiveData<Reporte?>(null)
    val reporte: LiveData<Reporte?> = _reporte

    fun generar(posicionTipo: Int, desde: String, hasta: String) {
        if (posicionTipo < 0 || posicionTipo >= tipos.size) {
            return
        }
        val tipo = tipos[posicionTipo]
        val etiqueta = etiquetas[posicionTipo]
        Reportes_Modelo.generar(tipo, desde, hasta) { resultado ->
            atender(resultado) { filas ->
                var rango = "Del " + Fechas_ModeloVista.legible(desde) + " al " + Fechas_ModeloVista.legible(hasta)
                if (desde.isEmpty() && hasta.isEmpty()) {
                    rango = "Todo el historial"
                } else if (desde.isEmpty()) {
                    rango = "Hasta el " + Fechas_ModeloVista.legible(hasta)
                } else if (hasta.isEmpty()) {
                    rango = "Desde el " + Fechas_ModeloVista.legible(desde)
                }
                _reporte.value = Reporte(tipo, etiqueta, rango, filas)
            }
        }
    }

    fun exportar(contexto: Context) {
        val actual = _reporte.value
        if (actual == null) {
            avisar("Primero genera un reporte.")
            return
        }
        if (actual.filas.isEmpty()) {
            avisar("El reporte no tiene registros para exportar.")
            return
        }
        try {
            val pdf = Archivos_Modelo.reportePdf(
                "Reporte " + actual.etiqueta.lowercase(),
                actual.rango + "\n" + actual.filas.size + " registros",
                bloques(actual),
            )
            val nombre = "reporte-" + actual.tipo + "-" + (System.currentTimeMillis() / 1000) + ".pdf"
            val ruta = Archivos_Modelo.guardarEnDescargas(contexto, nombre, "application/pdf", pdf)
            avisar("Reporte guardado en $ruta")
        } catch (e: Exception) {
            avisar("No se pudo exportar: " + e.message)
        }
    }

    // datos para tarjetas y pdf
    fun bloques(actual: Reporte): List<BloquePdf> {
        val lista = mutableListOf<BloquePdf>()
        for (fila in actual.filas) {
            var asistencia = fila["asistencia_pct"] ?: ""
            if (asistencia.isEmpty()) {
                asistencia = "Sin datos"
            } else {
                asistencia = "$asistencia %"
            }
            if (actual.tipo == "actividad") {
                val docente = fila["docente"] ?: ""
                lista.add(
                    BloquePdf(
                        fila["actividad"] ?: "",
                        listOf(
                            "Docente" to docente.ifBlank { "Sin docente" },
                            "Inscritos" to (fila["inscritos"] ?: "0") + " de " + (fila["cupo"] ?: "0"),
                            "Sesiones" to (fila["sesiones"] ?: "0"),
                            "Asistencia" to asistencia,
                        ),
                    )
                )
            } else if (actual.tipo == "alumno") {
                lista.add(
                    BloquePdf(
                        fila["alumno"] ?: "",
                        listOf(
                            "Número de control" to (fila["matricula"] ?: ""),
                            "Actividades" to (fila["actividades"] ?: "0"),
                            "Asistencia" to asistencia,
                        ),
                    )
                )
            } else {
                lista.add(
                    BloquePdf(
                        fila["docente"] ?: "",
                        listOf(
                            "Actividades" to (fila["actividades"] ?: "0"),
                            "Sesiones impartidas" to (fila["sesiones_impartidas"] ?: "0"),
                            "Evidencias" to (fila["evidencias"] ?: "0"),
                        ),
                    )
                )
            }
        }
        return lista
    }
}
