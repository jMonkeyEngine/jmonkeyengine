package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.mesh.VertexBinding;

public class Normal extends Position {

    public Normal(VertexBinding binding, GpuBuffer vertices, int size, int offset) {
        super(binding, vertices, size, offset);
    }

}
