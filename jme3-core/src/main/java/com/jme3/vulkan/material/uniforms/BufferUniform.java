package com.jme3.vulkan.material.uniforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.generate.BufferGenerator;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.AccessFrequency;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.ReflectionArgs;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.ByteBuffer;
import java.util.*;

public class BufferUniform extends AbstractUniform<GpuBuffer> {

    public BufferUniform(String name, IntEnum<Descriptor> type, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, type, bindingIndex, stages);
    }

    public BufferUniform(IntEnum<Descriptor> type, ReflectionArgs args) {
        super(type, args);
        if (args.getProperties().has("usage")) {
            AccessFrequency usage = AccessFrequency.valueOf(args.getProperties().get("usage").asText());
            List<BufferMember> members = new ArrayList<>();
            int size = 0;
            for (Iterator<Map.Entry<String, JsonNode>> it = args.getProperties().get("layout").fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> el = it.next();
                BufferMember member = args.create(el.getKey(), el.getValue()).instantiate();
                members.add(member);
                size += member.getSizeInBytes();
            }
            value = args.getGenerator().createBuffer(MemorySize.bytes(size), BufferUsage.Uniform, usage);
            ByteBuffer bytes = value.mapBytes();
            bytes.clear();
            for (BufferMember m : members) {
                m.fillBuffer(bytes);
            }
            bytes.flip();
            value.unmap();
        }
    }

    @Override
    public DescriptorSetWriter createWriter() {
        if (value == null) {
            throw new NullPointerException("Cannot write null value.");
        }
        return new Writer(value);
    }

    @Override
    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return type.is(binding.getType())
            && bindingIndex == binding.getBinding()
            && binding.getDescriptors() == 1;
    }

    private class Writer implements DescriptorSetWriter {

        private final long id;
        private final long bytes;

        private Writer(GpuBuffer buffer) {
            this.id = buffer.getId();
            this.bytes = buffer.size().getBytes();
        }

        @Override
        public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
            VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(id)
                    .offset(0L)
                    .range(bytes);
            write.pBufferInfo(info)
                    .descriptorCount(1)
                    .dstArrayElement(0)
                    .dstBinding(bindingIndex)
                    .descriptorType(type.getEnum());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer) o;
            return id == writer.id && bytes == writer.bytes;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, bytes);
        }

    }

}
