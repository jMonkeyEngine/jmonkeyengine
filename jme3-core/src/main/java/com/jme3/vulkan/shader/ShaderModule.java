package com.jme3.vulkan.shader;

import com.jme3.asset.AssetManager;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.shaderc.ShadercLoader;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class ShaderModule extends AbstractNative<Long> {

    public static final String DEFAULT_ENTRY_POINT = "main";

    private final LogicalDevice<?> device;
    private String assetName;
    private ShaderStage stage;
    private String entryPoint = DEFAULT_ENTRY_POINT;
    private final Map<String, String> defines = new HashMap<>();

    public ShaderModule(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyShaderModule(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ShaderModule that = (ShaderModule) o;
        return device == that.device
                && Objects.equals(assetName, that.assetName)
                && Objects.equals(stage, that.stage)
                && Objects.equals(entryPoint, that.entryPoint)
                && Objects.equals(defines, that.defines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, assetName, stage, entryPoint, defines);
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

    public Map<String, String> getDefines() {
        return Collections.unmodifiableMap(defines);
    }

    public static ShaderModule build(LogicalDevice<?> device, AssetManager assetManager, Consumer<Builder> config) {
        Builder b = new ShaderModule(device).new Builder(assetManager);
        config.accept(b);
        return b.build();
    }

    public class Builder extends CacheableNativeBuilder<ShaderModule, ShaderModule> {

        private final AssetManager assetManager;

        private Builder(AssetManager assetManager) {
            this.assetManager = assetManager;
        }

        @Override
        public ShaderModule build() {
            if (assetName == null) {
                throw new NullPointerException("Asset name not specified.");
            }
            if (stage == null) {
                throw new NullPointerException("Shader stage not specified.");
            }
            return super.build();
        }

        @Override
        protected void construct() {
            ByteBuffer code = assetManager.loadAsset(ShadercLoader.key(assetName, stage, entryPoint, defines));
            VkShaderModuleCreateInfo create = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(code);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateShaderModule(device.getNativeObject(), create, null, idBuf),
                    "Failed to create shader module.");
            object = idBuf.get(0);
            ref = Native.get().register(ShaderModule.this);
            device.getNativeReference().addDependent(ref);
        }

        @Override
        protected ShaderModule getBuildTarget() {
            return ShaderModule.this;
        }

        public void setDefine(String name, String value) {
            defines.put(name, value);
        }

        public void clearDefines() {
            defines.clear();
        }

        public void setAssetName(String assetName) {
            ShaderModule.this.assetName = assetName;
        }

        public void setStage(ShaderStage stage) {
            ShaderModule.this.stage = stage;
        }

        public void setEntryPoint(String entryPoint) {
            ShaderModule.this.entryPoint = entryPoint;
        }

    }

}
