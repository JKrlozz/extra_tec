package itsm.extra_tec.vistas.Comun

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import itsm.extra_tec.ModeloVista.Sesion_ModeloVista
import itsm.extra_tec.databinding.PrincipalVistaBinding
import itsm.extra_tec.vistas.Administrador.FormularioActividad_Vista
import itsm.extra_tec.vistas.Administrador.FormularioAlumno_Vista
import itsm.extra_tec.vistas.Administrador.FormularioDocente_Vista
import itsm.extra_tec.vistas.Administrador.GestionarActividades_Vista
import itsm.extra_tec.vistas.Administrador.InicioAdmin_Vista
import itsm.extra_tec.vistas.Administrador.Personas_Vista
import itsm.extra_tec.vistas.Administrador.Reportes_Vista
import itsm.extra_tec.vistas.Alumno.ActividadesAlumno_Vista
import itsm.extra_tec.vistas.Alumno.Constancia_Vista
import itsm.extra_tec.vistas.Alumno.HorarioAlumno_Vista
import itsm.extra_tec.vistas.Alumno.InicioAlumno_Vista
import itsm.extra_tec.vistas.Docente.AlumnosDocente_Vista
import itsm.extra_tec.vistas.Docente.Asistencia_Vista
import itsm.extra_tec.vistas.Docente.Evidencia_Vista
import itsm.extra_tec.vistas.Docente.HistorialAsistencia_Vista
import itsm.extra_tec.vistas.Docente.HistorialEvidencia_Vista
import itsm.extra_tec.vistas.Docente.InicioDocente_Vista

// pantalla anterior para el boton volver
class Paso_Vista(val pantalla: Pantalla_Vista, val argumentos: Bundle?, val scroll: Int)

