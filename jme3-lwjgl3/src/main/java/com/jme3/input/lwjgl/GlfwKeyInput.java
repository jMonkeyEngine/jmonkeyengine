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

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.system.lwjgl.LwjglWindow;

import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;

public class GlfwKeyInput implements KeyInput {

    private static final Logger logger = Logger.getLogger(GlfwKeyInput.class.getName());

    private LwjglWindow context;
    private RawInputListener listener;
    private boolean initialized;
    private GLFWKeyCallback keyCallback;
    private GLFWCharCallback charCallback;
    private Queue<KeyInputEvent> keyInputEvents = new LinkedList<KeyInputEvent>();

    public GlfwKeyInput(LwjglWindow context) {
        this.context = context;
    }

    public void initialize() {
        if (!context.isRenderable()) {
            return;
        }

        glfwSetKeyCallback(context.getWindowHandle(), keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {

                if (key < 0 || key > GLFW_KEY_LAST) {
                    return;
                }

                int jmeKey = GlfwKeyMap.toJmeKeyCode(key);

                final KeyInputEvent event = new KeyInputEvent(jmeKey, '\0', GLFW_PRESS == action, GLFW_REPEAT == action);
                event.setTime(getInputTimeNanos());

                keyInputEvents.add(event);
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

        glfwSetCharCallback(context.getWindowHandle(), charCallback = new GLFWCharCallback() {

            @Override
            public void invoke(long window, int codepoint) {

                final char keyChar = (char) codepoint;

                final KeyInputEvent pressed = new KeyInputEvent(KeyInput.KEY_UNKNOWN, keyChar, true, false);
                pressed.setTime(getInputTimeNanos());

                keyInputEvents.add(pressed);

                final KeyInputEvent released = new KeyInputEvent(KeyInput.KEY_UNKNOWN, keyChar, false, false);
                released.setTime(getInputTimeNanos());

                keyInputEvents.add(released);
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

        initialized = true;
        logger.fine("Keyboard created.");
    }

    public int getKeyCount() {
        // This might not be correct
        return GLFW_KEY_LAST - GLFW_KEY_SPACE;
    }

    public void update() {
        if (!context.isRenderable()) {
            return;
        }

        while (!keyInputEvents.isEmpty()) {
            listener.onKeyEvent(keyInputEvents.poll());
        }
    }

    public void destroy() {
        if (!context.isRenderable()) {
            return;
        }

        keyCallback.close();
        charCallback.close();
        logger.fine("Keyboard destroyed.");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return (long) (glfwGetTime() * 1000000000);
    }
}
