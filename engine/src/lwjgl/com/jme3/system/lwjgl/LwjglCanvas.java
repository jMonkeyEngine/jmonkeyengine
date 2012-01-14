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

import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import java.awt.Canvas;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

public class LwjglCanvas extends LwjglAbstractDisplay implements JmeCanvasContext {

    protected static final int TASK_NOTHING = 0,
                               TASK_DESTROY_DISPLAY = 1,
                               TASK_CREATE_DISPLAY = 2,
                               TASK_COMPLETE = 3;
    
//    protected static final boolean USE_SHARED_CONTEXT =
//                Boolean.parseBoolean(System.getProperty("jme3.canvas.sharedctx", "true"));
    
    protected static final boolean USE_SHARED_CONTEXT = false;
    
    private static final Logger logger = Logger.getLogger(LwjglDisplay.class.getName());
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

    private Pbuffer pbuffer;
    private PixelFormat pbufferFormat;
    private PixelFormat canvasFormat;

    private class GLCanvas extends Canvas {
        @Override
        public void addNotify(){
            super.addNotify();

            if (renderThread != null && renderThread.getState() == Thread.State.TERMINATED)
                return; // already destroyed.

            if (renderThread == null){
                logger.log(Level.INFO, "EDT: Creating OGL thread.");

                // Also set some settings on the canvas here.
                // So we don't do it outside the AWT thread.
                canvas.setFocusable(true);
                canvas.setIgnoreRepaint(true);

                renderThread = new Thread(LwjglCanvas.this, "LWJGL Renderer Thread");
                renderThread.start();
            }else if (needClose.get()){
                return;
            }

            logger.log(Level.INFO, "EDT: Telling OGL to create display ..");
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
//            logger.log(Level.INFO, "EDT: OGL has created the display");
        }

        @Override
        public void removeNotify(){
            if (needClose.get()){
                logger.log(Level.INFO, "EDT: Application is stopped. Not restoring canvas.");
                super.removeNotify();
                return;
            }

            // We must tell GL context to shutdown and wait for it to
            // shutdown, otherwise, issues will occur.
            logger.log(Level.INFO, "EDT: Telling OGL to destroy display ..");
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
            
            logger.log(Level.INFO, "EDT: Acknowledged receipt of canvas death");
            // GL context is dead at this point

            super.removeNotify();
        }
    }

    public LwjglCanvas(){
        super();
        canvas = new GLCanvas();
    }

    @Override
    public Type getType() {
        return Type.Canvas;
    }

    public void create(boolean waitFor){
        if (renderThread == null){
            logger.log(Level.INFO, "MAIN: Creating OGL thread.");

            renderThread = new Thread(LwjglCanvas.this, "LWJGL Renderer Thread");
            renderThread.start();
        }
        // do not do anything.
        // superclass's create() will be called at initInThread()
        if (waitFor)
            waitFor(true);
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public void restart() {
        frameRate = settings.getFrameRate();
        // TODO: Handle other cases, like change of pixel format, etc.
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
                        logger.log(Level.INFO, "OGL: Creating display ..");
                        restoreCanvas();
                        listener.gainFocus();
                        desiredTask = TASK_NOTHING;
                        break;
                    case TASK_DESTROY_DISPLAY:
                        logger.log(Level.INFO, "OGL: Destroying display ..");
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
        }else{
            if (frameRate <= 0){
                // NOTE: MUST be done otherwise 
                // Windows OS will freeze
                Display.sync(30);
            }
        }
        
        super.runLoop();
    }

    private void pauseCanvas(){
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

        renderable.set(false);
        destroyContext();
    }

