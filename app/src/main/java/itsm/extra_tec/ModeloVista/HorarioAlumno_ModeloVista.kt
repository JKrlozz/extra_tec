package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo

data class HorarioTarjeta(val nombre: String, val dias: String, val lugar: String)

class HorarioAlumno_ModeloVista : Base_ModeloVista() {
    private val _horario = MutableLiveData<List<HorarioTarjeta>?>(null)
    val horario: LiveData<List<HorarioTarjeta>?> = _horario

    init {
        Actividades_Modelo.horarioAlumno { resultado ->
            atender(resultado) { filas ->
                _horario.value = filas.groupBy { it.idActividad }.values.map { grupo ->
                    HorarioTarjeta(
                        nombre = grupo[0].nombre,
                        dias = grupo.joinToString("\n") { "${it.dia.diaSemana} · ${it.dia.horaInicio}–${it.dia.horaFin}" },
                        lugar = grupo[0].dia.lugar,
                    )
                }
            }
        }
    }
}
