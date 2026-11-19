package itsm.extra_tec.Modelo

import org.json.JSONObject

data class SemestreActivo(val id: Int, val nombre: String, val inicio: String, val fin: String)

// semestre ya cerrado con lo que quedo guardado
data class SemestreArchivado(
    val id: Int,
    val nombre: String,
    val inicio: String,
    val fin: String,
    val cerrado: String,
    val actividades: Int,
    val inscripciones: Int,
    val sesiones: Int,
    val evidencias: Int,
)

// copiadas es cuantas actividades pasaron al semestre nuevo
data class EstadoSemestre(
    val activo: SemestreActivo?,
    val archivados: List<SemestreArchivado>,
    val copiadas: Int,
)

object Semestre_Modelo {

    fun estado(alTerminar: (Resultado_Modelo<EstadoSemestre>) -> Unit) {
        Red.trabajo({ leer(Red.solicitud("GET", "/semestre").objeto()) }, alTerminar)
    }

    // con semestre activo cambia sus fechas, sin semestre inicia uno
    fun guardar(inicio: String, fin: String, alTerminar: (Resultado_Modelo<EstadoSemestre>) -> Unit) {
        Red.trabajo({
            if (inicio.isEmpty() || fin.isEmpty()) {
                throw ErrorDatos("Elige la fecha de inicio y la de fin.")
            }
            if (fin <= inicio) {
                throw ErrorDatos("La fecha de fin debe ser posterior a la de inicio.")
            }
            val cuerpo = JSONObject()
            cuerpo.put("inicio", inicio)
            cuerpo.put("fin", fin)
            leer(Red.solicitud("PUT", "/semestre", cuerpo).objeto())
        }, alTerminar)
    }

    // archiva el semestre antes de su fecha final
    fun cerrar(alTerminar: (Resultado_Modelo<EstadoSemestre>) -> Unit) {
        Red.trabajo({ leer(Red.solicitud("POST", "/semestre/cerrar").objeto()) }, alTerminar)
    }

    private fun leer(o: JSONObject): EstadoSemestre {
        var activo: SemestreActivo? = null
        if (!o.isNull("activo")) {
            val a = o.getJSONObject("activo")
            activo = SemestreActivo(a.entero("id"), a.texto("nombre"), a.texto("inicio"), a.texto("fin"))
        }
        val archivados = o.getJSONArray("archivados").lista {
            SemestreArchivado(
                it.entero("id"), it.texto("nombre"), it.texto("inicio"), it.texto("fin"), it.texto("cerrado"),
                it.entero("actividades"), it.entero("inscripciones"), it.entero("sesiones"), it.entero("evidencias"),
            )
        }
        return EstadoSemestre(activo, archivados, o.optInt("copiadas", 0))
    }
}
