#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include <fstream>
#include <algorithm>

#include "llama.h"

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global cancellation flag
static std::atomic<bool> g_cancelled{false};

// Track total tokens in KV cache (for incremental eval)
static int g_n_tokens_cached = 0;

// Thread/batch config (stored for diagnostics)
static int g_decode_threads = 0;
static int g_batch_threads = 0;
static int g_batch_size = 0;

// Sampler is created once per generation and reused across token calls
static llama_sampler *g_sampler = nullptr;

// Helper: count big cores on big.LITTLE devices
// Reads /sys/devices/system/cpu/cpuN/cpufreq/cpuinfo_max_freq and groups by frequency
// Returns count of cores in the highest-frequency cluster (typically 3-4 cores on Dimensity)
static int count_big_cores() {
    int n_cpus = (int) std::thread::hardware_concurrency();
    if (n_cpus < 1) n_cpus = 4;

    std::vector<int> frequencies;
    for (int i = 0; i < n_cpus; i++) {
        std::string path = "/sys/devices/system/cpu/cpu" + std::to_string(i) +
                          "/cpufreq/cpuinfo_max_freq";
        std::ifstream file(path);
        int freq = 0;
        if (file.is_open()) {
            file >> freq;
            file.close();
        }
        frequencies.push_back(freq);
    }

    // Find max frequency and count cores at that frequency
    if (!frequencies.empty()) {
        int max_freq = *std::max_element(frequencies.begin(), frequencies.end());
        if (max_freq > 0) {
            int big_core_count = std::count(frequencies.begin(), frequencies.end(), max_freq);
            LOGI("Big-core detection: %d cores at max freq %d kHz", big_core_count, max_freq);
            return big_core_count;
        }
    }

    // Fallback: use hardware_concurrency, but cap at 4 for safety
    LOGI("Big-core detection failed, using hardware_concurrency=%d capped at 4", n_cpus);
    return std::min(n_cpus, 4);
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeInit(JNIEnv *, jobject) {
    llama_backend_init();
    g_n_tokens_cached = 0;
    LOGI("llama backend initialized");
}

JNIEXPORT jlong JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeLoadModel(JNIEnv *env, jobject, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);

    auto params = llama_model_default_params();
    // CPU-only inference (no GPU offload)
    params.n_gpu_layers = 0;

    LOGI("Loading model from: %s (GPU offload: %d layers)", path, (int)params.n_gpu_layers);
    llama_model *model = llama_model_load_from_file(path, params);
    env->ReleaseStringUTFChars(jpath, path);

    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }

    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(model);
}

JNIEXPORT jlong JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeLoadModelGpu(JNIEnv *env, jobject, jstring jpath, jint n_gpu_layers) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);

    auto params = llama_model_default_params();
    params.n_gpu_layers = (uint32_t)n_gpu_layers;

    LOGI("Loading model from: %s (n_gpu_layers=%d)", path, n_gpu_layers);
    llama_model *model = llama_model_load_from_file(path, params);
    env->ReleaseStringUTFChars(jpath, path);

    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }

    LOGI("Model loaded successfully (GPU offload: %d layers)", n_gpu_layers);
    return reinterpret_cast<jlong>(model);
}

