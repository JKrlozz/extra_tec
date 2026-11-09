package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Asistencia_Modelo {

    // Carga la lista de la fecha indicada o de hoy
    fun lista(
        idActividad: Int,
        fecha: String? = null,
        alTerminar: (Resultado_Modelo<ListaAsistencia>) -> Unit,
    ) {
        BaseDatos.consultar({ db ->
            exigirTitular(db, idActividad)
            val dia = fecha?.takeIf { it.isNotBlank() } ?: hoy()
            db.rawQuery(
                """SELECT u.id,
                          TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                               COALESCE(u.apellido_materno, '')) AS nombre,
                          a.matricula, asi.presente
                     FROM inscripciones i
                     JOIN alumnos a ON a.id_usuario = i.id_alumno
                     JOIN usuarios u ON u.id = a.id_usuario
                     LEFT JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha = ?
                     LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
                    WHERE i.id_actividad = ? AND i.estado = 'activa'
                    ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(dia, idActividad.toString()),
            ).use { c ->
                val alumnos = buildList {
                    while (c.moveToNext()) {
                        val indice = c.getColumnIndex("presente")
                        add(
                            MarcaAsistencia(
                                c.entero("id"), c.texto("nombre"), c.texto("matricula"),
                                indice < 0 || c.isNull(indice) || c.getInt(indice) != 0,
                            )
                        )
                    }
                }
                ListaAsistencia(dia, alumnos)
            }
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
            if (marcas.isEmpty()) throw ErrorDatos("La lista de asistencia está vacía.")
            val dia = fecha.ifBlank { hoy() }

            db.insertWithOnConflict("sesiones", null, ContentValues().apply {
                put("id_actividad", idActividad)
                put("fecha", dia)
            }, SQLiteDatabase.CONFLICT_IGNORE)
            val idSesion = db.rawQuery(
                "SELECT id FROM sesiones WHERE id_actividad = ? AND fecha = ?",
                arrayOf(idActividad.toString(), dia),
            ).use { c ->
                if (c.moveToFirst()) c.getInt(0) else throw ErrorDatos("No se pudo abrir la sesión.")
            }

            for (marca in marcas) {
                val inscrito = db.rawQuery(
                    "SELECT 1 FROM inscripciones WHERE id_actividad = ? AND id_alumno = ? AND estado = 'activa'",
                    arrayOf(idActividad.toString(), marca.idAlumno.toString()),
                ).use { it.moveToFirst() }
                if (!inscrito) continue

                val valores = ContentValues().apply { put("presente", if (marca.presente) 1 else 0) }
                val filas = db.update(
                    "asistencias", valores, "id_sesion = ? AND id_alumno = ?",
                    arrayOf(idSesion.toString(), marca.idAlumno.toString()),
                )
                if (filas == 0) {
                    db.insertOrThrow("asistencias", null, ContentValues().apply {
                        put("id_sesion", idSesion)
                        put("id_alumno", marca.idAlumno)
                        put("presente", if (marca.presente) 1 else 0)
                    })
                }
            }
        }, alTerminar)
    }

    fun historial(idActividad: Int, alTerminar: (Resultado_Modelo<List<SesionAsistencia>>) -> Unit) {
        BaseDatos.consultar({ db ->
            exigirTitular(db, idActividad)
            db.rawQuery(
                """SELECT s.id AS id_sesion, s.fecha,
                          u.nombres, u.apellido_paterno, u.apellido_materno,
                          a.matricula, asi.presente
                     FROM sesiones s
                     LEFT JOIN asistencias asi ON asi.id_sesion = s.id
                     LEFT JOIN alumnos a ON a.id_usuario = asi.id_alumno
                     LEFT JOIN usuarios u ON u.id = a.id_usuario
                    WHERE s.id_actividad = ?
                    ORDER BY s.fecha DESC, u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(idActividad.toString()),
            ).use { c ->
                val mapa = LinkedHashMap<Int, SesionAsistencia>()
                while (c.moveToNext()) {
                    val idSesion = c.entero("id_sesion")
                    val anterior = mapa[idSesion]
                    val alumno = if (c.texto("matricula").isBlank()) {
                        null
                    } else {
                        DetalleAsistencia(
                            nombreCompleto(
                                c.texto("nombres"), c.texto("apellido_paterno"), c.texto("apellido_materno"),
                            ),
                            c.texto("matricula"),
                            c.entero("presente") != 0,
                        )
                    }
                    val alumnos = (anterior?.alumnos ?: emptyList()) + listOfNotNull(alumno)
                    mapa[idSesion] = SesionAsistencia(
                        id = idSesion,
                        fecha = c.texto("fecha"),
                        presentes = alumnos.count { it.presente },
                        total = alumnos.size,
                        alumnos = alumnos,
                    )
                }
                mapa.values.toList()
            }
        }, alTerminar)
    }


    // Comprueba que la actividad exista y que el docente sea el titular
    private fun exigirTitular(db: SQLiteDatabase, idActividad: Int) {
        val docente = db.rawQuery(
            "SELECT id_docente FROM actividades WHERE id = ? AND activa = 1",
            arrayOf(idActividad.toString()),
        ).use { c ->
            if (!c.moveToFirst()) throw ErrorDatos("Actividad no encontrada.")
            if (c.isNull(0)) null else c.getInt(0)
        }
        if (Sesion_Actual.rol() == "docente" && docente != Sesion_Actual.id()) {
            throw ErrorDatos("No eres el docente de esta actividad.")
        }
    }

    private fun hoy(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
