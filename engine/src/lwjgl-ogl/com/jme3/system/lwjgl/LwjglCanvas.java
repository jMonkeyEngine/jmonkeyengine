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
import com.jme3.system.JmeSystem.Platform;
import java.awt.Canvas;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

public class LwjglCanvas extends LwjglAbstractDisplay implements JmeCanvasContext {

    private static final Logger logger = Logger.getLogger(LwjglDisplay.class.getName());
    private Canvas canvas;
    private int width;
    private int height;

    private final AtomicBoolean needRestoreCanvas = new AtomicBoolean(false);
    private final AtomicBoolean needDestroyCanvas = new AtomicBoolean(false);
    private final CyclicBarrier actionRequiredBarrier = new CyclicBarrier(2);

    private Thread renderThread;
    private boolean runningFirstTime = true;
    private boolean mouseWasGrabbed = false;

    private Pbuffer pbuffer;
    private PixelFormat pixelFormat;

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

            logger.log(Level.INFO, "EDT: Notifying OGL that canvas is visible..");
            needRestoreCanvas.set(true);

            // NOTE: no need to wait for OGL to initialize the canvas,
            // it can happen at any time.
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
            logger.log(Level.INFO, "EDT: Notifying OGL that canvas is about to become invisible..");
            needDestroyCanvas.set(true);
            try {
                actionRequiredBarrier.await();
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "EDT: Interrupted! ", ex);
            } catch (BrokenBarrierException ex){
                logger.log(Level.SEVERE, "EDT: Broken barrier! ", ex);
            }

            // Reset barrier for future use
            actionRequiredBarrier.reset();
            
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
        if (needDestroyCanvas.getAndSet(false)){
            // Destroy canvas
            logger.log(Level.INFO, "OGL: Received destroy request! Complying..");
            try {
                listener.loseFocus();
                pauseCanvas();
            } finally {
                try {
                    // Required to avoid deadlock if an exception occurs
                    actionRequiredBarrier.await();
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, "OGL: Interrupted! ", ex);
                } catch (BrokenBarrierException ex) {
                    logger.log(Level.SEVERE, "OGL: Broken barrier! ", ex);
                }
            }
        }else if (needRestoreCanvas.getAndSet(false)){
            // Put canvas back online
            logger.log(Level.INFO, "OGL: Canvas is now visible! Re-initializing..");
            restoreCanvas();
            listener.gainFocus();
        }
        
        if (width != canvas.getWidth() || height != canvas.getHeight()){
            width = canvas.getWidth();
            height = canvas.getHeight();
            if (listener != null)
                listener.reshape(width, height);
        }
        
        super.runLoop();
    }

    private void pauseCanvas(){
        if (Mouse.isCreated() && Mouse.isGrabbed()){
            Mouse.setGrabbed(false);
            mouseWasGrabbed = true;
        }

        logger.log(Level.INFO, "OGL: Canvas will become invisible! Destroying ..");
        
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
        
        logger.log(Level.INFO, "OGL: Creating display..");

        // Set renderable to true, since canvas is now displayable.
        renderable.set(true);
        createContext(settings);

        logger.log(Level.INFO, "OGL: Display is active!");

        if (Mouse.isCreated() && mouseWasGrabbed){
            Mouse.setGrabbed(true);
            mouseWasGrabbed = false;
        }

        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                canvas.requestFocus();
            }
        });
    }
    
    /**
     * It seems it is best to use one pixel format for all shared contexts.
     * See http://developer.apple.com/library/mac/#qa/qa1248/_index.html.
     */
    protected PixelFormat acquirePixelFormat(){
        if (pixelFormat == null){
            pixelFormat = new PixelFormat(settings.getBitsPerPixel(),
                                          0,
                                          settings.getDepthBits(),
                                          settings.getStencilBits(),
                                          settings.getSamples());
        }
        return pixelFormat;
    }

    /**
     * Makes sure the pbuffer is available and ready for use
     */
    protected void makePbufferAvailable() throws LWJGLException{
        if (pbuffer == null || pbuffer.isBufferLost()){
            if (pbuffer != null && pbuffer.isBufferLost()){
                logger.log(Level.WARNING, "PBuffer was lost!");
                pbuffer.destroy();
            }
            // Let the implementation choose an appropriate pixel format.
            pbuffer = new Pbuffer(1, 1, new PixelFormat(0, 0, 0, 0, 0), null);
            //pbuffer = new Pbuffer(1, 1, acquirePixelFormat(), null);
            logger.log(Level.INFO, "OGL: Pbuffer has been created");
        }
    }
    
    /**
     * This is called:
     * 1) When the context thread ends
     * 2) Any time the canvas becomes non-displayable
     */
    protected void destroyContext(){
        if (Display.isCreated()){
            try {
                // NOTE: On Windows XP, not calling setParent(null)
                // freezes the application.
                // On Mac it freezes the application.
                if (JmeSystem.getPlatform() == Platform.Windows32
                 || JmeSystem.getPlatform() == Platform.Windows64){
                    Display.setParent(null);
                }
            } catch (LWJGLException ex) {
                logger.log(Level.SEVERE, "Encountered exception when setting parent to null", ex);
            }
            Display.destroy();
        }
        
        try {
            // The canvas is no longer visible,
            // but the context thread is still running.
            if (!needClose.get()){
                // MUST make sure there's still a context current here ..
                // Display is dead, make pbuffer available to the system
                makePbufferAvailable();

                // pbuffer is now available, make it current
                pbuffer.makeCurrent();
                
                // invalidate the state so renderer can resume operation
                renderer.invalidateState();
            }else{
                // The context thread is no longer running.
                // Destroy pbuffer.
                if (pbuffer != null){
                    pbuffer.destroy();
                }
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
            // First create the pbuffer, if it is needed.
            makePbufferAvailable();

            if (renderable.get()){
                // if the pbuffer is currently active, 
                // make sure to deactivate it
                if (pbuffer.isCurrent()){
                    pbuffer.releaseContext();
                }

                Display.setVSyncEnabled(settings.isVSync());
                Display.setParent(canvas);
                Display.create(acquirePixelFormat(), pbuffer);
                
                // because the display is a different opengl context
                // must reset the context state.
                renderer.invalidateState();
            }else{
                pbuffer.makeCurrent();
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
