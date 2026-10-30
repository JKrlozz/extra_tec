package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject

object Actividades_Modelo {

    fun listar(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        Api_Modelo.consultar("GET", "/actividades",
            convertir = { r -> (r as JSONArray).objetos().map(Actividad::desdeJson) },
            alTerminar = alTerminar)
    }

    fun detalle(id: Int, alTerminar: (Resultado_Modelo<DetalleActividad>) -> Unit) {
        Api_Modelo.consultar("GET", "/actividades/$id",
            convertir = { r ->
                val j = r as JSONObject
                DetalleActividad(
                    Actividad.desdeJson(j),
                    j.getJSONArray("horarios").objetos().map(HorarioDia::desdeJson),
                )
            },
            alTerminar = alTerminar)
    }

    // días y lugares para el editor de horarios
    fun catalogosHorario(alTerminar: (Resultado_Modelo<CatalogosHorario>) -> Unit) {
        Api_Modelo.consultar("GET", "/catalogos-horario",
            convertir = { r ->
                val j = r as JSONObject
                CatalogosHorario(
                    j.getJSONArray("dias").objetos().map(DiaSemana::desdeJson),
                    j.getJSONArray("lugares").objetos().map(Lugar::desdeJson),
                )
            },
            alTerminar = alTerminar)
    }

    fun guardar(
        id: Int?,
        nombre: String,
        descripcion: String,
        cupo: Int,
        idDocente: Int,
        horarios: List<HorarioDia>,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        val lista = JSONArray().also { arreglo -> horarios.forEach { arreglo.put(it.aJson()) } }
        val cuerpo = JSONObject()
            .put("nombre", nombre).put("descripcion", descripcion).put("cupo", cupo)
            .put("id_docente", idDocente).put("horarios", lista)
        val ruta = if (id == null) "/actividades" else "/actividades/$id"
        Api_Modelo.consultar(if (id == null) "POST" else "PUT", ruta, cuerpo, convertir = { }, alTerminar = alTerminar)
    }

    fun eliminar(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Api_Modelo.consultar("DELETE", "/actividades/$id", convertir = { }, alTerminar = alTerminar)
    }

    fun inscribir(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Api_Modelo.consultar("POST", "/inscripciones", JSONObject().put("id_actividad", idActividad),
            convertir = { }, alTerminar = alTerminar)
    }

    fun darDeBaja(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Api_Modelo.consultar("DELETE", "/inscripciones/$idActividad", convertir = { }, alTerminar = alTerminar)
    }

    fun horarioAlumno(alTerminar: (Resultado_Modelo<List<HorarioAlumno>>) -> Unit) {
        Api_Modelo.consultar("GET", "/mi-horario",
            convertir = { r -> (r as JSONArray).objetos().map(HorarioAlumno::desdeJson) },
            alTerminar = alTerminar)
    }

    fun constancias(alTerminar: (Resultado_Modelo<List<Constancia>>) -> Unit) {
        Api_Modelo.consultar("GET", "/constancias",
            convertir = { r -> (r as JSONArray).objetos().map(Constancia::desdeJson) },
            alTerminar = alTerminar)
    }

    fun descargarConstancia(idActividad: Int, alTerminar: (Resultado_Modelo<Archivo>) -> Unit) {
        Api_Modelo.descargar("/constancias/$idActividad/pdf",
            alExito = { bytes, nombre -> alTerminar(Resultado_Modelo.Exito(Archivo(bytes, nombre))) },
            alError = { alTerminar(Resultado_Modelo.Fallo(it)) })
    }

    fun misClases(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        Api_Modelo.consultar("GET", "/mis-actividades",
            convertir = { r -> (r as JSONArray).objetos().map(Actividad::desdeJson) },
            alTerminar = alTerminar)
    }

    fun alumnosDeActividad(idActividad: Int, alTerminar: (Resultado_Modelo<List<AlumnoInscrito>>) -> Unit) {
        Api_Modelo.consultar("GET", "/actividades/$idActividad/alumnos",
            convertir = { r -> (r as JSONArray).objetos().map(AlumnoInscrito::desdeJson) },
            alTerminar = alTerminar)
    }
}
