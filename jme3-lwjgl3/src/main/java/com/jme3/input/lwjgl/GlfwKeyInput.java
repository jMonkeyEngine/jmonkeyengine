/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.system.lwjgl.LwjglWindow;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * The LWJGL implementation of {@link KeyInput}.
 */
public class GlfwKeyInput implements KeyInput {

    private static final Logger logger = Logger.getLogger(GlfwKeyInput.class.getName());

    /**
     * The queue of key events.
     */
    private final Queue<KeyInputEvent> keyInputEvents = new ArrayDeque<>();

    /**
     * The LWJGL context.
     */
    private final LwjglWindow context;

    /**
     * The key callback.
     */
    private GLFWKeyCallback keyCallback;

    /**
     * The char callback.
     */
    private GLFWCharCallback charCallback;

    /**
     * The raw input listener.
     */
    private RawInputListener listener;

    private boolean initialized;

    public GlfwKeyInput(final LwjglWindow context) {
        this.context = context;
    }

    @Override
    public void initialize() {
        if (!context.isRenderable()) {
            return;
        }
        initCallbacks();

        initialized = true;
        logger.fine("Keyboard created.");
    }

    /**
     * Re-initializes the key input context window specific callbacks
     */
    public void resetContext() {
        if (!context.isRenderable()) {
            return;
        }

        closeCallbacks();
        initCallbacks();
    }

    private void closeCallbacks() {
        keyCallback.close();
        charCallback.close();
    }

    /**
     * Determine the name of the specified key in the current system language.
     *
     * @param jmeKey the keycode from {@link com.jme3.input.KeyInput}
     * @return the name of the key, or null if unknown
     */
    @Override
    public String getKeyName(int jmeKey) {
        int glfwKey = GlfwKeyMap.fromJmeKeyCode(jmeKey);
        if (glfwKey == GLFW_KEY_UNKNOWN) {
            return null;
        }

        String result = glfwGetKeyName(glfwKey, 0);
        return result;
    }

    private void initCallbacks() {
        glfwSetKeyCallback(context.getWindowHandle(), keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(final long window, final int key, final int scancode, final int action, final int mods) {

                if (key < 0 || key > GLFW_KEY_LAST) {
                    return;
                }

                int jmeKey = GlfwKeyMap.toJmeKeyCode(key);

                final KeyInputEvent event = new KeyInputEvent(jmeKey, '\0', GLFW_PRESS == action, GLFW_REPEAT == action);
                event.setTime(getInputTimeNanos());

                keyInputEvents.add(event);
            }
        });

        glfwSetCharCallback(context.getWindowHandle(), charCallback = new GLFWCharCallback() {
            @Override
            public void invoke(final long window, final int codepoint) {

                final char keyChar = (char) codepoint;

                final KeyInputEvent pressed = new KeyInputEvent(KeyInput.KEY_UNKNOWN, keyChar, true, false);
                pressed.setTime(getInputTimeNanos());

                keyInputEvents.add(pressed);

                final KeyInputEvent released = new KeyInputEvent(KeyInput.KEY_UNKNOWN, keyChar, false, false);
                released.setTime(getInputTimeNanos());

                keyInputEvents.add(released);
            }
        });
    }

    public int getKeyCount() {
        // This might not be correct
        return GLFW_KEY_LAST - GLFW_KEY_SPACE;
    }

    @Override
    public void update() {
        if (!context.isRenderable()) {
            return;
        }

        while (!keyInputEvents.isEmpty()) {
            listener.onKeyEvent(keyInputEvents.poll());
        }
    }

    @Override
    public void destroy() {
        if (!context.isRenderable()) {
            return;
        }

        closeCallbacks();
        logger.fine("Keyboard destroyed.");
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInputListener(final RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return (long) (glfwGetTime() * 1000000000);
    }
}
