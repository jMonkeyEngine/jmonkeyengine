package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.util.LibEnum;

import java.nio.ByteBuffer;

public class VertexBinding {

    private final int binding;
    private final int stride;
    private final LibEnum<InputRate> rate;

    public VertexBinding(int binding, int stride, LibEnum<InputRate> rate) {
        this.binding = binding;
        this.stride = stride;
        this.rate = rate;
    }

    public int getBinding() {
        return binding;
    }

    public int getStride() {
        return stride;
    }

    public LibEnum<InputRate> getRate() {
        return rate;
    }

}
