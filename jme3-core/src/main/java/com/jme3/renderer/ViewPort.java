/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer;

import com.jme3.renderer.pipeline.RenderPipeline;
import com.jme3.math.ColorRGBA;
import com.jme3.post.SceneProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.GlFrameBuffer;
import com.jme3.util.SafeArrayList;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;

import java.util.function.Predicate;

/**
 * Represents a view inside the display
 * window or a {@link GlFrameBuffer} to which scenes will be rendered.
 *
 * <p>A viewport has a camera
 * which is used to render a set of {@link #attachScene(com.jme3.scene.Spatial) scenes}.
 * A view port has a location on the screen.
 * By default, a view port does not clear the framebuffer, but it can be
 * set to {@link #setClearFlags(boolean, boolean, boolean) clear the framebuffer}.
 * The background color which the color buffer is cleared to can be specified
 * via the {@link #setBackgroundColor(com.jme3.math.ColorRGBA)} method.
 *
 * <p>A ViewPort has a list of {@link SceneProcessor}s which can
 * control how the ViewPort is rendered by the {@link RenderManager}.
 *
 * @author Kirill Vainer
 *
 * @see RenderManager
 * @see SceneProcessor
 * @see Spatial
 * @see Camera
 */
public class ViewPort implements Comparable<ViewPort> {

    protected final Camera cam;
    protected final SafeArrayList<Spatial> sceneList = new SafeArrayList<>(Spatial.class);
    protected final SafeArrayList<SceneProcessor> processors = new SafeArrayList<>(SceneProcessor.class);
    protected RenderPipeline pipeline;
    protected GlFrameBuffer out = null;

    protected int viewPriority;

    protected final ColorRGBA backColor = new ColorRGBA(0, 0, 0, 0);
    protected boolean clearDepth = false;
    protected boolean clearColor = false;
    protected boolean clearStencil = false;
    private boolean enabled = true;

    private final ViewPortArea area = new ViewPortArea(0f, 0f, 128f, 128f);

    private Predicate<Geometry> geometryFilter;

    /**
     * Creates a new viewport. User code should generally use these methods instead:<br>
     * <ul>
     * <li>{@link RenderManager#createPreView(java.lang.String, com.jme3.renderer.Camera) }</li>
     * <li>{@link RenderManager#createMainView(java.lang.String, com.jme3.renderer.Camera)  }</li>
     * <li>{@link RenderManager#createPostView(java.lang.String, com.jme3.renderer.Camera)  }</li>
     * </ul>
     *
     * @param cam The camera through which the viewport is rendered. The camera
     *     cannot be swapped to a different one after creating the viewport.
     */
    public ViewPort(Camera cam) {
        this(cam, 0);
    }

    /**
     * Creates a new viewport.
     *
     * @param cam camera used to render the viewport
     * @param viewPriority indicates the order in which viewports are rendered values
     *                     closer to negative infinity are rendered first (defaults to 0)
     */
    public ViewPort(Camera cam, int viewPriority) {
        this.cam = cam;
        this.viewPriority = viewPriority;
    }

    public Predicate<Geometry> getGeometryFilter() {
        return geometryFilter;
    }

    public void setGeometryFilter(Predicate<Geometry> geometryFilter) {
        this.geometryFilter = geometryFilter;
    }

    /**
     * Gets the area this viewport renders to.
     *
     * @return area
     */
    public ViewPortArea getArea() {
        return area;
    }

    /**
     * Gets the list of {@link SceneProcessor scene processors} that were
     * added to this <code>ViewPort</code>.
     *
     * @return the list of processors attached to this ViewPort
     *
     * @see #addProcessor(com.jme3.post.SceneProcessor)
     */
    public SafeArrayList<SceneProcessor> getProcessors() {
        return processors;
    }

