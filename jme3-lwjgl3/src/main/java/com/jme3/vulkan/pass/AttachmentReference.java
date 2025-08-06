package com.jme3.vulkan.pass;

import com.jme3.vulkan.images.Image;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentReference;

/**
 * Immutable reference to an {@link Attachment}.
 */
public class AttachmentReference {

    private final Attachment attachment;
    private final Image.Layout layout;

    protected AttachmentReference(Attachment attachment, Image.Layout layout) {
        this.attachment = attachment;
        this.layout = layout;
    }

    public void fillStruct(VkAttachmentReference struct) {
        struct.attachment(getAttachmentPosition())
                .layout(layout.getVkEnum());
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public Image.Layout getLayout() {
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

    public static AttachmentReference unused(Image.Layout layout) {
        return new AttachmentReference(null, layout);
    }

}
