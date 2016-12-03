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

import static org.lwjgl.glfw.GLFW.GLFW_ALPHA_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_BLUE_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DEPTH_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_GREEN_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_DEBUG_CONTEXT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RED_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_REFRESH_RATE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_SRGB_CAPABLE;
import static org.lwjgl.glfw.GLFW.GLFW_STENCIL_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_STEREO;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.system.MemoryUtil.NULL;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.lwjgl.GlfwJoystickInput;
import com.jme3.input.lwjgl.GlfwKeyInput;
import com.jme3.input.lwjgl.GlfwMouseInput;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.NanoTimer;
import com.jme3.util.BufferUtils;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper class over the GLFW framework in LWJGL 3.
 *
 * @author Daniel Johansson
 */
public abstract class LwjglWindow extends LwjglContext implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(LwjglWindow.class.getName());

    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected final AtomicBoolean needRestart = new AtomicBoolean(false);

    private final JmeContext.Type type;

    private GLFWErrorCallback errorCallback;
    private GLFWWindowSizeCallback windowSizeCallback;
    private GLFWWindowFocusCallback windowFocusCallback;

    private Thread mainThread;

    private double frameSleepTime;
    private long window = NULL;
    private int frameRateLimit = -1;

    protected boolean wasActive = false;
    protected boolean autoFlush = true;
    protected boolean allowSwapBuffers = false;

    public LwjglWindow(final JmeContext.Type type) {
        if (!JmeContext.Type.Display.equals(type) && !JmeContext.Type.OffscreenSurface.equals(type) && !JmeContext.Type.Canvas.equals(type)) {
            throw new IllegalArgumentException("Unsupported type '" + type.name() + "' provided");
        }

        this.type = type;
    }

    /**
     * @return Type.Display or Type.Canvas
     */
    public JmeContext.Type getType() {
        return type;
    }

    /**
     * Set the title if its a windowed display
     *
     * @param title the title to set
     */
    public void setTitle(final String title) {
        if (created.get() && window != NULL) {
            glfwSetWindowTitle(window, title);
        }
    }

    /**
     * Restart if its a windowed or full-screen display.
     */
    public void restart() {
        if (created.get()) {
            needRestart.set(true);
        } else {
            LOGGER.warning("Display is not created, cannot restart window.");
        }
    }

    /**
     * Apply the settings, changing resolution, etc.
     *
     * @param settings the settings to apply when creating the context.
     */
    protected void createContext(final AppSettings settings) {
        glfwSetErrorCallback(errorCallback = new GLFWErrorCallback() {
            @Override
            public void invoke(int error, long description) {
                final String message = GLFWErrorCallback.getDescription(description);
                listener.handleError(message, new Exception(message));
            }

            @Override
            public void close(){
                super.close();
            }

            @Override
            public void callback(long args) {
                super.callback(args);
            }
        });

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();

        if (settings.getRenderer().equals(AppSettings.LWJGL_OPENGL3)) {
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        } else {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        }

        if (settings.getBoolean("RendererDebug")) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        }

        if (settings.isGammaCorrection()) {
            glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE);
        }

        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, settings.isResizable() ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_DEPTH_BITS, settings.getDepthBits());
        glfwWindowHint(GLFW_STENCIL_BITS, settings.getStencilBits());
        glfwWindowHint(GLFW_SAMPLES, settings.getSamples());
        glfwWindowHint(GLFW_STEREO, settings.useStereo3D() ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_REFRESH_RATE, settings.getFrequency());

        if (settings.getBitsPerPixel() == 24) {
            glfwWindowHint(GLFW_RED_BITS, 8);
            glfwWindowHint(GLFW_GREEN_BITS, 8);
            glfwWindowHint(GLFW_BLUE_BITS, 8);
        } else if (settings.getBitsPerPixel() == 16) {
            glfwWindowHint(GLFW_RED_BITS, 5);
            glfwWindowHint(GLFW_GREEN_BITS, 6);
            glfwWindowHint(GLFW_BLUE_BITS, 5);
        }

        glfwWindowHint(GLFW_ALPHA_BITS, settings.getAlphaBits());

        // TODO: Add support for monitor selection
        long monitor = NULL;

        if (settings.isFullscreen()) {
            monitor = glfwGetPrimaryMonitor();
        }

        final GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        if (settings.getWidth() <= 0 || settings.getHeight() <= 0) {
            settings.setResolution(videoMode.width(), videoMode.height());
        }

        window = glfwCreateWindow(settings.getWidth(), settings.getHeight(), settings.getTitle(), monitor, NULL);

        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Add a resize callback which delegates to the listener
        glfwSetWindowSizeCallback(window, windowSizeCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(final long window, final int width, final int height) {
                settings.setResolution(width, height);
                listener.reshape(width, height);
            }

            @Override
            public void close() {
                super.close();
            }

            @Override
            public void callback(long args) {
                super.callback(args);
            }
        });

        glfwSetWindowFocusCallback(window, windowFocusCallback = new GLFWWindowFocusCallback() {
            @Override
            public void invoke(final long window, final boolean focus) {
                if (wasActive != focus) {
                    if (!wasActive) {
                        listener.gainFocus();
                        timer.reset();
                    } else {
                        listener.loseFocus();
                    }

                    wasActive = !wasActive;
                }
            }

            @Override
            public void close() {
                super.close();
            }

            @Override
            public void callback(long args) {
                super.callback(args);
            }
        });

        // Center the window
        if (!settings.isFullscreen()) {
            glfwSetWindowPos(window,
                    (videoMode.width() - settings.getWidth()) / 2,
                    (videoMode.height() - settings.getHeight()) / 2);
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable vsync
        if (settings.isVSync()) {
            glfwSwapInterval(1);
        } else {
            glfwSwapInterval(0);
        }

        setWindowIcon(settings);
        showWindow();

        allowSwapBuffers = settings.isSwapBuffers();
    }

    protected void showWindow() {
        glfwShowWindow(window);
    }

    /**
     * Set custom icons to the window of this application.
     */
    protected void setWindowIcon(final AppSettings settings) {

        final Object[] icons = settings.getIcons();
        if (icons == null) return;

        final GLFWImage[] images = imagesToGLFWImages(icons);

        try (final GLFWImage.Buffer iconSet = GLFWImage.malloc(images.length)) {

            for (int i = images.length - 1; i >= 0; i--) {
                final GLFWImage image = images[i];
                iconSet.put(i, image);
            }

            glfwSetWindowIcon(window, iconSet);
        }
    }

    /**
     * Convert array of images to array of {@link GLFWImage}.
     */
    private GLFWImage[] imagesToGLFWImages(final Object[] images) {

        final GLFWImage[] out = new GLFWImage[images.length];

        for (int i = 0; i < images.length; i++) {
            final BufferedImage image = (BufferedImage) images[i];
            out[i] = imageToGLFWImage(image);
        }

        return out;
    }

    /**
     * Convert the {@link BufferedImage} to the {@link GLFWImage}.
     */
    private GLFWImage imageToGLFWImage(BufferedImage image) {

        if (image.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {

            final BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            final Graphics2D graphics = convertedImage.createGraphics();

            final int targetWidth = image.getWidth();
            final int targetHeight = image.getHeight();

            graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
            graphics.dispose();

            image = convertedImage;
        }

        final ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                int colorSpace = image.getRGB(j, i);
                buffer.put((byte) ((colorSpace << 8) >> 24));
                buffer.put((byte) ((colorSpace << 16) >> 24));
                buffer.put((byte) ((colorSpace << 24) >> 24));
                buffer.put((byte) (colorSpace >> 24));
            }
        }

        buffer.flip();

        final GLFWImage result = GLFWImage.create();
        result.set(image.getWidth(), image.getHeight(), buffer);

        return result;
    }

    /**
     * Destroy the context.
     */
    protected void destroyContext() {
        try {
            if (renderer != null) {
                renderer.cleanup();
            }

            if (errorCallback != null) {
                errorCallback.close();
                errorCallback = null;
            }

            if (windowSizeCallback != null) {
                windowSizeCallback.close();
                windowSizeCallback = null;
            }

            if (windowFocusCallback != null) {
                windowFocusCallback.close();
                windowFocusCallback = null;
            }

            if (window != NULL) {
                glfwDestroyWindow(window);
                window = NULL;
            }
        } catch (final Exception ex) {
            listener.handleError("Failed to destroy context", ex);
        }
    }

    @Override
    public void create(boolean waitFor) {
        if (created.get()) {
            LOGGER.warning("create() called when display is already created!");
            return;
        }

        // NOTE: this is required for Mac OS X!
        mainThread = Thread.currentThread();
        run();
    }

    /**
     * Does LWJGL display initialization in the OpenGL thread
     */
    protected boolean initInThread() {
        try {
            if (!JmeSystem.isLowPermissions()) {
                // Enable uncaught exception handler only for current thread
                Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable thrown) {
                        listener.handleError("Uncaught exception thrown in " + thread.toString(), thrown);
                        if (needClose.get()) {
                            // listener.handleError() has requested the
                            // context to close. Satisfy request.
                            deinitInThread();
                        }
                    }
                });
            }

            timer = new NanoTimer();

            // For canvas, this will create a pbuffer,
            // allowing us to query information.
            // When the canvas context becomes available, it will
            // be replaced seamlessly.
            createContext(settings);
            printContextInitInfo();

            created.set(true);
            super.internalCreate();

            //create OpenCL
            //Must be done here because the window handle is needed
            if (settings.isOpenCLSupport()) {
                initOpenCL(window);
            }

        } catch (Exception ex) {
            try {
                if (window != NULL) {
                    glfwDestroyWindow(window);
                    window = NULL;
                }
            } catch (Exception ex2) {
                LOGGER.log(Level.WARNING, null, ex2);
            }

            listener.handleError("Failed to create display", ex);
            return false; // if we failed to create display, do not continue
        }

        listener.initialize();
        return true;
    }

    /**
     * execute one iteration of the render loop in the OpenGL thread
     */
    protected void runLoop() {
        // If a restart is required, lets recreate the context.
        if (needRestart.getAndSet(false)) {
            try {
                destroyContext();
                createContext(settings);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to set display settings!", ex);
            }

            LOGGER.fine("Display restarted.");
        }

        if (!created.get()) {
            throw new IllegalStateException();
        }

        listener.update();

        // All this does is call swap buffers
        // If the canvas is not active, there's no need to waste time
        // doing that ..
        if (renderable.get()) {
            // calls swap buffers, etc.
            try {
                if (allowSwapBuffers && autoFlush) {
                    glfwSwapBuffers(window);
                }
            } catch (Throwable ex) {
                listener.handleError("Error while swapping buffers", ex);
            }
        }

        // Subclasses just call GLObjectManager clean up objects here
        // it is safe .. for now.
        if (renderer != null) {
            renderer.postFrame();
        }

        if (autoFlush) {
            if (frameRateLimit != getSettings().getFrameRate()) {
                setFrameRateLimit(getSettings().getFrameRate());
            }
        } else if (frameRateLimit != 20) {
            setFrameRateLimit(20);
        }

        // If software frame rate limiting has been asked for, lets calculate sleep time based on a base value calculated
        // from 1000 / frameRateLimit in milliseconds subtracting the time it has taken to render last frame.
        // This gives an approximate limit within 3 fps of the given frame rate limit.
        if (frameRateLimit > 0) {
            final double sleep = frameSleepTime - (timer.getTimePerFrame() / 1000.0);
            final long sleepMillis = (long) sleep;
            final int additionalNanos = (int) ((sleep - sleepMillis) * 1000000.0);

            if (sleepMillis >= 0 && additionalNanos >= 0) {
                try {
                    Thread.sleep(sleepMillis, additionalNanos);
                } catch (InterruptedException ignored) {
                }
            }
        }

        glfwPollEvents();
    }

    private void setFrameRateLimit(int frameRateLimit) {
        this.frameRateLimit = frameRateLimit;
        frameSleepTime = 1000.0 / this.frameRateLimit;
    }

    /**
     * De-initialize in the OpenGL thread.
     */

    protected void deinitInThread() {
        listener.destroy();

        destroyContext();
        super.internalDestroy();

        LOGGER.fine("Display destroyed.");
    }

    @Override
    public void run() {
        if (listener == null) {
            throw new IllegalStateException("SystemListener is not set on context!"
                    + "Must set with JmeContext.setSystemListener().");
        }

        LOGGER.log(Level.FINE, "Using LWJGL {0}", Version.getVersion());

        if (!initInThread()) {
            LOGGER.log(Level.SEVERE, "Display initialization failed. Cannot continue.");
            return;
        }

        while (true) {

            runLoop();

            if (needClose.get()) {
                break;
            }

            if (glfwWindowShouldClose(window)) {
                listener.requestClose(false);
            }
        }

        deinitInThread();
    }

    public JoyInput getJoyInput() {
        if (joyInput == null) {
            joyInput = new GlfwJoystickInput();
        }
        return joyInput;
    }

    public MouseInput getMouseInput() {
        if (mouseInput == null) {
            mouseInput = new GlfwMouseInput(this);
        }
        return mouseInput;
    }

    public KeyInput getKeyInput() {
        if (keyInput == null) {
            keyInput = new GlfwKeyInput(this);
        }

        return keyInput;
    }

    public TouchInput getTouchInput() {
        return null;
    }

    public void setAutoFlushFrames(boolean enabled) {
        this.autoFlush = enabled;
    }

    public void destroy(boolean waitFor) {
        needClose.set(true);

        if (mainThread == Thread.currentThread()) {
            // Ignore waitFor.
            return;
        }

        if (waitFor) {
            waitFor(false);
        }
    }

    public long getWindowHandle() {
        return window;
    }
}