JNIEXPORT jlong JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeCreateContext(
        JNIEnv *env, jobject, jlong model_ptr, jint n_ctx, jint deviceTier) {
    auto *model = reinterpret_cast<llama_model *>(model_ptr);
    if (!model) {
        LOGE("nativeCreateContext: null model");
        return 0;
    }

    auto ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (uint32_t) n_ctx;

    // Tier-based batch size tuning
    switch (deviceTier) {
        case 0: ctx_params.n_batch = 256; break;   // LOW
        case 1: ctx_params.n_batch = 512; break;   // MID
        case 2: ctx_params.n_batch = 1024; break;  // HIGH
        default: ctx_params.n_batch = 512; break;
    }

    // On big.LITTLE devices, restrict to big cores only to avoid thread-stall on little cores
    // count_big_cores() reads cpuinfo_max_freq per CPU to detect the fast cluster
    int n_threads = count_big_cores();

    // Use big cores for both batch and decode
    ctx_params.n_threads_batch = n_threads;
    int decode_threads = n_threads;
    ctx_params.n_threads = decode_threads;

    // Enable flash attention for faster KV attention computation
    ctx_params.flash_attn = true;

    llama_context *ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context (n_ctx=%d, n_batch=%d, decode_threads=%d, batch_threads=%d)",
             n_ctx, (int)ctx_params.n_batch, decode_threads, n_threads);
        return 0;
    }

    g_n_tokens_cached = 0;
    LOGI("Context created (n_ctx=%d, decode_threads=%d, batch_threads=%d, tier=%d)",
         n_ctx, decode_threads, n_threads, deviceTier);

    g_decode_threads = decode_threads;
    g_batch_threads = n_threads;
    g_batch_size = (int)ctx_params.n_batch;

    return reinterpret_cast<jlong>(ctx);
}

// Backward-compatible version (accepts only model_ptr + n_ctx, uses defaults)
JNIEXPORT jlong JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeCreateContextLegacy(
        JNIEnv *env, jobject, jlong model_ptr, jint n_ctx) {
    return Java_com_example_qwen3chat_LlamaEngine_nativeCreateContext(
        env, nullptr, model_ptr, n_ctx, 1);  // tier 1 = MID default
}

JNIEXPORT jint JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeEvaluatePrompt(
        JNIEnv *env, jobject, jlong ctx_ptr, jstring jprompt) {
    auto *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
    if (!ctx) {
        LOGE("nativeEvaluatePrompt: null context");
        return -1;
    }

    const char *prompt = env->GetStringUTFChars(jprompt, nullptr);
    const int prompt_len = env->GetStringUTFLength(jprompt);

    const llama_vocab *vocab = llama_model_get_vocab(llama_get_model(ctx));

    // Tokenize
    int n_tokens = -llama_tokenize(vocab, prompt, prompt_len, nullptr, 0, true, true);
    std::vector<llama_token> tokens(n_tokens);
    llama_tokenize(vocab, prompt, prompt_len, tokens.data(), tokens.size(), true, true);
    env->ReleaseStringUTFChars(jprompt, prompt);

    LOGI("Tokenized prompt: %d tokens (cache has %d)", n_tokens, g_n_tokens_cached);

    // Evaluate: try single-batch first, fall back to chunked
    const int n_batch = llama_n_batch(ctx);
    if (n_tokens <= n_batch) {
        // Single batch — much faster for short prompts
        if (llama_decode(ctx, llama_batch_get_one(tokens.data(), n_tokens)) != 0) {
            LOGE("Failed to evaluate prompt (single batch)");
            return -1;
        }
    } else {
        // Chunked evaluation for long prompts
        for (int i = 0; i < n_tokens; i += n_batch) {
            int n_eval = std::min(n_batch, n_tokens - i);
            if (llama_decode(ctx, llama_batch_get_one(tokens.data() + i, n_eval)) != 0) {
                LOGE("Failed to evaluate prompt at chunk %d", i);
                return -1;
            }
        }
    }

    g_n_tokens_cached += n_tokens;
    return n_tokens;
}

