package com.jme3.vulkan.pass;

import com.jme3.vulkan.images.VulkanImage;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentReference;

/**
 * Immutable reference to an {@link Attachment}.
 */
public class AttachmentReference {

    private final Attachment attachment;
    private final VulkanImage.Layout layout;

    protected AttachmentReference(Attachment attachment, VulkanImage.Layout layout) {
        this.attachment = attachment;
        this.layout = layout;
    }

    public void fillStruct(VkAttachmentReference struct) {
        struct.attachment(getAttachmentPosition())
                .layout(layout.getEnum());
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public VulkanImage.Layout getLayout() {
        return layout;
    }

    public int getAttachmentPosition() {
        return attachment != null ? attachment.getPosition() : VK10.VK_ATTACHMENT_UNUSED;
    }

    public boolean isUnused() {
        return attachment == null;
    }

    public boolean isCompatible(AttachmentReference ref) {
        return isUnused() == ref.isUnused() && (isUnused() || attachment.isCompatible(ref.attachment)) && layout == ref.layout;
    }

    public static AttachmentReference unused(VulkanImage.Layout layout) {
        return new AttachmentReference(null, layout);
    }

}
