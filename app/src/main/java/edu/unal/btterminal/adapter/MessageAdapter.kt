package edu.unal.btterminal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.unal.btterminal.R
import edu.unal.btterminal.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<Message>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)

        fun bind(message: Message) {
            val prefix = if (message.isSent) "→ " else "← "
            tvMessage.text = prefix + message.content
            tvTimestamp.text = dateFormat.format(Date(message.timestamp))
        }
    }
} 