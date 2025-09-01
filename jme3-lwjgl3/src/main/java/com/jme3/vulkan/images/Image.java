package com.jme3.vulkan.images;

import com.jme3.vulkan.Format;
import com.jme3.vulkan.util.LibEnum;

public interface Image {

    interface Type extends LibEnum<Type> {}

    long getId();

    LibEnum<Type> getType();

    int getWidth();

    int getHeight();

    int getDepth();

    int getMipmaps();

    int getLayers();

    Format getFormat();

}
