/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.system.awt;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import com.jme3.system.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class AwtPanelsContext implements JmeContext {

    private static final Logger logger = Logger.getLogger(AwtPanelsContext.class.getName());

    protected JmeContext actualContext;
    protected AppSettings settings = new AppSettings(true);
    protected SystemListener listener;
    protected ArrayList<AwtPanel> panels = new ArrayList<>();
    protected AwtPanel inputSource;

    protected AwtMouseInput mouseInput = new AwtMouseInput();
    protected AwtKeyInput keyInput = new AwtKeyInput();

    protected boolean lastThrottleState = false;

    private class AwtPanelsListener implements SystemListener {

        @Override
        public void initialize() {
            initInThread();
        }

        @Override
        public void reshape(int width, int height) {
            logger.severe("reshape is not supported.");
        }

        @Override
        public void rescale(float x, float y) {
            logger.severe("rescale is not supported.");
        }

        @Override
        public void update() {
            updateInThread();
        }

        @Override
        public void requestClose(boolean esc) {
            // shouldn't happen
            throw new IllegalStateException();
        }

        @Override
        public void gainFocus() {
            // shouldn't happen
            throw new IllegalStateException();
        }

        @Override
        public void loseFocus() {
            // shouldn't happen
            throw new IllegalStateException();
        }

        @Override
        public void handleError(String errorMsg, Throwable t) {
            listener.handleError(errorMsg, t);
        }

        @Override
        public void destroy() {
            destroyInThread();
        }
    }

    public void setInputSource(AwtPanel panel){
        if (!panels.contains(panel))
            throw new IllegalArgumentException();

        inputSource = panel;
        mouseInput.setInputSource(panel);
        keyInput.setInputSource(panel);
    }

    @Override
    public Type getType() {
        return Type.OffscreenSurface;
    }

    /**
     * Accesses the listener that receives events related to this context.
     *
     * @return the pre-existing instance
     */
    @Override
    public SystemListener getSystemListener() {
        return listener;
    }

    @Override
    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public Renderer getRenderer() {
        return actualContext.getRenderer();
    }

    @Override
    public MouseInput getMouseInput() {
        return mouseInput;
    }

    @Override
    public KeyInput getKeyInput() {
        return keyInput;
    }

    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public Timer getTimer() {
        return actualContext.getTimer();
    }

    @Override
    public boolean isCreated() {
        return actualContext != null && actualContext.isCreated();
    }

    @Override
    public boolean isRenderable() {
        return actualContext != null && actualContext.isRenderable();
    }

    @Override
    public Context getOpenCLContext() {
        return actualContext.getOpenCLContext();
    }
    
    public AwtPanelsContext(){
    }

    public AwtPanel createPanel(PaintMode paintMode){
        AwtPanel panel = new AwtPanel(paintMode);
        panels.add(panel);
        return panel;
    }
    
    public AwtPanel createPanel(PaintMode paintMode, boolean srgb){
        AwtPanel panel = new AwtPanel(paintMode, srgb);
        panels.add(panel);
        return panel;
    }

    private void initInThread(){
        listener.initialize();
    }

    private void updateInThread(){
        // Check if throttle required
        boolean needThrottle = true;

        for (AwtPanel panel : panels){
            if (panel.isActiveDrawing()){
                needThrottle = false;
                break;
            }
        }

        if (lastThrottleState != needThrottle){
            lastThrottleState = needThrottle;
            if (lastThrottleState){
                System.out.println("OGL: Throttling update loop.");
            }else{
                System.out.println("OGL: Ceased throttling update loop.");
            }
        }

        if (needThrottle) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        listener.update();
        
        for (AwtPanel panel : panels){
            panel.onFrameEnd();
        }
    }

    private void destroyInThread(){
        listener.destroy();
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
        this.settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        if (actualContext != null){
            actualContext.setSettings(settings);
        }
    }

    @Override
    public void create(boolean waitFor) {
        if (actualContext != null){
            throw new IllegalStateException("Already created");
        }

        actualContext = JmeSystem.newContext(settings, Type.OffscreenSurface);
        actualContext.setSystemListener(new AwtPanelsListener());
        actualContext.create(waitFor);
    }

    @Override
    public void destroy(boolean waitFor) {
        if (actualContext == null)
            throw new IllegalStateException("Not created");

        // destroy parent context
        actualContext.destroy(waitFor);
    }

    @Override
    public void setTitle(String title) {
        // not relevant, ignore
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        // not relevant, ignore
    }

    @Override
    public void restart() {
        // only relevant if changing pixel format.
    }

    /**
     * Returns the height of the input panel.
     *
     * @return the height (in pixels)
     */
    @Override
    public int getFramebufferHeight() {
        return inputSource.getHeight();
    }

    /**
     * Returns the width of the input panel.
     *
     * @return the width (in pixels)
     */
    @Override
    public int getFramebufferWidth() {
        return inputSource.getWidth();
    }

    /**
     * Returns the screen X coordinate of the left edge of the input panel.
     *
     * @return the screen X coordinate
     */
    @Override
    public int getWindowXPosition() {
        return inputSource.getX();
    }

    /**
     * Returns the screen Y coordinate of the top edge of the input panel.
     *
     * @return the screen Y coordinate
     */
    @Override
    public int getWindowYPosition() {
        return inputSource.getY();
    }
}
