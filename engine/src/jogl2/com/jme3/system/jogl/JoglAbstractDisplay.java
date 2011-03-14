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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.renderer.jogl.JoglRenderer;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.FPSAnimator;

public abstract class JoglAbstractDisplay extends JoglContext implements GLEventListener {

    private static final Logger logger = Logger.getLogger(JoglAbstractDisplay.class.getName());

    protected GraphicsDevice device;

    protected GLCanvas canvas;

    protected AnimatorBase animator;

    protected AtomicBoolean active = new AtomicBoolean(false);

    protected boolean wasActive = false;

    protected int frameRate;

    protected boolean useAwt = true;

    protected AtomicBoolean autoFlush = new AtomicBoolean(true);

    protected boolean wasAnimating = false;

    static {
        // FIXME: should be called as early as possible before any GUI task
        GLProfile.initSingleton();
    }

    protected void initGLCanvas() {
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setHardwareAccelerated(true);
        caps.setDoubleBuffered(true);
        caps.setStencilBits(settings.getStencilBits());
        caps.setDepthBits(settings.getDepthBits());

        if (settings.getSamples() > 1) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(settings.getSamples());
        }

        canvas = new GLCanvas(caps) {
            @Override
            public void addNotify() {
                super.addNotify();
                onCanvasAdded();
            }

            @Override
            public void removeNotify() {
                onCanvasRemoved();
                super.removeNotify();
            }
        };
        // TODO: add a check on the settings
        // set the size of the canvas as early as possible to avoid further useless reshape attempts
        canvas.setSize(settings.getWidth(), settings.getHeight());
        if (settings.isVSync()) {
            GLContext.getCurrentGL().setSwapInterval(1);
        }
        canvas.setFocusable(true);
        canvas.setIgnoreRepaint(true);
        canvas.addGLEventListener(this);

        // N.B: it is too early to get the GL instance from the canvas
        // if (false){
        // trace mode
        // jME already uses err stream, use out instead
        // gl = new TraceGL(gl, System.out);
        // }else if (false){
        // debug mode
        // gl = new DebugGL(gl);
        // }else{
        // production mode
        // }
        renderer = new JoglRenderer();
    }

    protected void startGLCanvas() {
        if (frameRate > 0) {
            animator = new FPSAnimator(canvas, frameRate);
            // ((FPSAnimator)animator).setRunAsFastAsPossible(true);
        }
        else {
            animator = new Animator(canvas);
            ((Animator) animator).setRunAsFastAsPossible(true);
        }

        animator.start();
        wasAnimating = true;
    }

    protected void onCanvasAdded() {
    }

    protected void onCanvasRemoved() {
    }

    @Override
    public KeyInput getKeyInput() {
        return new AwtKeyInput(canvas);
    }

    @Override
    public MouseInput getMouseInput() {
        return new AwtMouseInput(canvas);
    }

    public void setAutoFlushFrames(boolean enabled) {
        autoFlush.set(enabled);
    }

    /**
     * Callback.
     */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        listener.reshape(width, height);
    }

    /**
     * Callback.
     */
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    /**
     * Callback
     */
    public void dispose(GLAutoDrawable drawable) {

    }
}
