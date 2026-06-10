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

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.lwjgl.SdlEventListener;
import com.jme3.input.lwjgl.SdlJoystickInput;
import com.jme3.input.lwjgl.SdlKeyInput;
import com.jme3.input.lwjgl.SdlMouseInput;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.Displays;
import com.jme3.system.DisplayScaleUtils;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.NanoTimer;
import com.jme3.system.NativeLibraries.LibraryInfo;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.system.Platform;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
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
import org.lwjgl.sdl.SDL;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.sdl.SDLStdinc;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.egl.EXTPlatformWayland.EGL_PLATFORM_WAYLAND_EXT;
import static org.lwjgl.egl.EXTPlatformX11.EGL_PLATFORM_X11_EXT;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLHints.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLPixels.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLStdinc.SDL_setenv_unsafe;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;
import static org.lwjgl.system.JNI.invokePPZ;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * SDL3-backed window/context implementation for LWJGL 3.4+.
 *
 * @author Daniel Johansson
 * @author Riccardo Balbo
 */
public abstract class LwjglWindow extends LwjglContext implements Runnable {
    private static final String BLIT_MATERIAL = "Common/MatDefs/Blit/Blit.j3md";
    private static final long SDL_SET_WINDOW_FULLSCREEN_MODE = apiGetFunctionAddress(
            SDL.getLibrary(), "SDL_SetWindowFullscreenMode");
    private static final LibraryInfo angleEGL = new LibraryInfo("angleEGL")
            .addNativeVariant(Platform.Windows64, "native/angle/windows/x86_64/libEGL.dll", "libEGL.dll")
            .addNativeVariant(Platform.Windows_ARM64, "native/angle/windows/arm64/libEGL.dll", "libEGL.dll")
            .addNativeVariant(Platform.Linux64, "native/angle/linux/x86_64/libEGL.so", "libEGL.so")
            .addNativeVariant(Platform.Linux_ARM64, "native/angle/linux/arm64/libEGL.so", "libEGL.so")
            .addNativeVariant(Platform.MacOSX64, "native/angle/osx/x86_64/libEGL.dylib", "libEGL.dylib")
            .addNativeVariant(Platform.MacOSX_ARM64, "native/angle/osx/arm64/libEGL.dylib", "libEGL.dylib");


    private static final LibraryInfo angleGLESv2 = new LibraryInfo("angleGLESv2")
            .addNativeVariant(Platform.Windows64, "native/angle/windows/x86_64/libGLESv2.dll", "libGLESv2.dll")
            .addNativeVariant(Platform.Windows_ARM64, "native/angle/windows/arm64/libGLESv2.dll", "libGLESv2.dll")
            .addNativeVariant(Platform.Linux64, "native/angle/linux/x86_64/libGLESv2.so", "libGLESv2.so")
            .addNativeVariant(Platform.Linux_ARM64, "native/angle/linux/arm64/libGLESv2.so", "libGLESv2.so")
            .addNativeVariant(Platform.MacOSX64, "native/angle/osx/x86_64/libGLESv2.dylib", "libGLESv2.dylib")
            .addNativeVariant(Platform.MacOSX_ARM64, "native/angle/osx/arm64/libGLESv2.dylib", "libGLESv2.dylib");


    private static final LibraryInfo d3dcompiler_47 = new LibraryInfo("d3dcompiler_47")
            .addNativeVariant(Platform.Windows64, "native/d3dcompiler/windows/x86_64/d3dcompiler_47.dll")
            .addNativeVariant(Platform.Windows_ARM64, "native/d3dcompiler/windows/arm64/d3dcompiler_47.dll");

