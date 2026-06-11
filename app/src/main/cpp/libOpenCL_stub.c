#include <stdint.h>

/* Minimal OpenCL stub - provides all required symbols as weak stubs.
 * Real OpenCL is loaded at runtime from /vendor/lib64/libOpenCL.so by the linker.
 * Each function returns CL_SUCCESS (0) or a safe default.
 */

typedef int32_t cl_int;
typedef uint32_t cl_uint;
typedef uint64_t cl_ulong;
typedef void* cl_platform_id;
typedef void* cl_device_id;
typedef void* cl_context;
typedef void* cl_command_queue;
typedef void* cl_mem;
typedef void* cl_program;
typedef void* cl_kernel;
typedef void* cl_event;
typedef void* cl_sampler;

#define CL_SUCCESS 0
#define CL_DEVICE_TYPE_GPU 0x00000002
#define CL_DEVICE_TYPE_DEFAULT 0x00000001
#define CL_PLATFORM_NAME 0x0090
#define CL_DEVICE_NAME 0x00120
#define CL_DEVICE_MAX_COMPUTE_UNITS 0x001002
#define CL_DEVICE_MAX_WORK_GROUP_SIZE 0x001003
#define CL_DEVICE_GLOBAL_MEM_SIZE 0x00101F
#define CL_MEM_READ_ONLY 0x00000004
#define CL_MEM_WRITE_ONLY 0x00000002
#define CL_MEM_READ_WRITE 0x00000001
#define CL_MEM_COPY_HOST_PTR 0x00000010
#define CL_MEM_USE_HOST_PTR 0x00000008
#define TRUE 1
#define FALSE 0

static char stub_name[] = "OpenCL Stub";
static char stub_version[] = "1.2 Stub";

