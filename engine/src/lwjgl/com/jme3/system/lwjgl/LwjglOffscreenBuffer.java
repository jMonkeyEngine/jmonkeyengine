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

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.*;

public class LwjglOffscreenBuffer extends LwjglContext implements Runnable {

    private static final Logger logger = Logger.getLogger(LwjglOffscreenBuffer.class.getName());
    private Pbuffer pbuffer;
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    private int width;
    private int height;
    private PixelFormat pixelFormat;

    protected void initInThread(){
        if ((Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) == 0){
            logger.severe("Offscreen surfaces are not supported.");
            return;
        }

	   int samples = 0;
         if (settings.getSamples() > 1){
              samples = settings.getSamples();
        }
        pixelFormat = new PixelFormat(settings.getBitsPerPixel(),
                                      0,
                                      settings.getDepthBits(),
                                      settings.getStencilBits(),
                                      settings.getSamples());
        
        width = settings.getWidth();
        height = settings.getHeight();
        try{
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread thread, Throwable thrown) {
                    listener.handleError("Uncaught exception thrown in "+thread.toString(), thrown);
                }
            });

            pbuffer = new Pbuffer(width, height, pixelFormat, null, null, createContextAttribs());
            pbuffer.makeCurrent();

            renderable.set(true);

            logger.info("Offscreen buffer created.");
            printContextInitInfo();
        } catch (LWJGLException ex){
            listener.handleError("Failed to create display", ex);
        } finally {
            // TODO: It is possible to avoid "Failed to find pixel format"
            // error here by creating a default display.
        }
        super.internalCreate();
        listener.initialize();
    }

    protected boolean checkGLError(){
        try {
            Util.checkGLError();
        } catch (OpenGLException ex){
            listener.handleError("An OpenGL error has occured!", ex);
        }
        // NOTE: Always return true since this is used in an "assert" statement
        return true;
    }

    protected void runLoop(){
        if (!created.get())
            throw new IllegalStateException();

        if (pbuffer.isBufferLost()){
            pbuffer.destroy();
            try{
                pbuffer = new Pbuffer(width, height, pixelFormat, null);
                pbuffer.makeCurrent();
            }catch (LWJGLException ex){
                listener.handleError("Failed to restore pbuffer content", ex);
            }
        }

        listener.update();
        assert checkGLError();
        
        renderer.onFrame();
        
        int frameRate = settings.getFrameRate();
        if (frameRate >= 1){
            Display.sync(frameRate);
        }
    }

    protected void deinitInThread(){
        renderable.set(false);
        
        listener.destroy();
        renderer.cleanup();
        pbuffer.destroy();
        logger.info("Offscreen buffer destroyed.");
    }

    public void run(){
        logger.log(Level.INFO, "Using LWJGL {0}", Sys.getVersion());
        initInThread();
        while (!needClose.get()){
            runLoop();
        }
        deinitInThread();
    }

    public void destroy(boolean waitFor){
        needClose.set(true);
        if (waitFor)
            waitFor(false);
    }

    public void create(boolean waitFor){
        if (created.get()){
            logger.warning("create() called when pbuffer is already created!");
            return;
        }

        new Thread(this, "LWJGL Renderer Thread").start();
        if (waitFor)
            waitFor(true);
    }

    public void restart() {
    }

    public void setAutoFlushFrames(boolean enabled){
    }

    public Type getType() {
        return Type.OffscreenSurface;
    }

    public MouseInput getMouseInput() {
        return new DummyMouseInput();
    }

    public KeyInput getKeyInput() {
        return new DummyKeyInput();
    }

    public JoyInput getJoyInput() {
        return null;
    }
    
    public TouchInput getTouchInput() {
        return null;
    }

    public void setTitle(String title) {
    }

}
