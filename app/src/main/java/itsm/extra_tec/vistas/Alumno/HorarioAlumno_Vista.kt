package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.HorarioAlumno_ModeloVista
import itsm.extra_tec.databinding.HorarioAlumnoVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class HorarioAlumno_Vista : Base_Vista<HorarioAlumnoVistaBinding>() {
    private val modelo: HorarioAlumno_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        HorarioAlumnoVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnAgregarActividad.setOnClickListener { navegador.ir(Pantalla_Vista.ACTIVIDADES_ALUMNO) }
        modelo.horario.observe(viewLifecycleOwner) { tarjetas ->
            estadoLista(binding.tvVacio, tarjetas, "Aún no estás inscrito en ninguna actividad.")
            binding.listaContenido.removeAllViews()
            tarjetas?.forEach { agregarTarjeta(binding.listaContenido, it.nombre, it.dias, it.lugar) }
        }
    }
}
