package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject

object Actividades_Modelo {

    // actividades que ve el alumno
    fun listar(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        Red.trabajo({ pedirActividades("/actividades") }, alTerminar)
    }

    // actividades que ve el administrador
    fun todas(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        Red.trabajo({ pedirActividades("/actividades/todas") }, alTerminar)
    }

    // actividades del docente
    fun misClases(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        Red.trabajo({ pedirActividades("/actividades/mias") }, alTerminar)
    }

    fun detalle(id: Int, alTerminar: (Resultado_Modelo<DetalleActividad>) -> Unit) {
        Red.trabajo({
            val respuesta = Red.solicitud("GET", "/actividades/$id").objeto()
            DetalleActividad(actividadDe(respuesta.getJSONObject("actividad")), horariosDe(respuesta.optJSONArray("horarios")))
        }, alTerminar)
    }

    // dias y lugares para horarios
    fun catalogosHorario(alTerminar: (Resultado_Modelo<CatalogosHorario>) -> Unit) {
        Red.trabajo({
            val respuesta = Red.solicitud("GET", "/catalogos/horario").objeto()
            CatalogosHorario(
                respuesta.getJSONArray("dias").lista { DiaSemana(it.entero("id"), it.texto("nombre")) },
                respuesta.getJSONArray("lugares").lista { Lugar(it.entero("id"), it.texto("nombre")) },
            )
        }, alTerminar)
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
        Red.trabajo({
            val cuerpo = JSONObject()
            cuerpo.put("nombre", nombre)
            cuerpo.put("descripcion", descripcion)
            cuerpo.put("cupo", cupo)
            cuerpo.put("idDocente", idDocente)
            val lista = JSONArray()
            for (h in horarios) {
                val fila = JSONObject()
                fila.put("idDia", h.idDia)
                fila.put("horaInicio", h.horaInicio)
                fila.put("horaFin", h.horaFin)
                fila.put("lugar", h.lugar)
                lista.put(fila)
            }
            cuerpo.put("horarios", lista)
            if (id == null) {
                Red.solicitud("POST", "/actividades", cuerpo)
            } else {
                Red.solicitud("PUT", "/actividades/$id", cuerpo)
            }
            Unit
        }, alTerminar)
    }

    // baja logica para conservar historial
    fun eliminar(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Red.trabajo({
            Red.solicitud("DELETE", "/actividades/$id")
            Unit
        }, alTerminar)
    }

    fun inscribir(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Red.trabajo({
            Red.solicitud("POST", "/actividades/$idActividad/inscripcion")
            Unit
        }, alTerminar)
    }

    fun darDeBaja(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Red.trabajo({
            Red.solicitud("DELETE", "/actividades/$idActividad/inscripcion")
            Unit
        }, alTerminar)
    }

    fun horarioAlumno(alTerminar: (Resultado_Modelo<List<HorarioAlumno>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/horario").arreglo().lista {
                HorarioAlumno(it.entero("idActividad"), it.texto("nombre"), horarioDe(it.getJSONObject("dia")))
            }
        }, alTerminar)
    }

    fun constancias(alTerminar: (Resultado_Modelo<List<Constancia>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/constancias").arreglo().lista {
                Constancia(
                    it.entero("idActividad"), it.texto("nombre"), it.texto("periodo"),
                    it.entero("sesiones"), it.entero("asistencias"), it.optDouble("porcentaje", 0.0),
                    it.optBoolean("disponible", false),
                )
            }
        }, alTerminar)
    }

    // la api revisa el 80 % y da los datos; el pdf se arma en el telefono
    fun descargarConstancia(idActividad: Int, alTerminar: (Resultado_Modelo<Archivo>) -> Unit) {
        Red.trabajo({
            val d = Red.solicitud("POST", "/actividades/$idActividad/constancia").objeto()
            val datos = ConstanciaPdf(
                alumno = d.texto("alumno"),
                matricula = d.texto("matricula"),
                carrera = d.texto("carrera"),
                actividad = d.texto("actividad"),
                periodo = d.texto("periodo"),
                nivel = d.texto("nivel"),
                valor = d.texto("valor"),
                creditos = d.texto("creditos"),
                folio = d.texto("folio"),
                dia = d.texto("dia"),
                mes = d.texto("mes"),
                anio = d.texto("anio"),
            )
            Archivo(Archivos_Modelo.constanciaPdf(datos), d.texto("nombreArchivo"))
        }, alTerminar)
    }

    fun alumnosDeActividad(idActividad: Int, alTerminar: (Resultado_Modelo<List<AlumnoInscrito>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/actividades/$idActividad/alumnos").arreglo().lista {
                AlumnoInscrito(it.entero("id"), it.texto("nombre"), it.texto("matricula"))
            }
        }, alTerminar)
    }

    private fun pedirActividades(ruta: String): List<Actividad> {
        return Red.solicitud("GET", ruta).arreglo().lista { actividadDe(it) }
    }
}
