package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.bih.BIHTree;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.buffers.MultiMappingBuffer;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.pipeline.VertexPipeline;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class AdaptiveMesh implements VulkanMesh, Iterable<AdaptiveMesh.VertexBuffer>  {

    private final MeshLayout layout;
    private final Collection<LodBuffer> lods = new ArrayList<>(1);
    private final List<VertexBuffer> vertexBuffers = new ArrayList<>();
    private LodBuffer currentLod;
    private final int vertexCapacity;
    private final int instanceCapacity;
    private int vertices, instances;
    private final BoundingVolume volume = new BoundingBox();
    private BIHTree collisionTree;

    public AdaptiveMesh(MeshLayout layout, int vertices, int instances) {
        this.layout = layout;
        this.vertices = this.vertexCapacity = vertices;
        this.instances = this.instanceCapacity = instances;
    }

    @Override
    public void render(CommandBuffer cmd, VertexPipeline pipeline) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer verts = stack.mallocLong(vertexBuffers.size());
            LongBuffer offsets = stack.mallocLong(vertexBuffers.size());
            for (VertexBinding b : layout) {
                if (!b.bindOnPipeline(pipeline)) {
                    continue;
                }
                VertexBuffer vb = getVertexBuffer(b);
                verts.put(vb.getId());
                offsets.put(vb.getBinding().getOffset());
            }
            verts.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts, offsets);
        }
        if (currentLod != null) {
            vkCmdBindIndexBuffer(cmd.getBuffer(), currentLod.getId(), 0, IndexType.of(currentLod).getEnum());
            vkCmdDrawIndexed(cmd.getBuffer(), currentLod.size().getElements(), instances, 0, 0, 0);
        } else {
            vkCmdDraw(cmd.getBuffer(), vertices, instances, 0, 0);
        }
    }

    @Override
    public <T extends Attribute> T mapAttribute(String name) {
        return layout.mapAttribute(this, name);
    }

    @Override
    public <T extends Attribute> T mapAttribute(GlVertexBuffer.Type name) {
        return layout.mapAttribute(this, name.name());
    }

    @Override
    public void addLevelOfDetail(LodBuffer lod) {
        lods.add(lod);
    }

    @Override
    public LodBuffer selectLevelOfDetail(Comparator<LodBuffer> selector) {
        currentLod = null;
        for (LodBuffer l : lods) {
            if (currentLod == null || selector.compare(l, currentLod) > 0) {
                currentLod = l;
            }
        }
        return currentLod;
    }

    @Override
    public VertexBuffer getVertexBuffer(VertexBinding binding) {
        VertexBuffer vb = vertexBuffers.stream().filter(v -> v.getBinding() == binding).findAny().orElse(null);
        if (vb == null) {
            vb = new VertexBuffer(binding, binding.createBuffer(getCapacity(binding.getInputRate())));
            vertexBuffers.add(vb);
        }
        return vb;
    }

    @Override
    public int setElements(IntEnum<InputRate> rate, int elements) {
        if (rate.is(InputRate.Vertex)) return vertices = Math.min(elements, vertexCapacity);
        if (rate.is(InputRate.Instance)) return instances = Math.min(elements, instanceCapacity);
        throw new IllegalArgumentException("Input rate enum \"" + rate + "\" is not supported.");
    }

    @Override
    public void pushElements(IntEnum<InputRate> rate, int baseElement, int elements) {
        for (VertexBuffer vb : vertexBuffers) {
            if (vb.getBinding().getInputRate().is(rate)) {
                vb.push((int)vb.getBinding().getOffset() + baseElement * vb.getBinding().getStride(), elements * vb.getBinding().getStride());
            }
        }
    }

    @Override
    public void pushElements(IntEnum<InputRate> rate) {
        pushElements(rate, 0, getElements(rate));
    }

    public MeshLayout getLayout() {
        return layout;
    }

    @Override
    public int getElements(IntEnum<InputRate> rate) {
        if (rate.is(InputRate.Vertex)) return vertices;
        if (rate.is(InputRate.Instance)) return instances;
        return 0;
    }

    @Override
    public int getCapacity(IntEnum<InputRate> rate) {
        if (rate.is(InputRate.Vertex)) return vertexCapacity;
        if (rate.is(InputRate.Instance)) return instanceCapacity;
        return 0;
    }

    @Override
    public Iterator<VertexBuffer> iterator() {
        return vertexBuffers.iterator();
    }

    public static class VertexBuffer implements VulkanBuffer {

        private final VertexBinding binding;
        private final MultiMappingBuffer<VulkanBuffer> buffer = new MultiMappingBuffer<>();

        private VertexBuffer(VertexBinding binding, GpuBuffer buffer) {
            this.binding = binding;
            this.buffer.setBuffer((VulkanBuffer)buffer);
        }

        public VertexBinding getBinding() {
            return binding;
        }

        @Override
        public LogicalDevice<?> getDevice() {
            return buffer.getBuffer().getDevice();
        }

        @Override
        public Flag<BufferUsage> getUsage() {
            return buffer.getBuffer().getUsage();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return buffer.getBuffer().getMemoryProperties();
        }

        @Override
        public boolean isConcurrent() {
            return buffer.getBuffer().isConcurrent();
        }

        @Override
        public PointerBuffer map(int offset, int size) {
            return buffer.map(offset, size);
        }

        @Override
        public void push(int offset, int size) {
            buffer.push(offset, size);
        }

        @Override
        public long getId() {
            return buffer.getId();
        }

        @Override
        public void unmap() {
            buffer.unmap();
        }

        @Override
        public MemorySize size() {
            return buffer.size();
        }

    }

}
