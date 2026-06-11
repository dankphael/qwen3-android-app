R"(#pragma OPENCL EXTENSION cl_khr_fp16 : enable
)"
R"(
)"
R"(#ifdef cl_intel_subgroups
)"
R"(#pragma OPENCL EXTENSION cl_intel_subgroups : enable
)"
R"(#else
)"
R"(#pragma OPENCL EXTENSION cl_khr_subgroups : enable
)"
R"(#endif
)"
R"(
)"
R"(#ifdef cl_intel_required_subgroup_size
)"
R"(#pragma OPENCL EXTENSION cl_intel_required_subgroup_size : enable
)"
R"(#define INTEL_GPU 1
)"
R"(#define REQD_SUBGROUP_SIZE_16 __attribute__((intel_reqd_sub_group_size(16)))
)"
R"(#define REQD_SUBGROUP_SIZE_32 __attribute__((intel_reqd_sub_group_size(32)))
)"
R"(#elif defined(cl_qcom_reqd_sub_group_size)
)"
R"(#pragma OPENCL EXTENSION cl_qcom_reqd_sub_group_size : enable
)"
R"(#define ADRENO_GPU 1
)"
R"(#define REQD_SUBGROUP_SIZE_64  __attribute__((qcom_reqd_sub_group_size("half")))
)"
R"(#define REQD_SUBGROUP_SIZE_128 __attribute__((qcom_reqd_sub_group_size("full")))
)"
R"(#endif
)"
R"(
)"
R"(//------------------------------------------------------------------------------
)"
R"(// kernel_mul_mv_q6_K_f32_flat
)"
R"(//------------------------------------------------------------------------------
)"
R"(#define Q6_K_MASK1 0x03
)"
R"(#define Q6_K_MASK2 0x0C
)"
R"(#define Q6_K_MASK3 0x30
)"
R"(#define Q6_K_MASK4 0xC0
)"
R"(
)"
R"(#define QK_K       256
)"
R"(
)"
R"(inline float block_q_6_K_dot_y_flat(
)"
R"(    global uchar * blk_ql,
)"
R"(    global uchar * blk_qh,
)"
R"(    global char  * blk_scales,
)"
R"(    global half  * blk_d,
)"
R"(    int ib,
)"
R"(    int ip,
)"
R"(    int is,
)"
R"(    int l0,
)"
R"(    float4 y0,
)"
R"(    float4 y1,
)"
R"(    float4 y2,
)"
R"(    float4 y3
)"
R"() {
)"
R"(    int q_offset_l =  64*ip + l0;
)"
R"(    int q_offset_h =  32*ip + l0;
)"
R"(
)"
R"(    global uchar * q1 = blk_ql     + ib*128 + q_offset_l;
)"
R"(    global uchar * q2 = q1         + QK_K/8;
)"
R"(    global uchar * qh = blk_qh     + ib*64 + q_offset_h;
)"
R"(    global char  * sc = blk_scales + ib*16 + is;
)"
R"(
)"
R"(    float dall = blk_d[ib];
)"
R"(
)"
R"(    // Vectorized loads: 3 uchar4 weight loads instead of 12 scalar byte reads.
)"
R"(    // q_offset_l/h are 4-aligned, so these are aligned vector loads.
)"
R"(    uchar4 q1v = vload4(0, q1);
)"
R"(    uchar4 q2v = vload4(0, q2);
)"
R"(    uchar4 qhv = vload4(0, qh);
)"
R"(
)"
R"(    int4 q1i = convert_int4(q1v);
)"
R"(    int4 q2i = convert_int4(q2v);
)"
R"(    int4 qhi = convert_int4(qhv);
)"
R"(
)"
R"(    // Reconstruct the four 6-bit weight groups (low/high nibble of ql OR'd with the
)"
R"(    // matching 2-bit plane of qh), same arithmetic as the scalar version, then dot()
)"
R"(    // against the cached activation lanes.
)"
R"(    float4 w0 = convert_float4((q1i & 0xF) | ((qhi & Q6_K_MASK1) << 4)) - 32.f;
)"
R"(    float4 w1 = convert_float4((q2i & 0xF) | ((qhi & Q6_K_MASK2) << 2)) - 32.f;
)"
R"(    float4 w2 = convert_float4((q1i >> 4)  | ((qhi & Q6_K_MASK3)     )) - 32.f;
)"
R"(    float4 w3 = convert_float4((q2i >> 4)  | ((qhi & Q6_K_MASK4) >> 2)) - 32.f;
)"
R"(
)"
R"(    return dall * (dot(y0, w0) * sc[0] + dot(y1, w1) * sc[2] +
)"
R"(                   dot(y2, w2) * sc[4] + dot(y3, w3) * sc[6]);
)"
R"(}
)"
R"(
)"
R"(#undef N_DST
)"
R"(#undef N_SIMDGROUP
)"
R"(#undef N_SIMDWIDTH
)"
R"(
)"
R"(#ifdef INTEL_GPU
)"
R"(#define N_DST 4
)"
R"(#define N_SIMDGROUP 2
)"
R"(#define N_SIMDWIDTH 16
)"
R"(#elif defined (ADRENO_GPU)
)"
R"(#define N_DST 16
)"
R"(#define N_SIMDGROUP 2
)"
R"(#define N_SIMDWIDTH 64
)"
R"(#endif
)"
R"(
)"
R"(#define BLOCK_STRIDE (N_SIMDWIDTH/16) // number of blocks each subgroup processes
)"
R"(
)"
R"(#ifdef INTEL_GPU
)"
R"(REQD_SUBGROUP_SIZE_16
)"
R"(#elif defined (ADRENO_GPU)
)"
R"(REQD_SUBGROUP_SIZE_64
)"
R"(#endif
)"
R"(kernel void kernel_mul_mv_q6_K_f32_flat(
)"
R"(        global uchar * src0_ql,
)"
R"(        global uchar * src0_qh,
)"
R"(        global char  * src0_s,
)"
R"(        global half  * src0_d,
)"
R"(        global float * src1,
)"
R"(        ulong offset1,
)"
R"(        global float * dst,
)"
R"(        ulong offsetd,
)"
R"(        int ne00,
)"
R"(        int ne01,
)"
R"(        int ne02,
)"
R"(        int ne10,
)"
R"(        int ne12,
)"
R"(        int ne0,
)"
R"(        int ne1,
)"
R"(        int r2,
)"
R"(        int r3
)"
R"() {
)"
R"(    src1 = (global float*)((global char*)src1 + offset1);
)"
R"(    dst = (global float*)((global char*)dst + offsetd);
)"
R"(
)"
R"(    int nb = ne00/QK_K;
)"
R"(
)"
R"(    int r0 = get_group_id(0);
)"
R"(    int r1 = get_group_id(1);
)"
R"(    int im = get_group_id(2);
)"
R"(
)"
R"(    int i12 = im%ne12;
)"
R"(    int i13 = im/ne12;
)"
R"(
)"
R"(    int first_row = (N_SIMDGROUP * r0 + get_sub_group_id()) * N_DST;
)"
R"(
)"
R"(    ulong offset_src0    = first_row*nb + (i12/r2)*(nb*ne01) + (i13/r3)*(nb*ne01*ne02);
)"
R"(    ulong offset_src0_ql = offset_src0 * 128;
)"
R"(    ulong offset_src0_qh = offset_src0 * 64;
)"
R"(    ulong offset_src0_s  = offset_src0 * 16;
)"
R"(    ulong offset_src0_d  = offset_src0;
)"
R"(
)"
R"(    global uchar * blk_ql     = (global uchar *) src0_ql + offset_src0_ql;
)"
R"(    global uchar * blk_qh     = (global uchar *) src0_qh + offset_src0_qh;
)"
R"(    global char  * blk_scales = (global char  *) src0_s  + offset_src0_s;
)"
R"(    global half  * blk_d      = (global half  *) src0_d  + offset_src0_d;
)"
R"(    global float * yy         = (global float *) src1    + r1*ne10 + im*ne00*ne1;
)"
R"(
)"
R"(    int tid = get_sub_group_local_id()%(N_SIMDWIDTH/BLOCK_STRIDE); // within-super-block part, 0..15
)"
R"(    int ix  = get_sub_group_local_id()/(N_SIMDWIDTH/BLOCK_STRIDE); // super-block selector, 0..BLOCK_STRIDE-1
)"
R"(    int ip  = tid/8;   // first or second half of (super) block (0 or 1)
)"
R"(    int il  = tid%8;   // each half has 8 parts, one per scale
)"
R"(    int n   = 4;       // 4 scales at a time (and 4 sums)
)"
R"(    int l0  = n*il;    // offset into half-block, 0..28
)"
R"(    int is  = 8*ip + l0/16; // 0, 1, 8, 9
)"
R"(
)"
R"(    float sumf[N_DST];
)"
R"(    for (int row = 0; row < N_DST; row++) {
)"
R"(        sumf[row] = 0.f;
)"
R"(    }
)"
R"(
)"
R"(    for (int ib = ix; ib < nb; ib += BLOCK_STRIDE) {
)"
R"(        global float * y = yy + ib * QK_K + 128*ip + l0;
)"
R"(        float4 y0 = vload4(0, y +  0);
)"
R"(        float4 y1 = vload4(0, y + 32);
)"
R"(        float4 y2 = vload4(0, y + 64);
)"
R"(        float4 y3 = vload4(0, y + 96);
)"
R"(
)"
R"(        for (int row = 0; row < N_DST; row++) {
)"
R"(            if (first_row + row < ne01) {
)"
R"(                sumf[row] += block_q_6_K_dot_y_flat(
)"
R"(                    blk_ql + row*nb*128, blk_qh + row*nb*64, blk_scales + row*nb*16, blk_d + row*nb,
)"
R"(                    ib, ip, is, l0, y0, y1, y2, y3);
)"
R"(            }
)"
R"(        }
)"
R"(    }
)"
R"(
)"
R"(    for (int row = 0; row < N_DST; row++) {
)"
R"(        float tot = sub_group_reduce_add(sumf[row]);
)"
R"(        if (get_sub_group_local_id() == 0 && first_row + row < ne01) {
)"
R"(            dst[r1*ne0 + im*ne0*ne1 + first_row + row] = tot;
)"
R"(        }
)"
R"(    }
)"
R"(}
)"
