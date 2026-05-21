package com.jme3.vulkan.alloc;

import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.buffernew.GpuBuffer;
import com.jme3.vulkan.buffers.mapping.BufferMapping;
import com.jme3.vulkan.buffers.mapping.DirectBufferMapping;
import com.jme3.vulkan.buffers.mapping.SourceBufferMapping;
import com.jme3.vulkan.buffers.mapping.VirtualBufferMapping;
import com.jme3.vulkan.buffers.stream.BufferTracker;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Set;

import static org.lwjgl.util.vma.Vma.*;

public class MemoryAllocator implements Disposable {

    private final long allocator;
    private final DisposableReference ref;

    public MemoryAllocator(LogicalDevice<?> device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VmaAllocatorCreateInfo create = VmaAllocatorCreateInfo.calloc(stack)
                    .instance(device.getInstance().getNativeObject())
                    .device(device.getNativeObject())
                    .physicalDevice(device.getPhysicalDevice().getDeviceHandle())
                    .vulkanApiVersion(device.getInstance().getApiVersion().getEnum())
                    .flags(allocatorCreateFlags(device.getEnabledExtensions()));
            PointerBuffer ptr = stack.mallocPointer(1);
            vmaCreateAllocator(create, ptr);
            allocator = ptr.get(0);
        }
        ref = DisposableManager.reference(this);
        device.getReference().addDependent(ref);
    }

    private int allocatorCreateFlags(Set<String> enabledExtensions) {
        int flags = 0;
        for (String ext : enabledExtensions) {
            switch (ext) {
                case "VK_KHR_dedication_allocation": flags |= VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT; break;
                case "VK_KHR_bind_memory2": flags |= VMA_ALLOCATOR_CREATE_KHR_BIND_MEMORY2_BIT; break;
                case "VK_KHR_maintenance4": flags |= VMA_ALLOCATOR_CREATE_KHR_MAINTENANCE4_BIT; break;
                case "VK_KHR_maintenance5": flags |= VMA_ALLOCATOR_CREATE_KHR_MAINTENANCE5_BIT; break;
                case "VK_EXT_memory_budget": flags |= VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT; break;
                case "VK_KHR_buffer_device_address": flags |= VMA_ALLOCATOR_CREATE_BUFFER_DEVICE_ADDRESS_BIT; break;
                case "VK_EXT_memory_priority": flags |= VMA_ALLOCATOR_CREATE_EXT_MEMORY_PRIORITY_BIT; break;
                case "VK_AMD_device_coherent_memory": flags |= VMA_ALLOCATOR_CREATE_AMD_DEVICE_COHERENT_MEMORY_BIT; break;
                case "VK_KHR_external_memory_win32": flags |= VMA_ALLOCATOR_CREATE_KHR_EXTERNAL_MEMORY_WIN32_BIT; break;
            }
        }
        return flags;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vmaDestroyAllocator(allocator);
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    public GpuBuffer createBuffer(VkBufferCreateInfo bufferCreate, VmaAllocationCreateInfo allocCreate) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer bufferPtr = stack.mallocLong(1);
            PointerBuffer allocPtr = stack.mallocPointer(1);
            vmaCreateBuffer(allocator, bufferCreate, allocCreate, bufferPtr, allocPtr, null);
            return new Buffer(bufferPtr.get(0), allocPtr.get(0), bufferCreate.size());
        }
    }

    private class Buffer implements GpuBuffer {

        private final long buffer;
        private final long alloc;
        private final long size;
        private final Flag<MemoryProp> memProps;
        private final BufferTracker tracker = new BufferTracker();

        public Buffer(long buffer, long alloc, long size) {
            this.buffer = buffer;
            this.alloc = alloc;
            this.size = size;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer memPropBuf = stack.mallocInt(1);
                vmaGetAllocationMemoryProperties(allocator, alloc, memPropBuf);
                memProps = Flag.of(memPropBuf.get(0));
            }
        }

        @Override
        public BufferMapping map() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer address = stack.mallocPointer(1);
                vmaMapMemory(allocator, alloc, address);
                return new SourceBufferMapping(this, MemoryUtil.memByteBuffer(address.get(0), (int)size), this::unmap);
            }
        }

        @Override
        public void unmap() {
            if (memProps.contains(MemoryProp.HostVisible)) {
                vmaUnmapMemory(allocator, alloc);
            }
        }

        @Override
        public void stage(long offset, long size) {
            tracker.add(offset, size);
        }

        @Override
        public void flush() {
            vmaFlushAllocation(allocator, alloc, 0, VK10.VK_WHOLE_SIZE);
        }

        @Override
        public void invalidate() {
            vmaInvalidateAllocation(allocator, alloc, 0, VK10.VK_WHOLE_SIZE);
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public void reclaim() {
            vmaDestroyBuffer(allocator, buffer, alloc);
        }

    }

}
