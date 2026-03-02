package com.jme3.vulkan.material.uniforms;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.material.technique.PushConstantRange;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;
import java.util.function.Function;

public class StructUniform <T extends Struct> implements VulkanUniform<T> {

    private final IntEnum<Descriptor> descriptor;
    private final StructLayout layout;
    private VulkanBuffer buffer;
    private T value;

    public StructUniform(IntEnum<Descriptor> descriptor, StructLayout layout, T value, Function<MemorySize, VulkanBuffer> buffer) {
        this.descriptor = descriptor;
        this.layout = layout;
        this.value = value;
        this.buffer = buffer.apply(MemorySize.bytes(value.getSize()));
    }

    @Override
    public DescriptorSetWriter createWriter(CommandBuffer cmd, SetLayoutBinding binding) {
        if (value == null) {
            return null;
        }
        value.setLayout(layout);
        int size = value.getSize();
        buffer.resize(buffer.size().setBytes(size));
        try (BufferMapping m = buffer.map(0, size)) {
            value.write(m, 0);
        }
        cmd.stageBufferUpload(buffer);
        return new Writer(cmd.getPool().getDevice(), binding, buffer);
    }

    @Override
    public SetLayoutBinding createBinding(int binding, Flag<ShaderStage> scope) {
        return new SetLayoutBinding(descriptor, binding, 1, scope);
    }

    @Override
    public void fillPushConstantsBuffer(CommandBuffer cmd, PushConstantRange constant, BufferMapping mapping) {
        if (value != null) {
            constant.getSize().position(mapping.getBytes());
            value.setLayout(layout);
            try (BufferMapping m = buffer.map(0, value.getSize())) {
                value.write(m, 0);
            }
            cmd.stageBufferUpload(buffer);
        }
    }

    @Override
    public void set(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public String getDefineValue() {
        return value == null ? null : Uniform.ENABLED_DEFINE;
    }

    public VulkanBuffer getBuffer() {
        return buffer;
    }

    private static class Writer extends AbstractSetWriter {

        private final long id, offset, bytes;

        private Writer(LogicalDevice<?> device, SetLayoutBinding binding, VulkanBuffer data) {
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
