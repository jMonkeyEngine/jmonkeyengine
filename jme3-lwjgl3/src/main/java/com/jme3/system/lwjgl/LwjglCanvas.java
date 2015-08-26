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

package com.jme3.system.lwjgl;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;

public class LwjglCanvas extends LwjglWindow implements JmeCanvasContext {

    protected static final int TASK_NOTHING = 0,
                               TASK_DESTROY_DISPLAY = 1,
                               TASK_CREATE_DISPLAY = 2,
                               TASK_COMPLETE = 3;
    
//    protected static final boolean USE_SHARED_CONTEXT =
//                Boolean.parseBoolean(System.getProperty("jme3.canvas.sharedctx", "true"));
    
    protected static final boolean USE_SHARED_CONTEXT = false;
    
    private static final Logger logger = Logger.getLogger(LwjglCanvas.class.getName());
    private Canvas canvas;
    private int width;
    private int height;

    private final Object taskLock = new Object();
    private int desiredTask = TASK_NOTHING;

    private Thread renderThread;
    private boolean runningFirstTime = true;
    private boolean mouseWasGrabbed = false;
    
    private boolean mouseWasCreated = false;
    private boolean keyboardWasCreated = false;

    private long window;

    private class GLCanvas extends Canvas {
        @Override
        public void addNotify(){
            super.addNotify();

            if (renderThread != null && renderThread.getState() == Thread.State.TERMINATED) {
                return; // already destroyed.
            }

            if (renderThread == null){
                logger.log(Level.FINE, "EDT: Creating OGL thread.");

                // Also set some settings on the canvas here.
                // So we don't do it outside the AWT thread.
                canvas.setFocusable(true);
                canvas.setIgnoreRepaint(true);

                renderThread = new Thread(LwjglCanvas.this, THREAD_NAME);
                renderThread.start();
            }else if (needClose.get()){
                return;
            }

            logger.log(Level.FINE, "EDT: Telling OGL to create display ..");
            synchronized (taskLock){
                desiredTask = TASK_CREATE_DISPLAY;
//                while (desiredTask != TASK_COMPLETE){
//                    try {
//                        taskLock.wait();
//                    } catch (InterruptedException ex) {
//                        return;
//                    }
//                }
//                desiredTask = TASK_NOTHING;
            }
//            logger.log(Level.FINE, "EDT: OGL has created the display");
        }

        @Override
        public void removeNotify(){
            if (needClose.get()){
                logger.log(Level.FINE, "EDT: Application is stopped. Not restoring canvas.");
                super.removeNotify();
                return;
            }

            // We must tell GL context to shutdown and wait for it to
            // shutdown, otherwise, issues will occur.
            logger.log(Level.FINE, "EDT: Telling OGL to destroy display ..");
            synchronized (taskLock){
                desiredTask = TASK_DESTROY_DISPLAY;
                while (desiredTask != TASK_COMPLETE){
                    try {
                        taskLock.wait();
                    } catch (InterruptedException ex){
                        super.removeNotify();
                        return;
                    }
                }
                desiredTask = TASK_NOTHING;
            }
            
            logger.log(Level.FINE, "EDT: Acknowledged receipt of canvas death");
            // GL context is dead at this point

            super.removeNotify();
        }
    }

    public LwjglCanvas(){
        super(Type.Canvas);
        canvas = new GLCanvas();
    }

    public void create(boolean waitFor){
        if (renderThread == null){
            logger.log(Level.FINE, "MAIN: Creating OGL thread.");

            renderThread = new Thread(LwjglCanvas.this, THREAD_NAME);
            renderThread.start();
        }
        // do not do anything.
        // superclass's create() will be called at initInThread()
        if (waitFor) {
            waitFor(true);
        }
    }

    public Canvas getCanvas(){
        return canvas;
    }
    
    @Override
    protected void runLoop(){
        if (desiredTask != TASK_NOTHING){
            synchronized (taskLock){
                switch (desiredTask){
                    case TASK_CREATE_DISPLAY:
                        logger.log(Level.FINE, "OGL: Creating display ..");
                        restoreCanvas();
                        listener.gainFocus();
                        desiredTask = TASK_NOTHING;
                        break;
                    case TASK_DESTROY_DISPLAY:
                        logger.log(Level.FINE, "OGL: Destroying display ..");
                        listener.loseFocus();
                        pauseCanvas();
                        break;
                }
                desiredTask = TASK_COMPLETE;
                taskLock.notifyAll();
            }
        }
        
        if (renderable.get()){
            int newWidth = Math.max(canvas.getWidth(), 1);
            int newHeight = Math.max(canvas.getHeight(), 1);

            if (width != newWidth || height != newHeight){
                width = newWidth;
                height = newHeight;
                if (listener != null){
                    listener.reshape(width, height);
                }
            }
        }
        
        super.runLoop();
    }

