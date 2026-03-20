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

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.lwjgl3.utils.APIUtil;
import com.jme3.opencl.Context;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.lwjgl.LwjglGL;
import com.jme3.renderer.lwjgl.LwjglGLES;
import com.jme3.renderer.lwjgl.LwjglGLExt;
import com.jme3.renderer.lwjgl.LwjglGLFboEXT;
import com.jme3.renderer.lwjgl.LwjglGLFboGL3;
import com.jme3.renderer.opengl.*;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3.util.BufferAllocatorFactory;
import com.jme3.util.BufferUtils;
import com.jme3.util.LWJGLBufferAllocator;
import com.jme3.util.LWJGLBufferAllocator.ConcurrentLWJGLBufferAllocator;
import static com.jme3.util.LWJGLBufferAllocator.PROPERTY_CONCURRENT_BUFFER_ALLOCATOR;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.Version;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.glGetInteger;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengles.GLES;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.opengles.GLESCapabilities;
import static org.lwjgl.sdl.SDLVideo.*;
/**
 * A LWJGL implementation of a graphics context.
 */
public abstract class LwjglContext implements JmeContext {
    protected boolean useAngle = false;

    private static final Logger logger = Logger.getLogger(LwjglContext.class.getName());

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
            AppSettings.LWJGL_OPENGL32,
            AppSettings.LWJGL_OPENGL33,
            AppSettings.LWJGL_OPENGL40,
            AppSettings.LWJGL_OPENGL41,
            AppSettings.LWJGL_OPENGL42,
            AppSettings.LWJGL_OPENGL43,
            AppSettings.LWJGL_OPENGL44,
            AppSettings.LWJGL_OPENGL45, AppSettings.ANGLE_GLES3
    ));

    public static final boolean CL_GL_SHARING_POSSIBLE = false;

    protected final Object createdLock = new Object();
    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AppSettings settings = new AppSettings(true);

    protected KeyInput keyInput;
    protected MouseInput mouseInput;
    protected JoyInput joyInput;

    protected Timer timer;

    protected Renderer renderer;
    protected SystemListener listener;
    
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
            logger.log(Level.INFO, "LWJGL {0} context running on thread {1}\n * Video backend: SDL {2}",
                    APIUtil.toArray(Version.getVersion(), Thread.currentThread().getName(), SDL_GetCurrentVideoDriver()));
        }
    }

    protected int determineMaxSamples() {
        final GLCapabilities capabilities = org.lwjgl.opengl.GL.getCapabilities();
        if (capabilities.GL_ARB_framebuffer_object) {
            return glGetInteger(ARBFramebufferObject.GL_MAX_SAMPLES);
        } else if (capabilities.GL_EXT_framebuffer_multisample) {
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

        final String renderer = settings.getRenderer();

        if (first) {
            GL gl;
            GLExt glext;
            GLFbo glfbo;
            if (!useAngle) {

                final GLCapabilities capabilities = createCapabilities(true);

                if (!SUPPORTED_RENDERS.contains(renderer)) {
                    throw new UnsupportedOperationException("Unsupported renderer: " + renderer);
                } else if (!capabilities.OpenGL32) {
                    throw new RendererException("OpenGL 3.2 core profile or higher is required for the LWJGL3 backend");
                }

                gl = new LwjglGL();
                glext = new LwjglGLExt();

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
                    gl = (GL) GLTiming.createGLTiming(gl, timingState, GL.class, GL2.class, GL3.class,
                            GL4.class);
                    glext = (GLExt) GLTiming.createGLTiming(glext, timingState, GLExt.class);
                    glfbo = (GLFbo) GLTiming.createGLTiming(glfbo, timingState, GLFbo.class);
                }

                if (settings.isGraphicsTrace()) {
                    gl = (GL) GLTracer.createDesktopGlTracer(gl, GL.class, GL2.class, GL3.class, GL4.class);
                    glext = (GLExt) GLTracer.createDesktopGlTracer(glext, GLExt.class);
                    glfbo = (GLFbo) GLTracer.createDesktopGlTracer(glfbo, GLFbo.class);
                }

                if (capabilities.GL_ARB_debug_output && settings.isGraphicsDebug()) {
                    ARBDebugOutput.glDebugMessageCallbackARB(new LwjglGLDebugOutputHandler(), 0);
                }

            } else {
                final GLESCapabilities capabilities = GLES.createCapabilities();
                LwjglGLES gles = new LwjglGLES();

                if (settings.isGraphicsDebug()) {
                    gles = (LwjglGLES) GLDebug.createProxy(gles, gles, GL.class, GLES_30.class, GLFbo.class,
                            GLExt.class);

                }

                if (settings.isGraphicsTiming()) {
                    GLTimingState timingState = new GLTimingState();
                    gles = (LwjglGLES) GLTiming.createGLTiming(gles, timingState, GL.class, GLES_30.class,
                            GLFbo.class, GLExt.class);
                }

                if (settings.getBoolean("GraphicsTrace")) {
                    gles = (LwjglGLES) GLTracer.createGlesTracer(gles, GL.class, GLES_30.class, GLFbo.class,
                            GLExt.class);
                }

                gl = gles;
                glext = gles;
                glfbo = gles;

            }
            this.renderer = new GLRenderer(gl, glext, glfbo);
            if (this.settings.isGraphicsDebug()) ((GLRenderer)this.renderer).setDebugEnabled(true);
        }
        this.renderer.initialize();

        boolean isSrgbFb = settings.isGammaCorrection();

        if (this.renderer.getCaps().contains(Caps.OpenGL30)) {
            int[] value = new int[1];
            GL30.glGetFramebufferAttachmentParameteriv(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, value);
            isSrgbFb = (value[0] == GL30.GL_SRGB);
        } else if (this.renderer.getCaps().contains(Caps.OpenGLES30)) {
            IntBuffer value = BufferUtils.createIntBuffer(1);
            GLES30.glGetFramebufferAttachmentParameteriv(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                    GLES30.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, value);
            isSrgbFb = (value.get(0) == GLES30.GL_SRGB);
        }

        if (!isSrgbFb && settings.isGammaCorrection()) {
            logger.warning(
                    "sRGB framebuffer not supported by the backend platform, disabling gamma correction");
        }

        this.renderer.setMainFrameBufferSrgb(isSrgbFb);
        this.renderer.setLinearizeSrgbImages(isSrgbFb);

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
        useAngle = settings.getRenderer().equals(AppSettings.ANGLE_GLES3);
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
        return null;
    }
}
