package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.vulkan.EXTLegacyDithering;
import org.lwjgl.vulkan.KHRMaintenance7;

import static org.lwjgl.vulkan.VK13.*;

public interface VulkanFrameBuffer <T extends RenderTarget<VulkanImageView>> extends FrameBuffer<T> {

    enum Render implements Flag<Render> {

        ContentsSecondaryCommandBuffer(VK_RENDERING_CONTENTS_SECONDARY_COMMAND_BUFFERS_BIT),
        Suspending(VK_RENDERING_SUSPENDING_BIT),
        Resuming(VK_RENDERING_RESUMING_BIT),
        EnableLegacyDithering(EXTLegacyDithering.VK_RENDERING_ENABLE_LEGACY_DITHERING_BIT_EXT),
        ContentsInline(KHRMaintenance7.VK_RENDERING_CONTENTS_INLINE_BIT_KHR);

        private final int bits;

        Render(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    void beginDynamicRender(CommandBuffer cmd, VulkanImage.Load colorLoad, VulkanImage.Store colorStore, VulkanImage.Load depthLoad, VulkanImage.Store depthStore, Flag<Render> flags);

    long getBufferId(LogicalDevice<?> device);

    default void beginDynamicRender(CommandBuffer cmd, VulkanImage.Load colorLoad, VulkanImage.Store colorStore, VulkanImage.Load depthLoad, VulkanImage.Store depthStore) {
        beginDynamicRender(cmd, colorLoad, colorStore, depthLoad, depthStore, Flag.empty());
    }

    default void endDynamicRender(CommandBuffer cmd) {
        vkCmdEndRendering(cmd.getBuffer());
    }

}
