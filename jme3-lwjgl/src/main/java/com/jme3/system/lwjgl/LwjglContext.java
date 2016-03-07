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
package com.jme3.system.lwjgl;

import com.jme3.input.lwjgl.JInputJoyInput;
import com.jme3.input.lwjgl.LwjglKeyInput;
import com.jme3.input.lwjgl.LwjglMouseInput;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.lwjgl.LwjglGL;
import com.jme3.renderer.lwjgl.LwjglGLExt;
import com.jme3.renderer.lwjgl.LwjglGLFboEXT;
import com.jme3.renderer.lwjgl.LwjglGLFboGL3;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.renderer.opengl.GLDebugDesktop;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.opengl.GLTiming;
import com.jme3.renderer.opengl.GLTimingState;
import com.jme3.renderer.opengl.GLTracer;
import com.jme3.system.*;
import java.io.File;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.*;

/**
 * A LWJGL implementation of a graphics context.
 */
public abstract class LwjglContext implements JmeContext {

    private static final Logger logger = Logger.getLogger(LwjglContext.class.getName());

    protected static final String THREAD_NAME = "jME3 Main";

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean renderable = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Renderer renderer;
    protected LwjglKeyInput keyInput;
    protected LwjglMouseInput mouseInput;
    protected JInputJoyInput joyInput;
    protected Timer timer;
    protected SystemListener listener;

    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
    }

    protected void printContextInitInfo() {
        logger.log(Level.INFO, "LWJGL {0} context running on thread {1}\n"
                + " * Graphics Adapter: {2}\n"
                + " * Driver Version: {3}\n"
                + " * Scaling Factor: {4}",
                new Object[]{Sys.getVersion(), Thread.currentThread().getName(),
                    Display.getAdapter(), Display.getVersion(),
                    Display.getPixelScaleFactor()});
    }

    protected ContextAttribs createContextAttribs() {
        if (settings.getBoolean("GraphicsDebug") || settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)) {
            ContextAttribs attr;
            if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)) {
                attr = new ContextAttribs(3, 2);
                attr = attr.withProfileCore(true).withForwardCompatible(true).withProfileCompatibility(false);
            } else {
                attr = new ContextAttribs();
            }
            if (settings.getBoolean("GraphicsDebug")) {
                attr = attr.withDebug(true);
            }
            return attr;
        } else {
            return null;
        }
    }

    protected int determineMaxSamples(int requestedSamples) {
        try {
            // If we already have a valid context, determine samples using current
            // context.
            if (Display.isCreated() && Display.isCurrent()) {
                if (GLContext.getCapabilities().GL_ARB_framebuffer_object) {
                    return GL11.glGetInteger(ARBFramebufferObject.GL_MAX_SAMPLES);
                } else if (GLContext.getCapabilities().GL_EXT_framebuffer_multisample) {
                    return GL11.glGetInteger(EXTFramebufferMultisample.GL_MAX_SAMPLES_EXT);
                } else {
                    // Unknown.
                    return Integer.MAX_VALUE;
                }
            }
        } catch (LWJGLException ex) {
            listener.handleError("Failed to check if display is current", ex);
        }

        if ((Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) == 0) {
            // No pbuffer, assume everything is supported.
            return Integer.MAX_VALUE;
        } else {
            Pbuffer pb = null;

            // OpenGL2 method: Create pbuffer and query samples
            // from GL_ARB_framebuffer_object or GL_EXT_framebuffer_multisample.
            try {
                pb = new Pbuffer(1, 1, new PixelFormat(0, 0, 0), null);
                pb.makeCurrent();

                if (GLContext.getCapabilities().GL_ARB_framebuffer_object) {
                    return GL11.glGetInteger(ARBFramebufferObject.GL_MAX_SAMPLES);
                } else if (GLContext.getCapabilities().GL_EXT_framebuffer_multisample) {
                    return GL11.glGetInteger(EXTFramebufferMultisample.GL_MAX_SAMPLES_EXT);
                }

                // OpenGL2 method failed.
                return Integer.MAX_VALUE;
            } catch (LWJGLException ex) {
                // Something else failed.
                return Integer.MAX_VALUE;
            } finally {
                if (pb != null) {
                    pb.destroy();
                }
            }
        }
    }

    protected void loadNatives() {
        if (JmeSystem.isLowPermissions()) {
            return;
        }
        if ("LWJGL".equals(settings.getAudioRenderer())) {
            NativeLibraryLoader.loadNativeLibrary("openal", true);
        }
        if (settings.useJoysticks()) {
            NativeLibraryLoader.loadNativeLibrary("jinput", true);
            NativeLibraryLoader.loadNativeLibrary("jinput-dx8", true);
        }
        if (NativeLibraryLoader.isUsingNativeBullet()) {
            NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
        }
        NativeLibraryLoader.loadNativeLibrary("lwjgl", true);
    }

    protected int getNumSamplesToUse() {
        int samples = 0;
        if (settings.getSamples() > 1) {
            samples = settings.getSamples();
            int supportedSamples = determineMaxSamples(samples);
            if (supportedSamples < samples) {
                logger.log(Level.WARNING,
                        "Couldn''t satisfy antialiasing samples requirement: x{0}. "
                        + "Video hardware only supports: x{1}",
                        new Object[]{samples, supportedSamples});

                samples = supportedSamples;
            }
        }
        return samples;
    }

    protected void initContextFirstTime() {
        if (!GLContext.getCapabilities().OpenGL20) {
            throw new RendererException("OpenGL 2.0 or higher is "
                    + "required for jMonkeyEngine");
        }

        if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL2)
                || settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)) {
            GL gl = new LwjglGL();
            GLExt glext = new LwjglGLExt();
            GLFbo glfbo;

            if (GLContext.getCapabilities().OpenGL30) {
                glfbo = new LwjglGLFboGL3();
            } else {
                glfbo = new LwjglGLFboEXT();
            }

            if (settings.getBoolean("GraphicsDebug")) {
                gl = new GLDebugDesktop(gl, glext, glfbo);
                glext = (GLExt) gl;
                glfbo = (GLFbo) gl;
            }

            if (settings.getBoolean("GraphicsTiming")) {
                GLTimingState timingState = new GLTimingState();
                gl = (GL) GLTiming.createGLTiming(gl, timingState, GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLTiming.createGLTiming(glext, timingState, GLExt.class);
                glfbo = (GLFbo) GLTiming.createGLTiming(glfbo, timingState, GLFbo.class);
            }

            if (settings.getBoolean("GraphicsTrace")) {
                gl = (GL) GLTracer.createDesktopGlTracer(gl, GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLTracer.createDesktopGlTracer(glext, GLExt.class);
                glfbo = (GLFbo) GLTracer.createDesktopGlTracer(glfbo, GLFbo.class);
            }

            renderer = new GLRenderer(gl, glext, glfbo);
            renderer.initialize();
        } else {
            throw new UnsupportedOperationException("Unsupported renderer: " + settings.getRenderer());
        }

        if (GLContext.getCapabilities().GL_ARB_debug_output && settings.getBoolean("GraphicsDebug")) {
            ARBDebugOutput.glDebugMessageCallbackARB(new ARBDebugOutputCallback(new LwjglGLDebugOutputHandler()));
        }

        renderer.setMainFrameBufferSrgb(settings.isGammaCorrection());
        renderer.setLinearizeSrgbImages(settings.isGammaCorrection());

        // Init input
        if (keyInput != null) {
            keyInput.initialize();
        }

        if (mouseInput != null) {
            mouseInput.initialize();
        }

        if (joyInput != null) {
            joyInput.initialize();
        }
    }

    public void internalDestroy() {
        renderer = null;
        timer = null;
        renderable.set(false);
        synchronized (createdLock) {
            created.set(false);
            createdLock.notifyAll();
        }
    }

    public void internalCreate() {
        timer = new LwjglTimer();

        synchronized (createdLock) {
            created.set(true);
            createdLock.notifyAll();
        }

        if (renderable.get()) {
            initContextFirstTime();
        } else {
            assert getType() == Type.Canvas;
        }
    }

    public void create() {
        create(false);
    }

    public void destroy() {
        destroy(false);
    }

    protected void waitFor(boolean createdVal) {
        synchronized (createdLock) {
            while (created.get() != createdVal) {
                try {
                    createdLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public boolean isCreated() {
        return created.get();
    }

    public boolean isRenderable() {
        return renderable.get();
    }

    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }

    public AppSettings getSettings() {
        return settings;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Timer getTimer() {
        return timer;
    }

}
