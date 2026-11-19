package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

object Asistencia_Modelo {

    // lista de la fecha elegida
    // sin registro todos salen presentes
    fun lista(idActividad: Int, fecha: String, alTerminar: (Resultado_Modelo<ListaAsistencia>) -> Unit) {
        Red.trabajo({
            val ruta = "/actividades/$idActividad/asistencia?fecha=" + URLEncoder.encode(fecha.trim(), "UTF-8")
            val respuesta = Red.solicitud("GET", ruta).objeto()
            val alumnos = respuesta.getJSONArray("alumnos").lista {
                MarcaAsistencia(it.entero("idAlumno"), it.texto("nombre"), it.texto("matricula"), it.optBoolean("presente", true))
            }
            ListaAsistencia(respuesta.texto("fecha"), alumnos)
        }, alTerminar)
    }

    fun guardar(
        idActividad: Int,
        fecha: String,
        marcas: List<MarcaAsistencia>,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        Red.trabajo({
            val lista = JSONArray()
            for (marca in marcas) {
                val fila = JSONObject()
                fila.put("idAlumno", marca.idAlumno)
                fila.put("presente", marca.presente)
                lista.put(fila)
            }
            val cuerpo = JSONObject()
            cuerpo.put("fecha", fecha)
            cuerpo.put("marcas", lista)
            Red.solicitud("PUT", "/actividades/$idActividad/asistencia", cuerpo)
            Unit
        }, alTerminar)
    }

    fun historial(idActividad: Int, alTerminar: (Resultado_Modelo<List<SesionAsistencia>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/actividades/$idActividad/asistencia/historial").arreglo().lista { sesion ->
                val alumnos = sesion.getJSONArray("alumnos").lista {
                    DetalleAsistencia(it.texto("nombre"), it.texto("matricula"), it.optBoolean("presente", false))
                }
                SesionAsistencia(sesion.entero("id"), sesion.texto("fecha"), sesion.entero("presentes"), sesion.entero("total"), alumnos)
            }
        }, alTerminar)
    }
}
