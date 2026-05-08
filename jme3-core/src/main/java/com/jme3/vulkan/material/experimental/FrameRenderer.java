package com.jme3.vulkan.material.experimental;

import com.jme3.renderer.ViewPort;

import java.util.Collection;
import java.util.Queue;
import java.util.function.Supplier;

@Deprecated
public interface FrameRenderer extends AutoCloseable {

    @Override
    void close();

    /**
     * Renders the ViewPorts from the queue in order. Each geometry is rendered using
     * the submitted {@link ShadingTechnique} that the geometry's material has enabled
     * and is closest to the front of the {@code techniques} queue. Geometries that
     * do not have an available technique enabled are not rendered.
     *
     * @param viewPorts viewports to render
     * @param techniques techniques to render by
     */
    void render(Collection<ViewPort> viewPorts, Queue<ShadingTechnique> techniques);

    /**
     * Fetches render-time globals by type. If no existing globals are assignable
     * to {@code type}, then a new {@link RenderGlobals} instance is created by
     * {@code factory} and registered. Globals registered more recently are
     * preferred.
     *
     * @param <T> globals type
     * @param type globals type
     * @param factory creates a new globals instance
     * @return globals for type
     */
    <T extends RenderGlobals> T getGlobals(Class<T> type, Supplier<T> factory);

}
