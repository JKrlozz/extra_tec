package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.GestionarDocentes_ModeloVista
import itsm.extra_tec.databinding.GestionarDocentesVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class GestionarDocentes_Vista : Base_Vista<GestionarDocentesVistaBinding>() {
    private val modelo: GestionarDocentes_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        GestionarDocentesVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnNuevoDocente.setOnClickListener { navegador.ir(Pantalla_Vista.FORMULARIO_DOCENTE) }
        binding.btnTabAlumnos.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_ALUMNOS) }

        modelo.visibles.observe(viewLifecycleOwner) { docentes ->
            estadoLista(binding.tvVacio, docentes, "No hay docentes.")
            binding.listaContenido.removeAllViews()
            docentes?.forEach { d ->
                agregarTarjeta(binding.listaContenido, d.nombre,
                    listOf(d.especialidad, d.telefono).filter { it.isNotEmpty() }.joinToString(", "),
                    d.correo,
                    b = Accion_Vista("Editar") {
                        navegador.ir(Pantalla_Vista.FORMULARIO_DOCENTE, FormularioDocente_Vista.argumentos(d))
                    },
                    c = Accion_Vista("Eliminar") {
                        confirmar("¿Eliminar a ${d.nombre}?") { modelo.eliminar(d.id) }
                    })
            }
        }
        alEscribir(binding.etBuscar) { modelo.buscar(it) }
    }
}
