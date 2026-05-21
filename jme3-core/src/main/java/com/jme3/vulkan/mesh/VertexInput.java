package com.jme3.vulkan.mesh;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.pipeline.VertexPipeline;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Function;

import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;

public class VertexInput {

    private final Map<Integer, BindingEntry> bindings = new HashMap<>();
    private final Map<Integer, AttributeEntry> attributes = new HashMap<>();
    private Integer hashcode;

    private VertexInput() {}

    public VertexInput(Function<String, Integer> attributeLocation, Collection<VertexBuffer> vertexBuffers) {
        Set<Integer> filledAttrLoc = new HashSet<>();
        int bindingIndex = 0;
        for (VertexBuffer<Struct<VertexAttr>> vb : vertexBuffers) {
            boolean anyValid = false;
            for (VertexAttr attr : vb.getStruct().getFields()) {
                Integer loc = attributeLocation.apply(attr.getName());
                if (loc == null || !filledAttrLoc.add(loc)) {
                    continue;
                }
                anyValid = true;
                int offset = attr.getOffset();
                Format[] formats = attr.getFormats();
                assert formats.length > 0 : "Vertex attribute must specify at least one format.";
                for (int i = 0; i < formats.length; i++) {
                    if (i > 0 && !filledAttrLoc.add(loc + i)) {
                        throw new IllegalStateException("Overlapping attributes at " + (loc + i));
                    }
                    add(new AttributeEntry(loc + i, bindingIndex, offset, formats[i]));
                    offset += formats[i].getBytes();
                }
            }
            if (anyValid) {
                add(new BindingEntry(bindingIndex++, vb.getStride(), vb.getRate()));
            }
        }
    }

    private void add(BindingEntry b) {
        if (bindings.putIfAbsent(b.index, b) != null) {
            throw new IllegalStateException("Failed to construct vertex input: duplicate binding index (" + b.index + ")");
        }
    }

    private void add(AttributeEntry a) {
        if (attributes.putIfAbsent(a.location, a) != null) {
            throw new IllegalStateException("Failed to construct vertex input: duplicate attribute location (" + a.location + ")");
        }
    }

    public Collection<BindingEntry> getBindings() {
        return Collections.unmodifiableCollection(bindings.values());
    }

    public Collection<AttributeEntry> getAttributes() {
        return Collections.unmodifiableCollection(attributes.values());
    }

    @Deprecated
    public VkVertexInputBindingDescription.Buffer getBindings(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer buffer = VkVertexInputBindingDescription.malloc(bindings.size(), stack);
        for (BindingEntry b : bindings.values()) {
            b.fill(buffer.get());
        }
        return buffer.flip();
    }

    @Deprecated
    public VkVertexInputAttributeDescription.Buffer getAttributes(MemoryStack stack) {
        VkVertexInputAttributeDescription.Buffer buffer = VkVertexInputAttributeDescription.malloc(attributes.size(), stack);
        for (AttributeEntry a : attributes.values()) {
            a.fill(buffer.get());
        }
        return buffer.flip();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VertexInput that = (VertexInput) o;
        return Objects.equals(bindings, that.bindings) && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        if (hashcode == null) hashcode = Objects.hash(bindings, attributes);
        return hashcode;
    }

    public static class BindingEntry {

        private final int index, stride;
        private final InputRate rate;

        protected BindingEntry(int index, int stride, InputRate rate) {
            this.index = index;
            this.stride = stride;
            this.rate = rate;
        }

        @Deprecated
        public void fill(VkVertexInputBindingDescription desc) {
            desc.set(index, stride, rate.getEnum());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            BindingEntry that = (BindingEntry) o;
            return index == that.index && stride == that.stride && rate == that.rate;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, stride, rate);
        }

        public int getIndex() {
            return index;
        }

        public int getStride() {
            return stride;
        }

        public InputRate getRate() {
            return rate;
        }

    }

    public static class AttributeEntry {

        private final int location, binding, offset;
        private final Format format;

        protected AttributeEntry(int location, int binding, int offset, Format format) {
            this.location = location;
            this.binding = binding;
            this.offset = offset;
            this.format = format;
        }

        @Deprecated
        public void fill(VkVertexInputAttributeDescription desc) {
            desc.set(location, binding, format.getEnum(VulkanEnums.instance), offset);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AttributeEntry that = (AttributeEntry) o;
            return location == that.location && binding == that.binding && offset == that.offset && format == that.format;
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, binding, offset, format);
        }

        public int getLocation() {
            return location;
        }

        public int getBinding() {
            return binding;
        }

        public int getOffset() {
            return offset;
        }

        public Format getFormat() {
            return format;
        }

    }

}
