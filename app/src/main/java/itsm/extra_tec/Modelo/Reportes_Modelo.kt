package itsm.extra_tec.Modelo

import org.json.JSONObject

object Reportes_Modelo {

    // Cada fila es un mapa de columna a valor en texto
    fun generar(
        tipo: String,
        desde: String,
        hasta: String,
        alTerminar: (Resultado_Modelo<List<Map<String, String>>>) -> Unit,
    ) {
        var ruta = "/reportes?tipo=$tipo"
        if (desde.isNotEmpty()) ruta += "&desde=$desde"
        if (hasta.isNotEmpty()) ruta += "&hasta=$hasta"
        Api_Modelo.consultar("GET", ruta,
            convertir = { r ->
                (r as JSONObject).getJSONArray("filas").objetos().map { fila ->
                    fila.keys().asSequence().associateWith { fila.texto(it) }
                }
            },
            alTerminar = alTerminar)
    }
}
