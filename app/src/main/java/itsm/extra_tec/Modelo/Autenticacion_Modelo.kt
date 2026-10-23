package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject

object Autenticacion_Modelo {

    fun iniciarSesion(
        correo: String,
        contrasena: String,
        rol: String,
        alTerminar: (Resultado_Modelo<SesionIniciada>) -> Unit,
    ) {
        val cuerpo = JSONObject().put("correo", correo).put("contrasena", contrasena).put("rol", rol)
        Api_Modelo.consultar("POST", "/login", cuerpo,
            convertir = { r ->
                val j = r as JSONObject
                SesionIniciada(j.getString("token"), Usuario.desdeJson(j.getJSONObject("usuario")))
            },
            alTerminar = alTerminar)
    }

    fun registrar(
        nombre: String,
        correo: String,
        contrasena: String,
        matricula: String,
        idCarrera: Int,
        semestre: Int,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        val cuerpo = JSONObject()
            .put("nombre", nombre).put("correo", correo).put("contrasena", contrasena)
            .put("matricula", matricula).put("id_carrera", idCarrera).put("semestre", semestre)
        Api_Modelo.consultar("POST", "/registro", cuerpo, convertir = { }, alTerminar = alTerminar)
    }

    fun carreras(alTerminar: (Resultado_Modelo<List<Carrera>>) -> Unit) {
        Api_Modelo.consultar("GET", "/carreras",
            convertir = { r -> (r as JSONArray).objetos().map(Carrera::desdeJson) },
            alTerminar = alTerminar)
    }

    fun probarConexion(alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Api_Modelo.consultar("GET", "/salud", convertir = { }, alTerminar = alTerminar)
    }
}
