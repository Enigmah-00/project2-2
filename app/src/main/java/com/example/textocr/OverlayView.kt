package com.example.textocr

import android.content.Context
import android.graphics.*
import android.view.View
import com.google.mlkit.vision.objects.DetectedObject

class OverlayView(context: Context) : View(context) {

    private var results: List<DetectedObject> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setResults(results: List<DetectedObject>, imageWidth: Int, imageHeight: Int) {
        this.results = results
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (results.isEmpty()) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        for (obj in results) {
            val box = obj.boundingBox
            val left = box.left * scaleX
            val top = box.top * scaleY
            val right = box.right * scaleX
            val bottom = box.bottom * scaleY
            canvas.drawRect(left, top, right, bottom, boxPaint)

            val label = obj.labels.firstOrNull()?.text ?: "Object"
            val confidence = obj.labels.firstOrNull()?.confidence ?: 0f
            val text = "$label ${(confidence * 100).toInt()}%"
            canvas.drawText(text, left, top - 10f, textPaint)
        }
    }
}
