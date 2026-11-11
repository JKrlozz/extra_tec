package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.HorarioAlumno
import java.util.Calendar

data class BloqueHorario(
    val nombre: String,
    val horaInicio: String,
    val horaFin: String,
    val lugar: String,
)

data class DiaCalendario(
    val nombre: String,
    val esHoy: Boolean,
    val bloques: List<BloqueHorario>,
)

class HorarioAlumno_ModeloVista : Base_ModeloVista() {
    private val _horario = MutableLiveData<List<DiaCalendario>?>(null)
    val horario: LiveData<List<DiaCalendario>?> = _horario

    private val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

    init {
        Actividades_Modelo.horarioAlumno { resultado ->
            atender(resultado) { filas -> armarSemana(filas) }
        }
    }

    // una tarjeta por dia
    private fun armarSemana(filas: List<HorarioAlumno>) {
        val hoy = diaDeHoy()
        val semana = mutableListOf<DiaCalendario>()
        for (i in dias.indices) {
            val idDia = i + 1
            val bloques = mutableListOf<BloqueHorario>()
            for (fila in filas) {
                if (fila.dia.idDia == idDia) {
                    bloques.add(BloqueHorario(fila.nombre, fila.dia.horaInicio, fila.dia.horaFin, fila.dia.lugar))
                }
            }
            semana.add(DiaCalendario(dias[i], idDia == hoy, bloques))
        }
        _horario.value = semana
    }

    // lunes es 1 y domingo 7
    private fun diaDeHoy(): Int {
        val dia = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (dia == Calendar.SUNDAY) {
            return 7
        }
        return dia - 1
    }
}
