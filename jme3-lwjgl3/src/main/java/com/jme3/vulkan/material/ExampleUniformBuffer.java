package com.jme3.vulkan.material;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class ExampleUniformBuffer implements DescriptorSetWriter {

    private final int binding;
    private GpuBuffer buffer;
    private boolean updateFlag = true;

    public ExampleUniformBuffer(int binding, GpuBuffer buffer) {
        this.binding = binding;
        this.buffer = buffer;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        VkDescriptorBufferInfo.Buffer descriptors = VkDescriptorBufferInfo.calloc(1, stack)
                .buffer(buffer.getNativeObject())
                .offset(0)
                .range(buffer.size().getBytes());
        write.descriptorType(Descriptor.UniformBuffer.getVkEnum())
                .dstBinding(binding)
                .dstArrayElement(0) // todo: make configurable?
                .descriptorCount(descriptors.limit())
                .pBufferInfo(descriptors);
        updateFlag = false;
    }

    @Override
    public boolean isUpdateNeeded() {
        return updateFlag;
    }

    public void setBuffer(GpuBuffer buffer) {
        this.buffer = buffer;
        this.updateFlag = true;
    }

    public GpuBuffer getBuffer() {
        return buffer;
    }

}
