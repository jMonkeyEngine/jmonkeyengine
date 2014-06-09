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
import com.jme3.renderer.Renderer;
import com.jme3.renderer.jogl.JoglRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.NanoTimer;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;

public abstract class JoglContext implements JmeContext {

    private static final Logger logger = Logger.getLogger(JoglContext.class.getName());
    
    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean renderable = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected JoglRenderer renderer;
    protected Timer timer;
    protected SystemListener listener;

    protected KeyInput keyInput;
    protected MouseInput mouseInput;
    protected JoyInput joyInput;

    public void loadNatives() {
        // Not sure if need to load OpenAL here ...
        if (NativeLibraryLoader.isUsingNativeBullet()) {
            NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
        }
    }
    
    public void setSystemListener(SystemListener listener){
        this.listener = listener;
    }

    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }
    
    public boolean isRenderable(){
        return renderable.get();
    }

    public AppSettings getSettings() {
        return settings;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public MouseInput getMouseInput() {
        return mouseInput;
    }

    public KeyInput getKeyInput() {
        return keyInput;
    }

    public JoyInput getJoyInput() {
        return joyInput;
    }

    public Timer getTimer() {
        return timer;
    }

    public boolean isCreated() {
        return created.get();
    }

    public void create(){
        create(false);
    }

    public void destroy(){
        destroy(false);
    }

    protected void waitFor(boolean createdVal){
        synchronized (createdLock){
            while (created.get() != createdVal){
                try {
                    createdLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

     public void internalCreate() {
        timer = new NanoTimer();
        synchronized (createdLock){
            created.set(true);
            createdLock.notifyAll();
        }
    }

    protected void internalDestroy() {
        renderer = null;
        timer = null;
        renderable.set(false);
        synchronized (createdLock){
            created.set(false);
            createdLock.notifyAll();
        }
    }
    
    protected int determineMaxSamples(int requestedSamples) {
        GL gl = GLContext.getCurrentGL();
        if (gl.hasFullFBOSupport()) {
            return gl.getMaxRenderbufferSamples();
        } else {
            if (gl.isExtensionAvailable("GL_ARB_framebuffer_object")
                    || gl.isExtensionAvailable("GL_EXT_framebuffer_multisample")) {
                IntBuffer intBuf1 = IntBuffer.allocate(1);
                gl.glGetIntegerv(GL2GL3.GL_MAX_SAMPLES, intBuf1);
                return intBuf1.get(0);
            } else {
                return Integer.MAX_VALUE;
            }
        }
    }
    
    protected int getNumSamplesToUse() {
        int samples = 0;
        if (settings.getSamples() > 1){
            samples = settings.getSamples();
            int supportedSamples = determineMaxSamples(samples);
            if (supportedSamples < samples) {
                logger.log(Level.WARNING,
                        "Couldn''t satisfy antialiasing samples requirement: x{0}. "
                        + "Video hardware only supports: x{1}",
                        new Object[]{samples, supportedSamples});
                
                samples = supportedSamples;
            }
        }
        return samples;
    }
}
