package com.jme3.vulkan.mesh;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.mesh.attribute.Attribute;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MeshLayout implements Iterable<VertexBinding> {

    private final List<VertexBinding> bindings = new ArrayList<>();

    public <T extends Attribute> T mapAttribute(AdaptiveMesh mesh, String name) {
        for (VertexBinding b : bindings) {
            AdaptiveMesh.VertexBuffer vb = mesh.getVertexBuffer(b);
            int elements = mesh.getElementsOf(b.getInputRate());
            if (vb == null) {
                vb = mesh.createVertexBuffer(b, b.createBuffer(elements));
            }
            T attr = b.mapAttribute(name, vb.getData(), elements);
            if (attr != null) return attr;
        }
        throw new IllegalArgumentException("\"" + name + "\" is not an existing attribute.");
    }

    public <T extends Attribute> T mapAttribute(AdaptiveMesh mesh, GlVertexBuffer.Type name) {
        return mapAttribute(mesh, name.name());
    }

    public void addBinding(VertexBinding binding) {
        bindings.add(binding);
    }

    public VkVertexInputBindingDescription.Buffer getBindingInfo(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer binds = VkVertexInputBindingDescription.calloc(bindings.size(), stack);
        int i = 0;
        for (VertexBinding vb : bindings) {
            binds.get().binding(i++)
                    .stride(vb.getStride())
                    .inputRate(vb.getInputRate().getEnum());
        }
        binds.flip();
        return binds;
    }

    public VkVertexInputAttributeDescription.Buffer getAttributeInfo(MemoryStack stack) {
        int size = 0;
        for (VertexBinding vb : bindings) {
            size += vb.getNumAttributes();
        }
        VkVertexInputAttributeDescription.Buffer attr = VkVertexInputAttributeDescription.calloc(size, stack);
        int binding = 0;
        for (VertexBinding vb : bindings) {
            int location = 0;
            int offset = 0;
            for (VertexBinding.NamedAttribute a : vb) {
                attr.get().binding(binding)
                        .location(location++)
                        .format(a.getFormat().getVkEnum())
                        .offset(offset);
                offset += a.getFormat().getTotalBytes();
            }
            binding++;
        }
        attr.flip();
        return attr;
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
        return bindings.iterator();
    }

}
