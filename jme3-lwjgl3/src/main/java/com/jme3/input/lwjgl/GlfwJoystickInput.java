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
import com.jme3.math.FastMath;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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
    private static final int POV_X_AXIS_ID = 7;
    private static final int POV_Y_AXIS_ID = 8;

    private final AppSettings settings;
    private final Map<Integer, GlfwJoystick> joysticks = new HashMap<>();
    private final Map<JoystickButton, Boolean> joyButtonPressed = new HashMap<>();
    private final Map<JoystickAxis, Float> joyAxisValues = new HashMap<>();

    private boolean initialized = false;
    private float virtualTriggerThreshold;
    private boolean xboxStyle;
    private float globalJitterThreshold = 0f;
    private GLFWGamepadState gamepadState;
    private RawInputListener listener;

    public GlfwJoystickInput(AppSettings settings) {
        this.settings = settings;
        try {
            String path = settings.getSDLGameControllerDBResourcePath();
            if (!path.isBlank()) {
                ByteBuffer bbf = SdlGameControllerDb.getGamecontrollerDb(path);
                if (!glfwUpdateGamepadMappings(bbf)) throw new Exception("Failed to load");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load gamecontrollerdb, fallback to glfw default mappings",
                    e);
        }
    }

    @Override
    public void initialize() {
        gamepadState = GLFWGamepadState.create();

        virtualTriggerThreshold = settings.getJoysticksTriggerToButtonThreshold();
        xboxStyle = settings.getJoysticksMapper().equals(AppSettings.JOYSTICKS_XBOX_LEGACY_MAPPER);
        globalJitterThreshold = settings.getJoysticksAxisJitterThreshold();

        initialized = true;
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
        joyAxisValues.clear();
        InputManager inputManager = (InputManager) listener;
        Joystick[] joysticks = loadJoysticks(inputManager);
        inputManager.setJoysticks(joysticks);
    }

    @Override
    public Joystick[] loadJoysticks(final InputManager inputManager) {

        for (int i = 0; i < GLFW_JOYSTICK_LAST; i++) {
            if (glfwJoystickPresent(i)) {
                boolean isGlfwGamepad = xboxStyle && glfwJoystickIsGamepad(i);

                String name;
                if (isGlfwGamepad) {
                    name = glfwGetGamepadName(i);
                } else {
                    name = glfwGetJoystickName(i);
                    LOGGER.log(Level.WARNING,
                            "Unknown controller detected: {0} - guid: {1}. Fallback to raw input handling",
                            new Object[] { name, glfwGetJoystickGUID(i) });
                }

                GlfwJoystick joystick = new GlfwJoystick(inputManager, this, i, name, isGlfwGamepad);
                joysticks.put(i, joystick);

                if(!isGlfwGamepad){
                    // RAW axis
                    FloatBuffer floatBuffer = glfwGetJoystickAxes(i);
                    if (floatBuffer == null) continue;

                    int axisIndex = 0;
                    while (floatBuffer.hasRemaining()) {
                        floatBuffer.get();

                        String logicalId = JoystickCompatibilityMappings.remapAxis(joystick.getName(),
                                convertAxisIndex(axisIndex));
                        JoystickAxis joystickAxis = new DefaultJoystickAxis(inputManager, joystick, axisIndex,
                                convertAxisIndex(axisIndex), logicalId, true, false, 0.0f);
                        joystick.addAxis(axisIndex, joystickAxis);
                        axisIndex++;
                    }

                    // raw buttons
                    ByteBuffer byteBuffer = glfwGetJoystickButtons(i);

                    if (byteBuffer != null) {
                        int buttonIndex = 0;
                        while (byteBuffer.hasRemaining()) {
                            byteBuffer.get();

                            String logicalId = JoystickCompatibilityMappings.remapButton(joystick.getName(),
                                    String.valueOf(buttonIndex));
                            JoystickButton button = new DefaultJoystickButton(inputManager, joystick,
                                    buttonIndex, String.valueOf(buttonIndex), logicalId);
                            joystick.addButton(button);
                            joyButtonPressed.put(button, false);
                            buttonIndex++;
                        }
                    }
                } else {
                    // Managed axis
                    for (int axisIndex = 0; axisIndex <= GLFW_GAMEPAD_AXIS_LAST; axisIndex++) {
                        String logicalId = remapAxisToJme(axisIndex);
                        if (logicalId == null) continue;
                        String axisName = logicalId; // no need to remap with JoystickCompatibilityMappings as
                                                     // glfw already handles remapping
                        JoystickAxis axis = new DefaultJoystickAxis(inputManager, joystick, axisIndex,
                                axisName, logicalId, true, false, 0.0f);
                        joystick.addAxis(axisIndex, axis);
                    }

                    // Virtual POV axes for D-pad.
                    JoystickAxis povX = new DefaultJoystickAxis(inputManager, joystick, POV_X_AXIS_ID,
                            JoystickAxis.POV_X, JoystickAxis.POV_X, true, false, 0.0f);
                    joystick.addAxis(POV_X_AXIS_ID, povX);

                    JoystickAxis povY = new DefaultJoystickAxis(inputManager, joystick, POV_Y_AXIS_ID,
                            JoystickAxis.POV_Y, JoystickAxis.POV_Y, true, false, 0.0f);
                    joystick.addAxis(POV_Y_AXIS_ID, povY);

                    // managed buttons
                    for (int buttonIndex = 0; buttonIndex <= GLFW_GAMEPAD_BUTTON_LAST; buttonIndex++) {
                        String logicalId = remapButtonToJme(buttonIndex);
                        if (logicalId == null) continue;
                        String buttonName = logicalId;
                        JoystickButton button = new DefaultJoystickButton(inputManager, joystick, buttonIndex,
                                buttonName, logicalId);
                        joystick.addButton(button);
                        joyButtonPressed.put(button, false);
                    }
                }

            }
        }

        return joysticks.values().toArray(new GlfwJoystick[joysticks.size()]);
    }
 
    private String convertAxisIndex(int index) {
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
    public void update() {
        float rawValue, value;
        for (Map.Entry<Integer, GlfwJoystick> entry : joysticks.entrySet()) {
            if (!glfwJoystickPresent(entry.getKey())) continue;
            if (!entry.getValue().isGlfwGamepad()) {

                // Axes
                FloatBuffer axisValues = glfwGetJoystickAxes(entry.getKey());

                // if a joystick is added or removed, the callback reloads the joysticks.
                // when the callback is called and reloads the joystick, this iterator may already have started iterating.
                // To avoid a NullPointerException we null-check the axisValues and bytebuffer objects.
                // If the joystick it's iterating over no-longer exists it will return null.

                if (axisValues != null) {
                    for (JoystickAxis axis : entry.getValue().getAxes()) {
                        rawValue = axisValues.get(axis.getAxisId());
                        value = JoystickCompatibilityMappings.remapAxisRange(axis, rawValue);
                        // listener.onJoyAxisEvent(new JoyAxisEvent(axis, value, rawValue));
                        updateAxis(axis, value, rawValue);
                    }
                }

                // Buttons
                ByteBuffer byteBuffer = glfwGetJoystickButtons(entry.getKey());

                if (byteBuffer != null) {
                    for (JoystickButton button : entry.getValue().getButtons()) {
                        boolean pressed = byteBuffer.get(button.getButtonId()) == GLFW_PRESS;
                        updateButton(button, pressed);
                    }
                }
            } else {
                if (!glfwGetGamepadState(entry.getKey(), gamepadState)) return;
                Joystick joystick = entry.getValue();

                FloatBuffer axes = gamepadState.axes();

                // handle axes (skip virtual POV axes)
                for (JoystickAxis axis : entry.getValue().getAxes()) {
                    int axisId = axis.getAxisId();
                    if (axisId == POV_X_AXIS_ID || axisId == POV_Y_AXIS_ID) continue;
                    if (axisId < 0 || axisId > GLFW_GAMEPAD_AXIS_LAST) continue;

                    rawValue = axes.get(axisId);
                    rawValue = remapAxisValueToJme(axisId, rawValue);
                    value = rawValue; // scaling handled by GLFW

                    updateAxis(axis, value, rawValue);
                }

                // virtual trigger buttons
                if (virtualTriggerThreshold > 0.0f) {
                    float leftTrigger = remapAxisValueToJme(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER,
                            axes.get(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER));
                    float rightTrigger = remapAxisValueToJme(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER,
                            axes.get(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER));
                    updateButton(joystick.getButton(JoystickButton.BUTTON_XBOX_LT),
                            leftTrigger > virtualTriggerThreshold);
                    updateButton(joystick.getButton(JoystickButton.BUTTON_XBOX_RT),
                            rightTrigger > virtualTriggerThreshold);
                }

                ByteBuffer buttons = gamepadState.buttons();
             
                for (int btnIndex = 0; btnIndex <= GLFW_GAMEPAD_BUTTON_LAST; btnIndex++) {
                    String jmeButtonIndex = remapButtonToJme(btnIndex);
                    if (jmeButtonIndex == null) continue;

                    JoystickButton button = joystick.getButton(jmeButtonIndex);
                    if (button == null) continue;

                    boolean pressed = buttons.get(btnIndex) == GLFW_PRESS;
                    updateButton(button, pressed);
                }

                // D-pad to virtual POV axes
                boolean dpadUp = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_UP) == GLFW_PRESS;
                boolean dpadDown = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_DOWN) == GLFW_PRESS;
                boolean dpadLeft = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_LEFT) == GLFW_PRESS;
                boolean dpadRight = buttons.get(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT) == GLFW_PRESS;

                float povX = dpadLeft ? -1f : (dpadRight ? 1f : 0f);
                float povY = dpadDown ? -1f : (dpadUp ? 1f : 0f);

                JoystickAxis povXAxis = joystick.getPovXAxis();
                if (povXAxis != null) {
                    updateAxis(povXAxis, povX, povX);
                }

                JoystickAxis povYAxis = joystick.getPovYAxis();
                if (povYAxis != null) {
                    updateAxis(povYAxis, povY, povY);
                }
            }
        }
    }


 
    private String remapAxisToJme(int glfwAxisIndex) {
        switch (glfwAxisIndex) {
            case GLFW_GAMEPAD_AXIS_LEFT_X:
                return JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X;
            case GLFW_GAMEPAD_AXIS_LEFT_Y:
                return JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y;
            case GLFW_GAMEPAD_AXIS_RIGHT_X:
                return JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X;
            case GLFW_GAMEPAD_AXIS_RIGHT_Y:
                return JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y;
            case GLFW_GAMEPAD_AXIS_LEFT_TRIGGER:
                return JoystickAxis.AXIS_XBOX_LEFT_TRIGGER;
            case GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER:
                return JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER;
            default:
                return null;

        }
    }

    private String remapButtonToJme(int glfwButtonIndex) {
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

    private static float remapAxisValueToJme(int axisId, float v) {
        if (axisId == GLFW_GAMEPAD_AXIS_LEFT_TRIGGER || axisId == GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) {
            if (v < -1f) v = -1f;
            if (v > 1f) v = 1f;
            return (v + 1f) * 0.5f;
        }
        return v;
    }

    private void updateButton(JoystickButton button, boolean pressed) {
        if (button == null) return;
        Boolean old = joyButtonPressed.get(button);
        if (old == null || old != pressed) {
            joyButtonPressed.put(button, pressed);
            listener.onJoyButtonEvent(new JoyButtonEvent(button, pressed));
        }
    }

    private void updateAxis(JoystickAxis axis, float value, float rawValue) {
        if (axis == null) return;
        Float old = joyAxisValues.get(axis);
        float jitter = FastMath.clamp(Math.max(axis.getJitterThreshold(), globalJitterThreshold), 0f, 1f);
        if (old == null || FastMath.abs(old - value) > jitter) {
            joyAxisValues.put(axis, value);
            listener.onJoyAxisEvent(new JoyAxisEvent(axis, value, rawValue));
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
        return (long) (glfwGetTime() * 1000000000);
    }

    private static class GlfwJoystick extends AbstractJoystick {
        private final boolean isGlfwGamepad;
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povAxisX;
        private JoystickAxis povAxisY;

        public GlfwJoystick(InputManager inputManager, JoyInput joyInput, int joyId, String name,
                boolean gamepad) {
            super(inputManager, joyInput, joyId, name);
            this.isGlfwGamepad = gamepad;
        }

        public boolean isGlfwGamepad() {
            return isGlfwGamepad;
        }

        public void addAxis(int index, JoystickAxis axis) {
            super.addAxis(axis);
            if (isGlfwGamepad) {
                switch (index) {
                    case GLFW_GAMEPAD_AXIS_LEFT_X: {
                        xAxis = axis;
                        break;
                    }
                    case GLFW_GAMEPAD_AXIS_LEFT_Y: {
                        yAxis = axis;
                        break;
                    }
                    case POV_X_AXIS_ID: {
                        povAxisX = axis;
                        break;
                    }
                    case POV_Y_AXIS_ID: {
                        povAxisY = axis;
                        break;
                    }
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
        protected void addButton(JoystickButton button) {
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
