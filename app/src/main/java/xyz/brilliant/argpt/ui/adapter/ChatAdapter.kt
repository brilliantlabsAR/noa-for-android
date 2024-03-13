package xyz.brilliant.argpt.ui.adapter

import android.graphics.Bitmap
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.model.ChatModel


class ChatAdapter(private val messages: List<ChatModel>,  private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val ITEM_LEFT = 1
    private val ITEM_RIGHT = 2
    private val ITEM_CENTER = 3

    interface OnItemClickListener {
        fun onUrlClick(position: Int, url: String)
        fun onImageClick(position: Int, url: String, bitmap: Bitmap?)
        fun onStabilityApiClick(position: Int, chatModel: ChatModel)
        fun onOpenApiClick(position: Int, chatModel: ChatModel)
    }
    override fun getItemViewType(position: Int): Int {


        return if(messages[position].translateEnabled) {
            3
        } else {
            if (messages[position].userInfo === "S") 2 else 1
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            ITEM_LEFT ->  LeftChatViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.chat_item_cell_left_with_corner, parent, false)
            )
            ITEM_RIGHT ->  RightChatViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.chat_item_cell_right_with_corner, parent, false)
            )
            ITEM_CENTER->  CenterChatViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.chat_item_cell_center, parent, false)
            )
            else -> {
                throw Exception("Error reading holder type")
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage: ChatModel = messages[position]
        val context = holder.itemView.context

        if (holder.itemViewType == ITEM_CENTER) {
            val viewHolder: CenterChatViewHolder = holder as CenterChatViewHolder
            viewHolder.contents.text = chatMessage.message
            if(chatMessage.message.isEmpty())
            {viewHolder.contents.visibility = View.GONE}
            else
            {viewHolder.contents.visibility = View.VISIBLE}

        }
        else if (holder.itemViewType == ITEM_LEFT) {
            val viewHolder: LeftChatViewHolder = holder as LeftChatViewHolder
            viewHolder.contents.text = chatMessage.message
            //For Click..
            Linkify.addLinks(viewHolder.contents,Linkify.WEB_URLS)
            viewHolder.contents.movementMethod = LinkMovementMethod.getInstance()
            viewHolder.contents.movementMethod = LinkMovementMethod.getInstance()




            if(chatMessage.bitmap != null)
            {
                viewHolder.chtImage.visibility = View.VISIBLE
                viewHolder.chtImage.setImageBitmap(chatMessage.bitmap)
            }

            else if(chatMessage.image.isNotEmpty())
            {
                viewHolder.chtImage.visibility = View.VISIBLE
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the image
                    .placeholder(R.drawable.round_white_background) // Placeholder image while loading
                    .error(R.drawable.round_white_background) // Image to display if loading fails

                Glide.with(context)
                    .load(chatMessage.image)
                    .apply(requestOptions)
                    .into(holder.chtImage)
            }
            else{
                viewHolder.chtImage.visibility = View.GONE
            }

            if(chatMessage.message.isEmpty())
            {viewHolder.contents.visibility = View.GONE}
            else
            {viewHolder.contents.visibility = View.VISIBLE}


            when (chatMessage.id) {
                2 -> {
                    viewHolder.layoutMsg.setOnClickListener {
                        onItemClickListener.onOpenApiClick(position,chatMessage)
                    }
                    viewHolder.contents.setOnClickListener {
                        //Open api key dialog
                        onItemClickListener.onOpenApiClick(position,chatMessage)
                    }
                }
                3 -> {
                    viewHolder.layoutMsg.setOnClickListener{
                        //Open api key dialog
                        onItemClickListener.onOpenApiClick(position,chatMessage)
                    }
                    viewHolder.contents.setOnClickListener {
                        //Open api key dialog
                        onItemClickListener.onOpenApiClick(position,chatMessage)
                    }
                }
                1 -> {
                    viewHolder.chtImage.setOnClickListener {
                        onItemClickListener.onImageClick(position,chatMessage.image,chatMessage.bitmap)
                    }
                    viewHolder.contents.setOnClickListener {
                        //Do nothing
                    }
                }
            }

        } else {
            val viewHolder: RightChatViewHolder = holder as RightChatViewHolder
            viewHolder.contents.text=chatMessage.message
            viewHolder.contents.setOnClickListener {
            }
            if(chatMessage.bitmap != null)
            {
                viewHolder.chtImage.visibility = View.VISIBLE
                viewHolder.chtImage.setImageBitmap(chatMessage.bitmap)
            }
            else if(chatMessage.image.isNotEmpty())
            {
                viewHolder.chtImage.visibility = View.VISIBLE
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the image
                    .placeholder(R.drawable.round_white_background) // Placeholder image while loading
                    .error(R.drawable.round_white_background) // Image to display if loading fails

                Glide.with(context)
                    .load(chatMessage.image)
                    .apply(requestOptions)
                    .into(holder.chtImage)
            }
            else{
                viewHolder.chtImage.visibility = View.GONE
            }

            if(chatMessage.message.isEmpty())
            {viewHolder.contents.visibility = View.GONE}
            else
            {viewHolder.contents.visibility = View.VISIBLE}
         if(chatMessage.id==1)
            {
                viewHolder.chtImage.setOnClickListener {
                    onItemClickListener.onImageClick(position,chatMessage.image,chatMessage.bitmap)
                }
            }
        }
    }
    override fun getItemCount(): Int {
        return messages.size
    }

    class LeftChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contents: TextView
        var chtImage: ImageView
        var layoutMsg : LinearLayout
//        var time: TextView

        init {
            layoutMsg = itemView.findViewById<View>(R.id.layoutMsg) as LinearLayout
            contents = itemView.findViewById<View>(R.id.messageText) as TextView
            chtImage = itemView.findViewById<View>(R.id.cht_image) as ImageView
//            time = itemView.findViewById<View>(R.id.timeText) as TextView
        }
    }


    class RightChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contents: TextView
        private var layoutMsg : LinearLayout
        var chtImage: ImageView

        init {
            layoutMsg = itemView.findViewById<View>(R.id.layoutMsg) as LinearLayout
            contents = itemView.findViewById<View>(R.id.messageText) as TextView
            chtImage = itemView.findViewById<View>(R.id.cht_image) as ImageView
        }
    }

    class CenterChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contents: TextView
        private var chtImage: ImageView

        init {
            contents = itemView.findViewById<View>(R.id.messageText) as TextView
            chtImage = itemView.findViewById<View>(R.id.cht_image) as ImageView
        }
    }
}