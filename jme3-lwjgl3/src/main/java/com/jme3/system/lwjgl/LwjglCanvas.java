/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
import com.jme3.input.TouchInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.input.lwjgl.SdlJoystickInput;
import com.jme3.system.AppSettings;
import com.jme3.system.Displays;
import com.jme3.system.JmeCanvasContext;
import com.jme3.system.lwjglx.LwjglxGLPlatform;

import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.SwingUtilities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.Version;
import org.lwjgl.awthacks.NonClearGraphics;
import org.lwjgl.awthacks.NonClearGraphics2D;
import org.lwjgl.opengl.awt.GLData;

import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;

import static org.lwjgl.system.MemoryUtil.*;
import static com.jme3.system.lwjglx.LwjglxDefaultGLPlatform.*;

/**
 * Class <code>LwjglCanvas</code> that integrates <a href="https://github.com/LWJGLX/lwjgl3-awt">LWJGLX</a>
 * which allows using AWT-Swing components, make sure you use an OpenGL renderer:
 * <pre><code>
 * settings.setRenderer(AppSettings.LWJGL_OPENGL32);
 * </code></pre>
 *
 * <p>
 * If <b>LwjglCanvas</b> throws an exception due to configuration problems, we can debug as follows:
 * <br>
 * - In <code>AppSettings</code>, set this property to enable a debug that displays
 * the effective data for the context.
 * <pre><code>
 * ....
 *  AppSettings settings = new AppSettings(true);
 *  settings.putBoolean("GLDataEffectiveDebug", true);
 * ...
 * </code></pre>
 *
 * <p>
 * <b>NOTE:</b> If running <code>LwjglCanvas</code> on older machines, the <code>SRGB | Gamma Correction</code> option
 * will raise an exception, so it should be disabled.
 * <pre><code>
 * ....
 *  AppSettings settings = new AppSettings(true);
 *  settings.setGammaCorrection(false);
 * ...
 * </code></pre>
 *
 * @author wil
 */
public class LwjglCanvas extends LwjglWindow implements JmeCanvasContext, Runnable {

    /** Logger class. */
    private static final Logger LOGGER = Logger.getLogger(LwjglCanvas.class.getName());

    /** GL versions map. */
    private static final Map<String, Consumer<GLData>> RENDER_CONFIGS = new HashMap<>();

