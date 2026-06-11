package com.example.qwen3chat.ui.chatlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qwen3chat.R
import com.example.qwen3chat.data.entity.ChatEntity
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val onItemClick: (ChatEntity) -> Unit,
    private val onItemLongClick: (ChatEntity) -> Unit
) : ListAdapter<ChatEntity, ChatListAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvChatName: TextView = itemView.findViewById(R.id.tvChatName)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvModelBadge: TextView = itemView.findViewById(R.id.tvModelBadge)

        fun bind(chat: ChatEntity) {
            tvChatName.text = chat.title.ifEmpty { "New Chat" }

            // Last message preview
            val lastMsg = chat.lastMessage
            tvLastMessage.text = lastMsg.ifEmpty { "No messages yet" }

            // Timestamp with smart formatting
            tvTime.text = formatRelativeTimestamp(chat.updatedAt)

            // Model badge
            tvModelBadge.text = chat.modelUsed
            tvModelBadge.visibility = if (chat.modelUsed.isNotEmpty()) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(chat) }
            itemView.setOnLongClickListener {
                onItemLongClick(chat)
                true
            }
        }

        private fun formatRelativeTimestamp(millis: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - millis

            return when {
                // Today (within 24 hours)
                diff < 24 * 60 * 60 * 1000L -> {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    sdf.format(Date(millis))
                }
                // Yesterday (within 48 hours)
                diff < 48 * 60 * 60 * 1000L -> "Yesterday"
                // This week
                diff < 7 * 24 * 60 * 60 * 1000L -> {
                    val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
                    sdf.format(Date(millis))
                }
                // Older
                else -> {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    sdf.format(Date(millis))
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatEntity>() {
        override fun areItemsTheSame(oldItem: ChatEntity, newItem: ChatEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatEntity, newItem: ChatEntity): Boolean =
            oldItem == newItem
    }
}
