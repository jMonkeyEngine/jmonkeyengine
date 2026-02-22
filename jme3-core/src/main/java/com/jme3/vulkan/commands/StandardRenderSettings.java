package com.jme3.vulkan.commands;

import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;

public interface StandardRenderSettings {

    void applySettings();

    void pushViewPort(ViewPortArea area);

    ViewPortArea popViewPort();

    ViewPortArea getViewPort();

    void pushScissor(ScissorArea area);

    ScissorArea popScissor();

    ScissorArea getScissor();

}
