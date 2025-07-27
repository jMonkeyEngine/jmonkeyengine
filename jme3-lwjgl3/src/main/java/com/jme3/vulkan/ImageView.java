package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

public class ImageView implements Native<Long> {

    private final Image image;
    private final NativeReference ref;
    private long id;

    public ImageView(Image image, VkImageViewCreateInfo create) {
        this.image = image;
        create.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
        create.image(image.getNativeObject());
        System.out.println("image ID: " + image.getNativeObject());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer idBuf = stack.mallocLong(1);
            VK10.vkCreateImageView(image.getDevice().getNativeObject(), create, null, idBuf);
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        image.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            VK10.vkDestroyImageView(image.getDevice().getNativeObject(), id, null);
            id = VK10.VK_NULL_HANDLE;
        };
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public Image getImage() {
        return image;
    }

}
