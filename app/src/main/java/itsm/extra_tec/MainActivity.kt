package itsm.extra_tec

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var rol: String = "alumno"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mostrarInicio()
    }

    // Respeta las barras del sistema (modo edge-to-edge); el fondo azul de la ventana las rellena.
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    private fun conectarNavegacion(rolDestino: String) {
        val destinos: List<() -> Unit> = when (rolDestino) {
            "alumno" -> listOf(::mostrarInicioAlumno, ::mostrarActividadesAlumno, ::mostrarHorarioAlumno, ::mostrarConstancia)
            "docente" -> listOf(::mostrarInicioDocente, ::mostrarAsistencia, ::mostrarEvidencia)
            else -> listOf(::mostrarInicioAdmin, ::mostrarGestionarActividades, ::mostrarGestionarDocentes, ::mostrarReportes)
        }
        val ids = listOf(R.id.nav1, R.id.nav2, R.id.nav3, R.id.nav4)
        destinos.forEachIndexed { indice, accion ->
            findViewById<Button?>(ids[indice])?.setOnClickListener { accion() }
        }
    }

    private fun cancelar(accion: () -> Unit) {
        findViewById<Button?>(R.id.btnCancelar)?.setOnClickListener { accion() }
    }

    private fun mostrarInicio() {
        setContentView(R.layout.inicio)
        findViewById<Button>(R.id.btnEntrar).setOnClickListener { mostrarInicioSesion() }
    }

    private fun mostrarInicioSesion() {
        setContentView(R.layout.inicio_sesion)
        val spinner = findViewById<Spinner>(R.id.spinnerRol)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("alumno", "docente", "admin")
        )
        findViewById<Button>(R.id.btnIniciarSesion).setOnClickListener {
            rol = spinner.selectedItem.toString()
            when (rol) {
                "alumno" -> mostrarInicioAlumno()
                "docente" -> mostrarInicioDocente()
                else -> mostrarInicioAdmin()
            }
        }
        findViewById<Button>(R.id.btnIrRegistro).setOnClickListener { mostrarRegistro() }
    }

    private fun mostrarRegistro() {
        setContentView(R.layout.registro)
        findViewById<Button>(R.id.btnRegistrarse).setOnClickListener {
            Toast.makeText(this, "Registro realizado.", Toast.LENGTH_SHORT).show()
            mostrarInicioSesion()
        }
        cancelar { mostrarInicioSesion() }
    }

    // ---------- ALUMNO ----------

    private fun mostrarInicioAlumno() {
        setContentView(R.layout.inicio_alumno)
        findViewById<Button>(R.id.btnIrActividades).setOnClickListener { mostrarActividadesAlumno() }
        findViewById<Button>(R.id.btnIrHorario).setOnClickListener { mostrarHorarioAlumno() }
        findViewById<Button>(R.id.btnIrConstancia).setOnClickListener { mostrarConstancia() }
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { mostrarInicioSesion() }
        conectarNavegacion("alumno")
    }

    private fun mostrarActividadesAlumno() {
        setContentView(R.layout.actividades_alumno)
        val inscribir: (Button) -> Unit = { boton ->
            boton.text = "Ya inscrito"
            boton.isEnabled = false
            Toast.makeText(this, "Inscripción realizada.", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnInscribir1).setOnClickListener { inscribir(it as Button) }
        findViewById<Button>(R.id.btnInscribir2).setOnClickListener { inscribir(it as Button) }
        findViewById<Button>(R.id.btnInscribir3).setOnClickListener { inscribir(it as Button) }
        conectarNavegacion("alumno")
    }

    private fun mostrarHorarioAlumno() {
        setContentView(R.layout.horario_alumno)
        findViewById<Button>(R.id.btnAgregarActividad).setOnClickListener { mostrarActividadesAlumno() }
        conectarNavegacion("alumno")
    }

    private fun mostrarConstancia() {
        setContentView(R.layout.constancia)
        findViewById<Button>(R.id.btnDescargar).setOnClickListener {
            Toast.makeText(this, "Descarga de constancia PDF iniciada.", Toast.LENGTH_SHORT).show()
        }
        conectarNavegacion("alumno")
    }

    // ---------- DOCENTE ----------

    private fun mostrarInicioDocente() {
        setContentView(R.layout.inicio_docente)
        findViewById<Button>(R.id.btnPaseLista).setOnClickListener { mostrarAsistencia() }
        findViewById<Button>(R.id.btnPaseLista2).setOnClickListener { mostrarAsistencia() }
        findViewById<Button>(R.id.btnVerAlumnos).setOnClickListener { mostrarAlumnosDocente() }
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { mostrarInicioSesion() }
        conectarNavegacion("docente")
    }

    private fun mostrarAsistencia() {
        setContentView(R.layout.asistencia)
        findViewById<Button>(R.id.btnGuardarAsistencia).setOnClickListener {
            Toast.makeText(this, "Asistencia guardada.", Toast.LENGTH_SHORT).show()
            mostrarInicioDocente()
        }
        cancelar { mostrarInicioDocente() }
        conectarNavegacion("docente")
    }

    private fun mostrarAlumnosDocente() {
        setContentView(R.layout.alumnos_docente)
        cancelar { mostrarInicioDocente() }
        conectarNavegacion("docente")
    }

    private fun mostrarEvidencia() {
        setContentView(R.layout.evidencia)
        findViewById<Button>(R.id.btnGuardarEvidencia).setOnClickListener {
            Toast.makeText(this, "Evidencia guardada.", Toast.LENGTH_SHORT).show()
            mostrarInicioDocente()
        }
        conectarNavegacion("docente")
    }

    // ---------- ADMINISTRADOR ----------

    private fun mostrarInicioAdmin() {
        setContentView(R.layout.inicio_admin)
        findViewById<Button>(R.id.btnIrActividades).setOnClickListener { mostrarGestionarActividades() }
        findViewById<Button>(R.id.btnIrDocentes).setOnClickListener { mostrarGestionarDocentes() }
        findViewById<Button>(R.id.btnIrAlumnos).setOnClickListener { mostrarGestionarAlumnos() }
        findViewById<Button>(R.id.btnIrReportes).setOnClickListener { mostrarReportes() }
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { mostrarInicioSesion() }
        conectarNavegacion("admin")
    }

    private fun mostrarGestionarActividades() {
        setContentView(R.layout.gestionar_actividades)
        findViewById<Button>(R.id.btnNuevaActividad).setOnClickListener { mostrarFormularioActividad() }
        findViewById<Button>(R.id.btnEditarActividad).setOnClickListener { mostrarFormularioActividad() }
        findViewById<Button>(R.id.btnEditarActividad2).setOnClickListener { mostrarFormularioActividad() }
        val eliminar: (Button) -> Unit = {
            Toast.makeText(this, "Actividad eliminada.", Toast.LENGTH_SHORT).show()
            it.isEnabled = false
            it.text = "Eliminado"
        }
        findViewById<Button>(R.id.btnEliminarActividad).setOnClickListener { eliminar(it as Button) }
        findViewById<Button>(R.id.btnEliminarActividad2).setOnClickListener { eliminar(it as Button) }
        conectarNavegacion("admin")
    }

    private fun mostrarFormularioActividad() {
        setContentView(R.layout.formulario_actividad)
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            Toast.makeText(this, "Actividad guardada.", Toast.LENGTH_SHORT).show()
            mostrarGestionarActividades()
        }
        cancelar { mostrarGestionarActividades() }
    }

    private fun mostrarGestionarDocentes() {
        setContentView(R.layout.gestionar_docentes)
        findViewById<Button>(R.id.btnNuevoDocente).setOnClickListener { mostrarFormularioDocente() }
        findViewById<Button>(R.id.btnEditarDocente).setOnClickListener { mostrarFormularioDocente() }
        findViewById<Button>(R.id.btnEditarDocente2).setOnClickListener { mostrarFormularioDocente() }
        findViewById<Button>(R.id.btnTabAlumnos).setOnClickListener { mostrarGestionarAlumnos() }
        val eliminar: (Button) -> Unit = {
            Toast.makeText(this, "Docente eliminado.", Toast.LENGTH_SHORT).show()
            it.isEnabled = false
            it.text = "Eliminado"
        }
        findViewById<Button>(R.id.btnEliminarDocente).setOnClickListener { eliminar(it as Button) }
        findViewById<Button>(R.id.btnEliminarDocente2).setOnClickListener { eliminar(it as Button) }
        conectarNavegacion("admin")
    }

    private fun mostrarFormularioDocente() {
        setContentView(R.layout.formulario_docente)
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            Toast.makeText(this, "Docente guardado.", Toast.LENGTH_SHORT).show()
            mostrarGestionarDocentes()
        }
        cancelar { mostrarGestionarDocentes() }
    }

    private fun mostrarGestionarAlumnos() {
        setContentView(R.layout.gestionar_alumnos)
        findViewById<Button>(R.id.btnNuevoAlumno).setOnClickListener { mostrarFormularioAlumno() }
        findViewById<Button>(R.id.btnEditarAlumno).setOnClickListener { mostrarFormularioAlumno() }
        findViewById<Button>(R.id.btnEditarAlumno2).setOnClickListener { mostrarFormularioAlumno() }
        findViewById<Button>(R.id.btnTabDocentes).setOnClickListener { mostrarGestionarDocentes() }
        val eliminar: (Button) -> Unit = {
            Toast.makeText(this, "Alumno eliminado.", Toast.LENGTH_SHORT).show()
            it.isEnabled = false
            it.text = "Eliminado"
        }
        findViewById<Button>(R.id.btnEliminarAlumno).setOnClickListener { eliminar(it as Button) }
        findViewById<Button>(R.id.btnEliminarAlumno2).setOnClickListener { eliminar(it as Button) }
        conectarNavegacion("admin")
    }

    private fun mostrarFormularioAlumno() {
        setContentView(R.layout.formulario_alumno)
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            Toast.makeText(this, "Alumno guardado.", Toast.LENGTH_SHORT).show()
            mostrarGestionarAlumnos()
        }
        cancelar { mostrarGestionarAlumnos() }
    }

    private fun mostrarReportes() {
        setContentView(R.layout.reportes)
        findViewById<Button>(R.id.btnGenerarReporte).setOnClickListener {
            findViewById<TextView>(R.id.tvResultadoReporte).text = "Reporte listo: 12 actividades · 248 alumnos"
        }
        findViewById<Button>(R.id.btnExportarPdf).setOnClickListener {
            Toast.makeText(this, "Exportación PDF iniciada.", Toast.LENGTH_SHORT).show()
        }
        conectarNavegacion("admin")
    }
}
