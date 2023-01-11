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
import com.jme3.input.lwjgl.GlfwKeyInput;
import com.jme3.input.lwjgl.GlfwMouseInput;
import com.jme3.lwjgl3.utils.APIUtil;
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
import com.jme3.renderer.opengl.*;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3.util.BufferAllocatorFactory;
import com.jme3.util.LWJGLBufferAllocator;
import com.jme3.util.LWJGLBufferAllocator.ConcurrentLWJGLBufferAllocator;
import static com.jme3.util.LWJGLBufferAllocator.PROPERTY_CONCURRENT_BUFFER_ALLOCATOR;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toSet;
import org.lwjgl.PointerBuffer;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWJoystickCallback;
import org.lwjgl.opencl.APPLEGLSharing;
import org.lwjgl.opencl.CL10;
import static org.lwjgl.opencl.CL10.CL_CONTEXT_PLATFORM;
import org.lwjgl.opencl.KHRGLSharing;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.glGetInteger;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

/**
 * A LWJGL implementation of a graphics context.
 */
public abstract class LwjglContext implements JmeContext {

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

    protected GlfwKeyInput keyInput;
    protected GlfwMouseInput mouseInput;
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

    /**
     * Returns a list of the available platforms, filtered by the specified
     * filter.
     * <p>
     * Copied from the old release
     *
     * @return the available platforms
     */
    private static long[] getPlatforms() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            final IntBuffer countBuffer = stack.callocInt(1);
            int errcode = CL10.clGetPlatformIDs(null, countBuffer);
            Utils.checkError(errcode, "clGetDeviceIDs");

            final int count = countBuffer.get();
            final PointerBuffer pointer = stack.callocPointer(count);

            errcode = CL10.clGetPlatformIDs(pointer, (IntBuffer) null);
            Utils.checkError(errcode, "clGetDeviceIDs");

            final long[] platformIDs = new long[count];
            for (int i = 0; i < count; i++) {
                platformIDs[i] = pointer.get();
            }

            return platformIDs;
        }
    }

    @SuppressWarnings("unchecked")
    protected void initOpenCL(final long window) {
        logger.info("Initialize OpenCL with LWJGL3");
        
//        try {
//            CL.create();
//        } catch (Exception ex) {
//            logger.log(Level.SEVERE, "Unable to initialize OpenCL", ex);
//            return;
//        }
        
        // load platforms and devices
        StringBuilder platformInfos = new StringBuilder();
        List<LwjglPlatform> platforms = new ArrayList<>();
        for (long platformId : getPlatforms()) {
            platforms.add(new LwjglPlatform(platformId));
        }

        platformInfos.append("Available OpenCL platforms:");

        for (int i = 0; i < platforms.size(); ++i) {
            LwjglPlatform platform = platforms.get(i);
            platformInfos.append("\n * Platform ").append(i + 1);
            platformInfos.append("\n *   Name: ").append(platform.getName());
            platformInfos.append("\n *   Vendor: ").append(platform.getVendor());
            platformInfos.append("\n *   Version: ").append(platform.getVersion());
            platformInfos.append("\n *   Profile: ").append(platform.getProfile());
            platformInfos.append("\n *   Supports interop: ").append(platform.hasOpenGLInterop());
            List<LwjglDevice> devices = platform.getDevices();
            platformInfos.append("\n *   Available devices:");
            for (int j=0; j<devices.size(); ++j) {
                LwjglDevice device = devices.get(j);
                platformInfos.append("\n *    * Device ").append(j + 1);
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
                logger.log(Level.WARNING, "Unable to instantiate custom PlatformChooser", ex);
            }
        }

        if (chooser == null) {
            chooser = new DefaultPlatformChooser();
        }

        final List<? extends Device> chosenDevices = chooser.chooseDevices(platforms);
        final Optional<? extends Device> unsupportedDevice = chosenDevices.stream()
                .filter(dev -> !(dev instanceof LwjglDevice))
                .findAny();

        if (unsupportedDevice.isPresent()) {
            logger.log(Level.SEVERE, "attempt to return a custom Device implementation " +
                    "from PlatformChooser: {0}", unsupportedDevice.get());
            return;
        }

        final Set<LwjglPlatform> lwjglPlatforms = chosenDevices.stream()
                .map(LwjglDevice.class::cast)
                .map(LwjglDevice::getPlatform)
                .collect(toSet());

        if (lwjglPlatforms.size() != 1) {
            logger.severe("attempt to use devices from different platforms");
            return;
        }

        final long[] deviceIds = chosenDevices.stream()
                .map(LwjglDevice.class::cast)
                .mapToLong(LwjglDevice::getDevice)
                .toArray();

        if (deviceIds.length < 1) {
            logger.warning("no devices specified, no OpenCL context created");
            return;
        }

        final LwjglPlatform platform = lwjglPlatforms.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found a platform"));

        logger.log(Level.INFO, "chosen platform: {0}", platform.getName());
        logger.log(Level.INFO, "chosen devices: {0}", chosenDevices);
        
        // create context
        try {
            long context = createContext(platform.getPlatform(), deviceIds, window);
            clContext = new com.jme3.opencl.lwjgl.LwjglContext(context, (List<LwjglDevice>) chosenDevices);
        } catch (final Exception ex) {
            logger.log(Level.SEVERE, "Unable to create OpenCL context", ex);
            return;
        }
        
        logger.info("OpenCL context created");
    }

    private long createContext(final long platform, final long[] devices, long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            final int propertyCount = 2 + 4 + 1;
            final PointerBuffer properties = stack.callocPointer(propertyCount + devices.length);

            // set sharing properties
            // https://github.com/glfw/glfw/issues/104
            // https://github.com/LWJGL/lwjgl3/blob/master/modules/core/src/test/java/org/lwjgl/demo/opencl/Mandelbrot.java
            // TODO: test on Linux and MacOSX
            switch (Platform.get()) {
                case WINDOWS:
                    properties.put(KHRGLSharing.CL_GL_CONTEXT_KHR)
                            .put(org.lwjgl.glfw.GLFWNativeWGL.glfwGetWGLContext(window))
                            .put(KHRGLSharing.CL_WGL_HDC_KHR)
                            .put(org.lwjgl.opengl.WGL.wglGetCurrentDC());
                    break;
                case LINUX:
                    properties.put(KHRGLSharing.CL_GL_CONTEXT_KHR)
                            .put(org.lwjgl.glfw.GLFWNativeGLX.glfwGetGLXContext(window))
                            .put(KHRGLSharing.CL_GLX_DISPLAY_KHR)
                            .put(org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display());
                    break;
                case MACOSX:
                    properties.put(APPLEGLSharing.CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE)
                            .put(org.lwjgl.opengl.CGL.CGLGetShareGroup(org.lwjgl.opengl.CGL.CGLGetCurrentContext()));
                    break;
                default:
                    break; // Unknown Platform, do nothing.
            }

            properties.put(CL_CONTEXT_PLATFORM).put(platform);
            properties.put(0);
            properties.flip();

            final IntBuffer error = stack.callocInt(1);
            final PointerBuffer deviceBuffer = stack.callocPointer(devices.length);
            for (final long deviceId : devices) {
                deviceBuffer.put(deviceId);
            }

            deviceBuffer.flip();

            long context = CL10.clCreateContext(properties, deviceBuffer, null, 0, error);
            Utils.checkError(error, "clCreateContext");

            return context;
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
