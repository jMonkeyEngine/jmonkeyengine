package com.jme3.vulkan.buffer.alloc;

import com.jme3.math.FastMath;
import com.jme3.math.IntVector;
import com.jme3.util.natives.*;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.alloc.RemoteBuffer;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.SharingMode;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.ColorSwizzle;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.images.newimage.ImageInfo;
import com.jme3.vulkan.images.newimage.ImageView;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Set;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class VmaMemoryAllocator implements MemoryAllocator, Disposable {

    private final LogicalDevice<?> device;
    private final long allocator;
    private final DisposableReference ref;

    public VmaMemoryAllocator(LogicalDevice<?> device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VmaAllocatorCreateInfo create = VmaAllocatorCreateInfo.calloc(stack)
                    .instance(device.getInstance().getNativeObject())
                    .device(device.getNativeObject())
                    .physicalDevice(device.getPhysicalDevice().getHandle())
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
        for (String ext : enabledExtensions) switch (ext) {
            case "VK_KHR_dedicated_allocation": flags |= VMA_ALLOCATOR_CREATE_KHR_DEDICATED_ALLOCATION_BIT; break;
            case "VK_KHR_bind_memory2": flags |= VMA_ALLOCATOR_CREATE_KHR_BIND_MEMORY2_BIT; break;
            case "VK_KHR_maintenance4": flags |= VMA_ALLOCATOR_CREATE_KHR_MAINTENANCE4_BIT; break;
            case "VK_KHR_maintenance5": flags |= VMA_ALLOCATOR_CREATE_KHR_MAINTENANCE5_BIT; break;
            case "VK_EXT_memory_budget": flags |= VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT; break;
            case "VK_KHR_buffer_device_address": flags |= VMA_ALLOCATOR_CREATE_BUFFER_DEVICE_ADDRESS_BIT; break;
            case "VK_EXT_memory_priority": flags |= VMA_ALLOCATOR_CREATE_EXT_MEMORY_PRIORITY_BIT; break;
            case "VK_AMD_device_coherent_memory": flags |= VMA_ALLOCATOR_CREATE_AMD_DEVICE_COHERENT_MEMORY_BIT; break;
            case "VK_KHR_external_memory_win32": flags |= VMA_ALLOCATOR_CREATE_KHR_EXTERNAL_MEMORY_WIN32_BIT; break;
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

    private VmaBuffer createBuffer(int capacity, Flag<EngineBuffer.Role> roles, int allocUsage, int allocFlags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer bufferPtr = stack.mallocLong(1);
            PointerBuffer allocPtr = stack.mallocPointer(1);
            VkBufferCreateInfo bufCreate = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
                    .size(capacity)
                    .usage(roles.bits());
            VmaAllocationCreateInfo allocCreate = VmaAllocationCreateInfo.calloc(stack)
                    .usage(allocUsage)
                    .flags(allocFlags);
            vmaCreateBuffer(allocator, bufCreate, allocCreate, bufferPtr, allocPtr, null);
            return new VmaBuffer(bufferPtr.get(0), allocPtr.get(0), capacity, roles);
        }
    }

    /* THE FOLLOWING PRESETS ARE RECOMMENDED BY VMA.
       https://gpuopen-librariesandsdks.github.io/VulkanMemoryAllocator/html/usage_patterns.html */

    @Override
    public EngineBuffer createDynamicBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        VmaBuffer buf = createBuffer(capacity, roles.add(EngineBuffer.Role.TransferDst), VMA_MEMORY_USAGE_AUTO,
                VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT | VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT);
        if (!buf.getMemoryProperties().contains(MemoryProp.HostVisible)) {
            return new RemoteBuffer(buf);
        }
        return buf;
    }

    @Override
    public EngineBuffer createReadbackBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        return createBuffer(capacity, roles.add(EngineBuffer.Role.TransferDst), VMA_MEMORY_USAGE_AUTO, VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT);
    }

    @Override
    public EngineBuffer createLocalBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        return createBuffer(capacity, roles.add(EngineBuffer.Role.TransferDst), VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, 0);
    }

    @Override
    public EngineBuffer createStreamingBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        return createBuffer(capacity, roles.add(EngineBuffer.Role.TransferSrc), VMA_MEMORY_USAGE_AUTO_PREFER_HOST, VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT);
    }

    /* IMAGES */

    private VmaImage createImage(ImageInfo info, int allocUsage, int allocFlags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imgCreate = VkImageCreateInfo.calloc(stack)
                .sType$Default()
                .imageType(info.getType().getEnum())
                .format(info.getFormat().getEnum(VulkanEnums.instance))
                .samples(info.getSamples())
                .mipLevels(info.getMipLevels())
                .arrayLayers(info.getArrayLayers())
                .usage(info.getRoles().bits())
                .tiling(info.getTiling().getEnum())
                .initialLayout(info.getLayout().getEnum())
                .sharingMode(SharingMode.Exclusive.getEnum())
                .flags(info.getArrayLayers());
            imgCreate.extent().set(info.getSize().x, info.getSize().y, info.getSize().z);
            VmaAllocationCreateInfo allocCreate = VmaAllocationCreateInfo.calloc(stack)
                .usage(allocUsage)
                .flags(allocFlags);
            LongBuffer imgPtr = stack.mallocLong(1);
            PointerBuffer allocPtr = stack.mallocPointer(1);
            vmaCreateImage(allocator, imgCreate, allocCreate, imgPtr, allocPtr, null);
            return new VmaImage(imgPtr.get(0), allocPtr.get(0), info);
        }
    }

    @Override
    public EngineImage createImage(ImageInfo info) {
        return createImage(info, VMA_MEMORY_USAGE_AUTO, 0);
    }

    private class VmaBuffer implements EngineBuffer {

        private final long buffer, alloc;
        private final Destructor destructor;
        private final Flag<Role> roles;
        private final Flag<MemoryProp> memProps;
        private final DataBuffer mapping;
        private final int capacity;

        public VmaBuffer(long buffer, long alloc, int capacity, Flag<Role> roles) {
            this.buffer = buffer;
            this.alloc = alloc;
            this.destructor = new Destructor(this) { @Override protected void runDestroy() {
                vmaDestroyBuffer(allocator, buffer, alloc);
            }};
            this.roles = roles;
            this.capacity = capacity;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer memPropBuf = stack.mallocInt(1);
                vmaGetAllocationMemoryProperties(allocator, alloc, memPropBuf);
                memProps = Flag.of(memPropBuf.get(0));
                if (memProps.contains(MemoryProp.HostVisible)) {
                    PointerBuffer pmap = stack.mallocPointer(1);
                    vmaMapMemory(allocator, alloc, pmap);
                    mapping = new DataBuffer(MemoryUtil.memByteBuffer(pmap.get(0), capacity));
                } else {
                    mapping = null;
                }
            }
        }

        @Override
        public void update(CommandBuffer cmd) {
            if (mapping != null && !memProps.contains(MemoryProp.HostCoherent)) {
                vmaFlushAllocation(allocator, alloc, 0, mapping.capacity());
            }
        }

        @Override
        public boolean isDeviceAccessible() {
            return false;
        }

        @Override
        public long getHandle() {
            return buffer;
        }

        @Override
        public long getDeviceAddress() {
            return 0;
        }

        @Override
        public void invalidateCache() {
            if (mapping != null && !memProps.contains(MemoryProp.HostCoherent)) {
                vmaInvalidateAllocation(allocator, alloc, 0, mapping.capacity());
            }
        }

        @Override
        public DataBuffer cache() {
            if (mapping == null) {
                throw new IllegalStateException("Memory mapping unavailable.");
            }
            return mapping;
        }

        @Override
        public int capacity() {
            return capacity;
        }

        @Override
        public int getBufferLocalOffset() {
            return 0;
        }

        @Override
        public Flag<Role> getRoles() {
            return roles;
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return memProps;
        }

        @Override
        public Destructor getDestructor() {
            return destructor;
        }

    }

    private class VmaImage implements EngineImage {

        private final long image, alloc;
        private final Destructor destructor;
        private final ImageInfo info;
        private final Flag<MemoryProp> memProps;

        public VmaImage(long image, long alloc, ImageInfo info) {
            this.image = image;
            this.alloc = alloc;
            this.destructor = new Destructor(this) {
                @Override
                protected void runDestroy() {
                    vmaDestroyImage(allocator, image, alloc);
                }
            };
            this.info = info;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer memPropBuf = stack.mallocInt(1);
                vmaGetAllocationMemoryProperties(allocator, alloc, memPropBuf);
                memProps = Flag.of(memPropBuf.get(0));
            }
        }

        @Override
        public long getHandle() {
            return image;
        }

        @Override
        public Type getType() {
            return info.getType();
        }

        @Override
        public Format getFormat() {
            return info.getFormat();
        }

        @Override
        public Layout getLayout() {
            return info.getLayout();
        }

        @Override
        public IntVector getSize() {
            return info.getSize();
        }

        @Override
        public int getSamples() {
            return info.getSamples();
        }

        @Override
        public int getMipLevels() {
            return info.getMipLevels();
        }

        @Override
        public int getArrayLayers() {
            return info.getArrayLayers();
        }

        @Override
        public Tiling getTiling() {
            return info.getTiling();
        }

        @Override
        public Flag<Role> getRoles() {
            return info.getRoles();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return memProps;
        }

        @Override
        public void transitionLayout(CommandBuffer cmd, Layout layout) {
            cmd.cmdTransitionLayout(this, info.getLayout(), layout);
            info.setLayout(layout);
        }

        @Override
        public ImageView createTexture() {
            ImageView.Type type = ImageView.Type.OneDimensional;
            if (info.getType() == EngineImage.Type.TwoDimensional
                    && FastMath.isMultipleOf(info.getArrayLayers(), 6)
                    && info.getCreateFlags().contains(Create.CubeCompatible)) {
                if (info.getArrayLayers() == 6) {
                    type = ImageView.Type.Cube;
                } else {
                    type = ImageView.Type.CubeArray;
                }
            } else if (info.getType() == EngineImage.Type.TwoDimensional) {
                if (info.getArrayLayers() > 1) {
                    type = ImageView.Type.OneDimensionalArray;
                } else {
                    type = ImageView.Type.TwoDimensional;
                }
            } else if (info.getType() == EngineImage.Type.ThreeDimensional) {
                if (info.getArrayLayers() > 1) {
                    type = ImageView.Type.TwoDimensionalArray;
                } else {
                    type = ImageView.Type.ThreeDimensional;
                }
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkImageViewCreateInfo create = VkImageViewCreateInfo.calloc(stack)
                    .sType$Default()
                    .viewType(type.getEnum(VulkanEnums.instance))
                    .image(image)
                    .format(info.getFormat().getEnum(VulkanEnums.instance));
                create.components()
                    .r(ColorSwizzle.Component.R.getEnum(VulkanEnums.instance))
                    .g(ColorSwizzle.Component.G.getEnum(VulkanEnums.instance))
                    .b(ColorSwizzle.Component.B.getEnum(VulkanEnums.instance))
                    .a(ColorSwizzle.Component.A.getEnum(VulkanEnums.instance));
                create.subresourceRange()
                    .baseMipLevel(0)
                    .baseMipLevel(info.getMipLevels())
                    .baseArrayLayer(0)
                    .layerCount(info.getArrayLayers())
                    .aspectMask(info.getFormat().getAspects().getImageAspect().bits());
                LongBuffer ptr = stack.mallocLong(1);
                check(vkCreateImageView(device.getNativeObject(), create, null, ptr),
                        "Failed to create image view");
                long viewHandle = ptr.get(0);
                return new ImageView<>(this, viewHandle, () -> vkDestroyImageView(device.getNativeObject(), viewHandle, null));
            }
        }

        @Override
        public Destructor getDestructor() {
            return destructor;
        }

    }

}
