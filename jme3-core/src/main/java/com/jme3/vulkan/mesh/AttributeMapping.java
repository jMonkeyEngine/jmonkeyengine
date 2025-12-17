package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.mesh.attribute.Attribute;

public interface AttributeMapping <T extends Attribute> {

    T map(VertexBinding binding, GpuBuffer vertices, int size, int offset);

}
