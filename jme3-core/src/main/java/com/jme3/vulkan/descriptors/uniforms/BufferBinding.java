package com.jme3.vulkan.descriptors.uniforms;

import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.descriptors.DescriptorBinding;
import com.jme3.vulkan.descriptors.DescriptorType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class BufferBinding extends DescriptorBinding<GpuBuffer> {

    public BufferBinding(DescriptorType type, int descriptorCount) {
        super(type, new GpuBuffer[descriptorCount]);
    }

    public BufferBinding(DescriptorType type, GpuBuffer... buffers) {
        super(type, buffers);
    }

    @Override
    protected void populateElementInfo(MemoryStack stack, VkWriteDescriptorSet write, int start, int count) {
        VkDescriptorBufferInfo.Buffer bufInfo = VkDescriptorBufferInfo.calloc(count, stack);
        for (int i = 0; i < count; i++) {
            GpuBuffer b = resources[start + i];
            if (b != null) {
                bufInfo.get().buffer(b.getHandle()).offset(b.getBufferLocalOffset()).range(b.capacity());
            } else {
                bufInfo.get().buffer(VK10.VK_NULL_HANDLE);
            }
        }
        write.pBufferInfo(bufInfo);
    }

}
