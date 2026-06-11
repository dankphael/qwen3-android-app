package com.example.qwen3chat.ui.notes

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qwen3chat.data.entity.NoteEntity
import com.example.qwen3chat.databinding.ItemNoteBinding
import java.util.Date

class NoteListAdapter(
    private val onItemClick: (NoteEntity) -> Unit,
    private val onItemLongClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NoteListAdapter.NoteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) {
            binding.tvNoteTitle.text = note.title
            binding.tvNoteSnippet.text = note.snippet.ifEmpty { "No content" }
            binding.tvNoteDate.text = formatDate(note.updatedAt)
            binding.tvPinIcon.visibility = if (note.isPinned) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvNoteFolder.visibility = if (note.folder.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvNoteFolder.text = if (note.folder.isNotEmpty()) "\uD83D\uDCC1 ${note.folder}" else ""
            binding.root.setOnClickListener { onItemClick(note) }
            binding.root.setOnLongClickListener {
                onItemLongClick(note)
                true
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        return DateFormat.format("MMM d, yyyy", date).toString()
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
            oldItem == newItem
    }
}
