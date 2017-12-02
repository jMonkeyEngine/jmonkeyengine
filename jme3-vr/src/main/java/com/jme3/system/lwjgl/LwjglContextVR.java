package com.jme3.system.lwjgl;

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
import com.jme3.input.lwjgl.GlfwJoystickInput;
import com.jme3.input.lwjgl.GlfwKeyInputVR;
import com.jme3.input.lwjgl.GlfwMouseInputVR;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.lwjgl.LwjglGL;
import com.jme3.renderer.lwjgl.LwjglGLExt;
import com.jme3.renderer.lwjgl.LwjglGLFboEXT;
import com.jme3.renderer.lwjgl.LwjglGLFboGL3;
import com.jme3.renderer.opengl.*;
import com.jme3.system.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.GLCapabilities;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.glGetInteger;

/**
 * A VR oriented LWJGL implementation of a graphics context.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public abstract class LwjglContextVR implements JmeContext {

    private static final Logger logger = Logger.getLogger(LwjglContextVR.class.getName());

    protected static final String THREAD_NAME = "jME3 Main";

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean renderable = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Renderer renderer;
    protected GlfwKeyInputVR keyInput;
    protected GlfwMouseInputVR mouseInput;
    protected GlfwJoystickInput joyInput;
    protected Timer timer;
    protected SystemListener listener;

    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
    }

    protected void printContextInitInfo() {
        logger.log(Level.INFO, "LWJGL {0} context running on thread {1}\n"
                + " * Graphics Adapter: GLFW {2}",
                new Object[]{Integer.toString(org.lwjgl.Version.VERSION_MAJOR), Thread.currentThread().getName(), GLFW.glfwGetVersionString()});
    }

    protected int determineMaxSamples() {
        // If we already have a valid context, determine samples using current context.
    	logger.log(Level.SEVERE, "glfwExtensionSupported(\"GL_ARB_framebuffer_object\"): "+GLFW.glfwExtensionSupported("GL_ARB_framebuffer_object"));
    	logger.log(Level.SEVERE, "glfwExtensionSupported(\"GL_EXT_framebuffer_multisample\"): "+GLFW.glfwExtensionSupported("GL_ARB_framebuffer_object"));
    	
        if (GLFW.glfwExtensionSupported("GL_ARB_framebuffer_object")) {
            return glGetInteger(ARBFramebufferObject.GL_MAX_SAMPLES);
        } else if (GLFW.glfwExtensionSupported("GL_EXT_framebuffer_multisample")) {
            return glGetInteger(EXTFramebufferMultisample.GL_MAX_SAMPLES_EXT);
        }

        return Integer.MAX_VALUE;
    }

    protected void loadNatives() {
        if (JmeSystem.isLowPermissions()) {
            return;
        }

        if ("LWJGL".equals(settings.getAudioRenderer())) {
            NativeLibraryLoader.loadNativeLibrary("openal-lwjgl3", true);
        }

        if (NativeLibraryLoader.isUsingNativeBullet()) {
            NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
        }

        NativeLibraryLoader.loadNativeLibrary("glfw-lwjgl3", true);
        NativeLibraryLoader.loadNativeLibrary("jemalloc-lwjgl3", true);
        NativeLibraryLoader.loadNativeLibrary("lwjgl3", true);
    }

    /**
     * Check if the display is a retina display.
     * @return <code>true</code> if the display is a retina display and <code>false</code> otherwise.
     */
    public boolean isRetinaDisplay() {
        return GLFW.glfwGetVersionString().contains("retina");
    }

    protected int getNumSamplesToUse() {
        int samples = 0;
        if (settings.getSamples() > 1) {
            samples = settings.getSamples();
            final int supportedSamples = determineMaxSamples();
            if (supportedSamples < samples) {
                logger.log(Level.WARNING,
                        "Couldn't satisfy antialiasing samples requirement: x{0}. "
                                + "Video hardware only supports: x{1}",
                        new Object[]{samples, supportedSamples});

                samples = supportedSamples;
            }
        }
        return samples;
    }

    protected void initContextFirstTime() {
        final GLCapabilities capabilities = createCapabilities(settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3));

        if (!capabilities.OpenGL20) {
            throw new RendererException("OpenGL 2.0 or higher is required for jMonkeyEngine");
        }

        if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL2)
                || settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)) {
            GL gl = new LwjglGL();
            GLExt glext = new LwjglGLExt();
            GLFbo glfbo;

            if (capabilities.OpenGL30) {
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

        if (capabilities.GL_ARB_debug_output && settings.getBoolean("GraphicsDebug")) {
            ARBDebugOutput.glDebugMessageCallbackARB(new LwjglGLDebugOutputHandler(), 0);
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
        renderable.set(true);
    }

    /**
     * Context internal destroy.
     */
    public void internalDestroy() {
        renderer = null;
        timer = null;
        renderable.set(false);
        synchronized (createdLock) {
            created.set(false);
            createdLock.notifyAll();
        }
    }

    /**
     * Context internal create.
     */
    public void internalCreate() {
        synchronized (createdLock) {
            created.set(true);
            createdLock.notifyAll();
        }

        initContextFirstTime();
    }

    /**
     * Create the context.
     */
    public void create() {
        create(false);
    }

    /**
     * Destroy the context.
     */
    public void destroy() {
        destroy(false);
    }

    /**
     * 
     * @param createdVal
     */
    protected void waitFor(boolean createdVal) {
        synchronized (createdLock) {
            while (created.get() != createdVal) {
                try {
                    createdLock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public boolean isCreated() {
        return created.get();
    }

    @Override
    public boolean isRenderable() {
        return renderable.get();
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

}
