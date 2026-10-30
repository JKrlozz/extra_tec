package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.GestionarAlumnos_ModeloVista
import itsm.extra_tec.databinding.GestionarAlumnosVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class GestionarAlumnos_Vista : Base_Vista<GestionarAlumnosVistaBinding>() {
    private val modelo: GestionarAlumnos_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        GestionarAlumnosVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnNuevoAlumno.setOnClickListener { navegador.ir(Pantalla_Vista.FORMULARIO_ALUMNO) }
        binding.btnTabDocentes.setOnClickListener { navegador.ir(Pantalla_Vista.GESTIONAR_DOCENTES) }

        modelo.alumnos.observe(viewLifecycleOwner) { alumnos ->
            estadoLista(binding.tvVacio, alumnos, "No hay alumnos.")
            binding.listaContenido.removeAllViews()
            alumnos?.forEach { al ->
                agregarTarjeta(binding.listaContenido, al.nombre,
                    "Matrícula ${al.matricula}, ${al.carrera}, semestre ${al.semestre}",
                    al.correo,
                    b = Accion_Vista("Editar") {
                        navegador.ir(Pantalla_Vista.FORMULARIO_ALUMNO, FormularioAlumno_Vista.argumentos(al))
                    },
                    c = Accion_Vista("Eliminar") {
                        confirmar("¿Eliminar a ${al.nombre}?") { modelo.eliminar(al.id) }
                    })
            }
        }
        alEscribir(binding.etBuscar) { modelo.buscar(it) }
    }
}
