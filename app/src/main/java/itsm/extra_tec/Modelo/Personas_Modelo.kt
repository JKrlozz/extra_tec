package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

object Personas_Modelo {

    fun alumnos(filtro: String, alTerminar: (Resultado_Modelo<List<Alumno>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val patron = "%" + filtro.trim() + "%"
            val cursor = db.rawQuery(
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
            )
            val lista = mutableListOf<Alumno>()
            while (cursor.moveToNext()) {
                val alumno = Alumno(
                    cursor.entero("id"), cursor.texto("nombres"), cursor.texto("apellido_paterno"),
                    cursor.texto("apellido_materno"), cursor.texto("correo"), cursor.texto("matricula"),
                    cursor.entero("semestre"), cursor.entero("id_carrera"), cursor.texto("carrera"),
                )
                lista.add(alumno)
            }
            cursor.close()
            lista
        }, alTerminar)
    }

    fun docentes(filtro: String, alTerminar: (Resultado_Modelo<List<Docente>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val patron = "%" + filtro.trim() + "%"
            val cursor = db.rawQuery(
                """SELECT u.id, u.nombres, u.apellido_paterno, u.apellido_materno, u.correo,
                          COALESCE(e.nombre, '') AS especialidad, COALESCE(d.telefono, '') AS telefono
                     FROM docentes d
                     JOIN usuarios u ON u.id = d.id_usuario
                     LEFT JOIN especialidades e ON e.id = d.id_especialidad
                    WHERE u.activo = 1
                      AND ((u.nombres || ' ' || u.apellido_paterno || ' ' || COALESCE(u.apellido_materno, '')) LIKE ?
                           OR u.correo LIKE ?)
                    ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(patron, patron),
            )
            val lista = mutableListOf<Docente>()
            while (cursor.moveToNext()) {
                val docente = Docente(
                    cursor.entero("id"), cursor.texto("nombres"), cursor.texto("apellido_paterno"),
                    cursor.texto("apellido_materno"), cursor.texto("correo"),
                    cursor.texto("especialidad"), cursor.texto("telefono"),
                )
                lista.add(docente)
            }
            cursor.close()
            lista
        }, alTerminar)
    }

    // catalogo para sugerir especialidades
    fun especialidades(alTerminar: (Resultado_Modelo<List<String>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery("SELECT nombre FROM especialidades ORDER BY nombre", null)
            val lista = mutableListOf<String>()
            while (cursor.moveToNext()) {
                lista.add(cursor.getString(0))
            }
            cursor.close()
            lista
        }, alTerminar)
    }

    // regresa la clave si se genero
    fun guardarAlumno(
        id: Int?,
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        matricula: String,
        idCarrera: Int,
        semestre: Int,
        contrasena: String,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            val numeroControl = NumeroControl.normalizar(matricula)
            revisar(Limites.revisarPersona(nombres, apellidoPaterno, apellidoMaterno, correo))
            if (!NumeroControl.esValido(numeroControl)) {
                throw ErrorDatos(NumeroControl.MENSAJE)
            }
            val errorCorreo = Limites.revisarCorreoAlumno(correo, numeroControl)
            if (errorCorreo != null) {
                throw ErrorDatos(errorCorreo)
            }
            if (semestre < 1 || semestre > Limites.SEMESTRE_MAX) {
                throw ErrorDatos("El semestre debe estar entre 1 y 8.")
            }

            var claveGenerada: String? = null
            if (id == null) {
                revisar(Limites.revisarClave(contrasena, true))
                var clave = contrasena
                if (clave.isEmpty()) {
                    clave = Clave.inicial()
                    claveGenerada = clave
                }
                val usuario = datosUsuario(nombres, apellidoPaterno, apellidoMaterno, correo)
                usuario.put("contrasena_hash", Clave.generar(clave))
                usuario.put("rol", "alumno")
                val idUsuario = db.insertOrThrow("usuarios", null, usuario)

                val alumno = ContentValues()
                alumno.put("id_usuario", idUsuario)
                alumno.put("matricula", numeroControl)
                alumno.put("id_carrera", idCarrera)
                alumno.put("semestre", semestre)
                db.insertOrThrow("alumnos", null, alumno)
            } else {
                val usuario = datosUsuario(nombres, apellidoPaterno, apellidoMaterno, correo)
                val filas = db.update("usuarios", usuario, "id = ? AND rol = 'alumno'", arrayOf(id.toString()))
                if (filas == 0) {
                    throw ErrorDatos("Alumno no encontrado.")
                }
                val alumno = ContentValues()
                alumno.put("matricula", numeroControl)
                alumno.put("id_carrera", idCarrera)
                alumno.put("semestre", semestre)
                db.update("alumnos", alumno, "id_usuario = ?", arrayOf(id.toString()))
            }
            claveGenerada
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
        contrasena: String,
        alTerminar: (Resultado_Modelo<String?>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            revisar(Limites.revisarPersona(nombres, apellidoPaterno, apellidoMaterno, correo))
            revisar(Limites.revisarEspecialidad(especialidad))
            revisar(Limites.revisarTelefono(telefono))

            val docente = ContentValues()
            val idEspecialidad = buscarEspecialidad(db, especialidad)
            if (idEspecialidad == null) {
                docente.putNull("id_especialidad")
            } else {
                docente.put("id_especialidad", idEspecialidad)
            }
            if (telefono.isBlank()) {
                docente.putNull("telefono")
            } else {
                docente.put("telefono", telefono.trim())
            }

            var claveGenerada: String? = null
            if (id == null) {
                revisar(Limites.revisarClave(contrasena, true))
                var clave = contrasena
                if (clave.isEmpty()) {
                    clave = Clave.inicial()
                    claveGenerada = clave
                }
                val usuario = datosUsuario(nombres, apellidoPaterno, apellidoMaterno, correo)
                usuario.put("contrasena_hash", Clave.generar(clave))
                usuario.put("rol", "docente")
                val idUsuario = db.insertOrThrow("usuarios", null, usuario)
                docente.put("id_usuario", idUsuario)
                db.insertOrThrow("docentes", null, docente)
            } else {
                val usuario = datosUsuario(nombres, apellidoPaterno, apellidoMaterno, correo)
                val filas = db.update("usuarios", usuario, "id = ? AND rol = 'docente'", arrayOf(id.toString()))
                if (filas == 0) {
                    throw ErrorDatos("Docente no encontrado.")
                }
                db.update("docentes", docente, "id_usuario = ?", arrayOf(id.toString()))
            }
            claveGenerada
        }, alTerminar)
    }

    fun eliminarAlumno(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        darDeBaja(id, "alumno", alTerminar)
    }

    fun eliminarDocente(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        darDeBaja(id, "docente", alTerminar)
    }

    // baja logica para conservar historial
    private fun darDeBaja(id: Int, rol: String, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val valores = ContentValues()
            valores.put("activo", 0)
            val filas = db.update("usuarios", valores, "id = ? AND rol = ?", arrayOf(id.toString(), rol))
            if (filas == 0) {
                throw ErrorDatos("No encontrado.")
            }
        }, alTerminar)
    }

    private fun datosUsuario(nombres: String, paterno: String, materno: String, correo: String): ContentValues {
        val valores = ContentValues()
        valores.put("nombres", nombres.trim())
        valores.put("apellido_paterno", paterno.trim())
        if (materno.isBlank()) {
            valores.putNull("apellido_materno")
        } else {
            valores.put("apellido_materno", materno.trim())
        }
        valores.put("correo", correo.trim().lowercase())
        return valores
    }

    // busca la especialidad o la crea
    private fun buscarEspecialidad(db: SQLiteDatabase, nombre: String): Int? {
        val texto = nombre.trim()
        if (texto.isEmpty()) {
            return null
        }
        val cursor = db.rawQuery("SELECT id FROM especialidades WHERE nombre = ?", arrayOf(texto))
        var id: Int? = null
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        if (id != null) {
            return id
        }
        val valores = ContentValues()
        valores.put("nombre", texto)
        return db.insertOrThrow("especialidades", null, valores).toInt()
    }

    private fun revisar(error: String?) {
        if (error != null) {
            throw ErrorDatos(error)
        }
    }
}
