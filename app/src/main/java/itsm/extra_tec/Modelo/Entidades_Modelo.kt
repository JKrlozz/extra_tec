package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject

// org.json devuelve "null" en optString si el valor es null; aquí devuelve vacío
fun JSONObject.texto(clave: String): String = if (isNull(clave)) "" else get(clave).toString()

fun JSONObject.enteroONulo(clave: String): Int? = if (isNull(clave)) null else optInt(clave)

fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

// Une las partes del nombre sin duplicar espacios
fun nombreCompleto(nombres: String, paterno: String, materno: String): String =
    listOf(nombres, paterno, materno).filter { it.isNotBlank() }.joinToString(" ")

// Reparte un nombre completo guardado por una versión anterior
private fun repartirNombre(completo: String): Triple<String, String, String> {
    val partes = completo.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return Triple(
        partes.getOrElse(0) { "" },
        partes.getOrElse(1) { "" },
        partes.drop(2).joinToString(" "),
    )
}

data class Usuario(
    val id: Int,
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val correo: String,
    val rol: String,
) {
    val nombre: String get() = nombreCompleto(nombres, apellidoPaterno, apellidoMaterno)

    fun aJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("nombres", nombres)
        .put("apellido_paterno", apellidoPaterno)
        .put("apellido_materno", apellidoMaterno)
        .put("correo", correo)
        .put("rol", rol)

    companion object {
        fun desdeJson(j: JSONObject): Usuario {
            if (!j.isNull("nombres")) {
                return Usuario(
                    j.getInt("id"), j.texto("nombres"), j.texto("apellido_paterno"),
                    j.texto("apellido_materno"), j.texto("correo"), j.getString("rol"),
                )
            }
            val (nombres, paterno, materno) = repartirNombre(j.texto("nombre"))
            return Usuario(j.getInt("id"), nombres, paterno, materno, j.texto("correo"), j.getString("rol"))
        }
    }
}

data class SesionIniciada(val token: String, val usuario: Usuario)

data class Carrera(val id: Int, val nombre: String) {
    companion object {
        fun desdeJson(j: JSONObject) = Carrera(j.getInt("id"), j.getString("nombre"))
    }
}

data class DiaSemana(val id: Int, val nombre: String) {
    companion object {
        fun desdeJson(j: JSONObject) = DiaSemana(j.getInt("id"), j.getString("nombre"))
    }
}

data class Lugar(val id: Int, val nombre: String) {
    companion object {
        fun desdeJson(j: JSONObject) = Lugar(j.getInt("id"), j.getString("nombre"))
    }
}

data class CatalogosHorario(val dias: List<DiaSemana>, val lugares: List<Lugar>)

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

data class HorarioDia(
    val idDia: Int,
    val diaSemana: String,
    val horaInicio: String,
    val horaFin: String,
    val idLugar: Int = 0,
    val lugar: String = "",
) {
    fun aJson(): JSONObject = JSONObject()
        .put("id_dia", idDia)
        .put("hora_inicio", horaInicio)
        .put("hora_fin", horaFin)
        .put("id_lugar", idLugar)
        .put("lugar", lugar)

    companion object {
        fun desdeJson(j: JSONObject) = HorarioDia(
            idDia = j.optInt("id_dia"),
            diaSemana = j.getString("dia_semana"),
            horaInicio = j.getString("hora_inicio").take(5),
            horaFin = j.getString("hora_fin").take(5),
            idLugar = j.optInt("id_lugar"),
            lugar = j.texto("lugar"),
        )
    }
}

data class DetalleActividad(val actividad: Actividad, val horarios: List<HorarioDia>)

data class HorarioAlumno(val idActividad: Int, val nombre: String, val dia: HorarioDia) {
    companion object {
        fun desdeJson(j: JSONObject) = HorarioAlumno(
            j.getInt("id_actividad"), j.getString("nombre"),
            HorarioDia(
                j.optInt("id_dia"), j.getString("dia_semana"),
                j.getString("hora_inicio").take(5), j.getString("hora_fin").take(5),
                j.optInt("id_lugar"), j.texto("lugar"),
            ),
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
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val correo: String,
    val matricula: String,
    val semestre: Int,
    val idCarrera: Int,
    val carrera: String,
) {
    val nombre: String get() = nombreCompleto(nombres, apellidoPaterno, apellidoMaterno)

    companion object {
        fun desdeJson(j: JSONObject): Alumno {
            val (nombres, paterno, materno) =
                if (!j.isNull("nombres")) Triple(j.texto("nombres"), j.texto("apellido_paterno"), j.texto("apellido_materno"))
                else repartirNombre(j.texto("nombre"))
            return Alumno(
                j.getInt("id"), nombres, paterno, materno, j.texto("correo"), j.getString("matricula"),
                j.getInt("semestre"), j.getInt("id_carrera"), j.texto("carrera"),
            )
        }
    }
}

data class Docente(
    val id: Int,
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val correo: String,
    val especialidad: String,
    val telefono: String,
) {
    val nombre: String get() = nombreCompleto(nombres, apellidoPaterno, apellidoMaterno)

    companion object {
        fun desdeJson(j: JSONObject): Docente {
            val (nombres, paterno, materno) =
                if (!j.isNull("nombres")) Triple(j.texto("nombres"), j.texto("apellido_paterno"), j.texto("apellido_materno"))
                else repartirNombre(j.texto("nombre"))
            return Docente(
                j.getInt("id"), nombres, paterno, materno, j.texto("correo"),
                j.texto("especialidad"), j.texto("telefono"),
            )
        }
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
