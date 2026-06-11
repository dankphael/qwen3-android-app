# Qwen3 Chat

A personal AI assistant that runs entirely on your Android device. Install the APK, the model downloads automatically, and you're chatting — no cloud, no API keys, fully private.

## Features

- Lightweight APK — the ~2.5GB model downloads automatically on first launch
- Real on-device inference powered by [llama.cpp](https://github.com/ggerganov/llama.cpp)
- Runs fully offline after initial setup
- Private by design — no data leaves your device
- Multi-turn conversation with full chat history
- Supports download resume if interrupted

## How It Works

1. Install the APK
2. On first launch, the app downloads the quantized model (~2.5GB) from Hugging Face
3. The model loads into memory (takes ~15-30 seconds)
4. Chat away — everything runs locally on your phone's CPU
5. The model is cached — subsequent launches skip the download

## Project Structure

```
qwen3-android-app/
├── llama.cpp/                          # Inference engine (git submodule)
├── app/src/main/
│   ├── cpp/
│   │   ├── CMakeLists.txt              # Native build config
│   │   └── llama-jni.cpp               # JNI bridge (C++ ↔ Kotlin)
│   ├── java/com/example/qwen3chat/
│   │   ├── MainActivity.kt             # Chat UI, download + loading flow
│   │   ├── ChatAdapter.kt              # RecyclerView adapter
│   │   ├── ChatMessage.kt              # Message data class
│   │   ├── LlamaEngine.kt              # Native method declarations
│   │   └── QwenModel.kt                # Model download, loading, and inference
│   ├── res/                            # Layouts, drawables, themes
│   └── AndroidManifest.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## Building & Installing

### Prerequisites
- Android Studio (with NDK and CMake installed via SDK Manager)
- JDK 17+

### Clone with submodules

```bash
git clone --recurse-submodules https://github.com/dankphael/experiment.git
cd experiment/qwen3-android-app
```

If you already cloned without `--recurse-submodules`:
```bash
git submodule update --init --recursive
```

### Build the APK

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Install on your phone

**Option A: USB with ADB**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Option B: Transfer manually**
1. Copy the APK to your phone (USB, Google Drive, email it to yourself, etc.)
2. On your phone, go to **Settings > Security** and enable **Install from Unknown Sources**
3. Open the APK file and tap **Install**

**Option C: Android Studio**
1. Open the project in Android Studio
2. Connect your phone via USB (enable USB Debugging in Developer Options)
3. Click the **Run** button

### First launch
The app will download the model (~2.5GB) over WiFi on first launch, then load it into memory. After that it works fully offline.

## Model

Uses [Qwen3.5-4B-Claude-4.6-Opus-Reasoning-Distilled](https://huggingface.co/Jackrong/Qwen3.5-4B-Claude-4.6-Opus-Reasoning-Distilled-GGUF) in Q4_K_M quantization (GGUF format, ~2.5GB).

Inference runs on-device via llama.cpp compiled for ARM64. No GPU required — runs on CPU with automatic thread detection.

## Requirements

- Android 8.0+ (API 26)
- ARM64 device (virtually all modern Android phones)
- ~4GB free storage (for the model)
- 6GB+ RAM recommended

## Troubleshooting

### Download fails or stalls
- Check your internet connection — the model is ~2.5GB
- Restart the app to resume the download from where it left off

### "Loading model..." takes too long
- First load takes 15-30 seconds depending on your phone
- Make sure you have enough free RAM (close other apps)

### App crashes or out of memory
- Close other apps to free RAM
- Devices with less than 6GB RAM may struggle with 4B models
- Check logcat for `LlamaJNI` or `QwenModel` errors

## License

MIT License