    /*
        Register the different versions.

        The 'COMPATIBILITY' profile is used for operational reasons on different
        platforms.
    
        see the discussion:
        https://github.com/jMonkeyEngine/jmonkeyengine/pull/2153#issuecomment-1860913192
    */
    static {
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL32, (data) -> {
            data.majorVersion = 3;
            data.minorVersion = 2;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL33, (data) -> {
            data.majorVersion = 3;
            data.minorVersion = 3;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL40, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 0;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL41, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 1;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL42, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 2;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL43, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 3;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL44, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 4;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL45, (data) -> {
            data.majorVersion = 4;
            data.minorVersion = 5;
            data.profile = GLData.Profile.COMPATIBILITY;
        });
    }

    /**
     * An AWT <code>java.awt.Canvas</code> that supports to be drawn on using OpenGL.
     */
    private class LwjglAWTGLCanvas extends Canvas {

        /**
         * A {@link com.jme3.system.lwjglx.LwjglxGLPlatform} object.
         * @see org.lwjgl.opengl.awt.PlatformGLCanvas
         */
        private LwjglxGLPlatform platformCanvas;

        /**  The OpenGL context (LWJGL3-AWT). */
        private long context;

        /**
         * Information object used to create the OpenGL context.
         */
        private GLData data;

        /** Effective data to initialize the context. */
        private GLData effective;

        /**
         * Constructor of the <code>LwjglAWTGLCanva</code> class where objects are
         * initialized for OpenGL-AWT rendering
         *
         * @param data A {@link org.lwjgl.opengl.awt.GLData} object
         */
        public LwjglAWTGLCanvas(GLData data) {
            this.effective = new GLData();
            this.context   = NULL;
            this.data      = data;

            try {
                platformCanvas = createLwjglxGLPlatform();
            } catch (UnsupportedOperationException e) {
                listener.handleError(e.getLocalizedMessage(), e);
            }
        }

        /**
         * (non-Javadoc)
         * @see java.awt.Component#addComponentListener(java.awt.event.ComponentListener)
         * @param l object-listener
         */
        @Override
        public synchronized void addComponentListener(ComponentListener l) {
            super.addComponentListener(l);
        }

        /**
         * This is where the OpenGL rendering context is generated.
         */
        public void createContext() {
            try {
                context = platformCanvas.create(this, data, effective);
            } catch (AWTException e) {
                listener.handleError("Exception while creating the OpenGL context", e);
            }
        }
        /** (non-Javadoc) */
        public boolean hasContext() {
            synchronized (lock) {
                return context != NULL;
            }
        }

        /**
         * Make the canvas' context current. It is highly recommended that the
         * context is only made current inside the AWT thread (for example in an
         * overridden paintGL()).
         */
        public void makeCurrent() {
            synchronized (lock) {
                if (context == NULL) {
                    throw new IllegalStateException("Canvas not yet displayable");
                }
                platformCanvas.makeCurrent(context);
            }
        }

        /**
         * Release the rendering context
         */
        public void releaseContext() {
            synchronized (lock) {
                platformCanvas.makeCurrent(NULL);
            }
        }

        /**
         * Returns the effective data (recommended or ideal) to initialize the
         * LWJGL3-AWT context.
         *
         * @return A {@link org.lwjgl.opengl.awt.GLData} object
         */
        public GLData getGLDataEffective() {
            return effective;
        }

        /**
         * To start drawing on the AWT surface, the AWT threads must be locked to
         * avoid conflicts when drawing on the canvas.
         */
        public void lock() {
            synchronized (lock) {
                try {
                    platformCanvas.lock();// <- MUST lock on Linux
                } catch (AWTException e) {
                    listener.handleError("Failed to lock Canvas", e);
                }
            }
        }

        /**
         * Unlock the current AWT thread to continue updating the user interface.
         */
        public void unlock() {
            synchronized (lock) {
                try {
                    platformCanvas.unlock();// <- MUST unlock on Linux
                } catch (AWTException e) {
                    listener.handleError("Failed to unlock Canvas", e);
                }
            }
        }

        /**
         * Frees up the drawing surface
         */
        public void doDisposeCanvas() {
            platformCanvas.dispose();
        }

        /**
         * This is where you actually draw on the canvas (framebuffer).
         */
        public void swapBuffers() {
            platformCanvas.swapBuffers();
        }

        /**
         * (non-Javadoc)
         * @see java.awt.Component#addNotify()
         */
        @Override
        public void addNotify() {
            super.addNotify();
            /* you have to notify if the canvas is visible to draw on it. */
            synchronized (lock) {
                hasNativePeer.set(true);
            }
            requestFocusInWindow();
        }

        /**
         * (non-Javadoc)
         * @see java.awt.Component#removeNotify()
         */
        @Override
        public void removeNotify() {
            if (needClose.get()) {
                LOGGER.log(Level.FINE, "EDT: Application is stopped. Not restoring canvas.");
                super.removeNotify();
                return;
            }

            synchronized (lock) {
                // prepare for a possible re-adding
                hasNativePeer.set(false);
                reinitcontext.set(true);

                 while (reinitcontext.get()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                        super.removeNotify();
                        return;
                    }
                }

                 reinitcontext.set(false);
            }

            // GL context is dead at this point
            LOGGER.log(Level.FINE, "EDT: Acknowledged receipt of canvas death");
            super.removeNotify();
        }

        /**
         * (non-Javadoc)
         * @see com.jme3.system.lwjglx.LwjglxGLPlatform#destroy()
         */
        public void destroy() {
            platformCanvas.destroy();
        }
        /** (non-Javadoc) */
        public void deleteContext() {
            platformCanvas.deleteContext(context);
        }

        /**
         * Returns Graphics object that ignores {@link java.awt.Graphics#clearRect(int, int, int, int)}
         * calls.
         * <p>
         * This is done so that the frame buffer will not be cleared by AWT/Swing internals.
         *
         * @see org.lwjgl.awthacks.NonClearGraphics2D
         * @see org.lwjgl.awthacks.NonClearGraphics
         * @return Graphics
         */
        @Override
        public Graphics getGraphics() {
            Graphics graphics = super.getGraphics();
            if (graphics instanceof Graphics2D) {
                return new NonClearGraphics2D((Graphics2D) graphics);
            }
            return new NonClearGraphics(graphics);
        }
    }