// Overload with startPos for incremental eval (avoids re-evaluating cached tokens)
JNIEXPORT jint JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeEvaluatePrompt__JLjava_lang_String_2I(
        JNIEnv *env, jobject, jlong ctx_ptr, jstring jprompt, jint startPos) {
    auto *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
    if (!ctx) {
        LOGE("nativeEvaluatePrompt: null context");
        return -1;
    }

    const char *prompt = env->GetStringUTFChars(jprompt, nullptr);
    const int prompt_len = env->GetStringUTFLength(jprompt);

    const llama_vocab *vocab = llama_model_get_vocab(llama_get_model(ctx));

    // Tokenize
    int n_tokens = -llama_tokenize(vocab, prompt, prompt_len, nullptr, 0, true, true);
    std::vector<llama_token> tokens(n_tokens);
    llama_tokenize(vocab, prompt, prompt_len, tokens.data(), tokens.size(), true, true);
    env->ReleaseStringUTFChars(jprompt, prompt);

    LOGI("Tokenized prompt: %d tokens (cache has %d, startPos=%d)", n_tokens, g_n_tokens_cached, startPos);

    // Evaluate in chunks of n_batch, with positions offset by startPos
    const int n_batch = llama_n_batch(ctx);
    for (int i = 0; i < n_tokens; i += n_batch) {
        int n_eval = std::min(n_batch, n_tokens - i);
        llama_batch batch = llama_batch_get_one(tokens.data() + i, n_eval);
        // Set correct positions for incremental eval
        for (int j = 0; j < n_eval; j++) {
            batch.pos[j] = startPos + i + j;
        }
        if (llama_decode(ctx, batch) != 0) {
            LOGE("Failed to evaluate prompt at chunk %d", i);
            return -1;
        }
    }

    g_n_tokens_cached += n_tokens;
    return n_tokens;
}

/** Start a streaming generation session: init sampler, reset cancel flag */
JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeStartGeneration(
        JNIEnv *, jobject, jfloat temperature) {
    g_cancelled.store(false);

    // Free any previous sampler
    if (g_sampler) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }

    auto sparams = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
}

/**
 * Generate one token. Returns the token text, or empty string if done/cancelled.
 * Returns null jstring on end-of-generation or cancellation.
 */
JNIEXPORT jstring JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGenerateNextToken(
        JNIEnv *env, jobject, jlong ctx_ptr) {
    auto *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
    if (!ctx || !g_sampler) {
        return env->NewStringUTF("");
    }

    if (g_cancelled.load()) {
        return env->NewStringUTF("");
    }

    const llama_vocab *vocab = llama_model_get_vocab(llama_get_model(ctx));
    llama_token new_token = llama_sampler_sample(g_sampler, ctx, -1);

    // End of generation
    if (llama_vocab_is_eog(vocab, new_token)) {
        return env->NewStringUTF("");
    }

    // Convert token to text
    char buf[256];
    int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, false);

    // Decode the token into the KV cache for next iteration
    if (llama_decode(ctx, llama_batch_get_one(&new_token, 1)) != 0) {
        LOGE("Failed to decode token");
        return env->NewStringUTF("");
    }

    g_n_tokens_cached++;

    if (n > 0) {
        // Use NewString with jbyteArray to handle non-UTF-8 token output safely
        // llama_token_to_piece can produce raw bytes that are not valid Modified UTF-8
        jbyteArray jbytes = env->NewByteArray(n);
        env->SetByteArrayRegion(jbytes, 0, n, reinterpret_cast<const jbyte*>(buf));
        // Convert bytes to Java String using UTF-8 decoder (not Modified UTF-8)
        jclass stringClass = env->FindClass("java/lang/String");
        jmethodID ctor = env->GetMethodID(stringClass, "<init>", "([BLjava/lang/String;)V");
        jstring encoding = env->NewStringUTF("UTF-8");
        jstring result = (jstring)env->NewObject(stringClass, ctor, jbytes, encoding);
        env->DeleteLocalRef(jbytes);
        env->DeleteLocalRef(encoding);
        env->DeleteLocalRef(stringClass);
        return result;
    }
    return env->NewStringUTF("");
}

