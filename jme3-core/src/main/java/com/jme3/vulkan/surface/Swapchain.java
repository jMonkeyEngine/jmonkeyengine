package com.jme3.vulkan.surface;

import com.jme3.texture.ImageView;
import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.*;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.Queue;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.GpuImage;
import com.jme3.vulkan.images.ImageUsage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.pipeline.framebuffer.VulkanFrameBuffer;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.util.Extent2;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class Swapchain extends AbstractNative<Long> {

    public static final String EXTENSION = KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;

    public enum PresentMode implements IntEnum<PresentMode> {

        FirstInFirstOut(KHRSurface.VK_PRESENT_MODE_FIFO_KHR),
        FirstInFirstOutRelaxed(KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR),
        Immediate(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR),
        Mailbox(KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR);

        private final int vkEnum;

        PresentMode(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    private final LogicalDevice<?> device;
    private final Surface surface;
    private final List<PresentImage> images = new ArrayList<>();
    private final Consumer<Builder> config;
    private Extent2 extent;
    private Format format;
    private int imageLayers = 1;
    private Flag<ImageUsage> imageUsage = ImageUsage.ColorAttachment;
    private Consumer<Swapchain> updater;
    private boolean updateNeeded = false;

    public Swapchain(LogicalDevice<?> device, Surface surface, Consumer<Builder> config) {
        this.device = device;
        this.surface = surface;
        this.config = config;
        this.object = VK_NULL_HANDLE;
        ref = DisposableManager.reference(this);
        device.getReference().addDependent(ref);
        surface.getNativeReference().addDependent(ref);
    }

    @Override
    public Runnable createDestroyer() {
        return () -> KHRSwapchain.vkDestroySwapchainKHR(device.getNativeObject(), object, null);
    }

    public void createFrameBuffers(RenderPass compat, VulkanImageView depthStencil) {
        for (PresentImage img : images) {
            img.createFrameBuffer(compat, depthStencil);
        }
    }

    public PresentImage acquireNextImage(MemoryStack stack, Semaphore signal, Fence fence, long timeoutMillis) {
        IntBuffer imageIndex = stack.mallocInt(1);
        int code = KHRSwapchain.vkAcquireNextImageKHR(device.getNativeObject(), object,
                TimeUnit.MILLISECONDS.toNanos(timeoutMillis), signal != null ? signal.getSemaphoreObject() : VK_NULL_HANDLE,
                fence != null ? fence.getNativeObject() : VK_NULL_HANDLE, imageIndex);
        if (updateNeeded || code == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || code == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            updateNeeded = false;
            if (updater == null) {
                throw new IllegalStateException("Swapchain is out of date.");
            }
            updater.accept(this);
            return null;
        }
        if (code != VK_SUCCESS) {
            throw new RuntimeException("Failed to acquire next swapchain image (exit code " + code + ")");
        }
        return images.get(imageIndex.get(0));
    }

    public PresentImage acquireNextImage(Semaphore signal, Fence fence, long timeoutMillis) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return acquireNextImage(stack, signal, fence, timeoutMillis);
        }
    }

    public void present(Queue presentQueue, PresentImage image, Semaphore... waits) {
        int imageIndex = images.indexOf(image);
        if (imageIndex < 0) {
            throw new IllegalArgumentException("Image does not belong to this swapchain.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR info = VkPresentInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(object))
                    .pImageIndices(stack.ints(imageIndex));
            if (waits.length > 0) {
                LongBuffer waitBuf = stack.mallocLong(waits.length);
                for (Semaphore w : waits) {
                    waitBuf.put(w.getSemaphoreObject());
                }
                info.pWaitSemaphores(waitBuf.flip());
            }
            check(KHRSwapchain.vkQueuePresentKHR(presentQueue.getQueue(), info));
        }
    }

    public void update() {
        update(null);
    }

    public void update(Consumer<Builder> update) {
        Builder b = new Builder();
        config.accept(b);
        if (update != null) {
            update.accept(b);
        }
        b.build();
    }

    public void setUpdateNeeded() {
        updateNeeded = true;
    }

    public void setUpdater(Consumer<Swapchain> updater) {
        this.updater = updater;
    }

    public LogicalDevice getDevice() {
        return device;
    }

    public Surface getSurface() {
        return surface;
    }

    public List<PresentImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public Extent2 getExtent() {
        return extent;
    }

    public Format getFormat() {
        return format;
    }

    public int getImageLayers() {
        return imageLayers;
    }

    public Flag<ImageUsage> getImageUsage() {
        return imageUsage;
    }

    public static Swapchain build(LogicalDevice<?> device, Surface surface, Consumer<Builder> config) {
        Builder b = new Swapchain(device, surface, config).new Builder();
        config.accept(b);
        return b.build();
    }

    public class PresentImage implements VulkanImage {

        private final LogicalDevice<?> device;
        private final long id;
        private final VulkanImageView colorView;
        private VulkanFrameBuffer frameBuffer;

        private PresentImage(LogicalDevice<?> device, long id) {
            this.device = device;
            this.id = id;
            colorView = VulkanImageView.build(this, ImageView.Type.TwoDemensional, v -> v.setLayerCount(imageLayers));
        }

        @Override
        public LogicalDevice<?> getDevice() {
            return device;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public IntEnum<GpuImage.Type> getType() {
            return GpuImage.Type.TwoDemensional;
        }

        @Override
        public int getWidth() {
            return extent.x;
        }

        @Override
        public int getHeight() {
            return extent.y;
        }

        @Override
        public int getDepth() {
            return 1; // swapchain images are always 2D
        }

        @Override
        public int getMipmaps() {
            return 1; // swapchain images always have only 1 mipmap
        }

        @Override
        public int getLayers() {
            return imageLayers;
        }

        @Override
        public Flag<ImageUsage> getUsage() {
            return imageUsage;
        }

        @Override
        public Format getFormat() {
            return format;
        }

        @Override
        public VulkanImage.Tiling getTiling() {
            return VulkanImage.Tiling.Optimal;
        }

        @Override
        public IntEnum<SharingMode> getSharingMode() {
            return SharingMode.Exclusive;
        }

        @Override
        public void addNativeDependent(DisposableReference ref) {
            Swapchain.this.ref.addDependent(ref);
        }

        @Override
        public void transitionLayout(CommandBuffer commands, Layout dstLayout) {
            throw new UnsupportedOperationException("Cannot transition swapchain image.");
        }

        public void createFrameBuffer(RenderPass compat, VulkanImageView depthStencil) {
            frameBuffer = new VulkanFrameBuffer(getDevice(), compat, extent.x, extent.y, 1);
            frameBuffer.addColorTarget(colorView);
            frameBuffer.setDepthTarget(depthStencil);
        }

        public VulkanFrameBuffer getFrameBuffer() {
            return frameBuffer;
        }

    }

    public class Builder extends AbstractNativeBuilder<Swapchain> {

        private final VkSurfaceCapabilitiesKHR caps;
        private final VkSurfaceFormatKHR.Buffer formats;
        private final IntBuffer modes;
        private final Collection<Queue> queues = new ArrayList<>();

        private VkSurfaceFormatKHR selectedFormat;
        private VkExtent2D selectedExtent;
        private IntEnum<PresentMode> selectedMode;
        private Integer selectedImageCount;

        private Swapchain base;

        public Builder() {
            caps = VkSurfaceCapabilitiesKHR.malloc(stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getPhysicalDevice().getDeviceHandle(), surface.getNativeObject(), caps);
            formats = enumerateBuffer(stack, n -> VkSurfaceFormatKHR.malloc(n, stack), (count, buffer)
                    -> KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice().getDeviceHandle(),
                            surface.getNativeObject(), count, buffer));
            modes = enumerateBuffer(stack, stack::mallocInt, (count, buffer) ->
                    KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device.getPhysicalDevice().getDeviceHandle(),
                            surface.getNativeObject(), count, buffer));
            if (formats == null || modes == null) {
                throw new UnsupportedOperationException("Swapchains are not supported by the device.");
            }
        }

        @Override
        protected Swapchain construct() {
            if (selectedFormat == null) {
                throw new IllegalStateException("Format not selected.");
            }
            if (selectedMode == null) {
                throw new IllegalStateException("Mode not selected.");
            }
            if (selectedExtent == null) {
                selectExtentByWindow();
            }
            if (selectedImageCount == null) {
                throw new IllegalStateException("Image count not selected.");
            }
            if (object != VK_NULL_HANDLE) {
                createDestroyer().run();
                object = VK_NULL_HANDLE;
            }
            VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.calloc(stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    device.getPhysicalDevice().getDeviceHandle(), surface.getNativeObject(), caps);
            format = Format.byVkEnum(selectedFormat.format());
            extent = new Extent2(selectedExtent);
            VkSwapchainCreateInfoKHR create = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface.getNativeObject())
                    .minImageCount(selectedImageCount)
                    .imageFormat(format.getEnum())
                    .imageColorSpace(selectedFormat.colorSpace())
                    .imageExtent(selectedExtent)
                    .imageArrayLayers(imageLayers)
                    .imageUsage(imageUsage.bits())
                    .preTransform(caps.currentTransform())
                    .compositeAlpha(CompositeAlpha.Opaque.bits())
                    .presentMode(selectedMode.getEnum())
                    .clipped(true);
            if (base != null) {
                create.oldSwapchain(base.getNativeObject());
            } else {
                create.oldSwapchain(VK_NULL_HANDLE);
            }
            if (queues.size() > 1) {
                IntBuffer concurrent = stack.mallocInt(queues.size());
                for (Queue q : queues) {
                    concurrent.put(q.getFamilyIndex());
                }
                concurrent.flip();
                create.imageSharingMode(SharingMode.Concurrent.getEnum())
                        .queueFamilyIndexCount(queues.size())
                        .pQueueFamilyIndices(concurrent);
            } else {
                create.imageSharingMode(SharingMode.Exclusive.getEnum());
            }
            LongBuffer ptr = stack.mallocLong(1);
            check(KHRSwapchain.vkCreateSwapchainKHR(device.getNativeObject(), create, null, ptr),
                    "Failed to create swapchain.");
            object = ptr.get(0);
            System.out.println("swapchain handle: " + object);
            LongBuffer imgs = enumerateBuffer(stack, stack::mallocLong, (c, b) ->
                    check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getNativeObject(), object, c, b),
                            "Failed to get swapchain images."));
            Objects.requireNonNull(imgs, "Swapchain contains no images.");
            for (int i = 0; i < imgs.limit(); i++) {
                images.add(new PresentImage(device, imgs.get(i)));
            }
            ref.refresh();
            return Swapchain.this;
        }

        public void setBaseSwapchain(Swapchain base) {
            this.base = base;
        }

        public void addQueue(Queue queue) {
            queues.add(queue);
        }

        public VkSurfaceFormatKHR selectFormat(FormatColorSpace... preferredFormats) {
            for (VkSurfaceFormatKHR f : formats) {
                for (int i = 0; i < preferredFormats.length; i += 2) {
                    if (f.format() == preferredFormats[i].format.getEnum()
                            && f.colorSpace() == preferredFormats[i].colorSpace.getEnum()) {
                        return (selectedFormat = f);
                    }
                }
            }
            return (selectedFormat = formats.get(0));
        }

        @SafeVarargs
        public final IntEnum<PresentMode> selectMode(IntEnum<PresentMode>... preferredModes) {
            for (IntEnum<PresentMode> m : preferredModes) {
                for (int i = 0; i < modes.limit(); i++) {
                    if (modes.get(i) == m.getEnum()) {
                        return (selectedMode = m);
                    }
                }
            }
            return (selectedMode = PresentMode.FirstInFirstOut);
        }

        public VkExtent2D selectExtentByWindow() {
            if (caps.currentExtent().width() != UINT32_MAX) {
                return (selectedExtent = caps.currentExtent());
            }
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(surface.getWindowHandle(), width, height);
            selectedExtent = VkExtent2D.malloc(stack);
            selectedExtent.width(Math.min(Math.max(width.get(0), caps.minImageExtent().width()), caps.maxImageExtent().width()));
            selectedExtent.height(Math.min(Math.max(width.get(0), caps.minImageExtent().height()), caps.maxImageExtent().height()));
            return selectedExtent;
        }

        public int selectImageCount(int preferredCount) {
            if (preferredCount < caps.minImageCount()) {
                preferredCount = caps.minImageCount();
            } else if (caps.maxImageCount() > 0 && preferredCount > caps.maxImageCount()) {
                preferredCount = caps.maxImageCount();
            }
            return (selectedImageCount = preferredCount);
        }

        public void setImageLayers(int layers) {
            Swapchain.this.imageLayers = layers;
        }

        public void setImageUsage(Flag<ImageUsage> usage) {
            Swapchain.this.imageUsage = usage;
        }

        public int getMinImageCount() {
            return caps.minImageCount();
        }

        public int getMaxImageCount() {
            return caps.maxImageCount();
        }

    }

    public static FormatColorSpace format(Format format, IntEnum<ColorSpace> colorSpace) {
        return new FormatColorSpace(format, colorSpace);
    }

    public static class FormatColorSpace {

        public final Format format;
        public final IntEnum<ColorSpace> colorSpace;

        public FormatColorSpace(Format format, IntEnum<ColorSpace> colorSpace) {
            this.format = format;
            this.colorSpace = colorSpace;
        }

    }

}
