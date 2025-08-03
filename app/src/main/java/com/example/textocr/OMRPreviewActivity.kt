package com.example.textocr

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class OMRPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_omr_preview)

        val imageView = findViewById<ImageView>(R.id.omrPreviewImageView)
        val imageUriString = intent.getStringExtra("omrImageUri")

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imageView.setImageURI(imageUri)
        } else {
            finish() // Close if no image passed
        }
    }
}
