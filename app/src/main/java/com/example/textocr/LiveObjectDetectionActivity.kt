package com.example.textocr

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveObjectLabelActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: ObjectOverlayView
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout container
        val container = FrameLayout(this)
        previewView = PreviewView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        overlayView = ObjectOverlayView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        container.addView(previewView)
        container.addView(overlayView)
        setContentView(container)

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ObjectDetectionAnalyzer(this, overlayView))
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}

// Analyzer for ML Kit Object Detection
private class ObjectDetectionAnalyzer(
    val context: Context,
    val overlayView: ObjectOverlayView
) : ImageAnalysis.Analyzer {

    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val detector = ObjectDetection.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                // Update overlay view with detected objects & image info
                overlayView.setObjects(detectedObjects, inputImage.width, inputImage.height, imageProxy.imageInfo.rotationDegrees)
            }
            .addOnFailureListener {
                // Optionally handle failure
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

// Custom View to draw bounding boxes and labels
class ObjectOverlayView(context: Context) : View(context) {

    private var detectedObjects: List<DetectedObject> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0
    private var rotationDegrees = 0

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = 6f
    }

    private val textBackgroundPaint = Paint().apply {
        color = Color.parseColor("#80000000") // semi-transparent black
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        isAntiAlias = true
    }

    fun setObjects(objects: List<DetectedObject>, imgWidth: Int, imgHeight: Int, rotation: Int) {
        detectedObjects = objects
        imageWidth = imgWidth
        imageHeight = imgHeight
        rotationDegrees = rotation
        postInvalidate() // Request redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (detectedObjects.isEmpty()) return
        if (imageWidth == 0 || imageHeight == 0) return

        // Calculate scale factors to map image coords to view coords
        val scaleX = width.toFloat() / imageWidth.toFloat()
        val scaleY = height.toFloat() / imageHeight.toFloat()

        for (obj in detectedObjects) {
            val box = obj.boundingBox

            // Adjust for rotation if needed - usually rotationDegrees is 0 or multiples of 90
            val left = box.left * scaleX
            val top = box.top * scaleY
            val right = box.right * scaleX
            val bottom = box.bottom * scaleY

            // Draw box
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Prepare label text (take first label or "Unknown")
            val labelText = obj.labels.firstOrNull()?.text ?: "Unknown"
            val confidence = obj.labels.firstOrNull()?.confidence ?: 0f
            val text = "$labelText (${String.format("%.1f", confidence * 100)}%)"

            // Measure text size
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize

            // Draw background rectangle for text above the bounding box
            val bgRect = RectF(left, top - textHeight - 12f, left + textWidth + 12f, top)
            canvas.drawRoundRect(bgRect, 8f, 8f, textBackgroundPaint)

            // Draw label text inside the bg rect
            canvas.drawText(text, left + 6f, top - 6f, textPaint)
        }
    }
}