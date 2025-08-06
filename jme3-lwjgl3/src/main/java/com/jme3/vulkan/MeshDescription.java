package com.jme3.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public interface MeshDescription {

    VkVertexInputBindingDescription.Buffer getBindings(MemoryStack stack);

    VkVertexInputAttributeDescription.Buffer getAttributes(MemoryStack stack);

}
