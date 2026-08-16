package com.jme3.vulkan.images;

import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.buffer.SharingMode;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public interface VulkanImage extends GpuImage {

    @Deprecated
    LogicalDevice<?> getDevice();

    Flag<EngineImage.Role> getUsage();

    IntEnum<EngineImage.Tiling> getTiling();

    IntEnum<SharingMode> getSharingMode();

    @Deprecated
    void addNativeDependent(DisposableReference ref);

    /**
     * Transitions this image to {@code layout}, if it is not already in that layout.
     * Operations often require images to be of a particular layout.
     *
     * @param stack memory stack
     * @param cmd command buffer
     * @param layout layout to transition to
     */
    void transitionLayout(MemoryStack stack, CommandBuffer cmd, EngineImage.Layout layout);

    default void transitionLayout(CommandBuffer cmd, EngineImage.Layout layout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            transitionLayout(stack, cmd, layout);
        }
    }

    /**
     * Copies the contents of this image to {@code dst}. The dimensions of the images need not
     * match. If the number of samples in this image is greater than 1 and {@code dst} has exactly
     * 1 samples, then the multisampled contents of this image will be resolved into {@code dst}.
     * Otherwise the number of samples in each image must match and the contents will be copied
     * normally.
     *
     * @param stack memory stack
     * @param cmd command buffer
     * @param dst copy destination image
     */
    default void copyTo(MemoryStack stack, CommandBuffer cmd, VulkanImage dst, Flag<EngineImage.Aspect> aspects) {
        int w = Math.min(getWidth(), dst.getWidth());
        int h = Math.min(getHeight(), dst.getHeight());
        int d = Math.min(getDepth(), dst.getDepth());
        transitionLayout(cmd, EngineImage.Layout.TransferSrcOptimal);
        dst.transitionLayout(cmd, EngineImage.Layout.TransferDstOptimal);
        aspects = aspects.and(getFormat().getAspects().getImageAspect(), dst.getFormat().getAspects().getImageAspect());
        if (getSamples() > 1 && dst.getSamples() == 1) {
            // resolve multisampled data into dst
            VkImageResolve resolve = VkImageResolve.calloc(stack);
            resolve.extent().set(w, h, d);
            resolve.srcSubresource().aspectMask(aspects.bits());
            resolve.dstSubresource().aspectMask(aspects.bits());
            vkCmdResolveImage(cmd.getBuffer(), getId(), cmd.getKnownLayout(this).getEnum(),
                    dst.getId(), cmd.getKnownLayout(dst).getEnum(), resolve);
        } else if (getSamples() == dst.getSamples()) {
            VkImageCopy.Buffer copy = VkImageCopy.calloc(1, stack);
            copy.extent().set(w, h, d);
            copy.srcSubresource().aspectMask(aspects.bits());
            copy.dstSubresource().aspectMask(aspects.bits());
            vkCmdCopyImage(cmd.getBuffer(), getId(), cmd.getKnownLayout(this).getEnum(),
                    dst.getId(), cmd.getKnownLayout(dst).getEnum(), copy);
        } else {
            throw new UnsupportedOperationException("Unable to copy from image with " + getSamples() + " samples to an " +
                    "image with " + dst.getSamples() + " samples. Must be from N to 1 or from N to N.");
        }
    }

}
