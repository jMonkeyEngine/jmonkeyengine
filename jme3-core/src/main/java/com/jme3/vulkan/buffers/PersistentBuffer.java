package com.jme3.vulkan.buffers;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class PersistentBuffer extends BasicVulkanBuffer {

    private final PointerBuffer regionAddress;
    private final ByteBuffer regionBuffer;
    private final PointerBuffer mappedAddress = MemoryUtil.memCallocPointer(1);

    public PersistentBuffer(LogicalDevice<?> device, MemorySize size, Flag<BufferUsage> usage, boolean concurrent) {
        this(device, size, usage, Flag.of(MemoryProp.HostVisible, MemoryProp.HostCoherent), concurrent);
    }

    public PersistentBuffer(LogicalDevice<?> device, MemorySize size, Flag<BufferUsage> usage, Flag<MemoryProp> mem, boolean concurrent) {
        super(device, size, usage, mem, concurrent);
        if (!mem.contains(MemoryProp.HostVisible)) {
            throw new IllegalArgumentException("Memory must be host visible.");
        }
        regionAddress = memory.map(0, size.getBytes());
        regionBuffer = regionAddress.getByteBuffer(0, size.getBytes());
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

}
