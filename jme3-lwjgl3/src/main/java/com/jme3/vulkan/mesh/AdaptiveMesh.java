package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public abstract class AdaptiveMesh implements Mesh {

    protected enum VertexMode {

        Stream, Dynamic, Static;

    }

    protected final MeshDescription description;
    protected VersionedResource<? extends GpuBuffer> indexBuffer;
    private final List<VertexBuffer> vertexBuffers = new ArrayList<>();
    private int vertices;

    public AdaptiveMesh(MeshDescription description) {
        this.description = description;
    }

    @Override
    public void bind(CommandBuffer cmd) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer verts = stack.mallocLong(vertexBuffers.size());
            LongBuffer offsets = stack.mallocLong(vertexBuffers.size());
            for (VertexBuffer vb : vertexBuffers) {
                verts.put(vb.getResource().get().getId());
                offsets.put(0L);
            }
            verts.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts, offsets);
        }
        if (indexBuffer != null) {
            vkCmdBindIndexBuffer(cmd.getBuffer(), indexBuffer.get().getId(), 0, IndexType.of(indexBuffer.get()).getEnum());
        }
    }

    @Override
    public void draw(CommandBuffer cmd) {
        if (indexBuffer != null) {
            vkCmdDrawIndexed(cmd.getBuffer(), indexBuffer.get().size().getElements(), 1, 0, 0, 0);
        } else {
            vkCmdDraw(cmd.getBuffer(), vertices, 1, 0, 0);
        }
    }

    public int getNumVertices() {
        return vertices;
    }

    @SuppressWarnings("resource")
    protected AttributeModifier modifyAttribute(String attribute) {
        VertexAttribute attr = description.getAttribute(attribute);
        if (attr != null) {
            return new AttributeModifier(vertexBuffers.get(attr.getBinding().getBindingIndex()), attr).map();
        } else {
            return new NullAttributeModifier().map();
        }
    }

    protected AttributeModifier modifyAttribute(BuiltInAttribute name) {
        return modifyAttribute(name.getName());
    }

    protected abstract VersionedResource<? extends GpuBuffer> createStreamingBuffer(MemorySize size);

    protected abstract VersionedResource<? extends GpuBuffer> createDynamicBuffer(MemorySize size);

    protected abstract VersionedResource<? extends GpuBuffer> createStaticBuffer(MemorySize size);

    protected Builder build(int vertices) {
        return new Builder(vertices);
    }

    protected class Builder implements AutoCloseable {

        private final Map<String, AttributeInfo> attributes = new HashMap<>();

        private Builder(int vertices) {
            AdaptiveMesh.this.vertices = vertices;
            for (VertexBinding b : description) {
                for (VertexAttribute a : b) {
                    attributes.put(a.getName(), new AttributeInfo());
                }
            }
        }

        @Override
        public void close() {
            VertexMode[] modes = new VertexMode[description.getBindings().size()];
            for (Map.Entry<String, AttributeInfo> e : attributes.entrySet()) {
                int i = description.getAttribute(e.getKey()).getBinding().getBindingIndex();
                VertexMode mode = modes[i];
                if (mode == null || e.getValue().getMode().ordinal() < mode.ordinal()) {
                    modes[i] = e.getValue().getMode();
                }
            }
            for (int i = 0; i < modes.length; i++) {
                vertexBuffers.add(new VertexBuffer(createVertexBuffer(description.getBinding(i), modes[i])));
            }
        }

        public void setMode(String name, VertexMode mode) {
            AttributeInfo attr = attributes.get(name);
            if (attr != null) {
                attr.setMode(mode);
            }
        }

        public void setMode(BuiltInAttribute name, VertexMode mode) {
            setMode(name.getName(), mode);
        }

        private VersionedResource<? extends GpuBuffer> createVertexBuffer(VertexBinding binding, VertexMode mode) {
            MemorySize size = MemorySize.bytes(binding.getStride() * vertices);
            switch (mode) {
                case Stream: return createStreamingBuffer(size);
                case Dynamic: return createDynamicBuffer(size);
                case Static: return createStaticBuffer(size);
                default: throw new UnsupportedOperationException();
            }
        }

    }

    /**
     * Contains information about an attribute with the purpose
     * of generating vertex buffers from that information.
     *
     * <p>This could technically be collapsed into VertexMode, but there
     * may be other properties in the future that could be set.</p>
     */
    private static class AttributeInfo {

        private VertexMode mode = VertexMode.Static;

        public void setMode(VertexMode mode) {
            this.mode = mode;
        }

        public VertexMode getMode() {
            return mode;
        }

    }

}
