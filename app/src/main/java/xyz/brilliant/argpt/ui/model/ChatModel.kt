package xyz.brilliant.argpt.ui.model

import android.graphics.Bitmap

data class ChatModel(
    val id: Int,
    val userInfo: String,
    val message: String,
    val translateEnabled : Boolean = false,
    val image : String = "",
val bitmap: Bitmap?=null
)