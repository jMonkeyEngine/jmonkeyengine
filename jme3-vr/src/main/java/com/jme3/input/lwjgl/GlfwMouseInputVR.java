package com.jme3.input.lwjgl;

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

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.system.lwjgl.LwjglWindowVR;
import com.jme3.util.BufferUtils;

import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

/**
 * Captures mouse input using GLFW callbacks. It then temporarily stores these in event queues which are processed in the
 * {@link #update()} method. Due to some of the GLFW button id's there is a conversion method in this class which will
 * convert the GLFW left, middle and right mouse button to JME3 left, middle and right button codes.<br><br>
 * This class support modifications dedicated to VR rendering.
 * @author Daniel Johansson (dannyjo)
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class GlfwMouseInputVR implements MouseInput {

    private static final Logger logger = Logger.getLogger(GlfwMouseInputVR.class.getName());

    private static final int WHEEL_SCALE = 120;

    private LwjglWindowVR context;
    private RawInputListener listener;
    private boolean cursorVisible = true;
    private int mouseX, xDelta;
    private int mouseY, yDelta;
    private int mouseWheel;
    private boolean initialized;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWScrollCallback scrollCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private Queue<MouseMotionEvent> mouseMotionEvents = new LinkedList<MouseMotionEvent>();
    private Queue<MouseButtonEvent> mouseButtonEvents = new LinkedList<MouseButtonEvent>();

    private Map<JmeCursor, Long> jmeToGlfwCursorMap = new HashMap<JmeCursor, Long>();

    /**
     * Create a new mouse input attached to the given {@link LwjglWindowVR context}.
     * @param context the context to which to attach the input.
     */
    public GlfwMouseInputVR(LwjglWindowVR context) {
        this.context = context;
    }

    private void onCursorPos(long window, double xpos, double ypos) {
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

    public void initialize() {
        glfwSetCursorPosCallback(context.getWindowHandle(), cursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                onCursorPos(window, xpos, ypos);
            }
        });

        glfwSetScrollCallback(context.getWindowHandle(), scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(final long window, final double xOffset, final double yOffset) {
                onWheelScroll(window, xOffset, yOffset * WHEEL_SCALE);
            }
        });

        glfwSetMouseButtonCallback(context.getWindowHandle(), mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(final long window, final int button, final int action, final int mods) {
                onMouseButton(window, button, action, mods);
            }
        });

        setCursorVisible(cursorVisible);
        logger.fine("Mouse created.");
        initialized = true;
    }

    /**
     * Set the position of the cursor on the display.
     * @param x the x position of the cursor (pixel).
     * @param y the y position of the cursor (pixel).
     */
    public void setCursorPosition(int x, int y) {
        if (!context.isRenderable()) {
            return;
        }
        
        glfwSetCursorPos(context.getWindowHandle(), x, y);	
    }
    
    /**
     * Hide the active cursor within the display.
     */
    public void hideActiveCursor() {
        if (!context.isRenderable()) {
            return;
        }

        if (cursorVisible) {
            glfwSetInputMode(context.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);            
        }
    }
    
    /**
     * Get the last delta in x (pixel).
     * @return the last delta in x (pixel).
     * @see #getLastDeltaY()
     */
    public int getLastDeltaX() {
        return xDelta;
    }
    
    /**
     * Get the last delta in y (pixel).
     * @return the last delta in y (pixel).
     * @see #getLastDeltaX()
     */
    public int getLastDeltaY() {
        return yDelta;
    }
    
    /**
     * Clear the last x and y deltas.
     * @see #getLastDeltaX()
     * @see #getLastDeltaY()
     */
    public void clearDeltas() {
        xDelta = 0;
        yDelta = 0;
    }
    
    /**
     * Check if the input is initialized.
     * @return <code>true</code> if the input is initialized and <code>false</code> otherwise.
     */
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public int getButtonCount() {
        return GLFW_MOUSE_BUTTON_LAST + 1;
    }

    @Override
    public void update() {
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
        
        cursorPosCallback.free();
        scrollCallback.free();
        mouseButtonCallback.free();
        
        for (long glfwCursor : jmeToGlfwCursorMap.values()) {
            glfwDestroyCursor(glfwCursor);
        }

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

    private long createGlfwCursor(JmeCursor jmeCursor) {
        GLFWImage glfwImage = new GLFWImage(BufferUtils.createByteBuffer(GLFWImage.SIZEOF));

        // TODO: currently animated cursors are not supported
        IntBuffer imageData = jmeCursor.getImagesData();
        ByteBuffer buf = BufferUtils.createByteBuffer(imageData.capacity() * 4);
        buf.asIntBuffer().put(imageData);

        glfwImage.set(jmeCursor.getWidth(), jmeCursor.getHeight(), buf);

        return glfwCreateCursor(glfwImage, jmeCursor.getXHotSpot(), jmeCursor.getYHotSpot());
    }

    public void setNativeCursor(JmeCursor jmeCursor) {
        if (jmeCursor != null) {
            Long glfwCursor = jmeToGlfwCursorMap.get(jmeCursor);

            if (glfwCursor == null) {
                glfwCursor = createGlfwCursor(jmeCursor);
                jmeToGlfwCursorMap.put(jmeCursor, glfwCursor);
            }

            glfwSetCursor(context.getWindowHandle(), glfwCursor);
        } else {
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