    /**
     * Called to restore the canvas.
     */
    private void restoreCanvas(){
        logger.log(Level.INFO, "OGL: Waiting for canvas to become displayable..");
        while (!canvas.isDisplayable()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "OGL: Interrupted! ", ex);
            }
        }
        
        logger.log(Level.INFO, "OGL: Creating display context ..");

        // Set renderable to true, since canvas is now displayable.
        renderable.set(true);
        createContext(settings);

        logger.log(Level.INFO, "OGL: Display is active!");

        try {
            if (mouseWasCreated){
                Mouse.create();
                if (mouseWasGrabbed){
                    Mouse.setGrabbed(true);
                    mouseWasGrabbed = false;
                }
            }
            if (keyboardWasCreated){
                Keyboard.create();
                keyboardWasCreated = false;
            }
        } catch (LWJGLException ex){
            logger.log(Level.SEVERE, "Encountered exception when restoring input", ex);
        }

        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                canvas.requestFocus();
            }
        });
    }
    
    /**
     * It seems it is best to use one pixel format for all shared contexts.
     * @see <a href="http://developer.apple.com/library/mac/#qa/qa1248/_index.html">http://developer.apple.com/library/mac/#qa/qa1248/_index.html</a>
     */
    protected PixelFormat acquirePixelFormat(boolean forPbuffer){
        if (forPbuffer){
            // Use 0 samples for pbuffer format, prevents
            // crashes on bad drivers
            if (pbufferFormat == null){
                pbufferFormat = new PixelFormat(settings.getBitsPerPixel(),
                                                0,
                                                settings.getDepthBits(),
                                                settings.getStencilBits(),
                                                0);
            }
            return pbufferFormat;
        }else{
            if (canvasFormat == null){
			int samples = 0;
		      if (settings.getSamples() > 1){
                    samples = settings.getSamples();
                }
                canvasFormat = new PixelFormat(settings.getBitsPerPixel(),
                                               0,
                                               settings.getDepthBits(),
                                               settings.getStencilBits(),
                                               samples);
            }
            return canvasFormat;
        }
    }

    /**
     * Makes sure the pbuffer is available and ready for use
     */
    protected void makePbufferAvailable() throws LWJGLException{
        if (pbuffer != null && pbuffer.isBufferLost()){
            logger.log(Level.WARNING, "PBuffer was lost!");
            pbuffer.destroy();
            pbuffer = null;
        }
        
        if (pbuffer == null) {
            pbuffer = new Pbuffer(1, 1, acquirePixelFormat(true), null);
            pbuffer.makeCurrent();
            logger.log(Level.INFO, "OGL: Pbuffer has been created");
            
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
    
    /**
     * This is called:
     * 1) When the context thread ends
     * 2) Any time the canvas becomes non-displayable
     */
    protected void destroyContext(){
        try {
            // invalidate the state so renderer can resume operation
            if (!USE_SHARED_CONTEXT){
                renderer.cleanup();
            }
            
            if (Display.isCreated()){
                /* FIXES:
                 * org.lwjgl.LWJGLException: X Error
                 * BadWindow (invalid Window parameter) request_code: 2 minor_code: 0
                 * 
                 * Destroying keyboard early prevents the error above, triggered
                 * by destroying keyboard in by Display.destroy() or Display.setParent(null).
                 * Therefore Keyboard.destroy() should precede any of these calls.
                 */
                if (Keyboard.isCreated()){
                    // Should only happen if called in 
                    // LwjglAbstractDisplay.deinitInThread().
                    Keyboard.destroy();
                }

                //try {
                    // NOTE: On Windows XP, not calling setParent(null)
                    // freezes the application.
                    // On Mac it freezes the application.
                    // On Linux it fixes a crash with X Window System.
                    if (JmeSystem.getPlatform() == Platform.Windows32
                     || JmeSystem.getPlatform() == Platform.Windows64){
                        //Display.setParent(null);
                    }
                //} catch (LWJGLException ex) {
                //    logger.log(Level.SEVERE, "Encountered exception when setting parent to null", ex);
                //}

                Display.destroy();
            }
            
            // The canvas is no longer visible,
            // but the context thread is still running.
            if (!needClose.get()){
                // MUST make sure there's still a context current here ..
                // Display is dead, make pbuffer available to the system
                makePbufferAvailable();
                
                renderer.invalidateState();
            }else{
                // The context thread is no longer running.
                // Destroy pbuffer.
                destroyPbuffer();
            }
        } catch (LWJGLException ex) {
            listener.handleError("Failed make pbuffer available", ex);
        }
    }

    /**
     * This is called:
     * 1) When the context thread starts
     * 2) Any time the canvas becomes displayable again.
     */
    @Override
    protected void createContext(AppSettings settings) {
        // In case canvas is not visible, we still take framerate
        // from settings to prevent "100% CPU usage"
        frameRate = settings.getFrameRate();
        
        try {
            if (renderable.get()){
                if (!runningFirstTime){
                    // because the display is a different opengl context
                    // must reset the context state.
                    if (!USE_SHARED_CONTEXT){
                        renderer.cleanup();
                    }
                }
                
                // if the pbuffer is currently active, 
                // make sure to deactivate it
                destroyPbuffer();
                
                if (Keyboard.isCreated()){
                    Keyboard.destroy();
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                
                Display.setVSyncEnabled(settings.isVSync());
                Display.setParent(canvas);
                
                if (USE_SHARED_CONTEXT){
                    Display.create(acquirePixelFormat(false), pbuffer);
                }else{
                    Display.create(acquirePixelFormat(false));
                }
                
                renderer.invalidateState();
            }else{
                // First create the pbuffer, if it is needed.
                makePbufferAvailable();
            }

            // At this point, the OpenGL context is active.
            if (runningFirstTime){
                // THIS is the part that creates the renderer.
                // It must always be called, now that we have the pbuffer workaround.
                initContextFirstTime();
                runningFirstTime = false;
            }
        } catch (LWJGLException ex) {
            listener.handleError("Failed to initialize OpenGL context", ex);
            // TODO: Fix deadlock that happens after the error (throw runtime exception?)
        }
    }
}
