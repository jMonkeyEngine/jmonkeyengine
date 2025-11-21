package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes the layout of vertex bindings and attributes, and simultaneously
 * acts as a compatability layer between a mesh and a mesh control.
 */
public class MeshDescription implements Iterable<OldVertexBinding> {

    private final List<OldVertexBinding> bindings = new ArrayList<>();
    private final AtomicBoolean built = new AtomicBoolean(false);

    @Override
    public Iterator<OldVertexBinding> iterator() {
        return bindings.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MeshDescription that = (MeshDescription) o;
        return Objects.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bindings);
    }

    public VkVertexInputBindingDescription.Buffer getBindingInfo(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer binds = VkVertexInputBindingDescription.calloc(bindings.size(), stack);
        for (OldVertexBinding b : bindings) {
            binds.get().binding(b.getBindingIndex())
                    .stride(b.getStride())
                    .inputRate(b.getRate().getEnum());
        }
        binds.flip();
        return binds;
    }

    public VkVertexInputAttributeDescription.Buffer getAttributeInfo(MemoryStack stack) {
        int size = 0;
        for (OldVertexBinding b : bindings) {
            size += b.getAttributes().size();
        }
        VkVertexInputAttributeDescription.Buffer attr = VkVertexInputAttributeDescription.calloc(size, stack);
        for (OldVertexBinding binding : bindings) {
            for (VertexAttribute a : binding) {
                attr.get().binding(binding.getBindingIndex())
                        .location(a.getLocation())
                        .format(a.getFormat().getVkEnum())
                        .offset(a.getOffset());
            }
        }
        attr.flip();
        return attr;
    }

    public OldVertexBinding getBinding(int index) {
        return bindings.get(index);
    }

    public VertexAttribute getAttribute(String name) {
        for (OldVertexBinding b : bindings) {
            VertexAttribute a = b.getAttribute(name);
            if (a != null) return a;
        }
        return null;
    }

    public List<OldVertexBinding> getBindings() {
        return Collections.unmodifiableList(bindings);
    }

    public Builder build() {
        if (built.getAndSet(true)) {
            throw new IllegalStateException("Cannot be modified after initial setup.");
        }
        return new Builder();
    }

    public class Builder implements AutoCloseable {

        @Override
        public void close() {}

        public OldVertexBinding addBinding(IntEnum<InputRate> rate) {
            OldVertexBinding binding = new OldVertexBinding(bindings.size(), rate);
            bindings.add(binding);
            return binding;
        }

        public VertexAttribute addAttribute(OldVertexBinding binding, String name, Format format, int location) {
            if (binding.getBindingIndex() >= bindings.size() || bindings.get(binding.getBindingIndex()) != binding) {
                throw new IllegalArgumentException("Vertex binding does not belong to this mesh description.");
            }
            return binding.addAttribute(name, format, location);
        }

    }

}
