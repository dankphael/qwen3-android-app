# Phase 4: iOS Native Setup with llama.cpp Metal Acceleration

**⚠️ This phase MUST be run on macOS with Xcode installed. It cannot run in the Linux sandbox.**

## Overview

Phase 4 builds `llama.xcframework` (llama.cpp compiled for iOS with Metal GPU acceleration), enables Kotlin/Native cinterop bindings, and fills in the scaffold in `IosInferenceEngine.kt`.

## Prerequisites on macOS

```bash
# Install Xcode (from App Store)
xcode-select --install

# Verify installations
xcodebuild -version
cmake --version
```

## Step 1: Initialize llama.cpp Submodule

```bash
cd qwen3-app
git submodule update --init --recursive
```

This fetches the llama.cpp source tree (already pinned in `.gitmodules`).

## Step 2: Build llama.xcframework

Run the build script on your Mac:

```bash
# From repo root
./scripts/build_ios_llama.sh

# Optionally, specify custom paths:
./scripts/build_ios_llama.sh /path/to/llama.cpp /path/to/build-output
```

**What it does:**
- Builds llama.cpp for iOS device (arm64) and simulator (arm64) with `-DGGML_METAL=ON -DGGML_METAL_EMBED_LIBRARY=ON`
- Combines static libraries from both slices into `libllama_all.a` per slice
- Creates `llama.xcframework` with device + simulator headers + libs

**Expected output:**
```
llama.xcframework/
├── ios-arm64/
│   ├── Headers/
│   │   ├── llama.h
│   │   └── ggml.h
│   └── libllama_all.a
├── ios-sim-arm64/
│   ├── Headers/
│   └── libllama_all.a
├── Info.plist
└── [xcframework metadata]
```

**Build time:** ~15-30 minutes depending on Mac hardware.

## Step 3: Enable Kotlin/Native Cinterop

In `shared/core/build.gradle.kts`, uncomment lines 27-29:

```kotlin
iosTarget.compilations.getByName("main").cinterops.create("llama") {
    definitionFile.set(project.file("src/nativeInterop/cinterop/llama.def"))
}
```

The `.def` file at `shared/core/src/nativeInterop/cinterop/llama.def` declares:
- C headers: `llama.h`, `ggml.h`
- Static libraries: llama + ggml pieces from xcframework
- Compiler/linker flags: Metal, Accelerate, Foundation frameworks

## Step 4: Fill in IosInferenceEngine Cinterop Calls

Open `shared/core/src/iosMain/kotlin/com/example/qwen3/core/InferenceEngine.ios.kt`.

The scaffold has TODO markers for each function. Replace them with actual cinterop calls. Below is the implementation strategy (detailed code examples follow).

### 4a. Global State

At the top of the class, add:

```kotlin
private var modelPtr: CPointer<llama_model>? = null
private var ctxPtr: CPointer<llama_context>? = null
private var samplerPtr: CPointer<llama_sampler>? = null
```

Import from the generated cinterop:

```kotlin
import llama.cpp.*
import kotlinx.cinterop.*
```

### 4b. Implementation Functions

Replace each TODO with the corresponding implementation:

**init():**
```kotlin
override fun init() {
    llama_backend_init()
}
```

**loadModel(path, nGpuLayers):**
```kotlin
override fun loadModel(path: String, nGpuLayers: Int): Boolean {
    val params = llama_model_default_params()
    params.n_gpu_layers = nGpuLayers.toUInt()
    
    modelPtr = path.withCString { cpath ->
        llama_model_load_from_file(cpath, params)
    }
    
    loaded = modelPtr != null
    return loaded
}
```

**createContext(nCtx, deviceTier):**
```kotlin
override fun createContext(nCtx: Int, deviceTier: Int): Boolean {
    if (modelPtr == null) return false
    
    val params = llama_context_default_params()
    params.n_ctx = nCtx.toUInt()
    
    // Tier-based batch size (mirror AndroidInferenceEngine)
    params.n_batch = when (deviceTier) {
        0 -> 256u   // LOW
        1 -> 512u   // MID
        2 -> 1024u  // HIGH
        else -> 512u
    }
    
    params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_ENABLED
    
    ctxPtr = llama_init_from_model(modelPtr!!, params)
    return ctxPtr != null
}
```

**evaluatePrompt(prompt, startPos):**
```kotlin
override fun evaluatePrompt(prompt: String, startPos: Int): Int {
    if (ctxPtr == null) return -1
    
    val vocab = llama_model_get_vocab(llama_get_model(ctxPtr!!))
    
    // Tokenize
    val n_tokens = prompt.withCString { cstr ->
        -llama_tokenize(vocab, cstr, 0, null, 0, true, true)
    }
    
    if (n_tokens <= 0) return -1
    
    val tokens = IntArray(n_tokens)
    prompt.withCString { cstr ->
        llama_tokenize(vocab, cstr, 0, tokens.toCValues().ptr, n_tokens, true, true)
    }
    
    // Evaluate in batches
    val n_batch = llama_n_batch(ctxPtr!!)
    for (i in 0 until n_tokens step n_batch) {
        val n_eval = minOf(n_batch, n_tokens - i)
        val batch = llama_batch_get_one(
            tokens.slice(i until i + n_eval).toIntArray().toCValues().ptr,
            n_eval
        )
        
        if (startPos > 0) {
            // Adjust positions for incremental eval
            for (j in 0 until n_eval) {
                batch.pos[j] = (startPos + i + j).toInt32()
            }
        }
        
        if (llama_decode(ctxPtr!!, batch) != 0) return -1
    }
    
    return n_tokens
}
```

**cachePosition():**
```kotlin
override fun cachePosition(): Int =
    llama_get_kv_cache_seq_end(ctxPtr!!, 0).toInt()
```

