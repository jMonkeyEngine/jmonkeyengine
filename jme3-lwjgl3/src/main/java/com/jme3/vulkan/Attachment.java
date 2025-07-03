package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;

import static org.lwjgl.vulkan.VK10.*;

public class Attachment implements Native<VkAttachmentDescription> {

    private final NativeReference ref;
    private VkAttachmentDescription description;
    private VkAttachmentReference reference;

    public Attachment(int format, int samples, int finalLayout) {
        description = VkAttachmentDescription.create()
                .format(format)
                .samples(samples)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .stencilLoadOp(VK_ATTACHMENT_STORE_OP_STORE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(finalLayout);
        reference = VkAttachmentReference.create();
        ref = Native.get().register(this);
    }

    @Override
    public VkAttachmentDescription getNativeObject() {
        return description;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return description::free;
    }

    @Override
    public void prematureNativeDestruction() {
        description = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return null;
    }

    public VkAttachmentDescription getDescription() {
        return description;
    }

}