    /**
     * Adds a {@link SceneProcessor} to this ViewPort.
     *
     * <p>SceneProcessors that are added to the ViewPort will be notified
     * of events as the ViewPort is being rendered by the {@link RenderManager}.
     *
     * @param processor The processor to add
     *
     * @see SceneProcessor
     */
    public void addProcessor(SceneProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null.");
        }
        processors.add(processor);
    }

    /**
     * Removes a {@link SceneProcessor} from this ViewPort.
     *
     * <p>The processor will no longer receive events occurring to this ViewPort.
     *
     * @param processor The processor to remove
     *
     * @see SceneProcessor
     */
    public void removeProcessor(SceneProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null.");
        }
        processors.remove(processor);
        processor.cleanup();
    }

    /**
     * Removes all {@link SceneProcessor scene processors} from this
     * ViewPort.
     *
     * @see SceneProcessor
     */
    public void clearProcessors() {
        for (SceneProcessor proc : processors) {
            proc.cleanup();
        }
        processors.clear();
    }

    /**
     * Checks if depth buffer clearing is enabled.
     *
     * @return true if depth buffer clearing is enabled.
     *
     * @see #setClearDepth(boolean)
     */
    public boolean isClearDepth() {
        return clearDepth;
    }

    /**
     * Enables or disables clearing of the depth buffer for this ViewPort.
     *
     * <p>By default depth clearing is disabled.
     *
     * @param clearDepth Enable/disable depth buffer clearing.
     */
    public void setClearDepth(boolean clearDepth) {
        this.clearDepth = clearDepth;
    }

    /**
     * Checks if color buffer clearing is enabled.
     *
     * @return true if color buffer clearing is enabled.
     *
     * @see #setClearColor(boolean)
     */
    public boolean isClearColor() {
        return clearColor;
    }

    /**
     * Enables or disables clearing of the color buffer for this ViewPort.
     *
     * <p>By default color clearing is disabled.
     *
     * @param clearColor Enable/disable color buffer clearing.
     */
    public void setClearColor(boolean clearColor) {
        this.clearColor = clearColor;
    }

    /**
     * Checks if stencil buffer clearing is enabled.
     *
     * @return true if stencil buffer clearing is enabled.
     *
     * @see #setClearStencil(boolean)
     */
    public boolean isClearStencil() {
        return clearStencil;
    }

    /**
     * Sets the view priority of this viewport. Values closer to negative infinity
     * relative to other viewports causes this viewport to be rendered before them.
     * Ties result in an arbitrary order.
     *
     * <p>default=0</p>
     *
     * @param viewPriority view priority
     */
    public void setViewPriority(int viewPriority) {
        this.viewPriority = viewPriority;
    }

    /**
     * Enables or disables clearing of the stencil buffer for this ViewPort.
     *
     * <p>Stencil clearing is disabled by default.</p>
     *
     * @param clearStencil Enable/disable stencil buffer clearing.
     */
    public void setClearStencil(boolean clearStencil) {
        this.clearStencil = clearStencil;
    }

    /**
     * Sets the clear flags (color, depth, stencil) in one call.
     *
     * @param color If color buffer clearing should be enabled.
     * @param depth If depth buffer clearing should be enabled.
     * @param stencil If stencil buffer clearing should be enabled.
     *
     * @see #setClearColor(boolean)
     * @see #setClearDepth(boolean)
     * @see #setClearStencil(boolean)
     */
    public void setClearFlags(boolean color, boolean depth, boolean stencil) {
        this.clearColor = color;
        this.clearDepth = depth;
        this.clearStencil = stencil;
    }

    /**
     * Returns the framebuffer where this ViewPort's scenes are
     * rendered to.
     *
     * @return the framebuffer where this ViewPort's scenes are
     *     rendered to.
     *
     * @see #setOutputFrameBuffer(GlFrameBuffer)
     */
    public FrameBuffer<?> getOutputFrameBuffer() {
        return out;
    }

    /**
     * Sets the output framebuffer for the ViewPort.
     *
     * <p>The output framebuffer specifies where the scenes attached
     * to this ViewPort are rendered to. By default, this is <code>null</code>,
     * which indicates the scenes are rendered to the display window.
     *
     * @param out The framebuffer to render scenes to, or null if to render
     *     to the screen.
     */
    public void setOutputFrameBuffer(GlFrameBuffer out) {
        this.out = out;
    }

    /**
     * Gets the view priority of this viewport.
     *
     * @return view priority
     * @see #setViewPriority(int)
     */
    public int getViewPriority() {
        return viewPriority;
    }

    /**
     * Returns the camera which renders the attached scenes.
     *
     * @return the camera which renders the attached scenes.
     *
     * @see Camera
     */
    public Camera getCamera() {
        return cam;
    }

    /**
     * Attaches a new scene to render in this ViewPort.
     *
     * @param scene The scene to attach
     *
     * @see Spatial
     */
    public void attachScene(Spatial scene) {
        if (scene == null) {
            throw new IllegalArgumentException("Scene cannot be null.");
        }
        sceneList.add(scene);
        if (scene instanceof Geometry) {
            scene.forceRefresh(true, false, true);
        }
    }

    /**
     * Detaches a scene from rendering.
     *
     * @param scene The scene to detach
     *
     * @see #attachScene(com.jme3.scene.Spatial)
     */
    public void detachScene(Spatial scene) {
        if (scene == null) {
            throw new IllegalArgumentException("Scene cannot be null.");
        }
        sceneList.remove(scene);
        if (scene instanceof Geometry) {
            scene.forceRefresh(true, false, true);
        }
    }

    /**
     * Removes all attached scenes.
     *
     * @see #attachScene(com.jme3.scene.Spatial)
     */
    public void clearScenes() {
        sceneList.clear();
    }

    /**
     * Returns a list of all attached scenes.
     *
     * @return a list of all attached scenes.
     *
     * @see #attachScene(com.jme3.scene.Spatial)
     */
    public SafeArrayList<Spatial> getScenes() {
        return sceneList;
    }

    /**
     * Sets the background color.
     *
     * <p>When the ViewPort's color buffer is cleared
     * (if {@link #setClearColor(boolean) color clearing} is enabled),
     * this specifies the color to which the color buffer is set to.
     * By default, the background color is black without alpha.
     *
     * @param background the background color.
     */
    public void setBackgroundColor(ColorRGBA background) {
        backColor.set(background);
    }

    /**
     * Returns the background color of this ViewPort.
     *
     * @return the background color of this ViewPort
     *
     * @see #setBackgroundColor(com.jme3.math.ColorRGBA)
     */
    public ColorRGBA getBackgroundColor() {
        return backColor;
    }

    /**
     * Enables or disables this ViewPort.
     *
     * <p>Disabled ViewPorts are skipped by the {@link RenderManager} when
     * rendering. By default, all viewports are enabled.
     *
     * @param enable If the viewport should be disabled or enabled.
     */
    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    /**
     * Returns true if the viewport is enabled, false otherwise.
     *
     * @return true if the viewport is enabled, false otherwise.
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets the pipeline used by this viewport for rendering.
     * <p>
     * If null, the render manager's default pipeline will be used
     * to render this viewport.
     * <p>
     * default=null
     * 
     * @param pipeline pipeline, or null to use render manager's pipeline
     */
    public void setPipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
    }
    
    /**
     * Gets the framegraph used by this viewport for rendering.
     * 
     * @return pipeline
     */
    public RenderPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public int compareTo(ViewPort o) {
        return Integer.compare(viewPriority, o.viewPriority);
    }

}
