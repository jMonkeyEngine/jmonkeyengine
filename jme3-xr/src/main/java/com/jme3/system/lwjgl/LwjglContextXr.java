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

package com.jme3.system.lwjgl;

import com.jme3.input.lwjgl.GlfwJoystickInput;
import com.jme3.lwjgl3.utils.APIUtil;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.lwjgl.LwjglGL;
import com.jme3.renderer.lwjgl.LwjglGLExt;
import com.jme3.renderer.lwjgl.LwjglGLFboEXT;
import com.jme3.renderer.lwjgl.LwjglGLFboGL3;
import com.jme3.renderer.opengl.*;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3.util.BufferAllocatorFactory;
import com.jme3.util.LWJGLBufferAllocator;
import com.jme3.util.LWJGLBufferAllocator.ConcurrentLWJGLBufferAllocator;
import static com.jme3.util.LWJGLBufferAllocator.PROPERTY_CONCURRENT_BUFFER_ALLOCATOR;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWJoystickCallback;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.glGetInteger;
import org.lwjgl.opengl.GLCapabilities;

/**
 * A LWJGL implementation of a graphics context.
 */
public abstract class LwjglContextXr implements JmeContext {

    private static final Logger logger = Logger.getLogger(LwjglContextXr.class.getName());

    static {

        final String implementation = BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION;

        if (System.getProperty(implementation) == null) {
            if (Boolean.parseBoolean(System.getProperty(PROPERTY_CONCURRENT_BUFFER_ALLOCATOR, "true"))) {
                System.setProperty(implementation, ConcurrentLWJGLBufferAllocator.class.getName());
            } else {
                System.setProperty(implementation, LWJGLBufferAllocator.class.getName());
            }
        }
    }

    private static final Set<String> SUPPORTED_RENDERS = new HashSet<>(Arrays.asList(
            AppSettings.LWJGL_OPENGL2,
            AppSettings.LWJGL_OPENGL30,
            AppSettings.LWJGL_OPENGL31,
            AppSettings.LWJGL_OPENGL32,
            AppSettings.LWJGL_OPENGL33,
            AppSettings.LWJGL_OPENGL40,
            AppSettings.LWJGL_OPENGL41,
            AppSettings.LWJGL_OPENGL42,
            AppSettings.LWJGL_OPENGL43,
            AppSettings.LWJGL_OPENGL44,
            AppSettings.LWJGL_OPENGL45
    ));

    public static final boolean CL_GL_SHARING_POSSIBLE = true;

    protected final Object createdLock = new Object();
    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AppSettings settings = new AppSettings(true);

    protected EmptyKeyInput keyInput;
    protected EmptyMouseInput mouseInput;
    protected GlfwJoystickInput joyInput;

    protected Timer timer;

    protected Renderer renderer;
    protected SystemListener listener;
    
    protected com.jme3.opencl.lwjgl.LwjglContext clContext;

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
    public void setSystemListener(final SystemListener listener) {
        this.listener = listener;
    }

