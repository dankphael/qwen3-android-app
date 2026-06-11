package com.example.qwen3chat

/**
 * Streaming state machine parser for Qwen3 think blocks and code fence extraction.
 *
 * States:
 *   IDLE       - haven't seen <thinking> yet
 *   IN_THINK   - between <thinking> and </thinking>, buffering reasoning
 *   AFTER_THINK - saw </thinking>, remaining output is the answer
 *
 * Supports both `<thinking>...</thinking>` and legacy ` thinking... response` tag formats.
 * Also extracts fenced code blocks (``` ... ```) from the answer portion.
 */
class ReasoningParser {

    enum class State {
        IDLE,
        IN_THINK,
        AFTER_THINK
    }

    var state: State = State.IDLE
        private set

    private val thinkingBuffer = StringBuilder()
    private val answerBuffer = StringBuilder()
    private val codeBlocks = mutableListOf<CodeBlock>()

    val thinking: String? get() = thinkingBuffer.toString().trim().ifEmpty { null }
    val answer: String get() = answerBuffer.toString().trim()
    val extractedCodeBlocks: List<CodeBlock> get() = codeBlocks.toList()

    /**
     * Feed a chunk of model output text. Can be called incrementally
     * (token by token) or all at once with the full output.
     */
    fun feed(chunk: String) {
        var i = 0
        while (i < chunk.length) {
            when (state) {
                State.IDLE -> {
                    // Try HTML-style <thinking> first, then legacy  thinking
                    val htmlStart = chunk.indexOf(THINK_OPEN_HTML, i)
                    val legacyStart = chunk.indexOf(THINK_OPEN_LEGACY, i)

                    val startIndex = when {
                        htmlStart != -1 && (legacyStart == -1 || htmlStart < legacyStart) -> {
                            state = State.IN_THINK
                            if (htmlStart > i) answerBuffer.append(chunk, i, htmlStart)
                            htmlStart + THINK_OPEN_HTML.length
                        }
                        legacyStart != -1 -> {
                            state = State.IN_THINK
                            if (legacyStart > i) answerBuffer.append(chunk, i, legacyStart)
                            legacyStart + THINK_OPEN_LEGACY.length
                        }
                        else -> {
                            answerBuffer.append(chunk, i, chunk.length)
                            chunk.length
                        }
                    }
                    i = startIndex
                }

                State.IN_THINK -> {
                    val htmlEnd = chunk.indexOf(THINK_CLOSE_HTML, i)
                    val legacyEnd = chunk.indexOf(THINK_CLOSE_LEGACY, i)

                    val endIndex = when {
                        htmlEnd != -1 && (legacyEnd == -1 || htmlEnd < legacyEnd) -> {
                            thinkingBuffer.append(chunk, i, htmlEnd)
                            state = State.AFTER_THINK
                            htmlEnd + THINK_CLOSE_HTML.length
                        }
                        legacyEnd != -1 -> {
                            thinkingBuffer.append(chunk, i, legacyEnd)
                            state = State.AFTER_THINK
                            legacyEnd + THINK_CLOSE_LEGACY.length
                        }
                        else -> {
                            thinkingBuffer.append(chunk, i, chunk.length)
                            chunk.length
                        }
                    }
                    i = endIndex
                }

                State.AFTER_THINK -> {
                    // Buffer answer text; code blocks are extracted after full parse
                    answerBuffer.append(chunk, i, chunk.length)
                    i = chunk.length
                }
            }
        }
    }

    /** Extract code blocks from the answer text. Call after feeding is complete. */
    private fun extractCodeBlocks(text: String): List<CodeBlock> {
        val blocks = mutableListOf<CodeBlock>()
        val regex = Regex("```(\\w*)\\s*\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
        for (match in regex.findAll(text)) {
            val language = match.groupValues[1].ifEmpty { "" }
            val code = match.groupValues[2].trim()
            if (code.isNotEmpty()) {
                blocks.add(CodeBlock(language, code))
            }
        }
        return blocks
    }

    /**
     * Get the answer text with code fences stripped for display.
     * Each fenced block is replaced with just its language label.
     */
    fun getDisplayAnswer(): String {
        val text = answer
        return text.replace(Regex("```\\w*\\s*\\n[\\s\\S]*?```"), "[code block]")
            .trim()
    }

    /** Reset for reuse */
    fun reset() {
        state = State.IDLE
        thinkingBuffer.clear()
        answerBuffer.clear()
        codeBlocks.clear()
    }

    /** Produce a ChatMessage from the current parsed state. */
    fun toChatMessage(messageId: Long = System.currentTimeMillis()): ChatMessage {
        val displayText = getDisplayAnswer()
        val blocks = extractCodeBlocks(answer)
        return ChatMessage(
            id = messageId,
            content = displayText.ifEmpty { if (thinking != null) "" else "--" },
            isUser = false,
            hasThinking = thinking != null,
            thinkingContent = thinking ?: "",
            thinkingExpanded = false,
            codeBlocks = blocks
        )
    }

    companion object {
        private const val THINK_OPEN_HTML = "<thinking>"
        private const val THINK_CLOSE_HTML = "</thinking>"
        private const val THINK_OPEN_LEGACY = " thinking"
        private const val THINK_CLOSE_LEGACY = " response"

        /**
         * Convenience: parse all text at once and return result.
         */
        fun parse(raw: String): ParsedResponse {
            val parser = ReasoningParser()
            parser.feed(raw)
            return ParsedResponse(
                thinking = parser.thinking,
                answer = parser.answer
            )
        }
    }
}

data class ParsedResponse(
    val thinking: String?,
    val answer: String
)