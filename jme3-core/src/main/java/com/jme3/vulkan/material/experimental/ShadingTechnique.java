package com.jme3.vulkan.material.experimental;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.render.Bucket;
import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.scene.TagListener;

public interface ShadingTechnique extends TagListener {

    boolean acceptNode(int node);

    /**
     * Renders {@code nodes}, in the order given. The order matters: a
     * {@link com.jme3.vulkan.render.RenderDispatcher} hands this a bucket's nodes already
     * sorted by that bucket's {@link Bucket}, e.g.
     * front-to-back for an opaque bucket or back-to-front for a transparent one - this
     * method is expected to preserve that order (grouping by pipeline internally is fine,
     * but should not reorder across pipeline groups in a way that defeats the sort).
     *
     * @param cmd command buffer to record into
     * @param nodes node ids to render
     */
    void render(CommandBuffer cmd, Scene.Group nodes);

}