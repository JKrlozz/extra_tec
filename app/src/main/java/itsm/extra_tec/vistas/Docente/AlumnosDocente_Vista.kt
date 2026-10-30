package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.AlumnosDocente_ModeloVista
import itsm.extra_tec.databinding.AlumnosDocenteVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class AlumnosDocente_Vista : Base_Vista<AlumnosDocenteVistaBinding>() {
    private val modelo: AlumnosDocente_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        AlumnosDocenteVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val idClase = sesion.claseId ?: return navegador.irRaiz(Pantalla_Vista.INICIO_DOCENTE)
        binding.tvSubtitulo.text = sesion.claseNombre
        modelo.cargar(idClase)

        modelo.visibles.observe(viewLifecycleOwner) { alumnos ->
            estadoLista(binding.tvVacio, alumnos, "Sin resultados.")
            binding.listaContenido.removeAllViews()
            alumnos?.forEach { agregarTarjeta(binding.listaContenido, it.nombre, "Matrícula ${it.matricula}") }
        }
        alEscribir(binding.etBuscar) { modelo.buscar(it) }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }
}
