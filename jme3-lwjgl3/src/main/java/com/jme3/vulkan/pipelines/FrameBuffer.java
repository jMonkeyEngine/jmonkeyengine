package com.jme3.vulkan.pipelines;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.ImageView;
import com.jme3.vulkan.pass.RenderPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class FrameBuffer implements Native<Long> {

    private final LogicalDevice<?> device;
    private final NativeReference ref;
    private final int width, height, layers;
    private final ImageView[] attachments;
    private long id;

    public FrameBuffer(LogicalDevice<?> device, RenderPass compat, int width, int height, int layers, ImageView... attachments) {
        this.device = device;
        this.width = width;
        this.height = height;
        this.layers = layers;
        this.attachments = attachments;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer att = stack.mallocLong(attachments.length);
            for (int i = 0; i < attachments.length; i++) {
                att.put(i, attachments[i].getNativeObject());
            }
            VkFramebufferCreateInfo create = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(compat.getNativeObject())
                    .pAttachments(att)
                    .width(width).height(height)
                    .layers(layers);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateFramebuffer(device.getNativeObject(), create, null, idBuf));
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyFramebuffer(device.getNativeObject(), id, null);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = VK_NULL_HANDLE;
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
