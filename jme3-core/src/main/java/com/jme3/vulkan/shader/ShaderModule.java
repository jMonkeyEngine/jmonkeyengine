package com.jme3.vulkan.shader;

import com.jme3.asset.AssetManager;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.states.IShaderState;
import com.jme3.vulkan.shaderc.ShadercLoader;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Objects;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderModule extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final ShaderStage stage;
    private final String entryPoint;

    public ShaderModule(LogicalDevice<?> device, AssetManager assetManager, IShaderState state) {
        this.device = device;
        this.stage = state.getStage();
        this.entryPoint = state.getEntryPoint();
        ByteBuffer code = assetManager.loadAsset(ShadercLoader.key(state.getAssetName(), state.getStage(), state.getEntryPoint()));
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo create = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(code);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateShaderModule(device.getNativeObject(), create, null, idBuf),
                    "Failed to create shader module.");
            object = idBuf.get(0);
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

    public VkPipelineShaderStageCreateInfo fill(MemoryStack stack, VkPipelineShaderStageCreateInfo struct) {
        return struct.module(object).stage(stage.getVk()).pName(stack.UTF8(entryPoint));
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public ShaderStage getStage() {
        return stage;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

}
