package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import itsm.extra_tec.databinding.InicioAdminVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class InicioAdmin_Vista : Base_Vista<InicioAdminVistaBinding>() {

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        InicioAdminVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnIrActividades.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_ACTIVIDADES) }
        binding.btnIrDocentes.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_DOCENTES) }
        binding.btnIrAlumnos.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_ALUMNOS) }
        binding.btnIrReportes.setOnClickListener { navegador.ir(Pantalla_Vista.REPORTES) }
        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }
    }
}
