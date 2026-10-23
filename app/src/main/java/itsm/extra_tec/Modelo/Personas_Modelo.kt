package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object Personas_Modelo {

    fun alumnos(filtro: String, alTerminar: (Resultado_Modelo<List<Alumno>>) -> Unit) {
        Api_Modelo.consultar("GET", "/alumnos?q=" + URLEncoder.encode(filtro.trim(), "UTF-8"),
            convertir = { r -> (r as JSONArray).objetos().map(Alumno::desdeJson) },
            alTerminar = alTerminar)
    }

    fun docentes(alTerminar: (Resultado_Modelo<List<Docente>>) -> Unit) {
        Api_Modelo.consultar("GET", "/docentes",
            convertir = { r -> (r as JSONArray).objetos().map(Docente::desdeJson) },
            alTerminar = alTerminar)
    }

    // Devuelve la contraseña inicial si el servidor la generó
    fun guardarAlumno(
        id: Int?,
        nombre: String,
        correo: String,
        matricula: String,
        idCarrera: Int,
        semestre: Int,
        contrasena: String?,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        val cuerpo = JSONObject()
            .put("nombre", nombre).put("correo", correo).put("matricula", matricula)
            .put("id_carrera", idCarrera).put("semestre", semestre)
        if (id == null && !contrasena.isNullOrEmpty()) cuerpo.put("contrasena", contrasena)
        Api_Modelo.consultar(if (id == null) "POST" else "PUT", if (id == null) "/alumnos" else "/alumnos/$id", cuerpo,
            convertir = ::claveInicial, alTerminar = alTerminar)
    }

    fun guardarDocente(
        id: Int?,
        nombre: String,
        correo: String,
        especialidad: String,
        telefono: String,
        contrasena: String?,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        val cuerpo = JSONObject()
            .put("nombre", nombre).put("correo", correo)
            .put("especialidad", especialidad).put("telefono", telefono)
        if (id == null && !contrasena.isNullOrEmpty()) cuerpo.put("contrasena", contrasena)
        Api_Modelo.consultar(if (id == null) "POST" else "PUT", if (id == null) "/docentes" else "/docentes/$id", cuerpo,
            convertir = ::claveInicial, alTerminar = alTerminar)
    }

    fun eliminarAlumno(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Api_Modelo.consultar("DELETE", "/alumnos/$id", convertir = { }, alTerminar = alTerminar)
    }

    fun eliminarDocente(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Api_Modelo.consultar("DELETE", "/docentes/$id", convertir = { }, alTerminar = alTerminar)
    }

    private fun claveInicial(respuesta: Any?): String? =
        (respuesta as? JSONObject)?.takeIf { !it.isNull("contrasena_inicial") }?.getString("contrasena_inicial")
}
