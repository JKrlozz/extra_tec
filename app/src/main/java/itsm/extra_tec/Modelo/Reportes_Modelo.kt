package itsm.extra_tec.Modelo

import java.net.URLEncoder

// reportes para detectar problemas y tomar decisiones
// la api hace las consultas y ordena
object Reportes_Modelo {
    const val MINIMO_ASISTENCIA = 80.0

    // cada fila es columna y valor
    fun generar(
        tipo: String,
        desde: String,
        hasta: String,
        alTerminar: (Resultado_Modelo<List<Map<String, String>>>) -> Unit,
    ) {
        Red.trabajo({
            if (desde.isNotEmpty() && hasta.isNotEmpty() && desde > hasta) {
                throw ErrorDatos("La fecha inicial es posterior a la final.")
            }
            val ruta = "/reportes?tipo=" + URLEncoder.encode(tipo, "UTF-8") +
                "&desde=" + URLEncoder.encode(desde, "UTF-8") +
                "&hasta=" + URLEncoder.encode(hasta, "UTF-8")
            val filas = mutableListOf<Map<String, String>>()
            val arreglo = Red.solicitud("GET", ruta).arreglo()
            for (i in 0 until arreglo.length()) {
                val objeto = arreglo.getJSONObject(i)
                val fila = linkedMapOf<String, String>()
                for (clave in objeto.keys()) {
                    fila[clave] = objeto.texto(clave)
                }
                filas.add(fila)
            }
            filas
        }, alTerminar)
    }
}
