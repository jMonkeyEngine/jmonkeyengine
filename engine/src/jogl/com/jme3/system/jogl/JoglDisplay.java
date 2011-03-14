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

package com.jme3.system.jogl;

import com.jme3.system.AppSettings;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class JoglDisplay extends JoglAbstractDisplay {

    private static final Logger logger = Logger.getLogger(JoglDisplay.class.getName());

    protected AtomicBoolean windowCloseRequest = new AtomicBoolean(false);
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    protected AtomicBoolean needRestart = new AtomicBoolean(false);
    protected boolean wasInited = false;
    protected Frame frame;

    public Type getType() {
        return Type.Display;
    }

    protected DisplayMode getFullscreenDisplayMode(DisplayMode[] modes, int width, int height, int bpp, int freq){
        for (DisplayMode mode : modes){
            if (mode.getWidth() == width
             && mode.getHeight() == height
             && (mode.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI 
                || mode.getBitDepth() == bpp
                || (mode.getBitDepth() == 32 && bpp==24))
             && mode.getRefreshRate() == freq){
                return mode;
            }
        }
        return null;
    }

    protected void createGLFrame(){
        Container contentPane;
        if (useAwt){
            frame = new Frame(settings.getTitle());
            contentPane = frame;
        }else{
            frame = new JFrame(settings.getTitle());
            contentPane = ((JFrame)frame).getContentPane();
        }

        contentPane.setLayout(new BorderLayout());

        applySettings(settings);

        frame.setResizable(false);
        frame.setFocusable(true);

        if (settings.getIcons() != null) {
            try {
                Method setIconImages = frame.getClass().getMethod("setIconImages", List.class);
                setIconImages.invoke(frame, Arrays.asList(settings.getIcons()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // only add canvas after frame is visible
        contentPane.add(canvas, BorderLayout.CENTER);
        frame.pack();
//        frame.setSize(contentPane.getPreferredSize());

        if (device.getFullScreenWindow() == null){
            // now that canvas is attached,
            // determine optimal size to contain it
           
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation((screenSize.width - frame.getWidth()) / 2,
                              (screenSize.height - frame.getHeight()) / 2);
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
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
    }

    protected void applySettings(AppSettings settings){
        DisplayMode displayMode;
        if (settings.getWidth() <= 0 || settings.getHeight() <= 0){
            displayMode = device.getDisplayMode();
            settings.setResolution(displayMode.getWidth(), displayMode.getHeight());
        }else if (settings.isFullscreen()){
            displayMode = getFullscreenDisplayMode(device.getDisplayModes(),
                                                   settings.getWidth(), settings.getHeight(),
                                                   settings.getBitsPerPixel(), settings.getFrequency());
            if (displayMode == null)
                throw new RuntimeException("Unable to find fullscreen display mode matching settings");
        }else{
            displayMode = new DisplayMode(settings.getWidth(), settings.getHeight(), 0, 0);
        }

        // FIXME: seems to return false even though
        // it is supported..
//        if (!device.isDisplayChangeSupported()){
//            // must use current device mode if display mode change not supported
//            displayMode = device.getDisplayMode();
//            settings.setResolution(displayMode.getWidth(), displayMode.getHeight());
//        }

        frameRate = settings.getFrameRate();
        logger.log(Level.INFO, "Selected display mode: {0}x{1}x{2} @{3}",
                new Object[]{displayMode.getWidth(),
                             displayMode.getHeight(),
                             displayMode.getBitDepth(),
                             displayMode.getRefreshRate()});
        
        canvas.setSize(displayMode.getWidth(), displayMode.getHeight());

        DisplayMode prevDisplayMode = device.getDisplayMode();

        if (settings.isFullscreen() && device.isFullScreenSupported()){
            frame.setUndecorated(true);

            try{
                device.setFullScreenWindow(frame);
                if (!prevDisplayMode.equals(displayMode)
                  && device.isDisplayChangeSupported()){
                    device.setDisplayMode(displayMode);
                }
            } catch (Throwable t){
                logger.log(Level.SEVERE, "Failed to enter fullscreen mode", t);
                device.setFullScreenWindow(null);
            }
        }else{
            if (!device.isFullScreenSupported()){
                logger.warning("Fullscreen not supported.");
            }else{
                frame.setUndecorated(false);
                device.setFullScreenWindow(null);
            }

            frame.setVisible(true);
        }
    }

    private void initInEDT(){
        initGLCanvas();

        createGLFrame();

        startGLCanvas();
    }

    public void init(GLAutoDrawable drawable){
        // prevent initializing twice on restart
        if (!wasInited){
            canvas.requestFocus();

            super.internalCreate();
            logger.info("Display created.");

            renderer.initialize();
            listener.initialize();

            wasInited = true;
        }
    }

    public void create(boolean waitFor){
        try {
            if (waitFor){
                try{
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            initInEDT();
                        }
                    });
                } catch (InterruptedException ex) {
                    listener.handleError("Interrupted", ex);
                }
            }else{
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
            logger.info("Display destroyed.");
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
        renderer.onFrame();
    }
}
