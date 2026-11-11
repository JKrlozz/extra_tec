package itsm.extra_tec.Modelo

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

// bloque de reporte con datos
data class BloquePdf(val titulo: String, val detalles: List<Pair<String, String>>)

// datos de la constancia oficial
data class ConstanciaPdf(
    val alumno: String,
    val matricula: String,
    val carrera: String,
    val actividad: String,
    val periodo: String,
    val nivel: String,
    val valor: String,
    val creditos: String,
    val folio: String,
    val dia: String,
    val mes: String,
    val anio: String,
)

object Archivos_Modelo {
    private const val ANCHO = 612
    private const val ALTO = 792
    private const val MARGEN = 46f
    private val VERDE = Color.rgb(19, 85, 58)
    private val VERDE_CLARO = Color.rgb(238, 246, 241)
    private val VERDE_TEXTO = Color.rgb(214, 236, 223)
    private val GRIS = Color.rgb(84, 98, 90)
    private val NEGRO = Color.rgb(20, 20, 20)

    // regresa donde quedo el archivo
    fun guardarEnDescargas(contexto: Context, nombre: String, mime: String, bytes: ByteArray): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues()
            valores.put(MediaStore.Downloads.DISPLAY_NAME, nombre)
            valores.put(MediaStore.Downloads.MIME_TYPE, mime)
            valores.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            val uri = contexto.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
            if (uri == null) {
                throw IllegalStateException("No se pudo crear el archivo")
            }
            val salida = contexto.contentResolver.openOutputStream(uri)
            if (salida == null) {
                throw IllegalStateException("No se pudo escribir el archivo")
            }
            salida.write(bytes)
            salida.close()
            return "Descargas/$nombre"
        }
        // android 9 usa otra carpeta
        var carpeta = contexto.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (carpeta == null) {
            carpeta = contexto.filesDir
        }
        val archivo = File(carpeta, nombre)
        archivo.writeBytes(bytes)
        return archivo.absolutePath
    }

    // constancia con el formato del tecnologico
    fun constanciaPdf(d: ConstanciaPdf): ByteArray {
        val doc = PdfDocument()
        val pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())
        val lienzo = pagina.canvas

        dibujarMembrete(lienzo)

        textoCentrado(lienzo, "CONSTANCIA", ANCHO / 2f, 188f, pincel(15f, true, NEGRO, serif = true))
        textoCentrado(
            lienzo,
            "DE TERMINACIÓN DE ACTIVIDAD EXTRAESCOLAR",
            ANCHO / 2f,
            206f,
            pincel(11.5f, true, NEGRO, serif = true),
        )

        lienzo.drawText("A QUIEN CORRESPONDA", MARGEN + 4, 252f, pincel(10f, true, NEGRO, serif = true))

        val partes = listOf(
            Parte("De acuerdo con documentos que obran en los archivos de esta Institución, el C. ", false),
            Parte(d.alumno.uppercase(Locale.getDefault()) + ",", true),
            Parte(" con número de control ", false),
            Parte(d.matricula, true),
            Parte(" de la carrera de ", false),
            Parte(d.carrera.uppercase(Locale.getDefault()), true),
            Parte(" ha obtenido ", false),
            Parte(d.creditos, true),
            Parte(" Crédito, Nivel de desempeño ", false),
            Parte(d.nivel + ",", true),
            Parte(" valor numérico ", false),
            Parte(d.valor + ",", true),
            Parte(" por haber concluido con su actividad extraescolar ", false),
            Parte(d.actividad.uppercase(Locale.getDefault()), true),
            Parte(" durante el periodo ", false),
            Parte(d.periodo + ".", true),
        )
        var y = parrafo(lienzo, partes, MARGEN + 4, 288f, ANCHO - 2 * MARGEN - 8, 10.5f, 17f, serif = true)

        y = parrafo(
            lienzo,
            listOf(
                Parte(
                    "Las actividades extraescolares se realizan de acuerdo con lo establecido en el Manual " +
                        "de Lineamientos Académicos-Administrativos del Tecnológico Nacional de México.",
                    false,
                ),
            ),
            MARGEN + 4, y + 20f, ANCHO - 2 * MARGEN - 8, 10.5f, 17f, serif = true,
        )

        parrafo(
            lienzo,
            listOf(
                Parte("Se extiende la presente en la ciudad de Misantla, Ver., a los ", false),
                Parte(d.dia, true),
                Parte(" días de ", false),
                Parte(d.mes, true),
                Parte(" de ", false),
                Parte(d.anio + ".", true),
            ),
            MARGEN + 4, y + 20f, ANCHO - 2 * MARGEN - 8, 10.5f, 17f, serif = true,
        )

        textoCentrado(lienzo, "ATENTAMENTE", ANCHO / 2f, 566f, pincel(10f, true, NEGRO, serif = true))

        lineaFirma(lienzo, 76f, 268f, 620f)
        lineaFirma(lienzo, 344f, 268f, 620f)
        textoCentrado(lienzo, "Mtra. María Teresa Meza Caizeros", 210f, 638f, pincel(9f, true, NEGRO, serif = true))
        textoCentrado(lienzo, "Directora General", 210f, 652f, pincel(8f, false, NEGRO, serif = true))
        textoCentrado(lienzo, "Dr. Federico Perera Zapata", 478f, 638f, pincel(9f, true, NEGRO, serif = true))
        textoCentrado(lienzo, "Subdirector de Vinculación", 478f, 652f, pincel(8f, false, NEGRO, serif = true))

        selloInstitucional(lienzo, ANCHO / 2f, 702f, 30f)
        cajaFolio(lienzo, d.folio)

        doc.finishPage(pagina)
        val salida = ByteArrayOutputStream()
        doc.writeTo(salida)
        doc.close()
        return salida.toByteArray()
    }

    // reporte con un dato por renglon
    fun reportePdf(titulo: String, subtitulo: String, bloques: List<BloquePdf>): ByteArray {
        val doc = PdfDocument()
        var numero = 1
        var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
        var lienzo = pagina.canvas

        cabeceraReporte(lienzo, titulo, subtitulo, false)
        var y = 126f

        val pTitulo = pincel(12f, true, NEGRO)
        val pEtiqueta = pincel(9f, true, VERDE)
        val pValor = pincel(9.5f, false, NEGRO)
        val anchoValor = ANCHO - 2 * MARGEN - 128f

        for (bloque in bloques) {
            // cada valor se parte en renglones
            val renglones = mutableListOf<List<String>>()
            var altoTarjeta = 46f
            for (detalle in bloque.detalles) {
                val texto = ajustar(detalle.second, pValor, anchoValor)
                renglones.add(texto)
                altoTarjeta += maxOf(17f, texto.size * 13f + 4f)
            }

            if (y + altoTarjeta > ALTO - 54) {
                piePagina(lienzo, numero)
                doc.finishPage(pagina)
                numero++
                pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
                lienzo = pagina.canvas
                cabeceraReporte(lienzo, titulo, subtitulo, true)
                y = 126f
            }

            val fondo = RectF(MARGEN, y, ANCHO - MARGEN, y + altoTarjeta)
            lienzo.drawRoundRect(fondo, 10f, 10f, pincelRelleno(VERDE_CLARO))
            lienzo.drawRect(MARGEN, y + 10f, MARGEN + 3f, y + altoTarjeta - 10f, pincelRelleno(VERDE))
            lienzo.drawText(bloque.titulo, MARGEN + 16f, y + 20f, pTitulo)
            var filaY = y + 38f
            for (i in bloque.detalles.indices) {
                val etiqueta = bloque.detalles[i].first
                val texto = renglones[i]
                lienzo.drawText(etiqueta, MARGEN + 16f, filaY, pEtiqueta)
                var renglon = filaY
                for (linea in texto) {
                    lienzo.drawText(linea, MARGEN + 112f, renglon, pValor)
                    renglon += 13f
                }
                filaY += maxOf(17f, texto.size * 13f + 4f)
            }
            y += altoTarjeta + 10f
        }

        piePagina(lienzo, numero)
        doc.finishPage(pagina)
        val salida = ByteArrayOutputStream()
        doc.writeTo(salida)
        doc.close()
        return salida.toByteArray()
    }

    private fun cabeceraReporte(lienzo: Canvas, titulo: String, subtitulo: String, continuacion: Boolean) {
        val fondo = RectF(MARGEN, 34f, ANCHO - MARGEN, 104f)
        lienzo.drawRoundRect(fondo, 12f, 12f, pincelRelleno(VERDE))
        val pTitulo = pincel(16f, true, Color.WHITE)
        var textoTitulo = titulo
        if (continuacion) {
            textoTitulo = "$titulo continuación"
        }
        lienzo.drawText(textoTitulo, MARGEN + 18f, 62f, pTitulo)
        val pSub = pincel(9f, false, VERDE_TEXTO)
        var subY = 80f
        for (linea in subtitulo.split("\n")) {
            lienzo.drawText(linea, MARGEN + 18f, subY, pSub)
            subY += 12f
        }
        val pEscuela = pincel(7.5f, false, Color.WHITE)
        pEscuela.textAlign = Paint.Align.RIGHT
        lienzo.drawText("Instituto Tecnológico Superior de Misantla", ANCHO - MARGEN - 18f, 96f, pEscuela)
    }

    private fun piePagina(lienzo: Canvas, numero: Int) {
        lienzo.drawText("ITSM Actividades extraescolares", MARGEN, ALTO - 24f, pincel(7.5f, false, GRIS))
        val pNumero = pincel(7.5f, false, GRIS)
        pNumero.textAlign = Paint.Align.RIGHT
        lienzo.drawText("Página $numero", ANCHO - MARGEN, ALTO - 24f, pNumero)
    }

    private fun dibujarMembrete(lienzo: Canvas) {
        lienzo.drawLine(38f, 32f, ANCHO - 38f, 32f, pincelTrazo(1.2f, NEGRO))

        selloSimple(lienzo, 70f, 76f, 24f, "SEV", "VERACRUZ")

        lienzo.drawText("GOBIERNO DEL ESTADO", 108f, 54f, pincel(6.5f, true, NEGRO, serif = true))
        lienzo.drawText("DE VERACRUZ", 108f, 63f, pincel(6.5f, true, NEGRO, serif = true))
        lienzo.drawText("VERACRUZ", 108f, 78f, pincel(11f, true, NEGRO, serif = true))
        lienzo.drawText("2024 - 2030", 108f, 90f, pincel(8f, true, NEGRO, serif = true))

        lienzo.drawLine(212f, 52f, 212f, 96f, pincelTrazo(0.6f, GRIS))
        lienzo.drawText("SEV", 224f, 68f, pincel(10.5f, true, NEGRO, serif = true))
        lienzo.drawText("SECRETARÍA DE EDUCACIÓN", 224f, 80f, pincel(4.6f, false, NEGRO, serif = true))
        lienzo.drawText("DE VERACRUZ", 224f, 87f, pincel(4.6f, false, NEGRO, serif = true))

        lienzo.drawLine(326f, 52f, 326f, 96f, pincelTrazo(0.6f, GRIS))
        lienzo.drawText("SEMSyS", 338f, 68f, pincel(10.5f, true, NEGRO, serif = true))
        lienzo.drawText("SUBSECRETARÍA DE EDUCACIÓN", 338f, 80f, pincel(4.4f, false, NEGRO, serif = true))
        lienzo.drawText("MEDIA SUPERIOR Y SUPERIOR", 338f, 87f, pincel(4.4f, false, NEGRO, serif = true))

        lienzo.drawLine(456f, 52f, 456f, 96f, pincelTrazo(0.6f, GRIS))
        lienzo.drawText("DET", 468f, 68f, pincel(10.5f, true, NEGRO, serif = true))
        lienzo.drawText("DIRECCIÓN DE EDUCACIÓN", 468f, 80f, pincel(4.4f, false, NEGRO, serif = true))
        lienzo.drawText("TECNOLÓGICA", 468f, 87f, pincel(4.4f, false, NEGRO, serif = true))

        selloSimple(lienzo, 554f, 76f, 24f, "ITSM", "MISANTLA")
        lienzo.drawLine(38f, 112f, ANCHO - 38f, 112f, pincelTrazo(0.9f, NEGRO))
    }

    private fun selloSimple(lienzo: Canvas, cx: Float, cy: Float, radio: Float, titulo: String, subtitulo: String) {
        lienzo.drawCircle(cx, cy, radio, pincelTrazo(1.3f, NEGRO))
        lienzo.drawCircle(cx, cy, radio - 3f, pincelTrazo(0.5f, NEGRO))
        textoCentrado(lienzo, titulo, cx, cy + 2f, pincel(8f, true, NEGRO, serif = true))
        textoCentrado(lienzo, subtitulo, cx, cy + 12f, pincel(4.2f, false, NEGRO, serif = true))
    }

    private fun selloInstitucional(lienzo: Canvas, cx: Float, cy: Float, radio: Float) {
        lienzo.drawCircle(cx, cy, radio, pincelTrazo(1.3f, NEGRO))
        lienzo.drawCircle(cx, cy, radio - 4f, pincelTrazo(0.5f, NEGRO))
        textoCentrado(lienzo, "ITSM", cx, cy + 2f, pincel(10f, true, NEGRO, serif = true))
        textoCentrado(lienzo, "ACTIVIDADES", cx, cy + 13f, pincel(4.8f, true, NEGRO, serif = true))
        textoCentrado(lienzo, "EXTRAESCOLARES", cx, cy + 20f, pincel(4.8f, true, NEGRO, serif = true))
    }

    private fun cajaFolio(lienzo: Canvas, folio: String) {
        val caja = RectF(38f, 733f, 178f, 759f)
        lienzo.drawRoundRect(caja, 4f, 4f, pincelTrazo(0.9f, NEGRO))
        lienzo.drawText("FOLIO:", 46f, 750f, pincel(9f, true, NEGRO, serif = true))
        lienzo.drawText(folio, 88f, 750f, pincel(10f, true, NEGRO, serif = true))
    }

    private fun lineaFirma(lienzo: Canvas, x: Float, ancho: Float, y: Float) {
        lienzo.drawLine(x, y, x + ancho, y, pincelTrazo(0.9f, NEGRO))
    }

    // texto normal o en negrita
    private class Parte(val texto: String, val negrita: Boolean)

    // escribe un parrafo justificado
    // regresa la altura donde termino
    private fun parrafo(
        lienzo: Canvas,
        partes: List<Parte>,
        x: Float,
        yInicial: Float,
        ancho: Float,
        tam: Float,
        altoLinea: Float,
        serif: Boolean = false,
    ): Float {
        val normal = pincel(tam, false, NEGRO, serif)
        val negrita = pincel(tam, true, NEGRO, serif)
        val anchoEspacio = normal.measureText(" ")

        // se separa todo en palabras
        val palabras = mutableListOf<Parte>()
        for (parte in partes) {
            for (palabra in parte.texto.split(" ")) {
                if (palabra.isNotEmpty()) {
                    palabras.add(Parte(palabra, parte.negrita))
                }
            }
        }

        var linea = mutableListOf<Parte>()
        var anchoLinea = 0f
        var y = yInicial
        for (palabra in palabras) {
            val anchoPalabra = medir(palabra, normal, negrita)
            if (linea.isNotEmpty() && anchoLinea + anchoEspacio + anchoPalabra > ancho) {
                dibujarLinea(lienzo, linea, x, y, ancho, normal, negrita, false)
                y += altoLinea
                linea = mutableListOf()
                anchoLinea = 0f
            }
            if (linea.isNotEmpty()) {
                anchoLinea += anchoEspacio
            }
            linea.add(palabra)
            anchoLinea += anchoPalabra
        }
        if (linea.isNotEmpty()) {
            dibujarLinea(lienzo, linea, x, y, ancho, normal, negrita, true)
            y += altoLinea
        }
        return y
    }

    private fun dibujarLinea(
        lienzo: Canvas,
        linea: List<Parte>,
        x: Float,
        y: Float,
        ancho: Float,
        normal: Paint,
        negrita: Paint,
        ultima: Boolean,
    ) {
        var suma = 0f
        for (palabra in linea) {
            suma += medir(palabra, normal, negrita)
        }
        // el espacio se reparte para justificar
        var hueco = normal.measureText(" ")
        if (!ultima && linea.size > 1) {
            val repartido = (ancho - suma) / (linea.size - 1)
            if (repartido > hueco) {
                hueco = repartido
            }
        }
        var cx = x
        for (palabra in linea) {
            var pintura = normal
            if (palabra.negrita) pintura = negrita
            lienzo.drawText(palabra.texto, cx, y, pintura)
            cx += medir(palabra, normal, negrita) + hueco
        }
    }

    private fun medir(palabra: Parte, normal: Paint, negrita: Paint): Float {
        if (palabra.negrita) {
            return negrita.measureText(palabra.texto)
        }
        return normal.measureText(palabra.texto)
    }

    // parte el texto en renglones
    private fun ajustar(texto: String, pintura: Paint, ancho: Float): List<String> {
        val resultado = mutableListOf<String>()
        for (bloque in texto.split("\n")) {
            var resto = bloque
            if (resto.isEmpty()) {
                resultado.add("")
            }
            while (resto.isNotEmpty()) {
                var caben = pintura.breakText(resto, true, ancho, null)
                if (caben < 1) caben = 1
                if (caben >= resto.length) {
                    resultado.add(resto)
                    resto = ""
                } else {
                    var corte = resto.lastIndexOf(' ', caben)
                    if (corte <= 0) corte = caben
                    resultado.add(resto.substring(0, corte))
                    resto = resto.substring(corte).trim()
                }
            }
        }
        if (resultado.isEmpty()) {
            resultado.add("")
        }
        return resultado
    }

    private fun pincel(tam: Float, negrita: Boolean, color: Int, serif: Boolean = false): Paint {
        val pintura = Paint()
        pintura.isAntiAlias = true
        pintura.textSize = tam
        pintura.color = color
        var familia = Typeface.SANS_SERIF
        if (serif) familia = Typeface.SERIF
        var estilo = Typeface.NORMAL
        if (negrita) estilo = Typeface.BOLD
        pintura.typeface = Typeface.create(familia, estilo)
        return pintura
    }

    private fun pincelTrazo(grosor: Float, color: Int): Paint {
        val pintura = Paint()
        pintura.isAntiAlias = true
        pintura.style = Paint.Style.STROKE
        pintura.strokeWidth = grosor
        pintura.color = color
        return pintura
    }

    private fun pincelRelleno(color: Int): Paint {
        val pintura = Paint()
        pintura.isAntiAlias = true
        pintura.style = Paint.Style.FILL
        pintura.color = color
        return pintura
    }

    private fun textoCentrado(lienzo: Canvas, texto: String, cx: Float, y: Float, pintura: Paint) {
        pintura.textAlign = Paint.Align.CENTER
        lienzo.drawText(texto, cx, y, pintura)
        pintura.textAlign = Paint.Align.LEFT
    }
}
