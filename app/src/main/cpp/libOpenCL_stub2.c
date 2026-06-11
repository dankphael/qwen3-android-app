/* OpenCL Stubs - Pure software stubs with NO external dependencies.
 * Each function matches the exact signature from CL/cl.h.
 * At runtime, if a real libOpenCL.so is available via dlopen,
 * we forward calls to it. Otherwise these stubs are used safely.
 *
 * This library has ZERO dependencies on Android system libraries,
 * so it can be loaded in the app's linker namespace without issues.
 */
#include "CL/cl.h"
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

static pthread_once_t g_init = PTHREAD_ONCE_INIT;
static void *g_real_opencl = NULL;
static int g_have_real = 0;

/* Function pointer types */
typedef cl_int (*pfn_clGetPlatformIDs)(cl_uint, cl_platform_id*, cl_uint*);
typedef cl_int (*pfn_clGetDeviceIDs)(cl_platform_id, cl_device_type, cl_uint, cl_device_id*, cl_uint*);
typedef cl_context (*pfn_clCreateContext)(const cl_context_properties*, cl_uint, const cl_device_id*, void(*)(const char*,const void*,size_t,void*), void*, cl_int*);
typedef cl_context (*pfn_clCreateContextFromType)(const cl_context_properties*, cl_device_type, void(*)(const char*,const void*,size_t,void*), void*, cl_int*);
typedef cl_int (*pfn_clGetContextInfo)(cl_context, cl_context_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clGetDeviceInfo)(cl_device_id, cl_device_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clGetPlatformInfo)(cl_platform_id, cl_platform_info, size_t, void*, size_t*);
typedef cl_command_queue (*pfn_clCreateCommandQueue)(cl_context, cl_device_id, cl_command_queue_properties, cl_int*);
typedef cl_command_queue (*pfn_clCreateCommandQueueWithProperties)(cl_context, cl_device_id, const cl_queue_properties*, cl_int*);
typedef cl_mem (*pfn_clCreateBuffer)(cl_context, cl_mem_flags, size_t, void*, cl_int*);
typedef cl_mem (*pfn_clCreateSubBuffer)(cl_mem, cl_mem_flags, cl_buffer_create_type, const void*, cl_int*);
typedef cl_mem (*pfn_clCreateImage)(cl_context, cl_mem_flags, const cl_image_format*, const cl_image_desc*, void*, cl_int*);
typedef cl_int (*pfn_clEnqueueReadBuffer)(cl_command_queue, cl_mem, cl_bool, size_t, size_t, void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueWriteBuffer)(cl_command_queue, cl_mem, cl_bool, size_t, size_t, const void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueCopyBuffer)(cl_command_queue, cl_mem, cl_mem, size_t, size_t, size_t, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueReadImage)(cl_command_queue, cl_mem, cl_bool, const size_t*, const size_t*, size_t, size_t, void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueWriteImage)(cl_command_queue, cl_mem, cl_bool, const size_t*, const size_t*, size_t, size_t, const void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueCopyImage)(cl_command_queue, cl_mem, cl_mem, const size_t*, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueCopyImageToBuffer)(cl_command_queue, cl_mem, cl_mem, const size_t*, const size_t*, size_t, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueCopyBufferToImage)(cl_command_queue, cl_mem, cl_mem, size_t, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueMapBuffer)(cl_command_queue, cl_mem, cl_bool, cl_map_flags, size_t, size_t, cl_uint, const cl_event*, cl_event*, cl_int*);
typedef cl_int (*pfn_clEnqueueMapImage)(cl_command_queue, cl_mem, cl_bool, cl_map_flags, const size_t*, const size_t*, size_t*, size_t*, cl_uint, const cl_event*, cl_event*, cl_int*);
typedef cl_int (*pfn_clEnqueueUnmapMemObject)(cl_command_queue, cl_mem, void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueNDRangeKernel)(cl_command_queue, cl_kernel, cl_uint, const size_t*, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueFillBuffer)(cl_command_queue, cl_mem, const void*, size_t, size_t, size_t, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueFillImage)(cl_command_queue, cl_mem, const void*, const size_t*, const size_t*, cl_uint, const cl_event*, cl_event*);
typedef cl_program (*pfn_clCreateProgramWithSource)(cl_context, cl_uint, const char**, const size_t*, cl_int*);
typedef cl_program (*pfn_clCreateProgramWithBinary)(cl_context, cl_uint, const cl_device_id*, const size_t*, const unsigned char**, cl_int*, cl_int*);
typedef cl_int (*pfn_clBuildProgram)(cl_program, cl_uint, const cl_device_id*, const char*, void(*)(cl_program,void*), void*);
typedef cl_int (*pfn_clGetProgramInfo)(cl_program, cl_program_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clGetProgramBuildInfo)(cl_program, cl_device_id, cl_program_build_info, size_t, void*, size_t*);
typedef cl_kernel (*pfn_clCreateKernel)(cl_program, const char*, cl_int*);
typedef cl_int (*pfn_clCreateKernelsInProgram)(cl_program, cl_uint, cl_kernel*, cl_uint*);
typedef cl_int (*pfn_clSetKernelArg)(cl_kernel, cl_uint, size_t, const void*);
typedef cl_int (*pfn_clGetKernelInfo)(cl_kernel, cl_kernel_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clGetKernelWorkGroupInfo)(cl_kernel, cl_device_id, cl_kernel_work_group_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clFinish)(cl_command_queue);
typedef cl_int (*pfn_clFlush)(cl_command_queue);
typedef cl_int (*pfn_clReleaseMemObject)(cl_mem);
typedef cl_int (*pfn_clReleaseKernel)(cl_kernel);
typedef cl_int (*pfn_clReleaseProgram)(cl_program);
typedef cl_int (*pfn_clReleaseCommandQueue)(cl_command_queue);
typedef cl_int (*pfn_clReleaseContext)(cl_context);
typedef cl_int (*pfn_clReleaseEvent)(cl_event);
typedef cl_int (*pfn_clReleaseSampler)(cl_sampler);
typedef cl_int (*pfn_clRetainMemObject)(cl_mem);
typedef cl_int (*pfn_clRetainKernel)(cl_kernel);
typedef cl_int (*pfn_clRetainProgram)(cl_program);
typedef cl_int (*pfn_clRetainCommandQueue)(cl_command_queue);
typedef cl_int (*pfn_clRetainContext)(cl_context);
typedef cl_int (*pfn_clRetainEvent)(cl_event);
typedef cl_int (*pfn_clRetainSampler)(cl_sampler);
typedef cl_int (*pfn_clGetEventInfo)(cl_event, cl_event_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clWaitForEvents)(cl_uint, const cl_event*);
typedef cl_event (*pfn_clCreateUserEvent)(cl_context, cl_int*);
typedef cl_int (*pfn_clSetUserEventStatus)(cl_event, cl_int);
typedef cl_int (*pfn_clSetEventCallback)(cl_event, cl_int, void(*)(cl_event,cl_int,void*), void*);
typedef cl_int (*pfn_clGetEventProfilingInfo)(cl_event, cl_profiling_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clUnloadCompiler)(void);
typedef cl_int (*pfn_clUnloadPlatformCompiler)(cl_platform_id);
typedef void* (*pfn_clGetExtensionFunctionAddress)(const char*);
typedef void* (*pfn_clGetExtensionFunctionAddressForPlatform)(cl_platform_id, const char*);
typedef cl_mem (*pfn_clCreatePipe)(cl_context, cl_mem_flags, cl_uint, cl_uint, const cl_pipe_properties*, cl_int*);
typedef cl_int (*pfn_clGetPipeInfo)(cl_mem, cl_pipe_info, size_t, void*, size_t*);
typedef void* (*pfn_clSVMAlloc)(cl_context, cl_svm_mem_flags, size_t, cl_uint);
typedef void (*pfn_clSVMFree)(cl_context, void*);
typedef cl_int (*pfn_clEnqueueSVMFree)(cl_command_queue, cl_uint, void**, void(*)(cl_command_queue,cl_uint,void**,void*), void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueSVMMemcpy)(cl_command_queue, cl_bool, void*, const void*, size_t, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueSVMMemFill)(cl_command_queue, void*, const void*, size_t, size_t, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueSVMMap)(cl_command_queue, cl_bool, cl_map_flags, void*, size_t, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clEnqueueSVMUnmap)(cl_command_queue, void*, cl_uint, const cl_event*, cl_event*);
typedef cl_int (*pfn_clSetKernelArgSVMPointer)(cl_kernel, cl_uint, const void*);
typedef cl_int (*pfn_clSetKernelExecInfo)(cl_kernel, cl_kernel_exec_info, size_t, const void*);
typedef cl_sampler (*pfn_clCreateSampler)(cl_context, cl_bool, cl_addressing_mode, cl_filter_mode, cl_int*);
typedef cl_sampler (*pfn_clCreateSamplerWithProperties)(cl_context, const cl_sampler_properties*, cl_int*);
typedef cl_int (*pfn_clGetSamplerInfo)(cl_sampler, cl_sampler_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clGetImageInfo)(cl_mem, cl_image_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clGetMemObjectInfo)(cl_mem, cl_mem_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clGetCommandQueueInfo)(cl_command_queue, cl_command_queue_info, size_t, void*, size_t*);
typedef cl_int (*pfn_clCreateSubDevices)(cl_device_id, const cl_device_partition_property*, cl_uint, cl_device_id*, cl_uint*);
typedef cl_int (*pfn_clRetainDevice)(cl_device_id);
typedef cl_int (*pfn_clReleaseDevice)(cl_device_id);
typedef cl_int (*pfn_clSetDefaultDeviceCommandQueue)(cl_context, cl_device_id, cl_command_queue);
typedef cl_int (*pfn_clGetDeviceAndHostTimer)(cl_device_id, cl_ulong*, cl_ulong*);
typedef cl_int (*pfn_clGetHostTimer)(cl_device_id, cl_ulong*);
typedef cl_program (*pfn_clCreateProgramWithIL)(cl_context, const void*, size_t, cl_int*);
typedef cl_kernel (*pfn_clCloneKernel)(cl_kernel, cl_int*);
typedef cl_int (*pfn_clGetKernelSubGroupInfo)(cl_kernel, cl_device_id, cl_kernel_sub_group_info, size_t, const void*, size_t, void*, size_t*);
typedef cl_int (*pfn_clEnqueueSVMMigrateMem)(cl_command_queue, cl_uint, const void**, const size_t*, cl_svm_mem_flags, cl_uint, const cl_event*, cl_event*);

