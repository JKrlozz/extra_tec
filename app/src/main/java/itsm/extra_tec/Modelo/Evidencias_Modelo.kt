package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object Evidencias_Modelo {
    // valor guardado y texto visible
    val ESTADOS = listOf(
        listOf("realizada", "Realizada"),
        listOf("suspendida", "Suspendida"),
        listOf("no_realizada", "No realizada"),
    )

    private val TIPOS_VALIDOS = listOf("image/jpeg", "image/png", "image/webp")

    fun historial(idActividad: Int, alTerminar: (Resultado_Modelo<List<Evidencia>>) -> Unit) {
        BaseDatos.consultar({ db ->
            exigirTitular(db, idActividad)
            val cursor = db.rawQuery(
                """SELECT id, id_actividad, fecha, creada_en, actualizada_en, estado,
                          COALESCE(descripcion, '') AS descripcion
                     FROM evidencias
                    WHERE id_actividad = ?
                    ORDER BY fecha DESC, creada_en DESC, id DESC""",
                arrayOf(idActividad.toString()),
            )
            val lista = mutableListOf<Evidencia>()
            while (cursor.moveToNext()) {
                lista.add(leerEvidencia(db, cursor))
            }
            cursor.close()
            lista
        }, alTerminar)
    }

    fun detalle(id: Int, alTerminar: (Resultado_Modelo<Evidencia>) -> Unit) {
        BaseDatos.consultar({ db ->
            val cursor = db.rawQuery(
                """SELECT id, id_actividad, fecha, creada_en, actualizada_en, estado,
                          COALESCE(descripcion, '') AS descripcion
                     FROM evidencias WHERE id = ?""",
                arrayOf(id.toString()),
            )
            if (!cursor.moveToFirst()) {
                cursor.close()
                throw ErrorDatos("Evidencia no encontrada.")
            }
            val evidencia = leerEvidencia(db, cursor)
            cursor.close()
            exigirTitular(db, evidencia.idActividad)
            evidencia
        }, alTerminar)
    }

    // crea o cambia una evidencia
    // guarda y borra las fotos
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
            revisar(Limites.revisarFecha(fecha))
            revisar(Limites.revisarDescripcionEvidencia(descripcion))
            var estadoValido = false
            for (opcion in ESTADOS) {
                if (opcion[0] == estado) estadoValido = true
            }
            if (!estadoValido) {
                throw ErrorDatos("Selecciona un estado válido para la evidencia.")
            }
            for (foto in nuevas) {
                if (!TIPOS_VALIDOS.contains(foto.tipoMime)) {
                    throw ErrorDatos("Solo se aceptan imágenes JPG, PNG o WebP.")
                }
                if (foto.datos.isEmpty()) {
                    throw ErrorDatos("Falta una fotografía.")
                }
                if (foto.datos.size > Limites.FOTO_BYTES_MAX) {
                    throw ErrorDatos("Una fotografía supera 8 MB.")
                }
            }

            var existentes = listOf<EvidenciaFoto>()
            if (idEvidencia != null) {
                val pertenece = db.rawQuery(
                    "SELECT 1 FROM evidencias WHERE id = ? AND id_actividad = ?",
                    arrayOf(idEvidencia.toString(), idActividad.toString()),
                )
                val existe = pertenece.moveToFirst()
                pertenece.close()
                if (!existe) {
                    throw ErrorDatos("Evidencia no encontrada.")
                }
                existentes = fotosDe(db, idEvidencia)
            }

            // cuantas fotos quedan al final
            val aQuitar = mutableListOf<EvidenciaFoto>()
            for (foto in existentes) {
                if (quitar.contains(foto.id)) aQuitar.add(foto)
            }
            val quedan = existentes.size - aQuitar.size + nuevas.size
            if (estado == "realizada" && quedan == 0) {
                throw ErrorDatos("Selecciona al menos una fotografía para una actividad realizada.")
            }
            if (quedan > Limites.FOTOS_MAX) {
                throw ErrorDatos("Puedes guardar hasta ${Limites.FOTOS_MAX} fotografías por evidencia.")
            }

            val valores = ContentValues()
            valores.put("fecha", fecha)
            valores.put("estado", estado)
            if (descripcion.isBlank()) {
                valores.putNull("descripcion")
            } else {
                valores.put("descripcion", descripcion.trim())
            }

            val creados = mutableListOf<File>()
            try {
                var id = 0
                if (idEvidencia == null) {
                    valores.put("id_actividad", idActividad)
                    if (docenteTitular == null) {
                        valores.putNull("id_docente")
                    } else {
                        valores.put("id_docente", docenteTitular)
                    }
                    id = db.insertOrThrow("evidencias", null, valores).toInt()
                } else {
                    valores.put("actualizada_en", ahora())
                    db.update("evidencias", valores, "id = ?", arrayOf(idEvidencia.toString()))
                    id = idEvidencia
                }

                for (foto in aQuitar) {
                    db.delete("evidencia_fotos", "id = ?", arrayOf(foto.id.toString()))
                }

                // las fotos nuevas van al final
                var orden = 0
                val cursor = db.rawQuery(
                    "SELECT COALESCE(MAX(orden), -1) + 1 FROM evidencia_fotos WHERE id_evidencia = ?",
                    arrayOf(id.toString()),
                )
                if (cursor.moveToFirst()) {
                    orden = cursor.getInt(0)
                }
                cursor.close()

                for (foto in nuevas) {
                    val archivo = guardarArchivo(foto)
                    creados.add(archivo)
                    val fila = ContentValues()
                    fila.put("id_evidencia", id)
                    fila.put("archivo", archivo.name)
                    fila.put("orden", orden)
                    db.insertOrThrow("evidencia_fotos", null, fila)
                    orden = orden + 1
                }
            } catch (e: Exception) {
                // si falla se borran archivos
                for (archivo in creados) {
                    archivo.delete()
                }
                throw e
            }

            for (foto in aQuitar) {
                File(foto.ruta).delete()
            }
        }, alTerminar)
    }

    fun eliminar(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        BaseDatos.escribir({ db ->
            val cursor = db.rawQuery("SELECT id_actividad FROM evidencias WHERE id = ?", arrayOf(id.toString()))
            var idActividad = 0
            if (cursor.moveToFirst()) {
                idActividad = cursor.getInt(0)
            }
            cursor.close()
            if (idActividad == 0) {
                throw ErrorDatos("Evidencia no encontrada.")
            }
            exigirTitular(db, idActividad)

            val fotos = fotosDe(db, id)
            db.delete("evidencias", "id = ?", arrayOf(id.toString()))
            for (foto in fotos) {
                File(foto.ruta).delete()
            }
        }, alTerminar)
    }

    private fun leerEvidencia(db: SQLiteDatabase, cursor: Cursor): Evidencia {
        val id = cursor.entero("id")
        var actualizada: String? = cursor.texto("actualizada_en")
        if (actualizada == "") {
            actualizada = null
        }
        return Evidencia(
            id = id,
            idActividad = cursor.entero("id_actividad"),
            fecha = cursor.texto("fecha"),
            creadaEn = cursor.texto("creada_en"),
            actualizadaEn = actualizada,
            estado = cursor.texto("estado"),
            descripcion = cursor.texto("descripcion"),
            fotos = fotosDe(db, id),
        )
    }

    // la ruta se arma aqui
    private fun fotosDe(db: SQLiteDatabase, idEvidencia: Int): List<EvidenciaFoto> {
        val cursor = db.rawQuery(
            "SELECT id, archivo FROM evidencia_fotos WHERE id_evidencia = ? ORDER BY orden, id",
            arrayOf(idEvidencia.toString()),
        )
        val lista = mutableListOf<EvidenciaFoto>()
        while (cursor.moveToNext()) {
            val ruta = File(carpetaFotos(), cursor.getString(1)).absolutePath
            lista.add(EvidenciaFoto(cursor.getInt(0), ruta))
        }
        cursor.close()
        return lista
    }

    private fun carpetaFotos(): File {
        val carpeta = File(BaseDatos.contexto.filesDir, "evidencias")
        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }
        return carpeta
    }

    private fun guardarArchivo(foto: FotoNueva): File {
        var extension = "jpg"
        if (foto.tipoMime == "image/png") extension = "png"
        if (foto.tipoMime == "image/webp") extension = "webp"
        val archivo = File(carpetaFotos(), UUID.randomUUID().toString() + "." + extension)
        archivo.writeBytes(foto.datos)
        return archivo
    }

    // revisa que la actividad exista
    // el docente solo ve sus actividades
    private fun exigirTitular(db: SQLiteDatabase, idActividad: Int): Int? {
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
        return docente
    }

    private fun revisar(error: String?) {
        if (error != null) {
            throw ErrorDatos(error)
        }
    }

    private fun ahora(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return formato.format(Date())
    }
}
