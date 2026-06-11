R"(#pragma OPENCL EXTENSION cl_khr_subgroups : enable
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
R"(#ifndef S_V
)"
R"(#define S_V 128
)"
R"(#endif
)"
R"(#ifndef KDA
)"
R"(#define KDA 0
)"
R"(#endif
)"
R"(#ifndef SUBGROUP_SIZE
)"
R"(#define SUBGROUP_SIZE 64
)"
R"(#endif
)"
R"(#ifndef LANES_PER_COLUMN
)"
R"(#define LANES_PER_COLUMN 8
)"
R"(#endif
)"
R"(#ifndef COLS_PER_LANE_GROUP
)"
R"(#define COLS_PER_LANE_GROUP 1
)"
R"(#endif
)"
R"(#ifndef SUBGROUPS_PER_WG
)"
R"(#define SUBGROUPS_PER_WG 1
)"
R"(#endif
)"
R"(#ifndef USE_QCOM_SUBGROUP_SHUFFLE
)"
R"(#define USE_QCOM_SUBGROUP_SHUFFLE 0
)"
R"(#endif
)"
R"(
)"
R"(#define WG_SIZE             (SUBGROUP_SIZE * SUBGROUPS_PER_WG)
)"
R"(#define LANE_GROUPS_PER_SG  (SUBGROUP_SIZE / LANES_PER_COLUMN)
)"
R"(#define COLS_PER_SG         (LANE_GROUPS_PER_SG * COLS_PER_LANE_GROUP)
)"
R"(#define COLS_PER_WG         (SUBGROUPS_PER_WG * COLS_PER_SG)
)"
R"(#define ROWS_PER_LANE       (S_V / LANES_PER_COLUMN)
)"
R"(
)"
R"(#if USE_QCOM_SUBGROUP_SHUFFLE
)"
R"(#pragma OPENCL EXTENSION cl_qcom_subgroup_shuffle : enable
)"
R"(#endif
)"
R"(
)"
R"(// XOR-based parallel sum
)"
R"(// This does a reduction across groups of LANES_PER_COLUMN
)"
R"(static inline float reduce_add_shmem(float partial, __local float * temp, uint lane) {
)"
R"(#if USE_QCOM_SUBGROUP_SHUFFLE
)"
R"(   #pragma unroll
)"
R"(    for (uint s = LANES_PER_COLUMN / 2u; s > 0u; s >>= 1u) {
)"
R"(        partial += qcom_sub_group_shuffle_xor(partial, s, CLK_SUB_GROUP_SHUFFLE_WIDTH_WAVE_SIZE_QCOM, partial);
)"
R"(    }
)"
R"(    return partial;
)"
R"(#else
)"
R"(    temp[lane] = partial;
)"
R"(    sub_group_barrier(CLK_LOCAL_MEM_FENCE);
)"
R"(    #pragma unroll
)"
R"(    for (uint s = LANES_PER_COLUMN / 2u; s > 0u; s >>= 1u) {
)"
R"(        float other = temp[lane ^ s];
)"
R"(        sub_group_barrier(CLK_LOCAL_MEM_FENCE);
)"
R"(        temp[lane] += other;
)"
R"(        sub_group_barrier(CLK_LOCAL_MEM_FENCE);
)"
R"(    }
)"
R"(    const float result = temp[lane];
)"
R"(    sub_group_barrier(CLK_LOCAL_MEM_FENCE);
)"
R"(    return result;
)"
R"(#endif
)"
R"(}
)"
R"(
)"
R"(#define REDUCE_PARTIAL(partial, temp_ptr, lid) \
)"
R"(    ((LANES_PER_COLUMN == 1u) ? (partial) : reduce_add_shmem((partial), (temp_ptr), (lid)))
)"
R"(
)"
R"(// force compiler to optimize kernel for a specific fixed work-group size
)"
R"(__attribute__((reqd_work_group_size(WG_SIZE, 1, 1)))
)"
R"(#ifdef INTEL_GPU
)"
R"(REQD_SUBGROUP_SIZE_32
)"
R"(#elif defined (ADRENO_GPU)
)"
R"(REQD_SUBGROUP_SIZE_64
)"
R"(#endif
)"
R"(kernel void kernel_gated_delta_net(
)"
R"(        global const char * q_buf,     ulong off_q,
)"
R"(        global const char * k_buf,     ulong off_k,
)"
R"(        global const char * v_buf,     ulong off_v,
)"
R"(        global const char * g_buf,     ulong off_g,
)"
R"(        global const char * beta_buf,  ulong off_beta,
)"
R"(        global const char * state_buf, ulong off_state,
)"
R"(        global       char * dst_buf,   ulong off_dst,
)"
R"(        uint  H_v,
)"
R"(        uint  n_tokens,
)"
R"(        uint  n_seqs,
)"
R"(        uint  s_off,
)"
R"(        uint  sq1, uint sq2, uint sq3,
)"
R"(        uint  sv1, uint sv2, uint sv3,
)"
R"(        uint  sb1, uint sb2, uint sb3,
)"
R"(        uint  H_k,
)"
R"(        uint  rq3,
)"
R"(        float scale,
)"
R"(        uint K) {
)"
R"(
)"
R"(    global const float * data_q     = (global const float *)(q_buf     + off_q);
)"
R"(    global const float * data_k     = (global const float *)(k_buf     + off_k);
)"
R"(    global const float * data_v     = (global const float *)(v_buf     + off_v);
)"
R"(    global const float * data_g     = (global const float *)(g_buf     + off_g);
)"
R"(    global const float * data_beta  = (global const float *)(beta_buf  + off_beta);
)"
R"(    global const float * data_state = (global const float *)(state_buf + off_state);
)"
R"(    global       float * data_dst   = (global       float *)(dst_buf   + off_dst);
)"
R"(
)"
R"(    const uint head_id     = get_group_id(0);
)"
R"(    const uint seq_id      = get_group_id(1);
)"
R"(    const uint tid         = (uint)get_local_id(0);
)"
R"(
)"
R"(    const uint sg_id       = get_sub_group_id(); // subgroup id
)"
R"(    const uint sg_lid      = get_sub_group_local_id(); // subgroup lane id
)"
R"(
)"
R"(    const uint lane        = sg_lid % LANES_PER_COLUMN;
)"
R"(    const uint lane_group  = sg_lid / LANES_PER_COLUMN;
)"
R"(    const uint wg_col_base = get_group_id(2) * COLS_PER_WG;
)"
R"(    const uint sg_col_base = wg_col_base + sg_id * COLS_PER_SG;
)"
R"(
)"
R"(    const uint iq1 = head_id % H_k; // head index for Q and K
)"
R"(    const uint iq3 = seq_id / rq3; // seq index for Q and K
)"
R"(
)"
R"(    const uint state_size = S_V * S_V;
)"
R"(    const uint state_base = (seq_id * K * H_v + head_id) * state_size;
)"
R"(    const uint q_off_base  = iq3 * sq3 + iq1 * sq1;
)"
R"(    const uint v_off_base  = seq_id * sv3 + head_id * sv1;
)"
R"(    const uint gb_off_base = seq_id * sb3 + head_id * sb1;
)"
R"(    const uint state_out_base      = (seq_id * H_v + head_id) * state_size;
)"
R"(    const uint state_size_per_snap = state_size * H_v * n_seqs;
)"
R"(
)"
R"(    __local float reduce_temp[WG_SIZE];
)"
R"(    __local float * temp_ptr = reduce_temp + sg_id * SUBGROUP_SIZE;
)"
R"(
)"
R"(    float s_shard[COLS_PER_LANE_GROUP][ROWS_PER_LANE];
)"
R"(    #pragma unroll
)"
R"(    for (uint cg = 0; cg < COLS_PER_LANE_GROUP; cg++) {
)"
R"(        const uint col = sg_col_base + cg * LANE_GROUPS_PER_SG + lane_group;
)"
R"(        #pragma unroll
)"
R"(        for (uint r = 0; r < ROWS_PER_LANE; r++) {
)"
R"(            s_shard[cg][r] = data_state[state_base + col * S_V + r * LANES_PER_COLUMN + lane];
)"
R"(        }
)"
R"(    }
)"
R"(
)"
R"(    const int shift = (int)n_tokens - (int)K;
)"
R"(    uint attn_off = (seq_id * n_tokens * H_v + head_id) * S_V;
)"
R"(
)"
R"(    for (uint t = 0; t < n_tokens; t++) {
)"
R"(        const uint  q_off    = q_off_base + t * sq2;
)"
R"(        const uint  k_off    = q_off;
)"
R"(        const uint  v_off    = v_off_base + t * sv2;
)"
R"(        const uint  gb_off   = gb_off_base + t * sb2;
)"
R"(        const float beta_val = data_beta[gb_off];
)"
R"(
)"
R"(        float k_reg[ROWS_PER_LANE];
)"
R"(        float q_reg[ROWS_PER_LANE];
)"
R"(#if KDA
)"
R"(        float g_exp[ROWS_PER_LANE];
)"
R"(        #pragma unroll
)"
R"(        for (uint r = 0; r < ROWS_PER_LANE; r++) {
)"
R"(            const uint i = r * LANES_PER_COLUMN + lane;
)"
R"(            k_reg[r] = data_k[k_off + i];
)"
R"(            q_reg[r] = data_q[q_off + i];
)"
R"(            g_exp[r] = exp(data_g[gb_off * S_V + i]);
)"
R"(        }
)"
R"(#else
)"
R"(        const float g_val = exp(data_g[gb_off]);
)"
R"(
)"
R"(        #pragma unroll
)"
R"(        for (uint r = 0; r < ROWS_PER_LANE; r++) {
)"
R"(            const uint i = r * LANES_PER_COLUMN + lane;
)"
R"(            k_reg[r] = data_k[k_off + i];
)"
R"(            q_reg[r] = data_q[q_off + i];
)"
R"(        }
)"
R"(#endif
)"
R"(
)"
R"(        #pragma unroll
)"
R"(        for (uint cg = 0; cg < COLS_PER_LANE_GROUP; cg++) {
)"
R"(            const uint col = sg_col_base + cg * LANE_GROUPS_PER_SG + lane_group;
)"
R"(            float v_val = data_v[v_off + col];
)"
R"(
)"
R"(            float kv_shard = 0.0f;
)"
R"(            #pragma unroll
)"
R"(            for (uint r = 0; r < ROWS_PER_LANE; r++) {
)"
R"(#if KDA
)"
R"(                float gs = g_exp[r] * s_shard[cg][r];
)"
R"(                kv_shard += gs * k_reg[r];
)"
R"(#else
)"
R"(                kv_shard += s_shard[cg][r] * k_reg[r];
)"
R"(#endif
)"
R"(            }
)"
R"(
)"
R"(#if !KDA
)"
R"(            kv_shard *= g_val; // Applied once instead of ROWS_PER_LANE times
)"
R"(#endif
)"
R"(
)"
R"(            const float kv_col = REDUCE_PARTIAL(kv_shard, temp_ptr, sg_lid);
)"
R"(
)"
R"(            const float delta_col = (v_val - kv_col) * beta_val;
)"
R"(
)"
R"(            float attn_partial = 0.0f;
)"
R"(            #pragma unroll
)"
R"(            for (uint r = 0; r < ROWS_PER_LANE; r++) {
)"
R"(#if KDA
)"
R"(                float gs = g_exp[r] * s_shard[cg][r];
)"
R"(#else
)"
R"(                float gs = g_val * s_shard[cg][r];
)"
R"(#endif
)"
R"(                s_shard[cg][r] = gs + k_reg[r] * delta_col;
)"
R"(                attn_partial += s_shard[cg][r] * q_reg[r];
)"
R"(            }
)"
R"(            const float attn_col = REDUCE_PARTIAL(attn_partial, temp_ptr, sg_lid);
)"
R"(
)"
R"(            if (lane == 0) {
)"
R"(                data_dst[attn_off + col] = attn_col * scale;
)"
R"(            }
)"
R"(        }
)"
R"(        attn_off += S_V * H_v;
)"
R"(
)"
R"(        if (K > 1u) {
)"
R"(            const int target_slot = (int)t - shift;
)"
R"(            if (target_slot >= 0 && target_slot < (int)K) {
)"
R"(                #pragma unroll
)"
R"(                for (uint cg = 0; cg < COLS_PER_LANE_GROUP; cg++) {
)"
R"(                    const uint col = sg_col_base + cg * LANE_GROUPS_PER_SG + lane_group;
)"
R"(                    const uint slot_base = s_off + (uint)target_slot * state_size_per_snap + state_out_base;
)"
R"(                    #pragma unroll
)"
R"(                    for (uint r = 0; r < ROWS_PER_LANE; r++) {
)"
R"(                        data_dst[slot_base + col * S_V + r * LANES_PER_COLUMN + lane] = s_shard[cg][r];
)"
R"(                    }
)"
R"(                }
)"
R"(            }
)"
R"(        }
)"
R"(    }
)"
R"(
)"
R"(    if (K == 1u) {
)"
R"(        #pragma unroll
)"
R"(        for (uint cg = 0; cg < COLS_PER_LANE_GROUP; cg++) {
)"
R"(            const uint col = sg_col_base + cg * LANE_GROUPS_PER_SG + lane_group;
)"
R"(            #pragma unroll
)"
R"(            for (uint r = 0; r < ROWS_PER_LANE; r++) {
)"
R"(                data_dst[s_off + state_base + col * S_V + r * LANES_PER_COLUMN + lane] = s_shard[cg][r];
)"
R"(            }
)"
R"(        }
)"
R"(    }
)"
R"(}
)"
