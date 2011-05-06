/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

    public ViewPort(String name, Camera cam) {
        this.name = name;
        this.cam = cam;
    }

    public String getName() {
        return name;
    }

    public List<SceneProcessor> getProcessors(){
        return processors;
    }

    public void addProcessor(SceneProcessor processor){
        processors.add(processor);
    }

    public void removeProcessor(SceneProcessor processor){
        processors.remove(processor);
        processor.cleanup();
    }

    /**
     * Does nothing.
     * @deprecated Use {@link ViewPort#setClearColor(boolean) } and similar
     * methods.
     */
    @Deprecated
    public boolean isClearEnabled() {
        return clearDepth && clearColor && clearStencil;
    }
    
    /**
     * Does nothing.
     * @deprecated Use {@link ViewPort#setClearColor(boolean) } and similar
     * methods.
     */
    @Deprecated
    public void setClearEnabled(boolean clearEnabled) {
        clearDepth = clearColor = clearStencil = clearEnabled;
    }

    public boolean isClearDepth() {
        return clearDepth;
    }

    public void setClearDepth(boolean clearDepth) {
        this.clearDepth = clearDepth;
    }

    public boolean isClearColor() {
        return clearColor;
    }

    public void setClearColor(boolean clearColor) {
        this.clearColor = clearColor;
    }

    public boolean isClearStencil() {
        return clearStencil;
    }

    public void setClearStencil(boolean clearStencil) {
        this.clearStencil = clearStencil;
    }

    public void setClearFlags(boolean color, boolean depth, boolean stencil){
        this.clearColor = color;
        this.clearDepth = depth;
        this.clearStencil = stencil;
    }

    public FrameBuffer getOutputFrameBuffer() {
        return out;
    }

    public void setOutputFrameBuffer(FrameBuffer out) {
        this.out = out;
    }

    public Camera getCamera() {
        return cam;
    }

    public RenderQueue getQueue() {
        return queue;
    }

    public void attachScene(Spatial scene){
        sceneList.add(scene);
    }

    public void detachScene(Spatial scene){
        sceneList.remove(scene);
    }

    public void clearScenes() {
        sceneList.clear();
    }

    public List<Spatial> getScenes(){
        return sceneList;
    }

    public void setBackgroundColor(ColorRGBA background){
        backColor.set(background);
    }

    public ColorRGBA getBackgroundColor(){
        return backColor;
    }
    
    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }
    
    public boolean isEnabled() {
        return enabled;
    }

}
