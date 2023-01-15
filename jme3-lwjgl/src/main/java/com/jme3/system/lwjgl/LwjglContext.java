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

import com.jme3.input.lwjgl.JInputJoyInput;
import com.jme3.input.lwjgl.LwjglKeyInput;
import com.jme3.input.lwjgl.LwjglMouseInput;
import com.jme3.opencl.DefaultPlatformChooser;
import com.jme3.opencl.Device;
import com.jme3.opencl.PlatformChooser;
import com.jme3.opencl.lwjgl.LwjglDevice;
import com.jme3.opencl.lwjgl.LwjglPlatform;
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
import com.jme3.renderer.opengl.GLDebug;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.opengl.GLTiming;
import com.jme3.renderer.opengl.GLTimingState;
import com.jme3.renderer.opengl.GLTracer;
import com.jme3.system.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opencl.*;
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
    
    protected LwjglPlatform clPlatform;
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

    protected int[] getGLVersion(String renderer) {
        int maj = -1, min = -1;
        switch (settings.getRenderer()) {
            case AppSettings.LWJGL_OPENGL2:
                maj = 2;
                min = 0;
                break;
            case AppSettings.LWJGL_OPENGL30:
                maj = 3;
                min = 0;
                break;
            case AppSettings.LWJGL_OPENGL31:
                maj = 3;
                min = 1;
                break;
            case AppSettings.LWJGL_OPENGL32:
                maj = 3;
                min = 2;
                break;
            case AppSettings.LWJGL_OPENGL33:
                maj = 3;
                min = 3;
                break;
            case AppSettings.LWJGL_OPENGL40:
                maj = 4;
                min = 0;
                break;
            case AppSettings.LWJGL_OPENGL41:
                maj = 4;
                min = 1;
                break;
            case AppSettings.LWJGL_OPENGL42:
                maj = 4;
                min = 2;
                break;
            case AppSettings.LWJGL_OPENGL43:
                maj = 4;
                min = 3;
                break;
            case AppSettings.LWJGL_OPENGL44:
                maj = 4;
                min = 4;
                break;
            case AppSettings.LWJGL_OPENGL45:
                maj = 4;
                min = 5;
                break;
        }
        return maj == -1 ? null : new int[] { maj, min };
    }

    protected ContextAttribs createContextAttribs() {
        int vers[] = getGLVersion(settings.getRenderer());
        if (settings.isGraphicsDebug() || (vers != null && vers[0] != 2)) {
            ContextAttribs attr;
            if (vers != null && vers[0] != 2) {
                attr = new ContextAttribs(vers[0], vers[1]);
                attr = attr.withProfileCore(true).withForwardCompatible(true).withProfileCompatibility(false);
            } else {
                attr = new ContextAttribs();
            }
            if (settings.isGraphicsDebug()) {
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
            // No PBuffer, assume everything is supported.
            return Integer.MAX_VALUE;
        } else {
            Pbuffer pb = null;
            // OpenGL2 method: Create PBuffer and query samples
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
        if (!GLContext.getCapabilities().OpenGL20) {
            throw new RendererException("OpenGL 2.0 or higher is "
                    + "required for jMonkeyEngine");
        }

        int version[] = getGLVersion(settings.getRenderer());
        if (version != null) {
            if (first) {
                GL gl = new LwjglGL();
                GLExt glext = new LwjglGLExt();
                GLFbo glfbo;

                if (GLContext.getCapabilities().OpenGL30) {
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
                renderer = new GLRenderer(gl, glext, glfbo);
                renderer.initialize();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported renderer: " + settings.getRenderer());
        }
        if (GLContext.getCapabilities().GL_ARB_debug_output && settings.isGraphicsDebug()) {
            ARBDebugOutput.glDebugMessageCallbackARB(new ARBDebugOutputCallback(new LwjglGLDebugOutputHandler()));
        }
        renderer.setMainFrameBufferSrgb(settings.isGammaCorrection());
        renderer.setLinearizeSrgbImages(settings.isGammaCorrection());

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
    }

    @SuppressWarnings("unchecked")
    protected void initOpenCL() {
        logger.info("Initialize OpenCL with LWJGL2");
        
        try {
            CL.create();
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "Unable to initialize OpenCL", ex);
            return;
        }
        
        //load platforms and devices
        StringBuilder platformInfos = new StringBuilder();
        ArrayList<LwjglPlatform> platforms = new ArrayList<>();
        for (CLPlatform p : CLPlatform.getPlatforms()) {
            platforms.add(new LwjglPlatform(p));
        }
        platformInfos.append("Available OpenCL platforms:");
        for (int i=0; i<platforms.size(); ++i) {
            LwjglPlatform platform = platforms.get(i);
            platformInfos.append("\n * Platform ").append(i+1);
            platformInfos.append("\n *   Name: ").append(platform.getName());
            platformInfos.append("\n *   Vendor: ").append(platform.getVendor());
            platformInfos.append("\n *   Version: ").append(platform.getVersion());
            platformInfos.append("\n *   Profile: ").append(platform.getProfile());
            platformInfos.append("\n *   Supports interop: ").append(platform.hasOpenGLInterop());
            List<LwjglDevice> devices = platform.getDevices();
            platformInfos.append("\n *   Available devices:");
            for (int j=0; j<devices.size(); ++j) {
                LwjglDevice device = devices.get(j);
                platformInfos.append("\n *    * Device ").append(j+1);
                platformInfos.append("\n *    *   Name: ").append(device.getName());
                platformInfos.append("\n *    *   Vendor: ").append(device.getVendor());
                platformInfos.append("\n *    *   Version: ").append(device.getVersion());
                platformInfos.append("\n *    *   Profile: ").append(device.getProfile());
                platformInfos.append("\n *    *   Compiler version: ").append(device.getCompilerVersion());
                platformInfos.append("\n *    *   Device type: ").append(device.getDeviceType());
                platformInfos.append("\n *    *   Compute units: ").append(device.getComputeUnits());
                platformInfos.append("\n *    *   Work group size: ").append(device.getMaxiumWorkItemsPerGroup());
                platformInfos.append("\n *    *   Global memory: ").append(device.getGlobalMemorySize()).append("B");
                platformInfos.append("\n *    *   Local memory: ").append(device.getLocalMemorySize()).append("B");
                platformInfos.append("\n *    *   Constant memory: ").append(device.getMaximumConstantBufferSize()).append("B");
                platformInfos.append("\n *    *   Supports double: ").append(device.hasDouble());
                platformInfos.append("\n *    *   Supports half floats: ").append(device.hasHalfFloat());
                platformInfos.append("\n *    *   Supports writable 3d images: ").append(device.hasWritableImage3D());
                platformInfos.append("\n *    *   Supports interop: ").append(device.hasOpenGLInterop());
            }
        }
        logger.info(platformInfos.toString());
        
        //choose devices
        PlatformChooser chooser = null;
        if (settings.getOpenCLPlatformChooser() != null) {
            try {
                chooser = (PlatformChooser) Class.forName(settings.getOpenCLPlatformChooser()).getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "unable to instantiate custom PlatformChooser", ex);
            }
        }
        if (chooser == null) {
            chooser = new DefaultPlatformChooser();
        }
        List<? extends Device> chosenDevices = chooser.chooseDevices(platforms);
        List<CLDevice> devices = new ArrayList<>(chosenDevices.size());
        LwjglPlatform platform = null;
        for (Device d : chosenDevices) {
            if (!(d instanceof LwjglDevice)) {
                logger.log(Level.SEVERE, "attempt to return a custom Device implementation from PlatformChooser: {0}", d);
                return;
            }
            LwjglDevice ld = (LwjglDevice) d;
            if (platform == null) {
                platform = ld.getPlatform();
            } else if (platform != ld.getPlatform()) {
                logger.severe("attempt to use devices from different platforms");
                return;
            }
            devices.add(ld.getDevice());
        }
        if (devices.isEmpty()) {
            logger.warning("no devices specified, no OpenCL context created");
            return;
        }
        clPlatform = platform;
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "chosen platform: {0}", platform.getName());
            logger.log(Level.INFO, "chosen devices: {0}", chosenDevices);
        }
        
        //create context
        try {
            CLContext c = CLContext.create(platform.getPlatform(), devices, null, Display.getDrawable(), null);
            clContext = new com.jme3.opencl.lwjgl.LwjglContext(c, (List<LwjglDevice>) chosenDevices);
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "Unable to create OpenCL context", ex);
            return;
        }
        
        logger.info("OpenCL context created");
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
    public com.jme3.opencl.Context getOpenCLContext() {
        return clContext;
    }

    /**
     * Returns the height of the framebuffer.
     *
     * @return the height (in pixels)
     */
    @Override
    public int getFramebufferHeight() {
        int result = Display.getHeight();
        return result;
    }

    /**
     * Returns the width of the framebuffer.
     *
     * @return the width (in pixels)
     */
    @Override
    public int getFramebufferWidth() {
        int result = Display.getWidth();
        return result;
    }

    /**
     * Returns the screen X coordinate of the left edge of the content area.
     *
     * @return the screen X coordinate
     */
    @Override
    public int getWindowXPosition() {
        int result = Display.getX();
        return result;
    }

    /**
     * Returns the screen Y coordinate of the top edge of the content area.
     *
     * @return the screen Y coordinate
     */
    @Override
    public int getWindowYPosition() {
        int result = Display.getY();
        return result;
    }
}
