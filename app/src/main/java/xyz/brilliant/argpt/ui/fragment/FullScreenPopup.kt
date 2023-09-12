package xyz.brilliant.argpt.ui.fragment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import xyz.brilliant.argpt.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

class FullScreenPopup(context: Context, private val imageUrl: String,private val bitmap: Bitmap?) : Dialog(context) {

    private val REQUEST_CODE = 101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the custom layout for the full-screen popup
        setContentView(R.layout.full_screen_popup)

        // Set layout parameters to match the screen size
        window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // Load and display the image using Glide or your preferred image-loading library
        val fullScreenImageView = findViewById<ImageView>(R.id.fullScreenImageView)
        val backIcon = findViewById<ImageView>(R.id.backIcon)

        backIcon.setOnClickListener {
            dismiss()
        }

        if(imageUrl.isNullOrEmpty())
        {
            fullScreenImageView.setImageBitmap(bitmap)

            downloadAndSaveImage(bitmap!!)
        }
        else {
            Glide.with(context)
                .load(imageUrl)
                .into(fullScreenImageView)
            downloadAndSaveImage(imageUrl)
        }

      //  downloadAndSaveImage(fullScreenImageView)
    }


    private fun downloadAndSaveImage(imageUrl: String) {
        val directory =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyApp")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "my_image.jpg"
        val file = File(directory, fileName)

        try {
            val inputStream = URL(imageUrl).openStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Update the gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.toString()),
                arrayOf("image/jpeg"),
                null
            )
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadAndSaveImage(bitmap: Bitmap) {
        val directory =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyApp")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "my_image.jpg"
        val file = File(directory, fileName)

        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()

            // Update the gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.toString()),
                arrayOf("image/jpeg"),
                null
            )
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
        }
    }



}
