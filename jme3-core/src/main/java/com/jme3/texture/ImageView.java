package com.jme3.texture;

import com.jme3.vulkan.images.GpuImage;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public interface ImageView <T extends GpuImage> {

    enum Type implements IntEnum<Type> {

        OneDemensional(VK_IMAGE_VIEW_TYPE_1D),
        TwoDemensional(VK_IMAGE_VIEW_TYPE_2D),
        ThreeDemensional(VK_IMAGE_VIEW_TYPE_3D),
        OneDemensionalArray(VK_IMAGE_VIEW_TYPE_1D_ARRAY),
        TwoDemensionalArray(VK_IMAGE_VIEW_TYPE_2D_ARRAY),
        Cube(VK_IMAGE_VIEW_TYPE_CUBE),
        CubeArray(VK_IMAGE_VIEW_TYPE_CUBE_ARRAY);

        private final int vkEnum;

        Type(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

        public static Type of(GlTexture.Type type) {
            switch (type) {
                case TwoDimensional: return TwoDemensional;
                case TwoDimensionalArray: return TwoDemensionalArray;
                case ThreeDimensional: return ThreeDemensional;
                case CubeMap: return Cube;
                default: throw new UnsupportedOperationException("No conversion implemented for " + type);
            }
        }

    }

    long getId();

    T getImage();

    IntEnum<Type> getViewType();

    int getBaseMipmap();

    int getMipmapCount();

    int getBaseLayer();

    int getLayerCount();

}
