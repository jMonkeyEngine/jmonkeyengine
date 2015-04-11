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
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jogamp.opengl.GLAutoDrawable;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class JoglDisplay extends JoglAbstractDisplay {

    private static final Logger logger = Logger.getLogger(JoglDisplay.class.getName());

    protected AtomicBoolean windowCloseRequest = new AtomicBoolean(false);
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    protected AtomicBoolean needRestart = new AtomicBoolean(false);
    protected volatile boolean wasInited = false;
    protected Frame frame;

    public Type getType() {
        return Type.Display;
    }

    protected void createGLFrame(){
        if (useAwt){
            frame = new Frame(settings.getTitle());
        }else{
            frame = new JFrame(settings.getTitle());
        }
        frame.setResizable(false);
        frame.add(canvas);
        
        applySettings(settings);
        
        // Make the window visible to realize the OpenGL surface.
        frame.setVisible(true);
        
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
        final boolean isDisplayModeModified;
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        // Get the current display mode
        final DisplayMode previousDisplayMode = gd.getDisplayMode();
        // Handle full screen mode if requested.
        if (settings.isFullscreen()) {
            frame.setUndecorated(true);
            // Check if the full-screen mode is supported by the OS
            boolean isFullScreenSupported = gd.isFullScreenSupported();
            if (isFullScreenSupported) {
                gd.setFullScreenWindow(frame);
                // Check if display mode changes are supported by the OS
                if (gd.isDisplayChangeSupported()) {
                    // Get all available display modes
                    final DisplayMode[] displayModes = gd.getDisplayModes();
                    DisplayMode multiBitsDepthSupportedDisplayMode = null;
                    DisplayMode refreshRateUnknownDisplayMode = null;
                    DisplayMode multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode = null;
                    DisplayMode matchingDisplayMode = null;
                    DisplayMode currentDisplayMode;
                    // Look for the display mode that matches with our parameters
                    // Look for some display modes that are close to these parameters
                    // and that could be used as substitutes
                    // On some machines, the refresh rate is unknown and/or multi bit
                    // depths are supported. If you try to force a particular refresh
                    // rate or a bit depth, you might find no available display mode
                    // that matches exactly with your parameters
                    for (int i = 0; i < displayModes.length && matchingDisplayMode == null; i++) {
                        currentDisplayMode = displayModes[i];
                        if (currentDisplayMode.getWidth() == settings.getWidth()
                                && currentDisplayMode.getHeight() == settings.getHeight()) {
                            if (currentDisplayMode.getBitDepth() == settings.getBitsPerPixel()) {
                                if (currentDisplayMode.getRefreshRate() == settings.getFrequency()) {
                                    matchingDisplayMode = currentDisplayMode;
                                } else if (currentDisplayMode.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) {
                                    refreshRateUnknownDisplayMode = currentDisplayMode;
                                }
                            } else if (currentDisplayMode.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI) {
                                if (currentDisplayMode.getRefreshRate() == settings.getFrequency()) {
                                    multiBitsDepthSupportedDisplayMode = currentDisplayMode;
                                } else if (currentDisplayMode.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) {
                                    multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode = currentDisplayMode;
                                }
                            }
                        }
                    }
                    DisplayMode nextDisplayMode = null;
                    if (matchingDisplayMode != null) {
                        nextDisplayMode = matchingDisplayMode;
                    } else if (multiBitsDepthSupportedDisplayMode != null) {
                        nextDisplayMode = multiBitsDepthSupportedDisplayMode;
                    } else if (refreshRateUnknownDisplayMode != null) {
                        nextDisplayMode = refreshRateUnknownDisplayMode;
                    } else if (multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode != null) {
                        nextDisplayMode = multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode;
                    } else {
                        isFullScreenSupported = false;
                    }
                    // If we have found a display mode that approximatively matches
                    // with the input parameters, use it
                    if (nextDisplayMode != null) {
                        gd.setDisplayMode(nextDisplayMode);
                        isDisplayModeModified = true;
                    } else {
                        isDisplayModeModified = false;
                    }
                } else {
                    isDisplayModeModified = false;
                    // Resize the canvas if the display mode cannot be changed
                    // and the screen size is not equal to the canvas size
                    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    if (screenSize.width != settings.getWidth() || screenSize.height != settings.getHeight()) {
                        canvas.setSize(screenSize);
                    }
                }
            } else {
                isDisplayModeModified = false;
            }

            // Software windowed full-screen mode
            if (!isFullScreenSupported) {
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                // Resize the canvas
                canvas.setSize(screenSize);
                // Resize the frame so that it occupies the whole screen
                frame.setSize(screenSize);
                // Set its location at the top left corner
                frame.setLocation(0, 0);
            }
        }
        // Otherwise, center the window on the screen.
        else {
            isDisplayModeModified = false;
            frame.pack();

            int x, y;
            x = (Toolkit.getDefaultToolkit().getScreenSize().width - settings.getWidth()) / 2;
            y = (Toolkit.getDefaultToolkit().getScreenSize().height - settings.getHeight()) / 2;
            frame.setLocation(x, y);
        }
        
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                // If required, restore the previous display mode
                if (isDisplayModeModified) {
                    gd.setDisplayMode(previousDisplayMode);
                }
                // If required, get back to the windowed mode
                if (gd.getFullScreenWindow() == frame) {
                    gd.setFullScreenWindow(null);
                }
                windowCloseRequest.set(true);
            }
            @Override
            public void windowActivated(WindowEvent evt) {
                active.set(true);
            }

            @Override
            public void windowDeactivated(WindowEvent evt) {
                active.set(false);
            }
        });

        logger.log(Level.FINE, "Selected display mode: {0}x{1}x{2} @{3}",
                new Object[]{gd.getDisplayMode().getWidth(),
                             gd.getDisplayMode().getHeight(),
                             gd.getDisplayMode().getBitDepth(),
                             gd.getDisplayMode().getRefreshRate()});
    }

    private void initInEDT(){
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
        if (SwingUtilities.isEventDispatchThread()) {
            initInEDT();
        } else {
            try {
                if (waitFor) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                initInEDT();
                            }
                        });
                    } catch (InterruptedException ex) {
                        listener.handleError("Interrupted", ex);
                    }
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            initInEDT();
                        }
                    });
                }
            } catch (InvocationTargetException ex) {
                throw new AssertionError(); // can never happen
            }
        }
    }

    public void destroy(boolean waitFor){
        needClose.set(true);
        if (waitFor){
            waitFor(false);
        }
    }

    public void restart() {
        if (created.get()){
            needRestart.set(true);
        }else{
            throw new IllegalStateException("Display not started yet. Cannot restart");
        }
    }

    public void setTitle(String title){
        if (frame != null)
            frame.setTitle(title);
    }

    /**
     * Callback.
     */
    public void display(GLAutoDrawable drawable) {
        if (needClose.get()) {
            listener.destroy();
            animator.stop();
            if (settings.isFullscreen()) {
                device.setFullScreenWindow(null);
            }
            frame.dispose();
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
            if (frame.isVisible()){
                animator.stop();
                frame.dispose();
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
