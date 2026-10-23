package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject

// org.json devuelve "null" con optString si el valor es null; estas funciones devuelven vacío
fun JSONObject.texto(clave: String): String = if (isNull(clave)) "" else get(clave).toString()

fun JSONObject.enteroONulo(clave: String): Int? = if (isNull(clave)) null else optInt(clave)

fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

data class Usuario(val id: Int, val nombre: String, val correo: String, val rol: String) {
    fun aJson(): JSONObject = JSONObject().put("id", id).put("nombre", nombre).put("correo", correo).put("rol", rol)

    companion object {
        fun desdeJson(j: JSONObject) = Usuario(j.getInt("id"), j.getString("nombre"), j.texto("correo"), j.getString("rol"))
    }
}

data class SesionIniciada(val token: String, val usuario: Usuario)

data class Carrera(val id: Int, val nombre: String) {
    companion object {
        fun desdeJson(j: JSONObject) = Carrera(j.getInt("id"), j.getString("nombre"))
    }
}

data class Actividad(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val cupo: Int,
    val inscritos: Int,
    val horario: String,
    val docente: String,
    val idDocente: Int?,
    val inscrito: Boolean,
) {
    val lleno: Boolean get() = inscritos >= cupo

    companion object {
        fun desdeJson(j: JSONObject) = Actividad(
            id = j.getInt("id"),
            nombre = j.texto("nombre"),
            descripcion = j.texto("descripcion"),
            cupo = j.getInt("cupo"),
            inscritos = j.optInt("inscritos", 0),
            horario = j.texto("horario"),
            docente = j.texto("docente"),
            idDocente = j.enteroONulo("id_docente"),
            inscrito = j.optBoolean("inscrito", false),
        )
    }
}

data class HorarioDia(val diaSemana: String, val horaInicio: String, val horaFin: String, val lugar: String = "") {
    fun aJson(): JSONObject = JSONObject()
        .put("dia_semana", diaSemana).put("hora_inicio", horaInicio).put("hora_fin", horaFin)

    companion object {
        fun desdeJson(j: JSONObject) = HorarioDia(
            j.getString("dia_semana"), j.getString("hora_inicio").take(5), j.getString("hora_fin").take(5), j.texto("lugar"),
        )
    }
}

data class DetalleActividad(val actividad: Actividad, val horarios: List<HorarioDia>)

data class HorarioAlumno(val idActividad: Int, val nombre: String, val dia: HorarioDia) {
    companion object {
        fun desdeJson(j: JSONObject) = HorarioAlumno(
            j.getInt("id_actividad"), j.getString("nombre"),
            HorarioDia(j.getString("dia_semana"), j.getString("hora_inicio").take(5), j.getString("hora_fin").take(5), j.texto("lugar")),
        )
    }
}

data class Constancia(
    val idActividad: Int,
    val nombre: String,
    val periodo: String,
    val sesiones: Int,
    val asistencias: Int,
    val porcentaje: Double,
    val disponible: Boolean,
) {
    companion object {
        fun desdeJson(j: JSONObject) = Constancia(
            j.getInt("id_actividad"), j.getString("nombre"), j.texto("periodo"),
            j.getInt("sesiones"), j.getInt("asistencias"), j.getDouble("porcentaje"), j.getBoolean("disponible"),
        )
    }
}

data class Alumno(
    val id: Int,
    val nombre: String,
    val correo: String,
    val matricula: String,
    val semestre: Int,
    val idCarrera: Int,
    val carrera: String,
) {
    companion object {
        fun desdeJson(j: JSONObject) = Alumno(
            j.getInt("id"), j.getString("nombre"), j.texto("correo"), j.getString("matricula"),
            j.getInt("semestre"), j.getInt("id_carrera"), j.texto("carrera"),
        )
    }
}

data class Docente(val id: Int, val nombre: String, val correo: String, val especialidad: String, val telefono: String) {
    companion object {
        fun desdeJson(j: JSONObject) = Docente(
            j.getInt("id"), j.getString("nombre"), j.texto("correo"), j.texto("especialidad"), j.texto("telefono"),
        )
    }
}

data class AlumnoInscrito(val id: Int, val nombre: String, val matricula: String) {
    companion object {
        fun desdeJson(j: JSONObject) = AlumnoInscrito(j.getInt("id"), j.getString("nombre"), j.getString("matricula"))
    }
}

data class MarcaAsistencia(val idAlumno: Int, val nombre: String, val matricula: String, val presente: Boolean)

data class ListaAsistencia(val fecha: String, val alumnos: List<MarcaAsistencia>)

data class Archivo(val bytes: ByteArray, val nombre: String)
