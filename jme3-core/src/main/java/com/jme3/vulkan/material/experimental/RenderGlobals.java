package com.jme3.vulkan.material.experimental;

import com.jme3.renderer.ViewPort;
import com.jme3.vulkan.buffers.MappableBuffer;

public interface RenderGlobals {

    void update();

    void update(ViewPort vp);

    MappableBuffer getGlobalsBuffer();

}
