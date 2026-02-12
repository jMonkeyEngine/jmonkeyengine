package com.jme3.vulkan.images;

import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public interface GpuImage {

    enum Type implements IntEnum<Type> {

        OneDemensional(VK_IMAGE_TYPE_1D),
        TwoDemensional(VK_IMAGE_TYPE_2D),
        ThreeDemensional(VK_IMAGE_TYPE_3D);

        private final int vkEnum;

        Type(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    long getId();

    IntEnum<Type> getType();

    int getWidth();

    int getHeight();

    int getDepth();

    int getMipmaps();

    int getLayers();

    Format getFormat();

}
