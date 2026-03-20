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

import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLHints.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLKeyboard.*;
import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLPixels.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.lwjgl.SdlJoystickInput;
import com.jme3.input.lwjgl.SdlKeyInput;
import com.jme3.input.lwjgl.SdlMouseInput;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;
import com.jme3.system.Displays;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.NanoTimer;
import com.jme3.system.NativeLibraries.LibraryInfo;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.system.Platform;
import com.jme3.util.BufferUtils;
import com.jme3.util.SafeArrayList;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.Version;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.sdl.SDLStdinc;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;

/**
 * SDL3-backed window/context implementation for LWJGL 3.4+.
 *
 * @author Daniel Johansson
 * @author Riccardo Balbo
 */
public abstract class LwjglWindow extends LwjglContext implements Runnable {
    private static final LibraryInfo angleEGL = new LibraryInfo("angleEGL")
            .addNativeVariant(Platform.Windows64, "native/angle/windows/x86_64/libEGL.dll", "libEGL.dll")
            .addNativeVariant(Platform.Windows_ARM64, "native/angle/windows/arm64/libEGL.dll", "libEGL.dll")
            .addNativeVariant(Platform.Linux64, "native/angle/linux/x86_64/libEGL.so", "libEGL.so")
            .addNativeVariant(Platform.Linux_ARM64, "native/angle/linux/arm64/libEGL.so", "libEGL.so")
            .addNativeVariant(Platform.MacOSX64, "native/angle/osx/x86_64/libEGL.dylib", "libEGL.dylib")
            .addNativeVariant(Platform.MacOSX_ARM64, "native/angle/osx/arm64/libEGL.dylib", "libEGL.dylib");

    private static final LibraryInfo angleEGLLinuxWayland = new LibraryInfo("angleEGLWayland")
            .addNativeVariant(Platform.Linux64, "native/angle/linux-wayland/x86_64/libEGL.so", "libEGL.so")
            .addNativeVariant(Platform.Linux_ARM64, "native/angle/linux-wayland/arm64/libEGL.so", "libEGL.so");

    private static final LibraryInfo angleGLESv2 = new LibraryInfo("angleGLESv2")
            .addNativeVariant(Platform.Windows64, "native/angle/windows/x86_64/libGLESv2.dll", "libGLESv2.dll")
            .addNativeVariant(Platform.Windows_ARM64, "native/angle/windows/arm64/libGLESv2.dll", "libGLESv2.dll")
            .addNativeVariant(Platform.Linux64, "native/angle/linux/x86_64/libGLESv2.so", "libGLESv2.so")
            .addNativeVariant(Platform.Linux_ARM64, "native/angle/linux/arm64/libGLESv2.so", "libGLESv2.so")
            .addNativeVariant(Platform.MacOSX64, "native/angle/osx/x86_64/libGLESv2.dylib", "libGLESv2.dylib")
            .addNativeVariant(Platform.MacOSX_ARM64, "native/angle/osx/arm64/libGLESv2.dylib", "libGLESv2.dylib");

    private static final LibraryInfo angleGLESv2LinuxWayland = new LibraryInfo("angleGLESv2Wayland")
            .addNativeVariant(Platform.Linux64, "native/angle/linux-wayland/x86_64/libGLESv2.so", "libGLESv2.so")
            .addNativeVariant(Platform.Linux_ARM64, "native/angle/linux-wayland/arm64/libGLESv2.so", "libGLESv2.so");

    private static final LibraryInfo d3dcompiler_47 = new LibraryInfo("d3dcompiler_47")
            .addNativeVariant(Platform.Windows64, "native/d3dcompiler/windows/x86_64/d3dcompiler_47.dll")
            .addNativeVariant(Platform.Windows_ARM64, "native/d3dcompiler/windows/arm64/d3dcompiler_47.dll");

