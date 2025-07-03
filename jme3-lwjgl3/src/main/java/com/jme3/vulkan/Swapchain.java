package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
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
    private final VkExtent2D extent;
    private final int format;
    private LongBuffer id = MemoryUtil.memAllocLong(1);
    private VkSurfaceCapabilitiesKHR caps;

    public Swapchain(LogicalDevice device, Surface surface, SwapchainSupport support) {
        assert support.isSupported() : "Swapchain for device is not supported.";
        this.device = device;
        this.surface = surface;
        caps = VkSurfaceCapabilitiesKHR.create();
        KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                device.getPhysicalDevice().getDevice(), surface.getNativeObject(), caps);
        VkSurfaceFormatKHR fmt = support.selectFormat();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            format = fmt.format();
            extent = support.selectExtent();
            VkSwapchainCreateInfoKHR create = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface.getNativeObject())
                    .minImageCount(support.selectImageCount())
                    .imageFormat(format)
                    .imageColorSpace(fmt.colorSpace())
                    .imageExtent(extent)
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
            check(KHRSwapchain.vkCreateSwapchainKHR(device.getNativeObject(), create, null, id),
                    "Failed to create swapchain.");
            LongBuffer imgs = enumerateBuffer(stack, stack::mallocLong, (c, b) ->
                    KHRSwapchain.vkGetSwapchainImagesKHR(device.getNativeObject(), id.get(0), c, b));
            Objects.requireNonNull(imgs, "Swapchain contains no images.");
            for (int i = 0; i < imgs.limit(); i++) {
                images.add(new SwapchainImage(device, imgs.get(i)));
            }
        }
        fmt.close();
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
        surface.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id != null ? id.get(0) : null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            KHRSwapchain.vkDestroySwapchainKHR(device.getNativeObject(), id.get(0), null);
            MemoryUtil.memFree(id);
            caps.free();
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = null;
        caps = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return null;
    }

    public SwapchainImage acquireNextImage(Semaphore semaphore, Fence fence, long timeout) {
        IntBuffer i = MemoryUtil.memAllocInt(1);
        check(KHRSwapchain.vkAcquireNextImageKHR(device.getNativeObject(), id.get(0),
                TimeUnit.MILLISECONDS.toNanos(timeout), Native.getId(semaphore), Native.getId(fence), i),
                "Failed to acquire next swapchain image.");
        SwapchainImage img = getImage(i.get(0));
        MemoryUtil.memFree(i);
        return img;
    }

    public void present(Queue presentQueue, SwapchainImage image, Semaphore wait) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR info = VkPresentInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .swapchainCount(1)
                    .pSwapchains(id)
                    .pImageIndices(stack.ints(images.indexOf(image)));
            if (wait != null) {
                info.pWaitSemaphores(stack.longs(wait.getNativeObject()));
            }
            check(KHRSwapchain.vkQueuePresentKHR(presentQueue.getQueue(), info));
        }
    }

    public List<ImageView> createViews() {
        List<ImageView> result = new ArrayList<>(images.size());
        for (SwapchainImage img : images) {
            VkImageViewCreateInfo create = VkImageViewCreateInfo.create()
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(img.getNativeObject())
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format);
            create.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            create.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);
            result.add(new ImageView(img, create));
            create.free();
        }
        return result;
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

    public SwapchainImage getImage(long id) {
        return images.stream().filter(i -> i.getNativeObject() == id)
                .findAny().orElseThrow(() -> new NoSuchElementException("Image not found."));
    }

    public VkExtent2D getExtent() {
        return extent;
    }

    public int getFormat() {
        return format;
    }

    public static class SwapchainImage extends Image {

        private SwapchainImage(LogicalDevice device, long id) {
            super(device, id);
        }

        @Override
        public Runnable createNativeDestroyer() {
            return () -> {
                MemoryUtil.memFree(id);
                MemoryUtil.memFree(memory);
            };
        }

    }

}
