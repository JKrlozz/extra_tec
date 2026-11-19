package itsm.extra_tec.Modelo

import org.json.JSONArray
import org.json.JSONObject

// lectura de lo que responde la api

internal fun JSONObject.texto(clave: String): String {
    if (isNull(clave)) return ""
    return getString(clave)
}

internal fun JSONObject.entero(clave: String): Int {
    if (isNull(clave)) return 0
    return getInt(clave)
}

internal fun JSONObject.enteroONulo(clave: String): Int? {
    if (isNull(clave)) return null
    return getInt(clave)
}

internal fun <T> JSONArray.lista(convertir: (JSONObject) -> T): List<T> {
    val resultado = mutableListOf<T>()
    for (i in 0 until length()) {
        resultado.add(convertir(getJSONObject(i)))
    }
    return resultado
}

internal fun Any?.objeto(): JSONObject {
    return this as? JSONObject ?: throw ErrorDatos("La respuesta del servidor no es válida.")
}

internal fun Any?.arreglo(): JSONArray {
    return this as? JSONArray ?: throw ErrorDatos("La respuesta del servidor no es válida.")
}

internal fun usuarioDe(o: JSONObject): Usuario {
    return Usuario(
        o.entero("id"), o.texto("nombres"), o.texto("apellidoPaterno"),
        o.texto("apellidoMaterno"), o.texto("correo"), o.texto("rol"),
    )
}

internal fun horarioDe(o: JSONObject): HorarioDia {
    return HorarioDia(
        o.entero("idDia"), o.texto("diaSemana"), o.texto("horaInicio"),
        o.texto("horaFin"), o.entero("idLugar"), o.texto("lugar"),
    )
}

internal fun actividadDe(o: JSONObject): Actividad {
    return Actividad(
        id = o.entero("id"),
        nombre = o.texto("nombre"),
        descripcion = o.texto("descripcion"),
        cupo = o.entero("cupo"),
        inscritos = o.entero("inscritos"),
        horario = o.texto("horario"),
        lugar = o.texto("lugar"),
        docente = o.texto("docente"),
        idDocente = o.enteroONulo("idDocente"),
        inscrito = o.optBoolean("inscrito", false),
    )
}

internal fun horariosDe(arreglo: JSONArray?): List<HorarioDia> {
    if (arreglo == null) return listOf()
    return arreglo.lista { horarioDe(it) }
}
