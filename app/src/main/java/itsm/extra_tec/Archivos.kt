package itsm.extra_tec

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File

object Archivos {

    /** Guarda en la carpeta Descargas del teléfono y devuelve una descripción de dónde quedó. */
    fun guardarEnDescargas(contexto: Context, nombre: String, mime: String, bytes: ByteArray): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nombre)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contexto.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                ?: throw IllegalStateException("No se pudo crear el archivo")
            contexto.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
            return "Descargas/$nombre"
        }
        // Android 9 o anterior: carpeta propia de la app (no requiere permisos)
        val carpeta = contexto.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: contexto.filesDir
        File(carpeta, nombre).writeBytes(bytes)
        return File(carpeta, nombre).absolutePath
    }

    /** PDF sencillo de texto, con salto de línea y de página automáticos (tamaño carta ~ A4). */
    fun textoAPdf(titulo: String, texto: String): ByteArray {
        val ancho = 595
        val alto = 842
        val margen = 48
        val pincel = Paint().apply { textSize = 11f; isAntiAlias = true }
        val pincelTitulo = Paint().apply { textSize = 16f; isFakeBoldText = true; isAntiAlias = true }
        val interlineado = 16
        val doc = PdfDocument()
        var numero = 1
        var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
        var y = margen + 8

        pagina.canvas.drawText(titulo, margen.toFloat(), y.toFloat(), pincelTitulo)
        y += interlineado * 2

        for (linea in texto.lines()) {
            var resto = linea
            do {
                val caben = pincel.breakText(resto, true, (ancho - 2 * margen).toFloat(), null)
                    .coerceAtLeast(1)
                if (y > alto - margen) {
                    doc.finishPage(pagina)
                    numero++
                    pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
                    y = margen
                }
                pagina.canvas.drawText(resto.substring(0, minOf(caben, resto.length)), margen.toFloat(), y.toFloat(), pincel)
                y += interlineado
                resto = resto.substring(minOf(caben, resto.length))
            } while (resto.isNotEmpty())
        }
        doc.finishPage(pagina)
        val salida = ByteArrayOutputStream()
        doc.writeTo(salida)
        doc.close()
        return salida.toByteArray()
    }
}