    static {
        NativeLibraryLoader.registerNativeLibrary(angleEGL);
        NativeLibraryLoader.registerNativeLibrary(angleGLESv2);
        NativeLibraryLoader.registerNativeLibrary(angleEGLLinuxWayland);
        NativeLibraryLoader.registerNativeLibrary(angleGLESv2LinuxWayland);
        NativeLibraryLoader.registerNativeLibrary(d3dcompiler_47);
    }

    private static final Logger LOGGER = Logger.getLogger(LwjglWindow.class.getName());
    private static final int SDL_WINDOW_SUBSYSTEM_FLAGS = SDL_INIT_VIDEO | SDL_INIT_EVENTS;

    private static final EnumSet<JmeContext.Type> SUPPORTED_TYPES = EnumSet.of(
            JmeContext.Type.Display,
            JmeContext.Type.Canvas,
            JmeContext.Type.OffscreenSurface
    );

    private static final Map<String, Runnable> RENDER_CONFIGS = new HashMap<>();

    static {
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL32, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 2);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL33, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL40, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 0);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL41, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 1);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL42, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 2);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL43, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL44, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 4);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.LWJGL_OPENGL45, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 5);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
        });
        RENDER_CONFIGS.put(AppSettings.ANGLE_GLES3, () -> {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 0);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES);
        });
    }

    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected final AtomicBoolean needRestart = new AtomicBoolean(false);

    private final JmeContext.Type type;
    private final SafeArrayList<WindowSizeListener> windowSizeListeners = new SafeArrayList<>(WindowSizeListener.class);
    private final AtomicBoolean windowCloseRequested = new AtomicBoolean(false);

    private Thread mainThread;

    private long display = 0L;
    private long window = NULL;
    private long glContext = NULL;
    private int windowId;
    private int frameRateLimit = -1;

    protected boolean wasActive = false;
    protected boolean autoFlush = true;
    protected boolean allowSwapBuffers = false;

    // state maintained by updateSizes()
    private int oldFramebufferWidth;
    private int oldFramebufferHeight;
    private final Vector2f oldScale = new Vector2f(1, 1);

    public LwjglWindow(final JmeContext.Type type) {
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported type '" + type.name() + "' provided");
        }
        this.type = type;
    }

    public void registerWindowSizeListener(WindowSizeListener listener) {
        windowSizeListeners.add(listener);
    }

    public void removeWindowSizeListener(WindowSizeListener listener) {
        windowSizeListeners.remove(listener);
    }

    @Override
    public JmeContext.Type getType() {
        return type;
    }

    @Override
    public void setTitle(final String title) {
        if (created.get() && window != NULL) {
            SDL_SetWindowTitle(window, title);
        }
    }

    @Override
    public void restart() {
        if (created.get()) {
            needRestart.set(true);
        } else {
            LOGGER.warning("Display is not created, cannot restart window.");
        }
    }

    protected void createContext(final AppSettings settings) {
        configureVideoDriverHints(settings);
        configureAngleHints(settings);

        if (!SDL_InitSubSystem(SDL_WINDOW_SUBSYSTEM_FLAGS)) {
            throw new IllegalStateException("Unable to initialize SDL video subsystem: " + SDL_GetError());
        }

        SDL_SetHint(SDL_HINT_QUIT_ON_LAST_WINDOW_CLOSE, "0");
        SDL_GL_ResetAttributes();
        configureGLAttributes(settings);

        display = settings.isFullscreen() ? getDisplay(settings.getDisplay()) : SDL_GetPrimaryDisplay();
        SDL_DisplayMode videoMode = SDL_GetCurrentDisplayMode((int) display);

        int requestWidth = settings.getWindowWidth();
        int requestHeight = settings.getWindowHeight();
        if (requestWidth <= 0 || requestHeight <= 0) {
            if (videoMode != null) {
                requestWidth = videoMode.w();
                requestHeight = videoMode.h();
            } else {
                requestWidth = 1280;
                requestHeight = 720;
            }
        }

        int requestX = SDL_WINDOWPOS_UNDEFINED_DISPLAY((int) display);
        int requestY = SDL_WINDOWPOS_UNDEFINED_DISPLAY((int) display);
        if (!settings.isFullscreen()) {
            if (settings.getCenterWindow()) {
                requestX = SDL_WINDOWPOS_CENTERED_DISPLAY((int) display);
                requestY = SDL_WINDOWPOS_CENTERED_DISPLAY((int) display);
            } else {
                requestX = settings.getWindowXPosition();
                requestY = settings.getWindowYPosition();
            }
        }

        long windowFlags = SDL_WINDOW_OPENGL | SDL_WINDOW_HIDDEN;
        if (settings.isResizable()) {
            windowFlags |= SDL_WINDOW_RESIZABLE;
        }
        if (settings.isUseRetinaFrameBuffer()) {
            windowFlags |= SDL_WINDOW_HIGH_PIXEL_DENSITY;
        }
        if (settings.isFullscreen()) {
            windowFlags |= SDL_WINDOW_FULLSCREEN;
        }

        window = SDL_CreateWindow(settings.getTitle(), requestWidth, requestHeight, windowFlags);
        if (window == NULL) {
            throw new RuntimeException("Failed to create SDL window: " + SDL_GetError());
        }

        windowId = SDL_GetWindowID(window);
        windowCloseRequested.set(false);

        if (!settings.isFullscreen()) {
            SDL_SetWindowPosition(window, requestX, requestY);
        }

        glContext = SDL_GL_CreateContext(window);
        if (glContext == NULL) {
            throw new RuntimeException("Failed to create SDL GL context: " + SDL_GetError());
        }

        if (!SDL_GL_MakeCurrent(window, glContext)) {
            throw new RuntimeException("Failed to make SDL GL context current: " + SDL_GetError());
        }

        if (!SDL_GL_SetSwapInterval(settings.isVSync() ? 1 : 0)) {
            LOGGER.log(Level.WARNING, "Unable to set SDL swap interval: {0}", SDL_GetError());
        }

        setWindowIcon(settings);
        showWindow();

        // Some platforms report wrong initial scale until a couple of polls happen.
        for (int i = 0; i < 4; i++) {
            pollEvents(false);
        }

        allowSwapBuffers = settings.isSwapBuffers();

        updateSizes();
    }

    private void configureVideoDriverHints(AppSettings settings) {
        if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.LINUX) {
            boolean isWaylandSession = "wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"));
            if (settings.isX11PlatformPreferred()) {
                SDL_SetHint(SDL_HINT_VIDEO_DRIVER, "x11");
            } else if (isWaylandSession) {
                SDL_SetHint(SDL_HINT_VIDEO_DRIVER, "wayland");
            }
        }

        if (settings.isFullscreen()) {
            SDL_SetHint(SDL_HINT_VIDEO_WAYLAND_ALLOW_LIBDECOR, "0");
        } else {
            SDL_SetHint(SDL_HINT_VIDEO_WAYLAND_PREFER_LIBDECOR, "1");
        }
    }

    private void configureAngleHints(AppSettings settings) {
        if (!useAngle) {
            return;
        }

        boolean isWayland = !settings.isX11PlatformPreferred()
                && "wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"));

        String angleEGLPath = null;
        String angleGLESv2Path = null;
        
        if(isWayland) {
            // angleEGLPath = NativeLibraryLoader.loadNativeLibrary("angleEGLWayland", true);
            // angleGLESv2Path =  NativeLibraryLoader.loadNativeLibrary("angleGLESv2Wayland", true);

            // force debug paths
            angleEGLPath = "/home/riccardobl/Desktop/angle-build/dist/angle-natives-local-dev/native/angle/linux-wayland/x86_64/libEGL.so";
            angleGLESv2Path = "/home/riccardobl/Desktop/angle-build/dist/angle-natives-local-dev/native/angle/linux-wayland/x86_64/libGLESv2.so";
        } else {
            // angleEGLPath = NativeLibraryLoader.loadNativeLibrary("angleEGL", true);
            // angleGLESv2Path = NativeLibraryLoader.loadNativeLibrary("angleGLESv2", true);

            // force debug paths
            angleEGLPath = "/home/riccardobl/Desktop/angle-build/dist/angle-natives-local-dev/native/angle/linux/x86_64/libEGL.so";
            angleGLESv2Path = "/home/riccardobl/Desktop/angle-build/dist/angle-natives-local-dev/native/angle/linux/x86_64/libGLESv2.so";
        }
    

        NativeLibraryLoader.loadNativeLibrary("d3dcompiler_47", false); // windows only

        Configuration.OPENGLES_LIBRARY_NAME.set(angleGLESv2Path);
        Configuration.EGL_LIBRARY_NAME.set(angleEGLPath);

        SDL_SetHint(SDL_HINT_EGL_LIBRARY, angleEGLPath);
        SDL_SetHint(SDL_HINT_OPENGL_LIBRARY, angleGLESv2Path);
        SDL_SetHint(SDL_HINT_OPENGL_ES_DRIVER, angleGLESv2Path);
        SDL_SetHint(SDL_HINT_VIDEO_FORCE_EGL, "1");
    }

    private void configureGLAttributes(AppSettings settings) {
        final String renderer = settings.getRenderer();
        final boolean glesContext = AppSettings.ANGLE_GLES3.equals(renderer);
        RENDER_CONFIGS.getOrDefault(renderer, RENDER_CONFIGS.get(AppSettings.LWJGL_OPENGL32)).run();

        int contextFlags = 0;
        if (settings.getBoolean("RendererDebug")) {
            contextFlags |= SDL_GL_CONTEXT_DEBUG_FLAG;
        }
        if (!glesContext) {
            contextFlags |= SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG;
        }
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, contextFlags);

        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, settings.getDepthBits());
        SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, settings.getStencilBits());
        SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, settings.getAlphaBits());
        SDL_GL_SetAttribute(SDL_GL_MULTISAMPLEBUFFERS, settings.getSamples() > 0 ? 1 : 0);
        SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, Math.max(settings.getSamples(), 0));

        if (settings.isGammaCorrection()) {
            SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 1);
        }

        if (settings.getBitsPerPixel() == 24) {
            SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
            SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
            SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
        } else if (settings.getBitsPerPixel() == 16) {
            SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 5);
            SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 6);
            SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 5);
        }
    }

    private void updateSizes() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer winW = stack.mallocInt(1);
            IntBuffer winH = stack.mallocInt(1);
            if (!SDL_GetWindowSize(window, winW, winH)) {
                return;
            }

            int windowWidth = Math.max(winW.get(0), 16);
            int windowHeight = Math.max(winH.get(0), 16);
            if (settings.getWindowWidth() != windowWidth || settings.getWindowHeight() != windowHeight) {
                settings.setWindowSize(windowWidth, windowHeight);
                for (WindowSizeListener wsListener : windowSizeListeners.getArray()) {
                    wsListener.onWindowSizeChanged(windowWidth, windowHeight);
                }
            }

            IntBuffer fbW = stack.mallocInt(1);
            IntBuffer fbH = stack.mallocInt(1);
            if (!SDL_GetWindowSizeInPixels(window, fbW, fbH)) {
                return;
            }

            int framebufferWidth = Math.max(fbW.get(0), 16);
            int framebufferHeight = Math.max(fbH.get(0), 16);
            if (framebufferWidth != oldFramebufferWidth || framebufferHeight != oldFramebufferHeight) {
                settings.setResolution(framebufferWidth, framebufferHeight);
                listener.reshape(framebufferWidth, framebufferHeight);
                oldFramebufferWidth = framebufferWidth;
                oldFramebufferHeight = framebufferHeight;
            }

            float xScale = (float) framebufferWidth / windowWidth;
            float yScale = (float) framebufferHeight / windowHeight;
            if (oldScale.x != xScale || oldScale.y != yScale) {
                listener.rescale(xScale, yScale);
                oldScale.set(xScale, yScale);
            }
        }
    }

    protected void showWindow() {
        SDL_ShowWindow(window);
    }

    protected void setWindowIcon(final AppSettings settings) {
        final Object[] icons = settings.getIcons();
        if (icons == null || icons.length == 0) {
            return;
        }

        String driver = SDL_GetCurrentVideoDriver();
        if ("wayland".equalsIgnoreCase(driver)) {
            // Wayland compositors generally ignore custom icons.
            return;
        }

        BufferedImage image = (BufferedImage) icons[0];
        SDL_Surface surface = imageToSdlSurface(image);
        if (surface != null) {
            SDL_SetWindowIcon(window, surface);
            SDL_DestroySurface(surface);
        }
    }

    private SDL_Surface imageToSdlSurface(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {
            BufferedImage convertedImage = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_ARGB_PRE
            );
            Graphics2D graphics = convertedImage.createGraphics();
            graphics.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
            graphics.dispose();
            image = convertedImage;
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);
                buffer.put((byte) ((color >> 16) & 0xFF));
                buffer.put((byte) ((color >> 8) & 0xFF));
                buffer.put((byte) (color & 0xFF));
                buffer.put((byte) ((color >> 24) & 0xFF));
            }
        }
        buffer.flip();
        return SDL_CreateSurfaceFrom(image.getWidth(), image.getHeight(), SDL_PIXELFORMAT_RGBA32, buffer,
                image.getWidth() * 4);
    }

    protected void destroyContext() {
        try {
            if (renderer != null) {
                renderer.cleanup();
            }

            if (glContext != NULL) {
                SDL_GL_DestroyContext(glContext);
                glContext = NULL;
            }

            if (window != NULL) {
                SDL_DestroyWindow(window);
                window = NULL;
                windowId = 0;
            }
        } catch (Exception ex) {
            listener.handleError("Failed to destroy context", ex);
        }
    }

    @Override
    public void create(boolean waitFor) {
        if (created.get()) {
            LOGGER.warning("create() called when display is already created!");
            return;
        }

        if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.MACOSX) {
            mainThread = Thread.currentThread();
            mainThread.setName("jME3 Main");
            if (waitFor) {
                LOGGER.warning("create(true) is not supported for macOS!");
            }
            run();
        } else {
            mainThread = new Thread(this, "jME3 Main");
            mainThread.start();
            if (waitFor) {
                waitFor(true);
            }
        }
    }

    protected boolean initInThread() {
        try {
            if (!JmeSystem.isLowPermissions()) {
                Thread.currentThread().setUncaughtExceptionHandler((thread, thrown) -> {
                    listener.handleError("Uncaught exception thrown in " + thread, thrown);
                    if (needClose.get()) {
                        deinitInThread();
                    }
                });
            }

            timer = new NanoTimer();
            createContext(settings);
            printContextInitInfo();

            created.set(true);
            super.internalCreate();
        } catch (Exception ex) {
            try {
                if (window != NULL) {
                    SDL_DestroyWindow(window);
                    window = NULL;
                }
            } catch (Exception ex2) {
                LOGGER.log(Level.WARNING, null, ex2);
            }
            listener.handleError("Failed to create display", ex);
            return false;
        }

        listener.initialize();
        updateSizes();
        return true;
    }

    protected void runLoop() {
        if (needRestart.getAndSet(false)) {
            restartContext();
        }

        if (!created.get()) {
            throw new IllegalStateException();
        }

        listener.update();

        if (renderable.get()) {
            try {
                if ((type != Type.Canvas) && allowSwapBuffers && autoFlush) {
                    SDL_GL_SwapWindow(window);
                }
            } catch (Throwable ex) {
                listener.handleError("Error while swapping buffers", ex);
            }
        }

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
        pollEvents(true);
    }

    private void pollEvents(boolean dispatchToInputs) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Event event = SDL_Event.malloc(stack);
            while (SDL_PollEvent(event)) {
                handleWindowEvent(event);
                if (dispatchToInputs) {
                    dispatchSDLEvent(event);
                }
            }
        }
    }

    private void handleWindowEvent(SDL_Event event) {
        int type = event.type();
        if (type == SDL_EVENT_QUIT) {
            windowCloseRequested.set(true);
            return;
        }

        if (type < SDL_EVENT_WINDOW_FIRST || type > SDL_EVENT_WINDOW_LAST) {
            return;
        }

        if (event.window().windowID() != windowId) {
            return;
        }

        switch (type) {
            case SDL_EVENT_WINDOW_FOCUS_GAINED:
                if (!wasActive) {
                    listener.gainFocus();
                    timer.reset();
                    wasActive = true;
                }
                break;
            case SDL_EVENT_WINDOW_FOCUS_LOST:
                if (wasActive) {
                    listener.loseFocus();
                    wasActive = false;
                }
                break;
            case SDL_EVENT_WINDOW_CLOSE_REQUESTED:
            case SDL_EVENT_WINDOW_DESTROYED:
                windowCloseRequested.set(true);
                break;
            case SDL_EVENT_WINDOW_RESIZED:
            case SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED:
            case SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED:
                updateSizes();
                break;
            default:
                break;
        }
    }

    private void dispatchSDLEvent(SDL_Event event) {
        if (keyInput instanceof SdlKeyInput) {
            ((SdlKeyInput) keyInput).onSDLEvent(event);
        }
        if (mouseInput instanceof SdlMouseInput) {
            ((SdlMouseInput) mouseInput).onSDLEvent(event);
        }
        if (joyInput instanceof SdlJoystickInput) {
            ((SdlJoystickInput) joyInput).onSDLEvent(event);
        }
    }

    private void restartContext() {
        try {
            destroyContext();
            createContext(settings);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to set display settings!", ex);
        }

        reinitContext();

        if (keyInput != null && keyInput.isInitialized() && keyInput instanceof SdlKeyInput) {
            ((SdlKeyInput) keyInput).resetContext();
        }
        if (mouseInput != null && mouseInput.isInitialized() && mouseInput instanceof SdlMouseInput) {
            ((SdlMouseInput) mouseInput).resetContext();
        }

        LOGGER.fine("Display restarted.");
    }

    private void setFrameRateLimit(int frameRateLimit) {
        this.frameRateLimit = frameRateLimit;
    }

    protected void deinitInThread() {
        listener.destroy();
        destroyContext();
        super.internalDestroy();

        if ((SDL_WasInit(SDL_WINDOW_SUBSYSTEM_FLAGS) & SDL_WINDOW_SUBSYSTEM_FLAGS) != 0) {
            SDL_QuitSubSystem(SDL_WINDOW_SUBSYSTEM_FLAGS);
        }

        LOGGER.fine("Display destroyed.");
    }

    @Override
    public void run() {
        if (listener == null) {
            throw new IllegalStateException(
                    "SystemListener is not set on context!Must set with JmeContext.setSystemListener().");
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

            if (windowCloseRequested.get()) {
                listener.requestClose(false);
            }
        }

        deinitInThread();
    }

    @Override
    public JoyInput getJoyInput() {
        if (joyInput == null) {
            joyInput = new SdlJoystickInput(settings);
        }
        return joyInput;
    }

    @Override
    public MouseInput getMouseInput() {
        if (mouseInput == null) {
            mouseInput = new SdlMouseInput(this);
        }
        return mouseInput;
    }

    @Override
    public KeyInput getKeyInput() {
        if (keyInput == null) {
            keyInput = new SdlKeyInput(this);
        }
        return keyInput;
    }

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        this.autoFlush = enabled;
    }

    @Override
    public void destroy(boolean waitFor) {
        needClose.set(true);

        if (mainThread == Thread.currentThread()) {
            return;
        }

        if (waitFor) {
            waitFor(false);
        }
    }

    public long getWindowHandle() {
        return window;
    }

    public int getWindowId() {
        return windowId;
    }

    public Vector2f getWindowContentScale(Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fbW = stack.mallocInt(1);
            IntBuffer fbH = stack.mallocInt(1);
            IntBuffer winW = stack.mallocInt(1);
            IntBuffer winH = stack.mallocInt(1);

            SDL_GetWindowSizeInPixels(window, fbW, fbH);
            SDL_GetWindowSize(window, winW, winH);

            float wx = Math.max(winW.get(0), 1);
            float wy = Math.max(winH.get(0), 1);
            store.set(fbW.get(0) / wx, fbH.get(0) / wy);
        }

        return store;
    }

    @Override
    public int getFramebufferHeight() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            if (!SDL_GetWindowSizeInPixels(window, w, h)) {
                return 0;
            }
            return h.get(0);
        }
    }

    @Override
    public int getFramebufferWidth() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            if (!SDL_GetWindowSizeInPixels(window, w, h)) {
                return 0;
            }
            return w.get(0);
        }
    }

    @Override
    public int getWindowXPosition() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            if (!SDL_GetWindowPosition(window, x, y)) {
                return 0;
            }
            return x.get(0);
        }
    }

    @Override
    public int getWindowYPosition() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x = stack.mallocInt(1);
            IntBuffer y = stack.mallocInt(1);
            if (!SDL_GetWindowPosition(window, x, y)) {
                return 0;
            }
            return y.get(0);
        }
    }

    @Override
    public int getPrimaryDisplay() {
        int primary = SDL_GetPrimaryDisplay();
        Displays monitors = getDisplays();
        for (int i = 0; i < monitors.size(); i++) {
            if ((int) monitors.get(i).getDisplay() == primary) {
                return i;
            }
        }

        LOGGER.log(Level.SEVERE, "Couldn't locate Primary Display in the list of displays.");
        return -1;
    }

    private long getDisplay(int pos) {
        Displays displays = getDisplays();
        if (pos < displays.size()) {
            return displays.get(pos).getDisplay();
        }

        LOGGER.log(Level.SEVERE, "Couldn't locate Display requested in the list of Displays. pos: {0} size: {1}",
                new Object[]{pos, displays.size()});
        return SDL_GetPrimaryDisplay();
    }

    @Override
    public Displays getDisplays() {
        Displays displayList = new Displays();
        IntBuffer displays = SDL_GetDisplays();
        if (displays == null) {
            return displayList;
        }

        int primary = SDL_GetPrimaryDisplay();
        try {
            for (int i = 0; i < displays.limit(); i++) {
                int displayId = displays.get(i);
                int monPos = displayList.addNewMonitor(displayId);
                if (primary == displayId) {
                    displayList.setPrimaryDisplay(monPos);
                }

                SDL_DisplayMode mode = SDL_GetCurrentDisplayMode(displayId);
                String name = SDL_GetDisplayName(displayId);
                if (name == null || name.trim().isEmpty()) {
                    name = "Display " + i;
                }

                int width = mode != null ? mode.w() : 0;
                int height = mode != null ? mode.h() : 0;
                int rate = mode != null ? Math.round(mode.refresh_rate()) : 0;
                displayList.setInfo(monPos, name, width, height, rate);

                LOGGER.log(Level.INFO, "Display id: {0} Resolution: {1} x {2} @ {3}",
                        new Object[]{displayId, width, height, rate});
            }
        } finally {
            SDLStdinc.SDL_free(displays);
        }

        return displayList;
    }
}
