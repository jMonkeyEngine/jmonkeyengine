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

import com.jme3.system.AppSettings;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.util.MonitorModeUtil;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.opengl.GLAutoDrawable;

public class JoglNewtDisplay extends JoglNewtAbstractDisplay {
    
    private static final Logger logger = Logger.getLogger(JoglNewtDisplay.class.getName());

    protected AtomicBoolean windowCloseRequest = new AtomicBoolean(false);
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    protected AtomicBoolean needRestart = new AtomicBoolean(false);
    protected volatile boolean wasInited = false;

    public Type getType() {
        return Type.Display;
    }

    protected void createGLFrame(){
        canvas.setTitle(settings.getTitle());
        
        applySettings(settings);
        
        // Make the window visible to realize the OpenGL surface.
        canvas.setVisible(true);
        
        //this is the earliest safe opportunity to get the context
        //final GLContext context = canvas.getContext();
        
        /*canvas.invoke(true, new GLRunnable() {
            @Override
            public boolean run(GLAutoDrawable glAutoDrawable) {     
                context.makeCurrent();
                try {
                    startGLCanvas();
                }
                finally {
                    context.release();
                }
                return true;
            }
        });*/
    }

    protected void applySettings(AppSettings settings){
        active.set(true);
        canvas.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                windowCloseRequest.set(true);
            }
            
            @Override
            public void windowGainedFocus(WindowEvent e) {
                active.set(true);
            }
            
            @Override
            public void windowLostFocus(WindowEvent e) {
                active.set(false);
            }
        });
        canvas.setSize(settings.getWidth(), settings.getHeight());
        canvas.setUndecorated(settings.isFullscreen());
        canvas.setFullscreen(settings.isFullscreen());
        
        /**
         * uses the filtering relying on resolution with the size to fetch only
         * the screen mode matching with the current resolution
         */
        Screen screen = canvas.getScreen();
        /**
         * The creation of native resources is lazy in JogAmp, i.e they are 
         * created only when they are used for the first time. When the GLWindow
         * is not yet visible, its screen might have been unused for now and 
         * then its native counterpart has not yet been created. That's why 
         * forcing the creation of this resource is necessary
         */
        screen.addReference();
        if (settings.isFullscreen()) {
            List<MonitorMode> screenModes = canvas.getMainMonitor().getSupportedModes();
            //the resolution is provided by the user
            Dimension dimension = new Dimension(settings.getWidth(), settings.getHeight());
            screenModes = MonitorModeUtil.filterByResolution(screenModes, dimension);
            screenModes = MonitorModeUtil.getHighestAvailableBpp(screenModes);
            if (settings.getFrequency() > 0) {
                screenModes = MonitorModeUtil.filterByRate(screenModes, settings.getFrequency());
            } else {
                screenModes = MonitorModeUtil.getHighestAvailableRate(screenModes);
            }
            canvas.getMainMonitor().setCurrentMode(screenModes.get(0));
        }
        
        MonitorMode currentScreenMode = canvas.getMainMonitor().getCurrentMode();
        logger.log(Level.FINE, "Selected display mode: {0}x{1}x{2} @{3}",
                new Object[]{currentScreenMode.getRotatedWidth(),
                             currentScreenMode.getRotatedHeight(),
                             currentScreenMode.getSurfaceSize().getBitsPerPixel(),
                             currentScreenMode.getRefreshRate()});
    }

    private void privateInit(){
        initGLCanvas();

        createGLFrame();

        startGLCanvas();
    }

    public void init(GLAutoDrawable drawable){
        // prevent initializing twice on restart
        if (!wasInited){
            wasInited = true;
            
            canvas.requestFocus();

            super.internalCreate();
            logger.fine("Display created.");

            renderer.initialize();
            listener.initialize();
        }
    }

    public void create(boolean waitFor){
        privateInit();
    }

    public void destroy(boolean waitFor){
        needClose.set(true);
        if (waitFor){
            waitFor(false);
        }
        if (animator.isAnimating())
            animator.stop();
    }

    public void restart() {
        if (created.get()){
            needRestart.set(true);
        }else{
            throw new IllegalStateException("Display not started yet. Cannot restart");
        }
    }

    public void setTitle(String title){
        if (canvas != null) {
            canvas.setTitle(title);
        }
    }

    /**
     * Callback.
     */
    public void display(GLAutoDrawable drawable) {
        if (needClose.get()) {
            listener.destroy();
            animator.stop();
            if (settings.isFullscreen()) {
                canvas.setFullscreen(false);
            }
            canvas.destroy();
            logger.fine("Display destroyed.");
            super.internalDestroy();
            return;
        }

        if (windowCloseRequest.get()){
            listener.requestClose(false);
            windowCloseRequest.set(false);
        }

        if (needRestart.getAndSet(false)){
            // for restarting contexts
            if (canvas.isVisible()){
                animator.stop();
                canvas.destroy();
                createGLFrame();
                startGLCanvas();
            }
        }

//        boolean flush = autoFlush.get();
//        if (animator.isAnimating() != flush){
//            if (flush)
//                animator.stop();
//            else
//                animator.start();
//        }

        if (wasActive != active.get()){
            if (!wasActive){
                listener.gainFocus();
                wasActive = true;
            }else{
                listener.loseFocus();
                wasActive = false;
            }
        }

        listener.update();
        renderer.postFrame();
    }
}