**resetCache():**
```kotlin
override fun resetCache() {
    if (ctxPtr != null) {
        llama_memory_clear(ctxPtr!!)
    }
}
```

**startGeneration(temperature):**
```kotlin
override fun startGeneration(temperature: Float) {
    cancelled.value = false
    
    // Free previous sampler
    if (samplerPtr != null) {
        llama_sampler_free(samplerPtr!!)
    }
    
    // Build sampler chain (mirror llama-jni.cpp)
    val sparams = llama_sampler_chain_default_params()
    samplerPtr = llama_sampler_chain_init(sparams)
    
    llama_sampler_chain_add(samplerPtr!!, llama_sampler_init_top_k(40))
    llama_sampler_chain_add(samplerPtr!!, llama_sampler_init_top_p(0.95f, 1))
    llama_sampler_chain_add(samplerPtr!!, llama_sampler_init_temp(temperature))
    llama_sampler_chain_add(samplerPtr!!, llama_sampler_init_dist(LLAMA_DEFAULT_SEED.toUInt()))
}
```

**nextToken():**
```kotlin
override fun nextToken(): String {
    if (cancelled.value || ctxPtr == null || samplerPtr == null) return ""
    
    val vocab = llama_model_get_vocab(llama_get_model(ctxPtr!!))
    val new_token = llama_sampler_sample(samplerPtr!!, ctxPtr!!, -1)
    
    // End of sequence
    if (llama_vocab_is_eog(vocab, new_token)) return ""
    
    // Token to piece
    val buf = ByteArray(256)
    val n = buf.usePinned { pinned ->
        llama_token_to_piece(vocab, new_token, pinned.addressOf(0), buf.size, 0, false)
    }
    
    if (n <= 0) return ""
    
    // Decode into KV cache
    val batch = llama_batch_get_one(IntArray(1) { new_token.toInt() }.toCValues().ptr, 1)
    if (llama_decode(ctxPtr!!, batch) != 0) return ""
    
    return buf.slice(0 until n).toByteArray().decodeToString()
}
```

**endGeneration():**
```kotlin
override fun endGeneration() {
    if (samplerPtr != null) {
        llama_sampler_free(samplerPtr!!)
        samplerPtr = null
    }
}
```

**freeContext():**
```kotlin
override fun freeContext() {
    if (ctxPtr != null) {
        llama_free(ctxPtr!!)
        ctxPtr = null
    }
}
```

**freeModel():**
```kotlin
override fun freeModel() {
    if (modelPtr != null) {
        llama_model_free(modelPtr!!)
        modelPtr = null
        loaded = false
    }
}
```

**cleanup():**
```kotlin
override fun cleanup() {
    endGeneration()
    freeContext()
    freeModel()
    llama_backend_free()
}
```

## Step 5: Test on iOS Device or Simulator

### Using Xcode

1. Open iosApp in Xcode:
   ```bash
   cd iosApp
   open iosApp.xcodeproj
   ```

2. Ensure the shared framework is linked:
   - In Xcode: **Build Phases** → **Link Binary With Libraries** → Add `sharedUi.framework`
   - Or, add a **Run Script Build Phase** to run `:shared:ui:embedAndSignAppleFrameworkForXcode`

3. Embed the llama.xcframework:
   - **Build Phases** → **Embed Frameworks** → Add `llama.xcframework`
   - Ensure it's marked **Do Not Embed** (since iosApp links it statically)

4. Build & Run:
   - Select an iOS device or simulator target
   - **Product** → **Build** (⌘B)
   - **Product** → **Run** (⌘R)

### Using Command Line

```bash
xcodebuild build -scheme iosApp -configuration Release -arch arm64
```

## Step 6: Verify GPU Backend

In the **Diagnostics** screen of the app, confirm:
- GPU Backend shows **"metal"**
- Device info displays correctly
- Model loading succeeds without crashes

## Troubleshooting

### "llama.xcframework not found"
- Run `./scripts/build_ios_llama.sh` first
- Ensure the framework is at the repo root: `ls llama.xcframework`

### Cinterop compilation errors ("undefined reference to llama_*")
- Verify `shared/core/build.gradle.kts` cinterop block is uncommented
- Check `llama.def` library paths point to the correct xcframework slice

### Metal shader compilation fails at runtime
- Ensure `-DGGML_METAL_EMBED_LIBRARY=ON` was used in the build script
- Check for any llama.cpp version conflicts (pin commit in `.gitmodules`)

### App crashes on generation
- Add `NSLocalizedDescription` to iOS error handling
- Check `llama.def` linker flags include `-framework Metal -framework Accelerate`
- Test with small 0.8B model first

## Next: Commit & Push

Once cinterop is working:

```bash
git add shared/core/build.gradle.kts shared/core/src/iosMain/...
git commit -m "Phase 4: Implement iOS cinterop to llama.cpp with Metal

- Enable Kotlin/Native cinterop in shared/core/build.gradle.kts
- Fill IosInferenceEngine with llama.cpp C calls (load, create context, tokenize, sample, etc.)
- Implement streaming token generation with cancellation support
- Metal GPU acceleration via llama.xcframework (built on Mac)"

git push origin claude/eager-ramanujan-5omwty
```

## Summary

| Task | Status |
|------|--------|
| Build llama.xcframework with Metal | ✓ Automated script (macOS only) |
| Enable cinterop in build.gradle.kts | → Uncomment 3 lines |
| Implement IosInferenceEngine methods | → Replace TODO markers |
| Test on device/simulator | → Verify "metal" in Diagnostics |
| Commit to branch | → Ready to push |

---

**Estimated time on macOS:** 30-45 min (15-30 min build + 15 min impl + test)
