package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderModule implements Native<Long> {

    private final LogicalDevice<?> device;
    private final NativeReference ref;
    private final String entryPoint;
    private long id;

    public ShaderModule(LogicalDevice<?> device, ByteBuffer code, String entryPoint) {
        this.device = device;
        this.entryPoint = entryPoint;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo create = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(code);
            id = getLong(stack, ptr -> check(vkCreateShaderModule(device.getNativeObject(), create, null, ptr),
                    "Failed to create shader module."));
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyShaderModule(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {
        id = MemoryUtil.NULL;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

}
