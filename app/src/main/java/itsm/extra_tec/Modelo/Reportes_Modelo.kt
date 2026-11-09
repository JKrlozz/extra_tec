package itsm.extra_tec.Modelo

import android.database.Cursor
import java.util.Locale

object Reportes_Modelo {

    // Cada fila es un mapa de columna a valor
    fun generar(
        tipo: String,
        desde: String,
        hasta: String,
        alTerminar: (Resultado_Modelo<List<Map<String, String>>>) -> Unit,
    ) {
        BaseDatos.consultar({ db ->
            val inicio = desde.ifEmpty { "1000-01-01" }
            val fin = hasta.ifEmpty { "9999-12-31" }
            if (inicio > fin) throw ErrorDatos("La fecha inicial es posterior a la final.")

            when (tipo) {
                "actividad" -> db.rawQuery(
                    """SELECT a.id, a.nombre AS actividad,
                              TRIM(COALESCE(d.nombres, '') || ' ' || COALESCE(d.apellido_paterno, '') || ' ' ||
                                   COALESCE(d.apellido_materno, '')) AS docente,
                              a.cupo,
                              (SELECT COUNT(*) FROM inscripciones i
                                WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos,
                              COUNT(DISTINCT s.id) AS sesiones,
                              ROUND(100.0 * COALESCE(SUM(asi.presente), 0) / NULLIF(COUNT(asi.id), 0), 1) AS asistencia_pct
                         FROM actividades a
                         LEFT JOIN usuarios d ON d.id = a.id_docente
                         LEFT JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?
                         LEFT JOIN asistencias asi ON asi.id_sesion = s.id
                        WHERE a.activa = 1
                        GROUP BY a.id
                        ORDER BY a.nombre""",
                    arrayOf(inicio, fin),
                ).use { c ->
                    buildList {
                        while (c.moveToNext()) add(
                            linkedMapOf(
                                "id" to c.texto("id"),
                                "actividad" to c.texto("actividad"),
                                "docente" to c.texto("docente"),
                                "cupo" to c.texto("cupo"),
                                "inscritos" to c.texto("inscritos"),
                                "sesiones" to c.texto("sesiones"),
                                "asistencia_pct" to c.porcentaje("asistencia_pct"),
                            )
                        )
                    }
                }

                "alumno" -> db.rawQuery(
                    """SELECT u.id,
                              TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                                   COALESCE(u.apellido_materno, '')) AS alumno,
                              al.matricula,
                              COUNT(DISTINCT i.id_actividad) AS actividades,
                              ROUND(100.0 * COALESCE(SUM(asi.presente), 0) / NULLIF(COUNT(asi.id), 0), 1) AS asistencia_pct
                         FROM alumnos al
                         JOIN usuarios u ON u.id = al.id_usuario
                         LEFT JOIN inscripciones i ON i.id_alumno = al.id_usuario AND i.estado = 'activa'
                         LEFT JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha BETWEEN ? AND ?
                         LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = al.id_usuario
                        WHERE u.activo = 1
                        GROUP BY u.id
                        ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                    arrayOf(inicio, fin),
                ).use { c ->
                    buildList {
                        while (c.moveToNext()) add(
                            linkedMapOf(
                                "id" to c.texto("id"),
                                "alumno" to c.texto("alumno"),
                                "matricula" to c.texto("matricula"),
                                "actividades" to c.texto("actividades"),
                                "asistencia_pct" to c.porcentaje("asistencia_pct"),
                            )
                        )
                    }
                }

                "docente" -> db.rawQuery(
                    """SELECT u.id,
                              TRIM(COALESCE(u.nombres, '') || ' ' || COALESCE(u.apellido_paterno, '') || ' ' ||
                                   COALESCE(u.apellido_materno, '')) AS docente,
                              COUNT(DISTINCT a.id) AS actividades,
                              COUNT(DISTINCT s.id) AS sesiones_impartidas,
                              (SELECT COUNT(*) FROM evidencias e
                                WHERE e.id_docente = u.id AND DATE(e.creada_en) BETWEEN ? AND ?) AS evidencias
                         FROM docentes d
                         JOIN usuarios u ON u.id = d.id_usuario
                         LEFT JOIN actividades a ON a.id_docente = d.id_usuario AND a.activa = 1
                         LEFT JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?
                        WHERE u.activo = 1
                        GROUP BY u.id
                        ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres""",
                    arrayOf(inicio, fin, inicio, fin),
                ).use { c ->
                    buildList {
                        while (c.moveToNext()) add(
                            linkedMapOf(
                                "id" to c.texto("id"),
                                "docente" to c.texto("docente"),
                                "actividades" to c.texto("actividades"),
                                "sesiones_impartidas" to c.texto("sesiones_impartidas"),
                                "evidencias" to c.texto("evidencias"),
                            )
                        )
                    }
                }

                else -> throw ErrorDatos("El reporte debe ser por actividad, alumno o docente.")
            }
        }, alTerminar)
    }

    // Porcentaje con un decimal o vacio si no hay datos
    private fun Cursor.porcentaje(columna: String): String {
        val indice = getColumnIndex(columna)
        if (indice < 0 || isNull(indice)) return ""
        val valor = getDouble(indice)
        return if (valor % 1.0 == 0.0) valor.toInt().toString()
        else String.format(Locale.US, "%.1f", valor)
    }
}