    private void pauseCanvas(){
        if (mouseInput != null) {
            mouseInput.setCursorVisible(true);
            mouseWasCreated = true;
        }

/*
        if (Mouse.isCreated()){
            if (Mouse.isGrabbed()){
                Mouse.setGrabbed(false);
                mouseWasGrabbed = true;
            }
            mouseWasCreated = true;
            Mouse.destroy();
        }
        if (Keyboard.isCreated()){
            keyboardWasCreated = true;
            Keyboard.destroy();
        }
*/

        renderable.set(false);
        destroyContext();
    }

    /**
     * Called to restore the canvas.
     */
    private void restoreCanvas(){
        logger.log(Level.FINE, "OGL: Waiting for canvas to become displayable..");
        while (!canvas.isDisplayable()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "OGL: Interrupted! ", ex);
            }
        }
        
        logger.log(Level.FINE, "OGL: Creating display context ..");

        // Set renderable to true, since canvas is now displayable.
        renderable.set(true);
        createContext(settings);

        logger.log(Level.FINE, "OGL: Display is active!");

        try {
            if (mouseWasCreated){
//                Mouse.create();
//                if (mouseWasGrabbed){
//                    Mouse.setGrabbed(true);
//                    mouseWasGrabbed = false;
//                }
            }
            if (keyboardWasCreated){
//                Keyboard.create();
//                keyboardWasCreated = false;
            }
        } catch (Exception ex){
            logger.log(Level.SEVERE, "Encountered exception when restoring input", ex);
        }

        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                canvas.requestFocus();
            }
        });
    }
    
/*
    */
/**
     * Makes sure the pbuffer is available and ready for use
     *//*

    protected void makePbufferAvailable() throws LWJGLException{
        if (pbuffer != null && pbuffer.isBufferLost()){
            logger.log(Level.WARNING, "PBuffer was lost!");
            pbuffer.destroy();
            pbuffer = null;
        }
        
        if (pbuffer == null) {
            pbuffer = new Pbuffer(1, 1, acquirePixelFormat(true), null);
            pbuffer.makeCurrent();
            logger.log(Level.FINE, "OGL: Pbuffer has been created");
            
            // Any created objects are no longer valid
            if (!runningFirstTime){
                renderer.resetGLObjects();
            }
        }
        
        pbuffer.makeCurrent();
        if (!pbuffer.isCurrent()){
            throw new LWJGLException("Pbuffer cannot be made current");
        }
    }
    
    protected void destroyPbuffer(){
        if (pbuffer != null){
            if (!pbuffer.isBufferLost()){
                pbuffer.destroy();
            }
            pbuffer = null;
        }
    }
*/

    /**
     * This is called:
     * 1) When the context thread ends
     * 2) Any time the canvas becomes non-displayable
     */
    protected void destroyContext(){
        // invalidate the state so renderer can resume operation
        if (!USE_SHARED_CONTEXT){
            renderer.cleanup();
        }

        if (window != 0) {
            glfwDestroyWindow(window);
        }

        // TODO: Destroy input


        // The canvas is no longer visible,
        // but the context thread is still running.
        if (!needClose.get()){
            renderer.invalidateState();
        }
    }

    /**
     * This is called:
     * 1) When the context thread starts
     * 2) Any time the canvas becomes displayable again.
     */
    @Override
    protected void createContext(final AppSettings settings) {
        // In case canvas is not visible, we still take framerate
        // from settings to prevent "100% CPU usage"
        allowSwapBuffers = settings.isSwapBuffers();
        
        if (renderable.get()){
            if (!runningFirstTime){
                // because the display is a different opengl context
                // must reset the context state.
                if (!USE_SHARED_CONTEXT){
                    renderer.cleanup();
                }
            }

            super.createContext(settings);
        }

        // At this point, the OpenGL context is active.
        if (runningFirstTime) {
            // THIS is the part that creates the renderer.
            // It must always be called, now that we have the pbuffer workaround.
            initContextFirstTime();
            runningFirstTime = false;
        }
    }
}
