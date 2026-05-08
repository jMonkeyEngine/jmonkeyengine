package com.jme3.vulkan.material.shader;

import com.jme3.util.natives.*;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.shaderc.ShaderType;
import com.jme3.vulkan.spvc.ModuleKey;
import com.jme3.vulkan.spvc.CompiledShaderCode;
import com.jme3.vulkan.spvc.SpvcCompiler;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.LongBuffer;
import java.util.Objects;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanShaderModule {

    private final CompiledShaderCode source;
    private final ShaderType type;
    private final String entryPoint;
    private ShaderHandle handle;

    public VulkanShaderModule(CompiledShaderCode source, String entryPoint) {
        this.source = source;
        this.entryPoint = entryPoint;
        this.type = source.getEntryPoint(entryPoint).getType();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VulkanShaderModule that = (VulkanShaderModule) o;
        return source == that.source && Objects.equals(entryPoint, that.entryPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(source), entryPoint);
    }

    public long getShaderId(LogicalDevice<?> device) {
        if (handle == null) {
            initialize(device);
        }
        return handle.getId();
    }

    protected void initialize(LogicalDevice<?> device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo create = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(source.getCompiledCode());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateShaderModule(device.getNativeObject(), create, null, idBuf),
                    "Failed to create shader module.");
            handle = new ShaderHandle(device, idBuf.get(0));
        }
    }

    public VkPipelineShaderStageCreateInfo fill(MemoryStack stack, VkPipelineShaderStageCreateInfo struct) {
        return struct.module(handle.getId()).stage(type.getEnum(VulkanEnums.instance)).pName(stack.UTF8(entryPoint));
    }

    public ShaderType getType() {
        return type;
    }

    public String getEntryPoint() {
        return entryPoint;
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

    public static class Key implements ModuleKey<VulkanShaderModule> {

        private final String entryPoint;

        public Key(String entryPoint) {
            this.entryPoint = entryPoint;
        }

        @Override
        public VulkanShaderModule createModule(SpvcCompiler compiler) {
            return new VulkanShaderModule(compiler, entryPoint);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(entryPoint, key.entryPoint);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(entryPoint);
        }

    }

}
