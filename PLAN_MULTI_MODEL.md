# Multi-Model Selection Architecture Plan
## qwen3-android-app
### By Pat (AI Technical Team Lead) — 2026-06-08

---

## 1. NEW & MODIFIED FILES

| File | Action | Responsibility |
|------|--------|----------------|
| `ModelMetadata.kt` | **NEW** | Data class + companion object holding all model metadata (URLs, filenames, sizes, RAM thresholds, n_ctx per model, tier assignments) |
| `DeviceCapability.kt` | **NEW** | Auto-detect RAM + CPU cores via ActivityManager + /proc/cpuinfo; classify into LOW/MID/HIGH tier |
| `ModelSelectionActivity.kt` | **NEW** | First-launch model selection screen (new Activity, declared in manifest as LAUNCHER-alternative) |
| `activity_model_selection.xml` | **NEW** | Layout for model selection: radio-button list per model, RAM indicator, "Download" button |
| `ModelPreferences.kt` | **NEW** | SharedPreferences wrapper: read/write selected model key, first-launch flag |
| `ModelDownloadService.kt` | **MODIFIED** | Accept model metadata as Intent extra (URL + filename); remove hardcoded constants; add SHA256 integrity check; cancel OkHttp on stop |
| `QwenModel.kt` | **MODIFIED** | Accept model metadata as constructor param instead of hardcoded constants; accept n_ctx from metadata; add integrity verification before load |
| `MainActivity.kt` | **MODIFIED** | Check ModelPreferences for selected model; route to ModelSelectionActivity on first launch; pass selected model to QwenModel and download service; add ViewModel to survive rotation; implement onTrimMemory() |
| `LlamaEngine.kt` | **NO CHANGE** | JNI bridge unchanged |
| `llama-jni.cpp` | **NO CHANGE** | Native layer unchanged |
| `AndroidManifest.xml` | **MODIFIED** | Add POST_NOTIFICATIONS permission; declare ModelSelectionActivity |
| `ChatViewModel.kt` | **NEW** | ViewModel that survives configuration changes: holds QwenModel reference, download state, message list, generation coroutine |
| `build.gradle.kts` (app) | **MODIFIED** | Add ViewModel + lifecycle-viewmodel-ktx dependency |

---

## 2. DEVICE CAPABILITY DETECTION LOGIC

### Inputs
- **Total RAM (MiB):** `ActivityManager.MemoryInfo.totalMem / (1024*1024)`
- **CPU cores:** `Runtime.getRuntime().availableProcessors()`
  - Fallback if returns 1 (some devices): `cat /sys/devices/system/cpu/present` or count `/sys/devices/system/cpu/cpu[0-9]*`

### Tier Classification

| Tier | RAM Threshold | CPU Threshold | Available Models |
|------|--------------|---------------|------------------|
| **LOW** | < 4,500 MiB | OR < 4 cores | 0.8B only |
| **MID** | 4,500–6,500 MiB | AND ≥ 4 cores | 0.8B, 2B |
| **HIGH** | > 6,500 MiB | AND ≥ 8 cores | 0.8B, 2B, 4B |

### Rationale

- 0.8B model: needs ~600MB loaded, ~500MB download. Works on 3GB devices.
- 2B model: needs ~1.5GB loaded, ~1.3GB download. Needs 4GB+ device with headroom for Android system.
- 4B model: needs ~2.8GB loaded, ~2.5GB download. Needs 6GB+ device (Android system takes 1–2GB). 8+ cores for decent speed.

### Implementation (DeviceCapability.kt)

```kotlin
class DeviceCapability(private val context: Context) {
    val ramMiB: Long
    val cpuCores: Int
    val tier: Tier

    enum class Tier { LOW, MID, HIGH }

    val availableModels: List<ModelMetadata>  // filtered by tier
}
```

---

## 3. MODEL SELECTION SCREEN FLOW

### App Launch Decision Tree

```
App starts (MainActivity.onCreate)
   │
   ├─ ModelPreferences.getSelectedModelKey() != null
   │      → Model already selected. Skip selection.
   │      → Check if model file exists for selected model.
   │          ├─ File exists → Load directly (or show "Loading…" overlay)
   │          └─ File missing → start ModelDownloadService with that model
   │
   └─ ModelPreferences.getSelectedModelKey() == null
          → FIRST LAUNCH. Start ModelSelectionActivity.
```

### ModelSelectionActivity UI

