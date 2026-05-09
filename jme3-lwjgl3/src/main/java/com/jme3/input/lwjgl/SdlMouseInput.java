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
package com.jme3.input.lwjgl;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Vector2f;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLPixels.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLHints.*;
import static org.lwjgl.sdl.SDLTimer.*;
import static org.lwjgl.sdl.SDLVideo.*;

/**
 * SDL implementation of {@link MouseInput}.
 */
public class SdlMouseInput implements MouseInput {

    private static final Logger LOGGER = Logger.getLogger(SdlMouseInput.class.getName());
    private static final int WHEEL_SCALE = 120;

    private final Map<JmeCursor, long[]> jmeToSdlCursorMap = new HashMap<>();
    private final Queue<MouseMotionEvent> mouseMotionEvents = new ArrayDeque<>();
    private final Queue<MouseButtonEvent> mouseButtonEvents = new ArrayDeque<>();
    private final Vector2f inputScale = new Vector2f(1f, 1f);

    private final LwjglWindow context;
    private RawInputListener listener;

    private long[] currentCursor;
    private IntBuffer currentCursorDelays;
    private long currentCursorFrameStartTimeNs = 0L;
    private int currentCursorFrame = 0;

    private int mouseX;
    private int mouseY;
    private int mouseWheel;
    private int currentWidth;
    private int currentHeight;

    private boolean cursorVisible = true;
    private boolean x11WarpGrabMode;
    private boolean ignoreNextX11WarpEvent;
    private boolean initialized;

    public SdlMouseInput(final LwjglWindow context) {
        this.context = context;
    }

    @Override
    public void initialize() {
        if (!context.isRenderable()) {
            return;
        }
        refreshWindowMetrics();
        initCurrentMousePosition();

        if (listener != null) {
            sendFirstMouseEvent();
        }

        setCursorVisible(cursorVisible);
        initialized = true;
        LOGGER.fine("SDL mouse created.");
    }

    public void resetContext() {
        if (!context.isRenderable()) {
            return;
        }
        refreshWindowMetrics();
        initCurrentMousePosition();
        setCursorVisible(cursorVisible);
    }

    public void onSDLEvent(SDL_Event event) {
        final int type = event.type();

        if (type == SDL_EVENT_MOUSE_MOTION) {
            if (event.motion().windowID() != context.getWindowId()) {
                return;
            }
            refreshWindowMetrics();
            final boolean relativeMode = SDL_GetWindowRelativeMouseMode(context.getWindowHandle());
            final int x;
            final int y;
            final int xDelta;
            final int yDelta;

            if (x11WarpGrabMode) {
                if (ignoreNextX11WarpEvent && isNearWindowCenter(event.motion().x(), event.motion().y())) {
                    ignoreNextX11WarpEvent = false;
                    return;
                }
                ignoreNextX11WarpEvent = false;
                float logicalWidth = currentWidth / Math.max(inputScale.x, 1f);
                float logicalHeight = currentHeight / Math.max(inputScale.y, 1f);
                int centerX = currentWidth / 2;
                int centerY = currentHeight / 2;
                xDelta = Math.round(event.motion().xrel() * inputScale.x);
                yDelta = -Math.round(event.motion().yrel() * inputScale.y);
                mouseX = centerX;
                mouseY = centerY;
                x = centerX;
                y = centerY;
                if (xDelta != 0 || yDelta != 0) {
                    warpMouseToWindowCenter();
                    ignoreNextX11WarpEvent = true;
                }
            } else if (relativeMode) {
                xDelta = Math.round(event.motion().xrel() * inputScale.x);
                yDelta = -Math.round(event.motion().yrel() * inputScale.y);
                mouseX = clamp(mouseX + xDelta, 0, currentWidth);
                mouseY = clamp(mouseY + yDelta, 0, currentHeight);
                x = mouseX;
                y = mouseY;
            } else {
                x = toPixelX(event.motion().x());
                y = toPixelY(event.motion().y());
                xDelta = x - mouseX;
                yDelta = y - mouseY;
                mouseX = x;
                mouseY = y;
            }

            if (xDelta != 0 || yDelta != 0) {
                MouseMotionEvent mouseMotionEvent =
                        new MouseMotionEvent(x, y, xDelta, yDelta, mouseWheel, 0);
                mouseMotionEvent.setTime(event.motion().timestamp());
                mouseMotionEvents.add(mouseMotionEvent);
            }
            return;
        }

        if (type == SDL_EVENT_MOUSE_WHEEL) {
            if (event.wheel().windowID() != context.getWindowId()) {
                return;
            }
            int wheelDelta = event.wheel().integer_y() * WHEEL_SCALE;
            if (wheelDelta == 0) {
                wheelDelta = Math.round(event.wheel().y() * WHEEL_SCALE);
            }
            mouseWheel += wheelDelta;
            MouseMotionEvent mouseMotionEvent =
                    new MouseMotionEvent(mouseX, mouseY, 0, 0, mouseWheel, wheelDelta);
            mouseMotionEvent.setTime(event.wheel().timestamp());
            mouseMotionEvents.add(mouseMotionEvent);
            return;
        }

        if (type == SDL_EVENT_MOUSE_BUTTON_DOWN || type == SDL_EVENT_MOUSE_BUTTON_UP) {
            if (event.button().windowID() != context.getWindowId()) {
                return;
            }
            refreshWindowMetrics();
            mouseX = toPixelX(event.button().x());
            mouseY = toPixelY(event.button().y());

            int button = Byte.toUnsignedInt(event.button().button());
            MouseButtonEvent mouseButtonEvent =
                    new MouseButtonEvent(convertButton(button), event.button().down(), mouseX, mouseY);
            mouseButtonEvent.setTime(event.button().timestamp());
            mouseButtonEvents.add(mouseButtonEvent);
        }
    }

