package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import itsm.extra_tec.databinding.InicioAdminVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class InicioAdmin_Vista : Base_Vista() {
    private var _binding: InicioAdminVistaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InicioAdminVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val usuario = sesion.usuario
        if (usuario != null) {
            binding.tvDetalle.text = usuario.correo
        }
        binding.btnIrActividades.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_ACTIVIDADES, null) }
        binding.btnIrDocentes.setOnClickListener {
            navegador.ir(Pantalla_Vista.GESTIONAR_ALUMNOS, Personas_Vista.argumentos("alumno"))
        }
        binding.btnIrReportes.setOnClickListener { navegador.ir(Pantalla_Vista.REPORTES, null) }
        binding.btnIrSemestre.setOnClickListener { navegador.ir(Pantalla_Vista.SEMESTRE, null) }
        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
