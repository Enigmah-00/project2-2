package com.example.textocr

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var saveBtn: Button
    private lateinit var shareBtn: Button

    private var imageUri: Uri? = null
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        imageView = findViewById(R.id.previewImageView)
        saveBtn = findViewById(R.id.saveBtn)
        shareBtn = findViewById(R.id.shareBtn)

        imageUri = intent.getParcelableExtra("imageUri")

        if (imageUri != null) {
            imageView.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        saveBtn.setOnClickListener {
            saveImage()
        }

        shareBtn.setOnClickListener {
            shareImage()
        }
    }

    private fun saveImage() {
        val drawable = imageView.drawable ?: return
        bitmap = (drawable as? BitmapDrawable)?.bitmap
        if (bitmap == null) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "SavedImage_${System.currentTimeMillis()}.jpg"
            val file = File(getExternalFilesDir(null), fileName)
            val fos = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            Toast.makeText(this, "Image saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareImage() {
        val drawable = imageView.drawable ?: return
        bitmap = (drawable as? BitmapDrawable)?.bitmap
        if (bitmap == null) {
            Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "SharedImage_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, fileName)
            val fos = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error sharing image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
