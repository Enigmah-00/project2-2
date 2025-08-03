package com.example.textocr

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import android.content.Intent
import android.net.Uri

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var cameraImage: ImageView
    private lateinit var scanTextBtn: Button
    private lateinit var detectObjectBtn: Button
    private lateinit var liveObjectBtn: Button
    private lateinit var omrCheckBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultText: TextView
    private lateinit var copyTextBtn: Button
    private lateinit var previewView: PreviewView
    private lateinit var liveLabelsTextView: TextView
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var scanQRCodeBtn: Button
    private lateinit var scanBarcodeBtn: Button

    private var isSpeaking = false


    // Image Uri
    private var imageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private enum class Mode { OCR, IMAGE_LABELING, LIVE_OBJECT_DETECTION }
    private var currentMode: Mode? = null

    // Permissions and Activity Result
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>

    // CameraX
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var analysisUseCase: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // OMR
    private var correctSheetUri: Uri? = null
    private var studentSheetUri: Uri? = null
    private var isSelectingCorrectSheet = true
    private var omrResultUri: Uri? = null
    private var processAsBarcode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = Color.parseColor("#121212")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Failed to initialize OpenCV")
        }

        initViews()
        initListeners()
        initActivityLaunchers()
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                textToSpeech.setSpeechRate(1.0f)
            } else {
                Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        val speakBtn = findViewById<ImageButton>(R.id.speakTextBtn)
        val animatedDrawable = speakBtn.background as? AnimationDrawable

        speakBtn.setOnClickListener {
            val text = resultText.text.toString()
            if (text.isNotEmpty()) {
                if (!isSpeaking) {
                    animatedDrawable?.start()
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    isSpeaking = true

                    // Optional: auto stop animation after estimated time
                    val speechLength = text.length * 50L
                    speakBtn.postDelayed({
                        if (!textToSpeech.isSpeaking) {
                            animatedDrawable?.stop()
                            isSpeaking = false
                        }
                    }, speechLength)

                } else {
                    textToSpeech.stop()
                    animatedDrawable?.stop()
                    isSpeaking = false
                }
            }
        }

    }

    private fun initViews() {
        cameraImage = findViewById(R.id.cameraImage)
        scanTextBtn = findViewById(R.id.scanTextBtn)
        detectObjectBtn = findViewById(R.id.detectObjectBtn)
        liveObjectBtn = findViewById(R.id.liveObjectBtn)
        omrCheckBtn = findViewById(R.id.omrCheckBtn)
        progressBar = findViewById(R.id.progressBar)
        resultText = findViewById(R.id.resultText)
        copyTextBtn = findViewById(R.id.copyTextBtn)
        previewView = findViewById(R.id.previewView)
        liveLabelsTextView = findViewById(R.id.liveLabelsTextView)
        scanQRCodeBtn = findViewById(R.id.btnScanQRCode)
        scanBarcodeBtn = findViewById(R.id.btnScanBarcode)

        resultText.setTextIsSelectable(true)
        copyTextBtn.visibility = View.GONE
        cameraImage.setOnClickListener {
            if (omrResultUri != null) {
                val intent = Intent(this, OMRPreviewActivity::class.java)
                intent.putExtra("omrImageUri", omrResultUri.toString())
                startActivity(intent)
            } else if (imageUri != null) {
                // fallback if no OMR result saved yet
                val intent = Intent(this, ImagePreviewActivity::class.java)
                intent.putExtra("imageUri", imageUri)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun initListeners() {
        copyTextBtn.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Result Text", resultText.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        scanTextBtn.setOnClickListener {
            stopLiveDetection()
            currentMode = Mode.OCR
            checkAndRequestPermissions()
        }

        detectObjectBtn.setOnClickListener {
            stopLiveDetection()
            currentMode = Mode.IMAGE_LABELING
            checkAndRequestPermissions()
        }

        liveObjectBtn.setOnClickListener {
            stopLiveDetection()
            resultText.visibility = View.GONE
            copyTextBtn.visibility = View.GONE
            startActivity(Intent(this, LiveObjectLabelActivity::class.java))
        }

        omrCheckBtn.setOnClickListener {
            stopLiveDetection()
            resultText.visibility = View.VISIBLE
            copyTextBtn.visibility = View.GONE
            currentMode = null
            showOMRSourceDialog()
        }
        scanQRCodeBtn.setOnClickListener {
            stopLiveDetection()
            currentMode = Mode.OCR  // reuse crop + image picker flow
            processAsBarcode = true
            checkAndRequestPermissions()
        }

        scanBarcodeBtn.setOnClickListener {
            stopLiveDetection()
            currentMode = Mode.OCR  // same flow, different interpretation
            processAsBarcode = true
            checkAndRequestPermissions()
        }

    }

    private fun initActivityLaunchers() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.entries.all { entry -> entry.value }) {
                launchImageSourceDialog()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show()
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && imageUri != null) {
                launchCrop(imageUri!!)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                if (currentMode == null) {
                    if (isSelectingCorrectSheet) {
                        correctSheetUri = it
                        isSelectingCorrectSheet = false
                        AlertDialog.Builder(this)
                            .setTitle("Now Select Student's Sheet")
                            .setPositiveButton("Select") { _, _ -> launchGallery() }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        studentSheetUri = it
                        if (correctSheetUri != null && studentSheetUri != null) {
                            resultText.text = "Checking OMR..."
                            progressBar.visibility = View.VISIBLE
                            runOMRCheck(correctSheetUri!!, studentSheetUri!!)
                        }
                    }
                } else {
                    launchCrop(it)
                }
            }
        }

        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val resultUri = UCrop.getOutput(it.data!!)
            if (it.resultCode == Activity.RESULT_OK && resultUri != null) {
                imageUri = resultUri  // <--- YOU MUST SET this!
                cameraImage.setImageURI(resultUri)
                processImageWithMode(resultUri)
            }
        }
    }
    private fun saveBitmapToCache(bitmap: Bitmap): Uri? {
        return try {
            val file = File(cacheDir, "omr_result_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isEmpty()) {
            launchImageSourceDialog()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun launchImageSourceDialog() {
        val options = arrayOf("Capture Image", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> launchGallery()
                }
            }.show()
    }

    private fun showOMRSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Correct Answer Sheet")
            .setMessage("Pick the correct answer sheet image first.")
            .setPositiveButton("Select") { _, _ ->
                isSelectingCorrectSheet = true
                launchGallery()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchCamera() {
        val photoFile = createImageFile() ?: return
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
        cameraLauncher.launch(imageUri)
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", cacheDir)
        } catch (ex: Exception) {
            Toast.makeText(this, "File creation failed: ${ex.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun launchGallery() = galleryLauncher.launch("image/*")

    private fun launchCrop(sourceUri: Uri) {
        val destUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop Image")
        }
        val intent = UCrop.of(sourceUri, destUri).withOptions(options).getIntent(this)
        cropLauncher.launch(intent)
    }

    private fun processImageWithMode(uri: Uri) {
        resultText.text = ""
        copyTextBtn.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        previewView.visibility = View.GONE
        liveLabelsTextView.visibility = View.GONE
        cameraImage.visibility = View.VISIBLE

        if (processAsBarcode) {
            processImageWithBarcode(uri)
            return
        }

        when (currentMode) {
            Mode.OCR -> processImageWithMLKitOCR(uri)
            Mode.IMAGE_LABELING -> processImageWithMLKitImageLabeling(uri)
            else -> {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Invalid mode", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun processImageWithMLKitOCR(uri: Uri) {
        val image = InputImage.fromFilePath(this, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                resultText.text = it.text.ifEmpty { "No text found" }
                copyTextBtn.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Text recognition failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun processImageWithMLKitImageLabeling(uri: Uri) {
        val image = InputImage.fromFilePath(this, uri)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                progressBar.visibility = View.GONE
                if (labels.isEmpty()) {
                    resultText.text = "No labels found."
                    return@addOnSuccessListener
                }
                resultText.text = labels.joinToString("\n") { "- ${it.text} (Confidence: ${"%.2f".format(it.confidence)})" }
                copyTextBtn.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Labeling failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ADD THIS METHOD INTO YOUR EXISTING MainActivity.kt


    private fun runOMRCheck(uriCorrect: Uri, uriStudent: Uri) {
        Thread {
            try {
                val correctBmp = decodeUriToBitmap(uriCorrect)
                val studentBmp = decodeUriToBitmap(uriStudent).copy(Bitmap.Config.ARGB_8888, true)
                val correctAns = extractOMRAnswers(correctBmp)
                val studentAns = extractOMRAnswers(studentBmp)

                val mat = Mat()
                Utils.bitmapToMat(studentBmp, mat)

                val gray = Mat()
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
                Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
                val thresh = Mat()
                Imgproc.adaptiveThreshold(
                    gray, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                    Imgproc.THRESH_BINARY_INV, 11, 2.0
                )

                val rows = correctAns.size
                val cols = 4
                val cellWidth = thresh.cols() / cols
                val cellHeight = thresh.rows() / rows

                var score = 0

                for (i in 0 until rows) {
                    val correctIndex = "ABCD".indexOf(correctAns[i])
                    val studentIndex = "ABCD".indexOf(studentAns[i])

                    if (studentIndex == correctIndex && studentIndex != -1) {
                        score++
                        val centerX = (studentIndex * cellWidth) + cellWidth / 2
                        val centerY = (i * cellHeight) + cellHeight / 2
                        val radius = (minOf(cellWidth, cellHeight) / 2.5).toInt()
                        Imgproc.circle(mat, Point(centerX.toDouble(), centerY.toDouble()), radius, Scalar(0.0, 255.0, 0.0), -1)
                    } else {
                        // Mark correct answer in red if student selected wrong or didn't answer
                        if (correctIndex != -1) {
                            val centerX = (correctIndex * cellWidth) + cellWidth / 2
                            val centerY = (i * cellHeight) + cellHeight / 2
                            val radius = (minOf(cellWidth, cellHeight) / 2.5).toInt()
                            Imgproc.circle(mat, Point(centerX.toDouble(), centerY.toDouble()), radius, Scalar(255.0, 0.0, 0.0), -1)
                        }
                    }
                }

                val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(mat, resultBitmap)

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    cameraImage.setImageBitmap(resultBitmap)

                    // Save bitmap to cache and keep URI for preview
                    omrResultUri = saveBitmapToCache(resultBitmap)

                    resultText.text = "Score: $score/$rows"
                    copyTextBtn.visibility = View.VISIBLE
                }


            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    resultText.text = "Error: ${e.message}"
                }
            }
        }.start()
    }
    private fun decodeUriToBitmap(uri: Uri): Bitmap {
        val input: InputStream? = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(input!!)
    }

    private fun extractOMRAnswers(bitmap: Bitmap): List<Char> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        val thresh = Mat()
        Imgproc.adaptiveThreshold(gray, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV, 11, 2.0)
        val rows = 10
        val cols = 4
        val answers = mutableListOf<Char>()
        val cellWidth = thresh.cols() / cols
        val cellHeight = thresh.rows() / rows
        for (i in 0 until rows) {
            var max = 0
            var chosen = -1
            for (j in 0 until cols) {
                val rect = Rect(j * cellWidth, i * cellHeight, cellWidth, cellHeight)
                val cell = thresh.submat(rect)
                val count = Core.countNonZero(cell)
                if (count > max) {
                    max = count
                    chosen = j
                }
            }
            answers.add("ABCD"[chosen])
        }
        return answers
    }
    private fun saveImageToGallery(bitmap: Bitmap) {
        val savedUri = MediaStore.Images.Media.insertImage(
            contentResolver, bitmap, "OMR_Result_${System.currentTimeMillis()}", null
        )
        Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
    }

    private fun shareImage(bitmap: Bitmap) {
        val file = File(cacheDir, "shared_image.jpg")
        val outputStream = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }
    private fun processImageWithBarcode(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        resultText.text = ""
        copyTextBtn.visibility = View.GONE
        cameraImage.visibility = View.VISIBLE
        previewView.visibility = View.GONE
        liveLabelsTextView.visibility = View.GONE

        val image = InputImage.fromFilePath(this, uri)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                progressBar.visibility = View.GONE

                if (barcodes.isEmpty()) {
                    resultText.text = "No barcode found."
                    return@addOnSuccessListener
                }

                // For simplicity, just handle the first barcode detected
                val barcode = barcodes[0]
                val rawValue = barcode.rawValue ?: ""
                val valueType = barcode.valueType

                when (valueType) {
                    Barcode.TYPE_URL -> {
                        // It's a URL, open it in browser
                        val url = barcode.url?.url ?: rawValue
                        openUrlInBrowser(url)
                    }
                    else -> {
                        // Not a URL, just show the raw value
                        resultText.text = rawValue
                        copyTextBtn.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Barcode scanning failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open URL", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onResume() {
        super.onResume()
        resultText.visibility = View.VISIBLE
        copyTextBtn.visibility = if (resultText.text.isNotEmpty()) View.VISIBLE else View.GONE
    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        stopLiveDetection()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    private fun stopLiveDetection() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        previewView.visibility = View.GONE
        liveLabelsTextView.visibility = View.GONE
    }

}
