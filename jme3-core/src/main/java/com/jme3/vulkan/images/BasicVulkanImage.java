package com.jme3.vulkan.images;

import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.SharingMode;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemoryRegion;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.VulkanEnums;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class BasicVulkanImage extends AbstractNative<Long> implements VulkanImage {

    private final LogicalDevice<?> device;
    private final IntEnum<GpuImage.Type> type;
    private MemoryRegion memory;

    private int width, height, depth;
    private int mipmaps, layers;
    private Flag<ImageUsage> usage;
    private Format format = Format.RGBA8_SRGB;
    private IntEnum<Tiling> tiling = Tiling.Optimal;
    private IntEnum<SharingMode> sharing = SharingMode.Exclusive;
    private Layout layout = Layout.Undefined;

    public BasicVulkanImage(LogicalDevice<?> device, IntEnum<GpuImage.Type> type) {
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
    public IntEnum<GpuImage.Type> getType() {
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
    public void addNativeDependent(DisposableReference ref) {
        this.ref.addDependent(ref);
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyImage(device.getNativeObject(), object, null);
    }

    @Override
    public void transitionLayout(CommandBuffer commands, Layout dstLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            //int[] args = VulkanImage.Layout.getTransferArguments(srcLayout, dstLayout);
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(layout.getEnum())
                    .newLayout(dstLayout.getEnum())
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED) // for transfering queue ownership
                    .image(object)
                    .srcAccessMask(layout.getAccessHint().bits())
                    .dstAccessMask(dstLayout.getAccessHint().bits());
            barrier.subresourceRange()
                    .baseMipLevel(0)
                    .levelCount(mipmaps)
                    .baseArrayLayer(0)
                    .layerCount(layers)
                    .aspectMask(VulkanEnums.imageAspects(format.getAspects()));
            vkCmdPipelineBarrier(commands.getBuffer(), layout.getStageHint().bits(), dstLayout.getStageHint().bits(), 0, null, null, barrier);
            layout = dstLayout;
        }
    }

    public static BasicVulkanImage build(LogicalDevice<?> device, IntEnum<GpuImage.Type> type, Consumer<Builder> config) {
        Builder b = new BasicVulkanImage(device, type).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractNativeBuilder<BasicVulkanImage> {

        private Flag<MemoryProp> mem;

        @Override
        protected BasicVulkanImage construct() {
            VkImageCreateInfo create = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(type.getEnum())
                    .mipLevels(mipmaps)
                    .arrayLayers(layers)
                    .format(format.getEnum())
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
            memory = MemoryRegion.build(device, memReq.size(), m -> {
                m.setFlags(mem);
                m.setUsableMemoryTypes(memReq.memoryTypeBits());
            });
            memory.bind(BasicVulkanImage.this, 0);
            ref = DisposableManager.reference(BasicVulkanImage.this);
            device.getReference().addDependent(ref);
            return BasicVulkanImage.this;
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

        public void setLayout(Layout l) {
            layout = l;
        }

        public void setSharing(IntEnum<SharingMode> sharing) {
            BasicVulkanImage.this.sharing = sharing;
        }

        public Flag<MemoryProp> getMemoryProps() {
            return mem;
        }

        public IntEnum<Layout> getLayout() {
            return layout;
        }

    }

}
