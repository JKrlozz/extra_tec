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
    val resumen: String,
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
                _reporte.value = Reporte(tipo, etiqueta, rango, filas, resumir(tipo, filas))
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
                actual.rango + "\n" + actual.resumen,
                bloques(actual),
            )
            val nombre = "reporte-" + actual.tipo + "-" + (System.currentTimeMillis() / 1000) + ".pdf"
            val ruta = Archivos_Modelo.guardarEnDescargas(contexto, nombre, "application/pdf", pdf)
            avisar("Reporte guardado en $ruta")
        } catch (e: Exception) {
            avisar("No se pudo exportar: " + e.message)
        }
    }

    private val SALTO = System.lineSeparator()

    // totales para decidir rapido
    private fun resumir(tipo: String, filas: List<Map<String, String>>): String {
        var texto = filas.size.toString() + " registros"
        if (tipo == "actividad") {
            var riesgo = 0
            var sinEvidencia = 0
            var conPendientes = 0
            var sumaOcupacion = 0
            for (fila in filas) {
                riesgo += (fila["en_riesgo"] ?: "0").toInt()
                sinEvidencia += (fila["sin_evidencia"] ?: "0").toInt()
                sumaOcupacion += (fila["ocupacion"] ?: "0").toInt()
                if ((fila["pendientes"] ?: "0").toInt() > 0) conPendientes++
            }
            val ocupacion = if (filas.isEmpty()) 0 else sumaOcupacion / filas.size
            texto = "${filas.size} actividades, $conPendientes con pendientes" + SALTO +
                "Ocupación promedio $ocupacion %" + SALTO +
                "$riesgo alumno(s) en riesgo, $sinEvidencia sesión(es) sin evidencia"
        } else if (tipo == "alumno") {
            var riesgo = 0
            var sinActividad = 0
            var cumplen = 0
            for (fila in filas) {
                val prioridad = fila["prioridad"]
                if (prioridad == "0") riesgo++
                if (prioridad == "1") sinActividad++
                if (prioridad == "3") cumplen++
            }
            texto = "${filas.size} alumnos: $riesgo en riesgo, $sinActividad sin actividad" + SALTO +
                "$cumplen cumplen el 80 % de asistencia"
        } else {
            var pendientes = 0
            var alCorriente = 0
            for (fila in filas) {
                pendientes += (fila["sin_evidencia"] ?: "0").toInt()
                if (fila["prioridad"] == "3") alCorriente++
            }
            texto = "${filas.size} docentes, $alCorriente al corriente" + SALTO +
                "$pendientes sesión(es) sin evidencia en total"
        }
        return texto
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
                val ultima = fila["ultima"] ?: ""
                lista.add(
                    BloquePdf(
                        fila["actividad"] ?: "",
                        listOf(
                            "Docente" to docente.ifBlank { "Sin docente" },
                            "Ocupación" to (fila["inscritos"] ?: "0") + " de " + (fila["cupo"] ?: "0") +
                                " (" + (fila["ocupacion"] ?: "0") + " %)",
                            "Sesiones" to (fila["sesiones"] ?: "0") + ", última: " +
                                (if (ultima.isEmpty()) "ninguna" else Fechas_ModeloVista.legible(ultima)),
                            "Asistencia" to asistencia,
                            "Atención" to (fila["atencion"] ?: ""),
                        ),
                    )
                )
            } else if (actual.tipo == "alumno") {
                val actividad = fila["actividad"] ?: ""
                lista.add(
                    BloquePdf(
                        fila["alumno"] ?: "",
                        listOf(
                            "Número de control" to (fila["matricula"] ?: ""),
                            "Actividad" to actividad.ifBlank { "Ninguna" },
                            "Asistencias" to (fila["presentes"] ?: "0") + " de " + (fila["sesiones"] ?: "0") +
                                ", faltas: " + (fila["faltas"] ?: "0"),
                            "Asistencia" to asistencia,
                            "Estado" to (fila["estado"] ?: ""),
                        ),
                    )
                )
            } else {
                lista.add(
                    BloquePdf(
                        fila["docente"] ?: "",
                        listOf(
                            "Actividades" to (fila["actividades"] ?: "0"),
                            "Sesiones" to (fila["sesiones"] ?: "0"),
                            "Evidencias" to (fila["evidencias"] ?: "0") + ", sin evidencia: " +
                                (fila["sin_evidencia"] ?: "0"),
                            "Asistencia" to asistencia,
                            "Estado" to (fila["estado"] ?: ""),
                        ),
                    )
                )
            }
        }
        return lista
    }
}
