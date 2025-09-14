package com.jme3.vulkan.images;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.util.IntEnum;

public interface GpuImage {

    interface Type extends IntEnum<Type> {}

    long getId();

    IntEnum<Type> getType();

    int getWidth();

    int getHeight();

    int getDepth();

    int getMipmaps();

    int getLayers();

    Format getFormat();

}
