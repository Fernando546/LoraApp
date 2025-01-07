package com.example.applora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messageList: List<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].isSentByUser) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val senderTextView: TextView = itemView.findViewById(R.id.senderTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.message_item_sent
        } else {
            R.layout.message_item_received
        }
        val itemView = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentMessage = messageList[position]
        holder.messageTextView.text = currentMessage.message
        if (currentMessage.isSentByUser) {
            holder.senderTextView.text = "You"
            holder.senderTextView.visibility = View.VISIBLE
        } else {
            if (currentMessage.senderName != null) {
                holder.senderTextView.visibility = View.VISIBLE
                holder.senderTextView.text = currentMessage.senderName
            } else {
                holder.senderTextView.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = messageList.size
}