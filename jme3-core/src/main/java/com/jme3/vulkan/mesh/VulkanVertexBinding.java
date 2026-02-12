package com.jme3.vulkan.mesh;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.newbuf.HostVisibleBuffer;
import com.jme3.vulkan.buffers.stream.StreamingBuffer;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class VulkanVertexBinding implements VertexBinding {

    private final LogicalDevice<?> device;
    private final BufferStream stream;
    private final List<NamedAttribute> attributes = new ArrayList<>();
    private final IntEnum<InputRate> rate;
    private int stride = 0;
    private long offset = 0L;

    protected VulkanVertexBinding(LogicalDevice<?> device, BufferStream stream, IntEnum<InputRate> rate) {
        this.device = device;
        this.stream = stream;
        this.rate = rate;
    }

    @Override
    public <T extends Attribute> T mapAttribute(String name, MappableBuffer vertices, int size) {
        for (NamedAttribute a : attributes) {
            if (a.getName().equals(name)) {
                return (T)a.getMapper().apply(new AttributeMappingInfo(this, vertices, size, a.getOffset()));
            }
        }
        return null;
    }

    @Override
    public MappableBuffer createBuffer(int elements, GlVertexBuffer.Usage usage) {
        MemorySize size = new MemorySize(elements, stride);
        switch (usage) {
            case Static:
            case Dynamic: {
                StreamingBuffer buffer = new StreamingBuffer(device, new MemorySize(elements, stride), BufferUsage.Vertex);
                stream.add(buffer);
                return buffer;
            }
            case Stream: {
                return new PersistentVulkanBuffer<>(HostVisibleBuffer.build(
                        device, size, b -> b.setUsage(BufferUsage.Vertex)));
            }
            case CpuOnly: throw new IllegalArgumentException("Cannot create cpu-only vertex buffer for Vulkan.");
            default: throw new UnsupportedOperationException("Unrecognized: " + usage);
        }
    }

    @Override
    public void setOffset(long offset) {
        this.offset = offset;
    }

    @Override
    public int getStride() {
        return stride;
    }

    @Override
    public IntEnum<InputRate> getInputRate() {
        return rate;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Collection<NamedAttribute> getAttributes() {
        return Collections.unmodifiableCollection(attributes);
    }

    @Override
    public NamedAttribute getFirstAttribute() {
        return attributes.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VulkanVertexBinding that = (VulkanVertexBinding)o;
        return stride == that.stride
                && Objects.equals(attributes, that.attributes)
                && Objects.equals(rate, that.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, rate, stride);
    }

    public static VulkanVertexBinding build(LogicalDevice<?> device, BufferStream stream, IntEnum<InputRate> rate, Consumer<Builder> config) {
        BuilderImpl b = new VulkanVertexBinding(device, stream, rate).new BuilderImpl();
        config.accept(b);
        return b.build();
    }

    public class BuilderImpl implements VertexBinding.Builder {

        private final Set<String> usedAttrNames = new HashSet<>();

        @Override
        public void add(String name, Format format, Function<AttributeMappingInfo, Attribute> mapping) {
            if (!usedAttrNames.add(name)) {
                throw new IllegalArgumentException("\"" + name + "\" is already used as an attribute name.");
            }
            attributes.add(new NamedAttribute(name, format, mapping, stride));
            stride += format.getTotalBytes();
        }

        protected VulkanVertexBinding build() {
            return VulkanVertexBinding.this;
        }

    }

}
