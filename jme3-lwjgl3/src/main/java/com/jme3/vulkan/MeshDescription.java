package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public class MeshDescription implements Native<Object> {

    private final VkVertexInputBindingDescription.Buffer bindings;
    private final VkVertexInputAttributeDescription.Buffer attributes;
    private final NativeReference ref;

    public MeshDescription() {
        // for each vertex buffer on the mesh
        bindings = VkVertexInputBindingDescription.calloc(1)
                .binding(0)
                .stride(Float.BYTES * 8) // bytes per vertex
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        // for each attribute in each vertex buffer
        attributes = VkVertexInputAttributeDescription.calloc(3);
        attributes.get(0).binding(0)
                .location(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(0);
        attributes.get(1).binding(0)
                .location(1)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(Float.BYTES * 3);
        attributes.get(2).binding(0)
                .location(2)
                .format(VK_FORMAT_R32G32_SFLOAT)
                .offset(Float.BYTES * 6);
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

    public VkVertexInputBindingDescription.Buffer getBindings() {
        return bindings;
    }

    public VkVertexInputAttributeDescription.Buffer getAttributes() {
        return attributes;
    }

}
