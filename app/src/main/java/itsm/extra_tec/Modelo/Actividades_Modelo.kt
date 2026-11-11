package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.Calendar
import java.util.Locale

object Actividades_Modelo {
    private const val MINIMO_ASISTENCIA = 80.0
    private val FORMATO_HORA = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")

    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    // actividades que ve el alumno
    fun listar(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
                """SELECT v.*, EXISTS(SELECT 1 FROM inscripciones i
                                       WHERE i.id_actividad = v.id AND i.id_alumno = ? AND i.estado = 'activa') AS inscrito
                     FROM v_actividades v WHERE v.activa = 1 ORDER BY v.nombre""",
                arrayOf(Sesion_Actual.id().toString()),
            )
            leerActividades(db, cursor)
        }, alTerminar)
    }

    // actividades que ve el administrador
    fun todas(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
                "SELECT v.*, 0 AS inscrito FROM v_actividades v WHERE v.activa = 1 ORDER BY v.nombre",
                null,
            )
            leerActividades(db, cursor)
        }, alTerminar)
    }

    // actividades del docente
    fun misClases(alTerminar: (Resultado_Modelo<List<Actividad>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
                "SELECT v.*, 0 AS inscrito FROM v_actividades v WHERE v.id_docente = ? AND v.activa = 1 ORDER BY v.nombre",
                arrayOf(Sesion_Actual.id().toString()),
            )
            leerActividades(db, cursor)
        }, alTerminar)
    }

    fun detalle(id: Int, alTerminar: (Resultado_Modelo<DetalleActividad>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
                "SELECT v.*, 0 AS inscrito FROM v_actividades v WHERE v.id = ?",
                arrayOf(id.toString()),
            )
            val lista = leerActividades(db, cursor)
            if (lista.isEmpty()) {
                throw ErrorDatos("Actividad no encontrada.")
            }
            DetalleActividad(lista[0], horariosDe(db, id))
        }, alTerminar)
    }

    // dias y lugares para horarios
    fun catalogosHorario(alTerminar: (Resultado_Modelo<CatalogosHorario>) -> Unit) {
        BaseDatos.consultar({ db ->
            val dias = mutableListOf<DiaSemana>()
            val cursorDias = db.rawQuery("SELECT id, nombre FROM dias_semana ORDER BY orden", null)
            while (cursorDias.moveToNext()) {
                dias.add(DiaSemana(cursorDias.getInt(0), cursorDias.getString(1)))
            }
            cursorDias.close()

            val lugares = mutableListOf<Lugar>()
            val cursorLugares = db.rawQuery("SELECT id, nombre FROM lugares ORDER BY nombre", null)
            while (cursorLugares.moveToNext()) {
                lugares.add(Lugar(cursorLugares.getInt(0), cursorLugares.getString(1)))
            }
            cursorLugares.close()
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
            val error = Limites.revisarActividad(nombre, descripcion, cupo)
            if (error != null) {
                throw ErrorDatos(error)
            }
            validarHorarios(db, horarios)

            val valores = ContentValues()
            valores.put("nombre", nombre.trim())
            if (descripcion.isBlank()) {
                valores.putNull("descripcion")
            } else {
                valores.put("descripcion", descripcion.trim())
            }
            valores.put("cupo", cupo)
            valores.put("id_docente", idDocente)

            if (id == null) {
                valores.put("id_periodo", periodoActivo(db))
                val idNueva = db.insertOrThrow("actividades", null, valores).toInt()
                guardarHorarios(db, idNueva, horarios)
            } else {
                // el cupo no baja de inscritos
                val inscritos = contar(
                    db,
                    "SELECT COUNT(*) FROM inscripciones WHERE id_actividad = ? AND estado = 'activa'",
                    id,
                )
                if (cupo < inscritos) {
                    throw ErrorDatos("El cupo no puede ser menor a los $inscritos alumnos inscritos.")
                }
                val filas = db.update("actividades", valores, "id = ?", arrayOf(id.toString()))
                if (filas == 0) {
                    throw ErrorDatos("Actividad no encontrada.")
                }
                guardarHorarios(db, id, horarios)
            }
        }, alTerminar)
    }

    // baja logica para conservar historial
    fun eliminar(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val valores = ContentValues()
            valores.put("activa", 0)
            val filas = db.update("actividades", valores, "id = ?", arrayOf(id.toString()))
            if (filas == 0) {
                throw ErrorDatos("Actividad no encontrada.")
            }
        }, alTerminar)
    }

    fun inscribir(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val idAlumno = Sesion_Actual.id()
            val disponible = contar(db, "SELECT COUNT(*) FROM actividades WHERE id = ? AND activa = 1", idActividad)
            if (disponible == 0) {
                throw ErrorDatos("Actividad no disponible.")
            }

            // revisa si ya tiene otra actividad
            val cursorActual = db.rawQuery(
                """SELECT a.nombre FROM inscripciones i JOIN actividades a ON a.id = i.id_actividad
                    WHERE i.id_alumno = ? AND i.estado = 'activa' AND i.id_actividad <> ?""",
                arrayOf(idAlumno.toString(), idActividad.toString()),
            )
            var actual: String? = null
            if (cursorActual.moveToFirst()) {
                actual = cursorActual.getString(0)
            }
            cursorActual.close()
            if (actual != null) {
                throw ErrorDatos("Ya estás inscrito en $actual. Da de baja esa actividad antes de inscribirte en otra.")
            }

            // si ya estuvo se reactiva
            val cursorPrevia = db.rawQuery(
                "SELECT id, estado FROM inscripciones WHERE id_alumno = ? AND id_actividad = ?",
                arrayOf(idAlumno.toString(), idActividad.toString()),
            )
            var idPrevia = 0
            var estadoPrevio = ""
            if (cursorPrevia.moveToFirst()) {
                idPrevia = cursorPrevia.getInt(0)
                estadoPrevio = cursorPrevia.getString(1)
            }
            cursorPrevia.close()

            if (idPrevia == 0) {
                val valores = ContentValues()
                valores.put("id_alumno", idAlumno)
                valores.put("id_actividad", idActividad)
                db.insertOrThrow("inscripciones", null, valores)
            } else {
                if (estadoPrevio == "activa") {
                    throw ErrorDatos("Ya estás inscrito en esta actividad.")
                }
                val cupo = contar(db, "SELECT cupo FROM actividades WHERE id = ?", idActividad)
                val inscritos = contar(
                    db,
                    "SELECT COUNT(*) FROM inscripciones WHERE id_actividad = ? AND estado = 'activa'",
                    idActividad,
                )
                if (inscritos >= cupo) {
                    throw ErrorDatos("Cupo lleno para esta actividad.")
                }
                db.execSQL(
                    "UPDATE inscripciones SET estado = 'activa', fecha = datetime('now', 'localtime') WHERE id = ?",
                    arrayOf(idPrevia),
                )
            }
        }, alTerminar)
    }

    fun darDeBaja(idActividad: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val valores = ContentValues()
            valores.put("estado", "baja")
            val filas = db.update(
                "inscripciones",
                valores,
                "id_alumno = ? AND id_actividad = ? AND estado = 'activa'",
                arrayOf(Sesion_Actual.id().toString(), idActividad.toString()),
            )
            if (filas == 0) {
                throw ErrorDatos("No estás inscrito en esa actividad.")
            }
        }, alTerminar)
    }

    fun horarioAlumno(alTerminar: (Resultado_Modelo<List<HorarioAlumno>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
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
            )
            val lista = mutableListOf<HorarioAlumno>()
            while (cursor.moveToNext()) {
                lista.add(HorarioAlumno(cursor.entero("id_actividad"), cursor.texto("nombre"), leerHorario(cursor)))
            }
            cursor.close()
            lista
        }, alTerminar)
    }

    fun constancias(alTerminar: (Resultado_Modelo<List<Constancia>>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
                """SELECT a.id AS id_actividad, a.nombre, p.nombre AS periodo,
                          v.sesiones, v.asistencias, COALESCE(v.porcentaje, 0) AS porcentaje
                     FROM v_asistencia_alumno v
                     JOIN actividades a ON a.id = v.id_actividad
                     JOIN v_periodos p ON p.id = a.id_periodo
                    WHERE v.id_alumno = ? ORDER BY a.nombre""",
                arrayOf(Sesion_Actual.id().toString()),
            )
            val lista = mutableListOf<Constancia>()
            while (cursor.moveToNext()) {
                val porcentaje = cursor.real("porcentaje")
                val constancia = Constancia(
                    cursor.entero("id_actividad"), cursor.texto("nombre"), cursor.texto("periodo"),
                    cursor.entero("sesiones"), cursor.entero("asistencias"), porcentaje,
                    porcentaje >= MINIMO_ASISTENCIA,
                )
                lista.add(constancia)
            }
            cursor.close()
            lista
        }, alTerminar)
    }

    fun descargarConstancia(idActividad: Int, alTerminar: (Resultado_Modelo<Archivo>) -> Unit) {
        BaseDatos.escribir({ db ->
            val cursor = db.rawQuery(
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
            )
            if (!cursor.moveToFirst()) {
                cursor.close()
                throw ErrorDatos("No estás inscrito en esa actividad.")
            }
            val idInscripcion = cursor.entero("id_inscripcion")
            val porcentaje = cursor.real("porcentaje")
            val actividad = cursor.texto("actividad")
            val inicio = cursor.texto("inicio")
            val fin = cursor.texto("fin")
            val alumno = cursor.texto("alumno")
            val matricula = cursor.texto("matricula")
            val carrera = cursor.texto("carrera")
            cursor.close()

            if (porcentaje < MINIMO_ASISTENCIA) {
                val llevas = String.format(Locale.US, "%.0f", porcentaje)
                throw ErrorDatos("Se requiere 80% de asistencia (llevas $llevas%).")
            }

            // el folio se guarda una vez
            var folio = leerFolio(db, idInscripcion)
            if (folio == null) {
                val anio = Calendar.getInstance().get(Calendar.YEAR)
                val nuevo = "ITSM-$anio-" + idInscripcion.toString().padStart(5, '0')
                val valores = ContentValues()
                valores.put("id_inscripcion", idInscripcion)
                valores.put("folio", nuevo)
                db.insertOrThrow("constancias", null, valores)
                folio = leerFolio(db, idInscripcion)
            }
            if (folio == null) {
                throw ErrorDatos("No se pudo registrar la constancia.")
            }

            var nivel = "NOTABLE"
            var valor = "3"
            if (porcentaje >= 95.0) {
                nivel = "SOBRESALIENTE"
                valor = "4"
            }

            // fecha de emision en partes
            val emision = folio[1].take(10).split("-")
            var dia = "1"
            var mes = "enero"
            var anio = ""
            if (emision.size == 3) {
                anio = emision[0]
                mes = nombreMes(emision[1])
                dia = (emision[2].toIntOrNull() ?: 1).toString()
            }
            val folioCorto = folio[0].substringAfterLast('-')

            val datos = ConstanciaPdf(
                alumno = alumno,
                matricula = matricula,
                carrera = carrera,
                actividad = actividad,
                periodo = periodoOficial(inicio) + " - " + periodoOficial(fin),
                nivel = nivel,
                valor = valor,
                creditos = "1",
                folio = folioCorto,
                dia = dia,
                mes = mes,
                anio = anio,
            )
            Archivo(Archivos_Modelo.constanciaPdf(datos), "constancia-" + folio[0] + ".pdf")
        }, alTerminar)
    }

    fun alumnosDeActividad(idActividad: Int, alTerminar: (Resultado_Modelo<List<AlumnoInscrito>>) -> Unit) {
        BaseDatos.consultar({ db ->
            if (Sesion_Actual.rol() == "docente") {
                val esTitular = contar(
                    db,
                    "SELECT COUNT(*) FROM actividades WHERE id = ? AND id_docente = " + Sesion_Actual.id(),
                    idActividad,
                )
                if (esTitular == 0) {
                    throw ErrorDatos("No eres el docente de esta actividad.")
                }
            }
            val cursor = db.rawQuery(
                """SELECT u.id, TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                          COALESCE(u.apellido_materno, '')) AS nombre, a.matricula
                     FROM inscripciones i
                     JOIN alumnos a ON a.id_usuario = i.id_alumno
                     JOIN usuarios u ON u.id = a.id_usuario
                    WHERE i.id_actividad = ? AND i.estado = 'activa'
                    ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                arrayOf(idActividad.toString()),
            )
            val lista = mutableListOf<AlumnoInscrito>()
            while (cursor.moveToNext()) {
                lista.add(AlumnoInscrito(cursor.entero("id"), cursor.texto("nombre"), cursor.texto("matricula")))
            }
            cursor.close()
            lista
        }, alTerminar)
    }

    // lee actividades con su horario
    private fun leerActividades(db: SQLiteDatabase, cursor: Cursor): List<Actividad> {
        val lista = mutableListOf<Actividad>()
        while (cursor.moveToNext()) {
            val id = cursor.entero("id")
            val horarios = horariosDe(db, id)
            var textoHorario = ""
            var textoLugar = ""
            for (h in horarios) {
                if (textoHorario.isNotEmpty()) textoHorario += "\n"
                textoHorario += h.diaSemana.take(3) + " " + h.horaInicio + " a " + h.horaFin
                if (!textoLugar.contains(h.lugar)) {
                    if (textoLugar.isNotEmpty()) textoLugar += "\n"
                    textoLugar += h.lugar
                }
            }
            val actividad = Actividad(
                id = id,
                nombre = cursor.texto("nombre"),
                descripcion = cursor.texto("descripcion"),
                cupo = cursor.entero("cupo"),
                inscritos = cursor.entero("inscritos"),
                horario = textoHorario,
                lugar = textoLugar,
                docente = cursor.texto("docente"),
                idDocente = cursor.enteroONulo("id_docente"),
                inscrito = cursor.entero("inscrito") != 0,
            )
            lista.add(actividad)
        }
        cursor.close()
        return lista
    }

    private fun horariosDe(db: SQLiteDatabase, idActividad: Int): List<HorarioDia> {
        val cursor = db.rawQuery(
            """SELECT h.id_dia, d.nombre AS dia_semana, h.hora_inicio, h.hora_fin,
                      h.id_lugar, l.nombre AS lugar
                 FROM horarios h
                 JOIN dias_semana d ON d.id = h.id_dia
                 JOIN lugares l ON l.id = h.id_lugar
                WHERE h.id_actividad = ?
                ORDER BY d.orden, h.hora_inicio""",
            arrayOf(idActividad.toString()),
        )
        val lista = mutableListOf<HorarioDia>()
        while (cursor.moveToNext()) {
            lista.add(leerHorario(cursor))
        }
        cursor.close()
        return lista
    }

    private fun leerHorario(cursor: Cursor): HorarioDia {
        return HorarioDia(
            cursor.entero("id_dia"), cursor.texto("dia_semana"),
            cursor.texto("hora_inicio").take(5), cursor.texto("hora_fin").take(5),
            cursor.entero("id_lugar"), cursor.texto("lugar"),
        )
    }

    private fun validarHorarios(db: SQLiteDatabase, horarios: List<HorarioDia>) {
        if (horarios.isEmpty()) {
            throw ErrorDatos("Indica al menos un horario.")
        }
        if (horarios.size > 7) {
            throw ErrorDatos("Puedes registrar hasta 7 horarios.")
        }
        for (i in horarios.indices) {
            val h = horarios[i]
            val existeDia = contar(db, "SELECT COUNT(*) FROM dias_semana WHERE id = ?", h.idDia)
            if (existeDia == 0 || !FORMATO_HORA.matches(h.horaInicio) || !FORMATO_HORA.matches(h.horaFin)) {
                throw ErrorDatos("Horario inválido: revisa el día y las horas.")
            }
            if (h.horaFin <= h.horaInicio) {
                throw ErrorDatos("La hora de fin debe ser posterior a la de inicio.")
            }
            val errorLugar = Limites.revisarLugar(h.lugar)
            if (errorLugar != null) {
                throw ErrorDatos(errorLugar)
            }
            // revisa choques con los anteriores
            for (j in 0 until i) {
                val otro = horarios[j]
                if (otro.idDia == h.idDia && h.horaInicio < otro.horaFin && otro.horaInicio < h.horaFin) {
                    throw ErrorDatos("Hay dos horarios traslapados el mismo día.")
                }
            }
        }
    }

    private fun guardarHorarios(db: SQLiteDatabase, idActividad: Int, horarios: List<HorarioDia>) {
        db.delete("horarios", "id_actividad = ?", arrayOf(idActividad.toString()))
        for (h in horarios) {
            val valores = ContentValues()
            valores.put("id_actividad", idActividad)
            valores.put("id_dia", h.idDia)
            valores.put("hora_inicio", h.horaInicio)
            valores.put("hora_fin", h.horaFin)
            valores.put("id_lugar", buscarLugar(db, h.lugar))
            db.insertOrThrow("horarios", null, valores)
        }
    }

    // busca el lugar o lo crea
    private fun buscarLugar(db: SQLiteDatabase, nombre: String): Int {
        val texto = nombre.trim()
        val cursor = db.rawQuery("SELECT id FROM lugares WHERE nombre = ?", arrayOf(texto))
        var id = 0
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        if (id != 0) {
            return id
        }
        val valores = ContentValues()
        valores.put("nombre", texto)
        return db.insertOrThrow("lugares", null, valores).toInt()
    }

    private fun periodoActivo(db: SQLiteDatabase): Int {
        val cursor = db.rawQuery("SELECT id FROM periodos WHERE activo = 1", null)
        var id = 0
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        if (id == 0) {
            throw ErrorDatos("No hay un periodo activo.")
        }
        return id
    }

    // regresa folio y fecha de emision
    private fun leerFolio(db: SQLiteDatabase, idInscripcion: Int): List<String>? {
        val cursor = db.rawQuery(
            "SELECT folio, emitida_en FROM constancias WHERE id_inscripcion = ?",
            arrayOf(idInscripcion.toString()),
        )
        var folio: List<String>? = null
        if (cursor.moveToFirst()) {
            folio = listOf(cursor.getString(0), cursor.getString(1))
        }
        cursor.close()
        return folio
    }

    // un numero sacado de una consulta
    private fun contar(db: SQLiteDatabase, sql: String, id: Int): Int {
        val cursor = db.rawQuery(sql, arrayOf(id.toString()))
        var numero = 0
        if (cursor.moveToFirst()) {
            numero = cursor.getInt(0)
        }
        cursor.close()
        return numero
    }

    // por ejemplo agosto 2026
    private fun periodoOficial(fecha: String): String {
        val partes = fecha.take(10).split("-")
        if (partes.size != 3) {
            return fecha
        }
        val texto = nombreMes(partes[1]) + " " + partes[0]
        return texto.uppercase(Locale.getDefault())
    }

    private fun nombreMes(numero: String): String {
        var mes = numero.toIntOrNull() ?: 1
        if (mes < 1 || mes > 12) {
            mes = 1
        }
        return MESES[mes - 1]
    }
}
