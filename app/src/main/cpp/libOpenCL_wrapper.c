#include <dlfcn.h>
#include <android/log.h>
#include <string.h>
#include <stdlib.h>

/* Include OpenCL headers for correct type definitions */
#include "CL/cl.h"

#define LOG_TAG "OpenCL Wrapper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void *g_opencl_lib = NULL;
static int g_initialized = 0;

static void ensure_opencl_loaded() {
    if (g_initialized) return;
    g_initialized = 1;

    /* Try loading from device paths */
    const char *paths[] = {
        "/vendor/lib64/libOpenCL.so",
        "/system/lib64/libOpenCL.so",
        "/vendor/lib64/egl/libOpenCL.so",
        "/system/vendor/lib64/libOpenCL.so",
        "libOpenCL.so",
        NULL
    };

    for (int i = 0; paths[i]; i++) {
        g_opencl_lib = dlopen(paths[i], RTLD_NOW | RTLD_LOCAL);
        if (g_opencl_lib) {
            LOGI("OpenCL loaded from: %s", paths[i]);
            return;
        }
    }

    LOGE("Failed to load libOpenCL.so from any path: %s", dlerror());
}

/* Force constructor to run when library is loaded */
__attribute__((constructor))
static void init_opencl() {
    ensure_opencl_loaded();
}

/* === OpenCL 1.0 core APIs — each resolves and forwards to real lib === */

cl_int clGetPlatformIDs(cl_uint nentries, cl_platform_id *platforms, cl_uint *nplatforms) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_uint, cl_platform_id*, cl_uint*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetPlatformIDs");
    if (!fn) { if (nplatforms) *nplatforms = 0; return -1; }
    return fn(nentries, platforms, nplatforms);
}

cl_int clGetDeviceIDs(cl_platform_id platform, cl_ulong device_type, cl_uint nentries, cl_device_id *devices, cl_uint *ndevices) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_platform_id, cl_ulong, cl_uint, cl_device_id*, cl_uint*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetDeviceIDs");
    if (!fn) { if (ndevices) *ndevices = 0; return -1; }
    return fn(platform, device_type, nentries, devices, ndevices);
}

cl_context clCreateContext(const cl_queue_properties *properties, cl_uint ndevices, const cl_device_id *devices,
                           void (*pfn_notify)(const char*, const void*, size_t, void*), void *user_data, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_context (*fn)(const cl_queue_properties*, cl_uint, const cl_device_id*, void(*)(const char*,const void*,size_t,void*), void*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateContext");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(properties, ndevices, devices, pfn_notify, user_data, errcode_ret);
}

cl_int clGetDeviceInfo(cl_device_id device, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_device_id, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetDeviceInfo");
    return fn ? fn(device, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clGetPlatformInfo(cl_platform_id platform, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_platform_id, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetPlatformInfo");
    return fn ? fn(platform, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_command_queue clCreateCommandQueue(cl_context context, cl_device_id device, cl_ulong properties, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_command_queue (*fn)(cl_context, cl_device_id, cl_ulong, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateCommandQueue");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, device, properties, errcode_ret);
}

cl_mem clCreateBuffer(cl_context context, cl_ulong flags, size_t size, void *host_ptr, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_mem (*fn)(cl_context, cl_ulong, size_t, void*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateBuffer");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, flags, size, host_ptr, errcode_ret);
}

cl_int clEnqueueWriteBuffer(cl_command_queue queue, cl_mem buffer, cl_bool blocking_write, size_t offset, size_t size, const void *ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, cl_bool, size_t, size_t, const void*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueWriteBuffer");
    return fn ? fn(queue, buffer, blocking_write, offset, size, ptr, num_events, event_list, event) : -1;
}

cl_int clEnqueueReadBuffer(cl_command_queue queue, cl_mem buffer, cl_bool blocking_read, size_t offset, size_t size, void *ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, cl_bool, size_t, size_t, void*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueReadBuffer");
    return fn ? fn(queue, buffer, blocking_read, offset, size, ptr, num_events, event_list, event) : -1;
}

cl_program clCreateProgramWithSource(cl_context context, cl_uint count, const char **strings, const size_t *lengths, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_program (*fn)(cl_context, cl_uint, const char**, const size_t*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateProgramWithSource");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, count, strings, lengths, errcode_ret);
}

cl_int clBuildProgram(cl_program program, cl_uint ndevices, const cl_device_id *device_list, const char *options, void (*pfn_notify)(cl_program, void*), void *user_data) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_program, cl_uint, const cl_device_id*, const char*, void(*)(cl_program,void*), void*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clBuildProgram");
    return fn ? fn(program, ndevices, device_list, options, pfn_notify, user_data) : -1;
}

cl_kernel clCreateKernel(cl_program program, const char *kernel_name, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_kernel (*fn)(cl_program, const char*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateKernel");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(program, kernel_name, errcode_ret);
}

cl_int clSetKernelArg(cl_kernel kernel, cl_uint arg_index, size_t arg_size, const void *arg_value) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel, cl_uint, size_t, const void*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSetKernelArg");
    return fn ? fn(kernel, arg_index, arg_size, arg_value) : -1;
}

cl_int clEnqueueNDRangeKernel(cl_command_queue queue, cl_kernel kernel, cl_uint work_dim, const size_t *global_work_offset, const size_t *global_work_size, const size_t *local_work_size, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_kernel, cl_uint, const size_t*, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueNDRangeKernel");
    return fn ? fn(queue, kernel, work_dim, global_work_offset, global_work_size, local_work_size, num_events, event_list, event) : -1;
}

cl_int clFinish(cl_command_queue queue) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clFinish");
    return fn ? fn(queue) : -1;
}

cl_int clFlush(cl_command_queue queue) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clFlush");
    return fn ? fn(queue) : -1;
}

cl_int clReleaseMemObject(cl_mem memobj) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_mem) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseMemObject");
    return fn ? fn(memobj) : -1;
}

cl_int clReleaseKernel(cl_kernel kernel) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseKernel");
    return fn ? fn(kernel) : -1;
}

cl_int clReleaseProgram(cl_program program) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_program) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseProgram");
    return fn ? fn(program) : -1;
}

