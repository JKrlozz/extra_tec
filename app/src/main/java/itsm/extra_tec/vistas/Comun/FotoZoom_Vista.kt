package itsm.extra_tec.vistas.Comun

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

// imagen que se agranda
// tambien con doble toque
class FotoZoom_Vista(contexto: Context, atributos: AttributeSet?) : AppCompatImageView(contexto, atributos) {
    private val matriz = Matrix()
    private var zoom = 1f
    private var ultimoX = 0f
    private var ultimoY = 0f

    private val pellizco = ScaleGestureDetector(contexto, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var factor = detector.scaleFactor
            // zoom de uno a cinco
            if (zoom * factor < 1f) factor = 1f / zoom
            if (zoom * factor > 5f) factor = 5f / zoom
            zoom = zoom * factor
            matriz.postScale(factor, factor, detector.focusX, detector.focusY)
            acomodar()
            return true
        }
    })

    private val toques = GestureDetector(contexto, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (zoom > 1f) {
                reiniciarZoom()
            } else {
                zoom = 2.5f
                matriz.postScale(2.5f, 2.5f, e.x, e.y)
                acomodar()
            }
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    // muestra la foto completa y centrada
    fun reiniciarZoom() {
        zoom = 1f
        matriz.reset()
        val foto = drawable
        if (foto == null || width == 0 || height == 0 || foto.intrinsicWidth <= 0) {
            imageMatrix = matriz
            return
        }
        val anchoFoto = foto.intrinsicWidth.toFloat()
        val altoFoto = foto.intrinsicHeight.toFloat()
        var ajuste = width / anchoFoto
        if (height / altoFoto < ajuste) {
            ajuste = height / altoFoto
        }
        matriz.postScale(ajuste, ajuste)
        matriz.postTranslate((width - anchoFoto * ajuste) / 2f, (height - altoFoto * ajuste) / 2f)
        imageMatrix = matriz
    }

    override fun onSizeChanged(ancho: Int, alto: Int, anchoAnterior: Int, altoAnterior: Int) {
        super.onSizeChanged(ancho, alto, anchoAnterior, altoAnterior)
        reiniciarZoom()
    }

    override fun onTouchEvent(evento: MotionEvent): Boolean {
        pellizco.onTouchEvent(evento)
        toques.onTouchEvent(evento)
        if (evento.actionMasked == MotionEvent.ACTION_DOWN) {
            ultimoX = evento.x
            ultimoY = evento.y
        } else if (evento.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            // sigue el dedo que queda
            var queda = 0
            if (evento.actionIndex == 0) queda = 1
            ultimoX = evento.getX(queda)
            ultimoY = evento.getY(queda)
        } else if (evento.actionMasked == MotionEvent.ACTION_MOVE) {
            // un dedo mueve la foto
            if (!pellizco.isInProgress && evento.pointerCount == 1 && zoom > 1f) {
                matriz.postTranslate(evento.x - ultimoX, evento.y - ultimoY)
                acomodar()
            }
            ultimoX = evento.x
            ultimoY = evento.y
        } else if (evento.actionMasked == MotionEvent.ACTION_UP) {
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    // la foto no se sale
    private fun acomodar() {
        val foto = drawable
        if (foto == null) {
            return
        }
        val area = RectF(0f, 0f, foto.intrinsicWidth.toFloat(), foto.intrinsicHeight.toFloat())
        matriz.mapRect(area)
        var dx = 0f
        var dy = 0f
        if (area.width() <= width) {
            dx = (width - area.width()) / 2f - area.left
        } else if (area.left > 0) {
            dx = -area.left
        } else if (area.right < width) {
            dx = width - area.right
        }
        if (area.height() <= height) {
            dy = (height - area.height()) / 2f - area.top
        } else if (area.top > 0) {
            dy = -area.top
        } else if (area.bottom < height) {
            dy = height - area.bottom
        }
        matriz.postTranslate(dx, dy)
        imageMatrix = matriz
    }
}
