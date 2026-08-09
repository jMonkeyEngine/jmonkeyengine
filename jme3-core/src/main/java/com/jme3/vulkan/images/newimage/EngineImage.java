package com.jme3.vulkan.images.newimage;

import com.jme3.math.Vector3i;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.formats.EnumInterpreter;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.ImageRoles;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.pipeline.Access;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.KHRSharedPresentableImage;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;

import static org.lwjgl.vulkan.VK10.*;

public interface EngineImage {

    enum Type implements IntEnum<Type> {

        OneDemensional(VK_IMAGE_TYPE_1D),
        TwoDemensional(VK_IMAGE_TYPE_2D),
        ThreeDemensional(VK_IMAGE_TYPE_3D);

        private final int vkEnum;

        Type(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

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
        PresentSrc(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR),
        SharedPresent(KHRSharedPresentableImage.VK_IMAGE_LAYOUT_SHARED_PRESENT_KHR);

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
            return interpreter.getImageLoadEnum(this);
        }

    }

    enum Store {

        Store, DontCare;

        public int getEnum(EnumInterpreter interpreter) {
            return interpreter.getImageStoreEnum(this);
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

    long getHandle();

    Type getType();

    Format getFormat();

    Layout getLayout();

    Vector3i getSize();

    int getSamples();

    int getMipLevels();

    int getArrayLayers();

    Tiling getTiling();

    Flag<ImageRoles> getRoles();

    Flag<MemoryProp> getMemoryProperties();

    void transitionLayout(CommandBuffer cmd, Layout layout);

}
