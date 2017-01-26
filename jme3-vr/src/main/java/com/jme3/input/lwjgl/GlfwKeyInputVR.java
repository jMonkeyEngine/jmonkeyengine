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

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.system.lwjgl.LwjglWindowVR;

import org.lwjgl.glfw.GLFWKeyCallback;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A key input that wraps GLFW underlying components.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 *
 */
public class GlfwKeyInputVR implements KeyInput {

    private static final Logger logger = Logger.getLogger(GlfwKeyInput.class.getName());

    private LwjglWindowVR context;
    private RawInputListener listener;
    private boolean initialized, shift_pressed;
    private GLFWKeyCallback keyCallback;
    private Queue<KeyInputEvent> keyInputEvents = new LinkedList<KeyInputEvent>();

    /**
     * Create a new input attached to the given {@link LwjglWindowVR context}.
     * @param context the context to attach to this input.
     */
    public GlfwKeyInputVR(LwjglWindowVR context) {
        this.context = context;
    }

    public void initialize() {
        if (!context.isRenderable()) {
            return;
        }

        glfwSetKeyCallback(context.getWindowHandle(), keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                scancode = GlfwKeyMap.toJmeKeyCode(key);
                if( key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT ) {
                    shift_pressed = (action == GLFW_PRESS);
                } else if( key >= 'A' && key <= 'Z' && !shift_pressed ) {
                    key += 32; // make lowercase
                } else if( key >= 'a' && key <= 'z' && shift_pressed ) {
                    key -= 32; // make uppercase
                }
                final KeyInputEvent evt = new KeyInputEvent(scancode, (char) key, GLFW_PRESS == action, GLFW_REPEAT == action);
                evt.setTime(getInputTimeNanos());
                keyInputEvents.add(evt);
            }
        });

        glfwSetInputMode(context.getWindowHandle(), GLFW_STICKY_KEYS, 1);

        initialized = true;
        logger.fine("Keyboard created.");
    }

    /**
     * Get the key count.
     * @return the key count.
     */
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

        keyCallback.free();
        
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
