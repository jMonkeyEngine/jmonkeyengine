/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.math.ColorRGBA;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A <code>ViewPort</code> represents a view inside the display
 * window or a {@link FrameBuffer} to which scenes will be rendered. 
 * <p>
 * A viewport has a {@link #ViewPort(java.lang.String, com.jme3.renderer.Camera) camera}
 * which is used to render a set of {@link #attachScene(com.jme3.scene.Spatial) scenes}.
 * A view port has a location on the screen as set by the 
 * {@link Camera#setViewPort(float, float, float, float) } method.
 * By default, a view port does not clear the framebuffer, but it can be
 * set to {@link #setClearFlags(boolean, boolean, boolean) clear the framebuffer}.
 * The background color which the color buffer is cleared to can be specified 
 * via the {@link #setBackgroundColor(com.jme3.math.ColorRGBA)} method.
 * <p>
 * A ViewPort has a list of {@link SceneProcessor}s which can
 * control how the ViewPort is rendered by the {@link RenderManager}.
 * 
 * @author Kirill Vainer
 * 
 * @see RenderManager
 * @see SceneProcessor
 * @see Spatial
 * @see Camera
 */
public class ViewPort {

    protected final String name;
    protected final Camera cam;
    protected final RenderQueue queue = new RenderQueue();
    protected final ArrayList<Spatial> sceneList = new ArrayList<Spatial>();
    protected final ArrayList<SceneProcessor> processors = new ArrayList<SceneProcessor>();
    protected FrameBuffer out = null;

    protected final ColorRGBA backColor = new ColorRGBA(0,0,0,0);
    protected boolean clearDepth = false, clearColor = false, clearStencil = false;
    private boolean enabled = true;

    /**
     * Create a new viewport. User code should generally use these methods instead:<br>
     * <ul>
     * <li>{@link RenderManager#createPreView(java.lang.String, com.jme3.renderer.Camera) }</li>
     * <li>{@link RenderManager#createMainView(java.lang.String, com.jme3.renderer.Camera)  }</li>
     * <li>{@link RenderManager#createPostView(java.lang.String, com.jme3.renderer.Camera)  }</li>
     * </ul>
     * 
     * @param name The name of the viewport. Used for debugging only.
     * @param cam The camera through which the viewport is rendered. The camera
     * cannot be swapped to a different one after creating the viewport.
     */
    public ViewPort(String name, Camera cam) {
        this.name = name;
        this.cam = cam;
    }

    /**
     * Returns the name of the viewport as set in the constructor.
     * 
     * @return the name of the viewport
     * 
     * @see #ViewPort(java.lang.String, com.jme3.renderer.Camera) 
     */
    public String getName() {
        return name;
    }

    /**
     * Get the list of {@link SceneProcessor scene processors} that were
     * added to this <code>ViewPort</code>
     * 
     * @return the list of processors attached to this ViewPort
     * 
     * @see #addProcessor(com.jme3.post.SceneProcessor) 
     */
    public List<SceneProcessor> getProcessors(){
        return processors;
    }

    /**
     * Adds a {@link SceneProcessor} to this ViewPort.
     * <p>
     * SceneProcessors that are added to the ViewPort will be notified
     * of events as the ViewPort is being rendered by the {@link RenderManager}.
     * 
     * @param processor The processor to add
     * 
     * @see SceneProcessor
     */
    public void addProcessor(SceneProcessor processor){
        processors.add(processor);
    }

    /**
     * Removes a {@link SceneProcessor} from this ViewPort.
     * <p>
     * The processor will no longer receive events occurring to this ViewPort.
     * 
     * @param processor The processor to remove
     * 
     * @see SceneProcessor
     */
    public void removeProcessor(SceneProcessor processor){
        processors.remove(processor);
        processor.cleanup();
    }

    /**
     * Check if depth buffer clearing is enabled.
     * 
     * @return true if depth buffer clearing is enabled.
     * 
     * @see #setClearDepth(boolean) 
     */
    public boolean isClearDepth() {
        return clearDepth;
    }

    /**
     * Enable or disable clearing of the depth buffer for this ViewPort.
     * <p>
     * By default depth clearing is disabled.
     * 
     * @param clearDepth Enable/disable depth buffer clearing.
     */
    public void setClearDepth(boolean clearDepth) {
        this.clearDepth = clearDepth;
    }

    /**
     * Check if color buffer clearing is enabled.
     * 
     * @return true if color buffer clearing is enabled.
     * 
     * @see #setClearColor(boolean) 
     */
    public boolean isClearColor() {
        return clearColor;
    }

    /**
     * Enable or disable clearing of the color buffer for this ViewPort.
     * <p>
     * By default color clearing is disabled.
     * 
     * @param clearColor Enable/disable color buffer clearing.
     */
    public void setClearColor(boolean clearColor) {
        this.clearColor = clearColor;
    }

    /**
     * Check if stencil buffer clearing is enabled.
     * 
     * @return true if stencil buffer clearing is enabled.
     * 
     * @see #setClearStencil(boolean) 
     */
    public boolean isClearStencil() {
        return clearStencil;
    }

    /**
     * Enable or disable clearing of the stencil buffer for this ViewPort.
     * <p>
     * By default stencil clearing is disabled.
     * 
     * @param clearStencil Enable/disable stencil buffer clearing.
     */
    public void setClearStencil(boolean clearStencil) {
        this.clearStencil = clearStencil;
    }

    /**
     * Set the clear flags (color, depth, stencil) in one call.
     * 
     * @param color If color buffer clearing should be enabled.
     * @param depth If depth buffer clearing should be enabled.
     * @param stencil If stencil buffer clearing should be enabled.
     * 
     * @see #setClearColor(boolean) 
     * @see #setClearDepth(boolean) 
     * @see #setClearStencil(boolean) 
     */
    public void setClearFlags(boolean color, boolean depth, boolean stencil){
        this.clearColor = color;
        this.clearDepth = depth;
        this.clearStencil = stencil;
    }

    /**
     * Returns the framebuffer where this ViewPort's scenes are
     * rendered to.
     * 
     * @return the framebuffer where this ViewPort's scenes are
     * rendered to.
     * 
     * @see #setOutputFrameBuffer(com.jme3.texture.FrameBuffer) 
     */
    public FrameBuffer getOutputFrameBuffer() {
        return out;
    }

    /**
     * Sets the output framebuffer for the ViewPort.
     * <p>
     * The output framebuffer specifies where the scenes attached
     * to this ViewPort are rendered to. By default this is <code>null</code>
     * which indicates the scenes are rendered to the display window.
     * 
     * @param out The framebuffer to render scenes to, or null if to render
     * to the screen.
     */
    public void setOutputFrameBuffer(FrameBuffer out) {
        this.out = out;
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
     * Internal use only.
     */
    public RenderQueue getQueue() {
        return queue;
    }

    /**
     * Attaches a new scene to render in this ViewPort.
     * 
     * @param scene The scene to attach
     * 
     * @see Spatial
     */
    public void attachScene(Spatial scene){
        sceneList.add(scene);
    }

    /**
     * Detaches a scene from rendering.
     * 
     * @param scene The scene to detach
     * 
     * @see #attachScene(com.jme3.scene.Spatial) 
     */
    public void detachScene(Spatial scene){
        sceneList.remove(scene);
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
    public List<Spatial> getScenes(){
        return sceneList;
    }

    /**
     * Sets the background color.
     * <p>
     * When the ViewPort's color buffer is cleared 
     * (if {@link #setClearColor(boolean) color clearing} is enabled), 
     * this specifies the color to which the color buffer is set to.
     * By default the background color is black without alpha.
     * 
     * @param background the background color.
     */
    public void setBackgroundColor(ColorRGBA background){
        backColor.set(background);
    }

    /**
     * Returns the background color of this ViewPort
     * 
     * @return the background color of this ViewPort
     * 
     * @see #setBackgroundColor(com.jme3.math.ColorRGBA) 
     */
    public ColorRGBA getBackgroundColor(){
        return backColor;
    }
    
    /**
     * Enable or disable this ViewPort.
     * <p>
     * Disabled ViewPorts are skipped by the {@link RenderManager} when
     * rendering. By default all ViewPorts are enabled.
     * 
     * @param enable If the viewport should be disabled or enabled.
     */
    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }
    
    /**
     * Returns true if the viewport is enabled, false otherwise.
     * @return true if the viewport is enabled, false otherwise.
     * @see #setEnabled(boolean) 
     */
    public boolean isEnabled() {
        return enabled;
    }

}
