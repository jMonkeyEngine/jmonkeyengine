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

package com.jme3.system.lwjgl;

import com.jme3.renderer.Renderer;
import com.jme3.renderer.lwjgl.LwjglGL1Renderer;
import com.jme3.renderer.lwjgl.LwjglRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.JmeContext;
import com.jme3.system.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.opengl.GLContext;

/**
 * A LWJGL implementation of a graphics context.
 */
public abstract class LwjglContext implements JmeContext {

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Renderer renderer;
    protected Timer timer;
    protected SystemListener listener;

    public void setSystemListener(SystemListener listener){
        this.listener = listener;
    }

    public void internalDestroy(){
        renderer = null;
        timer = null;
        synchronized (createdLock){
            created.set(false);
            createdLock.notifyAll();
        }
    }
    
    public void internalCreate(){
        timer = new LwjglTimer();
        if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL2)
         || settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)){
            renderer = new LwjglRenderer();
            ((LwjglRenderer)renderer).initialize();
        }else if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL1)){
            renderer = new LwjglGL1Renderer();
            ((LwjglGL1Renderer)renderer).initialize();
        }else{
            throw new UnsupportedOperationException("Unsupported renderer: " + settings.getRenderer());
        }
        synchronized (createdLock){
            created.set(true);
            createdLock.notifyAll();
        }
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

    public boolean isCreated(){
        return created.get();
    }

    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }

    public AppSettings getSettings(){
        return settings;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Timer getTimer() {
        return timer;
    }

}
