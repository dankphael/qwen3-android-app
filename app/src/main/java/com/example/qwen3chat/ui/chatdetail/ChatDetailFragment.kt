package com.example.qwen3chat.ui.chatdetail

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qwen3chat.ChatAdapter
import com.example.qwen3chat.ChatMessage
import com.example.qwen3chat.MainActivity
import com.example.qwen3chat.R
import com.example.qwen3chat.databinding.FragmentChatDetailBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import java.util.Locale

/**
 * Chat detail fragment with Phase 3 redesign:
 * - Pill-shaped input bar with stateful send button
 * - Empty state with suggestion chips
 * - Typing indicator
 */
class ChatDetailFragment : Fragment(), ChatAdapter.MessageActionListener {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatDetailViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    private var chatMessages = mutableListOf<ChatMessage>()

    /** Send button state: IDLE = mic/transparent, READY = arrow_up/primary, SENDING = stop/error */
    private enum class SendState { IDLE, READY, SENDING }

    private var currentSendState = SendState.IDLE
    private var generationStartTime = 0L

    companion object {
        private const val SPEECH_REQUEST_CODE = 100
        private const val STOP_CONFIRMATION_THRESHOLD_MS = 2000
    }

    /** FastOutSlowIn interpolator for button morph transitions */
    private val fastOutSlowIn = PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)

    /** Cached colors for send button states */
    private var colorPrimary = 0
    private var colorError = 0
    private var colorTransparent = 0
    private var colorOnSurfaceVariant = 0
    private var colorOnPrimary = 0
    private var colorWhite = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId = arguments?.getLong("chatId", -1L) ?: -1L
        if (chatId == -1L) {
            Toast.makeText(requireContext(), "Invalid chat", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // Cache colors
        colorPrimary = ContextCompat.getColor(requireContext(), R.color.primary)
        colorError = ContextCompat.getColor(requireContext(), R.color.error)
        colorTransparent = android.graphics.Color.TRANSPARENT
        colorOnSurfaceVariant = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        colorOnPrimary = ContextCompat.getColor(requireContext(), R.color.on_primary)
        colorWhite = android.graphics.Color.WHITE

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Attach the shared model from MainActivity
        val sharedModel = MainActivity.sharedChatViewModel
        viewModel.attachModel(sharedModel?.qwenModelInstance)

        setupRecyclerView()
        setupTextWatcher()
        setupSendButton()
        setupChipClickListeners()
        setupObservers()

        // Wire up toolbar menu (export)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_export -> {
                    exportConversation()
                    true
                }
                else -> false
            }
        }

        viewModel.setChatId(chatId)
        binding.toolbar.title = "Chat"
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(this)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChat.adapter = chatAdapter
    }

    /**
     * Text watcher drives send button state between IDLE and READY.
     */
    private fun setupTextWatcher() {
        binding.editTextInput.doOnTextChanged { text, _, _, _ ->
            val hasText = text?.isNotEmpty() == true
            if (currentSendState == SendState.SENDING) return@doOnTextChanged
            if (hasText && currentSendState == SendState.IDLE) {
                transitionToReady()
            } else if (!hasText && currentSendState == SendState.READY) {
                transitionToIdle()
            }
        }
    }

    /** Running animation for send button morph */
        private var sendButtonAnimator: AnimatorSet? = null

    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            when (currentSendState) {
                SendState.IDLE -> {
                    // Launch voice input when idle
                    launchSpeechRecognition()
                }
                SendState.READY -> {
                    val message = binding.editTextInput.text.toString().trim()
                    if (message.isNotEmpty() && viewModel.isModelLoaded) {
                        binding.editTextInput.text?.clear()
                        transitionToSending()
                        viewModel.sendMessage(
                            text = message,
                            onResponseStart = { /* already in SENDING state */ },
                            onError = { errorMsg ->
                                transitionToIdle()
                                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                SendState.SENDING -> {
                    // Check if generation is very early (< 2 seconds)
                    val elapsedMs = System.currentTimeMillis() - generationStartTime
                    if (elapsedMs < STOP_CONFIRMATION_THRESHOLD_MS) {
                        // Confirm early stop to prevent accidental taps
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Stop Generation?")
                            .setMessage("Generation just started. Stop anyway?")
                            .setNegativeButton("Continue") { _, _ -> }
                            .setPositiveButton("Stop") { _, _ ->
                                viewModel.stopGeneration()
                                transitionToIdle()
                            }
                            .show()
                    } else {
                        // Enough time has passed, stop immediately
                        viewModel.stopGeneration()
                        transitionToIdle()
                    }
                }
            }
        }
    }

    private fun launchSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Speech recognition not available: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val recognizedText = results[0]
                binding.editTextInput.setText(recognizedText)
                binding.editTextInput.setSelection(recognizedText.length)
                // Transition to READY state
                transitionToReady()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // MessageActionListener implementation (long-press menu callbacks)
    // ──────────────────────────────────────────────────────────────────

    override fun onRegenerate() {
        viewModel.regenerate(onError = { errorMsg ->
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        })
        transitionToSending()
    }

    override fun onEditAndResend(messageId: Long, newText: String) {
        viewModel.editAndResend(messageId, newText, onError = { errorMsg ->
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        })
        transitionToSending()
    }

    /** IDLE state: mic icon, transparent background */
    private fun transitionToIdle() {
        currentSendState = SendState.IDLE
        animateSendButton(
            iconRes = R.drawable.ic_mic,
            tintColor = colorOnSurfaceVariant,
            bgColor = colorTransparent,
            bgTintList = null
        )
    }

    /** READY state: arrow_up icon, primary background */
    private fun transitionToReady() {
        currentSendState = SendState.READY
        animateSendButton(
            iconRes = R.drawable.ic_send_filled,
            tintColor = colorWhite,
            bgColor = colorPrimary,
            bgTintList = null
        )
    }

    /** SENDING state: stop icon, error background */
    private fun transitionToSending() {
        currentSendState = SendState.SENDING
        generationStartTime = System.currentTimeMillis()
        animateSendButton(
            iconRes = R.drawable.ic_stop,
            tintColor = colorWhite,
            bgColor = colorError,
            bgTintList = null
        )
    }

    /**
     * Animate send button transition using property animators (200ms FastOutSlowIn).
     * We animate: icon alpha (crossfade), background color, and scale for a morph feel.
     */
    private fun animateSendButton(iconRes: Int, tintColor: Int, bgColor: Int, bgTintList: android.content.res.ColorStateList?) {
        val btn = binding.buttonSend

        // Cancel any running animation
        sendButtonAnimator?.cancel()

        // Scale down slightly for morph effect
        val scaleDown = ObjectAnimator.ofFloat(btn, "scaleX", 1f, 0.85f).apply {
            duration = 100
            interpolator = fastOutSlowIn
        }
        val scaleDownY = ObjectAnimator.ofFloat(btn, "scaleY", 1f, 0.85f).apply {
            duration = 100
            interpolator = fastOutSlowIn
        }

        // Change icon and tint at the midpoint
        val iconChange = ObjectAnimator.ofFloat(btn, "alpha", 1f, 0f).apply {
            duration = 100
            interpolator = fastOutSlowIn
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    btn.setIconResource(iconRes)
                    btn.iconTint = android.content.res.ColorStateList.valueOf(tintColor)
                    if (bgTintList != null) {
                        btn.backgroundTintList = bgTintList
                    } else {
                        btn.setBackgroundColor(bgColor)
                    }
                    btn.alpha = 0f
                }
            })
        }

        // Scale back up and fade in with new icon
        val scaleUp = ObjectAnimator.ofFloat(btn, "scaleX", 0.85f, 1f).apply {
            duration = 100
            interpolator = fastOutSlowIn
        }
        val scaleUpY = ObjectAnimator.ofFloat(btn, "scaleY", 0.85f, 1f).apply {
            duration = 100
            interpolator = fastOutSlowIn
        }
        val fadeIn = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f).apply {
            duration = 100
            interpolator = fastOutSlowIn
        }

        val set = AnimatorSet()
        set.play(scaleDown).with(scaleDownY)
        set.play(iconChange).after(scaleDown)
        set.play(scaleUp).with(scaleUpY).with(fadeIn).after(iconChange)
        set.start()
        sendButtonAnimator = set
    }

    /** Empty state suggestion chip click handlers */
    private fun setupChipClickListeners() {
        val chips = listOf(
            binding.emptyStateContainer.chipSuggestion1,
            binding.emptyStateContainer.chipSuggestion2,
            binding.emptyStateContainer.chipSuggestion3,
            binding.emptyStateContainer.chipSuggestion4
        )
        for (chip in chips) {
            chip.setOnClickListener { view ->
                val text = (view as? TextView)?.text?.toString() ?: return@setOnClickListener
                sendFromChip(text)
            }
        }
    }

    /** When a suggestion chip is tapped, fill the input and simulate send */
    private fun sendFromChip(text: String) {
        if (!viewModel.isModelLoaded) {
            Toast.makeText(requireContext(), "Model is still loading...", Toast.LENGTH_SHORT).show()
            return
        }
        // Set the text to trigger READY state, then simulate send
        binding.editTextInput.setText(text)
        binding.editTextInput.setSelection(text.length)
        // Wait for layout then trigger send
        binding.editTextInput.post {
            binding.buttonSend.performClick()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingHistory.collect { loading ->
                binding.progressBarHistory.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                chatMessages.clear()
                for (msg in messages) {
                    chatMessages.add(
                        ChatMessage(
                            content = msg.content,
                            isUser = msg.role == "user",
                            hasThinking = !msg.thinking.isNullOrEmpty(),
                            thinkingContent = msg.thinking ?: ""
                        )
                    )
                }
                chatAdapter.submitList(chatMessages.toList())

                val hasMessages = chatMessages.isNotEmpty()

                // Toggle empty state / recyclerview
                binding.emptyStateContainer.root.visibility = if (hasMessages) View.GONE else View.VISIBLE
                binding.recyclerViewChat.visibility = if (hasMessages) View.VISIBLE else View.GONE

                if (hasMessages) {
                    binding.recyclerViewChat.scrollToPosition(chatMessages.size - 1)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isGenerating.collect { generating ->
                binding.typingIndicator.visibility = if (generating) View.VISIBLE else View.GONE
                if (generating) {
                    // Add a placeholder assistant message for streaming tokens to append to
                    if (chatMessages.isEmpty() || chatMessages.last().isUser) {
                        chatMessages.add(ChatMessage(content = "", isUser = false))
                        chatAdapter.submitList(chatMessages.toList())
                    }
                } else {
                    // Reset send button to IDLE or READY based on text
                    val hasText = binding.editTextInput.text?.isNotEmpty() == true
                    if (hasText) transitionToReady() else transitionToIdle()
                }
            }
        }

        // Observe streaming tokens — append each token to the last assistant message live
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.streamingToken.collect { token ->
                if (token.isEmpty()) return@collect
                if (chatMessages.isEmpty()) return@collect
                // Append token to the last message (which should be the in-progress assistant message)
                val lastIdx = chatMessages.lastIndex
                val last = chatMessages[lastIdx]
                chatMessages[lastIdx] = last.copy(content = last.content + token)
                chatAdapter.notifyItemChanged(lastIdx)
                binding.recyclerViewChat.scrollToPosition(lastIdx)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isModelReady.collect { ready ->
                binding.inputBar.visibility = if (ready) View.VISIBLE else View.GONE
            }
        }

        // Observe chat title changes (auto-title from first message)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatTitle.collect { title ->
                binding.toolbar.title = title
            }
        }

        // Observe model used for this chat (show as subtitle)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chatModelUsed.collect { modelName ->
                binding.toolbar.subtitle = modelName
            }
        }

        // Observe token speed + context usage combined
        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.tokensPerSecond, viewModel.contextUsage) { speed, context ->
                Pair(speed, context)
            }.collect { (speed, context) ->
                if (speed > 0f) {
                    binding.tvSpeedIndicator.visibility = View.VISIBLE
                    val ctxStr = if (context.isNotEmpty()) " · $context" else ""
                    binding.tvSpeedIndicator.text = String.format("%.1f tok/s%s", speed, ctxStr)
                } else {
                    binding.tvSpeedIndicator.visibility = View.GONE
                }
            }
        }
    }

    private fun exportConversation() {
        viewLifecycleOwner.lifecycleScope.launch {
            val messages = viewModel.exportChat()
            if (messages.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Nothing to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val sb = StringBuilder()
            sb.appendLine("Qwen3 Chat — ${binding.toolbar.title}")
            sb.appendLine("Exported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
            sb.appendLine("=".repeat(40))
            sb.appendLine()

            for (msg in messages) {
                val role = if (msg.role == "user") "You" else "Assistant"
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                sb.appendLine("[$role · $time]")
                sb.appendLine(msg.content)
                sb.appendLine()
            }

            val intent = android.content.Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Qwen3 Chat — ${binding.toolbar.title}")
            }
            startActivity(android.content.Intent.createChooser(intent, "Export Chat"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}