package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.bih.BIHTree;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.buffers.AsyncBufferHandler;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.mesh.attribute.Position;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class AdaptiveMesh {

    private final MeshLayout layout;
    private final Queue<LodBuffer> lods = new PriorityQueue<>();
    private final List<VertexBuffer> vertexBuffers = new ArrayList<>();
    private int vertices, instances;
    private BoundingVolume volume = new BoundingBox();
    private BIHTree collisionTree;

    public AdaptiveMesh(MeshLayout layout, int vertices, int instances) {
        this.layout = layout;
        this.vertices = vertices;
        this.instances = instances;
    }

    public void render(CommandBuffer cmd, float distance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer verts = stack.mallocLong(vertexBuffers.size());
            LongBuffer offsets = stack.mallocLong(vertexBuffers.size());
            for (VertexBinding b : layout) {
                VertexBuffer vb = getVertexBuffer(b);
                if (vb == null) {
                    vb = createVertexBuffer(b, b.createBuffer(getElementsOf(b.getInputRate())));
                }
                verts.put(vb.getData().getBuffer().getId());
                offsets.put(vb.getBinding().getOffset());
            }
            verts.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts, offsets);
        }
        if (!lods.isEmpty()) {
            LodBuffer lod = selectLod(distance);
            vkCmdBindIndexBuffer(cmd.getBuffer(), lod.getId(), 0, IndexType.of(lod).getEnum());
            vkCmdDrawIndexed(cmd.getBuffer(), lod.size().getElements(), instances, 0, 0, 0);
        } else {
            vkCmdDraw(cmd.getBuffer(), vertices, instances, 0, 0);
        }
    }

    public <T extends Attribute> T mapAttribute(String name) {
        return layout.mapAttribute(this, name);
    }

    public <T extends Attribute> T mapAttribute(GlVertexBuffer.Type name) {
        return layout.mapAttribute(this, name);
    }

    public void addLod(LodBuffer lod) {
        lods.add(lod);
    }

    public VertexBuffer createVertexBuffer(VertexBinding binding, GpuBuffer buffer) {
        VertexBuffer vb = new VertexBuffer(binding, buffer);
        vertexBuffers.add(vb);
        return vb;
    }

    public VertexBuffer getBuffer(int binding) {
        return vertexBuffers.get(binding);
    }

    public VertexBuffer getVertexBuffer(VertexBinding binding) {
        return vertexBuffers.stream().filter(vb -> vb.getBinding() == binding).findAny().orElse(null);
    }

    public void computeBounds(BoundingVolume bounds) {
        Position pos = mapAttribute(GlVertexBuffer.Type.Position);
        volume.computeFromPoints(pos);
        pos.unmap();
    }

    public void setVertices(int vertices) {
        if (this.vertices != vertices) {
            this.vertices = vertices;
            for (VertexBuffer vb : vertexBuffers) {
                if (vb.getBinding().getInputRate().is(InputRate.Vertex)) {
                    vb.buffer.getBuffer().resize(vertices);
                }
            }
        }
    }

    public void setInstances(int instances) {
        if (this.instances != instances) {
            this.instances = instances;
            for (VertexBuffer vb : vertexBuffers) {
                if (vb.getBinding().getInputRate().is(InputRate.Instance)) {
                    vb.buffer.getBuffer().resize(instances);
                }
            }
        }
    }

    public MeshLayout getLayout() {
        return layout;
    }

    public int getVertices() {
        return vertices;
    }

    public int getInstances() {
        return instances;
    }

    protected LodBuffer selectLod(float distance) {
        LodBuffer usable = lods.peek();
        for (LodBuffer l : lods) {
            if (distance < l.getOptimalDistance()) {
                break;
            }
            usable = l;
        }
        return usable;
    }

    public int getElementsOf(IntEnum<InputRate> rate) {
        return rate.is(InputRate.Instance) ? instances : vertices;
    }

    public static class VertexBuffer {

        private final VertexBinding binding;
        private final AsyncBufferHandler<GpuBuffer> buffer = new AsyncBufferHandler<>();

        private VertexBuffer(VertexBinding binding, GpuBuffer buffer) {
            this.binding = binding;
            this.buffer.setBuffer(buffer);
        }

        public VertexBinding getBinding() {
            return binding;
        }

        public AsyncBufferHandler<GpuBuffer> getData() {
            return buffer;
        }

    }

}
