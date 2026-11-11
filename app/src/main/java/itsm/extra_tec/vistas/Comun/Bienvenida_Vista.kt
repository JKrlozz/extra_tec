package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import itsm.extra_tec.databinding.BienvenidaVistaBinding

class Bienvenida_Vista : Base_Vista() {
    private var _binding: BienvenidaVistaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BienvenidaVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnEntrar.setOnClickListener { navegador.ir(Pantalla_Vista.INICIO_SESION, null) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
