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
package com.jme3.system.jogl;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jogamp.newt.NewtVersion;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLOffscreenAutoDrawable;
import javax.media.opengl.GLProfile;


public class JoglOffscreenBuffer extends JoglContext implements Runnable {
    
    private static final Logger logger = Logger.getLogger(JoglOffscreenBuffer.class.getName());
    private GLOffscreenAutoDrawable offscreenDrawable;
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    private int width;
    private int height;
    private GLCapabilities caps;

    protected void initInThread(){
        GL gl = GLContext.getCurrentGL();
        if (!gl.hasFullFBOSupport()){
            logger.severe("Offscreen surfaces are not supported.");
            return;
        }

        int samples = getNumSamplesToUse();
        caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));
        caps.setHardwareAccelerated(true);
        caps.setDoubleBuffered(true);
        caps.setStencilBits(settings.getStencilBits());
        caps.setDepthBits(settings.getDepthBits());
        caps.setOnscreen(false);
        caps.setSampleBuffers(true);
        caps.setNumSamples(samples);

        offscreenDrawable = GLDrawableFactory.getFactory(GLProfile.getMaxFixedFunc(true)).createOffscreenAutoDrawable(null, caps, null, width, height, null);
        
        offscreenDrawable.display();
        
        renderable.set(true);
        
        logger.fine("Offscreen buffer created.");
        
        super.internalCreate();
        listener.initialize();
    }

    protected boolean checkGLError(){
        //FIXME
        // NOTE: Always return true since this is used in an "assert" statement
        return true;
    }
    
    protected void runLoop(){
        if (!created.get()) {
            throw new IllegalStateException();
        }

        listener.update();
        checkGLError();

        renderer.onFrame();

        int frameRate = settings.getFrameRate();
        if (frameRate >= 1) {
            //FIXME
        }
    }

    protected void deinitInThread(){
        renderable.set(false);

        listener.destroy();
        renderer.cleanup();
        offscreenDrawable.destroy();
        logger.fine("Offscreen buffer destroyed.");
        
        super.internalDestroy();
    }

    public void run(){
        loadNatives();
        logger.log(Level.FINE, "Using JOGL {0}", NewtVersion.getInstance().getImplementationVersion());
        initInThread();
        while (!needClose.get()){
            runLoop();
        }
        deinitInThread();
    }

    public void destroy(boolean waitFor){
        needClose.set(true);
        if (waitFor) {
            waitFor(false);
        }
    }

    public void create(boolean waitFor){
        if (created.get()){
            logger.warning("create() called when pbuffer is already created!");
            return;
        }

        new Thread(this, "JOGL Renderer Thread").start();
        if (waitFor) {
            waitFor(true);
        }
    }

    public void restart() {
    }

    public void setAutoFlushFrames(boolean enabled){
    }

    public Type getType() {
        return Type.OffscreenSurface;
    }

    @Override
    public MouseInput getMouseInput() {
        return new DummyMouseInput();
    }

    @Override
    public KeyInput getKeyInput() {
        return new DummyKeyInput();
    }

    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    public TouchInput getTouchInput() {
        return null;
    }

    public void setTitle(String title) {
    }
}
