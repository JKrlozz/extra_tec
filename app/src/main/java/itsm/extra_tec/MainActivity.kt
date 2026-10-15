package itsm.extra_tec

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.Normalizer

class MainActivity : AppCompatActivity() {

    private var rol: String = "alumno"
    private var usuario: JSONObject? = null

    // Se incrementa en cada cambio de pantalla: las respuestas tardías de pantallas anteriores se descartan.
    private var pantallaId = 0
    private val ui = Handler(Looper.getMainLooper())

    // Clase seleccionada en el panel del docente (para la barra de navegación)
    private var actividadActualId: Int? = null
    private var actividadActualNombre: String = ""

    // Evidencia
    private var fotoUri: Uri? = null
    private val elegirFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoUri = uri
            findViewById<ImageView?>(R.id.ivVistaPrevia)?.apply {
                setImageURI(uri)
                visibility = View.VISIBLE
            }
        }
    }

    private var ultimoReporte: String = ""
    private var ultimoReporteTitulo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getSharedPreferences("config", MODE_PRIVATE).getString("url", null)?.let { Api.baseUrl = it }
        Api.alExpirarSesion = {
            Toast.makeText(this, "Tu sesión expiró. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            cerrarSesion()
        }
        if (!restaurarSesion()) mostrarInicio()
    }

    // ---------- sesión persistente ----------

    // Archivo excluido de los respaldos en la nube (ver res/xml/backup_rules.xml)
    private val prefsSesion by lazy { getSharedPreferences("sesion", MODE_PRIVATE) }

    private fun guardarSesion(token: String, usuario: JSONObject) {
        prefsSesion.edit().putString("token", token).putString("usuario", usuario.toString()).apply()
    }

    /** Si quedó una sesión guardada, la reabre en la pantalla de inicio de su rol. */
    private fun restaurarSesion(): Boolean {
        val token = prefsSesion.getString("token", null) ?: return false
        val datos = runCatching { JSONObject(prefsSesion.getString("usuario", "")!!) }.getOrNull()
        if (datos == null) {
            prefsSesion.edit().clear().apply()
            return false
        }
        Api.token = token
        usuario = datos
        rol = datos.getString("rol")
        irAlInicioDelRol()
        return true
    }

    private fun irAlInicioDelRol() = when (rol) {
        "alumno" -> mostrarInicioAlumno()
        "docente" -> mostrarInicioDocente()
        else -> mostrarInicioAdmin()
    }

    // Respeta las barras del sistema (modo edge-to-edge); el fondo azul de la ventana las rellena.
    override fun setContentView(layoutResID: Int) {
        pantallaId++
        super.setContentView(layoutResID)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }

    // ---------- utilidades ----------

    private fun aviso(mensaje: String) = Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

    /** Texto de un campo JSON; cadena vacía si falta o es null (org.json devuelve "null" con optString). */
    private fun JSONObject.txt(clave: String): String = if (isNull(clave)) "" else get(clave).toString()

    private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun EditText.texto(): String = text.toString().trim()

    private fun campo(id: Int): String = findViewById<EditText>(id).texto()

    /** Llama a la API y ejecuta el resultado solo si el usuario sigue en la misma pantalla. */
    private fun llamar(
        metodo: String,
        ruta: String,
        cuerpo: JSONObject? = null,
        alExito: (Any?) -> Unit,
    ) {
        val pantalla = pantallaId
        Api.pedir(metodo, ruta, cuerpo,
            alExito = { if (pantalla == pantallaId) alExito(it) },
            alError = { if (pantalla == pantallaId) aviso(it) })
    }

    private fun mostrarVacio(texto: String?) {
        findViewById<TextView?>(R.id.tvVacio)?.apply {
            if (texto == null) visibility = View.GONE else { text = texto; visibility = View.VISIBLE }
        }
    }

    private class Accion(val texto: String, val activa: Boolean = true, val alPulsar: () -> Unit)

    /** Añade una tarjeta (item_tarjeta) al contenedor. */
    private fun tarjeta(
        contenedor: ViewGroup,
        titulo: String,
        linea1: String? = null,
        linea2: String? = null,
        progreso: Int? = null,
        a: Accion? = null,
        b: Accion? = null,
        c: Accion? = null,
    ) {
        val v = LayoutInflater.from(this).inflate(R.layout.item_tarjeta, contenedor, false)
        v.findViewById<TextView>(R.id.tvTitulo).text = titulo
        fun linea(id: Int, t: String?) = v.findViewById<TextView>(id).apply {
            if (t.isNullOrBlank()) visibility = View.GONE else { text = t; visibility = View.VISIBLE }
        }
        linea(R.id.tvLinea1, linea1)
        linea(R.id.tvLinea2, linea2)
        if (progreso != null) v.findViewById<ProgressBar>(R.id.pbProgreso).apply {
            this.progress = progreso
            visibility = View.VISIBLE
        }
        fun boton(id: Int, acc: Accion?) = v.findViewById<MaterialButton>(id).apply {
            if (acc == null) visibility = View.GONE else {
                text = acc.texto
                isEnabled = acc.activa
                visibility = View.VISIBLE
                setOnClickListener { acc.alPulsar() }
            }
        }
        boton(R.id.btnA, a); boton(R.id.btnB, b); boton(R.id.btnC, c)
        if (a != null || b != null || c != null) v.findViewById<View>(R.id.filaBotones).visibility = View.VISIBLE
        contenedor.addView(v)
    }

    private fun confirmar(mensaje: String, alAceptar: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(mensaje)
            .setPositiveButton("Sí") { _, _ -> alAceptar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun <T> llenarSpinner(spinner: Spinner, elementos: List<T>, etiqueta: (T) -> String) {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, elementos.map(etiqueta))
    }

    private fun hora(valor: String) = valor.take(5)

    private fun conectarNavegacion(rolDestino: String) {
        val destinos: List<() -> Unit> = when (rolDestino) {
            "alumno" -> listOf(::mostrarInicioAlumno, ::mostrarActividadesAlumno, ::mostrarHorarioAlumno, ::mostrarConstancia)
            "docente" -> listOf(::mostrarInicioDocente, ::navegarAsistencia, ::mostrarEvidencia)
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

    private fun cerrarSesion() {
        prefsSesion.edit().clear().apply()
        Api.token = null
        usuario = null
        actividadActualId = null
        mostrarInicioSesion()
    }

    // ---------- INICIO Y SESIÓN ----------

    private fun mostrarInicio() {
        setContentView(R.layout.inicio)
        findViewById<Button>(R.id.btnEntrar).setOnClickListener { mostrarInicioSesion() }
    }

    private fun mostrarInicioSesion() {
        setContentView(R.layout.inicio_sesion)
        val spinner = findViewById<Spinner>(R.id.spinnerRol)
        llenarSpinner(spinner, listOf("alumno", "docente", "admin")) { it }
        findViewById<Button>(R.id.btnIniciarSesion).setOnClickListener { boton ->
            val correo = campo(R.id.etCorreo)
            val clave = findViewById<EditText>(R.id.etContrasena).text.toString()
            if (correo.isEmpty() || clave.isEmpty()) return@setOnClickListener aviso("Escribe tu correo y contraseña.")
            boton.isEnabled = false
            val cuerpo = JSONObject()
                .put("correo", correo).put("contrasena", clave).put("rol", spinner.selectedItem.toString())
            val pantalla = pantallaId
            Api.pedir("POST", "/login", cuerpo,
                alExito = { r ->
                    val json = r as JSONObject
                    Api.token = json.getString("token")
                    usuario = json.getJSONObject("usuario")
                    rol = usuario!!.getString("rol")
                    guardarSesion(Api.token!!, usuario!!)
                    irAlInicioDelRol()
                },
                alError = {
                    aviso(it)
                    if (pantalla == pantallaId) boton.isEnabled = true
                })
        }
        findViewById<Button>(R.id.btnIrRegistro).setOnClickListener { mostrarRegistro() }
        findViewById<Button>(R.id.btnServidor).setOnClickListener { configurarServidor() }
    }

    private fun configurarServidor() {
        val entrada = EditText(this).apply {
            setText(Api.baseUrl)
            hint = "http://192.168.1.10:3000"
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Dirección del servidor")
            .setMessage("Emulador: ${Api.URL_EMULADOR}\nTeléfono real: la IP de tu PC, por ejemplo http://192.168.1.10:3000")
            .setView(entrada)
            .setPositiveButton("Guardar") { _, _ ->
                val url = entrada.texto().ifEmpty { Api.URL_EMULADOR }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    aviso("La dirección debe empezar con http://")
                } else {
                    Api.baseUrl = url
                    getSharedPreferences("config", MODE_PRIVATE).edit().putString("url", url).apply()
                    aviso("Servidor guardado: $url")
                }
            }
            .setNeutralButton("Probar") { _, _ ->
                Api.baseUrl = entrada.texto().ifEmpty { Api.URL_EMULADOR }
                Api.pedir("GET", "/salud", null, { aviso("Conexión correcta con ${Api.baseUrl}") }, { aviso(it) })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarRegistro() {
        setContentView(R.layout.registro)
        val spinner = findViewById<Spinner>(R.id.spinnerCarrera)
        var carreras: List<JSONObject> = emptyList()
        llamar("GET", "/carreras") { r ->
            carreras = (r as JSONArray).objetos()
            llenarSpinner(spinner, carreras) { it.getString("nombre") }
        }
        findViewById<Button>(R.id.btnRegistrarse).setOnClickListener {
            val semestre = campo(R.id.etSemestre).toIntOrNull()
            when {
                campo(R.id.etNombre).isEmpty() || campo(R.id.etCorreo).isEmpty() || campo(R.id.etMatricula).isEmpty() ->
                    return@setOnClickListener aviso("Completa nombre, correo y matrícula.")
                findViewById<EditText>(R.id.etContrasena).text.length < 8 ->
                    return@setOnClickListener aviso("La contraseña debe tener al menos 8 caracteres.")
                semestre == null || semestre !in 1..12 -> return@setOnClickListener aviso("El semestre debe ser de 1 a 12.")
                carreras.isEmpty() -> return@setOnClickListener aviso("Aún no se cargan las carreras.")
            }
            val cuerpo = JSONObject()
                .put("nombre", campo(R.id.etNombre))
                .put("correo", campo(R.id.etCorreo))
                .put("contrasena", findViewById<EditText>(R.id.etContrasena).text.toString())
                .put("matricula", campo(R.id.etMatricula))
                .put("id_carrera", carreras[spinner.selectedItemPosition].getInt("id"))
                .put("semestre", semestre)
            llamar("POST", "/registro", cuerpo) {
                aviso("Registro realizado. Ya puedes iniciar sesión.")
                mostrarInicioSesion()
            }
        }
        cancelar { mostrarInicioSesion() }
    }

    // ---------- ALUMNO ----------

    private fun mostrarInicioAlumno() {
        setContentView(R.layout.inicio_alumno)
        findViewById<TextView>(R.id.tvNombre).text = usuario?.txt("nombre")
        findViewById<TextView>(R.id.tvDetalle).text = "Alumno · " + usuario?.txt("correo")
        findViewById<Button>(R.id.btnIrActividades).setOnClickListener { mostrarActividadesAlumno() }
        findViewById<Button>(R.id.btnIrHorario).setOnClickListener { mostrarHorarioAlumno() }
        findViewById<Button>(R.id.btnIrConstancia).setOnClickListener { mostrarConstancia() }
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { cerrarSesion() }
        conectarNavegacion("alumno")
    }

    private fun mostrarActividadesAlumno() {
        setContentView(R.layout.actividades_alumno)
        conectarNavegacion("alumno")
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        mostrarVacio("Cargando…")
        llamar("GET", "/actividades") { r ->
            val actividades = (r as JSONArray).objetos()
            mostrarVacio(if (actividades.isEmpty()) "No hay actividades disponibles." else null)
            lista.removeAllViews()
            for (a in actividades) {
                val id = a.getInt("id")
                val lleno = a.getInt("inscritos") >= a.getInt("cupo")
                val docente = a.txt("docente").ifEmpty { "Por asignar" }
                val cupo = "Cupo ${a.getInt("inscritos")}/${a.getInt("cupo")}"
                if (a.getBoolean("inscrito")) {
                    tarjeta(lista, a.txt("nombre"), "${a.txt("horario")} · Prof. $docente", "Inscrito · $cupo",
                        c = Accion("Darme de baja") {
                            confirmar("¿Darte de baja de ${a.txt("nombre")}?") {
                                llamar("DELETE", "/inscripciones/$id") {
                                    aviso("Baja realizada.")
                                    mostrarActividadesAlumno()
                                }
                            }
                        })
                } else {
                    tarjeta(lista, a.txt("nombre"), "${a.txt("horario")} · Prof. $docente", cupo,
                        a = Accion(if (lleno) "Cupo lleno" else "Inscribirme", !lleno) {
                            llamar("POST", "/inscripciones", JSONObject().put("id_actividad", id)) {
                                aviso("Inscripción realizada.")
                                mostrarActividadesAlumno()
                            }
                        })
                }
            }
        }
    }

    private fun mostrarHorarioAlumno() {
        setContentView(R.layout.horario_alumno)
        conectarNavegacion("alumno")
        findViewById<Button>(R.id.btnAgregarActividad).setOnClickListener { mostrarActividadesAlumno() }
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        mostrarVacio("Cargando…")
        llamar("GET", "/mi-horario") { r ->
            val filas = (r as JSONArray).objetos()
            mostrarVacio(if (filas.isEmpty()) "Aún no estás inscrito en ninguna actividad." else null)
            lista.removeAllViews()
            // Una tarjeta por actividad con todos sus días
            for ((_, grupo) in filas.groupBy { it.getInt("id_actividad") }) {
                val dias = grupo.joinToString("\n") {
                    "${it.getString("dia_semana")} · ${hora(it.getString("hora_inicio"))}–${hora(it.getString("hora_fin"))}"
                }
                tarjeta(lista, grupo[0].getString("nombre"), dias, grupo[0].txt("lugar"))
            }
        }
    }

    private fun mostrarConstancia() {
        setContentView(R.layout.constancia)
        conectarNavegacion("alumno")
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        mostrarVacio("Cargando…")
        llamar("GET", "/constancias") { r ->
            val filas = (r as JSONArray).objetos()
            mostrarVacio(if (filas.isEmpty()) "Aún no estás inscrito en ninguna actividad." else null)
            lista.removeAllViews()
            for (c in filas) {
                val pct = c.getDouble("porcentaje")
                val disponible = c.getBoolean("disponible")
                tarjeta(lista, c.getString("nombre"),
                    "${c.getString("periodo")} · " + if (disponible) "Constancia disponible" else "Constancia no disponible (se requiere 80 %)",
                    "Asistencia: ${pct.toInt()} % (${c.getInt("asistencias")} de ${c.getInt("sesiones")} sesiones)",
                    progreso = pct.toInt(),
                    a = if (disponible) Accion("Descargar PDF") { descargarConstancia(c.getInt("id_actividad")) } else null)
            }
        }
    }

    private fun descargarConstancia(idActividad: Int) {
        val pantalla = pantallaId
        Api.descargar("/constancias/$idActividad/pdf",
            alExito = { bytes, nombre ->
                runCatching { Archivos.guardarEnDescargas(this, nombre, "application/pdf", bytes) }
                    .onSuccess { aviso("Constancia guardada en $it") }
                    .onFailure { aviso("No se pudo guardar el PDF: ${it.message}") }
            },
            alError = { if (pantalla == pantallaId) aviso(it) else aviso(it) })
    }

    // ---------- DOCENTE ----------

    private fun mostrarInicioDocente() {
        setContentView(R.layout.inicio_docente)
        conectarNavegacion("docente")
        findViewById<TextView>(R.id.tvNombre).text = "Prof. " + usuario?.txt("nombre")
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { cerrarSesion() }
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        mostrarVacio("Cargando…")
        llamar("GET", "/mis-actividades") { r ->
            val clases = (r as JSONArray).objetos()
            mostrarVacio(if (clases.isEmpty()) "Aún no tienes clases asignadas." else null)
            lista.removeAllViews()
            for (c in clases) {
                val id = c.getInt("id")
                val nombre = c.getString("nombre")
                tarjeta(lista, nombre, "${c.txt("horario")} · ${c.getInt("inscritos")} alumnos",
                    a = Accion("Pase de lista") { mostrarAsistencia(id, nombre) },
                    b = Accion("Ver alumnos") { mostrarAlumnosDocente(id, nombre) })
            }
        }
    }

    private fun navegarAsistencia() {
        val id = actividadActualId
        if (id == null) {
            aviso("Elige una clase y pulsa \"Pase de lista\".")
            mostrarInicioDocente()
        } else mostrarAsistencia(id, actividadActualNombre)
    }

    private fun mostrarAsistencia(idActividad: Int, nombre: String) {
        actividadActualId = idActividad
        actividadActualNombre = nombre
        setContentView(R.layout.asistencia)
        conectarNavegacion("docente")
        cancelar { mostrarInicioDocente() }
        val contenedor = findViewById<LinearLayout>(R.id.listaContenido)
        val marcas = mutableListOf<Pair<Int, CheckBox>>()
        var fecha = ""
        mostrarVacio("Cargando…")
        llamar("GET", "/asistencia/$idActividad") { r ->
            val json = r as JSONObject
            fecha = json.getString("fecha")
            findViewById<TextView>(R.id.tvSubtitulo).text = "$nombre · $fecha"
            val alumnos = json.getJSONArray("alumnos").objetos()
            mostrarVacio(if (alumnos.isEmpty()) "No hay alumnos inscritos." else null)
            for (al in alumnos) {
                val caja = CheckBox(this).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    buttonTintList = ContextCompat.getColorStateList(context, R.color.itsm_primary)
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    text = "${al.getString("nombre")} · Matrícula ${al.getString("matricula")}"
                    setTextColor(ContextCompat.getColor(context, R.color.itsm_text))
                    isChecked = al.getBoolean("presente")
                }
                contenedor.addView(caja)
                marcas.add(al.getInt("id") to caja)
            }
        }
        findViewById<Button>(R.id.btnGuardarAsistencia).setOnClickListener {
            if (marcas.isEmpty()) return@setOnClickListener aviso("No hay alumnos para guardar.")
            val lista = JSONArray()
            marcas.forEach { (id, caja) -> lista.put(JSONObject().put("id_alumno", id).put("presente", caja.isChecked)) }
            val cuerpo = JSONObject().put("id_actividad", idActividad).put("fecha", fecha).put("asistencias", lista)
            llamar("POST", "/asistencia", cuerpo) {
                aviso("Asistencia guardada.")
                mostrarInicioDocente()
            }
        }
    }

    private fun mostrarAlumnosDocente(idActividad: Int, nombre: String) {
        actividadActualId = idActividad
        actividadActualNombre = nombre
        setContentView(R.layout.alumnos_docente)
        conectarNavegacion("docente")
        cancelar { mostrarInicioDocente() }
        findViewById<TextView>(R.id.tvSubtitulo).text = nombre
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        var alumnos: List<JSONObject> = emptyList()
        fun pintar(filtro: String) {
            lista.removeAllViews()
            val f = filtro.trim().lowercase()
            val visibles = alumnos.filter { f.isEmpty() || it.getString("nombre").lowercase().contains(f) || it.getString("matricula").contains(f) }
            mostrarVacio(if (visibles.isEmpty()) "Sin resultados." else null)
            visibles.forEach { tarjeta(lista, it.getString("nombre"), "Matrícula ${it.getString("matricula")}") }
        }
        mostrarVacio("Cargando…")
        llamar("GET", "/actividades/$idActividad/alumnos") { r ->
            alumnos = (r as JSONArray).objetos()
            pintar(campo(R.id.etBuscar))
        }
        alEscribir(R.id.etBuscar) { pintar(it) }
    }

    private fun alEscribir(id: Int, accion: (String) -> Unit) {
        findViewById<EditText>(id).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = accion(s.toString())
        })
    }

    private fun mostrarEvidencia() {
        fotoUri = null
        setContentView(R.layout.evidencia)
        conectarNavegacion("docente")
        val spinner = findViewById<Spinner>(R.id.spinnerActividad)
        var clases: List<JSONObject> = emptyList()
        findViewById<TextView>(R.id.tvSubtitulo).text = "Selecciona la actividad y la fotografía"
        llamar("GET", "/mis-actividades") { r ->
            clases = (r as JSONArray).objetos()
            llenarSpinner(spinner, clases) { it.getString("nombre") }
            val actual = clases.indexOfFirst { it.getInt("id") == actividadActualId }
            if (actual >= 0) spinner.setSelection(actual)
        }
        findViewById<Button>(R.id.btnElegirFoto).setOnClickListener { elegirFoto.launch("image/*") }
        findViewById<Button>(R.id.btnGuardarEvidencia).setOnClickListener {
            val uri = fotoUri
            if (clases.isEmpty()) return@setOnClickListener aviso("No tienes clases asignadas.")
            if (uri == null) return@setOnClickListener aviso("Selecciona una fotografía.")
            val mime = contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = runCatching { contentResolver.openInputStream(uri)!!.use { it.readBytes() } }.getOrNull()
                ?: return@setOnClickListener aviso("No se pudo leer la fotografía.")
            if (bytes.size > 8 * 1024 * 1024) return@setOnClickListener aviso("La fotografía supera 8 MB.")
            val extension = if (mime.contains("png")) "png" else if (mime.contains("webp")) "webp" else "jpg"
            val pantalla = pantallaId
            Api.subirArchivo("/evidencias",
                mapOf("id_actividad" to clases[spinner.selectedItemPosition].getInt("id").toString(),
                    "descripcion" to campo(R.id.etDescripcion)),
                "foto", "evidencia.$extension", mime, bytes,
                alExito = {
                    aviso("Evidencia guardada.")
                    if (pantalla == pantallaId) mostrarInicioDocente()
                },
                alError = { aviso(it) })
        }
    }

    // ---------- ADMINISTRADOR ----------

    private fun mostrarInicioAdmin() {
        setContentView(R.layout.inicio_admin)
        findViewById<Button>(R.id.btnIrActividades).setOnClickListener { mostrarGestionarActividades() }
        findViewById<Button>(R.id.btnIrDocentes).setOnClickListener { mostrarGestionarDocentes() }
        findViewById<Button>(R.id.btnIrAlumnos).setOnClickListener { mostrarGestionarAlumnos() }
        findViewById<Button>(R.id.btnIrReportes).setOnClickListener { mostrarReportes() }
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { cerrarSesion() }
        conectarNavegacion("admin")
    }

    private val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

    private fun sinAcentos(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

    /** "Lunes 16:00-18:00, Miércoles 16:00-18:00" -> arreglo de horarios para la API; null si es inválido. */
    private fun parsearHorarios(texto: String): JSONArray? {
        val patron = Regex("([\\p{L}]+)\\s+(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})")
        val salida = JSONArray()
        for (m in patron.findAll(texto)) {
            val dia = dias.firstOrNull { sinAcentos(it) == sinAcentos(m.groupValues[1]) } ?: return null
            fun normal(h: String) = h.padStart(5, '0')
            val ini = normal(m.groupValues[2]); val fin = normal(m.groupValues[3])
            if (fin <= ini) return null
            salida.put(JSONObject().put("dia_semana", dia).put("hora_inicio", ini).put("hora_fin", fin))
        }
        return if (salida.length() == 0) null else salida
    }

    // --- Actividades ---

    private fun mostrarGestionarActividades() {
        setContentView(R.layout.gestionar_actividades)
        conectarNavegacion("admin")
        findViewById<Button>(R.id.btnNuevaActividad).setOnClickListener { mostrarFormularioActividad(null) }
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        mostrarVacio("Cargando…")
        llamar("GET", "/actividades") { r ->
            val actividades = (r as JSONArray).objetos()
            mostrarVacio(if (actividades.isEmpty()) "No hay actividades registradas." else null)
            lista.removeAllViews()
            for (a in actividades) {
                val id = a.getInt("id")
                tarjeta(lista, a.txt("nombre"),
                    "${a.txt("horario")} · Prof. ${a.txt("docente").ifEmpty { "Por asignar" }}",
                    "Cupo ${a.getInt("inscritos")}/${a.getInt("cupo")}",
                    b = Accion("Editar") { mostrarFormularioActividad(id) },
                    c = Accion("Eliminar") {
                        confirmar("¿Eliminar la actividad ${a.txt("nombre")}? Se conserva su historial.") {
                            llamar("DELETE", "/actividades/$id") {
                                aviso("Actividad eliminada.")
                                mostrarGestionarActividades()
                            }
                        }
                    })
            }
        }
    }

    private fun mostrarFormularioActividad(id: Int?) {
        setContentView(R.layout.formulario_actividad)
        findViewById<TextView>(R.id.tvTituloForm).text = if (id == null) "Nueva actividad" else "Editar actividad"
        cancelar { mostrarGestionarActividades() }
        val spinner = findViewById<Spinner>(R.id.spinnerDocente)
        var docentes: List<JSONObject> = emptyList()
        llamar("GET", "/docentes") { r ->
            docentes = (r as JSONArray).objetos()
            llenarSpinner(spinner, docentes) { it.getString("nombre") }
            if (id != null) llamar("GET", "/actividades/$id") { d ->
                val a = d as JSONObject
                findViewById<EditText>(R.id.etNombre).setText(a.txt("nombre"))
                findViewById<EditText>(R.id.etDescripcion).setText(a.txt("descripcion"))
                findViewById<EditText>(R.id.etCupo).setText(a.txt("cupo"))
                findViewById<EditText>(R.id.etHorario).setText(
                    a.getJSONArray("horarios").objetos().joinToString(", ") {
                        "${it.getString("dia_semana")} ${hora(it.getString("hora_inicio"))}-${hora(it.getString("hora_fin"))}"
                    })
                val pos = docentes.indexOfFirst { it.getInt("id") == a.optInt("id_docente", -1) }
                if (pos >= 0) spinner.setSelection(pos)
            }
        }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            val cupo = campo(R.id.etCupo).toIntOrNull()
            val horarios = parsearHorarios(campo(R.id.etHorario))
            when {
                campo(R.id.etNombre).isEmpty() -> return@setOnClickListener aviso("Escribe el nombre.")
                cupo == null || cupo <= 0 -> return@setOnClickListener aviso("El cupo debe ser un número mayor a 0.")
                horarios == null -> return@setOnClickListener aviso("Horario inválido. Ejemplo: Lunes 16:00-18:00, Miércoles 16:00-18:00")
                docentes.isEmpty() -> return@setOnClickListener aviso("Primero registra un docente.")
            }
            val cuerpo = JSONObject()
                .put("nombre", campo(R.id.etNombre))
                .put("descripcion", campo(R.id.etDescripcion))
                .put("cupo", cupo)
                .put("id_docente", docentes[spinner.selectedItemPosition].getInt("id"))
                .put("horarios", horarios)
            llamar(if (id == null) "POST" else "PUT", if (id == null) "/actividades" else "/actividades/$id", cuerpo) {
                aviso("Actividad guardada.")
                mostrarGestionarActividades()
            }
        }
    }

    // --- Docentes ---

    private fun mostrarGestionarDocentes() {
        setContentView(R.layout.gestionar_docentes)
        conectarNavegacion("admin")
        findViewById<Button>(R.id.btnNuevoDocente).setOnClickListener { mostrarFormularioDocente(null) }
        findViewById<Button>(R.id.btnTabAlumnos).setOnClickListener { mostrarGestionarAlumnos() }
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        var docentes: List<JSONObject> = emptyList()
        fun pintar(filtro: String) {
            lista.removeAllViews()
            val f = filtro.trim().lowercase()
            val visibles = docentes.filter { f.isEmpty() || it.getString("nombre").lowercase().contains(f) }
            mostrarVacio(if (visibles.isEmpty()) "No hay docentes." else null)
            for (d in visibles) {
                tarjeta(lista, d.getString("nombre"),
                    listOf(d.txt("especialidad"), d.txt("telefono")).filter { it.isNotEmpty() }.joinToString(" · "),
                    d.txt("correo"),
                    b = Accion("Editar") { mostrarFormularioDocente(d) },
                    c = Accion("Eliminar") {
                        confirmar("¿Eliminar a ${d.getString("nombre")}?") {
                            llamar("DELETE", "/docentes/${d.getInt("id")}") {
                                aviso("Docente eliminado.")
                                mostrarGestionarDocentes()
                            }
                        }
                    })
            }
        }
        mostrarVacio("Cargando…")
        llamar("GET", "/docentes") { r ->
            docentes = (r as JSONArray).objetos()
            pintar(campo(R.id.etBuscar))
        }
        alEscribir(R.id.etBuscar) { pintar(it) }
    }

    /** Muestra la contraseña generada por el servidor (solo se entrega una vez). */
    private fun mostrarClaveInicial(respuesta: Any?, alCerrar: () -> Unit) {
        val clave = (respuesta as? JSONObject)?.takeIf { !it.isNull("contrasena_inicial") }?.getString("contrasena_inicial")
        if (clave == null) return alCerrar()
        AlertDialog.Builder(this)
            .setTitle("Contraseña inicial")
            .setMessage("Anótala y entrégala al usuario; no se volverá a mostrar:\n\n$clave")
            .setPositiveButton("Entendido") { _, _ -> alCerrar() }
            .setCancelable(false)
            .show()
    }

    private fun mostrarFormularioDocente(docente: JSONObject?) {
        setContentView(R.layout.formulario_docente)
        findViewById<TextView>(R.id.tvTituloForm).text = if (docente == null) "Registrar docente" else "Editar docente"
        cancelar { mostrarGestionarDocentes() }
        if (docente != null) {
            findViewById<EditText>(R.id.etNombre).setText(docente.txt("nombre"))
            findViewById<EditText>(R.id.etCorreo).setText(docente.txt("correo"))
            findViewById<EditText>(R.id.etEspecialidad).setText(docente.txt("especialidad"))
            findViewById<EditText>(R.id.etTelefono).setText(docente.txt("telefono"))
            findViewById<View>(R.id.tilContrasena).visibility = View.GONE
        }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            if (campo(R.id.etNombre).isEmpty() || campo(R.id.etCorreo).isEmpty()) {
                return@setOnClickListener aviso("Escribe nombre y correo.")
            }
            val cuerpo = JSONObject()
                .put("nombre", campo(R.id.etNombre)).put("correo", campo(R.id.etCorreo))
                .put("especialidad", campo(R.id.etEspecialidad)).put("telefono", campo(R.id.etTelefono))
            if (docente == null && campo(R.id.etContrasena).isNotEmpty()) {
                cuerpo.put("contrasena", findViewById<EditText>(R.id.etContrasena).text.toString())
            }
            val metodo = if (docente == null) "POST" else "PUT"
            val ruta = if (docente == null) "/docentes" else "/docentes/${docente.getInt("id")}"
            llamar(metodo, ruta, cuerpo) { r ->
                aviso("Docente guardado.")
                mostrarClaveInicial(r) { mostrarGestionarDocentes() }
            }
        }
    }

    // --- Alumnos ---

    private fun mostrarGestionarAlumnos() {
        setContentView(R.layout.gestionar_alumnos)
        conectarNavegacion("admin")
        findViewById<Button>(R.id.btnNuevoAlumno).setOnClickListener { mostrarFormularioAlumno(null) }
        findViewById<Button>(R.id.btnTabDocentes).setOnClickListener { mostrarGestionarDocentes() }
        val lista = findViewById<LinearLayout>(R.id.listaContenido)
        val pantalla = pantallaId
        fun cargar(filtro: String) {
            llamar("GET", "/alumnos?q=" + URLEncoder.encode(filtro.trim(), "UTF-8")) { r ->
                val alumnos = (r as JSONArray).objetos()
                mostrarVacio(if (alumnos.isEmpty()) "No hay alumnos." else null)
                lista.removeAllViews()
                for (al in alumnos) {
                    tarjeta(lista, al.getString("nombre"),
                        "Matrícula ${al.getString("matricula")} · ${al.getString("carrera")} · ${al.getInt("semestre")}° semestre",
                        al.txt("correo"),
                        b = Accion("Editar") { mostrarFormularioAlumno(al) },
                        c = Accion("Eliminar") {
                            confirmar("¿Eliminar a ${al.getString("nombre")}?") {
                                llamar("DELETE", "/alumnos/${al.getInt("id")}") {
                                    aviso("Alumno eliminado.")
                                    mostrarGestionarAlumnos()
                                }
                            }
                        })
                }
            }
        }
        mostrarVacio("Cargando…")
        cargar("")
        // Espera 300 ms tras la última tecla para no consultar en cada letra
        var pendiente: Runnable? = null
        alEscribir(R.id.etBuscar) { texto ->
            pendiente?.let { ui.removeCallbacks(it) }
            pendiente = Runnable { if (pantalla == pantallaId) cargar(texto) }.also { ui.postDelayed(it, 300) }
        }
    }

    private fun mostrarFormularioAlumno(alumno: JSONObject?) {
        setContentView(R.layout.formulario_alumno)
        findViewById<TextView>(R.id.tvTituloForm).text = if (alumno == null) "Agregar alumno" else "Editar alumno"
        cancelar { mostrarGestionarAlumnos() }
        val spinner = findViewById<Spinner>(R.id.spinnerCarrera)
        var carreras: List<JSONObject> = emptyList()
        llamar("GET", "/carreras") { r ->
            carreras = (r as JSONArray).objetos()
            llenarSpinner(spinner, carreras) { it.getString("nombre") }
            val pos = carreras.indexOfFirst { it.getInt("id") == alumno?.optInt("id_carrera", -1) }
            if (pos >= 0) spinner.setSelection(pos)
        }
        if (alumno != null) {
            findViewById<EditText>(R.id.etNombre).setText(alumno.txt("nombre"))
            findViewById<EditText>(R.id.etCorreo).setText(alumno.txt("correo"))
            findViewById<EditText>(R.id.etMatricula).setText(alumno.txt("matricula"))
            findViewById<EditText>(R.id.etSemestre).setText(alumno.txt("semestre"))
            findViewById<View>(R.id.tilContrasena).visibility = View.GONE
        }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            val semestre = campo(R.id.etSemestre).toIntOrNull()
            when {
                campo(R.id.etNombre).isEmpty() || campo(R.id.etCorreo).isEmpty() || campo(R.id.etMatricula).isEmpty() ->
                    return@setOnClickListener aviso("Completa nombre, correo y matrícula.")
                semestre == null || semestre !in 1..12 -> return@setOnClickListener aviso("El semestre debe ser de 1 a 12.")
                carreras.isEmpty() -> return@setOnClickListener aviso("Aún no se cargan las carreras.")
            }
            val cuerpo = JSONObject()
                .put("nombre", campo(R.id.etNombre)).put("correo", campo(R.id.etCorreo))
                .put("matricula", campo(R.id.etMatricula)).put("semestre", semestre)
                .put("id_carrera", carreras[spinner.selectedItemPosition].getInt("id"))
            if (alumno == null && campo(R.id.etContrasena).isNotEmpty()) {
                cuerpo.put("contrasena", findViewById<EditText>(R.id.etContrasena).text.toString())
            }
            val metodo = if (alumno == null) "POST" else "PUT"
            val ruta = if (alumno == null) "/alumnos" else "/alumnos/${alumno.getInt("id")}"
            llamar(metodo, ruta, cuerpo) { r ->
                aviso("Alumno guardado.")
                mostrarClaveInicial(r) { mostrarGestionarAlumnos() }
            }
        }
    }

    // --- Reportes ---

    private fun mostrarReportes() {
        setContentView(R.layout.reportes)
        conectarNavegacion("admin")
        val tipos = listOf("actividad" to "Por actividad", "alumno" to "Por alumno", "docente" to "Por docente")
        val spinner = findViewById<Spinner>(R.id.spinnerTipo)
        llenarSpinner(spinner, tipos) { it.second }
        val salida = findViewById<TextView>(R.id.tvResultadoReporte)
        if (ultimoReporte.isNotEmpty()) salida.text = ultimoReporte

        findViewById<Button>(R.id.btnGenerarReporte).setOnClickListener {
            val fecha = Regex("\\d{4}-\\d{2}-\\d{2}")
            val desde = campo(R.id.etDesde)
            val hasta = campo(R.id.etHasta)
            if ((desde.isNotEmpty() && !fecha.matches(desde)) || (hasta.isNotEmpty() && !fecha.matches(hasta))) {
                return@setOnClickListener aviso("Las fechas deben tener el formato AAAA-MM-DD.")
            }
            val tipo = tipos[spinner.selectedItemPosition]
            var ruta = "/reportes?tipo=${tipo.first}"
            if (desde.isNotEmpty()) ruta += "&desde=$desde"
            if (hasta.isNotEmpty()) ruta += "&hasta=$hasta"
            llamar("GET", ruta) { r ->
                val json = r as JSONObject
                val filas = json.getJSONArray("filas").objetos()
                val pct = { f: JSONObject -> f.txt("asistencia_pct").let { if (it.isEmpty()) "sin datos" else "$it %" } }
                val lineas = filas.map { f ->
                    when (tipo.first) {
                        "actividad" -> "• ${f.txt("actividad")} (${f.txt("docente").ifEmpty { "sin docente" }}): " +
                            "${f.txt("inscritos")}/${f.txt("cupo")} inscritos, ${f.txt("sesiones")} sesiones, asistencia ${pct(f)}"
                        "alumno" -> "• ${f.txt("alumno")} (${f.txt("matricula")}): ${f.txt("actividades")} actividades, asistencia ${pct(f)}"
                        else -> "• ${f.txt("docente")}: ${f.txt("actividades")} actividades, ${f.txt("sesiones_impartidas")} sesiones, ${f.txt("evidencias")} evidencias"
                    }
                }
                val rango = if (desde.isEmpty() && hasta.isEmpty()) "todo el historial" else "${desde.ifEmpty { "inicio" }} a ${hasta.ifEmpty { "hoy" }}"
                ultimoReporteTitulo = "Reporte ${tipo.second.lowercase()} ($rango)"
                ultimoReporte = "$ultimoReporteTitulo\n${filas.size} registros\n\n" + lineas.joinToString("\n")
                salida.text = ultimoReporte
            }
        }
        findViewById<Button>(R.id.btnExportarPdf).setOnClickListener {
            if (ultimoReporte.isEmpty()) return@setOnClickListener aviso("Primero genera un reporte.")
            runCatching {
                val pdf = Archivos.textoAPdf("Actividades extraescolares ITSM", ultimoReporte)
                Archivos.guardarEnDescargas(this, "reporte-${System.currentTimeMillis() / 1000}.pdf", "application/pdf", pdf)
            }.onSuccess { aviso("Reporte guardado en $it") }
                .onFailure { aviso("No se pudo exportar: ${it.message}") }
        }
    }
}
