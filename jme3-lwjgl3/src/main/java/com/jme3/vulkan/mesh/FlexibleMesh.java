package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.BufferMode;
import com.jme3.vulkan.buffers.GpuBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlexibleMesh {

    protected final MeshDescription description;
    protected final List<GpuBuffer> buffers = new ArrayList<>();
    private final Map<String, BufferMode> attrModes = new HashMap<>();

    public FlexibleMesh(MeshDescription description) {
        this.description = description;
    }

    protected void declareAttribute(String name, BufferMode mode) {
        attrModes.put(name, mode);
    }

    public static class VertexBuffer {

        private final GpuBuffer buffer;

        public VertexBuffer(GpuBuffer buffer) {
            this.buffer = buffer;
        }

    }

}
