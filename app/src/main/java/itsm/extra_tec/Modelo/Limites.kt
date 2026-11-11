package itsm.extra_tec.Modelo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// limites de los datos
// iguales en la base y xml
object Limites {
    const val NOMBRE_MIN = 2
    const val NOMBRES_MAX = 50
    const val APELLIDO_MAX = 40
    const val CORREO_MAX = 100
    const val CLAVE_MIN = 8
    const val CLAVE_MAX = 30
    const val TELEFONO = 10
    const val ESPECIALIDAD_MAX = 50
    const val ACTIVIDAD_MIN = 3
    const val ACTIVIDAD_MAX = 50
    const val DESCRIPCION_ACTIVIDAD_MAX = 200
    const val CUPO_MIN = 1
    const val CUPO_MAX = 100
    const val LUGAR_MAX = 60
    const val DESCRIPCION_EVIDENCIA_MAX = 300
    const val FOTOS_MAX = 10
    const val FOTO_BYTES_MAX = 8 * 1024 * 1024
    const val SEMESTRE_MAX = 8
    const val DOMINIO = "@itsm.edu.mx"

    private val letras = Regex("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' .-]+$")
    private val correo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val digitos = Regex("^[0-9]+$")
    private val fecha = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    // todas regresan el error o null
    fun revisarNombre(campo: String, valor: String, maximo: Int, obligatorio: Boolean): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) {
            if (obligatorio) {
                return "Escribe $campo."
            }
            return null
        }
        if (texto.length < NOMBRE_MIN) {
            return "$campo debe tener al menos $NOMBRE_MIN letras."
        }
        if (texto.length > maximo) {
            return "$campo puede tener hasta $maximo caracteres."
        }
        if (!letras.matches(texto)) {
            return "$campo solo puede tener letras."
        }
        return null
    }

    fun revisarCorreo(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) {
            return "Escribe el correo."
        }
        if (texto.length > CORREO_MAX) {
            return "El correo puede tener hasta $CORREO_MAX caracteres."
        }
        if (!correo.matches(texto)) {
            return "Escribe un correo válido, por ejemplo nombre@itsm.edu.mx."
        }
        if (!texto.lowercase().endsWith(DOMINIO)) {
            return "El correo debe terminar en $DOMINIO."
        }
        return null
    }

    // el alumno usa numerocontrol@itsm.edu.mx
    fun revisarCorreoAlumno(valor: String, matricula: String): String? {
        val error = revisarCorreo(valor)
        if (error != null) {
            return error
        }
        val esperado = matricula.trim().lowercase() + DOMINIO
        if (valor.trim().lowercase() != esperado) {
            return "El correo del alumno debe ser su número de control, por ejemplo 193t0183$DOMINIO."
        }
        return null
    }

    // vacia si se genera sola
    fun revisarClave(valor: String, puedeIrVacia: Boolean): String? {
        if (valor.isEmpty() && puedeIrVacia) {
            return null
        }
        if (valor.length < CLAVE_MIN || valor.length > CLAVE_MAX) {
            return "La contraseña debe tener de $CLAVE_MIN a $CLAVE_MAX caracteres."
        }
        if (valor.contains(" ")) {
            return "La contraseña no puede tener espacios."
        }
        return null
    }

    fun revisarPersona(nombres: String, paterno: String, materno: String, correo: String): String? {
        var error = revisarNombre("Nombre(s)", nombres, NOMBRES_MAX, true)
        if (error == null) {
            error = revisarNombre("Apellido paterno", paterno, APELLIDO_MAX, true)
        }
        if (error == null) {
            error = revisarNombre("Apellido materno", materno, APELLIDO_MAX, false)
        }
        if (error == null) {
            error = revisarCorreo(correo)
        }
        return error
    }

    // el telefono es opcional
    fun revisarTelefono(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) {
            return null
        }
        if (texto.length != TELEFONO || !digitos.matches(texto)) {
            return "El teléfono debe tener $TELEFONO dígitos."
        }
        return null
    }

    fun revisarEspecialidad(valor: String): String? {
        return revisarNombre("Especialidad", valor, ESPECIALIDAD_MAX, false)
    }

    fun revisarActividad(nombre: String, descripcion: String, cupo: Int?): String? {
        val texto = nombre.trim()
        if (texto.length < ACTIVIDAD_MIN || texto.length > ACTIVIDAD_MAX) {
            return "El nombre debe tener de $ACTIVIDAD_MIN a $ACTIVIDAD_MAX caracteres."
        }
        if (descripcion.trim().length > DESCRIPCION_ACTIVIDAD_MAX) {
            return "La descripción puede tener hasta $DESCRIPCION_ACTIVIDAD_MAX caracteres."
        }
        if (cupo == null || cupo < CUPO_MIN || cupo > CUPO_MAX) {
            return "El cupo debe ser de $CUPO_MIN a $CUPO_MAX alumnos."
        }
        return null
    }

    fun revisarLugar(valor: String): String? {
        val texto = valor.trim()
        if (texto.length < NOMBRE_MIN || texto.length > LUGAR_MAX) {
            return "El lugar debe tener de $NOMBRE_MIN a $LUGAR_MAX caracteres."
        }
        return null
    }

    fun revisarDescripcionEvidencia(valor: String): String? {
        if (valor.trim().length > DESCRIPCION_EVIDENCIA_MAX) {
            return "La descripción puede tener hasta $DESCRIPCION_EVIDENCIA_MAX caracteres."
        }
        return null
    }

    // no se aceptan fechas futuras
    fun revisarFecha(valor: String): String? {
        if (!fecha.matches(valor)) {
            return "La fecha debe tener el formato AAAA-MM-DD."
        }
        if (valor > hoy()) {
            return "La fecha no puede ser posterior a hoy."
        }
        return null
    }

    fun hoy(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formato.format(Date())
    }
}
