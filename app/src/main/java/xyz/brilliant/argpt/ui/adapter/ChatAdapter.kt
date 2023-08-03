package xyz.brilliant.argpt.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.model.ChatModel


class ChatAdapter(private val messages: List<ChatModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val ITEM_LEFT = 1
    private val ITEM_RIGHT = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].userInfo === "S") 2 else 1
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

       return when (viewType) {
            ITEM_LEFT -> return LeftChatViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.chat_item_cell_left, parent, false)
            )
            ITEM_RIGHT -> return RightChatViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.chat_item_cell_right, parent, false)
            )
           else -> {
               throw Exception("Error reading holder type")
           }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage: ChatModel = messages[position]

        if (holder.itemViewType === ITEM_LEFT) {
            val viewHolder: LeftChatViewHolder = holder as LeftChatViewHolder
            viewHolder.contents.text = chatMessage.message
            //timeStampStr = chatMessage.getTime()
            //viewHolder.time.setText(timeStampStr)
        } else {
            val viewHolder: RightChatViewHolder = holder as RightChatViewHolder
            viewHolder.contents.text=chatMessage.message
           // timeStampStr = chatMessage.getTime()
           //viewHolder.time.setText(timeStampStr)
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class LeftChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contents: TextView
        var time: TextView

        init {
            contents = itemView.findViewById<View>(R.id.messageText) as TextView
            time = itemView.findViewById<View>(R.id.timeText) as TextView
        }
    }


    class RightChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var contents: TextView
        var time: TextView

        init {
            contents = itemView.findViewById<View>(R.id.messageText) as TextView
            time = itemView.findViewById<View>(R.id.timeText) as TextView
        }
    }
}