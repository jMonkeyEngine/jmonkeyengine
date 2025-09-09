package com.jme3.vulkan.images;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.AbstractNative;
import com.jme3.vulkan.Swizzle;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class ImageView extends AbstractNative<Long> {

    private final VulkanImage image;
    private final IntEnum<VulkanImage.View> type;

    private IntEnum<Swizzle> swizzleR = Swizzle.R;
    private IntEnum<Swizzle> swizzleG = Swizzle.G;
    private IntEnum<Swizzle> swizzleB = Swizzle.B;
    private IntEnum<Swizzle> swizzleA = Swizzle.A;
    private Flag<VulkanImage.Aspect> aspect = VulkanImage.Aspect.Color;
    private int baseMipmap = 0;
    private int mipmapCount = 1;
    private int baseLayer = 0;
    private int layerCount = 1;

    public ImageView(VulkanImage image, IntEnum<VulkanImage.View> type) {
        this.image = image;
        this.type = type;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyImageView(image.getDevice().getNativeObject(), object, null);
    }

    public VulkanImage getImage() {
        return image;
    }

    public IntEnum<VulkanImage.View> getType() {
        return type;
    }

    public IntEnum<Swizzle> getSwizzleR() {
        return swizzleR;
    }

    public IntEnum<Swizzle> getSwizzleG() {
        return swizzleG;
    }

    public IntEnum<Swizzle> getSwizzleB() {
        return swizzleB;
    }

    public IntEnum<Swizzle> getSwizzleA() {
        return swizzleA;
    }

    public Flag<VulkanImage.Aspect> getAspect() {
        return aspect;
    }

    public int getBaseMipmap() {
        return baseMipmap;
    }

    public int getMipmapCount() {
        return mipmapCount;
    }

    public int getBaseLayer() {
        return baseLayer;
    }

    public int getLayerCount() {
        return layerCount;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractNative.Builder<ImageView> {

        @Override
        protected void build() {
            VkImageViewCreateInfo create = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image.getId())
                    .viewType(type.getEnum())
                    .format(image.getFormat().getVkEnum());
            create.components()
                    .r(swizzleR.getEnum())
                    .g(swizzleG.getEnum())
                    .b(swizzleB.getEnum())
                    .a(swizzleA.getEnum());
            create.subresourceRange()
                    .aspectMask(aspect.bits())
                    .baseMipLevel(baseMipmap)
                    .levelCount(mipmapCount)
                    .baseArrayLayer(baseLayer)
                    .layerCount(layerCount);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateImageView(image.getDevice().getNativeObject(), create, null, idBuf),
                    "Failed to create image view.");
            object = idBuf.get(0);
            ref = Native.get().register(ImageView.this);
            image.addNativeDependent(ref);
        }

        public void allMipmaps() {
            baseMipmap = 0;
            mipmapCount = image.getMipmaps();
        }

        public void setSwizzleR(IntEnum<Swizzle> swizzleR) {
            ImageView.this.swizzleR = swizzleR;
        }

        public void setSwizzleG(IntEnum<Swizzle> swizzleG) {
            ImageView.this.swizzleG = swizzleG;
        }

        public void setSwizzleB(IntEnum<Swizzle> swizzleB) {
            ImageView.this.swizzleB = swizzleB;
        }

        public void setSwizzleA(IntEnum<Swizzle> swizzleA) {
            ImageView.this.swizzleA = swizzleA;
        }

        public void setAspect(Flag<VulkanImage.Aspect> aspect) {
            ImageView.this.aspect = aspect;
        }

        public void setBaseMipmap(int baseMipmap) {
            ImageView.this.baseMipmap = baseMipmap;
        }

        public void setMipmapCount(int mipmapCount) {
            ImageView.this.mipmapCount = mipmapCount;
        }

        public void setBaseLayer(int baseLayer) {
            ImageView.this.baseLayer = baseLayer;
        }

        public void setLayerCount(int layerCount) {
            ImageView.this.layerCount = layerCount;
        }

    }

}
