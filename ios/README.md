# iosApp

SwiftUI shell that hosts the shared Compose Multiplatform UI.

This folder contains the Swift sources; the Xcode project itself must be created
on a Mac (it is not generated in the Linux scaffold). Steps:

1. In Xcode: **File → New → Project → iOS App** (SwiftUI lifecycle), name `iosApp`,
   bundle id `com.example.qwen3`. Replace the generated `ContentView.swift` /
   `App.swift` with the ones in this folder.
2. Build the shared framework and link it (see the root README "iOS build"):
   - Add a "Run Script" build phase that runs `:shared:ui:embedAndSignAppleFrameworkForXcode`
     (the standard KMP Xcode integration), OR use the `cocoapods`/`xcframework`
     output. The framework `baseName` is **`sharedUi`** (exports `:shared:core`).
3. Build `llama.xcframework` first via `../scripts/build_ios_llama.sh`, enable the
   cinterop block in `shared/core/build.gradle.kts`, and embed `llama.xcframework`
   in the Xcode target (linked, static).
4. Add `RECORD_AUDIO`/Speech usage strings, background-download capabilities, and
   the `Metal`/`Accelerate` framework links as described in the root README.
