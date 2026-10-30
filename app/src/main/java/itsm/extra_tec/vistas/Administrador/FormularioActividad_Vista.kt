package itsm.extra_tec.vistas.Administrador

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.DetalleActividad
import itsm.extra_tec.Modelo.DiaSemana
import itsm.extra_tec.Modelo.HorarioDia
import itsm.extra_tec.Modelo.Lugar
import itsm.extra_tec.ModeloVista.FormularioActividad_ModeloVista
import itsm.extra_tec.databinding.FormularioActividadVistaBinding
import itsm.extra_tec.databinding.ItemHorarioVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.texto

class FormularioActividad_Vista : Base_Vista<FormularioActividadVistaBinding>() {
    private val modelo: FormularioActividad_ModeloVista by viewModels()

    // sin id, el formulario crea una actividad nueva
    private val idActividad: Int? get() = arguments?.getInt(ARG_ID, -1)?.takeIf { it >= 0 }

    private val filas = mutableListOf<ItemHorarioVistaBinding>()
    private var dias: List<DiaSemana> = emptyList()
    private var lugares: List<Lugar> = emptyList()
    private var detalle: DetalleActividad? = null
    private var horariosPintados = false

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        FormularioActividadVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.tvTituloForm.text = if (idActividad == null) "Nueva actividad" else "Editar actividad"
        modelo.iniciar(idActividad)

        modelo.docentes.observe(viewLifecycleOwner) { docentes ->
            llenarSpinner(binding.spinnerDocente, docentes) { d -> d.nombre }
            val id = detalle?.actividad?.idDocente ?: return@observe
            val posicion = docentes.indexOfFirst { it.id == id }
            if (posicion >= 0) binding.spinnerDocente.setSelection(posicion)
        }
        modelo.catalogos.observe(viewLifecycleOwner) { catalogos ->
            if (catalogos == null) return@observe
            dias = catalogos.dias
            lugares = catalogos.lugares
            filas.forEach { actualizarAdaptadores(it) }
            pintarHorarios()
        }
        modelo.detalle.observe(viewLifecycleOwner) { d ->
            if (d == null) return@observe
            detalle = d
            val a = d.actividad
            binding.etNombre.setText(a.nombre)
            binding.etDescripcion.setText(a.descripcion)
            binding.etCupo.setText(a.cupo.toString())
            val posicion = modelo.docentes.value.orEmpty().indexOfFirst { it.id == a.idDocente }
            if (posicion >= 0) binding.spinnerDocente.setSelection(posicion)
            pintarHorarios()
        }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.irRaiz(Pantalla_Vista.GESTIONAR_ACTIVIDADES) }
        }

        binding.btnAgregarHorario.setOnClickListener { agregarFila() }
        binding.btnGuardar.setOnClickListener {
            val horarios = leerHorarios() ?: return@setOnClickListener
            modelo.guardar(
                idActividad,
                binding.etNombre.texto(),
                binding.etDescripcion.texto(),
                binding.etCupo.texto(),
                horarios,
                binding.spinnerDocente.selectedItemPosition,
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    // Crea las filas una sola vez, cuando ya hay catálogos y, si es edición, detalle.
    private fun pintarHorarios() {
        if (horariosPintados || dias.isEmpty()) return
        if (idActividad != null && detalle == null) return
        horariosPintados = true
        val iniciales = detalle?.horarios.orEmpty()
        if (iniciales.isEmpty()) agregarFila() else iniciales.forEach { agregarFila(it) }
    }

    private fun agregarFila(horario: HorarioDia? = null) {
        val fila = ItemHorarioVistaBinding.inflate(layoutInflater, binding.contenedorHorarios, false)
        actualizarAdaptadores(fila)
        if (horario != null) {
            val posicion = dias.indexOfFirst { it.id == horario.idDia }
            if (posicion >= 0) fila.spinnerDia.setSelection(posicion)
            fila.etHoraInicio.setText(horario.horaInicio)
            fila.etHoraFin.setText(horario.horaFin)
            fila.etLugar.setText(horario.lugar, false)
        }
        fila.etHoraInicio.setOnClickListener { elegirHora(fila.etHoraInicio) }
        fila.etHoraFin.setOnClickListener { elegirHora(fila.etHoraFin) }
        fila.btnQuitar.setOnClickListener {
            binding.contenedorHorarios.removeView(fila.root)
            filas.remove(fila)
        }
        binding.contenedorHorarios.addView(fila.root)
        filas.add(fila)
    }

    private fun actualizarAdaptadores(fila: ItemHorarioVistaBinding) {
        val adaptadorDias = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, dias.map { it.nombre })
        fila.spinnerDia.adapter = adaptadorDias
        val adaptadorLugares = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, lugares.map { it.nombre })
        fila.etLugar.setAdapter(adaptadorLugares)
    }

    private fun elegirHora(campo: EditText) {
        val partes = campo.text.toString().split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 16
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(requireContext(), { _, h, m ->
            campo.setText(String.format("%02d:%02d", h, m))
        }, hora, minuto, true).show()
    }

    private fun leerHorarios(): List<HorarioDia>? {
        if (filas.isEmpty()) {
            aviso("Agrega al menos un horario.")
            return null
        }
        val lista = mutableListOf<HorarioDia>()
        for (fila in filas) {
            val dia = dias.getOrNull(fila.spinnerDia.selectedItemPosition)
            if (dia == null) {
                aviso("Selecciona el día de cada horario.")
                return null
            }
            val inicio = fila.etHoraInicio.text.toString().trim()
            val fin = fila.etHoraFin.text.toString().trim()
            if (!HORA.matches(inicio) || !HORA.matches(fin)) {
                aviso("Selecciona la hora de inicio y la hora de fin.")
                return null
            }
            if (fin <= inicio) {
                aviso("La hora de fin debe ser posterior a la de inicio.")
                return null
            }
            val lugar = fila.etLugar.text.toString().trim().ifEmpty { "ITSM" }
            val idLugar = lugares.firstOrNull { it.nombre.equals(lugar, ignoreCase = true) }?.id ?: 0
            lista.add(HorarioDia(dia.id, dia.nombre, inicio, fin, idLugar, lugar))
        }
        return lista
    }

    companion object {
        private val HORA = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        private const val ARG_ID = "id"

        fun argumentos(id: Int) = Bundle().apply { putInt(ARG_ID, id) }
    }
}
