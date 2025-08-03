package com.jme3.vulkan.images;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public interface Image extends Native<Long> {

    enum Format {

        RGBA32SFloat(VK_FORMAT_R32G32B32A32_SFLOAT, 32 * 4, true, false, false),
        RGBA8_SRGB(VK_FORMAT_R8G8B8A8_SRGB, 32, true, false, false),
        R8_SRGB(VK_FORMAT_R8_SRGB, 8, true, false, false),
        BGR8_SRGB(VK_FORMAT_B8G8R8_SRGB, 24, true, false, false),
        ABGR8_SRGB(VK_FORMAT_A8B8G8R8_SRGB_PACK32, 32, true, false, false),
        B8G8R8A8_SRGB(VK_FORMAT_B8G8R8A8_SRGB, 32, true, false, false),

        Depth32SFloat(VK_FORMAT_D32_SFLOAT, 32, false, true, false),
        Depth32SFloat_Stencil8UInt(VK_FORMAT_D32_SFLOAT_S8_UINT, 40, false, true, true),
        Depth24UNorm_Stencil8UInt(VK_FORMAT_D24_UNORM_S8_UINT, 32, false, true, true),
        Depth16UNorm(VK_FORMAT_D16_UNORM, 16, false, true, false),
        Depth16UNorm_Stencil8UInt(VK_FORMAT_D16_UNORM_S8_UINT, 24, false, true, true);

        private final int vkEnum, bits;
        private final boolean color, depth, stencil;

        Format(int vkEnum, int bits, boolean color, boolean depth, boolean stencil) {
            this.vkEnum = vkEnum;
            this.bits = bits;
            this.color = color;
            this.depth = depth;
            this.stencil = stencil;
        }

        public int getVkEnum() {
            return vkEnum;
        }

        public int getBits() {
            return bits;
        }

        public boolean isColor() {
            return color;
        }

        public boolean isDepth() {
            return depth;
        }

        public boolean isStencil() {
            return stencil;
        }

        public static Format vkEnum(int vkEnum) {
            for (Format f : Format.values()) {
                if (f.vkEnum == vkEnum) {
                    return f;
                }
            }
            throw new UnsupportedOperationException("Format " + vkEnum + " is not supported.");
        }

    }

    enum Layout {

        Undefined(VK_IMAGE_LAYOUT_UNDEFINED),
        General(VK_IMAGE_LAYOUT_GENERAL),
        PreInitialized(VK_IMAGE_LAYOUT_PREINITIALIZED),
        ColorAttachmentOptimal(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL),
        DepthStencilAttachmentOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL),
        DepthStencilReadOnlyOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL),
        TransferSrcOptimal(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL),
        TransferDstOptimal(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL),
        ShaderReadOnlyOptimal(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
        PresentSrc(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        private final int vkEnum;

        Layout(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int getVkEnum() {
            return vkEnum;
        }

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

    enum Tiling {

        Optimal(VK_IMAGE_TILING_OPTIMAL),
        Linear(VK_IMAGE_TILING_LINEAR);

        private final int vkEnum;

        Tiling(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int getVkEnum() {
            return vkEnum;
        }

    }

    ImageView createView(VkImageViewCreateInfo create);

    LogicalDevice getDevice();

    int getType();

    int getWidth();

    int getHeight();

    int getDepth();

    Format getFormat();

    Tiling getTiling();

    default ImageView createView(int type, int aspects, int baseMip, int mipCount, int baseLayer, int layerCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo create = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(getNativeObject())
                    .viewType(type)
                    .format(getFormat().getVkEnum());
            create.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            create.subresourceRange().aspectMask(aspects)
                    .baseMipLevel(baseMip)
                    .levelCount(mipCount)
                    .baseArrayLayer(baseLayer)
                    .layerCount(layerCount);
            return createView(create);
        }
    }

}
