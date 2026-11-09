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

// Bloque de un reporte con un titulo y varios datos
data class BloquePdf(val titulo: String, val detalles: List<Pair<String, String>>)

// Datos que lleva la constancia oficial
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
    private val GUINDA = Color.rgb(122, 23, 53)
    private val GUINDA_CLARO = Color.rgb(247, 241, 238)
    private val GRIS = Color.rgb(90, 80, 84)
    private val NEGRO = Color.rgb(20, 20, 20)

    // Devuelve la ubicacion del archivo guardado
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
        // Android 9 o anterior usa la carpeta de la app
        val carpeta = contexto.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: contexto.filesDir
        File(carpeta, nombre).writeBytes(bytes)
        return File(carpeta, nombre).absolutePath
    }

    // Constancia con el formato oficial del Tecnologico
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

    // Reporte con tarjetas y datos separados por renglon
    fun reportePdf(titulo: String, subtitulo: String, bloques: List<BloquePdf>): ByteArray {
        val doc = PdfDocument()
        var numero = 1
        var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
        var lienzo = pagina.canvas
        var y = 118f

        cabeceraReporte(lienzo, titulo, subtitulo, false)
        y = 126f

        val pTitulo = pincel(12f, true, NEGRO)
        val pEtiqueta = pincel(9f, true, GUINDA)
        val pValor = pincel(9.5f, false, NEGRO)
        val anchoValor = ANCHO - 2 * MARGEN - 128f

        for (bloque in bloques) {
            val lineas = bloque.detalles.map { (etiqueta, valor) ->
                Triple(etiqueta, valor, ajustar(valor, pValor, anchoValor))
            }
            val altoTarjeta = 46f + lineas.sumOf { (_, _, texto) ->
                maxOf(17.0, texto.size * 13.0 + 4.0)
            }.toFloat()

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
            lienzo.drawRoundRect(fondo, 10f, 10f, pincelRelleno(GUINDA_CLARO))
            lienzo.drawRect(MARGEN, y + 10f, MARGEN + 3f, y + altoTarjeta - 10f, pincelRelleno(GUINDA))
            lienzo.drawText(bloque.titulo, MARGEN + 16f, y + 20f, pTitulo)
            var filaY = y + 38f
            for ((etiqueta, _, texto) in lineas) {
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
        lienzo.drawRoundRect(fondo, 12f, 12f, pincelRelleno(GUINDA))
        val pTitulo = pincel(16f, true, Color.WHITE)
        lienzo.drawText(if (continuacion) "$titulo continuación" else titulo, MARGEN + 18f, 62f, pTitulo)
        val pSub = pincel(9f, false, Color.rgb(240, 220, 228))
        var subY = 80f
        for (linea in subtitulo.lines()) {
            lienzo.drawText(linea, MARGEN + 18f, subY, pSub)
            subY += 12f
        }
        lienzo.drawText("Instituto Tecnológico Superior de Misantla", ANCHO - MARGEN - 18f, 96f, pincel(7.5f, false, Color.WHITE).apply {
            textAlign = Paint.Align.RIGHT
        })
    }

    private fun piePagina(lienzo: Canvas, numero: Int) {
        lienzo.drawText(
            "ITSM Actividades extraescolares",
            MARGEN, ALTO - 24f, pincel(7.5f, false, GRIS),
        )
        lienzo.drawText(
            "Página $numero",
            ANCHO - MARGEN, ALTO - 24f,
            pincel(7.5f, false, GRIS).apply { textAlign = Paint.Align.RIGHT },
        )
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

    private data class Parte(val texto: String, val negrita: Boolean)

    private data class Palabra(val texto: String, val negrita: Boolean)

    // Escribe un parrafo con partes normales y en negrita
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
        val palabras = partes.flatMap { parte ->
            parte.texto.split(" ").filter { it.isNotEmpty() }.map { Palabra(it, parte.negrita) }
        }
        fun anchoDe(palabra: Palabra) = (if (palabra.negrita) negrita else normal).measureText(palabra.texto)
        val anchoEspacio = normal.measureText(" ")

        var linea = mutableListOf<Palabra>()
        var anchoLinea = 0f
        var y = yInicial

        fun dibujarLinea(ultima: Boolean) {
            if (linea.isEmpty()) return
            val suma = linea.fold(0f) { total, palabra -> total + anchoDe(palabra) }
            val espacios = (linea.size - 1).coerceAtLeast(0)
            val hueco = if (!ultima && espacios > 0) {
                ((ancho - suma) / espacios).coerceAtLeast(anchoEspacio)
            } else {
                anchoEspacio
            }
            var cx = x
            linea.forEachIndexed { indice, palabra ->
                lienzo.drawText(palabra.texto, cx, y, if (palabra.negrita) negrita else normal)
                cx += anchoDe(palabra)
                if (indice < linea.size - 1) cx += hueco
            }
            y += altoLinea
        }

        for (palabra in palabras) {
            val anchoPalabra = anchoDe(palabra)
            val necesario = if (linea.isEmpty()) anchoPalabra else anchoLinea + anchoEspacio + anchoPalabra
            if (necesario > ancho && linea.isNotEmpty()) {
                dibujarLinea(false)
                linea = mutableListOf()
                anchoLinea = 0f
            }
            if (linea.isNotEmpty()) anchoLinea += anchoEspacio
            linea.add(palabra)
            anchoLinea += anchoPalabra
        }
        dibujarLinea(true)
        return y
    }

    // Parte un texto en renglones que caben en el ancho
    private fun ajustar(texto: String, pintura: Paint, ancho: Float): List<String> {
        val resultado = mutableListOf<String>()
        for (bloque in texto.split("\n")) {
            if (bloque.isEmpty()) {
                resultado.add("")
                continue
            }
            var resto = bloque
            while (resto.isNotEmpty()) {
                val caben = pintura.breakText(resto, true, ancho, null).coerceAtLeast(1)
                if (caben >= resto.length) {
                    resultado.add(resto)
                    break
                }
                val corte = resto.lastIndexOf(' ', caben).takeIf { it > 0 } ?: caben
                resultado.add(resto.substring(0, corte))
                resto = resto.substring(corte).trimStart()
            }
        }
        return resultado.ifEmpty { listOf("") }
    }

    private fun pincel(tam: Float, negrita: Boolean, color: Int, serif: Boolean = false): Paint =
        Paint().apply {
            isAntiAlias = true
            textSize = tam
            this.color = color
            typeface = Typeface.create(
                if (serif) Typeface.SERIF else Typeface.SANS_SERIF,
                if (negrita) Typeface.BOLD else Typeface.NORMAL,
            )
        }

    private fun pincelTrazo(grosor: Float, color: Int): Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = grosor
        this.color = color
    }

    private fun pincelRelleno(color: Int): Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        this.color = color
    }

    private fun textoCentrado(lienzo: Canvas, texto: String, cx: Float, y: Float, pintura: Paint) {
        pintura.textAlign = Paint.Align.CENTER
        lienzo.drawText(texto, cx, y, pintura)
        pintura.textAlign = Paint.Align.LEFT
    }
}
