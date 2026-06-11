package com.example.qwen3chat.ui.notes

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.qwen3chat.MarkdownRenderer
import com.example.qwen3chat.R
import com.example.qwen3chat.databinding.FragmentNoteEditorBinding
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.io.File

class NoteEditorFragment : Fragment() {

    private val viewModel: NoteEditorViewModel by viewModels()
    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!
    private var isUpdatingFromCode = false
    private lateinit var saveWatcher: TextWatcher
    private val noteId: Long by lazy { arguments?.getLong("noteId", -1L) ?: -1L }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupTabs()
        loadNote()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            // Save before navigating back
            viewModel.saveNote()
            findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    shareNote()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isEdit = tab?.position == 0
                binding.editScrollView.visibility = if (isEdit) View.VISIBLE else View.GONE
                binding.previewScrollView.visibility = if (isEdit) View.GONE else View.VISIBLE
                if (!isEdit) {
                    updatePreview()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadNote() {
        viewModel.loadNote(noteId)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.note.collect { note ->
                note?.let {
                    binding.toolbar.title = it.title
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.content.collect { content ->
                if (!isUpdatingFromCode) {
                    isUpdatingFromCode = true
                    val cursorPos = binding.etNoteContent.selectionStart
                    binding.etNoteContent.setText(content)
                    try {
                        binding.etNoteContent.setSelection(
                            cursorPos.coerceIn(0, binding.etNoteContent.text.length)
                        )
                    } catch (_: Exception) {}
                    updateCharCount(content)
                    isUpdatingFromCode = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSaving.collect { saving ->
                binding.tvAutoSave.visibility = if (saving) View.VISIBLE else View.GONE
                binding.tvAutoSave.text = if (saving) "Saving..." else "Saved"
                if (!saving) {
                    binding.tvAutoSave.postDelayed({
                        binding.tvAutoSave.visibility = View.GONE
                    }, 2000)
                }
            }
        }

        // Text change listener
        saveWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingFromCode) {
                    val text = s?.toString() ?: ""
                    viewModel.updateContent(text)
                    updateCharCount(text)
                }
            }
        }
        binding.etNoteContent.addTextChangedListener(saveWatcher)
    }

    private fun updateCharCount(text: String) {
        val chars = text.length
        val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
        binding.tvCharCount.text = "$chars chars / $words words"
    }

    private fun updatePreview() {
        val content = viewModel.content.value
        val spanned = MarkdownRenderer.render(content)
        binding.tvPreviewContent.text = spanned
    }

    private fun shareNote() {
        val content = viewModel.content.value
        val title = viewModel.getNoteTitle()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, "Share Note"))
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveNote()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.etNoteContent.removeTextChangedListener(saveWatcher)
        _binding = null
    }
}
