package itsm.extra_tec.vistas.Docente

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.Modelo.Actividad
import itsm.extra_tec.ModeloVista.InicioDocente_ModeloVista
import itsm.extra_tec.databinding.InicioDocenteVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class InicioDocente_Vista : Base_Vista<InicioDocenteVistaBinding>() {
    private val modelo: InicioDocente_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        InicioDocenteVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.tvNombre.text = "Prof. ${sesion.usuario?.nombre.orEmpty()}"
        binding.btnCerrarSesion.setOnClickListener { cerrarSesion() }
        modelo.clases.observe(viewLifecycleOwner) { clases ->
            estadoLista(binding.tvVacio, clases, "Aún no tienes clases asignadas.")
            binding.listaContenido.removeAllViews()
            clases?.forEach { clase ->
                agregarTarjeta(binding.listaContenido, clase.nombre, "${clase.horario} · ${clase.inscritos} alumnos",
                    a = Accion_Vista("Pase de lista") { abrir(clase, Pantalla_Vista.ASISTENCIA) },
                    b = Accion_Vista("Ver alumnos") { abrir(clase, Pantalla_Vista.ALUMNOS_DOCENTE) })
            }
        }
    }

    // La clase elegida queda en la sesión para la barra de navegación
    private fun abrir(clase: Actividad, destino: Pantalla_Vista) {
        sesion.claseId = clase.id
        sesion.claseNombre = clase.nombre
        navegador.ir(destino)
    }
}
