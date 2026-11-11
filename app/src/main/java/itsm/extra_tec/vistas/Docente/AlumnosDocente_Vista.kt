package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.AlumnosDocente_ModeloVista
import itsm.extra_tec.databinding.AlumnosDocenteVistaBinding
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class AlumnosDocente_Vista : Base_Vista() {
    private var _binding: AlumnosDocenteVistaBinding? = null
    private val binding get() = _binding!!
    private val modelo: AlumnosDocente_ModeloVista by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AlumnosDocenteVistaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        val idClase = sesion.claseId
        if (idClase == null) {
            navegador.irAlInicio()
            return
        }
        binding.tvSubtitulo.text = sesion.claseNombre
        modelo.cargar(idClase)

        modelo.visibles.observe(viewLifecycleOwner) { alumnos ->
            estadoLista(binding.tvVacio, alumnos, "Sin resultados.")
            binding.listaContenido.removeAllViews()
            if (alumnos == null) {
                return@observe
            }
            for (alumno in alumnos) {
                agregarTarjeta(
                    binding.listaContenido,
                    alumno.nombre,
                    detalles = listOf("Número de control" to alumno.matricula),
                )
            }
        }
        alEscribir(binding.etBuscar) { texto -> modelo.buscar(texto) }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
