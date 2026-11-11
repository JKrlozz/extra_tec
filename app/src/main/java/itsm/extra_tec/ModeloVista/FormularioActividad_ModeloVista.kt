package itsm.extra_tec.ModeloVista

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import itsm.extra_tec.Modelo.Actividades_Modelo
import itsm.extra_tec.Modelo.CatalogosHorario
import itsm.extra_tec.Modelo.DetalleActividad
import itsm.extra_tec.Modelo.Docente
import itsm.extra_tec.Modelo.HorarioDia
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.Personas_Modelo

class FormularioActividad_ModeloVista : Base_ModeloVista() {
    private val _docentes = MutableLiveData<List<Docente>>(listOf())
    val docentes: LiveData<List<Docente>> = _docentes

    private val _catalogos = MutableLiveData<CatalogosHorario?>(null)
    val catalogos: LiveData<CatalogosHorario?> = _catalogos

    private val _detalle = MutableLiveData<DetalleActividad?>(null)
    val detalle: LiveData<DetalleActividad?> = _detalle

    private val _guardada = MutableLiveData<Evento_ModeloVista<Boolean>>()
    val guardada: LiveData<Evento_ModeloVista<Boolean>> = _guardada

    private var iniciado = false

    fun iniciar(idActividad: Int?) {
        if (iniciado) {
            return
        }
        iniciado = true
        Personas_Modelo.docentes("") { resultado ->
            atender(resultado) { lista -> _docentes.value = lista }
        }
        Actividades_Modelo.catalogosHorario { resultado ->
            atender(resultado) { catalogos -> _catalogos.value = catalogos }
        }
        if (idActividad != null) {
            Actividades_Modelo.detalle(idActividad) { resultado ->
                atender(resultado) { detalle -> _detalle.value = detalle }
            }
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
        val error = Limites.revisarActividad(nombre, descripcion, cupo)
        if (error != null || cupo == null) {
            avisar(error ?: "Escribe el cupo.")
            return
        }
        if (horarios.isEmpty()) {
            avisar("Agrega al menos un horario.")
            return
        }
        val docentes = _docentes.value ?: listOf()
        if (posicionDocente < 0 || posicionDocente >= docentes.size) {
            avisar("Primero registra un docente.")
            return
        }
        Actividades_Modelo.guardar(id, nombre, descripcion, cupo, docentes[posicionDocente].id, horarios) { resultado ->
            atender(resultado) {
                avisar("Actividad guardada.")
                _guardada.value = Evento_ModeloVista(true)
            }
        }
    }
}
