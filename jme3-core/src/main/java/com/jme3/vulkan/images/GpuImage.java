package com.jme3.vulkan.images;

import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.util.IntEnum;

@Deprecated
public interface GpuImage {

    long getId();

    IntEnum<EngineImage.Type> getType();

    int getWidth();

    int getHeight();

    int getDepth();

    int getMipmaps();

    int getLayers();

    int getSamples();

    Format getFormat();

}
