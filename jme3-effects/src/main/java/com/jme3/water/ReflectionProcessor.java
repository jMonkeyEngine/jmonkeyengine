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
package com.jme3.water;

import com.jme3.math.Plane;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

/**
 * Reflection Processor
 * Used to render the reflected scene in an off view port
 */
public class ReflectionProcessor implements SceneProcessor {

    private RenderManager rm;
    private ViewPort vp;
    private Camera reflectionCam;
    private FrameBuffer reflectionBuffer;
    private Plane reflectionClipPlane;
    private AppProfiler prof;

    /**
     * Creates a ReflectionProcessor
     * @param reflectionCam the cam to use for reflection
     * @param reflectionBuffer the FrameBuffer to render to
     * @param reflectionClipPlane the clipping plane
     */
    public ReflectionProcessor(Camera reflectionCam, FrameBuffer reflectionBuffer, Plane reflectionClipPlane) {
        this.reflectionCam = reflectionCam;
        this.reflectionBuffer = reflectionBuffer;
        this.reflectionClipPlane = reflectionClipPlane;
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
        this.vp = vp;
    }

    public void reshape(ViewPort vp, int w, int h) {
    }

    public boolean isInitialized() {
        return rm != null;
    }

    public void preFrame(float tpf) {
    }

    public void postQueue(RenderQueue rq) {
        //we need special treatement for the sky because it must not be clipped
        rm.getRenderer().setFrameBuffer(reflectionBuffer);
        reflectionCam.setProjectionMatrix(null);
        rm.setCamera(reflectionCam, false);
        rm.getRenderer().clearBuffers(true, true, true);
        //Rendering the sky whithout clipping
        rm.getRenderer().setDepthRange(1, 1);
        vp.getQueue().renderQueue(RenderQueue.Bucket.Sky, rm, reflectionCam, true);
        rm.getRenderer().setDepthRange(0, 1);
        //setting the clip plane to the cam
        reflectionCam.setClipPlane(reflectionClipPlane, Plane.Side.Positive);//,1
        rm.setCamera(reflectionCam, false);

    }

    public void postFrame(FrameBuffer out) {
    }

    public void cleanup() {
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        this.prof = profiler;
    }

    /**
     * Internal use only<br>
     * returns the frame buffer
     * @return 
     */
    public FrameBuffer getReflectionBuffer() {
        return reflectionBuffer;
    }

    /**
     * Internal use only<br>
     * sets the frame buffer
     * @param reflectionBuffer 
     */
    public void setReflectionBuffer(FrameBuffer reflectionBuffer) {
        this.reflectionBuffer = reflectionBuffer;
    }

    /**
     * returns the reflection cam
     * @return 
     */
    public Camera getReflectionCam() {
        return reflectionCam;
    }

    /**
     * sets the reflection cam
     * @param reflectionCam 
     */
    public void setReflectionCam(Camera reflectionCam) {
        this.reflectionCam = reflectionCam;
    }

    /**
     * returns the reflection clip plane
     * @return 
     */
    public Plane getReflectionClipPlane() {
        return reflectionClipPlane;
    }

    /**
     * Sets the reflection clip plane
     * @param reflectionClipPlane 
     */
    public void setReflectionClipPlane(Plane reflectionClipPlane) {
        this.reflectionClipPlane = reflectionClipPlane;
    }
}
