/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.lwjgl.JInputJoyInput;
import com.jme3.input.lwjgl.LwjglKeyInput;
import com.jme3.input.lwjgl.LwjglMouseInput;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.Util;

public abstract class LwjglAbstractDisplay extends LwjglContext implements Runnable {

    private static final Logger logger = Logger.getLogger(LwjglAbstractDisplay.class.getName());

    protected AtomicBoolean needClose = new AtomicBoolean(false);
    protected boolean wasActive = false;
    protected int frameRate = 0;
    protected boolean autoFlush = true;
    protected boolean allowSwapBuffers = false;

    /**
     * @return Type.Display or Type.Canvas
     */
    @Override
    public abstract Type getType();

    /**
     * Set the title if it's a windowed display
     * @param title the desired title
     */
    @Override
    public abstract void setTitle(String title);

    /**
     * Restart if it's a windowed or full-screen display.
     */
    @Override
    public abstract void restart();

    /**
     * Apply the settings, changing resolution, etc.
     * @param settings the AppSettings to apply
     * @throws LWJGLException for various error conditions
     */
    protected abstract void createContext(AppSettings settings) throws LWJGLException;

    /**
     * Destroy the context.
     */
    protected abstract void destroyContext();

    /**
     * Does LWJGL display initialization in the OpenGL thread
     * 
     * @return true if successful, otherwise false
     */
    protected boolean initInThread() {
        try {
            if (!JmeSystem.isLowPermissions()) {
                // Enable uncaught exception handler only for current thread
                Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable thrown) {
                        listener.handleError("Uncaught exception thrown in "+thread.toString(), thrown);
                        if (needClose.get()) {
                            // listener.handleError() has requested the
                            // context to close. Satisfy request.
                            deinitInThread();
                        }
                    }
                });
            }

            // For canvas, this will create a Pbuffer,
            // allowing us to query information.
            // When the canvas context becomes available, it will
            // be replaced seamlessly.
            createContext(settings);
            printContextInitInfo();

            created.set(true);
            super.internalCreate();
        } catch (Exception ex) {
            try {
                if (Display.isCreated()) {
                    Display.destroy();
                }
            } catch (Exception ex2){
                logger.log(Level.WARNING, null, ex2);
            }

            listener.handleError("Failed to create display", ex);
            createdLock.notifyAll(); // Release the lock, so start(true) doesn't deadlock.
            return false; // if we failed to create display, do not continue
        }

        listener.initialize();
        return true;
    }

    protected boolean checkGLError() {
        try {
            Util.checkGLError();
        } catch (OpenGLException ex){
            listener.handleError("An OpenGL error has occurred!", ex);
        }
        // NOTE: Always return true since this is used in an "assert" statement
        return true;
    }

    /**
     * execute one iteration of the render loop in the OpenGL thread
     */
    protected void runLoop(){
        if (!created.get())
            throw new IllegalStateException();

        listener.update();
        
        // All this does is call update().
        // If the canvas is not active, there's no need to waste time
        // doing that.
        if (renderable.get()){
            assert checkGLError();

            // calls swap buffers, etc.
            try {
                if (allowSwapBuffers && autoFlush) {
                    Display.update(false);
                } 
            } catch (Throwable ex){
                listener.handleError("Error while swapping buffers", ex);
            }
        }

        int frameRateCap;
        if (autoFlush) {
            frameRateCap = frameRate;
        } else {
            frameRateCap = 20;
        }
        
        if (frameRateCap > 0) {
            // Cap framerate
            Display.sync(frameRateCap);
        }
        
        // check input after we synchronize with framerate.
        // this reduces input lag.
        if (renderable.get()){
            Display.processMessages();
        }
        
        // Subclasses just call GLObjectManager. Clean up objects here.
        // It is safe ... for now.
        renderer.postFrame();
    }

    /**
     * De-initialize in the OpenGL thread.
     */
    protected void deinitInThread(){
        destroyContext();

        listener.destroy();
        logger.fine("Display destroyed.");
        super.internalDestroy();
    }

    @Override
    public void run(){
        if (listener == null) {
            throw new IllegalStateException("SystemListener is not set on context!"
                                          + "Must set with JmeContext.setSystemListener().");
        }

        loadNatives();
        logger.log(Level.FINE, "Using LWJGL {0}", Sys.getVersion());
        if (!initInThread()) {
            logger.log(Level.SEVERE, "Display initialization failed. Cannot continue.");
            return;
        }
        while (true){
            if (renderable.get()){
                if (Display.isCloseRequested())
                    listener.requestClose(false);

                if (wasActive != Display.isActive()) {
                    if (!wasActive) {
                        listener.gainFocus();
                        timer.reset();
                        wasActive = true;
                    } else {
                        listener.loseFocus();
                        wasActive = false;
                    }
                }
            }

            runLoop();

            if (needClose.get())
                break;
        }
        deinitInThread();
    }

    @Override
    public JoyInput getJoyInput() {
        if (joyInput == null){
            joyInput = new JInputJoyInput();
        }
        return joyInput;
    }

    @Override
    public MouseInput getMouseInput() {
        if (mouseInput == null){
            mouseInput = new LwjglMouseInput(this);
        }
        return mouseInput;
    }

    @Override
    public KeyInput getKeyInput() {
        if (keyInput == null){
            keyInput = new LwjglKeyInput(this);
        }
        return keyInput;
    }

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public void setAutoFlushFrames(boolean enabled){
        this.autoFlush = enabled;
    }

    @Override
    public void destroy(boolean waitFor) {
        if (needClose.get()) {
            return; // Already destroyed
        }

        needClose.set(true);
        if (waitFor)
            waitFor(false);
    }

}
