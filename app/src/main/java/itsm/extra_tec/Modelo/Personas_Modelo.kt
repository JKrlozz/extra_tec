package itsm.extra_tec.Modelo

import org.json.JSONObject
import java.net.URLEncoder

object Personas_Modelo {

    fun alumnos(filtro: String, alTerminar: (Resultado_Modelo<List<Alumno>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/alumnos?filtro=" + URLEncoder.encode(filtro.trim(), "UTF-8")).arreglo().lista {
                Alumno(
                    it.entero("id"), it.texto("nombres"), it.texto("apellidoPaterno"),
                    it.texto("apellidoMaterno"), it.texto("correo"), it.texto("matricula"),
                    it.entero("semestre"), it.entero("idCarrera"), it.texto("carrera"),
                )
            }
        }, alTerminar)
    }

    fun docentes(filtro: String, alTerminar: (Resultado_Modelo<List<Docente>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/docentes?filtro=" + URLEncoder.encode(filtro.trim(), "UTF-8")).arreglo().lista {
                Docente(
                    it.entero("id"), it.texto("nombres"), it.texto("apellidoPaterno"),
                    it.texto("apellidoMaterno"), it.texto("correo"),
                    it.texto("especialidad"), it.texto("telefono"),
                )
            }
        }, alTerminar)
    }

    // catalogo para sugerir especialidades
    fun especialidades(alTerminar: (Resultado_Modelo<List<String>>) -> Unit) {
        Red.trabajo({
            val arreglo = Red.solicitud("GET", "/especialidades").arreglo()
            val lista = mutableListOf<String>()
            for (i in 0 until arreglo.length()) {
                lista.add(arreglo.getString(i))
            }
            lista
        }, alTerminar)
    }

    // regresa la clave si se genero
    fun guardarAlumno(
        id: Int?,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        matricula: String,
        idCarrera: Int,
        semestre: Int,
        contrasena: String,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        Red.trabajo({
            val cuerpo = persona(nombres, apellidoPaterno, apellidoMaterno, correo, contrasena)
            cuerpo.put("matricula", NumeroControl.normalizar(matricula))
            cuerpo.put("idCarrera", idCarrera)
            cuerpo.put("semestre", semestre)
            claveGenerada(enviar(id, "/alumnos", cuerpo))
        }, alTerminar)
    }

    fun guardarDocente(
        id: Int?,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        especialidad: String,
        telefono: String,
        contrasena: String,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        Red.trabajo({
            val cuerpo = persona(nombres, apellidoPaterno, apellidoMaterno, correo, contrasena)
            cuerpo.put("especialidad", especialidad)
            cuerpo.put("telefono", telefono)
            claveGenerada(enviar(id, "/docentes", cuerpo))
        }, alTerminar)
    }

    fun eliminarAlumno(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        darDeBaja("/alumnos/$id", alTerminar)
    }

    fun eliminarDocente(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        darDeBaja("/docentes/$id", alTerminar)
    }

    // baja logica para conservar historial
    private fun darDeBaja(ruta: String, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Red.trabajo({
            Red.solicitud("DELETE", ruta)
            Unit
        }, alTerminar)
    }

    private fun persona(nombres: String, paterno: String, materno: String, correo: String, contrasena: String): JSONObject {
        val cuerpo = JSONObject()
        cuerpo.put("nombres", nombres)
        cuerpo.put("apellidoPaterno", paterno)
        cuerpo.put("apellidoMaterno", materno)
        cuerpo.put("correo", correo)
        cuerpo.put("contrasena", contrasena)
        return cuerpo
    }

    private fun enviar(id: Int?, base: String, cuerpo: JSONObject): JSONObject {
        if (id == null) {
            return Red.solicitud("POST", base, cuerpo).objeto()
        }
        return Red.solicitud("PUT", "$base/$id", cuerpo).objeto()
    }

    private fun claveGenerada(respuesta: JSONObject): String? {
        if (respuesta.isNull("clave")) {
            return null
        }
        return respuesta.getString("clave")
    }
}
