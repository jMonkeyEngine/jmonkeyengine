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
import com.jme3.input.JoyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
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
import static org.lwjgl.sdl.SDLTimer.*;

/**
 * SDL implementation of {@link MouseInput}.
 */
public class SdlMouseInput implements MouseInput, SdlEventListener {

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
    private float windowCoordWidth = 1f;
    private float windowCoordHeight = 1f;
    private float visibleCursorX;
    private float visibleCursorY;

    private boolean cursorVisible = true;
    private boolean windowFocused = true;
    private boolean visibleCursorPositionValid;
    private boolean protonCursorRestorePending;
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
        visibleCursorPositionValid = false;
        refreshWindowMetrics();
        initCurrentMousePosition();
        setCursorVisible(cursorVisible);
    }

    @Override
    public void onSDLEvent(SDL_Event event) {
        final int type = event.type();

        if (type == SDL_EVENT_WINDOW_FOCUS_GAINED) {
            if (event.window().windowID() == context.getWindowId()) {
                windowFocused = true;
                visibleCursorPositionValid = false;
                refreshWindowMetrics();
                initCurrentMousePosition();
                setCursorVisible(cursorVisible);
            }
            return;
        }

        if (type == SDL_EVENT_WINDOW_FOCUS_LOST) {
            if (event.window().windowID() == context.getWindowId()) {
                windowFocused = false;
                mouseMotionEvents.clear();
                if (!cursorVisible) {
                    restoreVisibleCursorPosition();
                }
                SDL_SetWindowRelativeMouseMode(context.getWindowHandle(), false);
            }
            return;
        }

        if (type == SDL_EVENT_MOUSE_MOTION) {
            if (event.motion().windowID() != context.getWindowId()) {
                return;
            }
            if (!windowFocused) {
                return;
            }
            refreshWindowMetrics();
            final boolean relativeMode = SDL_GetWindowRelativeMouseMode(context.getWindowHandle());
            final int x;
            final int y;
            final int xDelta;
            final int yDelta;

            if (relativeMode) {
                xDelta = Math.round(event.motion().xrel() * inputScale.x);
                yDelta = -Math.round(event.motion().yrel() * inputScale.y);
                mouseX += xDelta;
                mouseY += yDelta;
                x = mouseX;
                y = mouseY;
            } else {
                x = toInputX(event.motion().x());
                y = toInputY(event.motion().y());
                xDelta = x - mouseX;
                yDelta = y - mouseY;
                mouseX = x;
                mouseY = y;
            }

            if (xDelta != 0 || yDelta != 0) {
                if (onPointerMove(0, x, y, event.motion().timestamp())) {
                    return;
                }
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
            if (!SDL_GetWindowRelativeMouseMode(context.getWindowHandle())) {
                refreshWindowMetrics();
                mouseX = toInputX(event.button().x());
                mouseY = toInputY(event.button().y());
            }
            if (onPointerButton(0, event.button().down(), mouseX, mouseY, event.button().timestamp())) {
                return;
            }

            int button = Byte.toUnsignedInt(event.button().button());
            MouseButtonEvent mouseButtonEvent =
                    new MouseButtonEvent(convertButton(button), event.button().down(), mouseX, mouseY);
            mouseButtonEvent.setTime(event.button().timestamp());
            mouseButtonEvents.add(mouseButtonEvent);
        }
    }

    private boolean onPointerButton(int pointerId, boolean pressed, float x, float y, long time) {
        JoyInput joyInput = context.getJoyInput();
        if (joyInput instanceof SdlJoystickInput) {
            if (pressed) {
                return ((SdlJoystickInput) joyInput).onPointerDown(pointerId, x, y, time);
            }
            return ((SdlJoystickInput) joyInput).onPointerUp(pointerId, x, y, time);
        }
        return false;
    }

    private boolean onPointerMove(int pointerId, float x, float y, long time) {
        JoyInput joyInput = context.getJoyInput();
        if (joyInput instanceof SdlJoystickInput) {
            return ((SdlJoystickInput) joyInput).onPointerMove(pointerId, x, y, time);
        }
        return false;
    }

    private void refreshWindowMetrics() {
        AppSettings settings = context.getSettings();
        currentWidth = Math.max(settings.getWidth(), 1);
        currentHeight = Math.max(settings.getHeight(), 1);
        context.getMouseInputScale(inputScale);
        windowCoordWidth = currentWidth / Math.max(inputScale.x, 0.0001f);
        windowCoordHeight = currentHeight / Math.max(inputScale.y, 0.0001f);
    }

    private void initCurrentMousePosition() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.callocFloat(1);
            FloatBuffer y = stack.callocFloat(1);
            SDL_GetMouseState(x, y);
            mouseX = toInputX(x.get(0));
            mouseY = toInputY(y.get(0));
        }
    }

    private int toInputX(float x) {
        return Math.round(x * inputScale.x);
    }

    private int toInputY(float y) {
        return Math.round(currentHeight - (y * inputScale.y));
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
        if (protonCursorRestorePending && cursorVisible && windowFocused
                && !SDL_GetWindowRelativeMouseMode(context.getWindowHandle())) {
            protonCursorRestorePending = false;
            restoreVisibleCursorPosition();
        }

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
            if (!wasVisible) {
                if (restoreVisibleCursorPosition()) {
                    queueMousePositionSyncEvent();
                    protonCursorRestorePending = JmeSystem.getPlatform().isWineProton();
                }
            }
            SDL_SetWindowRelativeMouseMode(context.getWindowHandle(), false);
        } else {
            protonCursorRestorePending = false;
            if (wasVisible || !visibleCursorPositionValid) {
                saveVisibleCursorPosition();
            }
            SDL_SetWindowRelativeMouseMode(context.getWindowHandle(), true);
        }
    }

    private void saveVisibleCursorPosition() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.callocFloat(1);
            FloatBuffer y = stack.callocFloat(1);
            SDL_GetMouseState(x, y);
            visibleCursorX = x.get(0);
            visibleCursorY = y.get(0);
            visibleCursorPositionValid = true;
        }
    }

    private boolean restoreVisibleCursorPosition() {
        if (!visibleCursorPositionValid) {
            return false;
        }
        refreshWindowMetrics();
        float x = Math.max(0f, Math.min(visibleCursorX, windowCoordWidth));
        float y = Math.max(0f, Math.min(visibleCursorY, windowCoordHeight));
        SDL_WarpMouseInWindow(context.getWindowHandle(), x, y);
        mouseX = toInputX(x);
        mouseY = toInputY(y);
        return true;
    }

    private void queueMousePositionSyncEvent() {
        MouseMotionEvent event = new MouseMotionEvent(mouseX, mouseY, 0, 0, mouseWheel, 0);
        event.setTime(getInputTimeNanos());
        mouseMotionEvents.add(event);
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
