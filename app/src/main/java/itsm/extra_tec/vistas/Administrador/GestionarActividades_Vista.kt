package itsm.extra_tec.vistas.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.GestionarActividades_ModeloVista
import itsm.extra_tec.databinding.GestionarActividadesVistaBinding
import itsm.extra_tec.vistas.Comun.Accion_Vista
import itsm.extra_tec.vistas.Comun.Base_Vista
import itsm.extra_tec.vistas.Comun.Pantalla_Vista
import itsm.extra_tec.vistas.Comun.agregarTarjeta

class GestionarActividades_Vista : Base_Vista<GestionarActividadesVistaBinding>() {
    private val modelo: GestionarActividades_ModeloVista by viewModels()

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        GestionarActividadesVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        binding.btnNuevaActividad.setOnClickListener { navegador.ir(Pantalla_Vista.FORMULARIO_ACTIVIDAD) }
        modelo.actividades.observe(viewLifecycleOwner) { actividades ->
            estadoLista(binding.tvVacio, actividades, "No hay actividades registradas.")
            binding.listaContenido.removeAllViews()
            actividades?.forEach { a ->
                agregarTarjeta(binding.listaContenido, a.nombre,
                    "${a.horario} · Prof. ${a.docente.ifEmpty { "Por asignar" }}",
                    "Cupo ${a.inscritos}/${a.cupo}",
                    b = Accion_Vista("Editar") {
                        navegador.ir(Pantalla_Vista.FORMULARIO_ACTIVIDAD, FormularioActividad_Vista.argumentos(a.id))
                    },
                    c = Accion_Vista("Eliminar") {
                        confirmar("¿Eliminar la actividad ${a.nombre}? Se conserva su historial.") { modelo.eliminar(a.id) }
                    })
            }
        }
    }
}
