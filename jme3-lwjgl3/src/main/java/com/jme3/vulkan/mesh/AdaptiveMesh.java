package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingVolume;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdaptiveMesh implements Mesh {

    protected final MeshDescription description;
    protected final List<GpuBuffer> buffers = new ArrayList<>();

    public AdaptiveMesh(MeshDescription description) {
        this.description = description;
    }

    @Override
    public void bindVertexBuffers(CommandBuffer cmd) {

    }

    @Override
    public void bind(CommandBuffer cmd) {

    }

    @Override
    public void draw(CommandBuffer cmd) {

    }

    @Override
    public BoundingVolume computeBounds() {
        return null;
    }

    public static class VertexBuffer {

        //private final BufferMode hint;
        private GpuBuffer buffer;

        public VertexBuffer() {
            //this.hint = hint;
        }

        public void createBuffer() {
            //buffer = hint.createBuffer()
        }

    }

}
