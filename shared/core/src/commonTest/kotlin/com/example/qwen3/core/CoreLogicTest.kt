package com.example.qwen3.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CoreLogicTest {

    // ── DeviceCapability tier classification ──
    @Test
    fun highTier() {
        assertEquals(DeviceCapability.Tier.HIGH, DeviceCapability.classifyTier(8000, 8))
    }

    @Test
    fun midTier() {
        assertEquals(DeviceCapability.Tier.MID, DeviceCapability.classifyTier(6000, 6))
    }

    @Test
    fun lowTierWhenFewCores() {
        assertEquals(DeviceCapability.Tier.LOW, DeviceCapability.classifyTier(8000, 4))
    }

    @Test
    fun lowTierWhenLittleRam() {
        assertEquals(DeviceCapability.Tier.LOW, DeviceCapability.classifyTier(3000, 8))
    }

    // ── ReasoningParser ──
    @Test
    fun parsesThinkingBlock() {
        val p = ReasoningParser.parse("<think>step by step</think>The answer is 42.")
        assertEquals("step by step", p.thinking)
        assertEquals("The answer is 42.", p.answer)
    }

    @Test
    fun noThinkingBlock() {
        val p = ReasoningParser.parse("Just an answer.")
        assertNull(p.thinking)
        assertEquals("Just an answer.", p.answer)
    }

    @Test
    fun unclosedThinkingBlock() {
        val p = ReasoningParser.parse("<think>still reasoning")
        assertEquals("still reasoning", p.thinking)
        assertEquals("", p.answer)
    }

    // ── ChatRepository title generation ──
    @Test
    fun shortTitleKept() {
        assertEquals("Hello there", ChatRepository.generateTitle("Hello there"))
    }

    @Test
    fun emptyTitleFallback() {
        assertEquals("New Chat", ChatRepository.generateTitle("   "))
    }

    @Test
    fun longTitleTruncatedAtWord() {
        val long = "This is a really long first message that should get truncated nicely at a word boundary"
        val title = ChatRepository.generateTitle(long)
        assertTrue(title.length <= 51)
        assertTrue(title.endsWith("…"))
    }

    // ── ModelMetadata registry ──
    @Test
    fun findByKeyWorks() {
        assertEquals("Qwen3.5-2B", ModelMetadata.findByKey("qwen3.5-2b").displayName)
    }

    @Test
    fun threeModelsRegistered() {
        assertEquals(3, ModelMetadata.ALL.size)
    }

    // ── QwenModel prompt building (with a fake engine) ──
    @Test
    fun firstTurnIncludesSystemPrompt() {
        val model = QwenModel(
            metadata = ModelMetadata.findByKey("qwen3.5-0.8b"),
            engine = FakeEngine(),
            paths = FakePaths(),
            systemPromptProvider = { "You are a test bot." },
        )
        val prompt = model.buildPrompt(listOf(ChatMessage(content = "Hi", isUser = true)), isFirstTurn = true)
        assertTrue(prompt.contains("<|im_start|>system\nYou are a test bot."))
        assertTrue(prompt.contains("<|im_start|>user\nHi<|im_end|>"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun laterTurnOmitsSystemPrompt() {
        val model = QwenModel(
            metadata = ModelMetadata.findByKey("qwen3.5-0.8b"),
            engine = FakeEngine(),
            paths = FakePaths(),
            systemPromptProvider = { "You are a test bot." },
        )
        val prompt = model.buildPrompt(listOf(ChatMessage(content = "More", isUser = true)), isFirstTurn = false)
        assertFalse(prompt.contains("system"))
    }
}
