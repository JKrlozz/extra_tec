package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.DetalleActividad
import itsm.extra_tec.Modelo.Docente
import itsm.extra_tec.Modelo.HorarioDia
import itsm.extra_tec.Modelo.Personas_Modelo
import java.text.Normalizer

class FormularioActividad_ModeloVista : Base_ModeloVista() {
    private val _docentes = MutableLiveData<List<Docente>>(emptyList())
    val docentes: LiveData<List<Docente>> = _docentes

    private val _detalle = MutableLiveData<DetalleActividad?>(null)
    val detalle: LiveData<DetalleActividad?> = _detalle

    private val _guardada = MutableLiveData<Evento_ModeloVista<Unit>>()
    val guardada: LiveData<Evento_ModeloVista<Unit>> = _guardada

    private var iniciado = false

    fun iniciar(idActividad: Int?) {
        if (iniciado) return
        iniciado = true
        Personas_Modelo.docentes { resultado ->
            atender(resultado) { _docentes.value = it }
            if (idActividad != null) {
                Actividades_Modelo.detalle(idActividad) { r -> atender(r) { _detalle.value = it } }
            }
        }
    }

    fun guardar(
        id: Int?,
        nombre: String,
        descripcion: String,
        cupoTexto: String,
        horarioTexto: String,
        posicionDocente: Int,
    ) {
        val cupo = cupoTexto.toIntOrNull()
        val horarios = parsearHorarios(horarioTexto)
        val docente = _docentes.value.orEmpty().getOrNull(posicionDocente)
        when {
            nombre.isEmpty() -> return avisar("Escribe el nombre.")
            cupo == null || cupo <= 0 -> return avisar("El cupo debe ser un número mayor a 0.")
            horarios == null -> return avisar("Horario inválido. Ejemplo: Lunes 16:00-18:00, Miércoles 16:00-18:00")
            docente == null -> return avisar("Primero registra un docente.")
        }
        Actividades_Modelo.guardar(id, nombre, descripcion, cupo!!, docente!!.id, horarios!!) { resultado ->
            atender(resultado) {
                avisar("Actividad guardada.")
                _guardada.value = Evento_ModeloVista(Unit)
            }
        }
    }

    // Convierte "Lunes 16:00-18:00, Miércoles 16:00-18:00" a horarios; null si el texto es inválido
    private fun parsearHorarios(texto: String): List<HorarioDia>? {
        val patron = Regex("(\\p{L}+)\\s+(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})")
        val horarios = mutableListOf<HorarioDia>()
        for (m in patron.findAll(texto)) {
            val dia = DIAS.firstOrNull { sinAcentos(it) == sinAcentos(m.groupValues[1]) } ?: return null
            val inicio = m.groupValues[2].padStart(5, '0')
            val fin = m.groupValues[3].padStart(5, '0')
            if (fin <= inicio) return null
            horarios.add(HorarioDia(dia, inicio, fin))
        }
        return horarios.ifEmpty { null }
    }

    private fun sinAcentos(texto: String) =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

    private companion object {
        val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    }
}
