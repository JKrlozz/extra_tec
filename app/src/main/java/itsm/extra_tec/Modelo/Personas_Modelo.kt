package itsm.extra_tec.Modelo

import android.content.ContentValues

object Personas_Modelo {

    fun alumnos(filtro: String, alTerminar: (Resultado_Modelo<List<Alumno>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val patron = "%${filtro.trim()}%"
            db.rawQuery(
                """SELECT u.id, u.nombres, u.apellido_paterno, u.apellido_materno, u.correo,
                          a.matricula, a.semestre, c.id AS id_carrera, c.nombre AS carrera
                     FROM alumnos a
                     JOIN usuarios u ON u.id = a.id_usuario
                     JOIN carreras c ON c.id = a.id_carrera
                    WHERE u.activo = 1
                      AND ((u.nombres || ' ' || u.apellido_paterno || ' ' || COALESCE(u.apellido_materno, '')) LIKE ?
                           OR a.matricula LIKE ?)
                    ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(patron, patron),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        Alumno(
                            c.entero("id"), c.texto("nombres"), c.texto("apellido_paterno"),
                            c.texto("apellido_materno"), c.texto("correo"), c.texto("matricula"),
                            c.entero("semestre"), c.entero("id_carrera"), c.texto("carrera"),
                        )
                    )
                }
            }
        }, alTerminar)
    }

    fun docentes(
        filtro: String = "",
        alTerminar: (Resultado_Modelo<List<Docente>>) -> Unit,
    ) {
        BaseDatos.consultar({ db ->
            val patron = "%${filtro.trim()}%"
            db.rawQuery(
                """SELECT u.id, u.nombres, u.apellido_paterno, u.apellido_materno, u.correo,
                          COALESCE(d.especialidad, '') AS especialidad, COALESCE(d.telefono, '') AS telefono
                     FROM docentes d JOIN usuarios u ON u.id = d.id_usuario
                    WHERE u.activo = 1
                      AND ((u.nombres || ' ' || u.apellido_paterno || ' ' || COALESCE(u.apellido_materno, '')) LIKE ?
                           OR u.correo LIKE ?)
                    ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(patron, patron),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        Docente(
                            c.entero("id"), c.texto("nombres"), c.texto("apellido_paterno"),
                            c.texto("apellido_materno"), c.texto("correo"),
                            c.texto("especialidad"), c.texto("telefono"),
                        )
                    )
                }
            }
        }, alTerminar)
    }

    // Devuelve la contrasena inicial si se genero
    fun guardarAlumno(
        id: Int?,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        matricula: String,
        idCarrera: Int,
        semestre: Int,
        contrasena: String?,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            val numeroControl = NumeroControl.normalizar(matricula)
            if (nombres.isBlank() || apellidoPaterno.isBlank() || correo.isBlank()) {
                throw ErrorDatos("Escribe nombres, apellido paterno y correo.")
            }
            if (!NumeroControl.esValido(numeroControl)) throw ErrorDatos(NumeroControl.MENSAJE)
            if (semestre !in 1..9) throw ErrorDatos("El semestre debe estar entre 1 y 9.")

            if (id == null) {
                val generada = contrasena.isNullOrEmpty()
                val clave = if (generada) Clave.inicial() else contrasena.orEmpty()
                val idUsuario = db.insertOrThrow("usuarios", null, ContentValues().apply {
                    put("nombres", nombres.trim())
                    put("apellido_paterno", apellidoPaterno.trim())
                    if (apellidoMaterno.isBlank()) putNull("apellido_materno") else put("apellido_materno", apellidoMaterno.trim())
                    put("correo", correo.trim())
                    put("contrasena_hash", Clave.generar(clave))
                    put("rol", "alumno")
                })
                db.insertOrThrow("alumnos", null, ContentValues().apply {
                    put("id_usuario", idUsuario)
                    put("matricula", numeroControl)
                    put("id_carrera", idCarrera)
                    put("semestre", semestre)
                })
                if (generada) clave else null
            } else {
                val filas = db.update("usuarios", ContentValues().apply {
                    put("nombres", nombres.trim())
                    put("apellido_paterno", apellidoPaterno.trim())
                    if (apellidoMaterno.isBlank()) putNull("apellido_materno") else put("apellido_materno", apellidoMaterno.trim())
                    put("correo", correo.trim())
                }, "id = ? AND rol = 'alumno'", arrayOf(id.toString()))
                if (filas == 0) throw ErrorDatos("Alumno no encontrado.")
                db.update("alumnos", ContentValues().apply {
                    put("matricula", numeroControl)
                    put("id_carrera", idCarrera)
                    put("semestre", semestre)
                }, "id_usuario = ?", arrayOf(id.toString()))
                null
            }
        }, alTerminar)
    }

    fun guardarDocente(
        id: Int?,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        especialidad: String,
        telefono: String,
        contrasena: String?,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            if (nombres.isBlank() || apellidoPaterno.isBlank() || correo.isBlank()) {
                throw ErrorDatos("Escribe nombres, apellido paterno y correo.")
            }
            if (id == null) {
                val generada = contrasena.isNullOrEmpty()
                val clave = if (generada) Clave.inicial() else contrasena.orEmpty()
                val idUsuario = db.insertOrThrow("usuarios", null, ContentValues().apply {
                    put("nombres", nombres.trim())
                    put("apellido_paterno", apellidoPaterno.trim())
                    if (apellidoMaterno.isBlank()) putNull("apellido_materno") else put("apellido_materno", apellidoMaterno.trim())
                    put("correo", correo.trim())
                    put("contrasena_hash", Clave.generar(clave))
                    put("rol", "docente")
                })
                db.insertOrThrow("docentes", null, ContentValues().apply {
                    put("id_usuario", idUsuario)
                    if (especialidad.isBlank()) putNull("especialidad") else put("especialidad", especialidad.trim())
                    if (telefono.isBlank()) putNull("telefono") else put("telefono", telefono.trim())
                })
                if (generada) clave else null
            } else {
                val filas = db.update("usuarios", ContentValues().apply {
                    put("nombres", nombres.trim())
                    put("apellido_paterno", apellidoPaterno.trim())
                    if (apellidoMaterno.isBlank()) putNull("apellido_materno") else put("apellido_materno", apellidoMaterno.trim())
                    put("correo", correo.trim())
                }, "id = ? AND rol = 'docente'", arrayOf(id.toString()))
                if (filas == 0) throw ErrorDatos("Docente no encontrado.")
                db.update("docentes", ContentValues().apply {
                    if (especialidad.isBlank()) putNull("especialidad") else put("especialidad", especialidad.trim())
                    if (telefono.isBlank()) putNull("telefono") else put("telefono", telefono.trim())
                }, "id_usuario = ?", arrayOf(id.toString()))
                null
            }
        }, alTerminar)
    }

    fun eliminarAlumno(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) =
        darDeBaja(id, "alumno", alTerminar)

    fun eliminarDocente(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) =
        darDeBaja(id, "docente", alTerminar)

    private fun darDeBaja(id: Int, rol: String, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val filas = db.update("usuarios", ContentValues().apply { put("activo", 0) },
                "id = ? AND rol = ?", arrayOf(id.toString(), rol))
            if (filas == 0) throw ErrorDatos("No encontrado.")
        }, alTerminar)
    }
}
