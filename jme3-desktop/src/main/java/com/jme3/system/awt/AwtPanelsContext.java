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
package com.jme3.system.awt;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.renderer.Renderer;
import com.jme3.system.*;
import java.util.ArrayList;

public class AwtPanelsContext implements JmeContext {

    protected JmeContext actualContext;
    protected AppSettings settings = new AppSettings(true);
    protected SystemListener listener;
    protected ArrayList<AwtPanel> panels = new ArrayList<AwtPanel>();
    protected AwtPanel inputSource;

    protected AwtMouseInput mouseInput = new AwtMouseInput();
    protected AwtKeyInput keyInput = new AwtKeyInput();

    protected boolean lastThrottleState = false;

    private class AwtPanelsListener implements SystemListener {

        public void initialize() {
            initInThread();
        }

        public void reshape(int width, int height) {
            throw new IllegalStateException();
        }

        public void update() {
            updateInThread();
        }

        public void requestClose(boolean esc) {
            // shouldn't happen
            throw new IllegalStateException();
        }

        public void gainFocus() {
            // shouldn't happen
            throw new IllegalStateException();
        }

        public void loseFocus() {
            // shouldn't happen
            throw new IllegalStateException();
        }

        public void handleError(String errorMsg, Throwable t) {
            listener.handleError(errorMsg, t);
        }

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

    public Type getType() {
        return Type.OffscreenSurface;
    }

    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
    }

    public AppSettings getSettings() {
        return settings;
    }

    public Renderer getRenderer() {
        return actualContext.getRenderer();
    }

    public MouseInput getMouseInput() {
        return mouseInput;
    }

    public KeyInput getKeyInput() {
        return keyInput;
    }

    public JoyInput getJoyInput() {
        return null;
    }

    public TouchInput getTouchInput() {
        return null;
    }

    public Timer getTimer() {
        return actualContext.getTimer();
    }

    public boolean isCreated() {
        return actualContext != null && actualContext.isCreated();
    }

    public boolean isRenderable() {
        return actualContext != null && actualContext.isRenderable();
    }

    public AwtPanelsContext(){
    }

    public AwtPanel createPanel(PaintMode paintMode){
        AwtPanel panel = new AwtPanel(paintMode);
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

        if (needThrottle){
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        listener.update();
    }

    private void destroyInThread(){
        listener.destroy();
    }

    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
        this.settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        if (actualContext != null){
            actualContext.setSettings(settings);
        }
    }

    public void create(boolean waitFor) {
        if (actualContext != null){
            throw new IllegalStateException("Already created");
        }

        actualContext = JmeSystem.newContext(settings, Type.OffscreenSurface);
        actualContext.setSystemListener(new AwtPanelsListener());
        actualContext.create(waitFor);
    }

    public void destroy(boolean waitFor) {
        if (actualContext == null)
            throw new IllegalStateException("Not created");

        // destroy parent context
        actualContext.destroy(waitFor);
    }

    public void setTitle(String title) {
        // not relevant, ignore
    }

    public void setAutoFlushFrames(boolean enabled) {
        // not relevant, ignore
    }

    public void restart() {
        // only relevant if changing pixel format.
    }

}
