package com.example.qwen3chat

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qwen3chat.databinding.ItemMessageBinding
import com.example.qwen3chat.MarkdownRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private var messageActionListener: MessageActionListener? = null) : ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(DiffCallback()) {

    interface MessageActionListener {
        fun onRegenerate()
        fun onEditAndResend(messageId: Long, currentText: String)
    }

    fun setMessageActionListener(listener: MessageActionListener) {
        this.messageActionListener = listener
    }

    /** Track which positions have already had their appear animation fired */
    private val animatedPositions = mutableSetOf<Int>()

    /** Animation duration constants */
    private companion object {
        const val APPEAR_DURATION = 250L
        const val THINKING_EXPAND_DURATION = 300L
        const val THINKING_COLLAPSE_DURATION = 250L
        const val USER_MAX_WIDTH_FRACTION = 0.80
        const val ASSISTANT_MAX_WIDTH_FRACTION = 0.88
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.messageActionListener = messageActionListener
        holder.bind(getItem(position), position, animatedPositions)
    }

    override fun onViewDetachedFromWindow(holder: MessageViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // Cancel any running animations when view is recycled
        holder.itemView.animate().cancel()
        holder.thinkingContent?.let { v -> v.animate().cancel() }
    }

    class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        var messageActionListener: MessageActionListener? = null
        val thinkingSection: View get() = binding.thinkingSection
        val thinkingHeader: View get() = binding.thinkingHeader
        val thinkingContent: View? get() = binding.thinkingContent
        val thinkingText = binding.thinkingText
        val thinkingChevron = binding.thinkingChevron
        val bubbleContainer = binding.bubbleContainer
        val bubble = binding.bubble
        val tvMessage = binding.tvMessage
        val codeBlockContainer = binding.codeBlockContainer
        val tvTimestamp = binding.tvTimestamp

        /** Annimation guard: only animate thinking expand once */
        private var isAnimatingThinking = false

        fun bind(
            message: ChatMessage,
            position: Int,
            animatedPositions: MutableSet<Int>
        ) {
            val context = binding.root.context
            val resources = context.resources
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            // ──────────────────────────────────────────
            // 1. MESSAGE TEXT
            // ──────────────────────────────────────────
            if (message.isUser) {
                tvMessage.text = message.content
            } else {
                tvMessage.text = MarkdownRenderer.render(message.content)
            }

            // ──────────────────────────────────────────
            // 2. BUBBLE BACKGROUND & TEXT COLOR
            // ──────────────────────────────────────────
            val (bgRes, textColorRes) = if (message.isUser) {
                R.drawable.bg_bubble_user to R.color.on_primary
            } else {
                R.drawable.bg_bubble_assistant to R.color.on_surface
            }
            bubble.background = ContextCompat.getDrawable(context, bgRes)
            tvMessage.setTextColor(ContextCompat.getColor(context, textColorRes))

            // ──────────────────────────────────────────
            // 3. ALIGNMENT & MAX WIDTH
            // ──────────────────────────────────────────
            val maxWidthFraction = if (message.isUser) USER_MAX_WIDTH_FRACTION else ASSISTANT_MAX_WIDTH_FRACTION
            val maxWidth = (screenWidth * maxWidthFraction).toInt()

            // Reset bubbleContainer constraints
            bubbleContainer.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                matchConstraintMaxWidth = maxWidth
                topToTop = if (message.hasThinking) {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                } else {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                }

                if (message.isUser) {
                    // User: align to end
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    marginStart = resources.getDimensionPixelSize(R.dimen.bubble_margin_start_user)
                    marginEnd = 0
                } else {
                    // Assistant: align to start
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    marginEnd = resources.getDimensionPixelSize(R.dimen.bubble_margin_end_assistant)
                    marginStart = 0
                }
            }

            // ──────────────────────────────────────────
            // 4. TIMESTAMP
            // ──────────────────────────────────────────
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTimestamp.text = timeFormat.format(Date(message.timestamp))

            tvTimestamp.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                if (message.isUser) {
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    // Also align the timestamp with the bubble container start
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                } else {
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                }
            }

            // ──────────────────────────────────────────
            // 5. LONG-PRESS COPY/SHARE
            // ──────────────────────────────────────────
            bubbleContainer.setOnLongClickListener { view ->
                showMessageActions(view, message)
                true
            }

            // ──────────────────────────────────────────
            // 5. THINKING SECTION
            // ──────────────────────────────────────────
            if (message.hasThinking) {
                thinkingSection.visibility = View.VISIBLE
                thinkingText.text = message.thinkingContent

                // Set thinking background — swap based on light/dark theme
                // (bg_thinking_expanded uses primary_container at ~30% alpha for dark;
                //  for light it renders fine as-is with the darker container color)
                thinkingText.setTextColor(
                    ContextCompat.getColor(context, R.color.on_primary_container)
                )

                // Set chevron rotation based on expanded state
                thinkingChevron.rotation = if (message.thinkingExpanded) 180f else 0f

                // Set content visibility
                if (message.thinkingExpanded) {
                    thinkingContent?.visibility = View.VISIBLE
                    thinkingContent?.alpha = 1f
                } else {
                    thinkingContent?.visibility = View.GONE
                }

                // Wire up click listener for expand/collapse
                thinkingHeader.setOnClickListener {
                    if (isAnimatingThinking) return@setOnClickListener

                    val newExpanded = !message.thinkingExpanded
                    message.thinkingExpanded = newExpanded

                    if (newExpanded) {
                        expandThinkingContent()
                    } else {
                        collapseThinkingContent()
                    }
                }
            } else {
                thinkingSection.visibility = View.GONE
            }

            // ──────────────────────────────────────────
            // 6. CODE BLOCKS (simplified for Phase 2)
            // ──────────────────────────────────────────
            if (message.codeBlocks.isNotEmpty()) {
                codeBlockContainer.visibility = View.VISIBLE
                codeBlockContainer.removeAllViews()

                message.codeBlocks.forEach { codeBlock ->
                    val codeTextView = View.inflate(context, android.R.layout.simple_list_item_1, null) as android.widget.TextView
                    codeTextView.text = codeBlock.code
                    codeTextView.textSize = 14f
                    codeTextView.typeface = android.graphics.Typeface.MONOSPACE
                    codeTextView.setBackgroundResource(R.drawable.bg_code_block)
                    codeTextView.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                    codeTextView.setPadding(
                        resources.getDimensionPixelSize(R.dimen.code_block_padding),
                        resources.getDimensionPixelSize(R.dimen.code_block_padding),
                        resources.getDimensionPixelSize(R.dimen.code_block_padding),
                        resources.getDimensionPixelSize(R.dimen.code_block_padding)
                    )
                    val layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.space_4)
                    codeTextView.layoutParams = layoutParams
                    codeBlockContainer.addView(codeTextView)
                }
            } else {
                codeBlockContainer.visibility = View.GONE
            }

            // ──────────────────────────────────────────
            // 7. APPEAR ANIMATION (only for new messages)
            // ──────────────────────────────────────────
            if (position !in animatedPositions) {
                animatedPositions.add(position)
                animateAppear(binding.root)
            }
        }

        /** Expand thinking content with height + alpha animation */
        private fun expandThinkingContent() {
            val content = thinkingContent ?: return
            isAnimatingThinking = true

            // Show the view first to measure
            content.visibility = View.VISIBLE
            content.alpha = 0f

            // Measure to get target height
            content.measure(
                View.MeasureSpec.makeMeasureSpec(
                    content.parent?.let { (it as View).measuredWidth } ?: 0,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val targetHeight = content.measuredHeight

            // Animate height from 0 to target
            val heightAnimator = ValueAnimator.ofInt(0, targetHeight).apply {
                duration = THINKING_EXPAND_DURATION
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    content.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = value
                    }
                }
            }

            // Animate alpha fade in
            val alphaAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = THINKING_EXPAND_DURATION
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    content.alpha = animator.animatedValue as Float
                }
            }

            // Rotate chevron
            thinkingChevron.animate()
                .rotation(180f)
                .setDuration(THINKING_EXPAND_DURATION)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()

            heightAnimator.start()
            alphaAnimator.start()

            // Reset height to WRAP_CONTENT after animation
            heightAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    content.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    isAnimatingThinking = false
                }
            })
        }

        /** Collapse thinking content with height + alpha animation */
        private fun collapseThinkingContent() {
            val content = thinkingContent ?: return
            isAnimatingThinking = true

            val startHeight = content.height

            // Animate height from current to 0
            val heightAnimator = ValueAnimator.ofInt(startHeight, 0).apply {
                duration = THINKING_COLLAPSE_DURATION
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    content.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = value
                    }
                }
            }

            // Animate alpha fade out
            val alphaAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = THINKING_COLLAPSE_DURATION
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    content.alpha = animator.animatedValue as Float
                }
            }

            // Rotate chevron back
            thinkingChevron.animate()
                .rotation(0f)
                .setDuration(THINKING_COLLAPSE_DURATION)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()

            heightAnimator.start()
            alphaAnimator.start()

            heightAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    content.visibility = View.GONE
                    content.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    isAnimatingThinking = false
                }
            })
        }

        /** Fade + slide up animation for new message items */
        private fun animateAppear(view: View) {
            view.alpha = 0f
            view.translationY = 24f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(APPEAR_DURATION)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }

        /** Long-press popup with Copy, Share, Regenerate, Edit & Resend actions */
        private fun showMessageActions(view: View, message: ChatMessage) {
            val context = view.context
            val popup = android.widget.PopupMenu(context, view)
            popup.menu.add(0, ACTION_COPY, 0, "Copy")
            popup.menu.add(0, ACTION_SHARE, 0, "Share")

            // Assistant messages: offer Regenerate
            if (!message.isUser) {
                popup.menu.add(0, ACTION_REGENERATE, 0, "Regenerate")
            }

            // User messages: offer Edit & Resend
            if (message.isUser) {
                popup.menu.add(0, ACTION_EDIT_RESEND, 0, "Edit & Resend")
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    ACTION_COPY -> {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("message", message.content))
                        android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                        true
                    }
                    ACTION_SHARE -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, message.content)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share message"))
                        true
                    }
                    ACTION_REGENERATE -> {
                        messageActionListener?.onRegenerate()
                        true
                    }
                    ACTION_EDIT_RESEND -> {
                        showEditDialog(context, message.id, message.content)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        /** Dialog to edit message text before resending */
        private fun showEditDialog(context: android.content.Context, messageId: Long, currentText: String) {
            val input = android.widget.EditText(context).apply {
                setText(currentText)
                setSelection(currentText.length)
                setLines(3)
                maxLines = 5
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Edit Message")
                .setView(input)
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("Resend") { _, _ ->
                    val newText = input.text.toString().trim()
                    if (newText.isNotEmpty()) {
                        messageActionListener?.onEditAndResend(messageId, newText)
                    } else {
                        android.widget.Toast.makeText(context, "Message cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        companion object {
            private const val ACTION_COPY = 1
            private const val ACTION_SHARE = 2
            private const val ACTION_REGENERATE = 3
            private const val ACTION_EDIT_RESEND = 4
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.content == newItem.content
                    && oldItem.thinkingExpanded == newItem.thinkingExpanded
                    && oldItem.hasThinking == newItem.hasThinking
        }
    }
}