    /** Canvas-AWT. */
    private final LwjglAWTGLCanvas canvas;

    /**
     * Configuration data to start the AWT context, this is used by the
     * {@code lwjgl-awt} library.
     */
    private final GLData glData;

    /** Used to notify the canvas status ({@code remove()/add()}). */
    private final AtomicBoolean hasNativePeer = new AtomicBoolean(false);
    /**
     * It is used to create the initial context and all the resources that will
     * be activated only once.
     */
    private final AtomicBoolean initialize = new AtomicBoolean(false);
    /** Notify the context reintegration, invalidating the current renderer. */
    private final AtomicBoolean reinitcontext = new AtomicBoolean(false);

    /** Notify if there is a change in canvas dimensions. */
    private final AtomicBoolean needResize = new AtomicBoolean(false);

    /**
     * Flag that uses the context to check if it is initialized or not, this prevents
     * it from being initialized multiple times and potentially breaking the JVM.
     */
    private final AtomicBoolean contextFlag = new AtomicBoolean(false);

    /** lock-object. */
    private final Object lock = new Object();

    /** Framebuffer width. */
    private int framebufferWidth = 1;
    /** Framebuffer height. */
    private int framebufferHeight = 1;

    /** AWT keyboard input manager. */
    private AwtKeyInput keyInput;

    /** AWT mouse input manager. */
    private AwtMouseInput mouseInput;

