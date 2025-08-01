package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.images.ImageView;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class Swapchain implements Native<Long> {

    private final LogicalDevice device;
    private final Surface surface;
    private final NativeReference ref;
    private final List<SwapchainImage> images = new ArrayList<>();
    private SwapchainUpdater updater;
    private Extent2 extent;
    private int format;
    private long id = VK_NULL_HANDLE;

    public Swapchain(LogicalDevice device, Surface surface, SwapchainSupport support) {
        this.device = device;
        this.surface = surface;
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
        surface.getNativeReference().addDependent(ref);
        reload(support);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> KHRSwapchain.vkDestroySwapchainKHR(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return null;
    }

    public void reload(SwapchainSupport support) {
        assert support.isSupported() : "Swapchain for device is not supported.";
        if (id != VK_NULL_HANDLE) {
            createNativeDestroyer().run();
            id = VK_NULL_HANDLE;
        }
        images.clear();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.calloc(stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    device.getPhysicalDevice().getDevice(), surface.getNativeObject(), caps);
            VkSurfaceFormatKHR fmt = support.selectFormat();
            format = fmt.format();
            VkExtent2D ext = support.selectExtent();
            extent = new Extent2(ext);
            VkSwapchainCreateInfoKHR create = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface.getNativeObject())
                    .minImageCount(support.selectImageCount())
                    .imageFormat(format)
                    .imageColorSpace(fmt.colorSpace())
                    .imageExtent(ext)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .preTransform(caps.currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(support.selectMode())
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);
            IntBuffer concurrent = device.getPhysicalDevice().getQueueFamilies().getSwapchainConcurrentBuffers(stack);
            if (concurrent != null) {
                create.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .queueFamilyIndexCount(concurrent.limit())
                        .pQueueFamilyIndices(concurrent);
            } else {
                create.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }
            LongBuffer ptr = stack.mallocLong(1);
            check(KHRSwapchain.vkCreateSwapchainKHR(device.getNativeObject(), create, null, ptr),
                    "Failed to create swapchain.");
            id = ptr.get(0);
            System.out.println("swapchain handle: " + id);
            LongBuffer imgs = enumerateBuffer(stack, stack::mallocLong, (c, b) ->
                    check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getNativeObject(), id, c, b),
                            "Failed to get swapchain images."));
            Objects.requireNonNull(imgs, "Swapchain contains no images.");
            for (int i = 0; i < imgs.limit(); i++) {
                images.add(new SwapchainImage(device, imgs.get(i)));
            }
        }
        ref.refresh(); // refresh the native destroyer
    }

    public void createFrameBuffers(RenderPass compat) {
        for (SwapchainImage img : images) {
            img.createFrameBuffer(compat);
        }
    }

    public SwapchainImage acquireNextImage(SwapchainUpdater updater, Semaphore semaphore, Fence fence, long timeout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer i = stack.mallocInt(1);
            int code = KHRSwapchain.vkAcquireNextImageKHR(device.getNativeObject(), id,
                            TimeUnit.MILLISECONDS.toNanos(timeout), Native.getId(semaphore), Native.getId(fence), i);
            if (updater.swapchainOutOfDate(this, code)) {
                return null;
            }
            return images.get(i.get(0));
        }
    }

    public void present(Queue presentQueue, SwapchainImage image, Semaphore wait) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR info = VkPresentInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(id))
                    .pImageIndices(stack.ints(images.indexOf(image)));
            if (wait != null) {
                info.pWaitSemaphores(stack.longs(wait.getNativeObject()));
            }
            check(KHRSwapchain.vkQueuePresentKHR(presentQueue.getQueue(), info));
        }
    }

    public LogicalDevice getDevice() {
        return device;
    }

    public Surface getSurface() {
        return surface;
    }

    public List<SwapchainImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public Extent2 getExtent() {
        return extent;
    }

    public int getFormat() {
        return format;
    }

    public class SwapchainImage implements Image {

        private final LogicalDevice device;
        private final NativeReference ref;
        private final long id;
        private final ImageView view;
        private FrameBuffer frameBuffer;

        private SwapchainImage(LogicalDevice device, long id) {
            this.device = device;
            this.id = id;
            ref = Native.get().register(this);
            Swapchain.this.ref.addDependent(ref);
            view = createView(VK_IMAGE_VIEW_TYPE_2D, VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1);
        }

        @Override
        public ImageView createView(VkImageViewCreateInfo create) {
            return new ImageView(this, create);
        }

        @Override
        public LogicalDevice getDevice() {
            return device;
        }

        @Override
        public int getType() {
            return VK_IMAGE_TYPE_2D;
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
            return 1;
        }

        @Override
        public int getFormat() {
            return format;
        }

        @Override
        public Long getNativeObject() {
            return id;
        }

        @Override
        public Runnable createNativeDestroyer() {
            return () -> {};
        }

        @Override
        public void prematureNativeDestruction() {}

        @Override
        public NativeReference getNativeReference() {
            return ref;
        }

        public void createFrameBuffer(RenderPass compat) {
            this.frameBuffer = new FrameBuffer(getDevice(), compat, extent.x, extent.y, 1, view);
        }

        public FrameBuffer getFrameBuffer() {
            return frameBuffer;
        }

    }

}