cl_int clReleaseCommandQueue(cl_command_queue queue) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseCommandQueue");
    return fn ? fn(queue) : -1;
}

cl_int clReleaseContext(cl_context context) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_context) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseContext");
    return fn ? fn(context) : -1;
}

cl_int clGetKernelWorkGroupInfo(cl_kernel kernel, cl_device_id device, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel, cl_device_id, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetKernelWorkGroupInfo");
    return fn ? fn(kernel, device, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clGetProgramBuildInfo(cl_program program, cl_device_id device, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_program, cl_device_id, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetProgramBuildInfo");
    return fn ? fn(program, device, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clReleaseEvent(cl_event event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_event) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseEvent");
    return fn ? fn(event) : -1;
}

cl_int clGetEventInfo(cl_event event, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_event, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetEventInfo");
    return fn ? fn(event, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clWaitForEvents(cl_uint num_events, const cl_event *event_list) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_uint, const cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clWaitForEvents");
    return fn ? fn(num_events, event_list) : -1;
}

cl_int clRetainCommandQueue(cl_command_queue queue) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainCommandQueue");
    return fn ? fn(queue) : -1;
}

cl_int clRetainContext(cl_context context) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_context) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainContext");
    return fn ? fn(context) : -1;
}

cl_int clRetainKernel(cl_kernel kernel) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainKernel");
    return fn ? fn(kernel) : -1;
}

cl_int clRetainMemObject(cl_mem memobj) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_mem) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainMemObject");
    return fn ? fn(memobj) : -1;
}

cl_int clRetainProgram(cl_program program) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_program) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainProgram");
    return fn ? fn(program) : -1;
}

cl_int clRetainEvent(cl_event event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_event) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainEvent");
    return fn ? fn(event) : -1;
}

cl_event clCreateUserEvent(cl_context context, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_event (*fn)(cl_context, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateUserEvent");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, errcode_ret);
}

cl_int clSetUserEventStatus(cl_event event, cl_int execution_status) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_event, cl_int) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSetUserEventStatus");
    return fn ? fn(event, execution_status) : -1;
}

cl_int clSetEventCallback(cl_event event, cl_int command_exec_callback_type, void (*pfn_notify)(cl_event, cl_int, void*), void *user_data) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_event, cl_int, void(*)(cl_event,cl_int,void*), void*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSetEventCallback");
    return fn ? fn(event, command_exec_callback_type, pfn_notify, user_data) : -1;
}

cl_int clGetEventProfilingInfo(cl_event event, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_event, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetEventProfilingInfo");
    return fn ? fn(event, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clUnloadCompiler(void) {
    ensure_opencl_loaded();
    static cl_int (*fn)(void) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clUnloadCompiler");
    return fn ? fn() : -1;
}

cl_int clUnloadPlatformCompiler(cl_platform_id platform) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_platform_id) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clUnloadPlatformCompiler");
    return fn ? fn(platform) : -1;
}

