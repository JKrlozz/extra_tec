package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.Calendar
import java.util.Locale

object Actividades_Modelo {
    private const val MINIMO_ASISTENCIA = 80.0
    private val FORMATO_HORA = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun listar(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val idAlumno = Sesion_Actual.id()
            val resumen = horariosResumidos(db)
            db.rawQuery(
                """SELECT v.*, EXISTS(SELECT 1 FROM inscripciones i
                                       WHERE i.id_actividad = v.id AND i.id_alumno = ? AND i.estado = 'activa') AS inscrito
                     FROM v_actividades v WHERE v.activa = 1 ORDER BY v.nombre""",
                arrayOf(idAlumno.toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val id = c.entero("id")
                        add(actividadDesde(c, resumen[id]))
                    }
                }
            }
        }, alTerminar)
    }

    // Listado de actividades para el administrador
    fun todas(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val resumen = horariosResumidos(db)
            db.rawQuery(
                "SELECT v.*, 0 AS inscrito FROM v_actividades v WHERE v.activa = 1 ORDER BY v.nombre",
                null,
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val id = c.entero("id")
                        add(actividadDesde(c, resumen[id]))
                    }
                }
            }
        }, alTerminar)
    }

    fun detalle(id: Int, alTerminar: (Resultado_Modelo<DetalleActividad>) -> Unit) {
        BaseDatos.consultar({ db ->
            val idAlumno = Sesion_Actual.id()
            val actividad = db.rawQuery(
                """SELECT v.*, EXISTS(SELECT 1 FROM inscripciones i
                                       WHERE i.id_actividad = v.id AND i.id_alumno = ? AND i.estado = 'activa') AS inscrito
                     FROM v_actividades v WHERE v.id = ?""",
                arrayOf(idAlumno.toString(), id.toString()),
            ).use { c ->
                if (!c.moveToFirst()) throw ErrorDatos("Actividad no encontrada.")
                actividadDesde(c, horariosResumidos(db)[id])
            }
            DetalleActividad(actividad, horariosDe(db, id))
        }, alTerminar)
    }

    // Dias y lugares del editor de horarios
    fun catalogosHorario(alTerminar: (Resultado_Modelo<CatalogosHorario>) -> Unit) {
        BaseDatos.consultar({ db ->
            val dias = db.rawQuery("SELECT id, nombre FROM dias_semana ORDER BY orden", null).use { c ->
                buildList { while (c.moveToNext()) add(DiaSemana(c.entero("id"), c.texto("nombre"))) }
            }
            val lugares = db.rawQuery("SELECT id, nombre FROM lugares ORDER BY nombre", null).use { c ->
                buildList { while (c.moveToNext()) add(Lugar(c.entero("id"), c.texto("nombre"))) }
            }
            CatalogosHorario(dias, lugares)
        }, alTerminar)
    }

    fun guardar(
        id: Int?,
        nombre: String,
        descripcion: String,
        cupo: Int,
        idDocente: Int,
        horarios: List<HorarioDia>,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            if (nombre.isBlank()) throw ErrorDatos("Escribe el nombre de la actividad.")
            if (cupo <= 0) throw ErrorDatos("El cupo debe ser un número mayor a 0.")
            validarHorarios(db, horarios)

            if (id == null) {
                val periodo = db.rawQuery(
                    "SELECT id FROM periodos WHERE activo = 1 ORDER BY inicio DESC LIMIT 1", null,
                ).use { c -> if (c.moveToFirst()) c.getInt(0) else null }
                    ?: throw ErrorDatos("No hay un periodo activo.")
                val idNueva = db.insertOrThrow("actividades", null, ContentValues().apply {
                    put("nombre", nombre.trim())
                    if (descripcion.isBlank()) putNull("descripcion") else put("descripcion", descripcion.trim())
                    put("cupo", cupo)
                    put("id_docente", idDocente)
                    put("id_periodo", periodo)
                }).toInt()
                guardarHorarios(db, idNueva, horarios)
            } else {
                val filas = db.update("actividades", ContentValues().apply {
                    put("nombre", nombre.trim())
                    if (descripcion.isBlank()) putNull("descripcion") else put("descripcion", descripcion.trim())
                    put("cupo", cupo)
                    put("id_docente", idDocente)
                }, "id = ?", arrayOf(id.toString()))
                if (filas == 0) throw ErrorDatos("Actividad no encontrada.")
                guardarHorarios(db, id, horarios)
            }
        }, alTerminar)
    }

    fun eliminar(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val filas = db.update("actividades", ContentValues().apply { put("activa", 0) },
                "id = ?", arrayOf(id.toString()))
            if (filas == 0) throw ErrorDatos("Actividad no encontrada.")
        }, alTerminar)
    }

    fun inscribir(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val idAlumno = Sesion_Actual.id()
            val disponible = db.rawQuery(
                "SELECT 1 FROM actividades WHERE id = ? AND activa = 1", arrayOf(idActividad.toString()),
            ).use { it.moveToFirst() }
            if (!disponible) throw ErrorDatos("Actividad no disponible.")

            val actual = db.rawQuery(
                """SELECT a.nombre FROM inscripciones i JOIN actividades a ON a.id = i.id_actividad
                    WHERE i.id_alumno = ? AND i.estado = 'activa' AND i.id_actividad <> ?""",
                arrayOf(idAlumno.toString(), idActividad.toString()),
            ).use { if (it.moveToFirst()) it.getString(0) else null }
            if (actual != null) {
                throw ErrorDatos("Ya estás inscrito en $actual. Da de baja esa actividad antes de inscribirte en otra.")
            }

            val previa = db.rawQuery(
                "SELECT id, estado FROM inscripciones WHERE id_alumno = ? AND id_actividad = ?",
                arrayOf(idAlumno.toString(), idActividad.toString()),
            ).use { c ->
                if (c.moveToFirst()) c.getInt(0) to c.getString(1) else null
            }
            if (previa != null) {
                if (previa.second == "activa") throw ErrorDatos("Ya estás inscrito en esta actividad.")
                val cupo = db.rawQuery(
                    "SELECT cupo, inscritos FROM v_actividades WHERE id = ?", arrayOf(idActividad.toString()),
                ).use { c ->
                    if (c.moveToFirst()) c.getInt(0) to c.getInt(1) else 0 to 0
                }
                if (cupo.second >= cupo.first) throw ErrorDatos("Cupo lleno para esta actividad.")
                db.execSQL(
                    "UPDATE inscripciones SET estado = 'activa', fecha = datetime('now', 'localtime') WHERE id = ?",
                    arrayOf(previa.first.toString()),
                )
            } else {
                db.insertOrThrow("inscripciones", null, ContentValues().apply {
                    put("id_alumno", idAlumno)
                    put("id_actividad", idActividad)
                })
            }
            Unit
        }, alTerminar)
    }

    fun darDeBaja(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val filas = db.update(
                "inscripciones",
                ContentValues().apply { put("estado", "baja") },
                "id_alumno = ? AND id_actividad = ? AND estado = 'activa'",
                arrayOf(Sesion_Actual.id().toString(), idActividad.toString()),
            )
            if (filas == 0) throw ErrorDatos("No estás inscrito en esa actividad.")
        }, alTerminar)
    }

    fun horarioAlumno(alTerminar: (Resultado_Modelo<List<HorarioAlumno>>) -> Unit) {
        BaseDatos.consultar({ db ->
            db.rawQuery(
                """SELECT a.id AS id_actividad, a.nombre, h.id_dia, d.nombre AS dia_semana,
                          h.hora_inicio, h.hora_fin, h.id_lugar, l.nombre AS lugar
                     FROM inscripciones i
                     JOIN actividades a ON a.id = i.id_actividad
                     JOIN horarios h ON h.id_actividad = a.id
                     JOIN dias_semana d ON d.id = h.id_dia
                     JOIN lugares l ON l.id = h.id_lugar
                    WHERE i.id_alumno = ? AND i.estado = 'activa' AND a.activa = 1
                    ORDER BY d.orden, h.hora_inicio""",
                arrayOf(Sesion_Actual.id().toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        HorarioAlumno(
                            c.entero("id_actividad"), c.texto("nombre"),
                            HorarioDia(
                                c.entero("id_dia"), c.texto("dia_semana"),
                                c.texto("hora_inicio").take(5), c.texto("hora_fin").take(5),
                                c.entero("id_lugar"), c.texto("lugar"),
                            ),
                        )
                    )
                }
            }
        }, alTerminar)
    }

    fun constancias(alTerminar: (Resultado_Modelo<List<Constancia>>) -> Unit) {
        BaseDatos.consultar({ db ->
            db.rawQuery(
                """SELECT a.id AS id_actividad, a.nombre, p.nombre AS periodo,
                          v.sesiones, v.asistencias, COALESCE(v.porcentaje, 0) AS porcentaje
                     FROM v_asistencia_alumno v
                     JOIN actividades a ON a.id = v.id_actividad
                     JOIN v_periodos p ON p.id = a.id_periodo
                    WHERE v.id_alumno = ? ORDER BY a.nombre""",
                arrayOf(Sesion_Actual.id().toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val porcentaje = c.real("porcentaje")
                        add(
                            Constancia(
                                c.entero("id_actividad"), c.texto("nombre"), c.texto("periodo"),
                                c.entero("sesiones"), c.entero("asistencias"), porcentaje,
                                porcentaje >= MINIMO_ASISTENCIA,
                            )
                        )
                    }
                }
            }
        }, alTerminar)
    }

    fun descargarConstancia(idActividad: Int, alTerminar: (Resultado_Modelo<Archivo>) -> Unit) {
        BaseDatos.consultar({ db ->
            val datos = db.rawQuery(
                """SELECT v.id_inscripcion, COALESCE(v.porcentaje, 0) AS porcentaje,
                          a.nombre AS actividad, p.inicio, p.fin,
                          TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                               COALESCE(u.apellido_materno, '')) AS alumno,
                          al.matricula, c.nombre AS carrera
                     FROM v_asistencia_alumno v
                     JOIN actividades a ON a.id = v.id_actividad
                     JOIN v_periodos p ON p.id = a.id_periodo
                     JOIN usuarios u ON u.id = v.id_alumno
                     JOIN alumnos al ON al.id_usuario = v.id_alumno
                     JOIN carreras c ON c.id = al.id_carrera
                    WHERE v.id_alumno = ? AND v.id_actividad = ?""",
                arrayOf(Sesion_Actual.id().toString(), idActividad.toString()),
            ).use { c ->
                if (!c.moveToFirst()) throw ErrorDatos("No estás inscrito en esa actividad.")
                DatosConstancia(
                    c.entero("id_inscripcion"), c.real("porcentaje"), c.texto("actividad"),
                    c.texto("inicio"), c.texto("fin"), c.texto("alumno"), c.texto("matricula"),
                    c.texto("carrera"),
                )
            }
            if (datos.porcentaje < MINIMO_ASISTENCIA) {
                throw ErrorDatos(
                    "Se requiere ${MINIMO_ASISTENCIA.toInt()}% de asistencia " +
                        "(llevas ${String.format(Locale.US, "%.0f", datos.porcentaje)}%)."
                )
            }

            var constancia = leerConstancia(db, datos.idInscripcion)
            if (constancia == null) {
                val anio = Calendar.getInstance().get(Calendar.YEAR)
                val folio = "ITSM-$anio-${datos.idInscripcion.toString().padStart(5, '0')}"
                db.insertOrThrow("constancias", null, ContentValues().apply {
                    put("id_inscripcion", datos.idInscripcion)
                    put("folio", folio)
                })
                constancia = leerConstancia(db, datos.idInscripcion)
                    ?: throw ErrorDatos("No se pudo registrar la constancia.")
            }

            val nivel = if (datos.porcentaje >= 95.0) "SOBRESALIENTE" else "NOTABLE"
            val valor = if (datos.porcentaje >= 95.0) "4" else "3"
            val emision = constancia.second.take(10).split("-")
            val dia = emision.getOrNull(2)?.trimStart('0')?.ifBlank { "0" } ?: "1"
            val mes = MESES.getOrNull((emision.getOrNull(1)?.toIntOrNull() ?: 1) - 1) ?: "enero"
            val anio = emision.getOrNull(0).orEmpty()
            val folioCorto = constancia.first.substringAfterLast('-').ifBlank { constancia.first }

            Archivo(
                Archivos_Modelo.constanciaPdf(
                    ConstanciaPdf(
                        alumno = datos.alumno,
                        matricula = datos.matricula,
                        carrera = datos.carrera,
                        actividad = datos.actividad,
                        periodo = periodoOficial(datos.inicio, datos.fin),
                        nivel = nivel,
                        valor = valor,
                        creditos = "1",
                        folio = folioCorto,
                        dia = dia,
                        mes = mes,
                        anio = anio,
                    ),
                ),
                "constancia-${constancia.first}.pdf",
            )
        }, alTerminar)
    }

    fun misClases(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val resumen = horariosResumidos(db)
            db.rawQuery(
                "SELECT * FROM v_actividades WHERE id_docente = ? AND activa = 1 ORDER BY nombre",
                arrayOf(Sesion_Actual.id().toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val id = c.entero("id")
                        add(actividadDesde(c, resumen[id]))
                    }
                }
            }
        }, alTerminar)
    }

    fun alumnosDeActividad(idActividad: Int, alTerminar: (Resultado_Modelo<List<AlumnoInscrito>>) -> Unit) {
        BaseDatos.consultar({ db ->
            if (Sesion_Actual.rol() == "docente") {
                val esTitular = db.rawQuery(
                    "SELECT 1 FROM actividades WHERE id = ? AND id_docente = ?",
                    arrayOf(idActividad.toString(), Sesion_Actual.id().toString()),
                ).use { it.moveToFirst() }
                if (!esTitular) throw ErrorDatos("No eres el docente de esta actividad.")
            }
            db.rawQuery(
                """SELECT u.id, TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                          COALESCE(u.apellido_materno, '')) AS nombre, a.matricula
                     FROM inscripciones i
                     JOIN alumnos a ON a.id_usuario = i.id_alumno
                     JOIN usuarios u ON u.id = a.id_usuario
                    WHERE i.id_actividad = ? AND i.estado = 'activa'
                    ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(idActividad.toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        AlumnoInscrito(c.entero("id"), c.texto("nombre"), c.texto("matricula"))
                    )
                }
            }
        }, alTerminar)
    }


    private fun actividadDesde(c: Cursor, resumen: ResumenHorario?) = Actividad(
        id = c.entero("id"),
        nombre = c.texto("nombre"),
        descripcion = c.texto("descripcion"),
        cupo = c.entero("cupo"),
        inscritos = c.entero("inscritos"),
        horario = resumen?.horario.orEmpty(),
        lugar = resumen?.lugar.orEmpty(),
        docente = c.texto("docente"),
        idDocente = c.enteroONulo("id_docente"),
        inscrito = c.entero("inscrito") != 0,
    )

    private data class ResumenHorario(val horario: String, val lugar: String)

    private fun horariosResumidos(db: SQLiteDatabase): Map<Int, ResumenHorario> {
        val mapa = LinkedHashMap<Int, MutableList<Pair<String, String>>>()
        db.rawQuery(
            """SELECT h.id_actividad, d.nombre AS dia, h.hora_inicio, h.hora_fin, l.nombre AS lugar
                 FROM horarios h
                 JOIN dias_semana d ON d.id = h.id_dia
                 JOIN lugares l ON l.id = h.id_lugar
                 JOIN actividades a ON a.id = h.id_actividad
                WHERE a.activa = 1
                ORDER BY h.id_actividad, d.orden, h.hora_inicio""",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                mapa.getOrPut(c.entero("id_actividad")) { mutableListOf() }
                    .add(
                        "${c.texto("dia").take(3)} ${c.texto("hora_inicio").take(5)} a ${c.texto("hora_fin").take(5)}"
                            to c.texto("lugar")
                    )
            }
        }
        return mapa.mapValues { (_, filas) ->
            ResumenHorario(
                horario = filas.joinToString("\n") { it.first },
                lugar = filas.map { it.second }.distinct().joinToString("\n"),
            )
        }
    }

    private fun horariosDe(db: SQLiteDatabase, idActividad: Int): List<HorarioDia> =
        db.rawQuery(
            """SELECT h.id_dia, d.nombre AS dia_semana, h.hora_inicio, h.hora_fin,
                      h.id_lugar, l.nombre AS lugar
                 FROM horarios h
                 JOIN dias_semana d ON d.id = h.id_dia
                 JOIN lugares l ON l.id = h.id_lugar
                WHERE h.id_actividad = ?
                ORDER BY d.orden, h.hora_inicio""",
            arrayOf(idActividad.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    HorarioDia(
                        c.entero("id_dia"), c.texto("dia_semana"),
                        c.texto("hora_inicio").take(5), c.texto("hora_fin").take(5),
                        c.entero("id_lugar"), c.texto("lugar"),
                    )
                )
            }
        }

    private fun validarHorarios(db: SQLiteDatabase, horarios: List<HorarioDia>) {
        if (horarios.isEmpty()) throw ErrorDatos("Indica al menos un horario.")
        val dias = db.rawQuery("SELECT id FROM dias_semana", null).use { c ->
            buildSet { while (c.moveToNext()) add(c.getInt(0)) }
        }
        val revisados = mutableListOf<Triple<Int, String, String>>()
        for (h in horarios) {
            if (h.idDia !in dias || !FORMATO_HORA.matches(h.horaInicio) || !FORMATO_HORA.matches(h.horaFin)) {
                throw ErrorDatos("Horario inválido: revisa el día y las horas.")
            }
            if (h.horaFin <= h.horaInicio) {
                throw ErrorDatos("La hora de fin debe ser posterior a la de inicio.")
            }
            val choque = revisados.any { (dia, inicio, fin) ->
                dia == h.idDia && h.horaInicio < fin && inicio < h.horaFin
            }
            if (choque) throw ErrorDatos("Hay dos horarios traslapados el mismo día.")
            revisados.add(Triple(h.idDia, h.horaInicio, h.horaFin))
        }
    }

    private fun guardarHorarios(db: SQLiteDatabase, idActividad: Int, horarios: List<HorarioDia>) {
        db.delete("horarios", "id_actividad = ?", arrayOf(idActividad.toString()))
        for (h in horarios) {
            val idLugar = resolverLugar(db, h)
            db.insertOrThrow("horarios", null, ContentValues().apply {
                put("id_actividad", idActividad)
                put("id_dia", h.idDia)
                put("hora_inicio", h.horaInicio)
                put("hora_fin", h.horaFin)
                put("id_lugar", idLugar)
            })
        }
    }

    // Devuelve el lugar y lo registra si no existe
    private fun resolverLugar(db: SQLiteDatabase, h: HorarioDia): Int {
        if (h.idLugar != 0) {
            val existe = db.rawQuery(
                "SELECT 1 FROM lugares WHERE id = ?", arrayOf(h.idLugar.toString()),
            ).use { it.moveToFirst() }
            if (existe) return h.idLugar
        }
        val nombre = h.lugar.trim().ifEmpty { "ITSM" }
        db.rawQuery("SELECT id FROM lugares WHERE nombre = ?", arrayOf(nombre)).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return db.insertOrThrow("lugares", null, ContentValues().apply { put("nombre", nombre) }).toInt()
    }

    private fun leerConstancia(db: SQLiteDatabase, idInscripcion: Int): Pair<String, String>? =
        db.rawQuery(
            "SELECT folio, emitida_en FROM constancias WHERE id_inscripcion = ?",
            arrayOf(idInscripcion.toString()),
        ).use { c ->
            if (c.moveToFirst()) c.getString(0) to c.getString(1) else null
        }

    // Nombre oficial del periodo a partir de sus fechas
    private fun periodoOficial(inicio: String, fin: String): String {
        fun parte(fecha: String): String {
            val partes = fecha.take(10).split("-")
            if (partes.size != 3) return fecha
            val indice = (partes[1].toIntOrNull() ?: 1) - 1
            val mes = MESES.getOrElse(indice) { "" }
            return "$mes ${partes[0]}".trim().uppercase(Locale.getDefault())
        }
        return "${parte(inicio)} - ${parte(fin)}"
    }

    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    private data class DatosConstancia(
        val idInscripcion: Int,
        val porcentaje: Double,
        val actividad: String,
        val inicio: String,
        val fin: String,
        val alumno: String,
        val matricula: String,
        val carrera: String,
    )
}
