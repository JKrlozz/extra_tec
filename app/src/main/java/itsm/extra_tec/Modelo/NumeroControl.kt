package itsm.extra_tec.Modelo

// numero de control del alumno
// formato 000T0000
object NumeroControl {
    const val AYUDA = "Formato 000T0000, por ejemplo 232T0182"
    const val MENSAJE = "El número de control debe tener el formato 000T0000, por ejemplo 232T0182."

    private val formato = Regex("^[0-9]{3}T[0-9]{4}$")

    fun normalizar(valor: String): String {
        return valor.trim().uppercase()
    }

    fun esValido(valor: String): Boolean {
        return formato.matches(normalizar(valor))
    }
}
