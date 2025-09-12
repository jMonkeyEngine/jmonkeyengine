package com.jme3.vulkan.buffers;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class BackedStaticBuffer extends StaticBuffer {

    private final PointerBuffer location = MemoryUtil.memAllocPointer(1);
    private final ByteBuffer backing;
    private boolean backed = false;

    public BackedStaticBuffer(LogicalDevice device, MemorySize size, Flag<BufferUsage> usage, Flag<MemoryProp> mem, boolean concurrent) {
        super(device, size, usage, mem, concurrent);
        backing = MemoryUtil.memCalloc(size.getBytes(), Byte.BYTES);
        ref.refresh();
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        if (backed) {
            location.put(0, MemoryUtil.memAddress(backing, offset));
            return location;
        }
        return super.map(offset, size);
    }

    @Override
    public ByteBuffer mapBytes() {
        if (backed) {
            return backing;
        }
        return super.mapBytes();
    }

    @Override
    public void unmap() {
        super.unmap();
        ByteBuffer data = super.mapBytes();
        MemoryUtil.memCopy(data, backing);
        super.unmap();
        backed = true;
    }

    @Override
    public Runnable createNativeDestroyer() {
        Runnable sup = super.createNativeDestroyer();
        return () -> {
            sup.run();
            MemoryUtil.memFree(location);
            MemoryUtil.memFree(backing);
        };
    }

}
