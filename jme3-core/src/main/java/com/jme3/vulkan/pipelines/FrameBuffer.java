package com.jme3.vulkan.pipelines;

import com.jme3.util.AbstractBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class FrameBuffer extends AbstractNative<Long> {

    public enum Create implements Flag<Create> {

        Imageless(VK_FRAMEBUFFER_CREATE_IMAGELESS_BIT);

        private final int bits;

        Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    private final LogicalDevice<?> device;
    private final RenderPass compat;
    private final int width, height, layers;
    private final List<VulkanImageView> attachments = new ArrayList<>();
    private Flag<Create> flags = Flag.empty();

    public FrameBuffer(LogicalDevice<?> device, RenderPass compat, int width, int height, int layers) {
        this.device = device;
        this.compat = compat;
        this.width = width;
        this.height = height;
        this.layers = layers;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyFramebuffer(device.getNativeObject(), object, null);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLayers() {
        return layers;
    }

    public List<VulkanImageView> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public Flag<Create> getFlags() {
        return flags;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractBuilder {

        @Override
        protected void build() {
            LongBuffer att = stack.mallocLong(attachments.size());
            for (VulkanImageView a : attachments) {
                att.put(a.getNativeObject());
            }
            att.flip();
            VkFramebufferCreateInfo create = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .flags(flags.bits())
                    .renderPass(compat.getNativeObject())
                    .pAttachments(att)
                    .width(width).height(height)
                    .layers(layers);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateFramebuffer(device.getNativeObject(), create, null, idBuf));
            object = idBuf.get(0);
            ref = Native.get().register(FrameBuffer.this);
            device.getNativeReference().addDependent(ref);
        }

        public void addAttachment(VulkanImageView attachment) {
            attachments.add(attachment);
        }

        public void setFlags(Flag<Create> flags) {
            FrameBuffer.this.flags = flags;
        }

    }

}
