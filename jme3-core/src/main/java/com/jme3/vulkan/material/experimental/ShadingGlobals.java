package com.jme3.vulkan.material.experimental;

import com.jme3.renderer.ViewPort;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.material.exp2.RenderSession;

public interface ShadingGlobals {

    void update(RenderSession session, ViewPort vp);

}
