package itsm.extra_tec.Modelo

import android.content.ContentValues

object Autenticacion_Modelo {

    fun iniciarSesion(
        correo: String,
        contrasena: String,
        rol: String,
        alTerminar: (Resultado_Modelo<SesionIniciada>) -> Unit,
    ) {
        BaseDatos.consultar({ db ->
            val fila = db.rawQuery(
                """SELECT id, nombres, apellido_paterno, apellido_materno, correo, rol, contrasena_hash
                     FROM usuarios WHERE correo = ? AND activo = 1""",
                arrayOf(correo.trim()),
            ).use { c ->
                if (!c.moveToFirst()) null else FilaLogin(
                    Usuario(
                        c.entero("id"), c.texto("nombres"), c.texto("apellido_paterno"),
                        c.texto("apellido_materno"), c.texto("correo"), c.texto("rol"),
                    ),
                    c.texto("contrasena_hash"),
                )
            }
            if (fila == null || fila.usuario.rol != rol || !Clave.verificar(contrasena, fila.hash)) {
                throw ErrorDatos("Correo, contraseña o tipo de usuario incorrectos.")
            }
            SesionIniciada(fila.usuario)
        }, alTerminar)
    }

    fun registrar(
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        correo: String,
        contrasena: String,
        matricula: String,
        idCarrera: Int,
        semestre: Int,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            val numeroControl = NumeroControl.normalizar(matricula)
            if (nombres.isBlank() || apellidoPaterno.isBlank() || correo.isBlank() || contrasena.isEmpty()) {
                throw ErrorDatos("Completa todos los datos obligatorios.")
            }
            if (!NumeroControl.esValido(numeroControl)) throw ErrorDatos(NumeroControl.MENSAJE)
            if (semestre !in 1..9) throw ErrorDatos("El semestre debe estar entre 1 y 9.")

            val id = db.insertOrThrow("usuarios", null, ContentValues().apply {
                put("nombres", nombres.trim())
                put("apellido_paterno", apellidoPaterno.trim())
                if (apellidoMaterno.isBlank()) putNull("apellido_materno") else put("apellido_materno", apellidoMaterno.trim())
                put("correo", correo.trim())
                put("contrasena_hash", Clave.generar(contrasena))
                put("rol", "alumno")
            })
            db.insertOrThrow("alumnos", null, ContentValues().apply {
                put("id_usuario", id)
                put("matricula", numeroControl)
                put("id_carrera", idCarrera)
                put("semestre", semestre)
            })
            Unit
        }, alTerminar)
    }

    fun carreras(alTerminar: (Resultado_Modelo<List<Carrera>>) -> Unit) {
        BaseDatos.consultar({ db ->
            db.rawQuery(
                """SELECT id, nombre FROM carreras
                    ORDER BY CASE nombre
                        WHEN 'Ingeniería Industrial' THEN 1
                        WHEN 'Ingeniería en Sistemas Computacionales' THEN 2
                        WHEN 'Ingeniería Bioquímica' THEN 3
                        WHEN 'Ingeniería Electromecánica' THEN 4
                        WHEN 'Ingeniería Civil' THEN 5
                        WHEN 'Ingeniería en Tecnologías de la Información y Comunicaciones' THEN 6
                        WHEN 'Ingeniería Ambiental' THEN 7
                        WHEN 'Ingeniería en Gestión Empresarial' THEN 8
                        WHEN 'Ingeniería Petrolera' THEN 9
                        WHEN 'Licenciatura en Gastronomía' THEN 10
                        ELSE 99 END, nombre""",
                null,
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(Carrera(c.entero("id"), c.texto("nombre")))
                }
            }
        }, alTerminar)
    }

    private data class FilaLogin(val usuario: Usuario, val hash: String)
}
