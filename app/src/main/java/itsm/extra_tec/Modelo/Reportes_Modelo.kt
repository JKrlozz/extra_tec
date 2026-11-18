package itsm.extra_tec.Modelo

import android.database.sqlite.SQLiteDatabase
import java.util.Locale

// reportes para detectar problemas y tomar decisiones
object Reportes_Modelo {
    const val MINIMO_ASISTENCIA = 80.0

    // cada fila es columna y valor
    fun generar(
        tipo: String,
        desde: String,
        hasta: String,
        alTerminar: (Resultado_Modelo<List<Map<String, String>>>) -> Unit,
    ) {
        BaseDatos.consultar({ db ->
            var inicio = desde
            var fin = hasta
            if (inicio.isEmpty()) inicio = "1000-01-01"
            if (fin.isEmpty()) fin = "9999-12-31"
            if (inicio > fin) {
                throw ErrorDatos("La fecha inicial es posterior a la final.")
            }
            if (tipo == "actividad") {
                porActividad(db, inicio, fin)
            } else if (tipo == "alumno") {
                porAlumno(db, inicio, fin)
            } else if (tipo == "docente") {
                porDocente(db, inicio, fin)
            } else {
                throw ErrorDatos("El reporte debe ser por actividad, alumno o docente.")
            }
        }, alTerminar)
    }

    // actividades con mas problemas primero
    private fun porActividad(db: SQLiteDatabase, inicio: String, fin: String): List<Map<String, String>> {
        val cursor = db.rawQuery(
            """SELECT a.id, a.nombre AS actividad,
                      TRIM(COALESCE(d.nombres, '') || ' ' || COALESCE(d.apellido_paterno, '') || ' ' ||
                           COALESCE(d.apellido_materno, '')) AS docente,
                      a.cupo,
                      (SELECT COUNT(*) FROM inscripciones i
                        WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos,
                      (SELECT COUNT(*) FROM sesiones s
                        WHERE s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?) AS sesiones,
                      (SELECT COUNT(*) FROM sesiones s
                        WHERE s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?
                          AND NOT EXISTS (SELECT 1 FROM evidencias e
                                           WHERE e.id_actividad = a.id AND e.fecha = s.fecha)) AS sin_evidencia,
                      (SELECT MAX(s.fecha) FROM sesiones s
                        WHERE s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?) AS ultima,
                      (SELECT ROUND(100.0 * SUM(x.presente) / COUNT(x.id), 1)
                         FROM asistencias x JOIN sesiones s ON s.id = x.id_sesion
                        WHERE s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?) AS asistencia_pct
                 FROM actividades a
                 LEFT JOIN usuarios d ON d.id = a.id_docente
                WHERE a.activa = 1""",
            arrayOf(inicio, fin, inicio, fin, inicio, fin, inicio, fin),
        )
        val datos = mutableListOf<Array<String>>()
        val ids = mutableListOf<Int>()
        while (cursor.moveToNext()) {
            ids.add(cursor.entero("id"))
            datos.add(
                arrayOf(
                    cursor.texto("actividad"), cursor.texto("docente"), cursor.texto("cupo"),
                    cursor.texto("inscritos"), cursor.texto("sesiones"), cursor.texto("sin_evidencia"),
                    cursor.texto("ultima"), porcentaje(cursor.real("asistencia_pct"), cursor.isNull(cursor.getColumnIndex("asistencia_pct"))),
                ),
            )
        }
        cursor.close()

        val filas = mutableListOf<Map<String, String>>()
        for (i in ids.indices) {
            val d = datos[i]
            val enRiesgo = alumnosEnRiesgo(db, ids[i], inicio, fin)
            val inscritos = d[3].toInt()
            val sesiones = d[4].toInt()
            val sinEvidencia = d[5].toInt()

            val avisos = mutableListOf<String>()
            if (inscritos == 0) avisos.add("Sin alumnos inscritos")
            if (d[1].isBlank()) avisos.add("Sin docente asignado")
            if (inscritos > 0 && sesiones == 0) avisos.add("Sin pases de lista en el periodo")
            if (enRiesgo > 0) avisos.add("$enRiesgo alumno(s) por debajo del 80 %")
            if (sinEvidencia > 0) avisos.add("$sinEvidencia sesión(es) sin evidencia")

            val fila = linkedMapOf<String, String>()
            fila["actividad"] = d[0]
            fila["docente"] = d[1]
            fila["cupo"] = d[2]
            fila["inscritos"] = d[3]
            fila["ocupacion"] = if (d[2].toInt() > 0) ((inscritos * 100) / d[2].toInt()).toString() else "0"
            fila["sesiones"] = d[4]
            fila["ultima"] = d[6]
            fila["asistencia_pct"] = d[7]
            fila["en_riesgo"] = enRiesgo.toString()
            fila["sin_evidencia"] = d[5]
            fila["atencion"] = if (avisos.isEmpty()) "Sin pendientes" else avisos.joinToString("\n")
            fila["pendientes"] = avisos.size.toString()
            filas.add(fila)
        }
        filas.sortWith(
            compareByDescending<Map<String, String>> { it["pendientes"]!!.toInt() }
                .thenBy { it["actividad"]!!.lowercase() },
        )
        return filas
    }

