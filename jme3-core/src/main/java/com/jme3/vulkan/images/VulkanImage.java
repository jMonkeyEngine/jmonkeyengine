package com.jme3.vulkan.images;

import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.buffers.SharingMode;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.formats.EnumInterpreter;
import com.jme3.vulkan.pipeline.Access;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageResolve;

import static org.lwjgl.vulkan.VK10.*;

public interface VulkanImage extends GpuImage {

    enum Layout implements IntEnum<Layout> {

        Undefined(VK_IMAGE_LAYOUT_UNDEFINED,
                Flag.empty(), PipelineStage.TopOfPipe),
        General(VK_IMAGE_LAYOUT_GENERAL,
                Flag.empty(), Flag.empty()), // not sure how this layout should be treated for transitions
        PreInitialized(VK_IMAGE_LAYOUT_PREINITIALIZED),
        ReadOnlyOptimal(VK14.VK_IMAGE_LAYOUT_READ_ONLY_OPTIMAL),
        ColorAttachmentOptimal(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                Flag.of(Access.ColorAttachmentRead, Access.ColorAttachmentWrite), PipelineStage.ColorAttachmentOutput),
        DepthStencilAttachmentOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                Flag.of(Access.DepthStencilAttachmentRead, Access.DepthStencilAttachmentWrite), PipelineStage.EarlyFragmentTests),
        DepthStencilReadOnlyOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                Access.DepthStencilAttachmentRead, PipelineStage.EarlyFragmentTests),
        TransferSrcOptimal(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                Access.TransferRead, PipelineStage.Transfer),
        TransferDstOptimal(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                Access.TransferWrite, PipelineStage.Transfer),
        ShaderReadOnlyOptimal(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                Access.ShaderRead, PipelineStage.FragmentShader),
        PresentSrc(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        private final int vkEnum;
        private final Flag<Access> access;
        private final Flag<PipelineStage> stage;

        Layout(int vkEnum) {
            this.vkEnum = vkEnum;
            access = Flag.empty();
            stage = Flag.empty();
        }

        Layout(int vkEnum, Flag<Access> access, Flag<PipelineStage> stage) {
            this.vkEnum = vkEnum;
            this.access = access;
            this.stage = stage;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

        public Flag<Access> getAccessHint() {
            return access;
        }

        public Flag<PipelineStage> getStageHint() {
            return stage;
        }

        @Deprecated
        @SuppressWarnings("SwitchStatementWithTooFewBranches")
        public static int[] getTransferArguments(Layout srcLayout, Layout dstLayout) {
            // output array format: {srcAccessMask, dstAccessMask, srcStage, dstStage}
            switch (srcLayout) {
                case Undefined: switch (dstLayout) {
                    case TransferDstOptimal: return new int[] {
                            0, VK_ACCESS_TRANSFER_WRITE_BIT, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                            VK_PIPELINE_STAGE_TRANSFER_BIT};
                    case DepthStencilAttachmentOptimal: return new int[] {
                            0, VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                            VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT};
                } break;
                case TransferDstOptimal: switch (dstLayout) {
                    case ShaderReadOnlyOptimal: return new int[] {
                            VK_ACCESS_TRANSFER_WRITE_BIT, VK_ACCESS_SHADER_READ_BIT,
                            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT};
                } break;
            }
            throw new UnsupportedOperationException("Unsupported layout transition: " + srcLayout + " to " + dstLayout);
        }

    }

    enum Tiling implements IntEnum<Tiling> {

        Optimal(VK_IMAGE_TILING_OPTIMAL),
        Linear(VK_IMAGE_TILING_LINEAR);

        private final int vkEnum;

        Tiling(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    enum Load {

        Clear, Load, DontCare;

        public int getEnum(EnumInterpreter interpreter) {
            return interpreter.getLoadEnum(this);
        }

    }

    enum Store {

        Store, DontCare;

        public int getEnum(EnumInterpreter interpreter) {
            return interpreter.getStoreEnum(this);
        }

    }

    enum Aspect implements Flag<Aspect> {

        Color(VK_IMAGE_ASPECT_COLOR_BIT),
        Depth(VK_IMAGE_ASPECT_DEPTH_BIT),
        Stencil(VK_IMAGE_ASPECT_STENCIL_BIT),
        MetaData(VK_IMAGE_ASPECT_METADATA_BIT),
        DepthStencil(VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT);

        private final int bits;

        Aspect(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    LogicalDevice<?> getDevice();

    Flag<ImageUsage> getUsage();

    IntEnum<Tiling> getTiling();

    IntEnum<SharingMode> getSharingMode();

    void addNativeDependent(DisposableReference ref);

    /**
     * Transitions this image to {@code layout}, if it is not already in that layout.
     * Operations often require images to be of a particular layout.
     *
     * @param stack memory stack
     * @param cmd command buffer
     * @param layout layout to transition to
     */
    void transitionLayout(MemoryStack stack, CommandBuffer cmd, Layout layout);

    default void transitionLayout(CommandBuffer cmd, Layout layout) {
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
    default void copyTo(MemoryStack stack, CommandBuffer cmd, VulkanImage dst, Flag<VulkanImage.Aspect> aspects) {
        int w = Math.min(getWidth(), dst.getWidth());
        int h = Math.min(getHeight(), dst.getHeight());
        int d = Math.min(getDepth(), dst.getDepth());
        transitionLayout(cmd, Layout.TransferSrcOptimal);
        dst.transitionLayout(cmd, Layout.TransferDstOptimal);
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