cl_int __attribute__((weak)) clGetPlatformIDs(cl_uint nentries, cl_platform_id *platforms, cl_uint *nplatforms) {
    if (nplatforms) *nplatforms = 0;
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clGetDeviceIDs(cl_platform_id platform, cl_device_type device_type, cl_uint nentries, cl_device_id *devices, cl_uint *ndevices) {
    if (ndevices) *ndevices = 0;
    return CL_SUCCESS;
}
cl_context __attribute__((weak)) clCreateContext(const void *properties, cl_uint ndevices, const cl_device_id *devices, void *pfn_notify, void *user_data, cl_int *errcode_ret) {
    if (errcode_ret) *errcode_ret = CL_SUCCESS;
    return (cl_context)1;
}
cl_command_queue __attribute__((weak)) clCreateCommandQueue(cl_context context, cl_device_id device, cl_command_queue_properties properties, cl_int *errcode_ret) {
    if (errcode_ret) *errcode_ret = CL_SUCCESS;
    return (cl_command_queue)1;
}
cl_mem __attribute__((weak)) clCreateBuffer(cl_context context, cl_mem_flags flags, size_t size, void *host_ptr, cl_int *errcode_ret) {
    if (errcode_ret) *errcode_ret = CL_SUCCESS;
    return (cl_mem)malloc(size);
}
cl_program __attribute__((weak)) clCreateProgramWithSource(cl_context context, cl_uint count, const char **strings, const size_t *lengths, cl_int *errcode_ret) {
    if (errcode_ret) *errcode_ret = CL_SUCCESS;
    return (cl_program)1;
}
cl_int __attribute__((weak)) clBuildProgram(cl_program program, cl_uint ndevices, const cl_device_id *device_list, const char *options, void *pfn_notify, void *user_data) {
    return CL_SUCCESS;
}
cl_kernel __attribute__((weak)) clCreateKernel(cl_program program, const char *kernel_name, cl_int *errcode_ret) {
    if (errcode_ret) *errcode_ret = CL_SUCCESS;
    return (cl_kernel)1;
}
cl_int __attribute__((weak)) clSetKernelArg(cl_kernel kernel, cl_uint arg_index, size_t arg_size, const void *arg_value) {
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clEnqueueNDRangeKernel(cl_command_queue queue, cl_kernel kernel, cl_uint work_dim, const size_t *global_work_offset, const size_t *global_work_size, const size_t *local_work_size, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clFinish(cl_command_queue queue) {
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clEnqueueReadBuffer(cl_command_queue queue, cl_mem buffer, cl_bool blocking_read, size_t offset, size_t size, void *ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clEnqueueWriteBuffer(cl_command_queue queue, cl_mem buffer, cl_bool blocking_write, size_t offset, size_t size, const void *ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) {
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clReleaseMemObject(cl_mem memobj) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clReleaseKernel(cl_kernel kernel) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clReleaseProgram(cl_program program) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clReleaseCommandQueue(cl_command_queue queue) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clReleaseContext(cl_context context) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetPlatformInfo(cl_platform_id platform, cl_platform_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    if (param_value && param_value_size >= sizeof(stub_name)) memcpy(param_value, stub_name, sizeof(stub_name));
    if (param_value_size_ret) *param_value_size_ret = sizeof(stub_name);
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clGetDeviceInfo(cl_device_id device, cl_device_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    if (param_value && param_value_size >= sizeof(stub_name)) memcpy(param_value, stub_name, sizeof(stub_name));
    if (param_value_size_ret) *param_value_size_ret = sizeof(stub_name);
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clGetProgramBuildInfo(cl_program program, cl_device_id device, cl_program_build_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clGetKernelWorkGroupInfo(cl_kernel kernel, cl_device_id device, cl_kernel_work_group_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) {
    return CL_SUCCESS;
}
cl_int __attribute__((weak)) clRetainCommandQueue(cl_command_queue queue) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clRetainContext(cl_context context) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clRetainKernel(cl_kernel kernel) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clRetainMemObject(cl_mem memobj) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clRetainProgram(cl_program program) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetEventInfo(cl_event event, cl_event_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clWaitForEvents(cl_uint num_events, const cl_event *event_list) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clReleaseEvent(cl_event event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clRetainEvent(cl_event event) { return CL_SUCCESS; }
cl_event __attribute__((weak)) clCreateUserEvent(cl_context context, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_event)1; }
cl_int __attribute__((weak)) clSetUserEventStatus(cl_event event, cl_int execution_status) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clSetEventCallback(cl_event event, cl_int command_exec_callback_type, void *pfn_notify, void *user_data) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetEventProfilingInfo(cl_event event, cl_profiling_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clFlush(cl_command_queue queue) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clUnloadCompiler(void) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clUnloadPlatformCompiler(cl_platform_id platform) { return CL_SUCCESS; }
void* __attribute__((weak)) clGetExtensionFunctionAddress(const char *func_name) { return NULL; }
void* __attribute__((weak)) clGetExtensionFunctionAddressForPlatform(cl_platform_id platform, const char *func_name) { return NULL; }
cl_mem __attribute__((weak)) clCreateSubBuffer(cl_mem buffer, cl_mem_flags flags, cl_buffer_create_type buffer_create_type, const void *buffer_region, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_mem)1; }
cl_int __attribute__((weak)) clSetMemObjectDestructorCallback(cl_mem memobj, void *pfn_notify, void *user_data) { return CL_SUCCESS; }
cl_sampler __attribute__((weak)) clCreateSampler(cl_context context, cl_bool normalized_coords, cl_addressing_mode address_mode, cl_filter_mode filter_mode, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_sampler)1; }
cl_int __attribute__((weak)) clGetSamplerInfo(cl_sampler sampler, cl_sampler_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clReleaseSampler(cl_sampler sampler) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clRetainSampler(cl_sampler sampler) { return CL_SUCCESS; }
cl_program __attribute__((weak)) clCreateProgramWithBinary(cl_context context, cl_uint ndevices, const cl_device_id *device_list, const size_t *lengths, const unsigned char **binaries, cl_int *binary_status, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_program)1; }
cl_int __attribute__((weak)) clGetProgramInfo(cl_program program, cl_program_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clCreateKernelsInProgram(cl_program program, cl_uint num_kernels, cl_kernel *kernels, cl_uint *num_kernels_ret) { if (num_kernels_ret) *num_kernels_ret = 0; return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetKernelInfo(cl_kernel kernel, cl_kernel_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetKernelArgInfo(cl_kernel kernel, cl_uint arg_index, cl_kernel_arg_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueFillBuffer(cl_command_queue queue, cl_mem buffer, const void *pattern, size_t pattern_size, size_t offset, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueCopyBuffer(cl_command_queue queue, cl_mem src_buffer, cl_mem dst_buffer, size_t src_offset, size_t dst_offset, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueCopyImage(cl_command_queue queue, cl_mem src_image, cl_mem dst_image, const size_t *src_origin, const size_t *dst_origin, const size_t *region, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueCopyImageToBuffer(cl_command_queue queue, cl_mem src_image, cl_mem dst_buffer, const size_t *src_origin, const size_t *region, size_t dst_offset, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueCopyBufferToImage(cl_command_queue queue, cl_mem src_buffer, cl_mem dst_image, size_t src_offset, const size_t *dst_origin, const size_t *region, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueMapBuffer(cl_command_queue queue, cl_mem buffer, cl_bool blocking_map, cl_map_flags map_flags, size_t offset, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return malloc(size); }
cl_int __attribute__((weak)) clEnqueueUnmapMemObject(cl_command_queue queue, cl_mem memobj, void *mapped_ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetImageInfo(cl_mem image, cl_image_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetMemObjectInfo(cl_mem memobj, cl_mem_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetCommandQueueInfo(cl_command_queue queue, cl_command_queue_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clCreateSubDevices(cl_device_id device, const void *properties, cl_uint num_devices, cl_device_id *devices, cl_uint *num_devices_ret) { if (num_devices_ret) *num_devices_ret = 0; return CL_SUCCESS; }
cl_int __attribute__((weak)) clRetainDevice(cl_device_id device) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clReleaseDevice(cl_device_id device) { return CL_SUCCESS; }
cl_command_queue __attribute__((weak)) clCreateCommandQueueWithProperties(cl_context context, cl_device_id device, const void *properties, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_command_queue)1; }
cl_mem __attribute__((weak)) clCreatePipe(cl_context context, cl_mem_flags flags, cl_uint pipe_packet_size, cl_uint pipe_max_packets, const void *pipe_properties, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_mem)1; }
cl_int __attribute__((weak)) clGetPipeInfo(cl_mem pipe, cl_pipe_info param_name, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
void* __attribute__((weak)) clSVMAlloc(cl_context context, cl_svm_mem_flags flags, size_t size, cl_uint alignment) { return malloc(size); }
void __attribute__((weak)) clSVMFree(cl_context context, void *svm_pointer) { free(svm_pointer); }
cl_int __attribute__((weak)) clEnqueueSVMFree(cl_command_queue queue, cl_uint num_svm_pointers, void **svm_pointers, void *pfn_free_func, void *user_data, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueSVMMemcpy(cl_command_queue queue, cl_bool blocking_copy, void *dst_ptr, const void *src_ptr, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) { memcpy(dst_ptr, src_ptr, size); return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueSVMMemFill(cl_command_queue queue, void *svm_ptr, const void *pattern, size_t pattern_size, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) { memset(svm_ptr, *(int*)pattern, size); return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueSVMMap(cl_command_queue queue, cl_bool blocking_map, cl_map_flags flags, void *svm_ptr, size_t size, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueSVMUnmap(cl_command_queue queue, void *svm_ptr, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clSetKernelArgSVMPointer(cl_kernel kernel, cl_uint arg_index, const void *arg_value) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clSetKernelExecInfo(cl_kernel kernel, cl_kernel_exec_info param_name, size_t param_value_size, const void *param_value) { return CL_SUCCESS; }
cl_sampler __attribute__((weak)) clCreateSamplerWithProperties(cl_context context, const void *sampler_properties, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_sampler)1; }
cl_int __attribute__((weak)) clSetDefaultDeviceCommandQueue(cl_context context, cl_device_id device, cl_command_queue command_queue) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetDeviceAndHostTimer(cl_device_id device, cl_ulong *device_timestamp, cl_ulong *host_timestamp) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetHostTimer(cl_device_id device, cl_ulong *host_timestamp) { return CL_SUCCESS; }
cl_program __attribute__((weak)) clCreateProgramWithIL(cl_context context, const void *il, size_t length, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_program)1; }
cl_kernel __attribute__((weak)) clCloneKernel(cl_kernel source_kernel, cl_int *errcode_ret) { if (errcode_ret) *errcode_ret = CL_SUCCESS; return (cl_kernel)1; }
cl_int __attribute__((weak)) clGetKernelSubGroupInfo(cl_kernel kernel, cl_device_id device, cl_kernel_sub_group_info param_name, size_t input_value_size, const void *input_value, size_t param_value_size, void *param_value, size_t *param_value_size_ret) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clEnqueueSVMMigrateMem(cl_command_queue queue, cl_uint num_svm_pointers, const void **svm_pointers, const size_t *sizes, cl_mem_migration_flags flags, cl_uint num_events, const cl_event *event_list, cl_event *event) { return CL_SUCCESS; }
cl_int __attribute__((weak)) clGetDeviceIDsFromD3D10KHR(cl_platform_id platform, cl_d3d10_device_source_khr d3d_device_source, void *d3d_object, cl_d3d10_device_set_khr d3d_device_set, cl_uint num_entries, cl_device_id *devices, cl_uint *num_devices) { if (num_devices) *num_devices = 0; return CL_SUCCESS; }
