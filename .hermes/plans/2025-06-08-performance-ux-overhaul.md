# Qwen3 Android App — Performance, UX, and User Flow Overhaul

> **For Hermes:** Execute via Suresh subagent. Three phases. Each phase is one delegate_task.

**Goal:** Transform the Qwen3 Android app from a slow, basic prototype into a performant, polished, feature-complete chat app.

**Architecture:** Fix native layer first (KV cache, threading, OpenMP, NEON), then polish UI (typography, dark mode, layouts), then add user-facing features (settings, speed indicator, share/copy, markdown, export).

**Tech Stack:** Kotlin + XML layouts + NDK C++ (llama.cpp JNI) + Room DB + Navigation Component

---

## PHASE 1: PERFORMANCE FIXES (4 tasks, biggest impact)

### Task 1.1: Fix KV Cache — Don't Clear Every Generation

**Problem:** `QwenModel.kt` calls `nativeResetCache()` at the start of EVERY `generate()` and `generateStreaming()` call. This means the entire conversation history gets re-evaluated from scratch every time we send a new message. For a 10-message chat, we re-evaluate ~1000 tokens that are already in cache. This is the #1 perf killer.

**Fix:** Use incremental KV cache. The JNI layer already tracks `g_n_tokens_cached`. The key insight: llama.cpp supports appending new tokens to an existing eval. We should only evaluate the NEW prompt tokens (the latest user message + the assistant prefix), not the full conversation history.

**Approach:**
- Modify `nativeEvaluatePrompt` to accept a `startIndex` parameter telling it how many tokens are already cached. If `g_n_tokens_cached > 0`, we skip re-evaluating cached tokens and only evaluate the new ones. This uses llama's built-in capability — the llama_batch positions must be offset correctly.
- Modify `buildPrompt()` in `QwenModel.kt` to only return the new tokens since last evaluation.
- Store `cachedMessageCount` in QwenModel to track which messages are already in cache.
- Keep `nativeResetCache()` but only call it on first message or when explicitly switching context.

**Files:**
- `llama-jni.cpp`: Modify `nativeEvaluatePrompt` to accept jint startPos parameter; use it to offset batch positions
- `LlamaEngine.kt`: Update JNI signature for nativeEvaluatePrompt
- `QwenModel.kt`: Track cached message count, only evaluate new messages

**Verification:** After first message, second message generation should show `g_n_tokens_cached` growing incrementally rather than resetting.

### Task 1.2: Remove Thread Cap + Use All Cores

**Problem:** `llama-jni.cpp` line 67: `if (n_threads > 8) n_threads = 8`. Modern Android phones have 8+ cores. The cap prevents using all available compute. Also, there's no differentiation between batch threads and single-token threads.

**Fix:**
- Remove the thread cap (`if (n_threads > 8) n_threads = 8` → remove entirely)
- Use `std::thread::hardware_concurrency()` directly as `n_threads`
- Set `n_threads_batch` to use ALL cores (for prompt eval which is compute-heavy)
- Set `n_threads` for single-token decode to use only performance cores (leave 1-2 efficiency cores for UI thread)
- Add `ctx_params.cpuparams` for big.LITTLE topology if available

**Files:** `llama-jni.cpp` lines 65-69

**Verification:** Check log output: should show thread count > 8 on capable devices.

### Task 1.3: Enable OpenMP in CMakeLists.txt

**Problem:** `GGML_OPENMP OFF` disables parallel matrix ops. OpenMP enables multi-threaded matmul, which is critical for performance.

**Fix:** 
- Change `set(GGML_OPENMP OFF ...)` to `set(GGML_OPENMP ON ...)`
- Add OpenMP flags for ARM: `-fopenmp` and link against `omp` or `gomp`
- NDK 26 supports OpenMP via `-fopenmp` with the NDK's bundled libomp

**Files:** `CMakeLists.txt`

**Verification:** Build succeeds. Check log for OpenMP thread initialization.

### Task 1.4: Device-Tier n_batch Tuning + NEON Flags

**Problem:** `n_batch = 512` is arbitrary. Low-end devices may OOM, high-end devices can handle more. Also, NEON SIMD optimizations should be explicitly enabled.

