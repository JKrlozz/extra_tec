package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject

object Asistencia_Modelo {

    fun lista(idActividad: Int, alTerminar: (Resultado_Modelo<ListaAsistencia>) -> Unit) {
        Api_Modelo.consultar("GET", "/asistencia/$idActividad",
            convertir = { r ->
                val j = r as JSONObject
                ListaAsistencia(
                    j.getString("fecha"),
                    j.getJSONArray("alumnos").objetos().map {
                        MarcaAsistencia(it.getInt("id"), it.getString("nombre"), it.getString("matricula"), it.getBoolean("presente"))
                    },
                )
            },
            alTerminar = alTerminar)
    }

    fun guardar(
        idActividad: Int,
        fecha: String,
        marcas: List<MarcaAsistencia>,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        val lista = JSONArray().also { arreglo ->
            marcas.forEach { arreglo.put(JSONObject().put("id_alumno", it.idAlumno).put("presente", it.presente)) }
        }
        val cuerpo = JSONObject().put("id_actividad", idActividad).put("fecha", fecha).put("asistencias", lista)
        Api_Modelo.consultar("POST", "/asistencia", cuerpo, convertir = { }, alTerminar = alTerminar)
    }

    fun subirEvidencia(
        idActividad: Int,
        descripcion: String,
        nombreArchivo: String,
        tipoMime: String,
        datos: ByteArray,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        Api_Modelo.subirArchivo("/evidencias",
            mapOf("id_actividad" to idActividad.toString(), "descripcion" to descripcion),
            "foto", nombreArchivo, tipoMime, datos,
            alExito = { alTerminar(Resultado_Modelo.Exito(Unit)) },
            alError = { alTerminar(Resultado_Modelo.Fallo(it)) })
    }
}
