package com.jme3.vulkan.material.uniforms;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;

public class BufferUniform <T extends MappableBuffer> implements VulkanUniform<T> {

    private final IntEnum<Descriptor> descriptor;
    private final StructLayout layout;
    private T buffer;

    public BufferUniform(IntEnum<Descriptor> descriptor, StructLayout layout, T buffer) {
        this.descriptor = descriptor;
        this.layout = layout;
        this.buffer = buffer;
    }

    @Override
    public DescriptorSetWriter createWriter(CommandBuffer cmd, UniformBinding binding) {
        if (buffer == null) {
            return null;
        }
        VulkanBuffer buf = (VulkanBuffer)buffer;
        cmd.stageBufferUpload(buf);
        return new Writer(cmd.getPool().getDevice(), binding, buf);
    }

    @Override
    public UniformBinding createBinding(int binding, Flag<ShaderStage> scope) {
        return new UniformBinding(descriptor, binding, 1, scope);
    }

    @Override
    public void set(T value) {
        this.buffer = value;
    }

    @Override
    public T get() {
        return buffer;
    }

    @Override
    public String getDefineValue() {
        return buffer == null ? null : ShaderParam.ENABLED_DEFINE;
    }

    public <S extends Struct> StructMapping<S> map(S struct) {
        struct.bind(layout);
        return buffer.mapAllStructs(struct);
    }

    private static class Writer extends AbstractSetWriter {

        private final long id, offset, bytes;

        private Writer(LogicalDevice<?> device, UniformBinding binding, VulkanBuffer data) {
            super(binding, 0, 1);
            this.id = data.getBufferId(device);
            this.offset = data.size().getOffset();
            this.bytes = data.size().getBytes();
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pBufferInfo(VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(id)
                    .offset(offset)
                    .range(bytes));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer) o;
            return id == writer.id
                    && offset == writer.offset
                    && bytes == writer.bytes
                    && Objects.equals(binding, writer.binding);
        }

        @Override
        public int hashCode() {
            return Objects.hash(binding, id, offset, bytes);
        }

    }

}
