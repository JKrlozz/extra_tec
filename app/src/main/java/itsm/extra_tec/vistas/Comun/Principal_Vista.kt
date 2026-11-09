package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import itsm.extra_tec.ModeloVista.Sesion_ModeloVista
import itsm.extra_tec.databinding.PrincipalVistaBinding
import itsm.extra_tec.vistas.Administrador.Personas_Vista

// Activity unica que aloja el fragmento y las barras
class Principal_Vista : AppCompatActivity(), Navegador_Vista {
    private lateinit var binding: PrincipalVistaBinding
    private val sesion: Sesion_ModeloVista by viewModels()
    private var pantallaActual = Pantalla_Vista.BIENVENIDA
    private var argumentosActuales: Bundle? = null

    // Historial de pantallas para volver
    private val pila = ArrayDeque<Pair<Pantalla_Vista, Bundle?>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = PrincipalVistaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.btnAtras.setOnClickListener { atras() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pila.isEmpty()) finish() else atras()
            }
        })

        conectarBarras()

        val restaurada = sesion.restaurar()
        if (savedInstanceState == null) {
            if (restaurada) irAlInicio() else irRaiz(Pantalla_Vista.BIENVENIDA)
        } else {
            // Al restaurar solo se recupera la barra
            pantallaActual = Pantalla_Vista.valueOf(savedInstanceState.getString(CLAVE_PANTALLA) ?: pantallaActual.name)
            mostrarBarra(pantallaActual.barra)
            actualizarBotonAtras()
        }
    }

    override fun onSaveInstanceState(salida: Bundle) {
        super.onSaveInstanceState(salida)
        salida.putString(CLAVE_PANTALLA, pantallaActual.name)
    }

    override fun ir(pantalla: Pantalla_Vista, argumentos: Bundle?) {
        if (pantalla != pantallaActual) pila.addLast(pantallaActual to argumentosActuales)
        mostrar(pantalla, argumentos)
    }

    override fun irRaiz(pantalla: Pantalla_Vista, argumentos: Bundle?) {
        pila.clear()
        mostrar(pantalla, argumentos)
    }

    override fun atras() {
        if (pila.isEmpty()) {
            irAlInicio()
            return
        }
        val (pantalla, argumentos) = pila.removeLast()
        mostrar(pantalla, argumentos)
    }

    override fun irAlInicio() {
        pila.clear()
        mostrar(
            when (sesion.rol) {
                "alumno" -> Pantalla_Vista.INICIO_ALUMNO
                "docente" -> Pantalla_Vista.INICIO_DOCENTE
                else -> Pantalla_Vista.INICIO_ADMIN
            }
        )
    }

    private fun mostrar(pantalla: Pantalla_Vista, argumentos: Bundle? = null) {
        pantallaActual = pantalla
        argumentosActuales = argumentos
        val fragmento = pantalla.crear().apply { arguments = argumentos }
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(binding.contenedor.id, fragmento)
            .commitAllowingStateLoss()
        mostrarBarra(pantalla.barra)
        actualizarBotonAtras()
    }

    private fun actualizarBotonAtras() {
        binding.contenedorVolver.visibility = if (pila.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun mostrarBarra(barra: Barra_Vista) {
        binding.barraAlumno.root.visibility = if (barra == Barra_Vista.ALUMNO) View.VISIBLE else View.GONE
        binding.barraDocente.root.visibility = if (barra == Barra_Vista.DOCENTE) View.VISIBLE else View.GONE
        binding.barraAdmin.root.visibility = if (barra == Barra_Vista.ADMIN) View.VISIBLE else View.GONE
    }

    private fun conectarBarras() {
        with(binding.barraAlumno) {
            nav1.setOnClickListener { irRaiz(Pantalla_Vista.INICIO_ALUMNO) }
            nav2.setOnClickListener { irRaiz(Pantalla_Vista.ACTIVIDADES_ALUMNO) }
            nav3.setOnClickListener { irRaiz(Pantalla_Vista.HORARIO_ALUMNO) }
            nav4.setOnClickListener { irRaiz(Pantalla_Vista.CONSTANCIA) }
        }
        with(binding.barraDocente) {
            nav1.setOnClickListener { irRaiz(Pantalla_Vista.INICIO_DOCENTE) }
            nav2.setOnClickListener { irRaiz(Pantalla_Vista.ASISTENCIA) }
            nav3.setOnClickListener { irRaiz(Pantalla_Vista.EVIDENCIA) }
        }
        with(binding.barraAdmin) {
            nav1.setOnClickListener { irRaiz(Pantalla_Vista.INICIO_ADMIN) }
            nav2.setOnClickListener { irRaiz(Pantalla_Vista.GESTIONAR_ACTIVIDADES) }
            nav3.setOnClickListener {
                irRaiz(Pantalla_Vista.GESTIONAR_ALUMNOS, Personas_Vista.argumentos("alumno"))
            }
            nav4.setOnClickListener { irRaiz(Pantalla_Vista.REPORTES) }
        }
    }

    private companion object {
        const val CLAVE_PANTALLA = "pantalla_actual"
    }
}