```
┌──────────────────────────────────────┐
│  Choose Your Model                    │
│                                       │
│  Your device: 6GB RAM, 8 cores        │
│  Tier: HIGH — All models available    │
│                                       │
│  ┌─────────────────────────────────┐  │
│  │ ○ Qwen3.5-0.8B                  │  │
│  │   ~500MB download | Fastest     │  │
│  │   Context: 1024 tokens          │  │
│  ├─────────────────────────────────┤  │
│  │ ○ Qwen3.5-2B                    │  │
│  │   ~1.3GB download | Balanced    │  │
│  │   Context: 512 tokens           │  │
│  ├─────────────────────────────────┤  │
│  │ ● Qwen3.5-4B  (recommended)     │  │
│  │   ~2.5GB download | Smartest    │  │
│  │   Context: 512 tokens           │  │
│  └─────────────────────────────────┘  │
│                                       │
│  [ Download & Start ]                 │
│                                       │
│  Download over Wi-Fi recommended.     │
│  You can change model later in Settings.
└──────────────────────────────────────┘
```

For LOW tier: only 0.8B shown (no choice needed, but still show the screen with description so user knows what's happening).

For MID tier: 0.8B and 2B shown.

For HIGH tier: all three shown, with "recommended" badge on 4B (if RAM > 8GB) or 2B (if 6–8GB).

### Flow After Selection

1. User taps a model radio button
2. User taps "Download & Start"
3. `ModelPreferences.saveSelectedModelKey(key)` → persists choice
4. `setResult(RESULT_OK)` + `finish()` → returns to MainActivity
5. MainActivity.onActivityResult → starts ModelDownloadService with selected model
6. ModelDownloadService downloads the correct file
7. On complete → MainActivity loads model → chat UI appears

---

## 4. DOWNLOAD FLOW CHANGES

### Current (Hardcoded)
```
ModelDownloadService.ACTION_START → download MODEL_FILE_NAME from MODEL_DOWNLOAD_URL
```

### New (Parameterized)
```
ModelDownloadService.ACTION_START
   + EXTRA_MODEL_KEY = "qwen3.5-4b"       // or "qwen3.5-2b", "qwen3.5-0.8b"
   + EXTRA_MODEL_URL = "https://..."       // full download URL
   + EXTRA_MODEL_FILENAME = "Qwen3.5-4B-Q4_K_M.gguf"
   + EXTRA_EXPECTED_SHA256 = "abc123..."   // for integrity check
```

Intent extras passed by MainActivity when launching the download. The service reads them, downloads to `filesDir/<filename>`, and verifies SHA256 after download.

### Multiple Download Support

If a user previously downloaded 2B and now wants 4B:
1. The new model downloads alongside the old one (different filenames)
2. After download, old model file CAN remain (storage is cheap). Or we can delete it.
3. Decision: **Keep old file** — user might switch back without re-downloading.
4. Add "Manage Models" in settings later showing downloaded models + sizes.

---

## 5. BUG FIXES (All 7 Addressed)

### Bug 1: POST_NOTIFICATIONS permission missing (CRITICAL)

**Impact:** Crash on Android 13+ when showing download notification.

**Fix:**
- Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` to AndroidManifest.xml
- In ModelSelectionActivity (or first MainActivity launch), request permission at runtime:
```kotlin
if (Build.VERSION.SDK_INT >= 33) {
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
}
```
- Only call this on Android 13+. If denied, download still works but without foreground notification (the service falls back gracefully).

---

### Bug 2: Use-after-free on rotation during generation (CRITICAL)

**Root cause:** `qwenModel` is a field on MainActivity. On rotation, MainActivity is destroyed and recreated. If `qwenModel.close()` is called in `onDestroy()`, but a generation coroutine from the *old* activity is still running on `Dispatchers.IO` referencing the now-freed native context → use-after-free crash in JNI.

**Fix (ChatViewModel.kt):**
- Move `QwenModel` instance ownership to a **ViewModel** scoped to the Activity lifecycle, NOT the Activity instance.
- The ViewModel survives rotation. `QwenModel.close()` is only called in `ViewModel.onCleared()` (when Activity is finishing for real, not rotating).
- Generation coroutine launched in `viewModelScope` — auto-cancelled when ViewModel is cleared.
```kotlin
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private var qwenModel: QwenModel? = null
    val messages = mutableStateListOf<ChatMessage>()
    val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    fun loadModel(metadata: ModelMetadata) { ... }
    fun generate(userMessage: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                qwenModel?.generate(messages.toList())
            }
        }
    }

    override fun onCleared() {
        qwenModel?.close()
        super.onCleared()
    }
}
```

---

### Bug 3: No onTrimMemory() (CRITICAL)

**Impact:** Android sends `onTrimMemory(TRIM_MEMORY_RUNNING_CRITICAL)` when memory is critically low. If we have 2.8GB loaded and don't respond, the system kills the process. Worse: native memory (loaded via `llama_model_load_from_file`) is invisible to the Dalvik GC — the system doesn't know we're holding it.

**Fix:**
```kotlin
// In MainActivity
override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    when (level) {
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
        ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
            Log.w(TAG, "onTrimMemory critical — releasing model")
            viewModel.unloadModel()  // frees native memory, keeps download state
        }
        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
            // App in background — keep model loaded but cancel any active generation
            viewModel.cancelGeneration()
        }
    }
}
```

Also register with `application.registerComponentCallbacks(...)` if we want to catch this even when MainActivity isn't the visible component.

---

### Bug 4: Model load coroutine cancelled on rotation leaks 2.5GB native memory (HIGH)

**Root cause:** `lifecycleScope.launch { withContext(Dispatchers.IO) { model.loadModel() } }` — on rotation, `lifecycleScope` is cancelled. The coroutine is cancelled on the IO dispatcher, but `llama_model_load_from_file()` is a **blocking native call** that doesn't check for cancellation. The native load completes, returns a pointer, but the coroutine never runs the success callback — the pointer is leaked forever.

**Fix:**
- Move loading to `ChatViewModel.viewModelScope` which is NOT cancelled on rotation.
- Add a cancel-check wrapper: before calling `loadModel()` natively, check `isActive`. After load completes, check `isActive` again before proceeding. If cancelled mid-load, immediately call `llama_model_free()` on the obtained pointer.
- Alternative (better): show a non-dismissible dialog during loading with `setCancelable(false)` so the user can't rotate away while the massive model is loading. Or catch rotation and restore the loading state.

---

### Bug 5: No model file integrity check (HIGH)

**Fix in ModelDownloadService.kt:**

After download completes and before renaming `.part` to final file:

```kotlin
// After partialFile.renameTo(modelFile)
val actualSha256 = computeSha256(modelFile)
if (actualSha256 != expectedSha256) {
    modelFile.delete()
    throw RuntimeException("SHA256 mismatch: expected $expectedSha256, got $actualSha256")
}

