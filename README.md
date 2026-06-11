# Qwen3 Chat — Kotlin Multiplatform

On-device LLM inference for **Android and iOS** from one shared Kotlin codebase,
powered by [llama.cpp](https://github.com/ggml-org/llama.cpp). UI is **Compose
Multiplatform** (shared). Inference uses **JNI on Android** and **llama.cpp via
Kotlin/Native cinterop with Metal on iOS**.

This is the multiplatform sibling of the Android-only `qwen3-android-app`.

## Module layout

```
qwen3-kmp/
├── shared/core/   :shared:core  — KMP library (no Compose):
│                    models, device tiers, prompt/QwenModel orchestration,
│                    InferenceEngine (interface + Android/iOS impls),
│                    SQLDelight DB + ChatRepository, multiplatform-settings prefs.
├── shared/ui/     :shared:ui    — Compose Multiplatform UI (App()), iOS MainViewController.
├── androidApp/    Android application (Compose host + JNI llama-jni.cpp + CMake).
├── iosApp/        SwiftUI shell hosting the shared Compose UI (create in Xcode on a Mac).
├── scripts/       build_ios_llama.sh — builds llama.xcframework with Metal.
└── llama.cpp/     git submodule (engine sources).
```

## What works in this scaffold (Phases 0–2 + Android inference seam)

- **Shared pure core** (`:shared:core/commonMain`): `ModelMetadata`, `ChatMessage`,
  `ReasoningParser`, `DeviceCapability` tier logic, `QwenModel` (ChatML prompt
  building, incremental cache-aware streaming via pull-based `nextToken()`),
  `ChatRepository` (SQLDelight) + auto-title, `ModelPreferences`
  (multiplatform-settings). Covered by `commonTest` (`CoreLogicTest`).
- **Platform seams as interfaces** (testable, DI-friendly): `InferenceEngine`,
  `PathProvider`, `DeviceProbe`, `DatabaseDriverFactory` — each with Android and iOS
  implementations.
- **Android inference** delegates to the proven `llama-jni.cpp` (copied from the
  Android repo) through `LlamaEngine`.
- **iOS inference** scaffolded (`IosInferenceEngine`) with documented cinterop TODOs.
- **Shared Compose `App()`** placeholder rendering on both platforms.

## Known gaps / TODO (by plan phase)

- **Phase 3 (Android native alignment):** the copied `androidApp/src/main/cpp/
  llama-jni.cpp` still uses the **old JNI symbol names** (`Java_com_example_
  qwen3chat_LlamaEngine_*`) and the original method set. It must be updated to the
  new package `com_example_qwen3_core_LlamaEngine` and to expose the full surface
  declared in `LlamaEngine.kt` (`nativeStartGeneration`, `nativeEndGeneration`,
  `nativeEvaluatePromptAt`, `nativeCancel`, `nativeFreeContext`, `nativeFreeModel`,
  `nativeCleanup`). Some of these may need small additions to the C++.
- **Phase 4 (iOS native):** run `scripts/build_ios_llama.sh` on a Mac to produce
  `llama.xcframework`, enable the cinterop block in `shared/core/build.gradle.kts`,
  and fill in the `TODO(cinterop)` calls in `IosInferenceEngine`.
- **Phase 5 (UI):** port the screens (ChatList, ChatDetail with streaming + send-button
  states + long-press Copy/Share/Regenerate/Edit&Resend + voice, Settings, Diagnostics,
  ModelSelection, Notes) into `:shared:ui` with JB ViewModels + navigation-compose.
- **Phase 6:** `GenerationController` (Android foreground service / iOS BGTask +
  idle-timer), `ModelDownloader` (Ktor on Android, URLSession background on iOS),
  notifications, voice (`RecognizerIntent` / `SFSpeechRecognizer`).
- **Tooling:** the Gradle wrapper `.jar`/scripts are not included — run
  `gradle wrapper --gradle-version 8.11.1` once locally. The `llama.cpp` submodule
  must be initialized: `git submodule update --init --recursive`.

> This scaffold was assembled in a Linux environment **without** Android SDK/NDK,
> Xcode, or network access to Gradle/Maven, so it has **not been compiled here**.
> The shared Kotlin is written to compile; verify with the steps below.

## Build

### Prereqs
```bash
git submodule update --init --recursive       # fetch llama.cpp
gradle wrapper --gradle-version 8.11.1         # generate ./gradlew
```

### Shared core tests
```bash
./gradlew :shared:core:allTests
```

### Android
```bash
./gradlew :androidApp:assembleDebug
```
Requires Android SDK + NDK 26.x. The CMake build pulls in `llama.cpp` and compiles
`llama-jni`. `-DCMAKE_BUILD_TYPE=Release` is set in `androidApp` `defaultConfig` so
the native library is `-O3` even in debug APKs (the key perf fix carried over).

### iOS (on a Mac)
```bash
./scripts/build_ios_llama.sh            # produces llama.xcframework (Metal)
# enable the cinterop block in shared/core/build.gradle.kts, then open iosApp in Xcode
```
See `iosApp/README.md`.

## License / models
GGUF models (0.5–2.6 GB) are downloaded at runtime, never committed (`.gitignore`).
