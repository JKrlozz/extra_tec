package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.viewModels
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import itsm.extra_tec.Modelo.DetalleActividad
import itsm.extra_tec.Modelo.DiaSemana
import itsm.extra_tec.Modelo.HorarioDia
import itsm.extra_tec.Modelo.Limites
import itsm.extra_tec.Modelo.Lugar
import itsm.extra_tec.ModeloVista.FormularioActividad_ModeloVista
import itsm.extra_tec.databinding.FormularioActividadVistaBinding
import itsm.extra_tec.databinding.ItemHorarioVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.texto
import java.util.Locale

class FormularioActividad_Vista : Base_Vista() {
    private var _binding: FormularioActividadVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: FormularioActividad_ModeloVista by viewModels()

    // sin id es actividad nueva
    private var idActividad: Int? = null
    private val filas = mutableListOf<ItemHorarioVistaBinding>()
    private var dias = listOf<DiaSemana>()
    private var lugares = listOf<Lugar>()
    private var detalle: DetalleActividad? = null
    private var horariosPintados = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FormularioActividadVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        filas.clear()
        horariosPintados = false
        val id = arguments?.getInt(ARG_ID, -1) ?: -1
        if (id > 0) {
            idActividad = id
            binding.tvTituloForm.text = "Editar actividad"
        } else {
            binding.tvTituloForm.text = "Nueva actividad"
        }
        modelo.iniciar(idActividad)