    /**
     * Generate a new OpenGL context (<code>LwjglCanvas</code>) to integrate
     * AWT/Swing with JME3 in your desktop applications.
     */
    public LwjglCanvas() {
        super(Type.Canvas);
        glData = new GLData();
        canvas = new LwjglAWTGLCanvas(glData);
        canvas.setIgnoreRepaint(true);

        // To determine the size of the framebuffer every time the user resizes
        // the canvas (this works if the component has a parent)
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                synchronized (lock) {
                    updateSizes();
                }
            }
        });
    }

    /**
     * Returns the GL context handler.
     *
     * @return String
     */
    @Override
    protected String getCurrentVideoDriver() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("AWT|Swing (LWJGLX) GLv")
                .append(canvas.data.majorVersion)
                .append('.')
                .append(canvas.data.minorVersion);

        String driver = isWayland() ? "(XWayland|X11) GLX" : "X11 GLX";

        Platform platform = Platform.get();
        if (null == platform) {
            buffer.append(" Unknown NULL");
        } else {
            switch (platform) {
                case FREEBSD:
                    buffer.append(" FreeBSD ")
                          .append(driver);
                    break;
                case LINUX:
                    buffer.append(" Linux ")
                           .append(driver);
                    break;
                case MACOSX:
                    buffer.append(" MacOSX Cocoa NSGL");
                    break;
                case WINDOWS:
                    buffer.append(" Win32 WGL");
                    break;
                default:
                    break;
            }
        }
        return String.valueOf(buffer);
    }

    /**
     * Check if the canvas is displayed, that is, if it has a parent that has set it up.
     * <p>
     * It is very important that this verification be done so that LWJGL3-AWT works correctly.
     *
     * @return returns <code>true</code> if the canvas is ready to draw; otherwise
     * returns <code>false</code>
     */
    public boolean checkVisibilityState() {
        if (!hasNativePeer.get()) {
            return false;
        }
        return canvas.isDisplayable() && canvas.isShowing();
    }

    /**
     * Here the entire GL context is rendered and initialized.
     */
    @Override
    public void run() {
        if (listener == null) {
            throw new IllegalStateException(
                    "SystemListener is not set on context! Must set with JmeContext.setSystemListener()."
            );
        }

        LOGGER.log(Level.FINE, "Using LWJGL {0}", Version.getVersion());

        while (true) {
            if (needResize.getAndSet(false)) {
                settings.setResolution(framebufferWidth, framebufferHeight);
                listener.reshape(framebufferWidth, framebufferHeight, framebufferWidth, framebufferHeight);
            }

            synchronized (lock) {
                if (reinitcontext.getAndSet(false)) {
                    LOGGER.log(Level.FINE, "LWJGX: Destroying display ..");

                    try {
                        if (renderer != null) {
                            renderer.invalidateState();
                            renderer.cleanup();
                        }

                        canvas.releaseContext();
                        canvas.deleteContext();
                        canvas.doDisposeCanvas();
                        canvas.context = NULL;
                    } finally {
                        renderable.set(false);
                        lock.notifyAll();
                    }
                }
            }

            if (checkVisibilityState()) {
                // HACK: All components of the thread hosted in initInt() must be
                //       called after the context is created, but this is only valid
                //       if the canvas is validated by AWT, so it is created at "runtime".
                if (!initialize.getAndSet(true)) {
                    if (!initInThread()) {
                        LOGGER.log(Level.SEVERE, "Display initialization failed. Cannot continue.");
                        break;
                    }
                } else {
                    if (!canvas.hasContext()) {
                        LOGGER.log(Level.FINE, "AWT: Creating display ..");
                        createContext(settings);
                        reinitContext();

                        listener.gainFocus();
                    }
                }

                // HACK: In this thread, let OpenGL handle the heavy lifting,
                //       blocking the AWT/Swing EDT only to draw the corresponding
                //       buffer, thus preventing the user interface from freezing
                //       with demanding scenes.
                runLoop();

                // All this does is call swapBuffers().
                // If the canvas is not active, there's no need to waste time
                // doing that.
                if (renderable.get() && canvas.hasContext() && canvas.isValid()) {
                    try {
                        if (allowSwapBuffers && autoFlush) {
                            // calls swap buffers | lock, etc.
                            try {
                                canvas.lock();
                                canvas.swapBuffers();
                            } finally {
                                canvas.unlock();
                            }

                            // Sync the display on some systems.
                            Toolkit.getDefaultToolkit().sync();
                        }
                    } catch (Throwable ex) {
                        listener.handleError("Error while swapping buffers", ex);
                    }
                }
            } else {
                // HACK: If the GL context is not rendering, the thread will
                //       enter a waiting state, thus avoiding CPU overload.
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ignore) { }
            }

            if (needClose.get()) {
                break;
            }
        }

        deinitInThread();
    }

    /**
     * execute one iteration of the render loop in the OpenGL thread
     */
    @Override
    protected void runLoop() {
        // If a restart is required, lets recreate the context.
        if (needRestart.getAndSet(false)) {
            restartContext();
        }

        if (!created.get()) {
            throw new IllegalStateException();
        }

        if (!renderFrameWithBlitFramebuffer()) {
            listener.update();
        }

        // Subclasses just call GLObjectManager. Clean up objects here.
        // It is safe ... for now.
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

        Sync.sync(frameRateLimit);
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.JmeContext#destroy(boolean)
     * @param waitFor boolean
     */
    @Override
    public void destroy(boolean waitFor) {
        super.destroy(waitFor);
        this.contextFlag.set(false);
        this.initialize.set(false);
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.JmeContext#create(boolean)
     * @param waitFor boolean
     */
    @Override
    public void create(boolean waitFor) {
        if (this.contextFlag.get()) {
            return;
        }
        // create context
        super.create(waitFor);
        this.contextFlag.set(true);
    }
    
    /**(non-Javadoc)
     * @param createdVal boolean
     */
    @Override
    protected void waitFor(boolean createdVal) {
        // AWT together with LWJGLX cannot handle waitFor() in the best way,
        // since the context is created on the fly.
        if (createdVal) {
            LOGGER.log(Level.WARNING, "create(true) is not supported for AWT!");
        }
    }
    
    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglWindow#destroyContext()
     */
    @Override
    protected void destroyContext() {
        synchronized (lock) {
            canvas.destroy();
        }
        super.destroyContext();
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglWindow#createContext(com.jme3.system.AppSettings)
     * @param settings A {@link com.jme3.system.AppSettings} object
     */
    @Override
    protected void createContext(AppSettings settings) {
        if (!settings.isX11PlatformPreferred() && isWayland()) {
            LOGGER.log(Level.WARNING, "LWJGLX and AWT/Swing only work with X11, so XWayland will be used for GLX.");
        }

        // HACK: For LWJGLX to work in Wyland, it is necessary to use GLX via
        //       XWayland, so LWJGL must be forced to load GLX as a native API.
        //       This is because LWJGLX does not provide an EGL context.
        if (isWayland()) {
            Configuration.OPENGL_CONTEXT_API.set("native");
        }

        RENDER_CONFIGS.computeIfAbsent(settings.getRenderer(), (t) -> {
            return (data) -> {
                data.majorVersion = 2;
                data.minorVersion = 0;
            };
        }).accept(glData);

        if (settings.getBitsPerPixel() == 24) {
            glData.redSize = 8;
            glData.greenSize = 8;
            glData.blueSize = 8;
        } else if (settings.getBitsPerPixel() == 16) {
            glData.redSize = 5;
            glData.greenSize = 6;
            glData.blueSize = 5;
        }

        // Enable vsync for LWJGL3-AWT
        if (settings.isVSync()) {
            glData.swapInterval = 1;
        } else {
            glData.swapInterval = 0;
        }

        glData.alphaSize = settings.getAlphaBits();
        glData.sRGB = settings.isGammaCorrection(); // Not compatible with very old devices

        glData.depthSize = settings.getDepthBits();
        glData.stencilSize = settings.getStencilBits();
        glData.samples = settings.getSamples();
        glData.stereo = settings.useStereo3D();

        glData.debug = settings.isGraphicsDebug();
        glData.api = GLData.API.GL;

        /* This is done to prevent the context from breaking in Windows,
         * since the 'CORE' profile causes rendering failures (black screen).
         */
        glData.forwardCompatible = false;

        allowSwapBuffers = settings.isSwapBuffers();

        canvas.createContext();
        canvas.makeCurrent();

        SwingUtilities.invokeLater(() -> {
            canvas.validate();
        });

        // This will activate the "effective data" scrubber.
        if (settings.getBoolean("GLDataEffectiveDebug")) {
            LOGGER.log(Level.INFO, "[ DEBUGGER ] :Effective data to initialize the LWJGL3-AWT context\n{0}",
                    getPrintContextInitInfo(canvas.getGLDataEffective()));
        }
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglWindow#getKeyInput()
     * @return KeyInput
     */
    @Override
    public KeyInput getKeyInput() {
        if (keyInput == null) {
            keyInput = new AwtKeyInput();
            keyInput.setInputSource(canvas);
        }
        return keyInput;
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglWindow#getMouseInput()
     * @return MouseInput
     */
    @Override
    public MouseInput getMouseInput() {
        if (mouseInput == null) {
            mouseInput = new AwtMouseInput();
            mouseInput.setInputSource(canvas);
        }
        return mouseInput;
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglWindow#getJoyInput()
     * @return JoyInput
     */
    @Override
    public JoyInput getJoyInput() {
        if (joyInput == null) {
            String mapper = settings.getJoysticksMapper();
            if (AppSettings.JOYSTICKS_LEGACY_MAPPER.equals(mapper)
                    || AppSettings.JOYSTICKS_XBOX_LEGACY_MAPPER.equals(mapper)) {

                LOGGER.log(Level.WARNING, () -> "LWJGX does not support this configuration: " + mapper);
            }
            joyInput = new SdlJoystickInput(settings);
        }
        return joyInput;
    }

    /** (non-Javadoc) */
    @Override public TouchInput getTouchInput() { return null; }
    /** (non-Javadoc) */
    @Override public void setTitle(String title) { }

    /** (non-Javadoc) */
    @Override protected void showWindow() { }
    /** (non-Javadoc) */
    @Override protected void setWindowIcon(final AppSettings settings) { }

    @Override
    protected int getRenderFramebufferWidth() {
        return Math.max(framebufferWidth, 1);
    }

    @Override
    protected int getRenderFramebufferHeight() {
        return Math.max(framebufferHeight, 1);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void updateSizes() {
        synchronized (lock) {
            int fw = canvas.getWidth();
            int fh = canvas.getHeight();

            if (fw != framebufferWidth || fh != framebufferHeight) {
                framebufferWidth  = Math.max(fw, 1);
                framebufferHeight = Math.max(fh, 1);
                needResize.set(true);
            }
        }
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglContext#printContextInitInfo()
     */
    @Override
    protected void printContextInitInfo() {
        super.printContextInitInfo();
        LOGGER.log(Level.INFO, "Initializing LWJGL3-AWT with jMonkeyEngine\n{0}", getPrintContextInitInfo(glData));
    }

    /**
     * Returns a string with the information obtained from <code>GLData</code>
     * so that it can be displayed.
     *
     * @param glData context information
     * @return String
     */
    protected String getPrintContextInitInfo(GLData glData) {
        StringBuilder sb = new StringBuilder();
            sb.append(" *  Double Buffer: ").append(glData.doubleBuffer);
            sb.append('\n')
              .append(" *  Stereo: ").append(glData.stereo);
            sb.append('\n')
              .append(" *  Red Size: ").append(glData.redSize);
            sb.append('\n')
              .append(" *  Green Size: ").append(glData.greenSize);
            sb.append('\n')
              .append(" *  Blue Size: ").append(glData.blueSize);
            sb.append('\n')
              .append(" *  Alpha Size: ").append(glData.alphaSize);
            sb.append('\n')
              .append(" *  Depth Size: ").append(glData.depthSize);
            sb.append('\n')
              .append(" *  Stencil Size: ").append(glData.stencilSize);
            sb.append('\n')
              .append(" *  Accum Red Size: ").append(glData.accumRedSize);
            sb.append('\n')
              .append(" *  Accum Green Size: ").append(glData.accumGreenSize);
            sb.append('\n')
              .append(" *  Accum Blue Size: ").append(glData.accumBlueSize);
            sb.append('\n')
              .append(" *  Accum Alpha Size: ").append(glData.accumAlphaSize);
            sb.append('\n')
              .append(" *  Sample Buffers: ").append(glData.sampleBuffers);
            sb.append('\n')
              .append(" *  Share Context: ").append(glData.shareContext);
            sb.append('\n')
              .append(" *  Major Version: ").append(glData.majorVersion);
            sb.append('\n')
              .append(" *  Minor Version: ").append(glData.minorVersion);
            sb.append('\n')
              .append(" *  Forward Compatible: ").append(glData.forwardCompatible);
            sb.append('\n')
              .append(" *  Profile: ").append(glData.profile);
            sb.append('\n')
              .append(" *  API: ").append(glData.api);
            sb.append('\n')
              .append(" *  Debug: ").append(glData.debug);
            sb.append('\n')
              .append(" *  Swap Interval: ").append(glData.swapInterval);
            sb.append('\n')
              .append(" *  SRGB (Gamma Correction): ").append(glData.sRGB);
            sb.append('\n')
              .append(" *  Pixel Format Float: ").append(glData.pixelFormatFloat);
            sb.append('\n')
              .append(" *  Context Release Behavior: ").append(glData.contextReleaseBehavior);
            sb.append('\n')
              .append(" *  Color Samples NV: ").append(glData.colorSamplesNV);
            sb.append('\n')
              .append(" *  Swap Group NV: ").append(glData.swapGroupNV);
            sb.append('\n')
              .append(" *  Swap Barrier NV: ").append(glData.swapBarrierNV);
            sb.append('\n')
              .append(" *  Robustness: ").append(glData.robustness);
            sb.append('\n')
              .append(" *  Lose Context On Reset: ").append(glData.loseContextOnReset);
            sb.append('\n')
              .append(" *  Context Reset Isolation: ").append(glData.contextResetIsolation);
        return String.valueOf(sb);
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglWindow#getFramebufferHeight()
     * @return int
     */
    @Override
    public int getFramebufferHeight() {
        return this.framebufferHeight;
    }

    /**
     * (non-Javadoc)
     * @see com.jme3.system.lwjgl.LwjglWindow#getFramebufferWidth()
     * @return int
     */
    @Override
    public int getFramebufferWidth() {
        return this.framebufferWidth;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public long getWindowHandle() {
        return canvas.context;
    }

    /** (non-Javadoc) */
    @Override
    public int getWindowXPosition() {
        Component component = SwingUtilities.getRoot(canvas);
        if (component == null) {
            return 0;
        }
        return component.getX();
    }

    /** (non-Javadoc) */
    @Override
    public int getWindowYPosition() {
        Component component = SwingUtilities.getRoot(canvas);
        if (component == null) {
            return 0;
        }
        return component.getY();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Displays getDisplays() {
        Displays displays = new Displays();
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (environment == null) {
            return displays;
        }

        GraphicsDevice[] devices = environment.getScreenDevices();
        GraphicsDevice defaultgd = environment.getDefaultScreenDevice();
        if (devices == null || defaultgd == null) {
            return displays;
        }

        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice gd = devices[i];
            DisplayMode mode  = gd.getDisplayMode();

            int width  = mode.getWidth();
            int height = mode.getHeight();
            int rate   = mode.getRefreshRate();

            displays.addNewMonitor(i + 1);
            displays.setInfo(i, gd.getIDstring(), width, height, rate);

            if (defaultgd.equals(gd)) {
                displays.setPrimaryDisplay(i);
            }

            LOGGER.log(Level.INFO, "Display id: {0} Resolution: {1} x {2} @ {3}",
                    new Object[]{gd.getIDstring(), width, height, rate});
        }

        return displays;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getPrimaryDisplay() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (environment == null) {
            return 0;
        }

        GraphicsDevice[] devices = environment.getScreenDevices();
        GraphicsDevice defaultgd = environment.getDefaultScreenDevice();
        if (devices == null || defaultgd == null) {
            return 0;
        }

        for (int i = 0; i < devices.length; i++) {
            GraphicsDevice gd = devices[i];
            if (defaultgd.equals(gd)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns the AWT component where it is drawn (canvas).
     * @return Canvas
     */
    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * {@inheritDoc}
     * @param settings AppSettings
     */
    @Override
    public void setSettings(AppSettings settings) {
        if (settings.getRenderer().equals(AppSettings.ANGLE_GLES3)) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("LWJGLX is not compatible with ANGLE/SDL or GLES, as it only supports the following:")
                    .append('\n').append(" * WGL | Windows")
                    .append('\n').append(" * GLX | Linux (X11/XWayland)")
                    .append('\n').append(" * CGL | MacOsX")
                    .append('\n').append(" * Therefore, version ")
                    .append(AppSettings.LWJGL_OPENGL32)
                    .append("(3.2) will be used for the GL context.");
            
            LOGGER.log(Level.WARNING, String.valueOf(buffer));
            settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        }
        super.setSettings(settings);
    }
}
