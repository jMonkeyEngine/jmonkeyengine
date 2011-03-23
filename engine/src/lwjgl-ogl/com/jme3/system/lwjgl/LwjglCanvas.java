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
import java.awt.Canvas;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
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
    private boolean mouseActive, keyboardActive, joyActive;

    public LwjglCanvas(){
        super();

        canvas = new Canvas(){
            @Override
            public void addNotify(){
                super.addNotify();
                
                if (renderThread != null && renderThread.getState() == Thread.State.TERMINATED)
                    return; // already destroyed.

                if (renderThread == null){
                    logger.log(Level.INFO, "EDT: Creating OGL thread.");
                    
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
                logger.log(Level.INFO, "EDT: Sending destroy request..");
                needDestroyCanvas.set(true);
                try {
                    actionRequiredBarrier.await();
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, "EDT: Interrupted! ", ex);
                } catch (BrokenBarrierException ex){
                    logger.log(Level.SEVERE, "EDT: Broken barrier! ", ex);
                }
                
                logger.log(Level.INFO, "EDT: Acknowledged receipt of destroy request!");
                // GL context is dead at this point

                // Reset barrier for future use
                actionRequiredBarrier.reset();

                super.removeNotify();
            }
        };
        
        canvas.setFocusable(true);
        canvas.setIgnoreRepaint(true);
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
        mouseActive = Mouse.isCreated();
        keyboardActive = Keyboard.isCreated();
        joyActive = Controllers.isCreated();

        if (mouseActive && Mouse.isGrabbed()){
            Mouse.setGrabbed(false);
            mouseWasGrabbed = true;
        }

        if (mouseActive)
            Mouse.destroy();
        if (keyboardActive)
            Keyboard.destroy();
        if (joyActive)
            Controllers.destroy();

        logger.log(Level.INFO, "OGL: Destroying display (temporarily)");
        Display.destroy();

        renderable.set(false);
    }

    /**
     * Called to restore the canvas.
     */
    private void restoreCanvas(){
        logger.log(Level.INFO, "OGL: Waiting for canvas to become displayable..");
        while (!canvas.isDisplayable()){
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "OGL: Interrupted! ", ex);
            }
        }
        
        renderer.resetGLObjects();
        logger.log(Level.INFO, "OGL: Creating display..");

        // Set renderable to true, since canvas is now displayable.
        renderable.set(true);
        createContext(settings);

        logger.log(Level.INFO, "OGL: Waiting for display to become active..");
        while (!Display.isCreated()){
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "OGL: Interrupted! ", ex);
            }
        }
        logger.log(Level.INFO, "OGL: Display is active!");

        try {
            if (mouseActive){
                Mouse.create();
                if (mouseWasGrabbed){
                    Mouse.setGrabbed(true);
                    mouseWasGrabbed = false;
                }
            }
            if (keyboardActive){
                Keyboard.create();
            }
            logger.log(Level.INFO, "OGL: Input has been reinitialized");
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "Failed to re-init input", ex);
        }

        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                canvas.requestFocus();
            }
        });
    }

    @Override
    protected void createContext(AppSettings settings) {
        if (!renderable.get())
            return;

        frameRate = settings.getFrameRate();
        Display.setVSyncEnabled(settings.isVSync());

        try{
            Display.setParent(canvas);
            PixelFormat pf = new PixelFormat(settings.getBitsPerPixel(),
                                             0,
                                             settings.getDepthBits(),
                                             settings.getStencilBits(),
                                             settings.getSamples());
            Display.create(pf);
            Display.makeCurrent();

            if (runningFirstTime){
                initContextFirstTime();
                runningFirstTime = false;
            }
        }catch (LWJGLException ex){
            listener.handleError("Failed to parent canvas to display", ex);
        }
    }

}
