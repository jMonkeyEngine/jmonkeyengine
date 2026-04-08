package com.jme3.vulkan.descriptors.uniforms;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;

public class BufferUniformBinding extends UniformBinding<VulkanBuffer> {

    public BufferUniformBinding(IntEnum<Descriptor> descriptor, int binding, Flag<ShaderStage> stages) {
        super(descriptor, binding, 1, stages);
    }

    @Override
    public DescriptorSetWriter createWriter(VulkanBuffer value) {
        return new Writer(cmd.getPool().getDevice(), value);
    }

    private static class Writer implements DescriptorSetWriter {

        private final LogicalDevice<?> device;
        private final VulkanBuffer buffer;
        private final MemorySize size;

        public Writer(LogicalDevice<?> device, VulkanBuffer buffer) {
            this.buffer = buffer;
            this.device = device;
            this.size = buffer.size();
        }

        @Override
        public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pBufferInfo(VkDescriptorBufferInfo.calloc(1, stack)
                .buffer(buffer.getBufferId(device))
                .offset(size.getOffset())
                .range(size.getBytes()));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer) o;
            return buffer.getBufferId(device) == writer.buffer.getBufferId(device) && Objects.equals(size, writer.size);
        }

        @Override
        public int hashCode() {
            return Objects.hash(buffer.getBufferId(device), size);
        }

    }

}