private fun computeSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { fis ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
```

The expected SHA256 is part of `ModelMetadata`. Initial SHA256 values can be `null` (skip check) until we fetch them from HuggingFace. The file integrity is also checked on every load (in QwenModel.loadModel) via a quick SHA256 verify against a stored hash, preventing crashes from corrupted files.

---

### Bug 6: No token-limit warning when chat exceeds n_ctx (HIGH)

**Impact:** If conversation grows beyond n_ctx (512 or 1024), the oldest tokens get truncated silently by the KV cache, producing garbled output or crashes.

**Fix in QwenModel.kt:**
- After evaluating the prompt, check `n_tokens_evaluated >= n_ctx`:
```kotlin
val tokensEvaluated = LlamaEngine.nativeEvaluatePrompt(ctxPtr, prompt)
if (tokensEvaluated >= nCtx * 0.9) {
    Log.w(TAG, "Context near limit: $tokensEvaluated / $nCtx")
}
if (tokensEvaluated >= nCtx) {
    Log.e(TAG, "Context overflow: $tokensEvaluated > $nCtx")
    return "[Warning: conversation too long. Please start a new chat. Maximum context: $nCtx tokens.]"
}
```
- Also add a small UI indicator in the chat showing "Context: 412/512 tokens" (stretch goal).
- For the 0.8B model with n_ctx=1024, this is less critical — but still worth adding the safety check.

---

### Bug 7: OkHttp call not cancelled on service stop (HIGH)

**Root cause:** `ModelDownloadService.onDestroy()` calls `serviceScope.cancel()` which cancels the coroutine, but the OkHttp `Call` object is not explicitly cancelled. The TCP connection remains open, downloading data into a void until the socket times out.

**Fix:**
```kotlin
private var activeCall: okhttp3.Call? = null

private suspend fun downloadModelInternal() {
    // ...
    val call = client.newCall(requestBuilder.build())
    activeCall = call
    val response = call.execute()
    activeCall = null
    // ...
}

override fun onDestroy() {
    activeCall?.cancel()  // Cancel OkHttp call BEFORE cancelling scope
    isDownloading = false
    serviceScope.cancel()
    releaseWakeLock()
    super.onDestroy()
}
```

---

## 6. MODEL METADATA STRUCTURE

```kotlin
// ModelMetadata.kt
data class ModelMetadata(
    val key: String,                // "qwen3.5-0.8b", "qwen3.5-2b", "qwen3.5-4b"
    val displayName: String,        // "Qwen3.5-0.8B"
    val filename: String,           // "Qwen3.5-0.8B-Q4_K_M.gguf"
    val downloadUrl: String,        // Full HF resolve URL
    val fileSizeBytes: Long,        // Approximate download size
    val nCtx: Int,                  // Context window size (1024 for 0.8B, 512 for 2B/4B)
    val minRamMiB: Long,            // Minimum RAM to run this model
    val recommendedRamMiB: Long,    // Recommended RAM for comfortable use
    val minCpuCores: Int,           // Minimum CPU cores
    val description: String,        // Short description for selection screen
    val sha256: String?,            // Expected SHA256 (null = skip check until known)
    val maxTokens: Int,             // Max generation tokens (or use same for all)
    val temperature: Float,         // 0.7f for all
) {
    companion object {
        val ALL = listOf(
            ModelMetadata(
                key = "qwen3.5-0.8b",
                displayName = "Qwen3.5-0.8B",
                filename = "Qwen3.5-0.8B-Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF/resolve/main/Qwen3.5-0.8B-Q4_K_M.gguf",
                fileSizeBytes = 524_000_000,  // ~500MB
                nCtx = 1024,
                minRamMiB = 2500,
                recommendedRamMiB = 3500,
                minCpuCores = 4,
                description = "Smallest and fastest. Good for basic Q&A.",
                sha256 = null,  // TODO: fetch from HF
                maxTokens = 512,
                temperature = 0.7f,
            ),
            ModelMetadata(
                key = "qwen3.5-2b",
                displayName = "Qwen3.5-2B",
                filename = "Qwen3.5-2B-Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3.5-2B-GGUF/resolve/main/Qwen3.5-2B-Q4_K_M.gguf",
                fileSizeBytes = 1_342_000_000,  // ~1.28GB
                nCtx = 512,
                minRamMiB = 4000,
                recommendedRamMiB = 5000,
                minCpuCores = 4,
                description = "Balanced speed and intelligence. Good for everyday use.",
                sha256 = null,
                maxTokens = 512,
                temperature = 0.7f,
            ),
            ModelMetadata(
                key = "qwen3.5-4b",
                displayName = "Qwen3.5-4B",
                filename = "Qwen3.5-4B-Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/unsloth/Qwen3.5-4B-GGUF/resolve/main/Qwen3.5-4B-Q4_K_M.gguf",
                fileSizeBytes = 2_621_000_000,  // ~2.5GB
                nCtx = 512,
                minRamMiB = 5500,
                recommendedRamMiB = 7000,
                minCpuCores = 6,
                description = "Smartest model. Best reasoning quality.",
                sha256 = null,
                maxTokens = 512,
                temperature = 0.7f,
            ),
        )

        fun findByKey(key: String): ModelMetadata =
            ALL.find { it.key == key }
                ?: throw IllegalArgumentException("Unknown model: $key")
    }
}
```

---

## 7. DATA PERSISTENCE (ModelPreferences.kt)

```kotlin
// ModelPreferences.kt
class ModelPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("model_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_MODEL = "selected_model_key"
        private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"

        // Migration: apps upgrading from v1 had the hardcoded 4B model.
        // If a model file exists but no key is stored, auto-set to 4B.
        const val LEGACY_MODEL_KEY = "qwen3.5-4b"
        const val LEGACY_FILENAME = "qwen3.5-4b-q4_k_m.gguf"
    }

    fun getSelectedModelKey(): String? = prefs.getString(KEY_SELECTED_MODEL, null)
    fun saveSelectedModelKey(key: String) = prefs.edit().putString(KEY_SELECTED_MODEL, key).apply()

    fun isFirstLaunchDone(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    fun markFirstLaunchDone() = prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()

    /** Migrate legacy users who already have the 4B model downloaded */
    fun migrateFromV1(context: Context): Boolean {
        if (getSelectedModelKey() != null) return false // Already migrated or new install
        val legacyFile = File(context.filesDir, LEGACY_FILENAME)
        if (legacyFile.exists() && legacyFile.length() > 0) {
            saveSelectedModelKey(LEGACY_MODEL_KEY)
            markFirstLaunchDone()
            return true
        }
        return false
    }
}
```

Storage details:
- `SharedPreferences` file: `model_prefs.xml` (default location, survives updates)
- Keys stored: `selected_model_key` (String), `first_launch_done` (Boolean)
- Future: can add per-model SHA256 cache, download timestamps, model switch log

---

## 8. n_ctx PER MODEL

| Model | n_ctx | Rationale |
|-------|-------|-----------|
| 0.8B | **1024** | Tiny model, fast at long context. 1024 tokens gives ~800 words of conversation history. KV cache is small (~16MB at Q4_K_M for 1024 tokens). |
| 2B | **512** | Mid-size model. 512 tokens = ~400 words. KV cache ~40MB. Good enough for chat. |
| 4B | **512** | Largest model. KV cache for 1024 tokens would be ~80MB — pushing memory limits on 6GB devices. 512 is the safe default. |

The n_ctx is passed from `ModelMetadata.nCtx` → `QwenModel` constructor → `LlamaEngine.nativeCreateContext(modelPtr, nCtx)`. No hardcoding.

The `maxTokens` for generation stays at 512 for all models (the response length, not the context window).

---

## 9. HUGGINGFACE URLs

| Model | Repo | Q4_K_M GGUF URL |
|-------|------|-----------------|
| 0.8B | `unsloth/Qwen3.5-0.8B-GGUF` | `https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF/resolve/main/Qwen3.5-0.8B-Q4_K_M.gguf` |
| 2B | `unsloth/Qwen3.5-2B-GGUF` | `https://huggingface.co/unsloth/Qwen3.5-2B-GGUF/resolve/main/Qwen3.5-2B-Q4_K_M.gguf` |
| 4B | `unsloth/Qwen3.5-4B-GGUF` | `https://huggingface.co/unsloth/Qwen3.5-4B-GGUF/resolve/main/Qwen3.5-4B-Q4_K_M.gguf` |

Verified on HuggingFace. All three files exist and are public. The `resolve/main` path triggers a 302 redirect to the actual LFS-hosted file — OkHttp follows redirects by default.

---

## 10. IMPLEMENTATION ORDER (Recommended)

### Phase 1: Foundation (no UI changes yet)
1. Create `ModelMetadata.kt` with all three models
2. Create `DeviceCapability.kt` with tier detection
3. Create `ModelPreferences.kt` with migration logic
4. Modify `ModelDownloadService.kt` to accept model params via Intent extras (Bug 7 fix: cancel OkHttp call)
5. Modify `QwenModel.kt` to accept `ModelMetadata` parameter, n_ctx from metadata (Bug 5 fix: integrity check, Bug 6 fix: context overflow warning)

### Phase 2: ViewModel + Bug fixes (still backward-compatible)
6. Create `ChatViewModel.kt` (Bug 2 fix: ownership survives rotation, Bug 4 fix: load survives rotation)
7. Modify `MainActivity.kt` to use ViewModel, add `onTrimMemory()` (Bug 3 fix)
8. Add `POST_NOTIFICATIONS` permission + runtime request (Bug 1 fix)

### Phase 3: Selection UI
9. Create `activity_model_selection.xml` layout
10. Create `ModelSelectionActivity.kt`
11. Modify `MainActivity.kt` to route to selection on first launch
12. Update `AndroidManifest.xml` with new activity + permission

### Phase 4: Polish
13. Add context-usage indicator in chat UI (stretch)
14. Add "Switch Model" in settings/overflow menu
15. Fetch SHA256 values from HuggingFace and populate metadata
16. Test on LOW tier device (2GB/4GB emulator), MID tier, HIGH tier

---

## APPENDIX: Migration Path for Existing Users

Users with the v1 app (hardcoded 4B) get a seamless upgrade:
1. `ModelPreferences.migrateFromV1()` detects the existing `qwen3.5-4b-q4_k_m.gguf` file
2. Auto-sets `selected_model_key = "qwen3.5-4b"`
3. Auto-sets `first_launch_done = true`
4. MainActivity skips ModelSelectionActivity → loads 4B as before
5. User never sees the selection screen unless they factory reset or clear app data

Users who clear app data (or new installs) go through the full selection flow.
