package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import itsm.extra_tec.databinding.InicioAlumnoVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class InicioAlumno_Vista : Base_Vista<InicioAlumnoVistaBinding>() {

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        InicioAlumnoVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvNombre.text = sesion.usuario?.nombre
        binding.tvDetalle.text = "Alumno · ${sesion.usuario?.correo.orEmpty()}"
        binding.btnIrActividades.setOnClickListener { navegador.ir(Pantalla_Vista.ACTIVIDADES_ALUMNO) }
        binding.btnIrHorario.setOnClickListener { navegador.ir(Pantalla_Vista.HORARIO_ALUMNO) }
        binding.btnIrConstancia.setOnClickListener { navegador.ir(Pantalla_Vista.CONSTANCIA) }
        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }
    }
}
