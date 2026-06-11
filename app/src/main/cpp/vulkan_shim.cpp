// vulkan_shim.cpp — Shim for Vulkan functions not in NDK stub library
// On Android, many Vulkan 1.1+ functions are loaded dynamically.
// This shim converts direct calls to dynamic loads.

#include <vulkan/vulkan.h>
#include <dlfcn.h>
#include <pthread.h>

static void* g_vulkan_lib = NULL;
static VkInstance g_instance = VK_NULL_HANDLE;
static pthread_once_t g_init = PTHREAD_ONCE_INIT;

static PFN_vkGetInstanceProcAddr g_vkGetInstanceProcAddr = nullptr;

static void init_vulkan() {
    g_vulkan_lib = dlopen("libvulkan.so", RTLD_NOW | RTLD_LOCAL);
    if (g_vulkan_lib) {
        g_vkGetInstanceProcAddr = (PFN_vkGetInstanceProcAddr)dlsym(g_vulkan_lib, "vkGetInstanceProcAddr");
    }
}

static PFN_vkGetInstanceProcAddr get_gipa() {
    pthread_once(&g_init, init_vulkan);
    return g_vkGetInstanceProcAddr;
}

#define SHIM_FUNC(name) \
    PFN_##name g_##name = nullptr; \
    name##_func(name) { \
        if (!g_##name) { \
            auto gipa = get_gipa(); \
            if (gipa && g_instance) \
                g_##name = (PFN_##name)gipa(g_instance, #name); \
        } \
        return g_##name; \
    }

// We need to intercept vkGetInstanceProcAddr calls to capture the instance
extern "C" {

// Override vkGetInstanceProcAddr to capture instance
VKAPI_ATTR PFN_vkVoidFunction VKAPI_CALL vkGetInstanceProcAddr(VkInstance instance, const char* pName) {
    if (instance) g_instance = instance;
    auto gipa = get_gipa();
    return gipa ? gipa(instance, pName) : nullptr;
}

// Shim for vkGetPhysicalDeviceFeatures2 (Vulkan 1.1)
VKAPI_ATTR void VKAPI_CALL vkGetPhysicalDeviceFeatures2(VkPhysicalDevice physicalDevice, VkPhysicalDeviceFeatures2* pFeatures) {
    static PFN_vkGetPhysicalDeviceFeatures2 ptr = nullptr;
    if (!ptr) {
        auto gipa = get_gipa();
        if (gipa && g_instance)
            ptr = (PFN_vkGetPhysicalDeviceFeatures2)gipa(g_instance, "vkGetPhysicalDeviceFeatures2");
    }
    if (ptr) ptr(physicalDevice, pFeatures);
}

// Shim for vkGetPhysicalDeviceFeatures2KHR
VKAPI_ATTR void VKAPI_CALL vkGetPhysicalDeviceFeatures2KHR(VkPhysicalDevice physicalDevice, VkPhysicalDeviceFeatures2KHR* pFeatures) {
    static PFN_vkGetPhysicalDeviceFeatures2KHR ptr = nullptr;
    if (!ptr) {
        auto gipa = get_gipa();
        if (gipa && g_instance)
            ptr = (PFN_vkGetPhysicalDeviceFeatures2KHR)gipa(g_instance, "vkGetPhysicalDeviceFeatures2KHR");
    }
    if (ptr) ptr(physicalDevice, pFeatures);
}

} // extern "C"
