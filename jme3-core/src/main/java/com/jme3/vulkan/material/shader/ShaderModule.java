package com.jme3.vulkan.material.shader;

import com.jme3.asset.AssetManager;
import com.jme3.util.natives.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.shaderc.ShadercLoader;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
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

public class ShaderModule {

    public static final String DEFAULT_ENTRY_POINT = "main";

    private final AssetManager assetManager;
    private final String assetName;
    private final ShaderStage stage;
    private ShaderHandle handle;
    private String entryPoint = DEFAULT_ENTRY_POINT;
    private final Map<String, String> defines = new HashMap<>();

    /**
     * Creates a new shader module not subject to caching.
     *
     * @param assetManager asset manager
     * @param stage target shader stage
     * @param assetName path of the shader asset
     */
    public ShaderModule(AssetManager assetManager, ShaderStage stage, String assetName) {
        this.assetManager = assetManager;
        this.stage = stage;
        this.assetName = assetName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ShaderModule that = (ShaderModule) o;
        return Objects.equals(assetName, that.assetName)
                && Objects.equals(stage, that.stage)
                && Objects.equals(entryPoint, that.entryPoint)
                && Objects.equals(defines, that.defines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetName, stage, entryPoint, defines);
    }

    public long getShaderId(LogicalDevice<?> device) {
        if (handle == null) {
            initialize(device);
        }
        return handle.getId();
    }

    protected void initialize(LogicalDevice<?> device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer code = assetManager.loadAsset(ShadercLoader.key(assetName, stage, entryPoint, defines));
            VkShaderModuleCreateInfo create = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(code);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateShaderModule(device.getNativeObject(), create, null, idBuf),
                    "Failed to create shader module for " + assetName);
            handle = new ShaderHandle(device, idBuf.get(0));
        }
    }

    public VkPipelineShaderStageCreateInfo fill(MemoryStack stack, VkPipelineShaderStageCreateInfo struct) {
        return struct.module(handle.getId()).stage(stage.getVk()).pName(stack.UTF8(entryPoint));
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

    public static ShaderModule build(AssetManager assetManager, Cache<ShaderModule> cache, ShaderStage stage, String assetName) {
        return build(assetManager, stage, assetName, b -> b.setCache(cache));
    }

    public static ShaderModule build(AssetManager assetManager, ShaderStage stage, String assetName, Consumer<Builder> config) {
        Builder b = new ShaderModule(assetManager, stage, assetName).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends CacheableNativeBuilder<ShaderModule, ShaderModule> {

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
        protected void construct() {}

        @Override
        protected ShaderModule getBuildTarget() {
            return ShaderModule.this;
        }

        public void setDefine(String name, String value) {
            defines.put(name, value);
        }

        public void setDefines(Map<String, String> defines) {
            ShaderModule.this.defines.putAll(defines);
        }

        public void clearDefines() {
            defines.clear();
        }

        public void setEntryPoint(String entryPoint) {
            ShaderModule.this.entryPoint = entryPoint;
        }

    }

    protected static class ShaderHandle implements Disposable {

        private final LogicalDevice<?> device;
        private final long id;
        private final DisposableReference ref;

        public ShaderHandle(LogicalDevice<?> device, long id) {
            this.device = device;
            this.id = id;
            this.ref = DisposableManager.reference(this);
        }

        @Override
        public Runnable createDestroyer() {
            return () -> vkDestroyShaderModule(device.getNativeObject(), id, null);
        }

        @Override
        public DisposableReference getReference() {
            return ref;
        }

        public LogicalDevice<?> getDevice() {
            return device;
        }

        public long getId() {
            return id;
        }

    }

}