**Fix:**
- Add tier-based batch sizes: LOW=256, MID=512, HIGH=1024
- Pass tier info through JNI (add `deviceTier` parameter to `nativeCreateContext`)
- Add `-march=armv8-a+fp+simd` or equivalent for NEON in CMakeLists
- Add `-O3` optimization flag for release builds

**Files:** 
- `llama-jni.cpp`: `nativeCreateContext` accepts jint tier, maps to n_batch
- `LlamaEngine.kt`: Update JNI signature
- `QwenModel.kt`: Pass device tier
- `CMakeLists.txt`: Add NEON and optimization flags

**Verification:** Check log: different batch sizes for different tiers.

---

## PHASE 2: UX/UI POLISH (4 tasks)

### Task 2.1: Fix Typography LetterSpacing

**Problem:** BodyLarge has 0.50 letterSpacing (WAY too wide — body text looks like it's screaming). BodyMedium/BodySmall also too wide. LabelMedium/Small also wrong.

**Fix values (in em units, matching Material Design 3 spec):**
- BodyLarge: 0.50 → 0.01 (was effectively 0.50em = ultra-wide)
- BodyMedium: 0.25 → 0.01
- BodySmall: 0.40 → 0.005
- LabelMedium: 0.50 → 0.01
- LabelSmall: 0.50 → 0.005
- TitleMedium: 0.15 → 0.01
- TitleSmall: 0.10 → 0.01

**Files:** `typography.xml`

**Verification:** Visual check — text should look normally spaced.

### Task 2.2: Add Dark Mode Color Theme

**Problem:** `values/themes.xml` only has light theme. Dark mode uses default Material3 dark colors, which don't match the app's design.

**Fix:**
- Create `values-night/themes.xml` with dark theme
- Create `values-night/colors.xml` with dark palette:
  - background: #0D0D0D
  - surface: #1A1A1A
  - surface_variant: #252530
  - on_surface: #F5F5F5
  - on_surface_variant: #B0B0B0
  - primary_container: #2D2766
  - on_primary_container: #D4D0FF
  - Keep primary/secondary the same
  - code_bg_dark: #0D0D0D (already exists in light colors, will be properly used in dark)
- Add code block dark bg to drawable bg_code_block using theme color

**Files:**
- Create: `values-night/themes.xml`
- Create: `values-night/colors.xml`
- Modify: `drawable/bg_code_block.xml` (use `?attr/colorSurfaceVariant`)

**Verification:** Toggle system dark mode, app should have proper dark theme.

### Task 2.3: Polish Empty State + Chat List

**Problem:** Empty state uses raw Material3 styles instead of Qwen styles. Chat list items are plain white cards with no visual flair.

**Fix:**
- empty_state.xml: Use Qwen typography styles (HeadlineMedium → TextAppearance.Qwen.HeadlineMedium, etc.), add proper spacing
- item_chat_list.xml: Add model name next to title, better time formatting, improve avatar with gradient background, add subtle divider or elevation
- fragment_chat_list.xml: Add proper toolbar elevation/subtitle, show active model name

**Files:** `view_empty_state.xml`, `item_chat_list.xml`, `fragment_chat_list.xml`

**Verification:** Visual check — empty state and chat list look polished.

### Task 2.4: Polish Message Bubbles + Input Bar

**Problem:** Message bubbles have good structure but lack visual refinement. Input bar is pill-shaped but could be more refined.

**Fix:**
- item_message.xml: Use Qwen typography styles, add proper elevation/shadow on bubbles, improve timestamp placement, add user avatar (small circle with initial)
- fragment_chat_detail.xml: Improve toolbar with model name, add subtle top shadow/elevation

**Files:** `item_message.xml`, `fragment_chat_detail.xml`

**Verification:** Visual check — chat screen looks polished and professional.

---

## PHASE 3: USER FLOW FEATURES (6 tasks)

### Task 3.1: Add Settings/Preferences Screen

**Problem:** No way to change model parameters after initial selection. No settings at all.

**Fix:**
- Create `SettingsActivity.kt` — simple settings screen with:
  - Model selector (shows current model, tap to change)
  - Temperature slider (0.0-2.0, default 0.7)
  - Max tokens slider (64-1024, default 512)
  - Clear all conversations button
- Add settings gear icon to chat list toolbar
- Store settings in `ModelPreferences`

**Files:**
- Create: `SettingsActivity.kt`
- Create: `activity_settings.xml`
- Modify: `ModelPreferences.kt` (add temperature, maxTokens prefs)
- Modify: `fragment_chat_list.xml` (add settings button to toolbar)
- Modify: `ChatListFragment.kt` (wire settings button)

**Verification:** Can access settings from chat list, change params, settings persist.

### Task 3.2: Add Token Generation Speed Indicator

**Problem:** No feedback on how fast the model is running. Users need to know if it's slow because of their device or the model.

**Fix:**
- Track token generation time in `ChatDetailViewModel`
- Calculate tok/s: `generatedTokens / elapsedSeconds`
- Show in a small `TextView` next to the typing indicator or in a status bar
- Format: "12.5 tok/s" with appropriate color (green >10, yellow 5-10, red <5)

**Files:**
- Modify: `ChatDetailViewModel.kt` (track timing, expose speed StateFlow)
- Modify: `fragment_chat_detail.xml` (add tok/s text view)
- Modify: `ChatDetailFragment.kt` (observe and display speed)

**Verification:** During generation, tok/s indicator shows real-time speed.

### Task 3.3: Add Context Usage Indicator

**Problem:** Users don't know how full the context window is. Can lead to silent truncation.

**Fix:**
- Show context usage as a subtle progress bar: tokensUsed / totalContext
- Display as "128/256 tokens" or "50%"
- Show in the input bar area or toolbar subtitle

**Files:**
- Modify: `QwenModel.kt` (expose cache position getter)
- Modify: `ChatDetailViewModel.kt` (expose context usage)
- Modify: `fragment_chat_detail.xml` (add subtle progress bar)

**Verification:** Context indicator updates after each message generation.

### Task 3.4: Add Message Share/Copy Actions

**Problem:** Can't copy or share individual messages.

**Fix:**
- Add long-press listener to message bubbles in `ChatAdapter`
- Show popup menu with "Copy" and "Share" options
- "Copy" copies message content to clipboard
- "Share" opens Android share sheet with message text

**Files:**
- Modify: `ChatAdapter.kt` (add long-press + popup menu)
- Add: `res/menu/message_actions.xml` (popup menu)

**Verification:** Long-press on a message shows copy/share menu.

### Task 3.5: Add Conversation Export

**Problem:** Can't export conversations.

**Fix:**
- Add "Export Chat" option in chat detail toolbar overflow menu
- Export as plain text with timestamps, or share as text
- Format:
```
Qwen3 Chat — [title] — [date]

User: Hello
Assistant: Hi! How can I help?
...
```
- Use Android share sheet to send

**Files:**
- Modify: `fragment_chat_detail.xml` (add toolbar overflow menu)
- Modify: `ChatDetailFragment.kt` (wire export action)
- Modify: `ChatDetailViewModel.kt` (add export function)

**Verification:** Overflow menu → Export → share sheet appears with formatted chat.

### Task 3.6: Add Basic Markdown Rendering

**Problem:** Assistant messages render as plain text. No bold, italic, code, or lists.

**Fix:**
- Use Android's built-in `android.text.Html.fromHtml()` for basic formatting
- Convert markdown to HTML before setting text:
  - `**bold**` → `<b>bold</b>`
  - `*italic*` → `<i>italic</i>`
  - `` `code` `` → `<tt>code</tt>` with code styling
  - `` ```lang\ncode\n``` `` → styled code blocks
  - `- list items` → bullet points
  - `1. ordered` → numbers
- Simple regex-based converter (no external library needed)
- Create utility class `MarkdownRenderer.kt`

**Files:**
- Create: `MarkdownRenderer.kt` (markdown-to-html converter)
- Modify: `ChatAdapter.kt` (use MarkdownRenderer + Html.fromHtml for assistant messages)

**Verification:** Assistant messages with markdown syntax render as formatted text.
