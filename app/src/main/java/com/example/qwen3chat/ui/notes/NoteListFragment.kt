package com.example.qwen3chat.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qwen3chat.R
import com.example.qwen3chat.data.entity.NoteEntity
import com.example.qwen3chat.databinding.FragmentNoteListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class NoteListFragment : Fragment() {

    private val viewModel: NoteListViewModel by viewModels()
    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NoteListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSearch()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sort_name -> {
                    viewModel.setSortMode(NoteSortMode.NAME)
                    binding.tvSortLabel.text = "Sort: Name"
                    true
                }
                R.id.action_sort_modified -> {
                    viewModel.setSortMode(NoteSortMode.MODIFIED)
                    binding.tvSortLabel.text = "Sort: Last modified"
                    true
                }
                R.id.action_sort_created -> {
                    viewModel.setSortMode(NoteSortMode.CREATED)
                    binding.tvSortLabel.text = "Sort: Created"
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = NoteListAdapter(
            onItemClick = { note -> navigateToEditor(note) },
            onItemLongClick = { note -> showNoteOptions(note) }
        )
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotes.adapter = adapter
    }

    private fun setupFab() {
        binding.fabNewNote.setOnClickListener {
            showCreateNoteDialog()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                adapter.submitList(notes)
                binding.emptyState.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun showCreateNoteDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Note title"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Note")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.createNote(title) { noteId ->
                        navigateToEditor(noteId)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNoteOptions(note: NoteEntity) {
        val items = mutableListOf<Pair<String, () -> Unit>>(
            "Rename" to { showRenameDialog(note) },
            "Delete" to { showDeleteDialog(note) }
        )
        if (note.isPinned) {
            items.add(0, "Unpin" to { viewModel.togglePin(note.id) })
        } else {
            items.add(0, "Pin" to { viewModel.togglePin(note.id) })
        }

        val labels = items.map { it.first }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(note.title)
            .setItems(labels) { _, which ->
                items[which].second.invoke()
            }
            .show()
    }

    private fun showRenameDialog(note: NoteEntity) {
        val input = EditText(requireContext()).apply {
            setText(note.title)
            selectAll()
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Note")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    viewModel.renameNote(note.id, newTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(note: NoteEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Delete \"${note.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(note.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToEditor(note: NoteEntity) {
        navigateToEditor(note.id)
    }

    private fun navigateToEditor(noteId: Long) {
        val bundle = Bundle().apply {
            putLong("noteId", noteId)
        }
        findNavController().navigate(R.id.action_noteList_to_noteEditor, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
