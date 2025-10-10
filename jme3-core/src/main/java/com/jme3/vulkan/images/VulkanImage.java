package com.jme3.vulkan.images;

import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.SharingMode;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipelines.Access;
import com.jme3.vulkan.pipelines.PipelineStage;
import com.jme3.vulkan.pipelines.states.PipelineState;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.KHRSwapchain;

import static org.lwjgl.vulkan.VK10.*;

public interface VulkanImage extends GpuImage {

    enum Layout implements IntEnum<Layout> {

        Undefined(VK_IMAGE_LAYOUT_UNDEFINED,
                Flag.empty(), PipelineStage.TopOfPipe),
        General(VK_IMAGE_LAYOUT_GENERAL,
                Flag.empty(), Flag.empty()), // not sure how this layout should be treated for transitions
        PreInitialized(VK_IMAGE_LAYOUT_PREINITIALIZED),
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

    enum Load implements IntEnum<Load> {

        Clear(VK_ATTACHMENT_LOAD_OP_CLEAR),
        Load(VK_ATTACHMENT_LOAD_OP_LOAD),
        DontCare(VK_ATTACHMENT_LOAD_OP_DONT_CARE);

        private final int vkEnum;

        Load(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    enum Store implements IntEnum<Store> {

        Store(VK_ATTACHMENT_STORE_OP_STORE),
        DontCare(VK_ATTACHMENT_STORE_OP_DONT_CARE);

        private final int vkEnum;

        Store(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    enum Aspect implements Flag<Aspect> {

        Color(VK_IMAGE_ASPECT_COLOR_BIT),
        Depth(VK_IMAGE_ASPECT_DEPTH_BIT),
        Stencil(VK_IMAGE_ASPECT_STENCIL_BIT),
        MetaData(VK_IMAGE_ASPECT_METADATA_BIT);

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

    void addNativeDependent(NativeReference ref);

}
