package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object Evidencias_Modelo {
    const val MAX_FOTOS = 10

    val ESTADOS = listOf(
        "realizada" to "Realizada",
        "suspendida" to "Suspendida",
        "no_realizada" to "No realizada",
    )

    private const val LIMITE_BYTES = 8 * 1024 * 1024
    private val MIME_VALIDO = Regex("^image/(jpeg|png|webp)$")
    private val FORMATO_FECHA = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun historial(idActividad: Int, alTerminar: (Resultado_Modelo<List<Evidencia>>) -> Unit) {
        BaseDatos.consultar({ db ->
            exigirTitular(db, idActividad)
            val fotos = fotosPorEvidencia(db, idActividad)
            db.rawQuery(
                """SELECT id, id_actividad, fecha, creada_en, actualizada_en, estado,
                          COALESCE(descripcion, '') AS descripcion
                     FROM evidencias
                    WHERE id_actividad = ?
                    ORDER BY fecha DESC, creada_en DESC, id DESC""",
                arrayOf(idActividad.toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val id = c.entero("id")
                        add(
                            Evidencia(
                                id = id,
                                idActividad = c.entero("id_actividad"),
                                fecha = c.texto("fecha"),
                                creadaEn = c.texto("creada_en"),
                                actualizadaEn = c.texto("actualizada_en").ifBlank { null },
                                estado = c.texto("estado"),
                                descripcion = c.texto("descripcion"),
                                fotos = fotos[id].orEmpty(),
                            )
                        )
                    }
                }
            }
        }, alTerminar)
    }

    fun detalle(id: Int, alTerminar: (Resultado_Modelo<Evidencia>) -> Unit) {
        BaseDatos.consultar({ db ->
            val evidencia = db.rawQuery(
                """SELECT id, id_actividad, fecha, creada_en, actualizada_en, estado,
                          COALESCE(descripcion, '') AS descripcion
                     FROM evidencias WHERE id = ?""",
                arrayOf(id.toString()),
            ).use { c ->
                if (!c.moveToFirst()) throw ErrorDatos("Evidencia no encontrada.")
                exigirTitular(db, c.entero("id_actividad"))
                Evidencia(
                    id = c.entero("id"),
                    idActividad = c.entero("id_actividad"),
                    fecha = c.texto("fecha"),
                    creadaEn = c.texto("creada_en"),
                    actualizadaEn = c.texto("actualizada_en").ifBlank { null },
                    estado = c.texto("estado"),
                    descripcion = c.texto("descripcion"),
                    fotos = fotosDe(db, id),
                )
            }
            evidencia
        }, alTerminar)
    }

    // Crea o actualiza una evidencia
    // Guarda las fotos nuevas y borra las que se quitan
    fun guardar(
        idActividad: Int,
        idEvidencia: Int?,
        fecha: String,
        estado: String,
        descripcion: String,
        nuevas: List<FotoNueva>,
        quitar: List<Int>,
        alTerminar: (Resultado_Modelo<Unit>) -> Unit,
    ) {
        BaseDatos.escribir({ db ->
            val docenteTitular = exigirTitular(db, idActividad)
            if (!FORMATO_FECHA.matches(fecha)) {
                throw ErrorDatos("La fecha debe tener el formato AAAA-MM-DD.")
            }
            if (ESTADOS.none { it.first == estado }) {
                throw ErrorDatos("Selecciona un estado válido para la evidencia.")
            }
            nuevas.forEach { foto ->
                if (!MIME_VALIDO.matches(foto.tipoMime)) {
                    throw ErrorDatos("Solo se aceptan imágenes JPG, PNG o WebP.")
                }
                if (foto.datos.isEmpty()) throw ErrorDatos("Falta una fotografía.")
                if (foto.datos.size > LIMITE_BYTES) throw ErrorDatos("Una fotografía supera 8 MB.")
            }

            val existentes = if (idEvidencia == null) emptyList() else fotosDe(db, idEvidencia)
            if (idEvidencia != null && !perteneceA(db, idEvidencia, idActividad)) {
                throw ErrorDatos("Evidencia no encontrada.")
            }
            val idsQuitar = existentes.map { it.id }.filter { it in quitar }
            val quedan = existentes.size - idsQuitar.size + nuevas.size
            if (estado == "realizada" && quedan <= 0) {
                throw ErrorDatos("Selecciona al menos una fotografía para una actividad realizada.")
            }
            if (quedan > MAX_FOTOS) {
                throw ErrorDatos("Puedes guardar hasta $MAX_FOTOS fotografías por evidencia.")
            }

            val creados = mutableListOf<File>()
            val aBorrar = mutableListOf<File>()
            try {
                val id: Int = if (idEvidencia == null) {
                    db.insertOrThrow("evidencias", null, ContentValues().apply {
                        put("id_actividad", idActividad)
                        if (docenteTitular == null) putNull("id_docente") else put("id_docente", docenteTitular)
                        put("fecha", fecha)
                        put("estado", estado)
                        if (descripcion.isBlank()) putNull("descripcion") else put("descripcion", descripcion.trim())
                    }).toInt()
                } else {
                    val filas = db.update(
                        "evidencias",
                        ContentValues().apply {
                            put("fecha", fecha)
                            put("estado", estado)
                            if (descripcion.isBlank()) putNull("descripcion") else put("descripcion", descripcion.trim())
                            put("actualizada_en", ahora())
                        },
                        "id = ? AND id_actividad = ?",
                        arrayOf(idEvidencia.toString(), idActividad.toString()),
                    )
                    if (filas == 0) throw ErrorDatos("Evidencia no encontrada.")
                    idEvidencia
                }

                idsQuitar.forEach { idFoto ->
                    val ruta = existentes.firstOrNull { it.id == idFoto }?.ruta
                    db.delete("evidencia_fotos", "id = ? AND id_evidencia = ?", arrayOf(idFoto.toString(), id.toString()))
                    if (ruta != null) aBorrar.add(File(ruta))
                }

                var orden = db.rawQuery(
                    "SELECT COALESCE(MAX(orden), -1) + 1 FROM evidencia_fotos WHERE id_evidencia = ?",
                    arrayOf(id.toString()),
                ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

                nuevas.forEach { foto ->
                    val archivo = guardarArchivo(foto)
                    creados.add(archivo)
                    db.insertOrThrow("evidencia_fotos", null, ContentValues().apply {
                        put("id_evidencia", id)
                        put("ruta_foto", archivo.absolutePath)
                        put("orden", orden++)
                    })
                }
            } catch (e: Exception) {
                creados.forEach { it.delete() }
                throw e
            }
            aBorrar.forEach { it.delete() }
            Unit
        }, alTerminar)
    }

    fun eliminar(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val idActividad = db.rawQuery(
                "SELECT id_actividad FROM evidencias WHERE id = ?",
                arrayOf(id.toString()),
            ).use { c -> if (c.moveToFirst()) c.getInt(0) else null }
                ?: throw ErrorDatos("Evidencia no encontrada.")
            exigirTitular(db, idActividad)

            val rutas = fotosDe(db, id).map { it.ruta }
            val filas = db.delete("evidencias", "id = ?", arrayOf(id.toString()))
            if (filas == 0) throw ErrorDatos("Evidencia no encontrada.")
            rutas.forEach { File(it).delete() }
            Unit
        }, alTerminar)
    }


    private fun fotosPorEvidencia(db: SQLiteDatabase, idActividad: Int): Map<Int, List<EvidenciaFoto>> {
        val mapa = LinkedHashMap<Int, MutableList<EvidenciaFoto>>()
        db.rawQuery(
            """SELECT f.id, f.id_evidencia, f.ruta_foto
                 FROM evidencia_fotos f
                 JOIN evidencias e ON e.id = f.id_evidencia
                WHERE e.id_actividad = ?
                ORDER BY f.orden, f.id""",
            arrayOf(idActividad.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                mapa.getOrPut(c.entero("id_evidencia")) { mutableListOf() }
                    .add(EvidenciaFoto(c.entero("id"), c.texto("ruta_foto")))
            }
        }
        return mapa
    }

    private fun fotosDe(db: SQLiteDatabase, idEvidencia: Int): List<EvidenciaFoto> =
        db.rawQuery(
            "SELECT id, ruta_foto FROM evidencia_fotos WHERE id_evidencia = ? ORDER BY orden, id",
            arrayOf(idEvidencia.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(EvidenciaFoto(c.entero("id"), c.texto("ruta_foto")))
            }
        }

    private fun perteneceA(db: SQLiteDatabase, idEvidencia: Int, idActividad: Int): Boolean =
        db.rawQuery(
            "SELECT 1 FROM evidencias WHERE id = ? AND id_actividad = ?",
            arrayOf(idEvidencia.toString(), idActividad.toString()),
        ).use { it.moveToFirst() }

    private fun guardarArchivo(foto: FotoNueva): File {
        val extension = foto.nombre.substringAfterLast('.', "jpg").lowercase(Locale.US)
            .takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
        val carpeta = File(BaseDatos.contexto.filesDir, "evidencias").apply { mkdirs() }
        val archivo = File(carpeta, "${UUID.randomUUID()}.$extension")
        archivo.writeBytes(foto.datos)
        return archivo
    }

    // Comprueba que la actividad exista y que el docente sea el titular
    // Devuelve el docente de la actividad
    private fun exigirTitular(db: SQLiteDatabase, idActividad: Int): Int? {
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
        return docente
    }

    private fun ahora(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}
