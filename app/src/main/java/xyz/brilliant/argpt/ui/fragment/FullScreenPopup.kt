package xyz.brilliant.argpt.ui.fragment

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import xyz.brilliant.argpt.R
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
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
        val downloadButton = findViewById<ImageView>(R.id.download)
        backIcon.setOnClickListener {
            dismiss()
        }

        if(imageUrl.isNullOrEmpty())
        {
            fullScreenImageView.setImageBitmap(bitmap)
            downloadButton.setOnClickListener {
                if(bitmap!=null) downloadAndSaveImage(bitmap)
            }
        }
        else {
            Glide.with(context)
                .load(imageUrl)
                .into(fullScreenImageView)

            downloadButton.setOnClickListener {

                val job = GlobalScope.launch(Dispatchers.IO){
                    downloadAndSaveImage(imageUrl)
                }


            }

        }

        //  downloadAndSaveImage(fullScreenImageView)
    }



    private fun downloadAndSaveImage(bitmap: Bitmap) {
        var savedSuccessfully: Boolean = false
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {

// create a content values object with the image metadata
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Noa")
            }

// get the content resolver and insert a new row to the MediaStore
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

// open an output stream with the returned URI and write the image data
            uri?.let {
                val outputStream = resolver.openOutputStream(it)
                outputStream?.use { out ->
                    savedSuccessfully = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out.flush()
                    out.close()
                }
            }

// update the values with the date taken and the size
            values.clear()
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.SIZE, bitmap.byteCount.toLong())
            resolver.update(uri!!, values, null, null)

            if(savedSuccessfully) Toast.makeText(context,"Image saved",Toast.LENGTH_SHORT).show()
            else Toast.makeText(context,"Unable to save the image",Toast.LENGTH_SHORT).show()
        } else {
                val directory = File(
                    Environment.getExternalStorageDirectory().toString() + separator +Environment.DIRECTORY_PICTURES+ separator + "Noa"
                )
                // getExternalStorageDirectory is deprecated in API 29

                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val file = File(directory, fileName)
                saveImageToStream(bitmap, FileOutputStream(file))
                if (file.absolutePath != null) {
                    val values = contentValues()
                    values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                    // .DATA is deprecated in API 29
                    context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        var savedSuccessfully: Boolean = false
        if (outputStream != null) {
            try {
                savedSuccessfully = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
                GlobalScope.launch(Dispatchers.Main){
                    if(savedSuccessfully) Toast.makeText(context,"Image saved",Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context,"Unable to save the image",Toast.LENGTH_SHORT).show()
                }


            } catch (e: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }
    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Noa")
        return values
    }
    private fun downloadAndSaveImage(imageUrl: String) {
        try {

            if (android.os.Build.VERSION.SDK_INT >= 29) {
                // get the image data from the URL
                val inputStream = URL(imageUrl).openStream()
                val imageData = inputStream.readBytes()
                inputStream.close()

// create a content values object with the image metadata
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Noa")
                }
// get the content resolver and insert a new row to the MediaStore
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

// open an output stream with the returned URI and write the image data
                uri?.let {
                    val outputStream = resolver.openOutputStream(it)
                    outputStream?.use { out ->
                        out.write(imageData)
                        out.flush()
                        out.close()
                    }
                }

// update the values with the date taken and the size
                values.clear()
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.SIZE, imageData.size.toLong())
                resolver.update(uri!!, values, null, null)

// show a toast message from the main thread
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                val directory = File(
                    Environment.getExternalStorageDirectory().toString() + separator +Environment.DIRECTORY_PICTURES+ separator + "Noa"
                )
                // getExternalStorageDirectory is deprecated in API 29

                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val inputStream = URL(imageUrl).openStream()
                try {

                    val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                    val file = File(directory, fileName)
                    saveImageToStream(BitmapFactory.decodeStream(inputStream), FileOutputStream(file))
                    if (file.absolutePath != null) {
                        val values = contentValues()
                        values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                        // .DATA is deprecated in API 29
                        context.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                        )
                    }

                }catch (e:Exception){
                    GlobalScope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    inputStream.close()
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
// show a toast message from the main thread
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection =
                url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    } // Aut


}
