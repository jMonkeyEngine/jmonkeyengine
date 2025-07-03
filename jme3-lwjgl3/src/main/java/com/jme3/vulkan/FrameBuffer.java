package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class FrameBuffer implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final int width, height, layers;
    private final ImageView[] attachments;
    private LongBuffer id = MemoryUtil.memAllocLong(1);

    public FrameBuffer(LogicalDevice device, RenderPass compat, int width, int height, int layers, ImageView... attachments) {
        this.device = device;
        this.width = width;
        this.height = height;
        this.layers = layers;
        this.attachments = attachments;
        LongBuffer att = MemoryUtil.memAllocLong(attachments.length);
        for (int i = 0; i < attachments.length; i++) {
            att.put(i, attachments[i].getNativeObject());
        }
        VkFramebufferCreateInfo create = VkFramebufferCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(compat.getNativeObject())
                .pAttachments(att)
                .width(width).height(height)
                .layers(layers);
        check(vkCreateFramebuffer(device.getNativeObject(), create, null, id));
        MemoryUtil.memFree(att);
        create.close();
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id != null ? id.get(0) : null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyFramebuffer(device.getNativeObject(), id.get(0), null);
            MemoryUtil.memFree(id);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
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

    public ImageView[] getAttachments() {
        return attachments;
    }

}