    static {
        disableNvidiaThreadedOptimizations();
        NativeLibraryLoader.registerNativeLibrary(angleEGL);
        NativeLibraryLoader.registerNativeLibrary(angleGLESv2);
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
    private final SafeArrayList<SdlEventListener> sdlEventListeners = new SafeArrayList<>(SdlEventListener.class);
    private final AtomicBoolean windowCloseRequested = new AtomicBoolean(false);

    private Thread mainThread;

    private long display = 0L;
    private long window = NULL;
    private long glContext = NULL;
    private int windowId;
    protected int frameRateLimit = -1;

    protected boolean wasActive = false;
    protected boolean autoFlush = true;
    protected boolean allowSwapBuffers = false;

    // state maintained by updateSizes()
    private int logicalWidth = 1;
    private int logicalHeight = 1;
    private int windowWidth = 1;
    private int windowHeight = 1;
    private int framebufferWidth = 1;
    private int framebufferHeight = 1;
    private int oldFramebufferWidth;
    private int oldFramebufferHeight;
    private int oldLogicalWidth;
    private int oldLogicalHeight;
    private final Vector2f displayScale = new Vector2f(1, 1);
    private Material blitMaterial;
    private Picture blitGeometry;
    private final Camera blitCamera = new Camera(1, 1);
    private FrameBuffer blitFramebuffer;
    private Texture2D blitColorTexture;
    private boolean blitFramebufferDirty;
    private boolean blitFramebufferTextureMultisampleWarningIssued;
    private boolean windowStateChangedSinceLastSwap;
    private boolean windowSizeUpdatePending;

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

    public void registerSdlEventListener(SdlEventListener listener) {
        sdlEventListeners.add(listener);
    }

    public void removeSdlEventListener(SdlEventListener listener) {
        sdlEventListeners.remove(listener);
    }

    private static void disableNvidiaThreadedOptimizations() {
        if (SDL_setenv_unsafe("__GL_THREADED_OPTIMIZATIONS", "0", 1) != 0) {
            throw new IllegalStateException("Unable to disable NVIDIA OpenGL threaded optimizations: "
                    + SDL_GetError());
        }
    }

    @Override
    protected String getCurrentVideoDriver() {
        return "SDL " + SDL_GetCurrentVideoDriver();
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
        disableNvidiaThreadedOptimizations();
        useAngle = AppSettings.ANGLE_GLES3.equals(settings.getRenderer());
        configureVideoDriverHints(settings);
        configureOpenGLDriverHints(settings);

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

        int requestX;
        int requestY;
        if (settings.isFullscreen()) {
            int[] displayOrigin = getDisplayOrigin();
            requestX = displayOrigin[0];
            requestY = displayOrigin[1];
        } else {
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
        if (DisplayScaleUtils.requestsHighDensityFramebuffer(settings.getDisplayScaleMode())) {
            windowFlags |= SDL_WINDOW_HIGH_PIXEL_DENSITY;
        }
        window = SDL_CreateWindow(settings.getTitle(), requestWidth, requestHeight, windowFlags);
        if (window == NULL) {
            throw new RuntimeException("Failed to create SDL window: " + SDL_GetError());
        }

        windowId = SDL_GetWindowID(window);
        windowCloseRequested.set(false);
        SDL_SetWindowPosition(window, requestX, requestY);
        applyFullscreenMode(settings, requestWidth, requestHeight);

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

        updateSizes(false);
    }

    private void applyFullscreenMode(AppSettings settings, int width, int height) {
        if (!settings.isFullscreen()) {
            return;
        }

        if (AppSettings.FULLSCREEN_MODE_EXCLUSIVE_FULLSCREEN.equals(settings.getFullscreenMode())) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                SDL_DisplayMode fullscreenMode = SDL_DisplayMode.malloc(stack);
                float refreshRate = settings.getFrequency() > 0 ? settings.getFrequency() : 0f;
                boolean includeHighDensityModes = DisplayScaleUtils.requestsHighDensityFramebuffer(
                        settings.getDisplayScaleMode());
                if (!SDL_GetClosestFullscreenDisplayMode((int) display, width, height, refreshRate,
                        includeHighDensityModes, fullscreenMode)) {
                    throw new RuntimeException("Unable to find an SDL exclusive fullscreen mode for "
                            + width + "x" + height + "@" + refreshRate + "Hz: " + SDL_GetError());
                }
                if (!setWindowFullscreenMode(window, fullscreenMode.address())) {
                    throw new RuntimeException("Unable to set SDL exclusive fullscreen mode "
                            + fullscreenMode.w() + "x" + fullscreenMode.h() + "@"
                            + fullscreenMode.refresh_rate() + "Hz: " + SDL_GetError());
                }
            }
        } else if (!setWindowFullscreenMode(window, NULL)) {
            throw new RuntimeException("Unable to set SDL borderless fullscreen mode: " + SDL_GetError());
        }

        if (!SDL_SetWindowFullscreen(window, true)) {
            throw new RuntimeException("Unable to enter SDL fullscreen mode: " + SDL_GetError());
        }

        int actualDisplay = SDL_GetDisplayForWindow(window);
        LOGGER.log(Level.INFO, "Entered SDL {0} fullscreen on display {1} (requested display {2})",
                new Object[] {
                        AppSettings.FULLSCREEN_MODE_EXCLUSIVE_FULLSCREEN.equals(settings.getFullscreenMode())
                                ? "exclusive" : "borderless",
                        actualDisplay,
                        display
                });
    }

