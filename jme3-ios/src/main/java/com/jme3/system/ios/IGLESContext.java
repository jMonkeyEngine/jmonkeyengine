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
package com.jme3.system.ios;

import com.jme3.input.*;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.system.*;
import com.jme3.input.ios.IosInputHandler;
import com.jme3.renderer.ios.IosGL;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GLDebugES;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IGLESContext implements JmeContext {

    private static final Logger logger = Logger.getLogger(IGLESContext.class.getName());
    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected AppSettings settings = new AppSettings(true);
    protected boolean autoFlush = true;

    /*
     * >= OpenGL ES 2.0 (iOS)
     */
    protected GLRenderer renderer;
    protected Timer timer;
    protected SystemListener listener;
    protected IosInputHandler input;
    protected int minFrameDuration = 0;                   // No FPS cap

    public IGLESContext() {
           logger.log(Level.FINE, "IGLESContext constructor");
    }

    @Override
    public Type getType() {
        return Type.Display;
    }

    @Override
    public void setSettings(AppSettings settings) {
        logger.log(Level.FINE, "IGLESContext setSettings");
        this.settings.copyFrom(settings);
        if (input != null) {
            input.loadSettings(settings);
        }
    }

    @Override
    public void setSystemListener(SystemListener listener) {
        logger.log(Level.FINE, "IGLESContext setSystemListener");
        this.listener = listener;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public com.jme3.renderer.Renderer getRenderer() {
        logger.log(Level.FINE, "IGLESContext getRenderer");
        return renderer;
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
    /*
        if (androidSensorJoyInput == null) {
            androidSensorJoyInput = new AndroidSensorJoyInput();
        }
        return androidSensorJoyInput;
        */
        return null;//  new DummySensorJoyInput();
    }

    @Override
    public TouchInput getTouchInput() {
        return input;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public boolean isCreated() {
        logger.log(Level.FINE, "IGLESContext isCreated");
		return created.get();
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        this.autoFlush = enabled;
    }

    @Override
    public boolean isRenderable() {
        logger.log(Level.FINE, "IGLESContext isRenderable");
        return true;// renderable.get();
    }

    @Override
    public void create(boolean waitFor) {
        logger.log(Level.FINE, "IGLESContext create");
        
        GL gl = new IosGL();
        GLExt glext = (GLExt) gl;

//        if (settings.getBoolean("GraphicsDebug")) {
            gl = new GLDebugES(gl, glext, (GLFbo) glext);
            glext = (GLExt) gl;
//        }

        renderer = new GLRenderer(gl, glext, (GLFbo) glext);
        renderer.initialize();
        
        input = new IosInputHandler();
        timer = new NanoTimer();

//synchronized (createdLock){
            created.set(true);
            //createdLock.notifyAll();
        //}

        listener.initialize();

        if (waitFor) {
            //waitFor(true);
        }
        logger.log(Level.FINE, "IGLESContext created");
    }

    public void create() {
        create(false);
    }

    @Override
    public void restart() {
    }

    @Override
    public void destroy(boolean waitFor) {
        logger.log(Level.FINE, "IGLESContext destroy");
		listener.destroy();
		needClose.set(true);
        if (waitFor) {
            //waitFor(false);
        }
    }

    public void destroy() {
        destroy(true);
    }

    protected void waitFor(boolean createdVal) {
        while (renderable.get() != createdVal) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }
    }
}