package com.jme3.vulkan.mesh;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

/**
 * Describes the layout of vertex bindings and attributes, and simultaneously
 * acts as a compatability layer between a mesh and a mesh control.
 */
public interface MeshDescription {

    VertexBinding getBinding(int i);

    VertexAttribute getAttribute(String name);

    VkVertexInputBindingDescription.Buffer getBindingInfo(MemoryStack stack);

    VkVertexInputAttributeDescription.Buffer getAttributeInfo(MemoryStack stack);

    default AttributeModifier modifyAttribute(Mesh mesh, String attribute) {
        return new AttributeModifier(mesh, getAttribute(attribute));
    }

}