        modelo.docentes.observe(viewLifecycleOwner) { docentes ->
            val nombres = mutableListOf<String>()
            for (docente in docentes) {
                nombres.add(docente.nombre)
            }
            llenarSpinner(binding.spinnerDocente, nombres)
            seleccionarDocente()
        }
        modelo.catalogos.observe(viewLifecycleOwner) { catalogos ->
            if (catalogos == null) {
                return@observe
            }
            dias = catalogos.dias
            lugares = catalogos.lugares
            for (fila in filas) {
                llenarFila(fila)
            }
            pintarHorarios()
        }
        modelo.detalle.observe(viewLifecycleOwner) { datos ->
            if (datos == null) {
                return@observe
            }
            detalle = datos
            binding.etNombre.setText(datos.actividad.nombre)
            binding.etDescripcion.setText(datos.actividad.descripcion)
            binding.etCupo.setText(datos.actividad.cupo.toString())
            seleccionarDocente()
            pintarHorarios()
        }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            if (evento.obtener() != null) {
                navegador.irRaiz(Pantalla_Vista.GESTIONAR_ACTIVIDADES, null)
            }
        }

        binding.btnAgregarHorario.setOnClickListener {
            if (filas.size >= 7) {
                aviso("Puedes registrar hasta 7 horarios.")
            } else {
                agregarFila(null)
            }
        }
        binding.btnGuardar.setOnClickListener {
            // primero nombre y descripcion, luego horarios
            val errorTextos = Limites.revisarActividad(
                binding.etNombre.texto(), binding.etDescripcion.texto(), Limites.CUPO_MIN,
            )
            if (errorTextos != null) {
                aviso(errorTextos)
                return@setOnClickListener
            }
            val horarios = leerHorarios()
            if (horarios != null) {
                modelo.guardar(
                    idActividad,
                    binding.etNombre.texto(),
                    binding.etDescripcion.texto(),
                    binding.etCupo.texto(),
                    horarios,
                    binding.spinnerDocente.selectedItemPosition,
                )
            }
        }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    private fun seleccionarDocente() {
        val actual = detalle ?: return
        val docentes = modelo.docentes.value ?: listOf()
        for (i in docentes.indices) {
            if (docentes[i].id == actual.actividad.idDocente) {
                binding.spinnerDocente.setSelection(i)
            }
        }
    }

    // filas cuando ya hay datos
    private fun pintarHorarios() {
        if (horariosPintados || dias.isEmpty()) {
            return
        }
        if (idActividad != null && detalle == null) {
            return
        }
        horariosPintados = true
        val iniciales = detalle?.horarios ?: listOf()
        if (iniciales.isEmpty()) {
            agregarFila(null)
        } else {
            for (horario in iniciales) {
                agregarFila(horario)
            }
        }
    }

    private fun agregarFila(horario: HorarioDia?) {
        val fila = ItemHorarioVistaBinding.inflate(layoutInflater, binding.contenedorHorarios, false)
        llenarFila(fila)
        if (horario != null) {
            for (i in dias.indices) {
                if (dias[i].id == horario.idDia) fila.spinnerDia.setSelection(i)
            }
            fila.etHoraInicio.setText(horario.horaInicio)
            fila.etHoraFin.setText(horario.horaFin)
            fila.etLugar.setText(horario.lugar, false)
        }
        fila.etHoraInicio.setOnClickListener { elegirHora(fila.etHoraInicio, "Hora de inicio") }
        fila.etHoraFin.setOnClickListener { elegirHora(fila.etHoraFin, "Hora de fin") }
        fila.tilHoraInicio.setEndIconOnClickListener { elegirHora(fila.etHoraInicio, "Hora de inicio") }
        fila.tilHoraFin.setEndIconOnClickListener { elegirHora(fila.etHoraFin, "Hora de fin") }
        fila.btnQuitar.setOnClickListener {
            binding.contenedorHorarios.removeView(fila.root)
            filas.remove(fila)
        }
        binding.contenedorHorarios.addView(fila.root)
        filas.add(fila)
    }

    private fun llenarFila(fila: ItemHorarioVistaBinding) {
        val nombresDias = mutableListOf<String>()
        for (dia in dias) {
            nombresDias.add(dia.nombre)
        }
        llenarSpinner(fila.spinnerDia, nombresDias)
        val nombresLugares = mutableListOf<String>()
        for (lugar in lugares) {
            nombresLugares.add(lugar.nombre)
        }
        fila.etLugar.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresLugares))
    }

    // reloj para elegir la hora
    private fun elegirHora(campo: EditText, titulo: String) {
        val partes = campo.text.toString().split(":")
        var hora = 16
        var minuto = 0
        if (partes.size == 2) {
            hora = partes[0].toIntOrNull() ?: 16
            minuto = partes[1].toIntOrNull() ?: 0
        }
        val selector = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hora)
            .setMinute(minuto)
            .setTitleText(titulo)
            .setPositiveButtonText("Aceptar")
            .setNegativeButtonText("Cancelar")
            .build()
        selector.addOnPositiveButtonClickListener {
            campo.setText(String.format(Locale.US, "%02d:%02d", selector.hour, selector.minute))
        }
        selector.show(childFragmentManager, "selector_hora")
    }

    // revisa cada fila antes de guardar
    private fun leerHorarios(): List<HorarioDia>? {
        if (filas.isEmpty()) {
            aviso("Agrega al menos un horario.")
            return null
        }
        val lista = mutableListOf<HorarioDia>()
        for (fila in filas) {
            val posicion = fila.spinnerDia.selectedItemPosition
            if (posicion < 0 || posicion >= dias.size) {
                aviso("Selecciona el día de cada horario.")
                return null
            }
            val inicio = fila.etHoraInicio.texto()
            val fin = fila.etHoraFin.texto()
            if (inicio.isEmpty() || fin.isEmpty()) {
                aviso("Selecciona la hora de inicio y la hora de fin.")
                return null
            }
            if (fin <= inicio) {
                aviso("La hora de fin debe ser posterior a la de inicio.")
                return null
            }
            var lugar = fila.etLugar.text.toString().trim()
            if (lugar.isEmpty()) {
                lugar = "ITSM"
            }
            val error = Limites.revisarLugar(lugar)
            if (error != null) {
                aviso(error)
                return null
            }
            var idLugar = 0
            for (l in lugares) {
                if (l.nombre.equals(lugar, true)) idLugar = l.id
            }
            lista.add(HorarioDia(dias[posicion].id, dias[posicion].nombre, inicio, fin, idLugar, lugar))
        }
        return lista
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "id"

        fun argumentos(id: Int): Bundle {
            val datos = Bundle()
            datos.putInt(ARG_ID, id)
            return datos
        }
    }
}
