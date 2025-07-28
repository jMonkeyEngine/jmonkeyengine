package com.jme3.vulkan;

import com.jme3.util.IntMap;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;

import java.nio.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryRegion implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final long id;
    private final AtomicBoolean mapped = new AtomicBoolean(false);

    public MemoryRegion(LogicalDevice device, long size, int typeIndex) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryAllocateInfo allocate = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(size)
                    .memoryTypeIndex(typeIndex);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkAllocateMemory(device.getNativeObject(), allocate, null, idBuf),
                    "Failed to allocate buffer memory.");
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkFreeMemory(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public PointerBuffer map(MemoryStack stack, int offset, int size, int flags) {
        if (mapped.getAndSet(true)) {
            throw new IllegalStateException("Memory already mapped.");
        }
        PointerBuffer data = stack.mallocPointer(1);
        vkMapMemory(device.getNativeObject(), id, offset, size, flags, data);
        return data;
    }

    public ByteBuffer mapBytes(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size, flags).getByteBuffer(0, size);
    }

    public ShortBuffer mapShorts(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Short.BYTES, flags).getShortBuffer(0, size);
    }

    public IntBuffer mapInts(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Integer.BYTES, flags).getIntBuffer(0, size);
    }

    public FloatBuffer mapFloats(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Float.BYTES, flags).getFloatBuffer(0, size);
    }

    public DoubleBuffer mapDoubles(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Double.BYTES, flags).getDoubleBuffer(0, size);
    }

    public LongBuffer mapLongs(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Long.BYTES, flags).getLongBuffer(0, size);
    }

    public void unmap() {
        if (!mapped.getAndSet(false)) {
            throw new IllegalStateException("Memory is not mapped.");
        }
        vkUnmapMemory(device.getNativeObject(), id);
    }

}
