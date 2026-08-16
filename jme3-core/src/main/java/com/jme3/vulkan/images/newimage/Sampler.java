package com.jme3.vulkan.images.newimage;

import com.jme3.vulkan.images.AddressMode;
import com.jme3.vulkan.images.BorderColor;
import com.jme3.vulkan.images.FilterMode;
import com.jme3.vulkan.images.MipmapMode;
import com.jme3.vulkan.pipeline.CompareOp;

public interface Sampler {

    int U_AXIS = 0, V_AXIS = 1, W_AXIS = 2;

    long getHandle();

    MipmapMode getMipmapMode();

    FilterMode getMinFilter();

    FilterMode getMagFilter();

    AddressMode[] getEdgeModes();

    default AddressMode getEdgeModeU() {
        return getEdgeModes()[U_AXIS];
    }

    default AddressMode getEdgeModeV() {
        return getEdgeModes()[V_AXIS];
    }

    default AddressMode getEdgeModeW() {
        return getEdgeModes()[W_AXIS];
    }

    float getAnisotropy();

    BorderColor getBorderColor();

    CompareOp getCompare();

    float getLodBias();

    float getMinLod();

    float getMaxLod();

    boolean isUnnormalizedCoords();

}