    private void refreshWindowMetrics() {
        currentWidth = Math.max(context.getFramebufferWidth(), 1);
        currentHeight = Math.max(context.getFramebufferHeight(), 1);
        context.getWindowContentScale(inputScale);
    }

    private void initCurrentMousePosition() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.callocFloat(1);
            FloatBuffer y = stack.callocFloat(1);
            SDL_GetMouseState(x, y);
            mouseX = toPixelX(x.get(0));
            mouseY = toPixelY(y.get(0));
        }
    }

    private int toPixelX(float x) {
        return Math.round(x * inputScale.x);
    }

    private int toPixelY(float y) {
        return Math.round(currentHeight - (y * inputScale.y));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void sendFirstMouseEvent() {
        MouseMotionEvent evt = new MouseMotionEvent(mouseX, mouseY, 0, 0, mouseWheel, 0);
        evt.setTime(getInputTimeNanos());
        listener.onMouseMotionEvent(evt);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public int getButtonCount() {
        return 5;
    }

    @Override
    public void update() {
        if (currentCursor != null && currentCursor.length > 1) {
            long now = SDL_GetTicksNS();
            long frameTimeMs = (now - currentCursorFrameStartTimeNs) / 1_000_000L;
            if (currentCursorDelays == null || frameTimeMs >= currentCursorDelays.get(currentCursorFrame)) {
                currentCursorFrame = ++currentCursorFrame % currentCursor.length;
                currentCursorFrameStartTimeNs = now;
                SDL_SetCursor(currentCursor[currentCursorFrame]);
            }
        }

        if (listener == null) {
            mouseMotionEvents.clear();
            mouseButtonEvents.clear();
            return;
        }

        while (!mouseMotionEvents.isEmpty()) {
            listener.onMouseMotionEvent(mouseMotionEvents.poll());
        }

        while (!mouseButtonEvents.isEmpty()) {
            listener.onMouseButtonEvent(mouseButtonEvents.poll());
        }
    }

    @Override
    public void destroy() {
        if (!context.isRenderable()) {
            return;
        }

        currentCursor = null;
        currentCursorDelays = null;

        for (long[] sdlCursors : jmeToSdlCursorMap.values()) {
            for (long sdlCursor : sdlCursors) {
                if (sdlCursor != MemoryUtil.NULL) {
                    SDL_DestroyCursor(sdlCursor);
                }
            }
        }
        jmeToSdlCursorMap.clear();
        mouseMotionEvents.clear();
        mouseButtonEvents.clear();
        initialized = false;
        LOGGER.fine("SDL mouse destroyed.");
    }

    @Override
    public void setCursorVisible(boolean visible) {
        boolean wasVisible = cursorVisible;
        cursorVisible = visible;
        if (!context.isRenderable()) {
            return;
        }

        if (cursorVisible) {
            x11WarpGrabMode = false;
            ignoreNextX11WarpEvent = false;
            SDL_CaptureMouse(false);
            SDL_SetWindowMouseGrab(context.getWindowHandle(), false);
            SDL_SetWindowRelativeMouseMode(context.getWindowHandle(), false);
            if (!wasVisible) {
                centerVisibleCursor();
            }
            SDL_ShowCursor();
        } else {
            SDL_SetWindowMouseGrab(context.getWindowHandle(), true);
            SDL_CaptureMouse(true);
            if (isX11Backend()) {
                x11WarpGrabMode = true;
                ignoreNextX11WarpEvent = true;
                SDL_SetWindowRelativeMouseMode(context.getWindowHandle(), false);
                warpMouseToWindowCenter();
                syncMouseToWindowCenter();
            } else {
                x11WarpGrabMode = false;
                SDL_SetHint(SDL_HINT_MOUSE_RELATIVE_MODE_CENTER, "1");
                SDL_SetHint(SDL_HINT_MOUSE_RELATIVE_CURSOR_VISIBLE, "0");
                SDL_SetHint(SDL_HINT_MOUSE_RELATIVE_WARP_MOTION, "0");
                SDL_SetWindowRelativeMouseMode(context.getWindowHandle(), true);
                warpMouseToWindowCenter();
            }
            SDL_HideCursor();
        }
    }

    private boolean isX11Backend() {
        return "x11".equalsIgnoreCase(SDL_GetCurrentVideoDriver());
    }

    private void warpMouseToWindowCenter() {
        refreshWindowMetrics();
        float logicalWidth = currentWidth / Math.max(inputScale.x, 1f);
        float logicalHeight = currentHeight / Math.max(inputScale.y, 1f);
        SDL_WarpMouseInWindow(context.getWindowHandle(), logicalWidth * 0.5f, logicalHeight * 0.5f);
    }

    private void centerVisibleCursor() {
        warpMouseToWindowCenter();
        syncMouseToWindowCenter();
    }

    private void syncMouseToWindowCenter() {
        refreshWindowMetrics();
        mouseX = currentWidth / 2;
        mouseY = currentHeight / 2;
    }

    private boolean isNearWindowCenter(float x, float y) {
        refreshWindowMetrics();
        float logicalWidth = currentWidth / Math.max(inputScale.x, 1f);
        float logicalHeight = currentHeight / Math.max(inputScale.y, 1f);
        return Math.abs(x - (logicalWidth * 0.5f)) <= 1.5f
                && Math.abs(y - (logicalHeight * 0.5f)) <= 1.5f;
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
        if (listener != null && initialized) {
            sendFirstMouseEvent();
        }
    }

    @Override
    public long getInputTimeNanos() {
        return SDL_GetTicksNS();
    }

    @Override
    public void setNativeCursor(final JmeCursor jmeCursor) {
        if (jmeCursor != null) {
            final long[] sdlCursor = jmeToSdlCursorMap.computeIfAbsent(jmeCursor, SdlMouseInput::createSdlCursor);
            if (sdlCursor.length == 0) {
                return;
            }

            currentCursorFrame = 0;
            currentCursor = sdlCursor;
            currentCursorDelays = null;
            currentCursorFrameStartTimeNs = SDL_GetTicksNS();

            if (jmeCursor.getImagesDelay() != null) {
                currentCursorDelays = jmeCursor.getImagesDelay();
            }

            SDL_SetCursor(sdlCursor[currentCursorFrame]);
        } else {
            currentCursor = null;
            currentCursorDelays = null;
            SDL_SetCursor(MemoryUtil.NULL);
        }
    }

    private static long[] createSdlCursor(final JmeCursor jmeCursor) {
        long[] cursorArray = new long[jmeCursor.getNumImages()];
        for (int i = 0; i < jmeCursor.getNumImages(); i++) {
            ByteBuffer buffer = transformCursorImage(
                    jmeCursor.getImagesData(), jmeCursor.getWidth(), jmeCursor.getHeight(), i);
            SDL_Surface surface = SDL_CreateSurfaceFrom(
                    jmeCursor.getWidth(),
                    jmeCursor.getHeight(),
                    SDL_PIXELFORMAT_RGBA32,
                    buffer,
                    jmeCursor.getWidth() * 4
            );
            if (surface == null) {
                cursorArray[i] = MemoryUtil.NULL;
                continue;
            }

            int hotspotX = jmeCursor.getXHotSpot();
            int hotspotY = jmeCursor.getHeight() - jmeCursor.getYHotSpot();
            cursorArray[i] = SDL_CreateColorCursor(surface, hotspotX, hotspotY);
            SDL_DestroySurface(surface);
        }
        return cursorArray;
    }

    private static ByteBuffer transformCursorImage(final IntBuffer imageData, final int width, final int height,
                                                   final int index) {

        final ByteBuffer byteBuffer = BufferUtils.createByteBuffer(width * height * 4);

        // Transform image: ARGB -> RGBA, vertical flip.
        for (int y = height - 1; y >= 0; --y) {
            for (int x = 0; x < width; ++x) {
                int pixel = imageData.get(width * height * index + y * width + x);
                byteBuffer.put((byte) ((pixel >> 16) & 0xFF)); // red
                byteBuffer.put((byte) ((pixel >> 8) & 0xFF));  // green
                byteBuffer.put((byte) (pixel & 0xFF));         // blue
                byteBuffer.put((byte) ((pixel >> 24) & 0xFF)); // alpha
            }
        }

        byteBuffer.flip();
        return byteBuffer;
    }

    private int convertButton(final int sdlButton) {
        switch (sdlButton) {
            case SDL_BUTTON_LEFT:
                return MouseInput.BUTTON_LEFT;
            case SDL_BUTTON_MIDDLE:
                return MouseInput.BUTTON_MIDDLE;
            case SDL_BUTTON_RIGHT:
                return MouseInput.BUTTON_RIGHT;
            default:
                return sdlButton;
        }
    }
}
