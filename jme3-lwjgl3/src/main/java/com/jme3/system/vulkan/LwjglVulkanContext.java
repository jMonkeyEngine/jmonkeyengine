package com.jme3.system.vulkan;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.lwjgl.GlfwJoystickInput;
import com.jme3.input.lwjgl.GlfwKeyInput;
import com.jme3.input.lwjgl.GlfwMouseInput;
import com.jme3.math.Vector2f;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.vulkan.VulkanRenderer;
import com.jme3.system.*;
import com.jme3.system.Sync;
import com.jme3.system.WindowSizeListener;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.glfw.GLFW.*;

public class LwjglVulkanContext implements JmeContext, GlfwWindow, Runnable {

    private static final Logger LOGGER = Logger.getLogger(LwjglVulkanContext.class.getName());

    private long window = NULL;
    private SystemListener engine;
    private VulkanRenderer renderer;
    private Thread engineThread;
    private Timer engineTimer;
    private final AtomicBoolean created = new AtomicBoolean(false);
    private final AtomicBoolean destroy = new AtomicBoolean(false);
    private final AtomicBoolean restart = new AtomicBoolean(false);
    private final AtomicBoolean focused = new AtomicBoolean(true);
    private final AppSettings settings = new AppSettings(true);
    private final Collection<WindowSizeListener> sizeListeners = new ArrayList<>();
    private final Vector2f windowScale = new Vector2f(1, 1);
    private int frameBufferWidth, frameBufferHeight;

    private GLFWErrorCallback errorCallback;
    private GLFWWindowFocusCallback focusCallback;
    private GLFWWindowSizeCallback sizeCallback;
    private GLFWFramebufferSizeCallback fbSizeCallback;

    private GlfwMouseInput mouseInput;
    private GlfwKeyInput keyInput;
    private JoyInput joyInput;

    private final int[] width = new int[1];
    private final int[] height = new int[1];

    private boolean autoFlush = true;

    @Override
    public void run() {
        engineInitialize();
        engineLoop();
        engineTerminate();
    }

    @Override
    public void create(boolean waitFor) {
        (engineThread = new Thread(this, getClass().getSimpleName())).start();
        if (waitFor) {
            waitForContextLifeEvent(true);
        }
    }

    protected void waitForContextLifeEvent(boolean creation) {
        synchronized (created) {
            while (created.get() != creation) try {
                created.wait();
            } catch (InterruptedException ignored) {}
        }
    }

    protected void engineInitialize() {
        glfwInitialize();
        rendererInitialize();
        engineTimer = new NanoTimer();
        engine.initialize();
        synchronized (created) {
            created.set(true);
            created.notifyAll();
        }
    }

