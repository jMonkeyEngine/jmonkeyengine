package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

public class ImageView implements Native<Long> {

    private final Image image;
    private final NativeReference ref;
    private LongBuffer id = MemoryUtil.memAllocLong(1);

    public ImageView(Image image, VkImageViewCreateInfo create) {
        this.image = image;
        create.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
        create.image(image.getNativeObject());
        VK10.vkCreateImageView(image.getDevice().getNativeObject(), create, null, id);
        ref = Native.get().register(this);
        image.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id != null ? id.get(0) : null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            VK10.vkDestroyImageView(image.getDevice().getNativeObject(), id.get(0), null);
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

    public Image getImage() {
        return image;
    }

}
