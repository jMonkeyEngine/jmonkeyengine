package com.jme3.vulkan.buffers;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class PersistentBuffer extends BasicVulkanBuffer {

    private final PointerBuffer mappedAddress = MemoryUtil.memCallocPointer(1);
    private PointerBuffer regionAddress;
    private ByteBuffer regionBuffer;

    public PersistentBuffer(LogicalDevice<?> device, MemorySize size) {
        super(device, size);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        if (offset > 0) {
            mappedAddress.put(0, MemoryUtil.memAddress(regionBuffer, offset));
        } else {
            mappedAddress.put(0, regionAddress.get(0));
        }
        return mappedAddress;
    }

    @Override
    public void unmap() {}

    @Override
    public Runnable createNativeDestroyer() {
        Runnable sup = super.createNativeDestroyer();
        return () -> {
            sup.run();
            MemoryUtil.memFree(mappedAddress);
        };
    }

    @Override
    public Builder build() {
        return new Builder();
    }

    public class Builder extends BasicVulkanBuffer.Builder {

        public Builder() {
            memFlags = Flag.of(MemoryProp.HostVisible, MemoryProp.HostCoherent);
        }

        @Override
        public void build() {
            if (!memFlags.contains(MemoryProp.HostVisible)) {
                throw new IllegalArgumentException("Memory must be host visible.");
            }
            super.build();
            regionAddress = getMemory().map(0, size().getBytes());
            regionBuffer = regionAddress.getByteBuffer(0, size().getBytes());
        }

    }

}
