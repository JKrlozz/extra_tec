package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import itsm.extra_tec.databinding.BienvenidaVistaBinding

class Bienvenida_Vista : Base_Vista<BienvenidaVistaBinding>() {

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        BienvenidaVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnEntrar.setOnClickListener { navegador.ir(Pantalla_Vista.INICIO_SESION) }
    }
}
