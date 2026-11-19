package itsm.extra_tec.Modelo

import org.json.JSONObject

object Autenticacion_Modelo {

    fun iniciarSesion(
        correo: String,
        contrasena: String,
        rol: String,
        alTerminar: (Resultado_Modelo<SesionIniciada>) -> Unit,
    ) {
        Red.trabajo({
            val cuerpo = JSONObject()
            cuerpo.put("correo", correo.trim())
            cuerpo.put("contrasena", contrasena)
            cuerpo.put("rol", rol)
            val respuesta = Red.solicitud("POST", "/login", cuerpo).objeto()
            SesionIniciada(usuarioDe(respuesta.getJSONObject("usuario")), respuesta.getString("token"))
        }, alTerminar)
    }

    fun registrar(
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        contrasena: String,
        matricula: String,
        idCarrera: Int,
        semestre: Int,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        Red.trabajo({
            val cuerpo = JSONObject()
            cuerpo.put("nombres", nombres)
            cuerpo.put("apellidoPaterno", apellidoPaterno)
            cuerpo.put("apellidoMaterno", apellidoMaterno)
            cuerpo.put("correo", correo)
            cuerpo.put("contrasena", contrasena)
            cuerpo.put("matricula", NumeroControl.normalizar(matricula))
            cuerpo.put("idCarrera", idCarrera)
            cuerpo.put("semestre", semestre)
            Red.solicitud("POST", "/registro", cuerpo)
            Unit
        }, alTerminar)
    }

    // carreras en orden de la escuela
    fun carreras(alTerminar: (Resultado_Modelo<List<Carrera>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/carreras").arreglo().lista { Carrera(it.entero("id"), it.texto("nombre")) }
        }, alTerminar)
    }
}
