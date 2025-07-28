package com.jme3.vulkan.buffers;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.CommandBuffer;
import com.jme3.vulkan.LogicalDevice;
import com.jme3.vulkan.MemoryRegion;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.*;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class GpuBuffer implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final int size;
    private final long id;
    private final MemoryRegion memory;

    public GpuBuffer(LogicalDevice device, int size, BufferArgs args) {
        this.device = device;
        this.size = size;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo create = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size) // size in bytes
                    .usage(args.getUsage())
                    .sharingMode(args.getSharingMode());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateBuffer(device.getNativeObject(), create, null, idBuf),
                    "Failed to create buffer.");
            id = idBuf.get(0);
            VkMemoryRequirements bufferMem = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device.getNativeObject(), id, bufferMem);
            memory = new MemoryRegion(device, bufferMem.size(), device.getPhysicalDevice().findMemoryType(
                    stack, bufferMem.memoryTypeBits(), args.getMemoryFlags()));
            check(vkBindBufferMemory(device.getNativeObject(), id, memory.getNativeObject(), 0),
                    "Failed to bind buffer memory");
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

    public void copy(MemoryStack stack, ByteBuffer buffer) {
        MemoryUtil.memCopy(buffer, memory.mapBytes(stack, 0, buffer.limit(), 0));
        memory.unmap();
    }

    public void copy(MemoryStack stack, IntBuffer buffer) {
        MemoryUtil.memCopy(buffer, memory.mapInts(stack, 0, buffer.limit(), 0));
        memory.unmap();
    }

    public void copy(MemoryStack stack, FloatBuffer buffer) {
        MemoryUtil.memCopy(buffer, memory.mapFloats(stack, 0, buffer.limit(), 0));
        memory.unmap();
    }

    public void recordCopy(MemoryStack stack, CommandBuffer commands, GpuBuffer source, long srcOffset, long dstOffset, long size) {
        commands.begin();
        VkBufferCopy.Buffer copy = VkBufferCopy.calloc(1, stack)
                .srcOffset(srcOffset).dstOffset(dstOffset).size(size);
        vkCmdCopyBuffer(commands.getBuffer(), source.getNativeObject(), id, copy);
        commands.end();
    }

    public void freeMemory() {
        memory.getNativeReference().destroy();
    }

    public int getSize() {
        return size; // size in bytes
    }

    public MemoryRegion getMemory() {
        return memory;
    }

}