/** End generation session: free sampler, log stats */
JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeEndGeneration(JNIEnv *, jobject) {
    if (g_sampler) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGenerate(
        JNIEnv *env, jobject, jlong ctx_ptr, jint max_tokens, jfloat temperature) {
    auto *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
    if (!ctx) {
        LOGE("nativeGenerate: null context");
        return env->NewStringUTF("[Error: no inference context]");
    }

    g_cancelled.store(false);

    const llama_vocab *vocab = llama_model_get_vocab(llama_get_model(ctx));

    auto sparams = llama_sampler_chain_default_params();
    llama_sampler *sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string result;
    int generated = 0;
    for (int i = 0; i < max_tokens; i++) {
        if (g_cancelled.load()) break;

        llama_token new_token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, new_token)) break;

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, false);
        if (n > 0) result.append(buf, n);

        if (llama_decode(ctx, llama_batch_get_one(&new_token, 1)) != 0) break;
        generated++;
    }

    g_n_tokens_cached += generated;
    LOGI("Generated %d tokens (%zu chars), cache now %d", generated, result.size(), g_n_tokens_cached);

    llama_sampler_free(sampler);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGetCachePosition(JNIEnv *, jobject) {
    return g_n_tokens_cached;
}

JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeResetCache(JNIEnv *, jobject, jlong ctx_ptr) {
    auto *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
    if (ctx) {
        llama_memory_t mem = llama_get_memory(ctx);
        llama_memory_clear(mem, true);
        g_n_tokens_cached = 0;
        LOGI("KV cache cleared");
    }
}

JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeCancel(JNIEnv *, jobject) {
    g_cancelled.store(true);
    LOGI("Cancellation requested");
}

JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeFreeContext(JNIEnv *, jobject, jlong ctx_ptr) {
    auto *ctx = reinterpret_cast<llama_context *>(ctx_ptr);
    if (ctx) {
        llama_free(ctx);
        g_n_tokens_cached = 0;
        LOGI("Context freed");
    }
}

JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeFreeModel(JNIEnv *, jobject, jlong model_ptr) {
    auto *model = reinterpret_cast<llama_model *>(model_ptr);
    if (model) {
        llama_model_free(model);
        LOGI("Model freed");
    }
}

JNIEXPORT void JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeCleanup(JNIEnv *, jobject) {
    llama_backend_free();
    LOGI("llama backend freed");
}

JNIEXPORT jint JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGetDecodeThreads(JNIEnv *, jobject) {
    return g_decode_threads;
}

JNIEXPORT jint JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGetBatchThreads(JNIEnv *, jobject) {
    return g_batch_threads;
}

JNIEXPORT jint JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGetBatchSize(JNIEnv *, jobject) {
    return g_batch_size;
}

// ── GPU detection ──

#include <dlfcn.h>

// Attempt to load libOpenCL.so and query a GPU device
// Use android_dlopen_ext to load from the system namespace since libOpenCL.so
// has dependencies (libcutils.so, libutils.so) that are only in the system namespace
#include <android/dlext.h>
#include <dlfcn.h>

