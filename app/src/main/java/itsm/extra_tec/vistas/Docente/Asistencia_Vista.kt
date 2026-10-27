package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Asistencia_ModeloVista
import itsm.extra_tec.R
import itsm.extra_tec.databinding.AsistenciaVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class Asistencia_Vista : Base_Vista<AsistenciaVistaBinding>() {
    private val modelo: Asistencia_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        AsistenciaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val idClase = sesion.claseId ?: return navegador.ir(Pantalla_Vista.INICIO_DOCENTE)
        modelo.cargar(idClase)

        modelo.lista.observe(viewLifecycleOwner) { lista ->
            estadoLista(binding.tvVacio, lista?.alumnos, "No hay alumnos inscritos.")
            // las casillas se crean una vez; los cambios actualizan el viewmodel
            if (lista == null || binding.listaContenido.childCount > 0) return@observe
            binding.tvSubtitulo.text = "${sesion.claseNombre} · ${lista.fecha}"
            lista.alumnos.forEach { alumno ->
                val casilla = CheckBox(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    buttonTintList = ContextCompat.getColorStateList(context, R.color.itsm_primary)
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    text = "${alumno.nombre} · Matrícula ${alumno.matricula}"
                    setTextColor(ContextCompat.getColor(context, R.color.itsm_text))
                    isChecked = alumno.presente
                    setOnCheckedChangeListener { _, marcada -> modelo.marcar(alumno.idAlumno, marcada) }
                }
                binding.listaContenido.addView(casilla)
            }
        }
        modelo.guardada.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.ir(Pantalla_Vista.INICIO_DOCENTE) }
        }

        binding.btnGuardarAsistencia.setOnClickListener { modelo.guardar() }
        binding.btnCancelar.setOnClickListener { navegador.ir(Pantalla_Vista.INICIO_DOCENTE) }
    }
}
