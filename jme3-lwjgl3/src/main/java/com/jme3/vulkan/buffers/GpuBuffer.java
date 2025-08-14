package com.jme3.vulkan.buffers;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.flags.MemoryFlags;
import com.jme3.vulkan.flags.BufferUsageFlags;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.*;
import java.util.function.Function;
import java.util.function.LongFunction;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class GpuBuffer implements Native<Long> {

    private final LogicalDevice<?> device;
    private final NativeReference ref;
    private final MemorySize size;
    private final long id;
    protected final MemoryRegion memory;

    public GpuBuffer(LogicalDevice<?> device, MemorySize size, BufferUsageFlags usage, MemoryFlags mem, boolean concurrent) {
        this.device = device;
        this.size = size;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo create = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size.getBytes())
                    .usage(usage.getUsageFlags())
                    .sharingMode(VulkanUtils.sharingMode(concurrent));
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateBuffer(device.getNativeObject(), create, null, idBuf),
                    "Failed to create buffer.");
            id = idBuf.get(0);
            VkMemoryRequirements bufferMem = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device.getNativeObject(), id, bufferMem);
            memory = new MemoryRegion(device, bufferMem.size(), device.getPhysicalDevice().findSupportedMemoryType(
                    stack, bufferMem.memoryTypeBits(), mem.getMemoryFlags()));
            memory.bind(this, 0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
        memory.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyBuffer(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    private void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size.getBytes()) {
            throw new BufferOverflowException();
        }
    }

    public PointerBuffer map(MemoryStack stack, int offset, int size, int flags) {
        return memory.map(stack, offset, size, flags);
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

    public <T extends Struct<T>> T mapStruct(MemoryStack stack, int offset, int size, int flags, LongFunction<T> factory) {
        return factory.apply(map(stack, offset, size, flags).get(0));
    }

    public void copy(MemoryStack stack, ByteBuffer buffer) {
        verifyBufferSize(buffer.limit(), Byte.BYTES);
        MemoryUtil.memCopy(buffer, mapBytes(stack, 0, buffer.limit(), 0));
        unmap();
    }

    public void copy(MemoryStack stack, ShortBuffer buffer) {
        verifyBufferSize(buffer.limit(), Short.BYTES);
        MemoryUtil.memCopy(buffer, mapShorts(stack, 0, buffer.limit(), 0));
        unmap();
    }

    public void copy(MemoryStack stack, IntBuffer buffer) {
        verifyBufferSize(buffer.limit(), Integer.BYTES);
        MemoryUtil.memCopy(buffer, mapInts(stack, 0, buffer.limit(), 0));
        unmap();
    }

    public void copy(MemoryStack stack, FloatBuffer buffer) {
        verifyBufferSize(buffer.limit(), Float.BYTES);
        MemoryUtil.memCopy(buffer, mapFloats(stack, 0, buffer.limit(), 0));
        unmap();
    }

    public void copy(MemoryStack stack, DoubleBuffer buffer) {
        verifyBufferSize(buffer.limit(), Double.BYTES);
        MemoryUtil.memCopy(buffer, mapDoubles(stack, 0, buffer.limit(), 0));
        unmap();
    }

    public void copy(MemoryStack stack, LongBuffer buffer) {
        verifyBufferSize(buffer.limit(), Long.BYTES);
        MemoryUtil.memCopy(buffer, mapLongs(stack, 0, buffer.limit(), 0));
        unmap();
    }

    public void copy(MemoryStack stack, Struct<?> struct) {
        verifyBufferSize(struct.sizeof(), Byte.BYTES);
        MemoryUtil.memCopy(MemoryUtil.memByteBuffer(struct.address(), struct.sizeof()),
                mapBytes(stack, 0, struct.sizeof(), 0));
        unmap();
    }

    public void unmap() {
        memory.unmap();
    }

    public void recordCopy(MemoryStack stack, CommandBuffer commands, GpuBuffer source,
                           long srcOffset, long dstOffset, long size) {
        VkBufferCopy.Buffer copy = VkBufferCopy.calloc(1, stack)
                .srcOffset(srcOffset).dstOffset(dstOffset).size(size);
        vkCmdCopyBuffer(commands.getBuffer(), source.getNativeObject(), id, copy);
    }

    public void freeMemory() {
        memory.getNativeReference().destroy();
    }

    public MemorySize size() {
        return size;
    }

}
