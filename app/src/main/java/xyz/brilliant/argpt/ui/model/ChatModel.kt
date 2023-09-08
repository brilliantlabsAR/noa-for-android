package xyz.brilliant.argpt.ui.model

import android.graphics.Bitmap

data class ChatModel(
    val id: Int,// 1= normal message , 2= Open Api Key pop up msg , 3 = Stability Api Key pop up msg, 4 = url link
    val userInfo: String,
    val message: String,
    val translateEnabled : Boolean = false,
    val image : String = "",
val bitmap: Bitmap?=null
)