package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

object Asistencia_Modelo {

    // lista de la fecha elegida
    // sin registro todos salen presentes
    fun lista(idActividad: Int, fecha: String, alTerminar: (Resultado_Modelo<ListaAsistencia>) -> Unit) {
        BaseDatos.consultar({ db ->
            exigirTitular(db, idActividad)
            var dia = fecha
            if (dia.isBlank()) {
                dia = Limites.hoy()
            }
            val cursor = db.rawQuery(
                """SELECT u.id,
                          TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                               COALESCE(u.apellido_materno, '')) AS nombre,
                          a.matricula, COALESCE(asi.presente, 1) AS presente
                     FROM inscripciones i
                     JOIN alumnos a ON a.id_usuario = i.id_alumno
                     JOIN usuarios u ON u.id = a.id_usuario
                     LEFT JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha = ?
                     LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
                    WHERE i.id_actividad = ? AND i.estado = 'activa'
                    ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(dia, idActividad.toString()),
            )
            val alumnos = mutableListOf<MarcaAsistencia>()
            while (cursor.moveToNext()) {
                val marca = MarcaAsistencia(
                    cursor.entero("id"), cursor.texto("nombre"), cursor.texto("matricula"),
                    cursor.entero("presente") == 1,
                )
                alumnos.add(marca)
            }
            cursor.close()
            ListaAsistencia(dia, alumnos)
        }, alTerminar)
    }

    fun guardar(
        idActividad: Int,
        fecha: String,
        marcas: List<MarcaAsistencia>,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            exigirTitular(db, idActividad)
            if (marcas.isEmpty()) {
                throw ErrorDatos("La lista de asistencia está vacía.")
            }
            val error = Limites.revisarFecha(fecha)
            if (error != null) {
                throw ErrorDatos(error)
            }

            // crea la sesion si no existe
            val sesion = ContentValues()
            sesion.put("id_actividad", idActividad)
            sesion.put("fecha", fecha)
            db.insertWithOnConflict("sesiones", null, sesion, SQLiteDatabase.CONFLICT_IGNORE)

            val cursor = db.rawQuery(
                "SELECT id FROM sesiones WHERE id_actividad = ? AND fecha = ?",
                arrayOf(idActividad.toString(), fecha),
            )
            var idSesion = 0
            if (cursor.moveToFirst()) {
                idSesion = cursor.getInt(0)
            }
            cursor.close()
            if (idSesion == 0) {
                throw ErrorDatos("No se pudo abrir la sesión.")
            }

            for (marca in marcas) {
                var presente = 0
                if (marca.presente) presente = 1

                val valores = ContentValues()
                valores.put("presente", presente)
                val filas = db.update(
                    "asistencias", valores, "id_sesion = ? AND id_alumno = ?",
                    arrayOf(idSesion.toString(), marca.idAlumno.toString()),
                )
                if (filas == 0) {
                    valores.put("id_sesion", idSesion)
                    valores.put("id_alumno", marca.idAlumno)
                    db.insertOrThrow("asistencias", null, valores)
                }
            }
        }, alTerminar)
    }

    fun historial(idActividad: Int, alTerminar: (Resultado_Modelo<List<SesionAsistencia>>) -> Unit) {
        BaseDatos.consultar({ db ->
            exigirTitular(db, idActividad)
            val sesiones = mutableListOf<SesionAsistencia>()
            val cursorSesiones = db.rawQuery(
                "SELECT id, fecha FROM sesiones WHERE id_actividad = ? ORDER BY fecha DESC",
                arrayOf(idActividad.toString()),
            )
            while (cursorSesiones.moveToNext()) {
                val idSesion = cursorSesiones.getInt(0)
                val fecha = cursorSesiones.getString(1)
                val alumnos = detalleSesion(db, idSesion)
                var presentes = 0
                for (alumno in alumnos) {
                    if (alumno.presente) presentes++
                }
                sesiones.add(SesionAsistencia(idSesion, fecha, presentes, alumnos.size, alumnos))
            }
            cursorSesiones.close()
            sesiones
        }, alTerminar)
    }

    private fun detalleSesion(db: SQLiteDatabase, idSesion: Int): List<DetalleAsistencia> {
        val cursor = db.rawQuery(
            """SELECT u.nombres, u.apellido_paterno, u.apellido_materno, a.matricula, asi.presente
                 FROM asistencias asi
                 JOIN alumnos a ON a.id_usuario = asi.id_alumno
                 JOIN usuarios u ON u.id = a.id_usuario
                WHERE asi.id_sesion = ?
                ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
            arrayOf(idSesion.toString()),
        )
        val lista = mutableListOf<DetalleAsistencia>()
        while (cursor.moveToNext()) {
            val nombre = nombreCompleto(
                cursor.texto("nombres"), cursor.texto("apellido_paterno"), cursor.texto("apellido_materno"),
            )
            lista.add(DetalleAsistencia(nombre, cursor.texto("matricula"), cursor.entero("presente") == 1))
        }
        cursor.close()
        return lista
    }

    // revisa que la actividad exista
    // el docente solo ve sus actividades
    private fun exigirTitular(db: SQLiteDatabase, idActividad: Int) {
        val cursor = db.rawQuery(
            "SELECT id_docente FROM actividades WHERE id = ? AND activa = 1",
            arrayOf(idActividad.toString()),
        )
        if (!cursor.moveToFirst()) {
            cursor.close()
            throw ErrorDatos("Actividad no encontrada.")
        }
        var docente: Int? = null
        if (!cursor.isNull(0)) {
            docente = cursor.getInt(0)
        }
        cursor.close()
        if (Sesion_Actual.rol() == "docente" && docente != Sesion_Actual.id()) {
            throw ErrorDatos("No eres el docente de esta actividad.")
        }
    }
}
