R"(kernel void kernel_concat_f32(
)"
R"(    global  const char * src0,
)"
R"(    ulong                offset0,
)"
R"(    global  const char * src1,
)"
R"(    ulong                offset1,
)"
R"(    global        char * dst,
)"
R"(    ulong                offsetd,
)"
R"(    int             ne00,
)"
R"(    int             ne01,
)"
R"(    int             ne02,
)"
R"(    int             ne03,
)"
R"(    ulong           nb00,
)"
R"(    ulong           nb01,
)"
R"(    ulong           nb02,
)"
R"(    ulong           nb03,
)"
R"(    ulong           nb10,
)"
R"(    ulong           nb11,
)"
R"(    ulong           nb12,
)"
R"(    ulong           nb13,
)"
R"(    int             ne0,
)"
R"(    ulong           nb0,
)"
R"(    ulong           nb1,
)"
R"(    ulong           nb2,
)"
R"(    ulong           nb3,
)"
R"(    int             dim
)"
R"() {
)"
R"(    src0 = src0 + offset0;
)"
R"(    src1 = src1 + offset1;
)"
R"(    dst  = dst  + offsetd;
)"
R"(
)"
R"(    const int i3 = get_group_id(2);
)"
R"(    const int i2 = get_group_id(1);
)"
R"(    const int i1 = get_group_id(0);
)"
R"(
)"
R"(    int o[4] = {0, 0, 0, 0};
)"
R"(    o[dim] = dim == 0 ? ne00 : (dim == 1 ? ne01 : (dim == 2 ? ne02 : ne03));
)"
R"(
)"
R"(    global const float * x;
)"
R"(
)"
R"(    for (int i0 = get_local_id(0); i0 < ne0; i0 += get_local_size(0)) {
)"
R"(        if (i0 < ne00 && i1 < ne01 && i2 < ne02 && i3 < ne03) {
)"
R"(            x = (global const float *)(src0 + (i3       )*nb03 + (i2       )*nb02 + (i1       )*nb01 + (i0       )*nb00);
)"
R"(        } else {
)"
R"(            x = (global const float *)(src1 + (i3 - o[3])*nb13 + (i2 - o[2])*nb12 + (i1 - o[1])*nb11 + (i0 - o[0])*nb10);
)"
R"(        }
)"
R"(
)"
R"(        global float * y = (global float *)(dst + i3*nb3 + i2*nb2 + i1*nb1 + i0*nb0);
)"
R"(
)"
R"(        *y = *x;
)"
R"(    }
)"
R"(}
)"
R"(
)"
R"(kernel void kernel_concat_f32_pack(
)"
R"(    global  const char * src0,
)"
R"(    ulong                offset0,
)"
R"(    global  const char * src1,
)"
R"(    ulong                offset1,
)"
R"(    global        char * dst,
)"
R"(    ulong                offsetd,
)"
R"(    int             ne00,
)"
R"(    int             ne01,
)"
R"(    int             ne02,
)"
R"(    int             ne03,
)"
R"(    ulong           nb00,
)"
R"(    ulong           nb01,
)"
R"(    ulong           nb02,
)"
R"(    ulong           nb03,
)"
R"(    ulong           nb10,
)"
R"(    ulong           nb11,
)"
R"(    ulong           nb12,
)"
R"(    ulong           nb13,
)"
R"(    int             ne0,
)"
R"(    ulong           nb0,
)"
R"(    ulong           nb1,
)"
R"(    ulong           nb2,
)"
R"(    ulong           nb3,
)"
R"(    int             dim,
)"
R"(    int             ne1,
)"
R"(    int             ne2,
)"
R"(    int             ne3
)"
R"() {
)"
R"(    src0 = src0 + offset0;
)"
R"(    src1 = src1 + offset1;
)"
R"(    dst  = dst  + offsetd;
)"
R"(
)"
R"(    int lsz = get_local_size(0);
)"
R"(    int tpr = min(ne0, lsz);          // threads per row
)"
R"(    int rpw = lsz / tpr;              // rows per workgroup
)"
R"(    int lid = get_local_id(0);
)"
R"(    int row = get_group_id(0)*rpw + lid / tpr;
)"
R"(    int lane = lid - (lid / tpr) * tpr;
)"
R"(
)"
R"(    int nrows = ne1*ne2*ne3;
)"
R"(    if (row >= nrows) {
)"
R"(        return;
)"
R"(    }
)"
R"(
)"
R"(    int i1 = row % ne1;
)"
R"(    int t  = row / ne1;
)"
R"(    int i2 = t % ne2;
)"
R"(    int i3 = t / ne2;
)"
R"(
)"
R"(    int o[4] = {0, 0, 0, 0};
)"
R"(    o[dim] = dim == 0 ? ne00 : (dim == 1 ? ne01 : (dim == 2 ? ne02 : ne03));
)"
R"(
)"
R"(    for (int i0 = lane; i0 < ne0; i0 += tpr) {
)"
R"(        global const float * x;
)"
R"(        if (i0 < ne00 && i1 < ne01 && i2 < ne02 && i3 < ne03) {
)"
R"(            x = (global const float *)(src0 + (i3       )*nb03 + (i2       )*nb02 + (i1       )*nb01 + (i0       )*nb00);
)"
R"(        } else {
)"
R"(            x = (global const float *)(src1 + (i3 - o[3])*nb13 + (i2 - o[2])*nb12 + (i1 - o[1])*nb11 + (i0 - o[0])*nb10);
)"
R"(        }
)"
R"(
)"
R"(        global float * y = (global float *)(dst + i3*nb3 + i2*nb2 + i1*nb1 + i0*nb0);
)"
R"(
)"
R"(        *y = *x;
)"
R"(    }
)"
R"(}
)"
