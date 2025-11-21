package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.buffers.AdaptiveVulkanBuffer;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;

public class VulkanVertexBinding implements VertexBinding {

    private final LogicalDevice<?> device;
    private final List<NamedAttribute> attributes = new ArrayList<>();
    private final IntEnum<InputRate> rate;
    private int stride = 0;
    private long offset = 0L;

    protected VulkanVertexBinding(LogicalDevice<?> device, IntEnum<InputRate> rate) {
        this.device = device;
        this.rate = rate;
    }

    @Override
    public <T extends Attribute> T mapAttribute(String name, Mappable vertices, int size) {
        for (NamedAttribute a : attributes) {
            if (a.getName().equals(name)) {
                return (T)a.getMapping().map(vertices, size, getStride(), a.getOffset());
            }
        }
        return null;
    }

    @Override
    public GpuBuffer createBuffer(int elements) {
        return new AdaptiveVulkanBuffer(device, new MemorySize(elements, stride));
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
    public int getNumAttributes() {
        return attributes.size();
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Iterator<NamedAttribute> iterator() {
        return attributes.iterator();
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

    public static BuilderImpl create(LogicalDevice<?> device, IntEnum<InputRate> rate) {
        return new VulkanVertexBinding(device, rate).build();
    }

    private BuilderImpl build() {
        return new BuilderImpl();
    }

    public class BuilderImpl implements VertexBinding.Builder {

        private final Set<String> usedAttrNames = new HashSet<>();

        @Override
        public BuilderImpl add(String name, Format format, AttributeMapping mapping) {
            if (!usedAttrNames.add(name)) {
                throw new IllegalArgumentException("\"" + name + "\" is already used as an attribute name.");
            }
            attributes.add(new NamedAttribute(name, format, mapping, stride));
            stride += format.getTotalBytes();
            return this;
        }

        @Override
        public VulkanVertexBinding build() {
            return VulkanVertexBinding.this;
        }

    }

}
