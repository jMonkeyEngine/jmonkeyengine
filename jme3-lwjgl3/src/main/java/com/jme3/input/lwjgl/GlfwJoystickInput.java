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

import com.jme3.input.*;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import com.jme3.system.AppSettings;
import org.lwjgl.glfw.GLFWGamepadState;
import static org.lwjgl.glfw.GLFW.*;

/**
 * The LWJGL implementation of {@link JoyInput}.
 *
 * @author Daniel Johansson (dannyjo), Riccardo Balbo
 * @since 3.1
 */
public class GlfwJoystickInput implements JoyInput {

    private static final Logger LOGGER = Logger.getLogger(GlfwJoystickInput.class.getName());

    private final AppSettings settings;

    private RawInputListener listener;

    private final Map<Integer, GlfwJoystick> joysticks = new HashMap<>();

    private final Map<JoystickButton, Boolean> joyButtonPressed = new HashMap<>();

    private boolean initialized = false;

    private GLFWGamepadState gamepadState;

    private float virtualTriggerThreshold;
    
    private boolean xboxStyle;

    public GlfwJoystickInput(AppSettings settings) {
        this.settings = settings;
    }

    @Override
    public void setJoyRumble(final int joyId, final float amount) {
        if (joyId >= joysticks.size()) {
            throw new IllegalArgumentException();
        }
    }

    public void fireJoystickConnectedEvent(int jid) {
        Joystick joystick = joysticks.get(jid);
        ((InputManager)listener).fireJoystickConnectedEvent(joystick);
    }

    public void fireJoystickDisconnectedEvent(int jid) {
        Joystick joystick = joysticks.get(jid);
        ((InputManager)listener).fireJoystickDisconnectedEvent(joystick);
    }

    public void reloadJoysticks() {
        joysticks.clear();
        joyButtonPressed.clear();

        InputManager inputManager = (InputManager) listener;

        Joystick[] joysticks = loadJoysticks(inputManager);
        inputManager.setJoysticks(joysticks);
    }

