package com.example.qwen3.core

/**
 * Parses Qwen "thinking" blocks (<think>...</think>) out of raw model output,
 * separating the chain-of-thought from the final answer. Pure Kotlin.
 */
object ReasoningParser {

    data class Parsed(val thinking: String?, val answer: String)

    private const val OPEN = "<think>"
    private const val CLOSE = "</think>"

    fun parse(raw: String): Parsed {
        val openIdx = raw.indexOf(OPEN)
        if (openIdx < 0) {
            return Parsed(thinking = null, answer = raw.trim())
        }
        val closeIdx = raw.indexOf(CLOSE, startIndex = openIdx + OPEN.length)
        if (closeIdx < 0) {
            // Open tag but no close — treat everything after the tag as thinking,
            // leaving an empty answer (model still generating or truncated).
            val thinking = raw.substring(openIdx + OPEN.length).trim()
            return Parsed(thinking = thinking.ifEmpty { null }, answer = "")
        }
        val thinking = raw.substring(openIdx + OPEN.length, closeIdx).trim()
        val answer = (raw.substring(0, openIdx) + raw.substring(closeIdx + CLOSE.length)).trim()
        return Parsed(thinking = thinking.ifEmpty { null }, answer = answer)
    }
}