// unica activity que cambia los fragmentos
class Principal_Vista : AppCompatActivity(), Navegador_Vista {
    private lateinit var binding: PrincipalVistaBinding
    private val sesion: Sesion_ModeloVista by viewModels()
    private var pantallaActual = Pantalla_Vista.INICIO_SESION
    private var argumentosActuales: Bundle? = null
    private val historial = mutableListOf<Paso_Vista>()
    // posicion de la lista al volver
    private var scrollPendiente = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = PrincipalVistaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // espacio para barras y teclado
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.btnAtras.setOnClickListener { atras() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (historial.isEmpty()) {
                    finish()
                } else {
                    atras()
                }
            }
        })

        conectarBarras()
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                if (scrollPendiente > 0) {
                    val destino = scrollPendiente
                    scrollPendiente = 0
                    restaurarScroll(v, destino)
                }
            }
        }, false)

        val haySesion = sesion.restaurar()
        if (savedInstanceState == null) {
            if (haySesion) {
                irAlInicio()
            } else {
                irRaiz(Pantalla_Vista.INICIO_SESION, null)
            }
        } else {
            // al girar se recupera la barra
            val nombre = savedInstanceState.getString(CLAVE_PANTALLA)
            if (nombre != null) {
                pantallaActual = Pantalla_Vista.valueOf(nombre)
            }
            mostrarBarra(pantallaActual.barra)
            actualizarBotonAtras()
        }
    }

    override fun onSaveInstanceState(salida: Bundle) {
        super.onSaveInstanceState(salida)
        salida.putString(CLAVE_PANTALLA, pantallaActual.name)
    }

    override fun ir(pantalla: Pantalla_Vista, argumentos: Bundle?) {
        if (pantalla != pantallaActual) {
            historial.add(Paso_Vista(pantallaActual, argumentosActuales, scrollActual()))
        }
        mostrar(pantalla, argumentos)
    }

    override fun irRaiz(pantalla: Pantalla_Vista, argumentos: Bundle?) {
        historial.clear()
        mostrar(pantalla, argumentos)
    }

    override fun atras() {
        if (historial.isEmpty()) {
            irAlInicio()
            return
        }
        val paso = historial.removeAt(historial.size - 1)
        scrollPendiente = paso.scroll
        mostrar(paso.pantalla, paso.argumentos)
    }

    override fun irAlInicio() {
        historial.clear()
        if (sesion.rol == "alumno") {
            mostrar(Pantalla_Vista.INICIO_ALUMNO, null)
        } else if (sesion.rol == "docente") {
            mostrar(Pantalla_Vista.INICIO_DOCENTE, null)
        } else if (sesion.rol == "admin") {
            mostrar(Pantalla_Vista.INICIO_ADMIN, null)
        } else {
            mostrar(Pantalla_Vista.INICIO_SESION, null)
        }
    }

    private fun mostrar(pantalla: Pantalla_Vista, argumentos: Bundle?) {
        pantallaActual = pantalla
        argumentosActuales = argumentos
        val fragmento = crearFragmento(pantalla)
        fragmento.arguments = argumentos
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(binding.contenedor.id, fragmento)
            .commitAllowingStateLoss()
        mostrarBarra(pantalla.barra)
        actualizarBotonAtras()
    }

    private fun crearFragmento(pantalla: Pantalla_Vista): Fragment {
        return when (pantalla) {
            Pantalla_Vista.INICIO_SESION -> InicioSesion_Vista()
            Pantalla_Vista.REGISTRO -> Registro_Vista()
            Pantalla_Vista.INICIO_ALUMNO -> InicioAlumno_Vista()
            Pantalla_Vista.ACTIVIDADES_ALUMNO -> ActividadesAlumno_Vista()
            Pantalla_Vista.HORARIO_ALUMNO -> HorarioAlumno_Vista()
            Pantalla_Vista.CONSTANCIA -> Constancia_Vista()
            Pantalla_Vista.INICIO_DOCENTE -> InicioDocente_Vista()
            Pantalla_Vista.ASISTENCIA -> Asistencia_Vista()
            Pantalla_Vista.ALUMNOS_DOCENTE -> AlumnosDocente_Vista()
            Pantalla_Vista.EVIDENCIA -> Evidencia_Vista()
            Pantalla_Vista.HISTORIAL_ASISTENCIA -> HistorialAsistencia_Vista()
            Pantalla_Vista.HISTORIAL_EVIDENCIA -> HistorialEvidencia_Vista()
            Pantalla_Vista.INICIO_ADMIN -> InicioAdmin_Vista()
            Pantalla_Vista.GESTIONAR_ACTIVIDADES -> GestionarActividades_Vista()
            Pantalla_Vista.HISTORIAL_ASISTENCIA_ADMIN -> HistorialAsistencia_Vista()
            Pantalla_Vista.HISTORIAL_EVIDENCIA_ADMIN -> HistorialEvidencia_Vista()
            Pantalla_Vista.ASISTENCIA_ADMIN -> Asistencia_Vista()
            Pantalla_Vista.EVIDENCIA_ADMIN -> Evidencia_Vista()
            Pantalla_Vista.FORMULARIO_ACTIVIDAD -> FormularioActividad_Vista()
            Pantalla_Vista.GESTIONAR_DOCENTES -> Personas_Vista()
            Pantalla_Vista.FORMULARIO_DOCENTE -> FormularioDocente_Vista()
            Pantalla_Vista.GESTIONAR_ALUMNOS -> Personas_Vista()
            Pantalla_Vista.FORMULARIO_ALUMNO -> FormularioAlumno_Vista()
            Pantalla_Vista.REPORTES -> Reportes_Vista()
        }
    }

    private fun buscarScroll(vista: View?): ScrollView? {
        if (vista is ScrollView) {
            return vista
        }
        if (vista is ViewGroup) {
            for (i in 0 until vista.childCount) {
                val encontrado = buscarScroll(vista.getChildAt(i))
                if (encontrado != null) return encontrado
            }
        }
        return null
    }

    private fun scrollActual(): Int {
        val actual = supportFragmentManager.findFragmentById(binding.contenedor.id)
        return buscarScroll(actual?.view)?.scrollY ?: 0
    }

    // la lista carga despues, se espera a que crezca
    private fun restaurarScroll(raiz: View, destino: Int) {
        val scroll = buscarScroll(raiz) ?: return
        val limite = System.currentTimeMillis() + 2500
        scroll.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val contenido = scroll.getChildAt(0)
                val maximo = if (contenido == null) 0 else contenido.height - scroll.height
                if (maximo >= destino || System.currentTimeMillis() > limite) {
                    scroll.scrollTo(0, minOf(destino, maxOf(maximo, 0)))
                    if (scroll.viewTreeObserver.isAlive) {
                        scroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            }
        })
    }

    private fun actualizarBotonAtras() {
        // inicio de sesion y registro ya tienen sus botones
        val propias = pantallaActual == Pantalla_Vista.INICIO_SESION || pantallaActual == Pantalla_Vista.REGISTRO
        if (historial.isEmpty() || propias) {
            binding.contenedorVolver.visibility = View.GONE
        } else {
            binding.contenedorVolver.visibility = View.VISIBLE
        }
    }

    private fun mostrarBarra(barra: Barra_Vista) {
        binding.barraAlumno.root.visibility = View.GONE
        binding.barraDocente.root.visibility = View.GONE
        binding.barraAdmin.root.visibility = View.GONE
        if (barra == Barra_Vista.ALUMNO) binding.barraAlumno.root.visibility = View.VISIBLE
        if (barra == Barra_Vista.DOCENTE) binding.barraDocente.root.visibility = View.VISIBLE
        if (barra == Barra_Vista.ADMIN) binding.barraAdmin.root.visibility = View.VISIBLE
    }

    private fun conectarBarras() {
        binding.barraAlumno.nav1.setOnClickListener { irRaiz(Pantalla_Vista.INICIO_ALUMNO, null) }
        binding.barraAlumno.nav2.setOnClickListener { irRaiz(Pantalla_Vista.ACTIVIDADES_ALUMNO, null) }
        binding.barraAlumno.nav3.setOnClickListener { irRaiz(Pantalla_Vista.HORARIO_ALUMNO, null) }
        binding.barraAlumno.nav4.setOnClickListener { irRaiz(Pantalla_Vista.CONSTANCIA, null) }

        binding.barraDocente.nav1.setOnClickListener { irRaiz(Pantalla_Vista.INICIO_DOCENTE, null) }
        binding.barraDocente.nav2.setOnClickListener { irRaiz(Pantalla_Vista.ASISTENCIA, null) }
        binding.barraDocente.nav3.setOnClickListener { irRaiz(Pantalla_Vista.EVIDENCIA, null) }

        binding.barraAdmin.nav1.setOnClickListener { irRaiz(Pantalla_Vista.INICIO_ADMIN, null) }
        binding.barraAdmin.nav2.setOnClickListener { irRaiz(Pantalla_Vista.GESTIONAR_ACTIVIDADES, null) }
        binding.barraAdmin.nav3.setOnClickListener {
            irRaiz(Pantalla_Vista.GESTIONAR_ALUMNOS, Personas_Vista.argumentos("alumno"))
        }
        binding.barraAdmin.nav4.setOnClickListener { irRaiz(Pantalla_Vista.REPORTES, null) }
    }

    companion object {
        private const val CLAVE_PANTALLA = "pantalla_actual"
    }
}
