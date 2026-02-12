package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.mesh.NamedAttribute;
import com.jme3.vulkan.mesh.VertexBinding;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public interface VertexPipeline extends Pipeline {

    int getNumAttributes();

    Integer getAttributeLocation(String attributeName);

    default VkVertexInputBindingDescription.Buffer getBindingInfo(MemoryStack stack, MeshLayout layout) {
        if (getNumAttributes() == 0) {
            return VkVertexInputBindingDescription.malloc(0, stack);
        }
        VkVertexInputBindingDescription.Buffer binds = VkVertexInputBindingDescription.calloc(layout.getBindings().size(), stack);
        int i = 0;
        for (VertexBinding vb : layout.getBindings()) {
            if (vertexBindingCompatible(vb)) {
                binds.get().binding(i++)
                        .stride(vb.getStride())
                        .inputRate(vb.getInputRate().getEnum());
            }
        }
        binds.flip();
        return binds;
    }

    default VkVertexInputAttributeDescription.Buffer getAttributeInfo(MemoryStack stack, MeshLayout layout) {
        if (getNumAttributes() == 0) {
            return VkVertexInputAttributeDescription.malloc(0, stack);
        }
        int numAttrSlots = 0;
        for (VertexBinding vb : layout.getBindings()) {
            for (NamedAttribute a : vb.getAttributes()) {
                if (getAttributeLocation(a.getName()) == null) {
                    continue;
                }
                numAttrSlots += a.getFormats().length;
            }
        }
        VkVertexInputAttributeDescription.Buffer attr = VkVertexInputAttributeDescription.calloc(numAttrSlots, stack);
        int binding = 0;
        for (VertexBinding vb : layout.getBindings()) {
            boolean anyValid = false;
            for (NamedAttribute a : vb.getAttributes()) {
                Integer loc = getAttributeLocation(a.getName());
                if (loc == null) {
                    continue;
                }
                int offset = a.getOffset();
                for (int i = 0; i < a.getFormats().length; i++) {
                    Format f = a.getFormats()[i];
                    attr.get().binding(binding)
                        .location(loc + i)
                        .format(f.getEnum())
                        .offset(offset);
                    offset += f.getTotalBytes();
                }
                anyValid = true;
            }
            if (anyValid) binding++;
        }
        attr.flip();
        return attr;
    }

    default boolean vertexBindingCompatible(VertexBinding binding) {
        return binding.getAttributes().stream().anyMatch(a -> getAttributeLocation(a.getName()) != null);
    }

}
