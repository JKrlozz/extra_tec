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
            val cursor = db.rawQuery(
                """SELECT id, nombres, apellido_paterno, apellido_materno, correo, rol, contrasena_hash
                     FROM usuarios WHERE correo = ? AND activo = 1""",
                arrayOf(correo.trim()),
            )
            var usuario: Usuario? = null
            var hash = ""
            if (cursor.moveToFirst()) {
                usuario = Usuario(
                    cursor.entero("id"), cursor.texto("nombres"), cursor.texto("apellido_paterno"),
                    cursor.texto("apellido_materno"), cursor.texto("correo"), cursor.texto("rol"),
                )
                hash = cursor.texto("contrasena_hash")
            }
            cursor.close()

            if (usuario == null || usuario.rol != rol || !Clave.verificar(contrasena, hash)) {
                throw ErrorDatos("Correo, contraseña o tipo de usuario incorrectos.")
            }
            SesionIniciada(usuario)
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
            var error = Limites.revisarPersona(nombres, apellidoPaterno, apellidoMaterno, correo)
            if (error == null) {
                error = Limites.revisarClave(contrasena, false)
            }
            if (error != null) {
                throw ErrorDatos(error)
            }
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

            val usuario = ContentValues()
            usuario.put("nombres", nombres.trim())
            usuario.put("apellido_paterno", apellidoPaterno.trim())
            if (apellidoMaterno.isBlank()) {
                usuario.putNull("apellido_materno")
            } else {
                usuario.put("apellido_materno", apellidoMaterno.trim())
            }
            usuario.put("correo", correo.trim().lowercase())
            usuario.put("contrasena_hash", Clave.generar(contrasena))
            usuario.put("rol", "alumno")
            val id = db.insertOrThrow("usuarios", null, usuario)

            val alumno = ContentValues()
            alumno.put("id_usuario", id)
            alumno.put("matricula", numeroControl)
            alumno.put("id_carrera", idCarrera)
            alumno.put("semestre", semestre)
            db.insertOrThrow("alumnos", null, alumno)
            Unit
        }, alTerminar)
    }

    // carreras en orden de la escuela
    fun carreras(alTerminar: (Resultado_Modelo<List<Carrera>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
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
            )
            val lista = mutableListOf<Carrera>()
            while (cursor.moveToNext()) {
                lista.add(Carrera(cursor.entero("id"), cursor.texto("nombre")))
            }
            cursor.close()
            lista
        }, alTerminar)
    }
}
