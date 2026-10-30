package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import itsm.extra_tec.ModeloVista.Registro_ModeloVista
import itsm.extra_tec.databinding.RegistroVistaBinding

class Registro_Vista : Base_Vista<RegistroVistaBinding>() {
    private val modelo: Registro_ModeloVista by viewModels()

    private val semestres = listOf(
        "Primero", "Segundo", "Tercero", "Cuarto", "Quinto",
        "Sexto", "Séptimo", "Octavo", "Noveno",
    )

    override fun crearBinding(inflador: LayoutInflater, contenedor: ViewGroup?) =
        RegistroVistaBinding.inflate(inflador, contenedor, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        escuchar(modelo)
        modelo.carreras.observe(viewLifecycleOwner) { llenarSpinner(binding.spinnerCarrera, it) { carrera -> carrera.nombre } }
        llenarSpinner(binding.spinnerSemestre, semestres) { it }
        modelo.registrado.observe(viewLifecycleOwner) { evento ->
            evento.obtener()?.let { navegador.irRaiz(Pantalla_Vista.INICIO_SESION) }
        }

        binding.btnRegistrarse.setOnClickListener {
            modelo.registrar(
                nombres = binding.etNombres.texto(),
                apellidoPaterno = binding.etApellidoPaterno.texto(),
                apellidoMaterno = binding.etApellidoMaterno.texto(),
                correo = binding.etCorreo.texto(),
                contrasena = binding.etContrasena.text.toString(),
                matricula = binding.etMatricula.texto(),
                posicionSemestre = binding.spinnerSemestre.selectedItemPosition,
                posicionCarrera = binding.spinnerCarrera.selectedItemPosition,
            )
        }
        binding.btnCancelar.setOnClickListener { navegador.atras() }
    }
}