    protected void printContextInitInfo() {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "LWJGL {0} context running on thread {1}\n * Graphics Adapter: GLFW {2}",
                    APIUtil.toArray(Version.getVersion(), Thread.currentThread().getName(), GLFW.glfwGetVersionString()));
        }
    }

    protected int determineMaxSamples() {

        // If we already have a valid context, determine samples using current context.
        if (GLFW.glfwExtensionSupported("GL_ARB_framebuffer_object")) {
            return glGetInteger(ARBFramebufferObject.GL_MAX_SAMPLES);
        } else if (GLFW.glfwExtensionSupported("GL_EXT_framebuffer_multisample")) {
            return glGetInteger(EXTFramebufferMultisample.GL_MAX_SAMPLES_EXT);
        }

        return Integer.MAX_VALUE;
    }

    protected int getNumSamplesToUse() {

        int samples = 0;

        if (settings.getSamples() > 1) {
            samples = settings.getSamples();
            final int supportedSamples = determineMaxSamples();
            if (supportedSamples < samples) {
                logger.log(Level.WARNING, "Couldn't satisfy antialiasing samples requirement: x{0}. " +
                        "Video hardware only supports: x{1}", APIUtil.toArray(samples, supportedSamples));
                samples = supportedSamples;
            }
        }

        return samples;
    }

    /**
     * Reinitializes the relevant details of the context. For internal use only.
     */
    protected void reinitContext() {
        initContext(false);
    }

    /**
     * Initializes the LWJGL renderer and input for the first time. For internal
     * use only.
     */
    protected void initContextFirstTime() {
        initContext(true);
    }

    /**
     * Initializes the LWJGL renderer and input.
     * @param first - Whether this is the first time we are initializing and we
     * need to create the renderer or not. Otherwise, we'll just reset the
     * renderer as needed.
     */
    private void initContext(boolean first) {

        settings.setRenderer(AppSettings.LWJGL_OPENGL45); // We force Opengl4 as needed for openxr
        final String renderer = settings.getRenderer();
        final GLCapabilities capabilities = createCapabilities(!renderer.equals(AppSettings.LWJGL_OPENGL2));

        if (!capabilities.OpenGL20) {
            throw new RendererException("OpenGL 2.0 or higher is required for jMonkeyEngine");
        } else if (!SUPPORTED_RENDERS.contains(renderer)) {
            throw new UnsupportedOperationException("Unsupported renderer: " + renderer);
        }

        if (first) {
            GL gl = new LwjglGL();
            GLExt glext = new LwjglGLExt();
            GLFbo glfbo;

            if (capabilities.OpenGL30) {
                glfbo = new LwjglGLFboGL3();
            } else {
                glfbo = new LwjglGLFboEXT();
            }

            if (settings.isGraphicsDebug()) {
                gl = (GL) GLDebug.createProxy(gl, gl, GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLDebug.createProxy(gl, glext, GLExt.class);
                glfbo = (GLFbo) GLDebug.createProxy(gl, glfbo, GLFbo.class);
            }

            if (settings.isGraphicsTiming()) {
                GLTimingState timingState = new GLTimingState();
                gl = (GL) GLTiming.createGLTiming(gl, timingState, GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLTiming.createGLTiming(glext, timingState, GLExt.class);
                glfbo = (GLFbo) GLTiming.createGLTiming(glfbo, timingState, GLFbo.class);
            }

            if (settings.isGraphicsTrace()) {
                gl = (GL) GLTracer.createDesktopGlTracer(gl, GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLTracer.createDesktopGlTracer(glext, GLExt.class);
                glfbo = (GLFbo) GLTracer.createDesktopGlTracer(glfbo, GLFbo.class);
            }

            this.renderer = new GLRenderer(gl, glext, glfbo);
            if (this.settings.isGraphicsDebug()) ((GLRenderer)this.renderer).setDebugEnabled(true);
        }
        this.renderer.initialize();

        if (capabilities.GL_ARB_debug_output && settings.isGraphicsDebug()) {
            ARBDebugOutput.glDebugMessageCallbackARB(new LwjglGLDebugOutputHandler(), 0);
        }

        this.renderer.setMainFrameBufferSrgb(settings.isGammaCorrection());
        this.renderer.setLinearizeSrgbImages(settings.isGammaCorrection());

        if (first) {
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

            GLFW.glfwSetJoystickCallback(new GLFWJoystickCallback() {
                @Override
                public void invoke(int jid, int event) {

                    // Invoke the disconnected event before we reload the joysticks or lose the reference to it.
                    // Invoke the connected event after we reload the joysticks to obtain the reference to it.

                    if ( event == GLFW.GLFW_CONNECTED ) {
                        joyInput.reloadJoysticks();
                        joyInput.fireJoystickConnectedEvent(jid);
                    }
                    else {
                        joyInput.fireJoystickDisconnectedEvent(jid);
                        joyInput.reloadJoysticks();
                    }
                }
            });
        }

        renderable.set(true);
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
        synchronized (createdLock) {
            created.set(true);
            createdLock.notifyAll();
        }
        initContextFirstTime();
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

    @Override
    public Context getOpenCLContext() {
        return clContext;
    }
}