static bool probe_opencl(char *name_buf, size_t name_len, long *mem_mb) {
    // Try loading libOpenCL.so from various paths
    const char *paths[] = {
        "libOpenCL.so",
        "/vendor/lib64/libOpenCL.so",
        "/system/lib64/libOpenCL.so",
        "/vendor/lib64/egl/libGLES_mali.so",  // Mali GPU driver
        NULL
    };
    void *handle = NULL;
    for (int i = 0; paths[i]; i++) {
        handle = dlopen(paths[i], RTLD_NOW | RTLD_LOCAL);
        if (handle) {
            LOGI("OpenCL: loaded from %s", paths[i]);
            break;
        }
    }
    if (!handle) {
        LOGI("OpenCL: dlopen failed for all paths: %s", dlerror());
        return false;
    }

    // Load required OpenCL function pointers
    // Use int for cl_int, unsigned for cl_uint, void* for opaque handles
    typedef int (*clGetPlatformIDs_fn)(unsigned, void*, unsigned*);
    typedef int (*clGetDeviceIDs_fn)(void*, unsigned, unsigned, void*, unsigned*);
    typedef int (*clGetDeviceInfo_fn)(void*, unsigned, size_t, void*, size_t*);

    clGetPlatformIDs_fn pGetPlatformIDs = (clGetPlatformIDs_fn)dlsym(handle, "clGetPlatformIDs");
    clGetDeviceIDs_fn   pGetDeviceIDs   = (clGetDeviceIDs_fn)dlsym(handle, "clGetDeviceIDs");
    clGetDeviceInfo_fn  pGetDeviceInfo  = (clGetDeviceInfo_fn)dlsym(handle, "clGetDeviceInfo");

    if (!pGetPlatformIDs || !pGetDeviceIDs || !pGetDeviceInfo) {
        LOGI("OpenCL: dlsym failed for core functions");
        dlclose(handle);
        return false;
    }

    // Query platforms
    unsigned num_platforms = 0;
    int err = pGetPlatformIDs(0, nullptr, &num_platforms);
    if (err != 0 || num_platforms == 0) {
        LOGI("OpenCL: no platforms found (err=%d)", err);
        dlclose(handle);
        return false;
    }

    // Query GPU devices on first platform
    void *platform = nullptr;
    pGetPlatformIDs(1, &platform, nullptr);

    unsigned num_devices = 0;
    // CL_DEVICE_TYPE_GPU = 4
    err = pGetDeviceIDs(platform, 4, 0, nullptr, &num_devices);
    if (err != 0 || num_devices == 0) {
        LOGI("OpenCL: no GPU devices found (err=%d)", err);
        dlclose(handle);
        return false;
    }

    void *device = nullptr;
    pGetDeviceIDs(platform, 4, 1, &device, nullptr);

    // Get device name (CL_DEVICE_NAME = 0x102B)
    if (name_buf && name_len > 0) {
        size_t name_size = 0;
        err = pGetDeviceInfo(device, 0x102B, 0, nullptr, &name_size);
        if (err == 0 && name_size > 0 && name_size < name_len) {
            pGetDeviceInfo(device, 0x102B, name_size, name_buf, nullptr);
        } else {
            snprintf(name_buf, name_len, "Unknown OpenCL GPU");
        }
    }

    // Get global memory size (CL_DEVICE_GLOBAL_MEM_SIZE = 0x101F)
    if (mem_mb) {
        unsigned long long mem_size = 0;
        size_t mem_size_len = 0;
        err = pGetDeviceInfo(device, 0x101F, sizeof(mem_size), &mem_size, &mem_size_len);
        if (err == 0 && mem_size_len > 0) {
            *mem_mb = (long)(mem_size / (1024 * 1024));
        } else {
            *mem_mb = 0;
        }
    }

    LOGI("OpenCL: found %u GPU device(s), name='%s', mem=%ld MB",
         num_devices, name_buf ? name_buf : "?", mem_mb ? *mem_mb : 0);

    dlclose(handle);
    return true;
}

JNIEXPORT jstring JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeDetectGpuBackend(JNIEnv *env, jobject) {
    char name_buf[256] = {0};
    long mem_mb = 0;

    // Try OpenCL first (most widely supported)
    if (probe_opencl(name_buf, sizeof(name_buf), &mem_mb)) {
        LOGI("GPU backend: OpenCL (%s, %ld MB)", name_buf, mem_mb);
        return env->NewStringUTF("opencl");
    }

    // TODO: Try Vulkan probe here in the future

    LOGI("GPU backend: CPU (no GPU found)");
    return env->NewStringUTF("cpu");
}

JNIEXPORT jstring JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGetGpuName(JNIEnv *env, jobject) {
    char name_buf[256] = {0};
    long mem_mb = 0;

    if (probe_opencl(name_buf, sizeof(name_buf), &mem_mb)) {
        return env->NewStringUTF(name_buf);
    }

    return env->NewStringUTF("None (CPU only)");
}

JNIEXPORT jlong JNICALL
Java_com_example_qwen3chat_LlamaEngine_nativeGetGpuMemory(JNIEnv *, jobject) {
    char name_buf[256] = {0};
    long mem_mb = 0;

    if (probe_opencl(name_buf, sizeof(name_buf), &mem_mb)) {
        return (jlong)mem_mb;
    }

    return 0;
}

} // extern "C"
