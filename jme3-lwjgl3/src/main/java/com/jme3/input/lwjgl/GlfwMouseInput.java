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
package com.jme3.input.lwjgl;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.system.MemoryUtil;

/**
 * Captures mouse input using GLFW callbacks. It then temporarily stores these
 * in event queues which are processed in the {@link #update()} method. Due to
 * some of the GLFW button id's there is a conversion method in this class which
 * will convert the GLFW left, middle and right mouse button to JME3 left,
 * middle and right button codes.
 *
 * @author Daniel Johansson (dannyjo)
 * @since 3.1
 */
public class GlfwMouseInput implements MouseInput {

    private static final Logger logger = Logger.getLogger(GlfwMouseInput.class.getName());

    private static final int WHEEL_SCALE = 120;

    private final LwjglWindow context;
    private RawInputListener listener;
    private boolean cursorVisible = true;
    private long[] currentCursor;
    private IntBuffer currentCursorDelays;
    private int currentCursorFrame = 0;
    private double currentCursorFrameStartTime = 0.0;
    private int mouseX;
    private int mouseY;
    private int mouseWheel;
    private boolean initialized;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWScrollCallback scrollCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private final Queue<MouseMotionEvent> mouseMotionEvents = new ArrayDeque<>();
    private final Queue<MouseButtonEvent> mouseButtonEvents = new ArrayDeque<>();

    private final Map<JmeCursor, long[]> jmeToGlfwCursorMap = new HashMap<>();

    public GlfwMouseInput(LwjglWindow context) {
        this.context = context;
    }

    private void onCursorPos(long window, double xpos, double ypos) {
        int xDelta;
        int yDelta;
        int x = (int) Math.round(xpos);
        int y = context.getSettings().getHeight() - (int) Math.round(ypos);

        if (mouseX == 0) {
            mouseX = x;
        }

        if (mouseY == 0) {
            mouseY = y;
        }

        xDelta = x - mouseX;
        yDelta = y - mouseY;
        mouseX = x;
        mouseY = y;

        if (xDelta != 0 || yDelta != 0) {
            final MouseMotionEvent mouseMotionEvent = new MouseMotionEvent(x, y, xDelta, yDelta, mouseWheel, 0);
            mouseMotionEvent.setTime(getInputTimeNanos());
            mouseMotionEvents.add(mouseMotionEvent);
        }
    }

    private void onWheelScroll(long window, double xOffset, double yOffset) {
        mouseWheel += yOffset;
        final MouseMotionEvent mouseMotionEvent = new MouseMotionEvent(mouseX, mouseY, 0, 0, mouseWheel, (int) Math.round(yOffset));
        mouseMotionEvent.setTime(getInputTimeNanos());
        mouseMotionEvents.add(mouseMotionEvent);
    }

    private void onMouseButton(final long window, final int button, final int action, final int mods) {
        final MouseButtonEvent mouseButtonEvent = new MouseButtonEvent(convertButton(button), action == GLFW_PRESS, mouseX, mouseY);
        mouseButtonEvent.setTime(getInputTimeNanos());
        mouseButtonEvents.add(mouseButtonEvent);
    }

