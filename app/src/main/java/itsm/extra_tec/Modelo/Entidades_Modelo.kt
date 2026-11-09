package itsm.extra_tec.Modelo

// Une las partes del nombre
fun nombreCompleto(nombres: String, paterno: String, materno: String): String =
    listOf(nombres, paterno, materno).filter { it.isNotBlank() }.joinToString(" ")

data class Usuario(
    val id: Int,
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val correo: String,
    val rol: String,
) {
    val nombre: String get() = nombreCompleto(nombres, apellidoPaterno, apellidoMaterno)
}

data class SesionIniciada(val usuario: Usuario)

data class Carrera(val id: Int, val nombre: String)

data class DiaSemana(val id: Int, val nombre: String)

data class Lugar(val id: Int, val nombre: String)

data class CatalogosHorario(val dias: List<DiaSemana>, val lugares: List<Lugar>)

data class Actividad(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val cupo: Int,
    val inscritos: Int,
    val horario: String,
    val lugar: String,
    val docente: String,
    val idDocente: Int?,
    val inscrito: Boolean,
) {
    val lleno: Boolean get() = inscritos >= cupo
}

data class HorarioDia(
    val idDia: Int,
    val diaSemana: String,
    val horaInicio: String,
    val horaFin: String,
    val idLugar: Int = 0,
    val lugar: String = "",
)

data class DetalleActividad(val actividad: Actividad, val horarios: List<HorarioDia>)

data class HorarioAlumno(val idActividad: Int, val nombre: String, val dia: HorarioDia)

data class Constancia(
    val idActividad: Int,
    val nombre: String,
    val periodo: String,
    val sesiones: Int,
    val asistencias: Int,
    val porcentaje: Double,
    val disponible: Boolean,
)

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
}

data class AlumnoInscrito(val id: Int, val nombre: String, val matricula: String)

data class MarcaAsistencia(val idAlumno: Int, val nombre: String, val matricula: String, val presente: Boolean)

data class ListaAsistencia(val fecha: String, val alumnos: List<MarcaAsistencia>)

data class DetalleAsistencia(val nombre: String, val matricula: String, val presente: Boolean)

data class SesionAsistencia(
    val id: Int,
    val fecha: String,
    val presentes: Int,
    val total: Int,
    val alumnos: List<DetalleAsistencia>,
)

data class EvidenciaFoto(val id: Int, val ruta: String)

data class Evidencia(
    val id: Int,
    val idActividad: Int,
    val fecha: String,
    val creadaEn: String,
    val actualizadaEn: String?,
    val estado: String,
    val descripcion: String,
    val fotos: List<EvidenciaFoto>,
) {
    val estadoEtiqueta: String get() = etiquetaEstado(estado)
    val marcaTiempo: String get() = creadaEn.take(16)
    val editada: Boolean get() = actualizadaEn != null
}

fun etiquetaEstado(estado: String): String = when (estado) {
    "realizada" -> "Realizada"
    "suspendida" -> "Suspendida"
    "no_realizada" -> "No realizada"
    else -> estado
}

// Foto nueva con los bytes del selector
data class FotoNueva(val nombre: String, val tipoMime: String, val datos: ByteArray)

data class Archivo(val bytes: ByteArray, val nombre: String)
