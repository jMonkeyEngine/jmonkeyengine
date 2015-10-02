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
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Captures mouse input using GLFW callbacks. It then temporarily stores these in event queues which are processed in the
 * {@link #update()} method. Due to some of the GLFW button id's there is a conversion method in this class which will
 * convert the GLFW left, middle and right mouse button to JME3 left, middle and right button codes.
 *
 * @author Daniel Johansson (dannyjo)
 * @since 3.1
 */
public class GlfwMouseInput implements MouseInput {

    private static final Logger logger = Logger.getLogger(GlfwMouseInput.class.getName());

    private LwjglWindow context;
    private RawInputListener listener;
    private boolean cursorVisible = true;
    private int mouseX;
    private int mouseY;
    private int mouseWheel;
    private boolean initialized;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWScrollCallback scrollCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private Queue<MouseMotionEvent> mouseMotionEvents = new LinkedList<MouseMotionEvent>();
    private Queue<MouseButtonEvent> mouseButtonEvents = new LinkedList<MouseButtonEvent>();

    public GlfwMouseInput(final LwjglWindow context) {
        this.context = context;
    }

    public void initialize() {
        glfwSetCursorPosCallback(context.getWindowHandle(), cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
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
        });

        glfwSetScrollCallback(context.getWindowHandle(), scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(final long window, final double xOffset, final double yOffset) {
                mouseWheel += yOffset;

                final MouseMotionEvent mouseMotionEvent = new MouseMotionEvent(mouseX, mouseY, 0, 0, mouseWheel, (int) Math.round(yOffset));
                mouseMotionEvent.setTime(getInputTimeNanos());
                mouseMotionEvents.add(mouseMotionEvent);
            }
        });

        glfwSetMouseButtonCallback(context.getWindowHandle(), mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(final long window, final int button, final int action, final int mods) {
                final MouseButtonEvent mouseButtonEvent = new MouseButtonEvent(convertButton(button), action == GLFW_PRESS, mouseX, mouseY);
                mouseButtonEvent.setTime(getInputTimeNanos());
                mouseButtonEvents.add(mouseButtonEvent);
            }
        });

        setCursorVisible(cursorVisible);
        logger.fine("Mouse created.");
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getButtonCount() {
        return GLFW_MOUSE_BUTTON_LAST + 1;
    }

    public void update() {
        while (!mouseMotionEvents.isEmpty()) {
            listener.onMouseMotionEvent(mouseMotionEvents.poll());
        }

        while (!mouseButtonEvents.isEmpty()) {
            listener.onMouseButtonEvent(mouseButtonEvents.poll());
        }
    }

    public void destroy() {
        if (!context.isRenderable()) {
            return;
        }

        cursorPosCallback.release();
        scrollCallback.release();
        mouseButtonCallback.release();

        logger.fine("Mouse destroyed.");
    }

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

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return (long) (glfwGetTime() * 1000000000);
    }

    public void setNativeCursor(final JmeCursor jmeCursor) {
        if (jmeCursor != null) {
            final ByteBuffer byteBuffer = org.lwjgl.BufferUtils.createByteBuffer(jmeCursor.getImagesData().capacity());
            byteBuffer.asIntBuffer().put(jmeCursor.getImagesData().array());
            final long cursor = glfwCreateCursor(byteBuffer, jmeCursor.getXHotSpot(), jmeCursor.getYHotSpot());
            glfwSetCursor(context.getWindowHandle(), cursor);
        }
    }

    /**
     * Simply converts the GLFW button code to a JME button code. If there is no match it just returns the GLFW button
     * code. Bare in mind GLFW supports 8 different mouse buttons.
     *
     * @param glfwButton the raw GLFW button index.
     * @return the mapped {@link MouseInput} button id.
     */
    private int convertButton(final int glfwButton) {
        if (glfwButton == GLFW_MOUSE_BUTTON_LEFT) {
            return MouseInput.BUTTON_LEFT;
        } else if(glfwButton == GLFW_MOUSE_BUTTON_MIDDLE) {
            return MouseInput.BUTTON_MIDDLE;
        } else if(glfwButton == GLFW_MOUSE_BUTTON_RIGHT) {
            return MouseInput.BUTTON_RIGHT;
        }

        return glfwButton;
    }
}