void* clGetExtensionFunctionAddress(const char *func_name) {
    ensure_opencl_loaded();
    static void* (*fn)(const char*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetExtensionFunctionAddress");
    return fn ? fn(func_name) : NULL;
}

void* clGetExtensionFunctionAddressForPlatform(cl_platform_id platform, const char *func_name) {
    ensure_opencl_loaded();
    static void* (*fn)(cl_platform_id, const char*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetExtensionFunctionAddressForPlatform");
    return fn ? fn(platform, func_name) : NULL;
}

cl_mem clCreateSubBuffer(cl_mem buffer, cl_ulong flags, cl_uint buffer_create_type, const void *buffer_region, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_mem (*fn)(cl_mem, cl_ulong, cl_uint, const void*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateSubBuffer");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(buffer, flags, buffer_create_type, buffer_region, errcode_ret);
}

cl_int clSetMemObjectDestructorCallback(cl_mem memobj, void (*pfn_notify)(cl_mem, void*), void *user_data) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_mem, void(*)(cl_mem,void*), void*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSetMemObjectDestructorCallback");
    return fn ? fn(memobj, pfn_notify, user_data) : -1;
}

cl_sampler clCreateSampler(cl_context context, cl_bool normalized_coords, cl_uint address_mode, cl_uint filter_mode, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_sampler (*fn)(cl_context, cl_bool, cl_uint, cl_uint, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateSampler");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, normalized_coords, address_mode, filter_mode, errcode_ret);
}

cl_int clGetSamplerInfo(cl_sampler sampler, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_sampler, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetSamplerInfo");
    return fn ? fn(sampler, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clReleaseSampler(cl_sampler sampler) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_sampler) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseSampler");
    return fn ? fn(sampler) : -1;
}

cl_int clRetainSampler(cl_sampler sampler) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_sampler) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainSampler");
    return fn ? fn(sampler) : -1;
}

cl_program clCreateProgramWithBinary(cl_context context, cl_uint ndevices, const cl_device_id *device_list, const size_t *lengths, const unsigned char **binaries, cl_int *binary_status, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_program (*fn)(cl_context, cl_uint, const cl_device_id*, const size_t*, const unsigned char**, cl_int*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateProgramWithBinary");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, ndevices, device_list, lengths, binaries, binary_status, errcode_ret);
}

cl_int clGetProgramInfo(cl_program program, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_program, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetProgramInfo");
    return fn ? fn(program, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clCreateKernelsInProgram(cl_program program, cl_uint num_kernels, cl_kernel *kernels, cl_uint *num_kernels_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_program, cl_uint, cl_kernel*, cl_uint*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateKernelsInProgram");
    if (!fn) { if (num_kernels_ret) *num_kernels_ret = 0; return -1; }
    return fn(program, num_kernels, kernels, num_kernels_ret);
}

cl_int clGetKernelInfo(cl_kernel kernel, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetKernelInfo");
    return fn ? fn(kernel, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clGetKernelArgInfo(cl_kernel kernel, cl_uint arg_index, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel, cl_uint, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetKernelArgInfo");
    return fn ? fn(kernel, arg_index, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clEnqueueFillBuffer(cl_command_queue queue, cl_mem buffer, const void *pattern, size_t pattern_size, size_t offset, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, const void*, size_t, size_t, size_t, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueFillBuffer");
    return fn ? fn(queue, buffer, pattern, pattern_size, offset, size, num_events, event_list, event) : -1;
}

cl_int clEnqueueCopyBuffer(cl_command_queue queue, cl_mem src_buffer, cl_mem dst_buffer, size_t src_offset, size_t dst_offset, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, cl_mem, size_t, size_t, size_t, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueCopyBuffer");
    return fn ? fn(queue, src_buffer, dst_buffer, src_offset, dst_offset, size, num_events, event_list, event) : -1;
}

cl_int clEnqueueCopyImage(cl_command_queue queue, cl_mem src_image, cl_mem dst_image, const size_t *src_origin, const size_t *dst_origin, const size_t *region, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, cl_mem, const size_t*, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueCopyImage");
    return fn ? fn(queue, src_image, dst_image, src_origin, dst_origin, region, num_events, event_list, event) : -1;
}

cl_int clEnqueueCopyImageToBuffer(cl_command_queue queue, cl_mem src_image, cl_mem dst_buffer, const size_t *src_origin, const size_t *region, size_t dst_offset, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, cl_mem, const size_t*, const size_t*, size_t, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueCopyImageToBuffer");
    return fn ? fn(queue, src_image, dst_buffer, src_origin, region, dst_offset, num_events, event_list, event) : -1;
}

cl_int clEnqueueCopyBufferToImage(cl_command_queue queue, cl_mem src_buffer, cl_mem dst_image, size_t src_offset, const size_t *dst_origin, const size_t *region, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, cl_mem, size_t, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueCopyBufferToImage");
    return fn ? fn(queue, src_buffer, dst_image, src_offset, dst_origin, region, num_events, event_list, event) : -1;
}

cl_int clEnqueueMapBuffer(cl_command_queue queue, cl_mem buffer, cl_bool blocking_map, cl_ulong map_flags, size_t offset, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static void* (*fn)(cl_command_queue, cl_mem, cl_bool, cl_ulong, size_t, size_t, cl_uint, const cl_event*, cl_event*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueMapBuffer");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(queue, buffer, blocking_map, map_flags, offset, size, num_events, event_list, event, errcode_ret);
}

cl_int clEnqueueUnmapMemObject(cl_command_queue queue, cl_mem memobj, void *mapped_ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_mem, void*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueUnmapMemObject");
    return fn ? fn(queue, memobj, mapped_ptr, num_events, event_list, event) : -1;
}

cl_int clGetImageInfo(cl_mem image, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_mem, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetImageInfo");
    return fn ? fn(image, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clGetMemObjectInfo(cl_mem memobj, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_mem, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetMemObjectInfo");
    return fn ? fn(memobj, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clGetCommandQueueInfo(cl_command_queue queue, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetCommandQueueInfo");
    return fn ? fn(queue, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clCreateSubDevices(cl_device_id device, const cl_device_partition_property *properties, cl_uint num_devices, cl_device_id *devices, cl_uint *num_devices_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_device_id, const cl_device_partition_property*, cl_uint, cl_device_id*, cl_uint*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateSubDevices");
    if (!fn) { if (num_devices_ret) *num_devices_ret = 0; return -1; }
    return fn(device, properties, num_devices, devices, num_devices_ret);
}

cl_int clRetainDevice(cl_device_id device) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_device_id) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clRetainDevice");
    return fn ? fn(device) : -1;
}

cl_int clReleaseDevice(cl_device_id device) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_device_id) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clReleaseDevice");
    return fn ? fn(device) : -1;
}

cl_command_queue clCreateCommandQueueWithProperties(cl_context context, cl_device_id device, const cl_queue_properties *properties, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_command_queue (*fn)(cl_context, cl_device_id, const cl_queue_properties*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateCommandQueueWithProperties");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, device, properties, errcode_ret);
}

cl_mem clCreatePipe(cl_context context, cl_ulong flags, cl_uint pipe_packet_size, cl_uint pipe_max_packets, const cl_pipe_properties *pipe_properties, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_mem (*fn)(cl_context, cl_ulong, cl_uint, cl_uint, const cl_pipe_properties*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreatePipe");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, flags, pipe_packet_size, pipe_max_packets, pipe_properties, errcode_ret);
}

cl_int clGetPipeInfo(cl_mem pipe, cl_uint param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_mem, cl_uint, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetPipeInfo");
    return fn ? fn(pipe, param_name, param_value_size, param_value, param_value_size_ret) : -1;
}

void* clSVMAlloc(cl_context context, cl_ulong flags, size_t size, cl_uint alignment) {
    ensure_opencl_loaded();
    static void* (*fn)(cl_context, cl_ulong, size_t, cl_uint) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSVMAlloc");
    return fn ? fn(context, flags, size, alignment) : NULL;
}

void clSVMFree(cl_context context, void *svm_pointer) {
    ensure_opencl_loaded();
    static void (*fn)(cl_context, void*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSVMFree");
    if (fn) fn(context, svm_pointer);
}

cl_int clEnqueueSVMFree(cl_command_queue queue, cl_uint num_svm_pointers, void **svm_pointers, void (*pfn_free_func)(cl_command_queue, cl_uint, void**, void*), void *user_data, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_uint, void**, void(*)(cl_command_queue,cl_uint,void**,void*), void*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueSVMFree");
    return fn ? fn(queue, num_svm_pointers, svm_pointers, pfn_free_func, user_data, num_events, event_list, event) : -1;
}

cl_int clEnqueueSVMMemcpy(cl_command_queue queue, cl_bool blocking_copy, void *dst_ptr, const void *src_ptr, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_bool, void*, const void*, size_t, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueSVMMemcpy");
    return fn ? fn(queue, blocking_copy, dst_ptr, src_ptr, size, num_events, event_list, event) : -1;
}

cl_int clEnqueueSVMMemFill(cl_command_queue queue, void *svm_ptr, const void *pattern, size_t pattern_size, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, void*, const void*, size_t, size_t, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueSVMMemFill");
    return fn ? fn(queue, svm_ptr, pattern, pattern_size, size, num_events, event_list, event) : -1;
}

cl_int clEnqueueSVMMap(cl_command_queue queue, cl_bool blocking_map, cl_ulong flags, void *svm_ptr, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_bool, cl_ulong, void*, size_t, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueSVMMap");
    return fn ? fn(queue, blocking_map, flags, svm_ptr, size, num_events, event_list, event) : -1;
}

cl_int clEnqueueSVMUnmap(cl_command_queue queue, void *svm_ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, void*, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueSVMUnmap");
    return fn ? fn(queue, svm_ptr, num_events, event_list, event) : -1;
}

cl_int clSetKernelArgSVMPointer(cl_kernel kernel, cl_uint arg_index, const void *arg_value) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel, cl_uint, const void*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSetKernelArgSVMPointer");
    return fn ? fn(kernel, arg_index, arg_value) : -1;
}

cl_int clSetKernelExecInfo(cl_kernel kernel, cl_uint param_name, size_t param_value_size, const void *param_value) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel, cl_uint, size_t, const void*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSetKernelExecInfo");
    return fn ? fn(kernel, param_name, param_value_size, param_value) : -1;
}

cl_sampler clCreateSamplerWithProperties(cl_context context, const cl_sampler_properties *sampler_properties, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_sampler (*fn)(cl_context, const cl_sampler_properties*, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateSamplerWithProperties");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, sampler_properties, errcode_ret);
}

cl_int clSetDefaultDeviceCommandQueue(cl_context context, cl_device_id device, cl_command_queue command_queue) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_context, cl_device_id, cl_command_queue) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clSetDefaultDeviceCommandQueue");
    return fn ? fn(context, device, command_queue) : -1;
}

cl_int clGetDeviceAndHostTimer(cl_device_id device, cl_ulong *device_timestamp, cl_ulong *host_timestamp) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_device_id, cl_ulong*, cl_ulong*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetDeviceAndHostTimer");
    return fn ? fn(device, device_timestamp, host_timestamp) : -1;
}

cl_int clGetHostTimer(cl_device_id device, cl_ulong *host_timestamp) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_device_id, cl_ulong*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetHostTimer");
    return fn ? fn(device, host_timestamp) : -1;
}

cl_program clCreateProgramWithIL(cl_context context, const void *il, size_t length, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_program (*fn)(cl_context, const void*, size_t, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCreateProgramWithIL");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(context, il, length, errcode_ret);
}

cl_kernel clCloneKernel(cl_kernel source_kernel, cl_int *errcode_ret) {
    ensure_opencl_loaded();
    static cl_kernel (*fn)(cl_kernel, cl_int*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clCloneKernel");
    if (!fn) { if (errcode_ret) *errcode_ret = -1; return NULL; }
    return fn(source_kernel, errcode_ret);
}

cl_int clGetKernelSubGroupInfo(cl_kernel kernel, cl_device_id device, cl_uint param_name, size_t input_value_size, const void *input_value, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_kernel, cl_device_id, cl_uint, size_t, const void*, size_t, void*, size_t*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetKernelSubGroupInfo");
    return fn ? fn(kernel, device, param_name, input_value_size, input_value, param_value_size, param_value, param_value_size_ret) : -1;
}

cl_int clEnqueueSVMMigrateMem(cl_command_queue queue, cl_uint num_svm_pointers, const void **svm_pointers, const size_t *sizes, cl_ulong flags, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_command_queue, cl_uint, const void**, const size_t*, cl_ulong, cl_uint, const cl_event*, cl_event*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clEnqueueSVMMigrateMem");
    return fn ? fn(queue, num_svm_pointers, svm_pointers, sizes, flags, num_events, event_list, event) : -1;
}

cl_int clGetDeviceIDsFromD3D10KHR(cl_platform_id platform, cl_ulong d3d_device_source, void *d3d_object, cl_ulong d3d_device_set, cl_uint num_entries, cl_device_id *devices, cl_uint *num_devices) {
    ensure_opencl_loaded();
    static cl_int (*fn)(cl_platform_id, cl_ulong, void*, cl_ulong, cl_uint, cl_device_id*, cl_uint*) = NULL;
    if (!fn && g_opencl_lib) fn = dlsym(g_opencl_lib, "clGetDeviceIDsFromD3D10KHR");
    if (!fn) { if (num_devices) *num_devices = 0; return -1; }
    return fn(platform, d3d_device_source, d3d_object, d3d_device_set, num_entries, devices, num_devices);
}
