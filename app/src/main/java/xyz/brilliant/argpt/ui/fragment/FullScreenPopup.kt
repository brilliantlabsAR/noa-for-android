package xyz.brilliant.argpt.ui.fragment

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import com.bumptech.glide.Glide
import xyz.brilliant.argpt.R

class FullScreenPopup(context: Context, private val imageUrl: String,private val bitmap: Bitmap?) : Dialog(context) {

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
        }
        else {
            Glide.with(context)
                .load(imageUrl)
                .into(fullScreenImageView)
        }
    }
}
