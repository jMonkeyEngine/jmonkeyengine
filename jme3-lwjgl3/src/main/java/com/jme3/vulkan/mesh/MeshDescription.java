package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.util.LibEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.util.*;

/**
 * Describes the layout of vertex bindings and attributes, and simultaneously
 * acts as a compatability layer between a mesh and a mesh control.
 */
public class MeshDescription implements Iterable<VertexBinding> {

    private final List<VertexBinding> bindings = new ArrayList<>();

    public VkVertexInputBindingDescription.Buffer getBindingInfo(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer binds = VkVertexInputBindingDescription.calloc(bindings.size(), stack);
        for (VertexBinding b : bindings) {
            binds.get().binding(b.getBindingIndex())
                    .stride(b.getStride())
                    .inputRate(b.getRate().getEnum());
        }
        binds.flip();
        return binds;
    }

    public VkVertexInputAttributeDescription.Buffer getAttributeInfo(MemoryStack stack) {
        int size = 0;
        for (VertexBinding b : bindings) {
            size += b.getAttributes().size();
        }
        VkVertexInputAttributeDescription.Buffer attr = VkVertexInputAttributeDescription.calloc(size, stack);
        for (VertexBinding binding : bindings) {
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

    public int addBinding(LibEnum<InputRate> rate) {
        VertexBinding binding = new VertexBinding(bindings.size(), rate);
        bindings.add(binding);
        return binding.getBindingIndex();
    }

    public int addAttribute(String name, LibEnum<InputRate> rate, Format format, int location) {
        int binding = addBinding(rate);
        addAttribute(name, binding, format, location);
        return binding;
    }

    public void addAttribute(String name, int bindingIndex, Format format, int location) {
        getBinding(bindingIndex).addAttribute(name, format, location);
    }

    public VertexBinding getBinding(int index) {
        return bindings.get(index);
    }

    public VertexAttribute getAttribute(String name) {
        for (VertexBinding b : bindings) {
            VertexAttribute a = b.getAttribute(name);
            if (a != null) return a;
        }
        return null;
    }

    public List<VertexBinding> getBindings() {
        return Collections.unmodifiableList(bindings);
    }

    @Override
    public Iterator<VertexBinding> iterator() {
        return bindings.iterator();
    }

}
