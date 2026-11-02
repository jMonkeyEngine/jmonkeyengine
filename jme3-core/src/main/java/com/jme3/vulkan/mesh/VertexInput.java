package com.jme3.vulkan.mesh;

import com.jme3.scene.Mesh;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructArray;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.VulkanEnums;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class VertexInput {

    private final Map<Integer, BindingEntry> bindings = new HashMap<>();
    private final Map<Integer, AttributeEntry> attributes = new HashMap<>();
    private Integer hashcode;

    private VertexInput() {}

    /**
     * Gets a vertex input layout with no bindings or attributes at all, for a technique
     * that reads vertex data itself (e.g. vertex pulling via a storage buffer or buffer
     * device address) instead of relying on the fixed-function vertex input stage.
     *
     * @return an empty vertex input layout
     */
    public static VertexInput empty() {
        return new VertexInput();
    }

    /**
     * Builds the vertex input layout a {@link com.jme3.vulkan.pipeline.graphics.GraphicsPipeline}
     * needs to render {@code mesh}: one binding per vertex/instance buffer that contributes
     * at least one attribute {@code attributeLocation} resolves, and one attribute entry per
     * format component of each attribute it contributes. Instance buffers are bound after
     * vertex buffers, at consecutive binding indices.
     *
     * @param attributeLocation maps an attribute name (e.g. one of
     *                          {@link com.jme3.vulkan.mesh.attributes.CommonAttributes}) to
     *                          the shader location it occupies, or null if the shading
     *                          technique's shader doesn't consume that attribute at all
     * @param mesh mesh to read vertex/instance buffers from
     */
    public VertexInput(Function<String, Integer> attributeLocation, Mesh mesh) {
        BitSet filledAttrLoc = new BitSet();
        int bindingIndex = addBuffers(attributeLocation, mesh.getVertexBuffers(), InputRate.Vertex, filledAttrLoc, 0);
        addBuffers(attributeLocation, mesh.getInstanceBuffers(), InputRate.Instance, filledAttrLoc, bindingIndex);
    }

    /**
     * Gets the vertex and instance buffers of {@code mesh} that the layout built by
     * {@link #VertexInput(Function, Mesh)} gives a binding, in binding index order. Buffers that
     * contribute no attribute the shader consumes get no binding and are left out.
     *
     * @param attributeLocation maps an attribute name to the shader location it occupies, or null
     * @param mesh mesh to read vertex/instance buffers from
     * @return buffers to bind, where element {@code i} belongs at binding index {@code i}
     */
    @SuppressWarnings("unchecked")
    public static List<StructArray<? extends Struct<VertexAttr>>> getReferencedBuffers(Function<String, Integer> attributeLocation, Mesh mesh) {
        List<StructArray<? extends Struct<VertexAttr>>> bound = new ArrayList<>();
        BitSet filledAttrLoc = new BitSet();
        for (Collection<StructArray<? extends Struct<VertexAttr>>> buffers : Arrays.asList(mesh.getVertexBuffers(), mesh.getInstanceBuffers())) {
            for (StructArray vb : buffers) {
                if (claimLocations(attributeLocation, vb.getStruct(), filledAttrLoc, null)) {
                    bound.add(vb);
                }
            }
        }
        return bound;
    }

    /**
     * Claims the locations of every attribute of {@code struct} that the shader consumes and no
     * earlier buffer already filled. Shared by the layout and {@link #getReferencedBuffers} so both
     * select the same buffers.
     *
     * @param claimed receives each claimed attribute and its first location, or null
     * @return true if any attribute was claimed, i.e. the buffer gets a binding
     */
    private static boolean claimLocations(Function<String, Integer> attributeLocation, Struct<VertexAttr> struct,
                                          BitSet filledAttrLoc, BiConsumer<VertexAttr, Integer> claimed) {
        boolean anyValid = false;
        for (VertexAttr attr : struct.getFields()) {
            Integer loc = attributeLocation.apply(attr.getName());
            if (loc == null || filledAttrLoc.get(loc)) {
                continue;
            }
            Format[] formats = attr.getFormats();
            assert formats.length > 0 : "Vertex attribute must specify at least one format.";
            for (int i = 0; i < formats.length; i++) {
                if (i > 0 && filledAttrLoc.get(loc + i)) {
                    throw new IllegalStateException("Overlapping attributes at " + (loc + i));
                }
                filledAttrLoc.set(loc + i);
            }
            anyValid = true;
            if (claimed != null) {
                claimed.accept(attr, loc);
            }
        }
        return anyValid;
    }

    @SuppressWarnings("unchecked")
    private int addBuffers(Function<String, Integer> attributeLocation,
                           Collection<StructArray<? extends Struct<VertexAttr>>> buffers,
                           InputRate rate, BitSet filledAttrLoc, int bindingIndex) {
        for (StructArray vb : buffers) {
            Struct<VertexAttr> struct = vb.getStruct();
            int binding = bindingIndex;
            boolean anyValid = claimLocations(attributeLocation, struct, filledAttrLoc, (attr, loc) -> {
                int offset = struct.getFieldOffset(attr);
                Format[] formats = attr.getFormats();
                for (int i = 0; i < formats.length; i++) {
                    add(new AttributeEntry(loc + i, binding, offset, formats[i]));
                    offset += formats[i].getBytes();
                }
            });
            if (anyValid) {
                add(new BindingEntry(bindingIndex++, vb.stride(), rate));
            }
        }
        return bindingIndex;
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

    public VkVertexInputBindingDescription.Buffer getBindings(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer buffer = VkVertexInputBindingDescription.malloc(bindings.size(), stack);
        for (BindingEntry b : bindings.values()) {
            b.fill(buffer.get());
        }
        return buffer.flip();
    }

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
