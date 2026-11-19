package itsm.extra_tec.Modelo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object Evidencias_Modelo {
    // valor guardado y texto visible
    val ESTADOS = listOf(
        listOf("realizada", "Realizada"),
        listOf("suspendida", "Suspendida"),
        listOf("no_realizada", "No realizada"),
    )

    private val TIPOS_VALIDOS = listOf("image/jpeg", "image/png", "image/webp")

    // las fotos de camara se achican antes de subirlas
    private const val LADO_MAXIMO = 1600
    private const val PESO_SIN_REDUCIR = 1_200_000

    fun historial(idActividad: Int, alTerminar: (Resultado_Modelo<List<Evidencia>>) -> Unit) {
        Red.trabajo({
            Red.solicitud("GET", "/actividades/$idActividad/evidencias").arreglo().lista { evidenciaDe(it) }
        }, alTerminar)
    }

    fun detalle(id: Int, alTerminar: (Resultado_Modelo<Evidencia>) -> Unit) {
        Red.trabajo({ evidenciaDe(Red.solicitud("GET", "/evidencias/$id").objeto()) }, alTerminar)
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
        Red.trabajo({
            for (foto in nuevas) {
                if (!TIPOS_VALIDOS.contains(foto.tipoMime)) {
                    throw ErrorDatos("Solo se aceptan imágenes JPG, PNG o WebP.")
                }
                if (foto.datos.isEmpty()) {
                    throw ErrorDatos("Falta una fotografía.")
                }
            }
            val datos = JSONObject()
            datos.put("fecha", fecha)
            datos.put("estado", estado)
            datos.put("descripcion", descripcion)
            val ids = JSONArray()
            for (id in quitar) {
                ids.put(id)
            }
            datos.put("quitar", ids)

            val listas = mutableListOf<FotoNueva>()
            for (foto in nuevas) {
                listas.add(reducir(foto))
            }
            if (idEvidencia == null) {
                Red.multiparte("POST", "/actividades/$idActividad/evidencias", datos, listas)
            } else {
                Red.multiparte("PUT", "/evidencias/$idEvidencia", datos, listas)
            }
            Unit
        }, alTerminar)
    }

    fun eliminar(id: Int, alTerminar: (Resultado_Modelo<Unit>) -> Unit) {
        Red.trabajo({
            Red.solicitud("DELETE", "/evidencias/$id")
            Unit
        }, alTerminar)
    }

    // las fotos se bajan una vez y quedan en el telefono
    private fun evidenciaDe(o: JSONObject): Evidencia {
        val fotos = o.getJSONArray("fotos").lista { EvidenciaFoto(it.entero("id"), Red.archivoFoto(it.entero("id"))) }
        var actualizada: String? = o.texto("actualizadaEn")
        if (actualizada == "") {
            actualizada = null
        }
        return Evidencia(
            id = o.entero("id"),
            idActividad = o.entero("idActividad"),
            fecha = o.texto("fecha"),
            creadaEn = o.texto("creadaEn"),
            actualizadaEn = actualizada,
            estado = o.texto("estado"),
            descripcion = o.texto("descripcion"),
            fotos = fotos,
        )
    }

    // foto mas ligera: lado de 1600 px y jpeg, con su giro corregido
    private fun reducir(foto: FotoNueva): FotoNueva {
        if (foto.datos.size <= PESO_SIN_REDUCIR) {
            return foto
        }
        try {
            val medidas = BitmapFactory.Options()
            medidas.inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(foto.datos, 0, foto.datos.size, medidas)
            if (medidas.outWidth <= 0 || medidas.outHeight <= 0) {
                return foto
            }
            var escala = 1
            while (maxOf(medidas.outWidth, medidas.outHeight) / (escala * 2) >= LADO_MAXIMO) {
                escala *= 2
            }
            val opciones = BitmapFactory.Options()
            opciones.inSampleSize = escala
            var imagen = BitmapFactory.decodeByteArray(foto.datos, 0, foto.datos.size, opciones) ?: return foto

            val lado = maxOf(imagen.width, imagen.height)
            if (lado > LADO_MAXIMO) {
                val factor = LADO_MAXIMO.toFloat() / lado
                imagen = Bitmap.createScaledBitmap(
                    imagen, (imagen.width * factor).toInt().coerceAtLeast(1),
                    (imagen.height * factor).toInt().coerceAtLeast(1), true,
                )
            }
            val giro = giroExif(foto.datos)
            if (giro != 0f) {
                val matriz = Matrix()
                matriz.postRotate(giro)
                imagen = Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, matriz, true)
            }
            val salida = ByteArrayOutputStream()
            imagen.compress(Bitmap.CompressFormat.JPEG, 82, salida)
            return FotoNueva(foto.nombre, "image/jpeg", salida.toByteArray())
        } catch (e: Exception) {
            return foto
        }
    }

    private fun giroExif(datos: ByteArray): Float {
        try {
            val exif = ExifInterface(ByteArrayInputStream(datos))
            val orientacion = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            if (orientacion == ExifInterface.ORIENTATION_ROTATE_90) return 90f
            if (orientacion == ExifInterface.ORIENTATION_ROTATE_180) return 180f
            if (orientacion == ExifInterface.ORIENTATION_ROTATE_270) return 270f
        } catch (e: Exception) {
            // sin datos de giro
        }
        return 0f
    }
}
