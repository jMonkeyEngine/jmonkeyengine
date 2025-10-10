package com.jme3.vulkan.shader;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderModule extends AbstractNative<Long> {

    private final LogicalDevice<?> device;

    public ShaderModule(LogicalDevice<?> device, ByteBuffer code) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo create = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(code);
            object = getLong(stack, ptr -> check(vkCreateShaderModule(device.getNativeObject(), create, null, ptr),
                    "Failed to create shader module."));
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyShaderModule(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ShaderModule that = (ShaderModule) o;
        return device == that.device && Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, object);
    }

}
