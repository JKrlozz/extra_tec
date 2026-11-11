package itsm.extra_tec.vistas.Alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import itsm.extra_tec.databinding.InicioAlumnoVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista

class InicioAlumno_Vista : Base_Vista() {
    private var _binding: InicioAlumnoVistaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InicioAlumnoVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val usuario = sesion.usuario
        if (usuario != null) {
            binding.tvNombre.text = usuario.nombre
            binding.tvDetalle.text = "Alumno\n" + usuario.correo
        }
        binding.btnIrActividades.setOnClickListener { navegador.ir(Pantalla_Vista.ACTIVIDADES_ALUMNO, null) }
        binding.btnIrHorario.setOnClickListener { navegador.ir(Pantalla_Vista.HORARIO_ALUMNO, null) }
        binding.btnIrConstancia.setOnClickListener { navegador.ir(Pantalla_Vista.CONSTANCIA, null) }
        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
