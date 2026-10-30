package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.CatalogosHorario
import itsm.extra_tec.Modelo.DetalleActividad
import itsm.extra_tec.Modelo.Docente
import itsm.extra_tec.Modelo.HorarioDia
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioActividad_ModeloVista : Base_ModeloVista() {
    private val _docentes = MutableLiveData<List<Docente>>(emptyList())
    val docentes: LiveData<List<Docente>> = _docentes

    private val _catalogos = MutableLiveData<CatalogosHorario?>(null)
    val catalogos: LiveData<CatalogosHorario?> = _catalogos

    private val _detalle = MutableLiveData<DetalleActividad?>(null)
    val detalle: LiveData<DetalleActividad?> = _detalle

    private val _guardada = MutableLiveData<Evento_ModeloVista<Unit>>()
    val guardada: LiveData<Evento_ModeloVista<Unit>> = _guardada

    private var iniciado = false

    fun iniciar(idActividad: Int?) {
        if (iniciado) return
        iniciado = true
        Personas_Modelo.docentes { resultado -> atender(resultado) { _docentes.value = it } }
        Actividades_Modelo.catalogosHorario { resultado -> atender(resultado) { _catalogos.value = it } }
        if (idActividad != null) {
            Actividades_Modelo.detalle(idActividad) { r -> atender(r) { _detalle.value = it } }
        }
    }

    fun guardar(
        id: Int?,
        nombre: String,
        descripcion: String,
        cupoTexto: String,
        horarios: List<HorarioDia>,
        posicionDocente: Int,
    ) {
        val cupo = cupoTexto.toIntOrNull()
        val docente = _docentes.value.orEmpty().getOrNull(posicionDocente)
        when {
            nombre.isEmpty() -> return avisar("Escribe el nombre.")
            cupo == null || cupo <= 0 -> return avisar("El cupo debe ser un número mayor a 0.")
            horarios.isEmpty() -> return avisar("Agrega al menos un horario.")
            horarios.any { it.horaInicio >= it.horaFin } ->
                return avisar("La hora de fin debe ser posterior a la de inicio.")
            docente == null -> return avisar("Primero registra un docente.")
        }
        Actividades_Modelo.guardar(id, nombre, descripcion, cupo!!, docente!!.id, horarios) { resultado ->
            atender(resultado) {
                avisar("Actividad guardada.")
                _guardada.value = Evento_ModeloVista(Unit)
            }
        }
    }
}