    private boolean setWindowFullscreenMode(long window, long mode) {
        return invokePPZ(window, mode, SDL_SET_WINDOW_FULLSCREEN_MODE);
    }

    private int[] getDisplayOrigin() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_Rect bounds = SDL_Rect.malloc(stack);
            if (SDL_GetDisplayBounds((int) display, bounds)) {
                return new int[] { bounds.x(), bounds.y() };
            }
        }

        LOGGER.log(Level.WARNING, "Unable to query SDL display bounds for display {0}: {1}",
                new Object[] { display, SDL_GetError() });
        return new int[] {
                SDL_WINDOWPOS_UNDEFINED_DISPLAY((int) display),
                SDL_WINDOWPOS_UNDEFINED_DISPLAY((int) display)
        };
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

    private void configureOpenGLDriverHints(AppSettings settings) {
        final boolean angleGles = AppSettings.ANGLE_GLES3.equals(settings.getRenderer());
        if (!angleGles) {
            resetAngleLibraries();
            if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.LINUX && !settings.isX11PlatformPreferred()) {
                SDL_SetHint(SDL_HINT_VIDEO_FORCE_EGL, "1");
            } else {
                SDL_ResetHint(SDL_HINT_VIDEO_FORCE_EGL);
            }
            return;
        }

        String angleEGLPath = null;
        String angleGLESv2Path = null;

        angleEGLPath = NativeLibraryLoader.loadNativeLibrary("angleEGL", true);
        angleGLESv2Path = NativeLibraryLoader.loadNativeLibrary("angleGLESv2", true);

        NativeLibraryLoader.loadNativeLibrary("d3dcompiler_47", false); // windows only

        Configuration.OPENGLES_LIBRARY_NAME.set(angleGLESv2Path);
        Configuration.EGL_LIBRARY_NAME.set(angleEGLPath);

        SDL_SetHint(SDL_HINT_EGL_LIBRARY, angleEGLPath);
        SDL_SetHint(SDL_HINT_OPENGL_LIBRARY, angleGLESv2Path);
        SDL_SetHint(SDL_HINT_OPENGL_ES_DRIVER, "1");
        SDL_SetHint(SDL_HINT_VIDEO_FORCE_EGL, "1");
    }

    private void resetAngleLibraries() {
        SDL_ResetHint(SDL_HINT_EGL_LIBRARY);
        SDL_ResetHint(SDL_HINT_OPENGL_LIBRARY);
        SDL_ResetHint(SDL_HINT_OPENGL_ES_DRIVER);
        Configuration.OPENGLES_LIBRARY_NAME.set(null);
        Configuration.EGL_LIBRARY_NAME.set(null);
    }

    private void configureGLAttributes(AppSettings settings) {
        final String renderer = settings.getRenderer();
        final boolean glesContext = AppSettings.ANGLE_GLES3.equals(renderer);
        RENDER_CONFIGS.getOrDefault(renderer, RENDER_CONFIGS.get(AppSettings.LWJGL_OPENGL32)).run();

        if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.LINUX) {
            if (settings.isX11PlatformPreferred()) {
                SDL_GL_SetAttribute(SDL_GL_EGL_PLATFORM, EGL_PLATFORM_X11_EXT);
            } else if ("wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"))) {
                SDL_GL_SetAttribute(SDL_GL_EGL_PLATFORM, EGL_PLATFORM_WAYLAND_EXT);
            }
        }

        int contextFlags = 0;
        if (settings.isGraphicsDebug()) {
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

    protected void updateSizes() {
        updateSizes(true);
    }

    private void updateSizes(boolean notifyListener) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer winW = stack.mallocInt(1);
            IntBuffer winH = stack.mallocInt(1);
            if (!SDL_GetWindowSize(window, winW, winH)) {
                return;
            }

            windowWidth = Math.max(winW.get(0), 16);
            windowHeight = Math.max(winH.get(0), 16);
            if (settings.getWindowWidth() != windowWidth || settings.getWindowHeight() != windowHeight) {
                settings.setWindowSize(windowWidth, windowHeight);
                if (notifyListener) {
                    for (WindowSizeListener wsListener : windowSizeListeners.getArray()) {
                        wsListener.onWindowSizeChanged(windowWidth, windowHeight);
                    }
                }
            }

            IntBuffer fbW = stack.mallocInt(1);
            IntBuffer fbH = stack.mallocInt(1);
            if (!SDL_GetWindowSizeInPixels(window, fbW, fbH)) {
                return;
            }

            framebufferWidth = Math.max(fbW.get(0), 16);
            framebufferHeight = Math.max(fbH.get(0), 16);
            updateScaleState(windowWidth, windowHeight, framebufferWidth, framebufferHeight);

            float mode = settings.getDisplayScaleMode();
            int[] logicalSize = DisplayScaleUtils.resolveLogicalSize(mode, windowWidth, windowHeight,
                    framebufferWidth, framebufferHeight, displayScale.x, displayScale.y);
            logicalWidth = logicalSize[0];
            logicalHeight = logicalSize[1];

            if (!notifyListener) {
                settings.setResolution(logicalWidth, logicalHeight);
                return;
            }

            if (logicalWidth != oldLogicalWidth || logicalHeight != oldLogicalHeight
                    || framebufferWidth != oldFramebufferWidth || framebufferHeight != oldFramebufferHeight) {
                settings.setResolution(logicalWidth, logicalHeight);
                listener.reshape(logicalWidth, logicalHeight, getRenderFramebufferWidth(), getRenderFramebufferHeight());
                oldLogicalWidth = logicalWidth;
                oldLogicalHeight = logicalHeight;
                oldFramebufferWidth = framebufferWidth;
                oldFramebufferHeight = framebufferHeight;
            }

        }
    }

    private void updateScaleState(int windowWidth, int windowHeight, int framebufferWidth, int framebufferHeight) {
        float density = DisplayScaleUtils.sanitizeScale(SDL_GetWindowPixelDensity(window));
        float scale = SDL_GetWindowDisplayScale(window);
        if (!Float.isFinite(scale) || scale <= 0f) {
            scale = density;
        }
        scale = DisplayScaleUtils.sanitizeScale(scale);
        displayScale.set(scale, scale);
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
        Exception failure = null;
        try {
            destroyBlitFramebufferResources();
            if (renderer != null) {
                renderer.cleanup();
            }
        } catch (Exception ex) {
            failure = ex;
        }

        try {
            if (glContext != NULL) {
                SDL_GL_DestroyContext(glContext);
                glContext = NULL;
            }

            if (window != NULL) {
                SDL_DestroyWindow(window);
                window = NULL;
                windowId = 0;
            }
            oldFramebufferWidth = 0;
            oldFramebufferHeight = 0;
            oldLogicalWidth = 0;
            oldLogicalHeight = 0;
        } catch (Exception ex) {
            if (failure == null) {
                failure = ex;
            } else {
                failure.addSuppressed(ex);
            }
        }

        if (failure != null) {
            listener.handleError("Failed to destroy context", failure);
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
                destroyContext();
                if ((SDL_WasInit(SDL_WINDOW_SUBSYSTEM_FLAGS) & SDL_WINDOW_SUBSYSTEM_FLAGS) != 0) {
                    SDL_QuitSubSystem(SDL_WINDOW_SUBSYSTEM_FLAGS);
                }
            } catch (Exception ex2) {
                LOGGER.log(Level.WARNING, null, ex2);
            }
            listener.handleError("Failed to create display", ex);
            synchronized (createdLock) {
                createdLock.notifyAll();
            }
            return false;
        }

        listener.initialize();
        updateSizes();
        return true;
    }

    private boolean canUseBlitFramebuffer() {
        return (type == Type.Display || type == Type.Canvas) && listener instanceof Application;
    }

    protected boolean useBlitFramebuffer() {
        return canUseBlitFramebuffer();
    }

    protected int getRenderFramebufferWidth() {
        float mode = settings.getDisplayScaleMode();
        if (DisplayScaleUtils.isEmulatedScaleMode(mode)) {
            return Math.max(Math.round(framebufferWidth * mode), 1);
        }
        return Math.max(framebufferWidth, 1);
    }

    protected int getRenderFramebufferHeight() {
        float mode = settings.getDisplayScaleMode();
        if (DisplayScaleUtils.isEmulatedScaleMode(mode)) {
            return Math.max(Math.round(framebufferHeight * mode), 1);
        }
        return Math.max(framebufferHeight, 1);
    }

    @Override
    protected boolean isDefaultFramebufferSrgb() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer value = stack.mallocInt(1);
            if (SDL_GL_GetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, value)) {
                if (value.get(0) != 0) {
                    return true;
                }
                return super.isDefaultFramebufferSrgb();
            }
            LOGGER.log(Level.WARNING, "Unable to query SDL sRGB framebuffer capability: {0}", SDL_GetError());
        }
        return super.isDefaultFramebufferSrgb();
    }

    private Application getApplicationListener() {
        if (listener instanceof Application) {
            return (Application) listener;
        }
        return null;
    }

    private int getBlitFramebufferSampleCount() {
        int samples = Math.max(settings.getSamples(), 1);
        if (samples > 1 && renderer != null
                && (!renderer.getCaps().contains(Caps.TextureMultisample)
                || !renderer.getCaps().contains(Caps.OpenGL32))) {
            if (!blitFramebufferTextureMultisampleWarningIssued) {
                LOGGER.log(Level.WARNING,
                        "Blit framebuffer requested {0}x MSAA, but this backend cannot sample multisample textures for the blit path. Falling back to a single-sample blit framebuffer.",
                        samples);
                blitFramebufferTextureMultisampleWarningIssued = true;
            }
            return 1;
        }
        return samples;
    }

    private void rebuildBlitFramebufferIfNeeded() {
        int width = getRenderFramebufferWidth();
        int height = getRenderFramebufferHeight();
        int samples = getBlitFramebufferSampleCount();

        if (blitFramebuffer != null && blitFramebuffer.getWidth() == width
                && blitFramebuffer.getHeight() == height && blitFramebuffer.getSamples() == samples) {
            return;
        }

        destroyBlitFramebuffer();

        FrameBuffer frameBuffer = new FrameBuffer(width, height, samples);
        frameBuffer.setName("LWJGL3 Blit FrameBuffer");
        frameBuffer.setSrgb(false);

        Texture2D colorTexture = new Texture2D(
                new Image(Format.RGBA16F, width, height, null, ColorSpace.Linear));
        colorTexture.setMagFilter(Texture.MagFilter.Bilinear);
        colorTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        if (samples > 1) {
            colorTexture.getImage().setMultiSamples(samples);
        }
        frameBuffer.addColorTarget(FrameBufferTarget.newTarget(colorTexture));

        if (settings.getDepthBits() > 0 || settings.getStencilBits() > 0) {
            frameBuffer.setDepthTarget(FrameBufferTarget
                    .newTarget(settings.getStencilBits() > 0 ? Format.Depth24Stencil8 : Format.Depth));
        }

        blitColorTexture = colorTexture;
        blitFramebuffer = frameBuffer;
        blitFramebufferDirty = true;
    }

    private boolean ensureBlitResources() {
        if (!useBlitFramebuffer()) {
            return false;
        }

        Application application = getApplicationListener();
        if (application == null) {
            return false;
        }

        AssetManager assetManager = application.getAssetManager();
        RenderManager renderManager = application.getRenderManager();
        if (assetManager == null || renderManager == null) {
            return false;
        }

        if (blitMaterial == null) {
            blitMaterial = new Material(assetManager, BLIT_MATERIAL);
            blitMaterial.getAdditionalRenderState().setDepthTest(false);
            blitMaterial.getAdditionalRenderState().setDepthWrite(false);
        }
        blitMaterial.setBoolean("Srgb", useBlitFramebufferShaderSrgbConversion());

        if (blitGeometry == null) {
            blitGeometry = new Picture("Blit FrameBuffer");
            blitGeometry.setWidth(1f);
            blitGeometry.setHeight(1f);
            blitGeometry.setMaterial(blitMaterial);
        }

        if (blitFramebufferDirty && blitColorTexture != null) {
            blitMaterial.setTexture("Texture", blitColorTexture);
            if (blitFramebuffer != null && blitFramebuffer.getSamples() > 1) {
                blitMaterial.setInt("NumSamples", blitFramebuffer.getSamples());
            } else {
                blitMaterial.clearParam("NumSamples");
            }
            blitFramebufferDirty = false;
        }

        return true;
    }

    private boolean useBlitFramebufferShaderSrgbConversion() {
        return settings.isGammaCorrection() && renderer != null && !renderer.isMainFrameBufferSrgb();
    }

    private void destroyBlitFramebuffer() {
        if (blitFramebuffer != null) {
            blitFramebuffer.dispose();
            blitFramebuffer = null;
        }
        if (blitColorTexture != null && blitColorTexture.getImage() != null) {
            blitColorTexture.getImage().dispose();
        }
        blitColorTexture = null;
        blitFramebufferDirty = true;
    }

    private void destroyBlitFramebufferResources() {
        destroyBlitFramebuffer();
        blitMaterial = null;
        blitGeometry = null;
        blitFramebufferTextureMultisampleWarningIssued = false;
    }

    protected boolean renderFrameWithBlitFramebuffer() {
        if (!(renderer instanceof GLRenderer) || !useBlitFramebuffer()) {
            return false;
        }

        FrameBuffer previousMainFramebuffer = renderer.getCurrentFrameBuffer();
        if (previousMainFramebuffer != null) {
            return false;
        }

        rebuildBlitFramebufferIfNeeded();
        if (blitFramebuffer == null || !ensureBlitResources()) {
            return false;
        }

        GLRenderer glRenderer = (GLRenderer) renderer;
        RenderManager renderManager = getApplicationListener().getRenderManager();
        FrameBuffer restoreMainFramebuffer = previousMainFramebuffer;

        glRenderer.setMainFrameBufferOverride(blitFramebuffer);
        try {
            listener.update();
            FrameBuffer currentMainFramebuffer = renderer.getCurrentFrameBuffer();
            if (currentMainFramebuffer != blitFramebuffer) {
                restoreMainFramebuffer = currentMainFramebuffer;
            }
        } finally {
            glRenderer.setMainFrameBufferOverride(restoreMainFramebuffer);
        }

        glRenderer.setMainFrameBufferOverride(null);
        Camera previousCamera = renderManager.getCurrentCamera();
        try {
            glRenderer.setFrameBuffer(null);
            int blitWidth = Math.max(getFramebufferWidth(), 1);
            int blitHeight = Math.max(getFramebufferHeight(), 1);
            if (blitCamera.getWidth() != blitWidth
                    || blitCamera.getHeight() != blitHeight) {
                blitCamera.resize(blitWidth, blitHeight, true);
            }
            renderManager.setCamera(blitCamera, true);
            if (blitGeometry.getWidth() != blitWidth || blitGeometry.getHeight() != blitHeight) {
                blitGeometry.setWidth(blitWidth);
                blitGeometry.setHeight(blitHeight);
            }
            blitGeometry.updateGeometricState();
            renderManager.renderGeometry(blitGeometry);
        } finally {
            glRenderer.setMainFrameBufferOverride(restoreMainFramebuffer);
            if (previousCamera != null) {
                renderManager.setCamera(previousCamera, false);
            }
        }
        return true;
    }

    protected void runLoop() {
        if (needRestart.getAndSet(false)) {
            restartContext();
        }

        if (!created.get()) {
            throw new IllegalStateException();
        }

        pollEvents(true);
        if (needClose.get() || windowCloseRequested.get()) {
            return;
        }

        if (!renderFrameWithBlitFramebuffer()) {
            listener.update();
        }

        if (renderable.get()) {
            try {
                if ((type != Type.Canvas) && allowSwapBuffers && autoFlush) {
                    if (!SDL_GL_SwapWindow(window)) {
                        String error = SDL_GetError();
                        pollEvents(true);
                        if (!isTransientSwapFailure()) {
                            throw new IllegalStateException("SDL_GL_SwapWindow failed: " + error);
                        }
                        LOGGER.log(Level.FINE, "Skipping transient SDL_GL_SwapWindow failure: {0}", error);
                    } else {
                        windowStateChangedSinceLastSwap = false;
                    }
                }
            } catch (Throwable ex) {
                renderable.set(false);
                needClose.set(true);
                listener.handleError("Error while swapping buffers", ex);
                return;
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

    private boolean isTransientSwapFailure() {
        if (needClose.get() || windowCloseRequested.get() || windowStateChangedSinceLastSwap) {
            return true;
        }
        long flags = window == NULL ? 0 : SDL_GetWindowFlags(window);
        return (flags & (SDL_WINDOW_HIDDEN | SDL_WINDOW_MINIMIZED | SDL_WINDOW_OCCLUDED)) != 0;
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
        if (windowSizeUpdatePending) {
            windowSizeUpdatePending = false;
            updateSizes(created.get() && dispatchToInputs);
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
                if (!created.get()) {
                    break;
                }
                if (!wasActive) {
                    listener.gainFocus();
                    timer.reset();
                    wasActive = true;
                }
                break;
            case SDL_EVENT_WINDOW_FOCUS_LOST:
                if (!created.get()) {
                    break;
                }
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
                windowStateChangedSinceLastSwap = true;
                windowSizeUpdatePending = true;
                break;
            case SDL_EVENT_WINDOW_SHOWN:
            case SDL_EVENT_WINDOW_HIDDEN:
            case SDL_EVENT_WINDOW_EXPOSED:
            case SDL_EVENT_WINDOW_MINIMIZED:
            case SDL_EVENT_WINDOW_RESTORED:
            case SDL_EVENT_WINDOW_DISPLAY_CHANGED:
            case SDL_EVENT_WINDOW_OCCLUDED:
            case SDL_EVENT_WINDOW_ENTER_FULLSCREEN:
            case SDL_EVENT_WINDOW_LEAVE_FULLSCREEN:
                windowStateChangedSinceLastSwap = true;
                break;
            default:
                break;
        }
    }

    private void dispatchSDLEvent(SDL_Event event) {
        for (SdlEventListener listener : sdlEventListeners.getArray()) {
            listener.onSDLEvent(event);
        }
        if (keyInput instanceof SdlEventListener) {
            ((SdlEventListener) keyInput).onSDLEvent(event);
        }
        if (mouseInput instanceof SdlEventListener) {
            ((SdlEventListener) mouseInput).onSDLEvent(event);
        }
        if (joyInput instanceof SdlEventListener) {
            ((SdlEventListener) joyInput).onSDLEvent(event);
        }
    }

    protected void restartContext() {
        try {
            destroyContext();
            createContext(settings);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to set display settings!", ex);
        }

        reinitContext();
        updateSizes();

        if (keyInput != null && keyInput.isInitialized() && keyInput instanceof SdlKeyInput) {
            ((SdlKeyInput) keyInput).resetContext();
        }
        if (mouseInput != null && mouseInput.isInitialized() && mouseInput instanceof SdlMouseInput) {
            ((SdlMouseInput) mouseInput).resetContext();
        }

        LOGGER.fine("Display restarted.");
    }

    protected final void setFrameRateLimit(int frameRateLimit) {
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

    public Vector2f getMouseInputScale(Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        float mode = settings.getDisplayScaleMode();
        if (DisplayScaleUtils.isDpiAwareMode(mode)) {
            return store.set((float) logicalWidth / Math.max(windowWidth, 1),
                    (float) logicalHeight / Math.max(windowHeight, 1));
        }
        return store.set((float) framebufferWidth / Math.max(windowWidth, 1),
                (float) framebufferHeight / Math.max(windowHeight, 1));
    }

    @Override
    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    @Override
    public int getFramebufferWidth() {
        return framebufferWidth;
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
