package itsm.extra_tec.vistas.Comun

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

// carga fotos sin gastar mucha memoria
object Fotos_Vista {

    // se lee en otro hilo
    // tamano es el lado minimo
    fun mostrar(imagen: ImageView, fuente: String, tamano: Int, alTerminar: (() -> Unit)? = null) {
        imagen.tag = fuente
        val contexto = imagen.context.applicationContext
        Thread {
            val foto = cargar(contexto, fuente, tamano)
            imagen.post {
                // la vista pudo cambiar de foto
                if (imagen.tag == fuente) {
                    imagen.setImageBitmap(foto)
                    if (alTerminar != null) {
                        alTerminar()
                    }
                }
            }
        }.start()
    }

    fun cargar(contexto: Context, fuente: String, tamano: Int): Bitmap? {
        try {
            // primero solo se lee el tamano
            val medidas = BitmapFactory.Options()
            medidas.inJustDecodeBounds = true
            var entrada = abrir(contexto, fuente)
            if (entrada == null) {
                return null
            }
            BitmapFactory.decodeStream(entrada, null, medidas)
            entrada.close()

            // se reduce a la mitad
            var escala = 1
            while (medidas.outWidth / (escala * 2) >= tamano && medidas.outHeight / (escala * 2) >= tamano) {
                escala = escala * 2
            }
            val opciones = BitmapFactory.Options()
            opciones.inSampleSize = escala
            entrada = abrir(contexto, fuente)
            if (entrada == null) {
                return null
            }
            val foto = BitmapFactory.decodeStream(entrada, null, opciones)
            entrada.close()
            return foto
        } catch (e: Exception) {
            return null
        } catch (e: OutOfMemoryError) {
            return null
        }
    }

    // la fuente es uri o ruta
    private fun abrir(contexto: Context, fuente: String): InputStream? {
        if (fuente.startsWith("content:") || fuente.startsWith("file:")) {
            return contexto.contentResolver.openInputStream(Uri.parse(fuente))
        }
        val archivo = File(fuente)
        if (!archivo.exists()) {
            return null
        }
        return FileInputStream(archivo)
    }
}
