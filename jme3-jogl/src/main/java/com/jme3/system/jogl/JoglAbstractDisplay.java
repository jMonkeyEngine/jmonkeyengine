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

import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.renderer.jogl.JoglRenderer;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.FPSAnimator;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.DebugGL3bc;
import com.jogamp.opengl.DebugGL4;
import com.jogamp.opengl.DebugGL4bc;
import com.jogamp.opengl.DebugGLES1;
import com.jogamp.opengl.DebugGLES2;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.awt.GLCanvas;

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

    protected void initGLCanvas() {
        loadNatives();
        
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        
        //FIXME use the settings to know whether to use the max programmable profile
        //then call GLProfile.getMaxProgrammable(true);
        GLCapabilities caps = new GLCapabilities(GLProfile.getMaxFixedFunc(true));
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
        canvas.invoke(false, new GLRunnable() {
            public boolean run(GLAutoDrawable glad) {
                canvas.getGL().setSwapInterval(settings.isVSync() ? 1 : 0);
                return true;
            }
        });
        canvas.setFocusable(true);
        canvas.requestFocus();
        canvas.setSize(settings.getWidth(), settings.getHeight());
        canvas.setIgnoreRepaint(true);
        canvas.addGLEventListener(this);

        if (settings.getBoolean("GraphicsDebug")) {
            canvas.invoke(false, new GLRunnable() {
                public boolean run(GLAutoDrawable glad) {
                    GL gl = glad.getGL();
                    if (gl.isGLES()) {
                        if (gl.isGLES1()) {
                            glad.setGL(new DebugGLES1(gl.getGLES1()));
                        } else {
                            if (gl.isGLES2()) {
                                glad.setGL(new DebugGLES2(gl.getGLES2()));
                            } else {
                                // TODO ES3
                            }
                        }
                    } else {
                        if (gl.isGL4bc()) {
                            glad.setGL(new DebugGL4bc(gl.getGL4bc()));
                        } else {
                            if (gl.isGL4()) {
                                glad.setGL(new DebugGL4(gl.getGL4()));
                            } else {
                                if (gl.isGL3bc()) {
                                    glad.setGL(new DebugGL3bc(gl.getGL3bc()));
                                } else {
                                    if (gl.isGL3()) {
                                        glad.setGL(new DebugGL3(gl.getGL3()));
                                    } else {
                                        if (gl.isGL2()) {
                                            glad.setGL(new DebugGL2(gl.getGL2()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            });
        }
        
        renderer = new JoglRenderer();
        
        renderer.setMainFrameBufferSrgb(settings.getGammaCorrection());
    }

    protected void startGLCanvas() {
        if (frameRate > 0) {
            animator = new FPSAnimator(canvas, frameRate);
        }
        else {
            animator = new Animator();
            animator.add(canvas);
            ((Animator) animator).setRunAsFastAsPossible(true);
        }

        animator.start();
        wasAnimating = true;
        
        //FIXME not sure it is the best place to do that
        renderable.set(true);
    }

    protected void onCanvasAdded() {
    }

    protected void onCanvasRemoved() {
    }

    @Override
    public KeyInput getKeyInput() {
        if (keyInput == null) {
            keyInput = new AwtKeyInput();
            ((AwtKeyInput)keyInput).setInputSource(canvas);
        }
        return keyInput;
    }

    @Override
    public MouseInput getMouseInput() {
        if (mouseInput == null) {
            mouseInput = new AwtMouseInput();
            ((AwtMouseInput)mouseInput).setInputSource(canvas);
        }
        return mouseInput;
    }
    
    public TouchInput getTouchInput() {
        return null;
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
