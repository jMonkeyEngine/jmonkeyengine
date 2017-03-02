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

import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.glGetInteger;

import com.jme3.input.lwjgl.GlfwJoystickInput;
import com.jme3.input.lwjgl.GlfwKeyInput;
import com.jme3.input.lwjgl.GlfwMouseInput;
import com.jme3.opencl.Context;
import com.jme3.opencl.DefaultPlatformChooser;
import com.jme3.opencl.Device;
import com.jme3.opencl.PlatformChooser;
import com.jme3.opencl.lwjgl.LwjglDevice;
import com.jme3.opencl.lwjgl.LwjglPlatform;
import com.jme3.opencl.lwjgl.Utils;
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
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3.util.BufferAllocatorFactory;
import com.jme3.util.LWJGLBufferAllocator;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opencl.APPLEGLSharing;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.KHRGLSharing;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Platform;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A LWJGL implementation of a graphics context.
 */
public abstract class LwjglContext implements JmeContext {

    private static final Logger logger = Logger.getLogger(LwjglContext.class.getName());

    static {
        final String implementation = BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION;
        if(System.getProperty(implementation) == null) {
            System.setProperty(implementation, LWJGLBufferAllocator.class.getName());
        }
    }

    public static final boolean CL_GL_SHARING_POSSIBLE = true;
    
    protected static final String THREAD_NAME = "jME3 Main";

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean renderable = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Renderer renderer;
    protected GlfwKeyInput keyInput;
    protected GlfwMouseInput mouseInput;
    protected GlfwJoystickInput joyInput;
    protected Timer timer;
    protected SystemListener listener;
    
    protected com.jme3.opencl.lwjgl.LwjglContext clContext;

    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
    }

    protected void printContextInitInfo() {
        logger.log(Level.INFO, "LWJGL {0} context running on thread {1}\n"
                        + " * Graphics Adapter: GLFW {2}",
                new Object[]{org.lwjgl.Version.getVersion(), Thread.currentThread().getName(), GLFW.glfwGetVersionString()});
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
     * Returns a list of the available platforms, filtered by the specified
     * filter.
     *
     * Copied from the old release
     *
     * @return the available platforms
     */
    private static long[] getPlatforms() {
        int[] count = new int[1];
        int errcode = CL10.clGetPlatformIDs(null, count);
        Utils.checkError(errcode, "clGetDeviceIDs");

        int num_platforms = count[0];
        if (num_platforms == 0) {
            return new long[0];
        }

        PointerBuffer platforms = PointerBuffer.allocateDirect(num_platforms);
        errcode = CL10.clGetPlatformIDs(platforms, (IntBuffer) null);
        Utils.checkError(errcode, "clGetDeviceIDs");

        platforms.rewind();
        long[] platformIDs = new long[num_platforms];
        for (int i = 0; i < num_platforms; i++) {
            platformIDs[i] = platforms.get();
        }

        return platformIDs;
    }

    protected void initOpenCL(long window) {
        logger.info("Initialize OpenCL with LWJGL3");
        
//        try {
//            CL.create();
//        } catch (Exception ex) {
//            logger.log(Level.SEVERE, "Unable to initialize OpenCL", ex);
//            return;
//        }
        
        //load platforms and devices
        StringBuilder platformInfos = new StringBuilder();
        ArrayList<LwjglPlatform> platforms = new ArrayList<>();
        for (long p : getPlatforms()) {
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
                chooser = (PlatformChooser) Class.forName(settings.getOpenCLPlatformChooser()).newInstance();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "unable to instantiate custom PlatformChooser", ex);
            }
        }
        if (chooser == null) {
            chooser = new DefaultPlatformChooser();
        }
        List<? extends Device> choosenDevices = chooser.chooseDevices(platforms);
        List<Long> devices = new ArrayList<>(choosenDevices.size());
        LwjglPlatform platform = null;
        for (Device d : choosenDevices) {
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
        logger.log(Level.INFO, "chosen platform: {0}", platform.getName());
        logger.log(Level.INFO, "chosen devices: {0}", choosenDevices);
        
        //create context
        try {
            long c = createContext(platform.getPlatform(), devices, window);
            clContext = new com.jme3.opencl.lwjgl.LwjglContext(c, (List<LwjglDevice>) choosenDevices);
        } catch (final Exception ex) {
            logger.log(Level.SEVERE, "Unable to create OpenCL context", ex);
            return;
        }
        
        logger.info("OpenCL context created");
    }
    private long createContext(final long platform, final List<Long> devices, long window) throws Exception {
        
        final int propertyCount = 2 + 4 + 1;

        final PointerBuffer properties = PointerBuffer.allocateDirect(propertyCount + devices.size());
        
        //set sharing properties
        //https://github.com/glfw/glfw/issues/104
        //https://github.com/LWJGL/lwjgl3/blob/master/modules/core/src/test/java/org/lwjgl/demo/opencl/Mandelbrot.java
        //TODO: test on Linus and MacOSX
        switch (Platform.get()) {
            case WINDOWS:
                properties
                        .put(KHRGLSharing.CL_GL_CONTEXT_KHR)
                        .put(org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext(window))
                        .put(KHRGLSharing.CL_WGL_HDC_KHR)
                        .put(org.lwjgl.opengl.WGL.wglGetCurrentDC());
                break;
            case LINUX:
                properties
                        .put(KHRGLSharing.CL_GL_CONTEXT_KHR)
                        .put(org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext(window))
                        .put(KHRGLSharing.CL_GLX_DISPLAY_KHR)
                        .put(org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display());
                break;
            case MACOSX:
                properties
                        .put(APPLEGLSharing.CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE)
                        .put(org.lwjgl.opengl.CGL.CGLGetShareGroup(org.lwjgl.opengl.CGL.CGLGetCurrentContext()));
        }
        properties.put(CL_CONTEXT_PLATFORM).put(platform);
        properties.put(0);
        properties.flip();

        Utils.errorBuffer.rewind();
        PointerBuffer deviceBuffer = PointerBuffer.allocateDirect(devices.size());
        for (long d : devices) {
            deviceBuffer.put(d);
        }
        deviceBuffer.flip();
        long context = CL10.clCreateContext(properties, deviceBuffer, null, 0, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateContext");
        
        return context;
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
