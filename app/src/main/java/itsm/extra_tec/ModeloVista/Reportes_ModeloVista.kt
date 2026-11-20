package itsm.extra_tec.ModeloVista

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.Archivos_Modelo
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.BloquePdf
import itsm.extra_tec.Modelo.Carrera
import itsm.extra_tec.Modelo.Reportes_Modelo

// filtros opcionales del reporte, vacio o 0 es sin filtro
data class FiltrosReporte(
    val matricula: String = "",
    val nombre: String = "",
    val idCarrera: Int = 0,
    val semestre: Int = 0,
    val idActividad: Int = 0,
    val docente: String = "",
)

data class Reporte(
    val tipo: String,
    val etiqueta: String,
    val rango: String,
    val filtros: String,
    val filas: List<Map<String, String>>,
    val resumen: String,
)

class Reportes_ModeloVista : Base_ModeloVista() {
    // valor del reporte y su texto
    val tipos = listOf("actividad", "alumno", "docente")
    val etiquetas = listOf("Por actividad", "Por alumno", "Por docente")

    private val _reporte = MutableLiveData<Reporte?>(null)
    val reporte: LiveData<Reporte?> = _reporte

    // catalogos para los filtros
    private val _carreras = MutableLiveData<List<Carrera>>(emptyList())
    val carreras: LiveData<List<Carrera>> = _carreras
    private val _actividades = MutableLiveData<List<Actividad>>(emptyList())
    val actividades: LiveData<List<Actividad>> = _actividades

    fun cargarCatalogos() {
        Autenticacion_Modelo.carreras { resultado -> atender(resultado) { lista -> _carreras.value = lista } }
        Actividades_Modelo.todas { resultado -> atender(resultado) { lista -> _actividades.value = lista } }
    }

    fun generar(posicionTipo: Int, desde: String, hasta: String, filtros: FiltrosReporte = FiltrosReporte()) {
        if (posicionTipo < 0 || posicionTipo >= tipos.size) {
            return
        }
        val tipo = tipos[posicionTipo]
        val etiqueta = etiquetas[posicionTipo]
        // el de actividades se queda general
        val usados = if (tipo == "actividad") FiltrosReporte() else filtros
        val parametros = linkedMapOf<String, String>()
        val textos = mutableListOf<String>()
        if (tipo == "alumno") {
            parametros["matricula"] = usados.matricula
            parametros["nombre"] = usados.nombre
            if (usados.matricula.isNotBlank()) textos.add("Número de control: " + usados.matricula.trim().uppercase())
            if (usados.nombre.isNotBlank()) textos.add("Alumno: " + usados.nombre.trim())
            if (usados.idCarrera > 0) {
                parametros["carrera"] = usados.idCarrera.toString()
                val carrera = _carreras.value.orEmpty().firstOrNull { it.id == usados.idCarrera }
                textos.add("Carrera: " + (carrera?.nombre ?: usados.idCarrera.toString()))
            }
            if (usados.semestre > 0) {
                parametros["semestre"] = usados.semestre.toString()
                textos.add("Semestre: " + usados.semestre)
            }
        } else if (tipo == "docente") {
            parametros["docente"] = usados.docente
            if (usados.docente.isNotBlank()) textos.add("Docente: " + usados.docente.trim())
        }
        if (tipo != "actividad" && usados.idActividad > 0) {
            parametros["actividad"] = usados.idActividad.toString()
            val actividad = _actividades.value.orEmpty().firstOrNull { it.id == usados.idActividad }
            textos.add("Actividad: " + (actividad?.nombre ?: usados.idActividad.toString()))
        }
        val textoFiltros = if (textos.isEmpty()) "" else "Filtros: " + textos.joinToString(", ")
        Reportes_Modelo.generar(tipo, desde, hasta, parametros) { resultado ->
            atender(resultado) { filas ->
                var rango = "Del " + Fechas_ModeloVista.legible(desde) + " al " + Fechas_ModeloVista.legible(hasta)
                if (desde.isEmpty() && hasta.isEmpty()) {
                    rango = "Todo el historial"
                } else if (desde.isEmpty()) {
                    rango = "Hasta el " + Fechas_ModeloVista.legible(hasta)
                } else if (hasta.isEmpty()) {
                    rango = "Desde el " + Fechas_ModeloVista.legible(desde)
                } else if (desde == hasta) {
                    rango = "El " + Fechas_ModeloVista.legible(desde)
                }
                _reporte.value = Reporte(tipo, etiqueta, rango, textoFiltros, filas, resumir(tipo, filas))
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
                encabezado(actual),
                bloques(actual),
            )
            val nombre = "reporte-" + actual.tipo + "-" + (System.currentTimeMillis() / 1000) + ".pdf"
            val ruta = Archivos_Modelo.guardarEnDescargas(contexto, nombre, "application/pdf", pdf)
            avisar("Reporte guardado en $ruta")
        } catch (e: Exception) {
            avisar("No se pudo exportar: " + e.message)
        }
    }

    // periodo, filtros y totales para dar contexto
    fun encabezado(actual: Reporte): String {
        var texto = actual.rango
        if (actual.filtros.isNotEmpty()) {
            texto += "\n" + actual.filtros
        }
        return texto + "\n" + actual.resumen
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
                val carrera = (fila["carrera"] ?: "").ifBlank { "Sin carrera" }
                val semestre = (fila["semestre"] ?: "").ifBlank { "-" }
                lista.add(
                    BloquePdf(
                        fila["alumno"] ?: "",
                        listOf(
                            "Número de control" to (fila["matricula"] ?: ""),
                            "Carrera" to "$carrera, semestre $semestre",
                            "Actividad" to actividad.ifBlank { "Ninguna" },
                            "Asistencias" to (fila["presentes"] ?: "0") + " de " + (fila["sesiones"] ?: "0") +
                                ", faltas: " + (fila["faltas"] ?: "0"),
                            "Asistencia" to asistencia,
                            "Estado" to (fila["estado"] ?: ""),
                        ) + detalle("Sesiones del periodo", detalleSesiones(fila["detalle"] ?: "")),
                    )
                )
            } else {
                lista.add(
                    BloquePdf(
                        fila["docente"] ?: "",
                        listOf(
                            "Correo" to (fila["correo"] ?: ""),
                            "Actividades" to (fila["actividades"] ?: "0"),
                            "Sesiones" to (fila["sesiones"] ?: "0"),
                            "Evidencias" to (fila["evidencias"] ?: "0") + ", sin evidencia: " +
                                (fila["sin_evidencia"] ?: "0"),
                            "Asistencia" to asistencia,
                            "Estado" to (fila["estado"] ?: ""),
                        ) + detalle("Por actividad", fila["detalle"] ?: ""),
                    )
                )
            }
        }
        return lista
    }

    // la api manda fecha · estado, aqui la fecha queda legible
    private fun detalleSesiones(texto: String): String {
        return texto.lines().filter { it.isNotBlank() }.joinToString("\n") { linea ->
            Fechas_ModeloVista.legible(linea.substringBefore(" · ")) + " · " + linea.substringAfter(" · ")
        }
    }

    // fila extra solo cuando hay detalle
    private fun detalle(nombre: String, texto: String): List<Pair<String, String>> {
        if (texto.isBlank()) {
            return emptyList()
        }
        return listOf(nombre to texto)
    }
}
