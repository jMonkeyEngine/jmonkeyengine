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

import static org.lwjgl.glfw.GLFW.*;
import com.jme3.input.*;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The LWJGL implementation of {@link JoyInput}.
 *
 * @author Daniel Johansson (dannyjo)
 * @since 3.1
 */
public class GlfwJoystickInput implements JoyInput {

    private static final Logger LOGGER = Logger.getLogger(InputManager.class.getName());

    private RawInputListener listener;
    private Map<Integer, GlfwJoystick> joysticks = new HashMap<>();

    private boolean initialized = false;

    @Override
    public void setJoyRumble(final int joyId, final float amount) {
        if (joyId >= joysticks.size()) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Joystick[] loadJoysticks(final InputManager inputManager) {
        for (int i = 0; i < GLFW_JOYSTICK_LAST; i++) {
            if (glfwJoystickPresent(i)) {
                final String name = glfwGetJoystickName(i);
                final GlfwJoystick joystick = new GlfwJoystick(inputManager, this, i, name);
                joysticks.put(i, joystick);

                final FloatBuffer floatBuffer = glfwGetJoystickAxes(i);

                int axisIndex = 0;
                while (floatBuffer.hasRemaining()) {
                    floatBuffer.get();

                    final String logicalId = JoystickCompatibilityMappings.remapComponent(joystick.getName(), convertAxisIndex(axisIndex));
                    final JoystickAxis joystickAxis = new DefaultJoystickAxis(inputManager, joystick, axisIndex, convertAxisIndex(axisIndex), logicalId, true, false, 0.0f);
                    joystick.addAxis(axisIndex, joystickAxis);
                    axisIndex++;
                }

                final ByteBuffer byteBuffer = glfwGetJoystickButtons(i);

                int buttonIndex = 0;
                while (byteBuffer.hasRemaining()) {
                    byteBuffer.get();
                    final String logicalId = JoystickCompatibilityMappings.remapComponent(joystick.getName(), String.valueOf(buttonIndex));
                    joystick.addButton(new DefaultJoystickButton(inputManager, joystick, buttonIndex, String.valueOf(buttonIndex), logicalId));
                    buttonIndex++;
                }
            }
        }

        return joysticks.values().toArray(new GlfwJoystick[joysticks.size()]);
    }

    private String convertAxisIndex(final int index) {
        if (index == 0) {
            return "pov_x";
        } else if (index == 1) {
            return "pov_y";
        } else if (index == 2) {
            return "z";
        } else if (index == 3) {
            return "rz";
        }

        return String.valueOf(index);
    }

    @Override
    public void initialize() {
        initialized = true;
    }

    @Override
    public void update() {
        for (final Map.Entry<Integer, GlfwJoystick> entry : joysticks.entrySet()) {
            // Axes
            final FloatBuffer axisValues = glfwGetJoystickAxes(entry.getKey());

            for (final JoystickAxis axis : entry.getValue().getAxes()) {
                final float value = axisValues.get(axis.getAxisId());
                listener.onJoyAxisEvent(new JoyAxisEvent(axis, value));
            }

            // Buttons
            final ByteBuffer byteBuffer = glfwGetJoystickButtons(entry.getKey());

            for (final JoystickButton button : entry.getValue().getButtons()) {
                final boolean pressed = byteBuffer.get(button.getButtonId()) == GLFW_PRESS;
                listener.onJoyButtonEvent(new JoyButtonEvent(button, pressed));
            }
        }
    }

    @Override
    public void destroy() {
        initialized = false;
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
        return 0;
    }

    protected class GlfwJoystick extends AbstractJoystick {

        private JoystickAxis povAxisX;
        private JoystickAxis povAxisY;

        public GlfwJoystick(final InputManager inputManager, final JoyInput joyInput, final int joyId, final String name) {
            super(inputManager, joyInput, joyId, name);
        }

        public void addAxis(final int index, final JoystickAxis axis) {
            super.addAxis(axis);

            if (index == 0) {
                povAxisX = axis;
            } else if (index == 1) {
                povAxisY = axis;
            }
        }

        @Override
        protected void addButton(final JoystickButton button) {
            super.addButton(button);
        }

        @Override
        public JoystickAxis getXAxis() {
            return povAxisX;
        }

        @Override
        public JoystickAxis getYAxis() {
            return povAxisY;
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return povAxisX;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return povAxisY;
        }

        @Override
        public int getXAxisIndex() {
            return povAxisX.getAxisId();
        }

        @Override
        public int getYAxisIndex() {
            return povAxisY.getAxisId();
        }
    }
}



