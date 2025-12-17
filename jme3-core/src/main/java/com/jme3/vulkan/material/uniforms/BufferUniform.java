package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;

public class BufferUniform implements Uniform<GpuBuffer> {

    private GpuBuffer value;

    @Override
    public DescriptorSetWriter createWriter(SetLayoutBinding binding) {
        if (value == null) {
            throw new NullPointerException("Cannot write null value.");
        }
        return new Writer(binding, value);
    }

    @Override
    public void set(GpuBuffer value) {
        this.value = value;
    }

    @Override
    public GpuBuffer get() {
        return value;
    }

    private static class Writer extends AbstractSetWriter {

        private final long id, bytes;

        private Writer(SetLayoutBinding binding, GpuBuffer buffer) {
            super(binding, 0, 1);
            this.id = buffer.getId();
            this.bytes = buffer.size().getBytes();
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pBufferInfo(VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(id)
                    .offset(0L)
                    .range(bytes));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer) o;
            return id == writer.id && bytes == writer.bytes
                    && Objects.equals(binding, writer.binding);
        }

        @Override
        public int hashCode() {
            return Objects.hash(binding, id, bytes);
        }

    }

}