    protected void glfwInitialize() {
        //if (!GLFWVulkan.glfwVulkanSupported()) {
        //    throw new NullPointerException("Hardware does not support Vulkan.");
        //}
        glfwSetErrorCallback(errorCallback = new GLFWErrorCallback() {
            @Override
            public void invoke(int error, long description) {
                final String message = GLFWErrorCallback.getDescription(description);
                engine.handleError(message, new Exception(message));
            }
        });
        if (glfwPlatformSupported(GLFW_PLATFORM_WAYLAND)) {
            // Disables the libdecor bar when creating a fullscreen context
            // https://www.glfw.org/docs/latest/intro_guide.html#init_hints_wayland
            glfwInitHint(GLFW_WAYLAND_LIBDECOR, settings.isFullscreen() ? GLFW_WAYLAND_DISABLE_LIBDECOR : GLFW_WAYLAND_PREFER_LIBDECOR);
        }
        glfwInit();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        window = glfwCreateWindow(getSettings().getWidth(), getSettings().getHeight(), getSettings().getTitle(), NULL, NULL);
        glfwSetWindowFocusCallback(window, focusCallback = new GLFWWindowFocusCallback() {
            @Override
            public void invoke(long window, boolean focus) {
                if (focus != focused.get()) {
                    if (!focus) {
                        engine.loseFocus();
                    } else {
                        engine.gainFocus();
                        engineTimer.reset();
                    }
                    focused.set(focus);
                }
            }
        });
        glfwSetWindowSizeCallback(window, sizeCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(final long window, final int width, final int height) {
                updateSizes();
            }
        });
        glfwSetFramebufferSizeCallback(window, fbSizeCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(final long window, final int width, final int height) {
                updateSizes();
            }
        });
    }

    protected void rendererInitialize() {
        renderer = new VulkanRenderer();
    }

    protected void engineLoop() {
        while (true) {
            if (restart.get()) {
                restartContext();
            }
            engine.update();
            if (renderer != null) {
                renderer.postFrame();
            }
            syncFrames();
            glfwPollEvents();
            if (destroy.get()) {
                break;
            }
            if (glfwWindowShouldClose(window)) {
                engine.requestClose(false);
            }
        }
    }

    protected void syncFrames() {
        if (autoFlush) {
            Sync.sync(getSettings().getFrameRate());
        } else {
            Sync.sync(20);
        }
    }

    protected void engineTerminate() {
        engine.destroy();
        glfwDestroyWindow(window);
        glfwTerminate();
        LOGGER.fine("Display destroyed.");
    }

    protected void updateSizes() {
        // framebuffer size (resolution) may differ from window size (e.g. HiDPI)
        // resize window informants
        glfwGetWindowSize(window, width, height);
        int w = Math.max(width[0], 1);
        int h = Math.max(height[0], 1);
        if (settings.getWindowWidth() != w || settings.getWindowHeight() != h) {
            settings.setWindowSize(w, h);
            for (WindowSizeListener l : sizeListeners) {
                l.onWindowSizeChanged(w, h);
            }
        }
        // resize framebuffer informants
        glfwGetFramebufferSize(window, width, height);
        if (width[0] != frameBufferWidth || height[0] != frameBufferHeight) {
            settings.setResolution(width[0], height[0]);
            engine.reshape(width[0], height[0]);
            frameBufferWidth = width[0];
            frameBufferHeight = height[0];
        }
        // rescale engine
        float xScale = (float)width[0] / w;
        float yScale = (float)height[0] / h;
        if (windowScale.x != xScale || windowScale.y != yScale) {
            engine.rescale(xScale, yScale);
            windowScale.set(xScale, yScale);
        }
    }

    protected void restartContext() {
        try {
            glfwDestroy();
            glfwInitialize();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to set display settings!", ex);
        }
        // Reinitialize context flags and such
        rendererInitialize();

        // We need to reinit the mouse and keyboard input as they are tied to a window handle
        if (keyInput != null && keyInput.isInitialized()) {
            keyInput.resetContext();
        }
        if (mouseInput != null && mouseInput.isInitialized()) {
            mouseInput.resetContext();
        }

        LOGGER.fine("Display restarted.");
    }

    protected void glfwDestroy() {
        try {
            if (renderer != null) {
                renderer.cleanup();
            }
            if (errorCallback != null) {
                // We need to specifically set this to null as we might set a new callback before we reinit GLFW
                glfwSetErrorCallback(null);
                errorCallback.close();
                errorCallback = null;
            }
            if (sizeCallback != null) {
                sizeCallback.close();
                sizeCallback = null;
            }
            if (fbSizeCallback != null) {
                fbSizeCallback.close();
                fbSizeCallback = null;
            }
            if (focusCallback != null) {
                focusCallback.close();
                focusCallback = null;
            }
            if (window != NULL) {
                glfwDestroyWindow(window);
                window = NULL;
            }
        } catch (final Exception ex) {
            engine.handleError("Failed to destroy context", ex);
        }
    }

    @Override
    public Type getType() {
        return Type.Display;
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }

    @Override
    public SystemListener getSystemListener() {
        return engine;
    }

    @Override
    public void setSystemListener(SystemListener listener) {
        this.engine = listener;
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
    public Context getOpenCLContext() {
        return null;
    }

    @Override
    public MouseInput getMouseInput() {
        if (mouseInput == null) {
            mouseInput = new GlfwMouseInput(this);
        }
        return mouseInput;
    }

    @Override
    public KeyInput getKeyInput() {
        if (keyInput == null) {
            keyInput = new GlfwKeyInput(this);
        }
        return keyInput;
    }

    @Override
    public JoyInput getJoyInput() {
        if (joyInput == null) {
            joyInput = new GlfwJoystickInput();
        }
        return joyInput;
    }

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public Timer getTimer() {
        return engineTimer;
    }

    @Override
    public void setTitle(String title) {
        if (window != NULL) {
            glfwSetWindowTitle(window, title);
        }
    }

    @Override
    public boolean isCreated() {
        return created.get();
    }

    @Override
    public long getWindowHandle() {
        return window;
    }

    @Override
    public Vector2f getWindowContentScale(Vector2f store) {
        if (store == null) store = new Vector2f();
        glfwGetFramebufferSize(window, width, height);
        store.set(width[0], height[0]);
        glfwGetWindowSize(window, width, height);
        store.x /= width[0];
        store.y /= height[0];
        return store;
    }

    @Override
    public boolean isRenderable() {
        return renderer != null;
    }

    @Override
    public void registerWindowSizeListener(WindowSizeListener listener) {
        sizeListeners.add(listener);
    }

    @Override
    public void removeWindowSizeListener(WindowSizeListener listener) {
        sizeListeners.remove(listener);
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        autoFlush = enabled;
    }

    @Override
    public void restart() {
        if (created.get()) {
            restart.set(true);
        } else {
            LOGGER.warning("Cannot restart context: not yet initialized.");
        }
    }

    @Override
    public void destroy(boolean waitFor) {
        destroy.set(true);
        if (Thread.currentThread() != engineThread) {
            waitForContextLifeEvent(false);
        }
    }

    @Override
    public int getFramebufferHeight() {
        glfwGetFramebufferSize(window, width, height);
        return width[0];
    }

    @Override
    public int getFramebufferWidth() {
        glfwGetFramebufferSize(window, width, height);
        return height[0];
    }

    @Override
    public int getWindowXPosition() {
        glfwGetWindowPos(window, width, height);
        return width[0];
    }

    @Override
    public int getWindowYPosition() {
        glfwGetWindowPos(window, width, height);
        return height[0];
    }

    @Override
    public Displays getDisplays() {
        PointerBuffer displays = glfwGetMonitors();
        long primary = glfwGetPrimaryMonitor();
        Displays displayList = new Displays();

        for (int i = 0; i < displays.limit(); i++) {
            long monitorI = displays.get(i);
            int monPos = displayList.addNewMonitor(monitorI);
            if (primary == monitorI) displayList.setPrimaryDisplay(monPos);
            final GLFWVidMode modes = glfwGetVideoMode(monitorI);
            String name = glfwGetMonitorName(monitorI);
            int width = modes.width();
            int height = modes.height();
            int rate = modes.refreshRate();
            displayList.setInfo(monPos, name, width, height, rate);
            LOGGER.log(Level.INFO, "Display id: " + monitorI + " Resolution: " + width + " x " + height + " @ " + rate);
        }
        return displayList;
    }

    @Override
    public int getPrimaryDisplay() {
        long prim = glfwGetPrimaryMonitor();
        Displays monitors = getDisplays();
        for (int i = 0; i < monitors.size(); i++) {
            long monitorI = monitors.get(i).getDisplay();
            if (monitorI == prim) return i;
        }
        LOGGER.log(Level.SEVERE, "Couldn't locate Primary Monitor in the list of Monitors.");
        return -1;
    }

}
