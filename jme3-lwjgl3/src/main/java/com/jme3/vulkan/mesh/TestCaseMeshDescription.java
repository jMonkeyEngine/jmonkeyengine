package com.jme3.vulkan.mesh;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;

import static org.lwjgl.vulkan.VK10.*;

public class TestCaseMeshDescription implements Native<Object>, MeshDescription {

    private final VkVertexInputBindingDescription.Buffer bindings;
    private final VkVertexInputAttributeDescription.Buffer attributes;
    private final NativeReference ref;

    public TestCaseMeshDescription() {
        // for each vertex buffer on the mesh
        bindings = VkVertexInputBindingDescription.calloc(1)
                .binding(0)
                .stride(Float.BYTES * 8) // bytes per vertex
                .inputRate(InputRate.Vertex.getVkEnum());
        try (VkViewport struct = VkViewport.calloc()) {
            // for each attribute in each vertex buffer
            attributes = VkVertexInputAttributeDescription.calloc(3);
            attributes.get(0)
                    .binding(0)
                    .location(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(0);
            attributes.get(1)
                    .binding(0)
                    .location(1)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(Float.BYTES * 3);
            attributes.get(2)
                    .binding(0)
                    .location(2)
                    .format(VK_FORMAT_R32G32_SFLOAT)
                    .offset(Float.BYTES * 6);
        }
        ref = Native.get().register(this);
    }

    @Override
    public Object getNativeObject() {
        return null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            bindings.free();
            attributes.free();
        };
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    @Override
    public VkVertexInputBindingDescription.Buffer getBindings(MemoryStack stack) {
        return bindings;
    }

    @Override
    public VkVertexInputAttributeDescription.Buffer getAttributes(MemoryStack stack) {
        return attributes;
    }

}
