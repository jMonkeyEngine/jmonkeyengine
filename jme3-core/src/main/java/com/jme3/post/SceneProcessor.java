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
package com.jme3.post;

import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

/**
 * Scene processors are used to compute/render things before and after the classic render of the scene.
 * They have to be added to a viewport and are rendered in the order they've been added
 *
 * @author Kirill Vainer
 */
public interface SceneProcessor {

    /**
     * Called in the render thread to initialize the scene processor.
     *
     * @param rm The render manager to which the SP was added to
     * @param vp The viewport to which the SP is assigned
     */
    public void initialize(RenderManager rm, ViewPort vp);

    /**
     * Called when the resolution of the viewport has been changed.
     * @param vp
     */
    public void reshape(ViewPort vp, int w, int h);

    /**
     * @return True if initialize() has been called on this SceneProcessor,
     * false if otherwise.
     */
    public boolean isInitialized();

    /**
     * Called before a frame
     *
     * @param tpf Time per frame
     */
    public void preFrame(float tpf);

    /**
     * Called after the scene graph has been queued, but before it is flushed.
     *
     * @param rq The render queue
     */
    public void postQueue(RenderQueue rq);

    /**
     * Called after a frame has been rendered and the queue flushed.
     *
     * @param out The FB to which the scene was rendered.
     */
    public void postFrame(FrameBuffer out);

    /**
     * Called when the SP is removed from the RM.
     */
    public void cleanup();

    /**
     * Sets a profiler Instance for this processor.
     *
     * @param profiler the profiler instance.
     */
    public void setProfiler(AppProfiler profiler);

}
