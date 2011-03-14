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

    private AtomicBoolean reinitReq = new AtomicBoolean(false);
    private final Object reinitReqLock = new Object();

    private AtomicBoolean reinitAuth = new AtomicBoolean(false);
    private final Object reinitAuthLock = new Object();

    private Thread renderThread;
    private boolean mouseWasGrabbed = false;
//    private Pbuffer dummyCtx;

    public LwjglCanvas(){
        super();

        canvas = new Canvas(){
            
            @Override
            public void addNotify(){
                super.addNotify();
                if (renderThread == null || renderThread.getState() == Thread.State.TERMINATED){
                    if (renderThread != null && renderThread.getState() == Thread.State.TERMINATED){
                        logger.log(Level.INFO, "EDT: Creating OGL thread. Was terminated.");
                    }else{
                        logger.log(Level.INFO, "EDT: Creating OGL thread.");
                    }
                    renderThread = new Thread(LwjglCanvas.this, "LWJGL Renderer Thread");
                    renderThread.start();
                }else{
                    if (needClose.get())
                        return;

                    logger.log(Level.INFO, "EDT: Sending re-init authorization..");

                    // reinitializing canvas
                    synchronized (reinitAuthLock){
                        reinitAuth.set(true);
                        reinitAuthLock.notifyAll();
                    }
                }
            }

            @Override
            public void removeNotify(){
                if (needClose.get()){
                    logger.log(Level.INFO, "EDT: Close requested. Not re-initing.");
                    return;
                }
                
                // request to put context into reinit mode
                // this waits until reinit is authorized
                logger.log(Level.INFO, "EDT: Sending re-init request..");
                synchronized (reinitReqLock){
                    reinitReq.set(true);
                    while (reinitReq.get()){
                        try {
                            reinitReqLock.wait();
                        } catch (InterruptedException ex) {
                            logger.log(Level.SEVERE, "EDT: Interrupted! ", ex);
                        }
                    }
                    // NOTE: reinitReq is now false.
                }
                logger.log(Level.INFO, "EDT: Acknowledged receipt of re-init request!");
                
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
        boolean reinitNeeded;
        synchronized (reinitReqLock){
            reinitNeeded = reinitReq.get();
        }
        
        if (reinitNeeded){
            logger.log(Level.INFO, "OGL: Re-init request received!");
            listener.loseFocus();

            boolean mouseActive = Mouse.isCreated();
            boolean keyboardActive = Keyboard.isCreated();
            boolean joyActive = Controllers.isCreated();

            if (mouseActive)
                Mouse.destroy();
            if (keyboardActive)
                Keyboard.destroy();
            if (joyActive)
                Controllers.destroy();

            pauseCanvas();

            synchronized (reinitReqLock){
                reinitReq.set(false);
                reinitReqLock.notifyAll();
            }

            // we got the reinit request, now we wait for reinit to happen..
            logger.log(Level.INFO, "OGL: Waiting for re-init authorization..");
            synchronized (reinitAuthLock){
                while (!reinitAuth.get()){
                    try {
                        reinitAuthLock.wait();
                        if (Thread.interrupted())
                            throw new InterruptedException();
                    } catch (InterruptedException ex) {
                        if (needClose.get()){
                            logger.log(Level.INFO, "OGL: Re-init aborted. Closing display..");
                            return;
                        }

                        logger.log(Level.SEVERE, "OGL: Interrupted! ", ex);
                    }
                }
                // NOTE: reinitAuth becamse true, now set it to false.
                reinitAuth.set(false);
            }
            
            logger.log(Level.INFO, "OGL: Re-init authorization received. Re-initializing..");
            restoreCanvas();

            try {
                if (mouseActive){
                    Mouse.create();
                }
                if (keyboardActive){
                    Keyboard.create();
                }
                if (joyActive){
                    Controllers.create();
                }
            } catch (LWJGLException ex){
                listener.handleError("Failed to re-init input", ex);
            }
        }
        if (width != canvas.getWidth() || height != canvas.getHeight()){
            width = canvas.getWidth();
            height = canvas.getHeight();
            if (listener != null)
                listener.reshape(width, height);
        }
        super.runLoop();
    }

    @Override
    public void destroy(boolean waitFor){
        needClose.set(true);
        if (renderThread != null && renderThread.isAlive()){
            renderThread.interrupt();
            // make sure it really does get interrupted
            synchronized(reinitAuthLock){
                reinitAuthLock.notifyAll();
            }
        }
        if (waitFor)
            waitFor(false);
    }

    private void pauseCanvas(){
        if (Mouse.isCreated() && Mouse.isGrabbed()){
            Mouse.setGrabbed(false);
            mouseWasGrabbed = true;
        }

        logger.log(Level.INFO, "OGL: Destroying display (temporarily)");
        Display.destroy();
    }

    /**
     * Called if canvas was removed and then restored unexpectedly
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
            if (mouseWasGrabbed){
                Mouse.create();
                Mouse.setGrabbed(true);
                mouseWasGrabbed = false;
            }

            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    canvas.requestFocus();
                }
            });
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "restoreCanvas()", ex);
        }

        listener.gainFocus();
    }

    @Override
    protected void createContext(AppSettings settings) {
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
        }catch (LWJGLException ex){
            listener.handleError("Failed to parent canvas to display", ex);
        }
    }

}