    @Override
    public void initialize() {
        glfwSetCursorPosCallback(context.getWindowHandle(), cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                onCursorPos(window, xpos, ypos);
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

        glfwSetScrollCallback(context.getWindowHandle(), scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(final long window, final double xOffset, final double yOffset) {
                onWheelScroll(window, xOffset, yOffset * WHEEL_SCALE);
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

        glfwSetMouseButtonCallback(context.getWindowHandle(), mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(final long window, final int button, final int action, final int mods) {
                onMouseButton(window, button, action, mods);
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

        setCursorVisible(cursorVisible);
        logger.fine("Mouse created.");
        initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public int getButtonCount() {
        return GLFW_MOUSE_BUTTON_LAST + 1;
    }

    @Override
    public void update() {

        // Manage cursor animation
        if (currentCursor != null && currentCursor.length > 1) {
            double now = glfwGetTime();
            double frameTime = (glfwGetTime() - currentCursorFrameStartTime) * 1000;
            if (currentCursorDelays == null || frameTime >= currentCursorDelays.get(currentCursorFrame)) {
                currentCursorFrame = ++currentCursorFrame % currentCursor.length;
                currentCursorFrameStartTime = now;
                glfwSetCursor(context.getWindowHandle(), currentCursor[currentCursorFrame]);
            }
        }

        // Process events
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

        cursorPosCallback.close();
        scrollCallback.close();
        mouseButtonCallback.close();

        currentCursor = null;
        currentCursorDelays = null;
        for (long[] glfwCursors : jmeToGlfwCursorMap.values()) {
            for (long glfwCursor : glfwCursors) {
                glfwDestroyCursor(glfwCursor);
            }
        }
        jmeToGlfwCursorMap.clear();

        logger.fine("Mouse destroyed.");
    }

    @Override
    public void setCursorVisible(boolean visible) {
        cursorVisible = visible;

        if (!context.isRenderable()) {
            return;
        }

        if (cursorVisible) {
            glfwSetInputMode(context.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        } else {
            glfwSetInputMode(context.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return (long) (glfwGetTime() * 1000000000);
    }

    private ByteBuffer transformCursorImage(IntBuffer imageData, int w, int h, int index) {
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);

        // Transform image: ARGB -> RGBA, vertical flip
        for (int y = h - 1; y >= 0; --y) {
            for (int x = 0; x < w; ++x) {
                int pixel = imageData.get(w * h * index + y * w + x);
                buf.put((byte) ((pixel >> 16) & 0xFF));  // red
                buf.put((byte) ((pixel >> 8) & 0xFF));  // green
                buf.put((byte) (pixel & 0xFF));  // blue
                buf.put((byte) ((pixel >> 24) & 0xFF));  // alpha
            }
        }

        buf.flip();
        return buf;
    }

    private long[] createGlfwCursor(JmeCursor jmeCursor) {
        long[] cursorArray = new long[jmeCursor.getNumImages()];
        for (int i = 0; i < jmeCursor.getNumImages(); i++) {
            ByteBuffer buf = transformCursorImage(jmeCursor.getImagesData(), jmeCursor.getWidth(), jmeCursor.getHeight(), i);

            GLFWImage glfwImage = new GLFWImage(BufferUtils.createByteBuffer(GLFWImage.SIZEOF));
            glfwImage.set(jmeCursor.getWidth(), jmeCursor.getHeight(), buf);

            int hotspotX = jmeCursor.getXHotSpot();
            int hotspotY = jmeCursor.getHeight() - jmeCursor.getYHotSpot();

            cursorArray[i] = glfwCreateCursor(glfwImage, hotspotX, hotspotY);
        }
        return cursorArray;
    }

    @Override
    public void setNativeCursor(JmeCursor jmeCursor) {
        if (jmeCursor != null) {
            long[] glfwCursor = jmeToGlfwCursorMap.get(jmeCursor);

            if (glfwCursor == null) {
                glfwCursor = createGlfwCursor(jmeCursor);
                jmeToGlfwCursorMap.put(jmeCursor, glfwCursor);
            }

            currentCursorFrame = 0;
            currentCursor = glfwCursor;
            currentCursorDelays = null;
            currentCursorFrameStartTime = glfwGetTime();
            if (jmeCursor.getImagesDelay() != null) {
                currentCursorDelays = jmeCursor.getImagesDelay();
            }
            glfwSetCursor(context.getWindowHandle(), glfwCursor[currentCursorFrame]);
        } else {
            currentCursor = null;
            currentCursorDelays = null;
            glfwSetCursor(context.getWindowHandle(), MemoryUtil.NULL);
        }
    }

    /**
     * Simply converts the GLFW button code to a JME button code. If there is no
     * match it just returns the GLFW button code. Bear in mind GLFW supports 8
     * different mouse buttons.
     *
     * @param glfwButton the raw GLFW button index.
     * @return the mapped {@link MouseInput} button id.
     */
    private int convertButton(final int glfwButton) {
        switch (glfwButton) {
            case GLFW_MOUSE_BUTTON_LEFT:
                return MouseInput.BUTTON_LEFT;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                return MouseInput.BUTTON_MIDDLE;
            case GLFW_MOUSE_BUTTON_RIGHT:
                return MouseInput.BUTTON_RIGHT;
            default:
                return glfwButton;
        }
    }
}
