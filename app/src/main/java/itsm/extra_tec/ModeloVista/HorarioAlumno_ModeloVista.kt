package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import java.util.Calendar

data class BloqueHorario(
    val idActividad: Int,
    val nombre: String,
    val horaInicio: String,
    val horaFin: String,
    val lugar: String,
)

data class DiaCalendario(
    val idDia: Int,
    val nombre: String,
    val esHoy: Boolean,
    val bloques: List<BloqueHorario>,
)

class HorarioAlumno_ModeloVista : Base_ModeloVista() {
    private val _horario = MutableLiveData<List<DiaCalendario>?>(null)
    val horario: LiveData<List<DiaCalendario>?> = _horario

    init {
        Actividades_Modelo.horarioAlumno { resultado ->
            atender(resultado) { filas ->
                val hoy = idDiaHoy()
                _horario.value = (1..6).map { idDia ->
                    val delDia = filas.filter { it.dia.idDia == idDia }
                    DiaCalendario(
                        idDia = idDia,
                        nombre = delDia.firstOrNull()?.dia?.diaSemana ?: nombreDia(idDia),
                        esHoy = idDia == hoy,
                        bloques = delDia.map {
                            BloqueHorario(
                                idActividad = it.idActividad,
                                nombre = it.nombre,
                                horaInicio = it.dia.horaInicio,
                                horaFin = it.dia.horaFin,
                                lugar = it.dia.lugar,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun idDiaHoy(): Int = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }

    private fun nombreDia(id: Int): String = when (id) {
        1 -> "Lunes"
        2 -> "Martes"
        3 -> "Miércoles"
        4 -> "Jueves"
        5 -> "Viernes"
        6 -> "Sábado"
        else -> "Domingo"
    }
}
