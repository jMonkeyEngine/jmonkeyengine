package com.jme3.vulkan.alloc;

import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.buffer.BufferMapping;
import com.jme3.vulkan.buffer.BufferTracker;
import com.jme3.vulkan.buffer.BufferUsage;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Set;
import java.util.logging.Logger;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class VmaMemoryAllocator implements Disposable {

    private static final Logger LOG = Logger.getLogger(VmaMemoryAllocator.class.getName());

    private final long allocator;
    private final DisposableReference ref;

    public VmaMemoryAllocator(LogicalDevice<?> device) {
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

    public MappableBuffer createBuffer(VkBufferCreateInfo bufferCreate, VmaAllocationCreateInfo allocCreate) {
        if (bufferCreate.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unable to allocate buffer with more than " + Integer.MAX_VALUE + " bytes (maximum int).");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer bufferPtr = stack.mallocLong(1);
            PointerBuffer allocPtr = stack.mallocPointer(1);
            vmaCreateBuffer(allocator, bufferCreate, allocCreate, bufferPtr, allocPtr, null);
            if ((allocCreate.flags() & VMA_ALLOCATION_CREATE_MAPPED_BIT) != 0) {
                return new PersistentBuffer(bufferPtr.get(0), allocPtr.get(0), (int)bufferCreate.size());
            } else {
                return new VmaBuffer(bufferPtr.get(0), allocPtr.get(0), (int)bufferCreate.size());
            }
        }
    }

    public MappableBuffer createStreamingBuffer(int size, Flag<BufferUsage> usage) {
        // a streaming buffer is made up of a host visible buffer for each frame in flight.
        // host changes for the current frame are written to the corresponding buffer, and then
        // changes from previous frames are copied over from the previous frame's buffer.
        // alternatively, a nio buffer can be used by the host and changes are copied to each
        // frame's buffer on that frame.

    }

    public MappableBuffer createDynamicBuffer(int size, Flag<BufferUsage> usage) {
        // a dynamic buffer
    }

    public MappableBuffer createStaticBuffer(int size, Flag<BufferUsage> usage) {

    }

    private class VmaBuffer implements EngineBuffer, Disposable {

        private final long buffer, alloc;
        private final int size;
        private final Flag<MemoryProp> memProps;
        private final int[] staged = new int[2];
        private ByteBuffer mapping;

        public VmaBuffer(long buffer, long alloc, int size) {
            this.buffer = buffer;
            this.alloc = alloc;
            this.size = size;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer memPropBuf = stack.mallocInt(1);
                vmaGetAllocationMemoryProperties(allocator, alloc, memPropBuf);
                memProps = Flag.of(memPropBuf.get(0));
            }
            staged[0] = size;
            staged[1] = 0;
        }

        @Override
        public void stage(int offset, int size) {
            staged[0] = Math.min(staged[0], offset);
            staged[1] = Math.max(staged[1], offset + size);
        }

        @Override
        public void pushStaged() {
            if (staged[0] < staged[1] && memProps.contains(MemoryProp.HostVisible) && !memProps.contains(MemoryProp.HostCoherent)) {
                vmaFlushAllocation(allocator, alloc, staged[0], staged[1] - staged[0]);
            }
        }

        @Override
        public void pullStaged() {
            if (staged[0] < staged[1] && memProps.contains(MemoryProp.HostVisible) && !memProps.contains(MemoryProp.HostCoherent)) {
                vmaInvalidateAllocation(allocator, alloc, staged[0], staged[1] - staged[0]);
            }
        }

        @Override
        public void clearStaging() {
            staged[0] = size;
            staged[1] = 0;
        }

        @Override
        public BufferMapping map() {
            return new BufferMapping(this, mapping.duplicate());
        }

        @Override
        public EngineBuffer getSourceBuffer() {
            return this;
        }

        @Override
        public Runnable createDestroyer() {
            return null;
        }

        @Override
        public DisposableReference getReference() {
            return null;
        }

        @Override
        public int size() {
            return size;
        }

    }

    private class PersistentBuffer implements MappableBuffer {

        private final long buffer, alloc;
        private final int size;
        private final Flag<MemoryProp> memProps;
        private final VmaAllocationInfo allocInfo = VmaAllocationInfo.malloc();
        private final ByteBuffer mapping;
        private final int[] staged = new int[2];

        public PersistentBuffer(long buffer, long alloc, int size) {
            this.buffer = buffer;
            this.alloc = alloc;
            this.size = size;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer memPropBuf = stack.mallocInt(1);
                vmaGetAllocationMemoryProperties(allocator, alloc, memPropBuf);
                memProps = Flag.of(memPropBuf.get(0));
                if (!memProps.contains(MemoryProp.HostVisible)) {
                    throw new IllegalArgumentException("Persistent vulkan buffer must be host visible.");
                }
                vmaGetAllocationInfo(allocator, alloc, allocInfo);
                mapping = MemoryUtil.memByteBuffer(allocInfo.pMappedData(), size);
            }
            staged[0] = size;
            staged[1] = 0;
        }

        @Override
        public void map(MappingArena arena) { }

        @Override
        public ByteBuffer map() {
            return mapping.duplicate(); // duplicate required for concurrent access
        }

        @Override
        public void stage(int offset, int size) {
            staged[0] = Math.min(staged[0], offset);
            staged[1] = Math.max(staged[1], offset + size);
        }

        @Override
        public void push() {
            if (staged[0] < staged[1] && memProps.contains(MemoryProp.HostVisible) && !memProps.contains(MemoryProp.HostCoherent)) {
                vmaFlushAllocation(allocator, alloc, staged[0], staged[1] - staged[0]);
                staged[0] = size;
                staged[1] = 0;
            }
        }

        @Override
        public void pull() {
            if (memProps.contains(MemoryProp.HostVisible) && !memProps.contains(MemoryProp.HostCoherent)) {
                vmaInvalidateAllocation(allocator, alloc, 0, VK_WHOLE_SIZE);
                staged[0] = size;
                staged[1] = 0;
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void destroy() {
            allocInfo.free();
            vmaDestroyBuffer(allocator, buffer, alloc);
        }

    }

}
