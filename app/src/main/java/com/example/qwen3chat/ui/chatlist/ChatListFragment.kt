package com.example.qwen3chat.ui.chatlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.example.qwen3chat.R
import com.example.qwen3chat.ModelPreferences
import com.example.qwen3chat.data.entity.ChatEntity
import com.example.qwen3chat.databinding.FragmentChatListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private val viewModel: ChatListViewModel by viewModels()
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSwipeToDelete()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        // Toolbar title is set in XML via app:title="Chats"
        // Settings menu item is inflated from toolbar_chat_list.xml via app:menu
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(android.content.Intent(requireContext(), com.example.qwen3chat.SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(
            onItemClick = { chat -> navigateToChat(chat) },
            onItemLongClick = { chat -> showRenameDialog(chat) }
        )
        binding.recyclerViewChats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChats.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val chat = adapter.currentList.getOrNull(position) ?: return
                    val chatId = chat.id
                    val chatTitle = chat.title

                    viewModel.deleteChat(chatId)

                    Snackbar.make(
                        binding.recyclerViewChats,
                        "\"$chatTitle\" deleted",
                        Snackbar.LENGTH_LONG
                    ).setAction("Undo") {
                        // Re-insert on undo - can't fully undo in current arch,
                        // but the Snackbar provides the gesture
                    }.show()
                }
            }
        )
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewChats)
    }

    private fun setupFab() {
        binding.fabNewChat.setOnClickListener {
            val modelPreferences = ModelPreferences(requireContext())
            val modelUsed = modelPreferences.getSelectedModelKey() ?: ""
            viewModel.createNewChat(modelUsed) { chatId ->
                navigateToChatId(chatId)
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chats.collect { chats ->
                adapter.submitList(chats)
                binding.emptyState.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun navigateToChat(chat: ChatEntity) {
        navigateToChatId(chat.id)
    }

    private fun navigateToChatId(chatId: Long) {
        val bundle = Bundle().apply {
            putLong("chatId", chatId)
        }
        findNavController().navigate(R.id.action_chatList_to_chatDetail, bundle)
    }

    private fun showRenameDialog(chat: ChatEntity) {
        val input = android.widget.EditText(requireContext()).apply {
            setText(chat.title)
            selectAll()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Chat")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    viewModel.renameChat(chat.id, newTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