    @Override
    public Joystick[] loadJoysticks(final InputManager inputManager) {

        for (int i = 0; i < GLFW_JOYSTICK_LAST; i++) {
            if (glfwJoystickPresent(i)) {
                boolean isGlfwGamepad = xboxStyle && glfwJoystickIsGamepad(i);
                final String name = isGlfwGamepad ? glfwGetGamepadName(i) : glfwGetJoystickName(i);
                final GlfwJoystick joystick = new GlfwJoystick(inputManager, this, i, name, isGlfwGamepad);
                joysticks.put(i, joystick);

                if(!isGlfwGamepad){
                    // RAW axis
                    final FloatBuffer floatBuffer = glfwGetJoystickAxes(i);
                    if (floatBuffer == null) continue;

                    int axisIndex = 0;
                    while (floatBuffer.hasRemaining()) {
                        floatBuffer.get();

                        final String logicalId = JoystickCompatibilityMappings.remapAxis(joystick.getName(), convertAxisIndex(axisIndex));
                        final JoystickAxis joystickAxis = new DefaultJoystickAxis(inputManager, joystick, axisIndex, convertAxisIndex(axisIndex), logicalId, true, false, 0.0f);
                        joystick.addAxis(axisIndex, joystickAxis);
                        axisIndex++;
                    }

                    // raw buttons
                    final ByteBuffer byteBuffer = glfwGetJoystickButtons(i);

                    if (byteBuffer != null) {
                        int buttonIndex = 0;
                        while (byteBuffer.hasRemaining()) {
                            byteBuffer.get();

                            final String logicalId = JoystickCompatibilityMappings.remapButton(joystick.getName(), String.valueOf(buttonIndex));
                            final JoystickButton button = new DefaultJoystickButton(inputManager, joystick, buttonIndex, String.valueOf(buttonIndex), logicalId);
                            joystick.addButton(button);
                            joyButtonPressed.put(button, false);
                            buttonIndex++;
                        }
                    }
                } else {
                    // Managed axis

                    final String[] axisNames = {
                        JoystickAxis.X_AXIS,       // 0: LEFT_X
                        JoystickAxis.Y_AXIS,       // 1: LEFT_Y
                        JoystickAxis.Z_AXIS,       // 2: RIGHT_X
                        JoystickAxis.Z_ROTATION,   // 3: RIGHT_Y
                        JoystickAxis.LEFT_TRIGGER, // 4: LEFT_TRIGGER
                        JoystickAxis.RIGHT_TRIGGER // 5: RIGHT_TRIGGER
                    };

                    for (int axisIndex = 0; axisIndex <= GLFW_GAMEPAD_AXIS_LAST; axisIndex++) {
                        final String axisName = axisNames[axisIndex];
                        final String logicalId = axisName;  // no need to remap with JoystickCompatibilityMappings 
                                                            // as glfw already handles remapping
                        final JoystickAxis axis = new DefaultJoystickAxis(inputManager, joystick, axisIndex, axisName, logicalId, true, false, 0.0f);
                        joystick.addAxis(axisIndex, axis);
                    }

                    // Virtual POV axes for D-pad.
                    final JoystickAxis povX = new DefaultJoystickAxis(inputManager, joystick, 6, JoystickAxis.POV_X, JoystickAxis.POV_X, true, false, 0.0f);
                    joystick.addAxis(6, povX);

                    final JoystickAxis povY = new DefaultJoystickAxis(inputManager, joystick, 7, JoystickAxis.POV_Y, JoystickAxis.POV_Y, true, false, 0.0f);
                    joystick.addAxis(7, povY);

                    // managed buttons
                    for (int j = 0; j <= 15; j++) {
                        final String logicalId =  String.valueOf(j);  
                        final String buttonName = logicalId; 
                        final JoystickButton button = new DefaultJoystickButton(inputManager, joystick, j, buttonName, logicalId);
                        joystick.addButton(button);
                        joyButtonPressed.put(button, false);
                    }
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
        gamepadState = GLFWGamepadState.create();
        initialized = true;
        virtualTriggerThreshold = settings.getTriggerToButtonThreshold();
        xboxStyle = settings.isXboxLikeControllerLayout();
    }

    @Override
    public void update() {
        float rawValue, value;
        for (final Map.Entry<Integer, GlfwJoystick> entry : joysticks.entrySet()) {
            if (!glfwJoystickPresent(entry.getKey())) continue;
            if (!entry.getValue().isGlfwGamepad()) {

                // Axes
                final FloatBuffer axisValues = glfwGetJoystickAxes(entry.getKey());

                // if a joystick is added or removed, the callback reloads the joysticks.
                // when the callback is called and reloads the joystick, this iterator may already have started iterating.
                // To avoid a NullPointerException we null-check the axisValues and bytebuffer objects.
                // If the joystick it's iterating over no-longer exists it will return null.

                if (axisValues != null) {
                    for (final JoystickAxis axis : entry.getValue().getAxes()) {
                        rawValue = axisValues.get(axis.getAxisId());
                        value = JoystickCompatibilityMappings.remapAxisRange(axis, rawValue);
                        listener.onJoyAxisEvent(new JoyAxisEvent(axis, value, rawValue));
                    }
                }

                // Buttons
               final ByteBuffer byteBuffer = glfwGetJoystickButtons(entry.getKey());

                if (byteBuffer != null) {
                    for (final JoystickButton button : entry.getValue().getButtons()) {
                        final boolean pressed = byteBuffer.get(button.getButtonId()) == GLFW_PRESS;
                        updateButton(button, pressed);
                    }
                }
            } else {
                if (!glfwGetGamepadState(entry.getKey(), gamepadState)) return;
                Joystick joystick = entry.getValue();

                final FloatBuffer axes = gamepadState.axes();

                // handle axes (skip virtual POV axes 6 & 7)
                for (final JoystickAxis axis : entry.getValue().getAxes()) {
                    final int axisId = axis.getAxisId();
                    if (axisId == 6 || axisId == 7) continue;
                    if (axisId < 0 || axisId > GLFW_GAMEPAD_AXIS_LAST) continue;

                    rawValue = axes.get(axisId);

                    if (axisId == GLFW_GAMEPAD_AXIS_LEFT_TRIGGER || axisId == GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) {
                        rawValue = remapAxisToJme(rawValue);
                    }

                    value = rawValue; // scaling handled by GLFW
                    listener.onJoyAxisEvent(new JoyAxisEvent(axis, value, rawValue));
                }


                // virtual trigger buttons
                if (virtualTriggerThreshold > 0.0f) {
                    final float leftTrigger = remapAxisToJme(axes.get(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER));
                    final float rightTrigger = remapAxisToJme(axes.get(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER));
                    updateButton(joystick.getButton(JoystickButton.BUTTON_6), leftTrigger > virtualTriggerThreshold);
                    updateButton(joystick.getButton(JoystickButton.BUTTON_7), rightTrigger > virtualTriggerThreshold);
                }

                final ByteBuffer buttons = gamepadState.buttons();

             
                for( int btnIndex = 0; btnIndex <= GLFW_GAMEPAD_BUTTON_LAST; btnIndex++) {
                    int glfwButtonIndex = btnIndex;
                    boolean pressed = buttons.get(glfwButtonIndex) == GLFW_PRESS;
                    String jmeButtonIndex = remapButtonToJme(btnIndex);
                    if(jmeButtonIndex==null)   continue;                    
                    JoystickButton button = joystick.getButton(jmeButtonIndex);
                    if (button != null) {
                        updateButton(button, pressed);
                    }
                }

                // D-pad to virtual POV axes
                final boolean dpadUp = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_UP) == GLFW_PRESS;
                final boolean dpadDown = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_DOWN) == GLFW_PRESS;
                final boolean dpadLeft = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_LEFT) == GLFW_PRESS;
                final boolean dpadRight = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT) == GLFW_PRESS;

                final float povX = dpadLeft ? -1f : (dpadRight ? 1f : 0f);
                final float povY = dpadDown ? -1f : (dpadUp ? 1f : 0f);

                final JoystickAxis povXAxis = joystick.getPovXAxis();
                if (povXAxis != null) {
                    listener.onJoyAxisEvent(new JoyAxisEvent(povXAxis, povX, povX));
                }

                final JoystickAxis povYAxis = joystick.getPovYAxis();
                if (povYAxis != null) {
                    listener.onJoyAxisEvent(new JoyAxisEvent(povYAxis, povY, povY));
                }
            }
        }
    }


 
    private static String remapButtonToJme(int glfwButtonIndex) {
        switch (glfwButtonIndex) {
            case GLFW_GAMEPAD_BUTTON_Y:
                return JoystickButton.BUTTON_XBOX_Y;
            case GLFW_GAMEPAD_BUTTON_B:
                return JoystickButton.BUTTON_XBOX_B;
            case GLFW_GAMEPAD_BUTTON_A:
                return JoystickButton.BUTTON_XBOX_A;
            case GLFW_GAMEPAD_BUTTON_X:
                return JoystickButton.BUTTON_XBOX_X;
            case GLFW_GAMEPAD_BUTTON_LEFT_BUMPER:
                return JoystickButton.BUTTON_XBOX_LB;   
            case GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER:
                return JoystickButton.BUTTON_XBOX_RB;
            case GLFW_GAMEPAD_BUTTON_BACK:
                return JoystickButton.BUTTON_XBOX_BACK;
            case GLFW_GAMEPAD_BUTTON_START:
                return JoystickButton.BUTTON_XBOX_START;
            case GLFW_GAMEPAD_BUTTON_LEFT_THUMB:
                return JoystickButton.BUTTON_XBOX_L3;
            case GLFW_GAMEPAD_BUTTON_RIGHT_THUMB:
                return JoystickButton.BUTTON_XBOX_R3;
            case GLFW_GAMEPAD_BUTTON_DPAD_UP:
                return JoystickButton.BUTTON_XBOX_DPAD_UP;
            case GLFW_GAMEPAD_BUTTON_DPAD_DOWN:
                return JoystickButton.BUTTON_XBOX_DPAD_DOWN;
            case GLFW_GAMEPAD_BUTTON_DPAD_LEFT:
                return JoystickButton.BUTTON_XBOX_DPAD_LEFT;
            case GLFW_GAMEPAD_BUTTON_DPAD_RIGHT:
                return JoystickButton.BUTTON_XBOX_DPAD_RIGHT;
            default:
                return null;
        
        }
    }

    private static float remapAxisToJme(float v) {
        if (v < -1f) v = -1f;
        if (v > 1f) v = 1f;
        return (v + 1f) * 0.5f;
    }

    private void updateButton(final JoystickButton button, final boolean pressed) {
        if (button == null) return;
        final Boolean old = joyButtonPressed.get(button);
        if (old == null || old != pressed) {
            joyButtonPressed.put(button, pressed);
            listener.onJoyButtonEvent(new JoyButtonEvent(button, pressed));
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
        private final boolean isGlfwGamepad;
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povAxisX;
        private JoystickAxis povAxisY;

        public GlfwJoystick(final InputManager inputManager, final JoyInput joyInput, final int joyId, final String name, final boolean gamepad) {
            super(inputManager, joyInput, joyId, name);
            this.isGlfwGamepad = gamepad;
        }

        public boolean isGlfwGamepad() {
            return isGlfwGamepad;
        }

        public void addAxis(final int index, final JoystickAxis axis) {
            super.addAxis(axis);

            if (isGlfwGamepad) {
                if (index == GLFW_GAMEPAD_AXIS_LEFT_X) {
                    xAxis = axis;
                } else if (index == GLFW_GAMEPAD_AXIS_LEFT_Y) {
                    yAxis = axis;
                } else if (index == 6) {
                    povAxisX = axis;
                } else if (index == 7) {
                    povAxisY = axis;
                }
            } else {
                if (index == 0) {
                    xAxis = axis;
                    povAxisX = axis;
                } else if (index == 1) {
                    yAxis = axis;
                    povAxisY = axis;
                }
            }
        }

        @Override
        protected void addButton(final JoystickButton button) {
            super.addButton(button);
        }

        @Override
        public JoystickAxis getXAxis() {
            return xAxis;
        }

        @Override
        public JoystickAxis getYAxis() {
            return yAxis;
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
            return xAxis != null ? xAxis.getAxisId() : 0;
        }

        @Override
        public int getYAxisIndex() {
            return yAxis != null ? yAxis.getAxisId() : 1;
        }
    }
}
