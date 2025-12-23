package com.jme3.vulkan.mesh;

import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.pipeline.VertexPipeline;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.util.*;
import java.util.function.Consumer;

public class MeshLayout implements Iterable<VertexBinding> {

    private final List<VertexBinding> bindings = new ArrayList<>();

    protected MeshLayout() {}

    public <T extends Attribute> T mapAttribute(VulkanMesh mesh, String name) {
        for (VertexBinding vb : bindings) {
            T attr = vb.mapAttribute(name, mesh.getVertexBuffer(vb), mesh.getElements(vb.getInputRate()));
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

    public VkVertexInputBindingDescription.Buffer getBindingInfo(MemoryStack stack, VertexPipeline pipeline) {
        if (pipeline.getNumAttributes() == 0) {
            return VkVertexInputBindingDescription.malloc(0, stack);
        }
        VkVertexInputBindingDescription.Buffer binds = VkVertexInputBindingDescription.calloc(bindings.size(), stack);
        int i = 0;
        for (VertexBinding vb : bindings) {
            if (vb.bindOnPipeline(pipeline)) {
                binds.get().binding(i++)
                    .stride(vb.getStride())
                    .inputRate(vb.getInputRate().getEnum());
            }
        }
        binds.flip();
        return binds;
    }

    public VkVertexInputAttributeDescription.Buffer getAttributeInfo(MemoryStack stack, VertexPipeline pipeline) {
        if (pipeline.getNumAttributes() == 0) {
            return VkVertexInputAttributeDescription.malloc(0, stack);
        }
        long numAttr = 0;
        for (VertexBinding vb : bindings) {
            numAttr += vb.getAttributes().stream()
                    .filter(a -> a.bindOnPipeline(pipeline))
                    .count();
        }
        VkVertexInputAttributeDescription.Buffer attr = VkVertexInputAttributeDescription.calloc((int)numAttr, stack);
        int binding = 0;
        for (VertexBinding vb : bindings) {
            boolean anyValid = false;
            for (VertexBinding.NamedAttribute a : vb.getAttributes()) {
                Integer loc = pipeline.getAttributeLocation(a.getName());
                if (loc == null) {
                    continue;
                }
                attr.get().binding(binding)
                    .location(loc)
                    .format(a.getFormat().getVkEnum())
                    .offset(a.getOffset());
                anyValid = true;
            }
            if (anyValid) binding++;
        }
        attr.flip();
        return attr;
    }

    public boolean attributeExists(String name) {
        for (VertexBinding binding : bindings) {
            if (binding.getAttributes().stream().anyMatch(a -> a.getName().equals(name))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MeshLayout that = (MeshLayout) o;
        return Objects.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bindings);
    }

    @Override
    public Iterator<VertexBinding> iterator() {
        return Collections.unmodifiableList(bindings).iterator();
    }

    public static MeshLayout build(Consumer<Builder> config) {
        Builder b = new MeshLayout().new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder {

        public MeshLayout build() {
            return MeshLayout.this;
        }

        public void addBinding(VertexBinding binding) {
            bindings.add(binding);
        }

    }

}
