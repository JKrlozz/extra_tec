package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.Autenticacion_Modelo
import itsm.extra_tec.Modelo.Carrera
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.NumeroControl
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioAlumno_ModeloVista : Base_ModeloVista() {
    private val _carreras = MutableLiveData<List<Carrera>>(listOf())
    val carreras: LiveData<List<Carrera>> = _carreras

    private val _guardado = MutableLiveData<Evento_ModeloVista<Guardado_ModeloVista>>()
    val guardado: LiveData<Evento_ModeloVista<Guardado_ModeloVista>> = _guardado

    private val _actividades = MutableLiveData<List<Actividad>>()
    val actividades: LiveData<List<Actividad>> = _actividades

    // actividad que tiene hoy el alumno, null si ninguna
    private val _actividadActual = MutableLiveData<Int?>()
    val actividadActual: LiveData<Int?> = _actividadActual
    private var actualCargada = false

    init {
        Autenticacion_Modelo.carreras { resultado ->
            atender(resultado) { lista -> _carreras.value = lista }
        }
    }

    // solo al editar: lista y actividad actual
    fun cargarActividad(idAlumno: Int) {
        if (actualCargada) return
        Actividades_Modelo.todas { resultado ->
            atender(resultado) { lista -> _actividades.value = lista }
        }
        Actividades_Modelo.inscripcionDeAlumno(idAlumno) { resultado ->
            atender(resultado) { id ->
                actualCargada = true
                _actividadActual.value = id
            }
        }
    }

    fun actividadCargada(): Boolean = actualCargada

    // posicion 0 es sin actividad
    private fun actividadElegida(posicion: Int): Int? {
        val lista = _actividades.value ?: return null
        if (posicion < 1 || posicion > lista.size) return null
        return lista[posicion - 1].id
    }

    // texto para confirmar, null si no cambia
    fun avisoDeCambio(posicionActividad: Int): String? {
        if (!actualCargada || _actividades.value == null) return null
        val actual = _actividadActual.value
        val nueva = actividadElegida(posicionActividad)
        if (actual == nueva) return null
        if (nueva == null) return "¿Dar de baja al alumno de su actividad?"
        if (actual == null) return null
        return "¿Cambiar al alumno de actividad? Se dará de baja de la actual."
    }

    fun guardar(
        id: Int?,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        matricula: String,
        posicionSemestre: Int,
        posicionCarrera: Int,
        contrasena: String,
        posicionActividad: Int = -1,
    ) {
        val semestre = posicionSemestre + 1
        val carreras = _carreras.value ?: listOf()
        var error = Limites.revisarPersona(nombres, apellidoPaterno, apellidoMaterno, correo)
        if (error == null && !NumeroControl.esValido(matricula)) {
            error = NumeroControl.MENSAJE
        }
        if (error == null) {
            error = Limites.revisarCorreoAlumno(correo, matricula)
        }
        // al editar no cambia la clave
        if (error == null && id == null) {
            error = Limites.revisarClave(contrasena, true)
        }
        if (error == null && (semestre < 1 || semestre > Limites.SEMESTRE_MAX)) {
            error = "Elige un semestre de primero a octavo."
        }
        if (error == null && (posicionCarrera < 0 || posicionCarrera >= carreras.size)) {
            error = "Aún no se cargan las carreras."
        }
        if (error != null) {
            avisar(error)
            return
        }
        Personas_Modelo.guardarAlumno(
            id, nombres, apellidoPaterno, apellidoMaterno, correo, matricula,
            carreras[posicionCarrera].id, semestre, contrasena,
        ) { resultado ->
            atender(resultado) { clave ->
                if (id == null) {
                    terminar(clave)
                } else {
                    guardarActividad(id, posicionActividad, clave)
                }
            }
        }
    }

    // si la actividad cambio se inscribe o se da de baja
    private fun guardarActividad(idAlumno: Int, posicionActividad: Int, clave: String?) {
        val actual = _actividadActual.value
        val nueva = actividadElegida(posicionActividad)
        if (!actualCargada || _actividades.value == null || actual == nueva) {
            terminar(clave)
        } else if (nueva == null) {
            Actividades_Modelo.darDeBajaAlumno(idAlumno) { resultado ->
                atender(resultado) {
                    _actividadActual.value = null
                    terminar(clave)
                }
            }
        } else {
            Actividades_Modelo.inscribirAlumno(idAlumno, nueva) { resultado ->
                atender(resultado) {
                    _actividadActual.value = nueva
                    terminar(clave)
                }
            }
        }
    }

    private fun terminar(clave: String?) {
        avisar("Alumno guardado.")
        _guardado.value = Evento_ModeloVista(Guardado_ModeloVista(clave))
    }
}
