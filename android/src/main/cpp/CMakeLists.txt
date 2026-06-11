cmake_minimum_required(VERSION 3.22.1)
project(llama-android)

set(LLAMA_CPP_DIR ${CMAKE_SOURCE_DIR}/../../../../llama.cpp)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(GGML_OPENMP ON CACHE BOOL "" FORCE)
# GPU: disabled — OpenCL/Vulkan backends have linker namespace issues on Android
# CPU-only with OpenMP + NEON is stable. GPU offload to be revisited.
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL_EMBED_KERNELS OFF CACHE BOOL "" FORCE)
# Android cross-compile: disable native CPU detection, use armv8-a+simd
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_ARM_ARCH "armv8.2-a" CACHE STRING "" FORCE)
# Performance: enable Link-Time Optimization
set(GGML_LTO ON CACHE BOOL "" FORCE)

# Tell llama.cpp's ggml-opencl where to find our OpenCL headers and kernel files
set(GGML_ANDROID_APP_CPP_DIR ${CMAKE_SOURCE_DIR} CACHE PATH "Android app cpp dir" FORCE)

add_subdirectory(${LLAMA_CPP_DIR} llama.cpp)

# Enable ARM NEON optimizations for arm64-v8a
if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64")
    add_compile_options(-march=armv8.2-a+fp+simd)
endif()

add_library(llama-jni SHARED llama-jni.cpp)

# OpenCL: link device's libOpenCL.so + libcutils.so (transitive dep)
# Both are loaded at runtime from /vendor/lib64/ and /system/lib64/
# The --unresolved-symbols flag handles remaining transitive deps
target_link_libraries(llama-jni PRIVATE
    llama
    llama-common
    log
)
message(STATUS "OpenCL: linked with device libOpenCL.so")

# OpenMP for parallel matrix ops on ARM
find_package(OpenMP)
if(OpenMP_CXX_FOUND)
    target_link_libraries(llama-jni PRIVATE OpenMP::OpenMP_CXX)
    target_compile_options(llama PRIVATE -fopenmp)
    message(STATUS "OpenMP enabled for parallel matrix ops")
endif()
