package com.jme3.vulkan.render;

import com.jme3.renderer.ViewPort;

import java.util.Collection;

/**
 * Maintains a set of viewports which it renders on command.
 */
public interface NewRenderer {

    void render(Collection<ViewPort> viewPorts);

}