/* Static function pointers - initially point to stubs */
static pfn_clGetPlatformIDs s_clGetPlatformIDs = NULL;
static pfn_clGetDeviceIDs s_clGetDeviceIDs = NULL;
static pfn_clCreateContext s_clCreateContext = NULL;
/* ... (all other function pointers) ... */

static void try_load_real_opencl(void) {
    /* Try to load the real libOpenCL.so from the device */
    const char *paths[] = {
        "/vendor/lib64/libOpenCL.so",
        "/system/lib64/libOpenCL.so",
        NULL
    };
    for (int i = 0; paths[i]; i++) {
        /* Use android_dlopen_ext to load from system namespace */
        void *handle = dlopen(paths[i], RTLD_NOW | RTLD_LOCAL);
        if (handle) {
            g_real_opencl = handle;
            g_have_real = 1;
            /* Resolve all function pointers */
            s_clGetPlatformIDs = (pfn_clGetPlatformIDs)dlsym(handle, "clGetPlatformIDs");
            s_clGetDeviceIDs = (pfn_clGetDeviceIDs)dlsym(handle, "clGetDeviceIDs");
            s_clCreateContext = (pfn_clCreateContext)dlsym(handle, "clCreateContext");
            /* ... resolve all others ... */
            return;
        }
    }
}

__attribute__((constructor))
static void init_opencl_stubs(void) {
    pthread_once(&g_init, try_load_real_opencl);
}

/* === Stub implementations that forward to real OpenCL if available === */

cl_int clGetPlatformIDs(cl_uint nentries, cl_platform_id *platforms, cl_uint *nplatforms) {
    if (s_clGetPlatformIDs) return s_clGetPlatformIDs(nentries, platforms, nplatforms);
    if (nplatforms) *nplatforms = 0;
    return CL_INVALID_PLATFORM;
}

/* ... (all other stub functions) ... */
