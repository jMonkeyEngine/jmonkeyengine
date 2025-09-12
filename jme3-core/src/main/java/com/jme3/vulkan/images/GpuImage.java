package com.jme3.vulkan.images;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.AbstractNative;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.SharingMode;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class GpuImage extends AbstractNative<Long> implements VulkanImage {

    private final LogicalDevice<?> device;
    private final IntEnum<Image.Type> type;
    private MemoryRegion memory;

    private int width, height, depth;
    private int mipmaps, layers;
    private Flag<ImageUsage> usage;
    private Format format = Format.RGBA8_SRGB;
    private IntEnum<Tiling> tiling = Tiling.Optimal;
    private IntEnum<SharingMode> sharing = SharingMode.Exclusive;

    public GpuImage(LogicalDevice<?> device, IntEnum<Image.Type> type) {
        this.device = device;
        this.type = type;
        width = height = depth = 1;
        mipmaps = layers = 1;
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return device;
    }

    @Override
    public long getId() {
        return object;
    }

    @Override
    public IntEnum<Image.Type> getType() {
        return type;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public int getMipmaps() {
        return mipmaps;
    }

    @Override
    public int getLayers() {
        return layers;
    }

    @Override
    public Flag<ImageUsage> getUsage() {
        return usage;
    }

    @Override
    public Format getFormat() {
        return format;
    }

    @Override
    public IntEnum<Tiling> getTiling() {
        return tiling;
    }

    @Override
    public IntEnum<SharingMode> getSharingMode() {
        return sharing;
    }

    @Override
    public void addNativeDependent(NativeReference ref) {
        this.ref.addDependent(ref);
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyImage(device.getNativeObject(), object, null);
    }

    // todo: replace with transition autocloseable and deprecate this method
    public void transitionLayout(CommandBuffer commands, Layout srcLayout, Layout dstLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] args = VulkanImage.Layout.getTransferArguments(srcLayout, dstLayout);
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(srcLayout.getEnum())
                    .newLayout(dstLayout.getEnum())
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED) // for transfering queue ownership
                    .image(object)
                    .srcAccessMask(args[0])
                    .dstAccessMask(args[1]);
            barrier.subresourceRange()
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1)
                    .aspectMask(format.getAspects().bits());
            vkCmdPipelineBarrier(commands.getBuffer(), args[2], args[3], 0, null, null, barrier);
        }
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractNative.Builder<GpuImage> {

        private Flag<MemoryProp> mem;
        private IntEnum<Layout> layout = Layout.Undefined;

        @Override
        protected void build() {
            VkImageCreateInfo create = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(type.getEnum())
                    .mipLevels(mipmaps)
                    .arrayLayers(layers)
                    .format(format.getVkEnum())
                    .tiling(tiling.getEnum())
                    .initialLayout(layout.getEnum())
                    .usage(usage.bits())
                    .samples(VK_SAMPLE_COUNT_1_BIT) // todo: multisampling
                    .sharingMode(sharing.getEnum());
            create.extent().width(width).height(height).depth(depth);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateImage(device.getNativeObject(), create, null, idBuf),
                    "Failed to create image.");
            object = idBuf.get(0);
            VkMemoryRequirements memReq = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device.getNativeObject(), object, memReq);
            memory = new MemoryRegion(device, memReq.size(), mem, memReq.memoryTypeBits());
            memory.bind(GpuImage.this, 0);
            ref = Native.get().register(GpuImage.this);
            device.getNativeReference().addDependent(ref);
        }

        public void setWidth(int w) {
            width = w;
        }

        public void setHeight(int h) {
            height = h;
        }

        public void setDepth(int d) {
            depth = d;
        }

        public void setSize(int w) {
            width = w;
        }

        public void setSize(int w, int h) {
            width = w;
            height = h;
        }

        public void setSize(int w, int h, int d) {
            width = w;
            height = h;
            depth = d;
        }

        public void setNumMipmaps(int m) {
            mipmaps = m;
        }

        public void setNumLayers(int l) {
            layers = l;
        }

        public void setUsage(Flag<ImageUsage> u) {
            usage = u;
        }

        public void setMemoryProps(Flag<MemoryProp> m) {
            this.mem = m;
        }

        public void setFormat(Format f) {
            format = f;
        }

        public void setTiling(IntEnum<Tiling> t) {
            tiling = t;
        }

        public void setLayout(IntEnum<Layout> l) {
            this.layout = l;
        }

        public Flag<MemoryProp> getMemoryProps() {
            return mem;
        }

        public IntEnum<Layout> getLayout() {
            return layout;
        }

    }

}
