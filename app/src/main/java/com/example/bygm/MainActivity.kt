package com.example.bygm

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import com.example.bygm.OCRAsyncTask

class MainActivity : AppCompatActivity(), IOCRCallBack {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: TextView
    private val apiKey = "K86448249188957" // Your API key
    private val SELECT_PICTURE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        textViewResult = findViewById(R.id.textViewResult)
        val buttonSelectImage = findViewById<Button>(R.id.buttonSelectImage)

        buttonSelectImage.setOnClickListener {
            openImageChooser()
        }
    }

    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PICTURE && data != null) {
            val selectedImageUri: Uri? = data.data
            if (selectedImageUri != null) {
                try {
                    val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                    imageView.setImageBitmap(bitmap)

                    // Get input stream from Uri
                    val inputStream = contentResolver.openInputStream(selectedImageUri)
                    if (inputStream != null) {
                        // Call OCRAsyncTask with InputStream
                        OCRAsyncTask(this, apiKey, false, inputStream, "eng", this).execute()
                    } else {
                        textViewResult.text = "Unable to get image stream"
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    textViewResult.text = "Error loading image"
                }
            }
        }
    }


    private fun getFilePathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }
        return null
    }


    override fun getOCRCallBackResult(result: String?) {
        textViewResult.text = result ?: "No text found"
    }
}
