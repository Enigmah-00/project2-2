package com.example.textocr
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

class GraphicOverlay(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val lock = Any()
    private val graphics: MutableList<Graphic> = ArrayList()

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)
        fun scaleX(horizontal: Float): Float = horizontal * overlay.width / overlay.imageWidth
        fun scaleY(vertical: Float): Float = vertical * overlay.height / overlay.imageHeight
    }

    var imageWidth: Int = 1
    var imageHeight: Int = 1

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}
