/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.system;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NullContext implements JmeContext, Runnable {

    protected static final Logger logger = Logger.getLogger(NullContext.class.getName());

    protected static final String THREAD_NAME = "jME3 Headless Main";
    
    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected int frameRate;
    protected AppSettings settings = new AppSettings(true);
    protected Timer timer;
    protected SystemListener listener;
    protected NullRenderer renderer;

    @Override
    public Type getType() {
        return Type.Headless;
    }

    /**
     * Accesses the listener that receives events related to this context.
     *
     * @return the pre-existing instance
     */
    @Override
    public SystemListener getSystemListener() {
        return listener;
    }

    @Override
    public void setSystemListener(SystemListener listener){
        this.listener = listener;
    }

    protected void initInThread(){
        logger.fine("NullContext created.");
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Running on thread: {0}", Thread.currentThread().getName());
        }

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable thrown) {
                listener.handleError("Uncaught exception thrown in "+thread.toString(), thrown);
            }
        });

        timer = new NanoTimer();
        renderer = new NullRenderer();
        synchronized (createdLock){
            created.set(true);
            createdLock.notifyAll();
        }

        listener.initialize();
    }

    protected void deinitInThread(){
        listener.destroy();
        timer = null;
        synchronized (createdLock){
            created.set(false);
            createdLock.notifyAll();
        }
    }

    private long timeThen;
    private long timeLate;

    public void sync(int fps) {
        long timeNow;
        long gapTo;
        long savedTimeLate;

        gapTo = timer.getResolution() / fps + timeThen;
        timeNow = timer.getTime();
        savedTimeLate = timeLate;

        try {
            while (gapTo > timeNow + savedTimeLate) {
                Thread.sleep(1);
                timeNow = timer.getTime();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (gapTo < timeNow) {
            timeLate = timeNow - gapTo;
        } else {
            timeLate = 0;
        }

        timeThen = timeNow;
    }

    @Override
    public void run(){
        initInThread();

        do {
            listener.update();

            if (frameRate > 0) {
                sync(frameRate);
            }
        } while (!needClose.get());

        deinitInThread();

        logger.fine("NullContext destroyed.");
    }

    @Override
    public void destroy(boolean waitFor){
        needClose.set(true);
        if (waitFor)
            waitFor(false);
    }

    @Override
    public void create(boolean waitFor){
        if (created.get()){
            logger.warning("create() called when NullContext is already created!");
            return;
        }

        new Thread(this, THREAD_NAME).start();
        if (waitFor)
            waitFor(true);
    }

    @Override
    public void restart() {
    }

    @Override
    public void setAutoFlushFrames(boolean enabled){
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

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public void setTitle(String title) {
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

    @Override
    public boolean isCreated(){
        return created.get();
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
        frameRate = settings.getFrameRate();
        if (frameRate <= 0)
            frameRate = 60; // use default update rate.
    }

    @Override
    public AppSettings getSettings(){
        return settings;
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public boolean isRenderable() {
        return true; // Doesn't really matter if true or false. Either way
                     // RenderManager won't render anything.
    }

    @Override
    public Context getOpenCLContext() {
        return null;
    }

    /**
     * Returns the height of the framebuffer.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getFramebufferHeight() {
        throw new UnsupportedOperationException("null context");
    }

    /**
     * Returns the width of the framebuffer.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getFramebufferWidth() {
        throw new UnsupportedOperationException("null context");
    }

    /**
     * Returns the screen X coordinate of the left edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowXPosition() {
        throw new UnsupportedOperationException("null context");
    }

    /**
     * Returns the screen Y coordinate of the top edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowYPosition() {
        throw new UnsupportedOperationException("null context");
    }
}