    // alumnos de una actividad que no llegan al minimo
    private fun alumnosEnRiesgo(db: SQLiteDatabase, idActividad: Int, inicio: String, fin: String): Int {
        val cursor = db.rawQuery(
            """SELECT COUNT(DISTINCT s.id) AS sesiones, COALESCE(SUM(x.presente), 0) AS presentes
                 FROM inscripciones i
                 JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha BETWEEN ? AND ?
                 LEFT JOIN asistencias x ON x.id_sesion = s.id AND x.id_alumno = i.id_alumno
                WHERE i.id_actividad = ? AND i.estado = 'activa'
                GROUP BY i.id_alumno""",
            arrayOf(inicio, fin, idActividad.toString()),
        )
        var total = 0
        while (cursor.moveToNext()) {
            val sesiones = cursor.entero("sesiones")
            val presentes = cursor.entero("presentes")
            if (sesiones > 0 && presentes * 100.0 / sesiones < MINIMO_ASISTENCIA) {
                total++
            }
        }
        cursor.close()
        return total
    }

    // alumnos en riesgo primero
    private fun porAlumno(db: SQLiteDatabase, inicio: String, fin: String): List<Map<String, String>> {
        val cursor = db.rawQuery(
            """SELECT TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                           COALESCE(u.apellido_materno, '')) AS alumno,
                      al.matricula, a.nombre AS actividad,
                      COUNT(DISTINCT s.id) AS sesiones,
                      COALESCE(SUM(x.presente), 0) AS presentes
                 FROM alumnos al
                 JOIN usuarios u ON u.id = al.id_usuario
                 LEFT JOIN inscripciones i ON i.id_alumno = al.id_usuario AND i.estado = 'activa'
                 LEFT JOIN actividades a ON a.id = i.id_actividad AND a.activa = 1
                 LEFT JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?
                 LEFT JOIN asistencias x ON x.id_sesion = s.id AND x.id_alumno = al.id_usuario
                WHERE u.activo = 1
                GROUP BY u.id, a.id""",
            arrayOf(inicio, fin),
        )
        val filas = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val actividad = cursor.texto("actividad")
            val sesiones = cursor.entero("sesiones")
            val presentes = cursor.entero("presentes")
            val faltas = sesiones - presentes
            val pct = if (sesiones > 0) presentes * 100.0 / sesiones else 0.0

            var estado: String
            var prioridad: Int
            if (actividad.isEmpty()) {
                estado = "Sin actividad inscrita"
                prioridad = 1
            } else if (sesiones == 0) {
                estado = "Aún sin pases de lista"
                prioridad = 2
            } else if (pct < MINIMO_ASISTENCIA) {
                estado = "En riesgo: $faltas falta(s) en $sesiones sesión(es)"
                prioridad = 0
            } else {
                estado = "Cumple el 80 % para su constancia"
                prioridad = 3
            }

            val fila = linkedMapOf<String, String>()
            fila["alumno"] = cursor.texto("alumno")
            fila["matricula"] = cursor.texto("matricula")
            fila["actividad"] = actividad
            fila["sesiones"] = sesiones.toString()
            fila["presentes"] = presentes.toString()
            fila["faltas"] = faltas.toString()
            fila["asistencia_pct"] = if (sesiones > 0) porcentaje(pct, false) else ""
            fila["estado"] = estado
            fila["prioridad"] = prioridad.toString()
            fila["orden"] = String.format(Locale.US, "%06.2f", pct)
            filas.add(fila)
        }
        cursor.close()
        filas.sortWith(
            compareBy<Map<String, String>> { it["prioridad"]!!.toInt() }
                .thenBy { it["orden"]!! }
                .thenBy { it["alumno"]!!.lowercase() },
        )
        return filas
    }

    // docentes con pendientes primero
    private fun porDocente(db: SQLiteDatabase, inicio: String, fin: String): List<Map<String, String>> {
        val cursor = db.rawQuery(
            """SELECT TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                           COALESCE(u.apellido_materno, '')) AS docente,
                      (SELECT COUNT(*) FROM actividades a
                        WHERE a.id_docente = u.id AND a.activa = 1) AS actividades,
                      (SELECT COUNT(*) FROM sesiones s JOIN actividades a ON a.id = s.id_actividad
                        WHERE a.id_docente = u.id AND a.activa = 1
                          AND s.fecha BETWEEN ? AND ?) AS sesiones,
                      (SELECT COUNT(*) FROM sesiones s JOIN actividades a ON a.id = s.id_actividad
                        WHERE a.id_docente = u.id AND a.activa = 1 AND s.fecha BETWEEN ? AND ?
                          AND NOT EXISTS (SELECT 1 FROM evidencias e
                                           WHERE e.id_actividad = a.id AND e.fecha = s.fecha)) AS sin_evidencia,
                      (SELECT COUNT(*) FROM evidencias e JOIN actividades a ON a.id = e.id_actividad
                        WHERE a.id_docente = u.id AND a.activa = 1
                          AND e.fecha BETWEEN ? AND ?) AS evidencias,
                      (SELECT ROUND(100.0 * SUM(x.presente) / COUNT(x.id), 1)
                         FROM asistencias x
                         JOIN sesiones s ON s.id = x.id_sesion
                         JOIN actividades a ON a.id = s.id_actividad
                        WHERE a.id_docente = u.id AND a.activa = 1
                          AND s.fecha BETWEEN ? AND ?) AS asistencia_pct
                 FROM docentes dc JOIN usuarios u ON u.id = dc.id_usuario
                WHERE u.activo = 1""",
            arrayOf(inicio, fin, inicio, fin, inicio, fin, inicio, fin),
        )
        val filas = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val actividades = cursor.entero("actividades")
            val sesiones = cursor.entero("sesiones")
            val sinEvidencia = cursor.entero("sin_evidencia")

            var estado: String
            var prioridad: Int
            if (actividades == 0) {
                estado = "Sin actividades asignadas"
                prioridad = 1
            } else if (sinEvidencia > 0) {
                estado = "$sinEvidencia sesión(es) sin evidencia"
                prioridad = 0
            } else if (sesiones == 0) {
                estado = "Sin pases de lista en el periodo"
                prioridad = 2
            } else {
                estado = "Al corriente"
                prioridad = 3
            }

            val fila = linkedMapOf<String, String>()
            fila["docente"] = cursor.texto("docente")
            fila["actividades"] = actividades.toString()
            fila["sesiones"] = sesiones.toString()
            fila["sin_evidencia"] = sinEvidencia.toString()
            fila["evidencias"] = cursor.texto("evidencias")
            fila["asistencia_pct"] = porcentaje(cursor.real("asistencia_pct"), cursor.isNull(cursor.getColumnIndex("asistencia_pct")))
            fila["estado"] = estado
            fila["prioridad"] = prioridad.toString()
            fila["pendientes"] = sinEvidencia.toString()
            filas.add(fila)
        }
        cursor.close()
        filas.sortWith(
            compareBy<Map<String, String>> { it["prioridad"]!!.toInt() }
                .thenByDescending { it["pendientes"]!!.toInt() }
                .thenBy { it["docente"]!!.lowercase() },
        )
        return filas
    }

    // porcentaje con un decimal o vacio
    private fun porcentaje(valor: Double, vacio: Boolean): String {
        if (vacio) {
            return ""
        }
        if (valor % 1.0 == 0.0) {
            return valor.toInt().toString()
        }
        return String.format(Locale.US, "%.1f", valor)
    }
}
