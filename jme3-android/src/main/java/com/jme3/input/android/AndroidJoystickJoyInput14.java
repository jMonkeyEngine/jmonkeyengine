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
package com.jme3.input.android;

import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.jme3.input.AbstractJoystick;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.DefaultJoystickButton;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.JoystickCompatibilityMappings;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class that creates and manages Android inputs for physical gamepads/joysticks.
 *
 * @author iwgeric
 */
public class AndroidJoystickJoyInput14 {
    private static final Logger logger = Logger.getLogger(AndroidJoystickJoyInput14.class.getName());

    private AndroidJoyInput joyInput;
    private Map<Integer, AndroidJoystick> joystickIndex = new HashMap<>();

    private static int[] AndroidGamepadButtons = {
            // Dpad buttons
            KeyEvent.KEYCODE_DPAD_UP,        KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,      KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,

            // pressing joystick down
            KeyEvent.KEYCODE_BUTTON_THUMBL,  KeyEvent.KEYCODE_BUTTON_THUMBR,

            // buttons
            KeyEvent.KEYCODE_BUTTON_A,       KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,       KeyEvent.KEYCODE_BUTTON_Y,

            // buttons on back of device
            KeyEvent.KEYCODE_BUTTON_L1,      KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,      KeyEvent.KEYCODE_BUTTON_R2,

            // start / select buttons
            KeyEvent.KEYCODE_BUTTON_START,   KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE,

    };

    public AndroidJoystickJoyInput14(AndroidJoyInput joyInput) {
        this.joyInput = joyInput;
    }


    public void pauseJoysticks() {

    }

    public void resumeJoysticks() {

    }

    public void destroy() {

    }

    public List<Joystick> loadJoysticks(int joyId, InputManager inputManager) {
        logger.log(Level.INFO, "loading Joystick devices");
        ArrayList<Joystick> joysticks = new ArrayList<>();
        joysticks.clear();
        joystickIndex.clear();

        ArrayList<Integer> gameControllerDeviceIds = new ArrayList<>();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "deviceId[{0}] sources: {1}", new Object[]{deviceId, sources});
            }

            // Verify that the device has gamepad buttons, control sticks, or both.
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                    ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                // This device is a game controller. Store its device ID.
                if (!gameControllerDeviceIds.contains(deviceId)) {
                    gameControllerDeviceIds.add(deviceId);
                    logger.log(Level.FINE, "Attempting to create joystick for device: {0}", dev);
                    // Create an AndroidJoystick and store the InputDevice, so we
                    // can later convert the input from the InputDevice to the
                    // appropriate jME Joystick event.
                    AndroidJoystick joystick = new AndroidJoystick(inputManager,
                                                                joyInput,
                                                                dev,
                                                                joyId+joysticks.size(),
                                                                dev.getName());
                    joystickIndex.put(deviceId, joystick);
                    joysticks.add(joystick);

                    // Each analog input is reported as a MotionRange
                    // The axis number corresponds to the type of axis
                    // The AndroidJoystick.addAxis(MotionRange) converts the axis
                    // type reported by Android into the jME Joystick axis
                    List<MotionRange> motionRanges = dev.getMotionRanges();
                    for (MotionRange motionRange: motionRanges) {
                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, "motion range: {0}", motionRange);
                            logger.log(Level.INFO, "axis: {0}", motionRange.getAxis());
                        }
                        JoystickAxis axis = joystick.addAxis(motionRange);
                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, "added axis: {0}", axis);
                        }
                    }

                    // InputDevice has a method for determining if a keyCode is
                    // supported (InputDevice  public boolean[] hasKeys (int... keys)).
                    // But this method wasn't added until rev 19 (Android 4.4)
                    // Therefore, we only can query the entire device and see if
                    // any InputDevice supports the keyCode.  This may result in
                    // buttons being configured that don't exist on the specific
                    // device, but I haven't found a better way yet.
                    for (int keyCode: AndroidGamepadButtons) {
                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, "button[{0}]: {1}",
                                    new Object[]{keyCode, KeyCharacterMap.deviceHasKey(keyCode)});
                        }
                        if (KeyCharacterMap.deviceHasKey(keyCode)) {
                            // add button even though we aren't sure if the button
                            // actually exists on this InputDevice
                            logger.log(Level.INFO, "button[{0}] exists somewhere", keyCode);
                            JoystickButton button = joystick.addButton(keyCode);
                            logger.log(Level.INFO, "added button: {0}", button);
                        }
                    }

                }
            }
        }

        return joysticks;
    }

    public boolean onGenericMotion(MotionEvent event) {
        boolean consumed = false;
        float rawValue, value;
//        logger.log(Level.INFO, "onGenericMotion event: {0}", event);
        event.getDeviceId();
        event.getSource();
//        logger.log(Level.INFO, "deviceId: {0}, source: {1}", new Object[]{event.getDeviceId(), event.getSource()});
        AndroidJoystick joystick = joystickIndex.get(event.getDeviceId());
        if (joystick != null) {
            for (int androidAxis: joystick.getAndroidAxes()) {
                String axisName = MotionEvent.axisToString(androidAxis);
                rawValue = event.getAxisValue(androidAxis);
                value = JoystickCompatibilityMappings.remapAxisRange(joystick.getAxis(androidAxis), rawValue);
                int action = event.getAction();
                if (action == MotionEvent.ACTION_MOVE) {
//                    logger.log(Level.INFO, "MOVE axis num: {0}, axisName: {1}, value: {2}",
//                            new Object[]{androidAxis, axisName, value});
                    JoystickAxis axis = joystick.getAxis(androidAxis);
                    if (axis != null) {
//                        logger.log(Level.INFO, "MOVE axis num: {0}, axisName: {1}, value: {2}, deadzone: {3}",
//                                new Object[]{androidAxis, axisName, value, axis.getDeadZone()});
                        JoyAxisEvent axisEvent = new JoyAxisEvent(axis, value, rawValue);
                        joyInput.addEvent(axisEvent);
                        consumed = true;
                    } else {
//                        logger.log(Level.INFO, "axis was null for axisName: {0}", axisName);
                    }
                } else {
//                    logger.log(Level.INFO, "action: {0}", action);
                }
            }
        }

        return consumed;
    }

    public boolean onKey(KeyEvent event) {
        boolean consumed = false;
//        logger.log(Level.INFO, "onKey event: {0}", event);

        event.getDeviceId();
        event.getSource();
        AndroidJoystick joystick = joystickIndex.get(event.getDeviceId());
        if (joystick != null) {
            JoystickButton button = joystick.getButton(event.getKeyCode());
            boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
            if (button != null) {
                JoyButtonEvent buttonEvent = new JoyButtonEvent(button, pressed);
                joyInput.addEvent(buttonEvent);
                consumed = true;
            } else {
                JoystickButton newButton = joystick.addButton(event.getKeyCode());
                JoyButtonEvent buttonEvent = new JoyButtonEvent(newButton, pressed);
                joyInput.addEvent(buttonEvent);
                consumed = true;
            }
        }

        return consumed;
    }

    protected class AndroidJoystick extends AbstractJoystick {

        private JoystickAxis nullAxis;
        private InputDevice device;
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povX;
        private JoystickAxis povY;
        private Map<Integer, JoystickAxis> axisIndex = new HashMap<>();
        private Map<Integer, JoystickButton> buttonIndex = new HashMap<>();

        public AndroidJoystick( InputManager inputManager, JoyInput joyInput, InputDevice device,
                               int joyId, String name ) {
            super( inputManager, joyInput, joyId, name );

            this.device = device;

            this.nullAxis = new DefaultJoystickAxis( getInputManager(), this, -1,
                                                     "Null", "null", false, false, 0 );
            this.xAxis = nullAxis;
            this.yAxis = nullAxis;
            this.povX = nullAxis;
            this.povY = nullAxis;
        }

        protected JoystickAxis getAxis(int androidAxis) {
            return axisIndex.get(androidAxis);
        }

        protected Set<Integer> getAndroidAxes() {
            return axisIndex.keySet();
        }

        protected JoystickButton getButton(int keyCode) {
            return buttonIndex.get(keyCode);
        }

        protected JoystickButton addButton( int keyCode ) {

//            logger.log(Level.FINE, "Adding button: {0}", keyCode);

            String name = KeyEvent.keyCodeToString(keyCode);
            String original = KeyEvent.keyCodeToString(keyCode);
            // A/B/X/Y buttons
            if (keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
                original = JoystickButton.BUTTON_0;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                original = JoystickButton.BUTTON_1;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                original = JoystickButton.BUTTON_2;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_X) {
                original = JoystickButton.BUTTON_3;
            // Front buttons  Some of these have the top ones and the bottoms ones flipped.
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
                original = JoystickButton.BUTTON_4;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
                original = JoystickButton.BUTTON_5;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
                original = JoystickButton.BUTTON_6;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
                original = JoystickButton.BUTTON_7;
//            // Dpad buttons
//            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//                original = JoystickButton.BUTTON_8;
//            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//                original = JoystickButton.BUTTON_9;
//            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//                original = JoystickButton.BUTTON_8;
//            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
//                original = JoystickButton.BUTTON_9;
            // Select and start buttons
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT) {
                original = JoystickButton.BUTTON_8;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_START) {
                original = JoystickButton.BUTTON_9;
            // Joystick push buttons
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_THUMBL) {
                original = JoystickButton.BUTTON_10;
            } else if (keyCode == KeyEvent.KEYCODE_BUTTON_THUMBR) {
                original = JoystickButton.BUTTON_11;
            }

            String logicalId = JoystickCompatibilityMappings.remapButton( getName(), original );
            if (logger.isLoggable(Level.FINE) && !Objects.equals(logicalId, original)) {
                logger.log(Level.FINE, "Remapped: {0} to: {1}",
                        new Object[]{original, logicalId});
            }

            JoystickButton button = new DefaultJoystickButton( getInputManager(), this, getButtonCount(),
                                                               name, logicalId );
            addButton(button);
            buttonIndex.put( keyCode, button );
            return button;
        }

        protected JoystickAxis addAxis(MotionRange motionRange) {

            String name = MotionEvent.axisToString(motionRange.getAxis());

            String original = MotionEvent.axisToString(motionRange.getAxis());
            if (motionRange.getAxis() == MotionEvent.AXIS_X) {
                original = JoystickAxis.X_AXIS;
            } else if (motionRange.getAxis() == MotionEvent.AXIS_Y) {
                original = JoystickAxis.Y_AXIS;
            } else if (motionRange.getAxis() == MotionEvent.AXIS_Z) {
                original = JoystickAxis.Z_AXIS;
            } else if (motionRange.getAxis() == MotionEvent.AXIS_RZ) {
                original = JoystickAxis.Z_ROTATION;
            } else if (motionRange.getAxis() == MotionEvent.AXIS_HAT_X) {
                original = JoystickAxis.POV_X;
            } else if (motionRange.getAxis() == MotionEvent.AXIS_HAT_Y) {
                original = JoystickAxis.POV_Y;
            }
            String logicalId = JoystickCompatibilityMappings.remapAxis( getName(), original );
            if (logger.isLoggable(Level.FINE) && !Objects.equals(logicalId, original)) {
                logger.log(Level.FINE, "Remapped: {0} to: {1}",
                        new Object[]{original, logicalId});
            }

            JoystickAxis axis = new DefaultJoystickAxis(getInputManager(),
                                                this,
                                                getAxisCount(),
                                                name,
                                                logicalId,
                                                true,
                                                true,
                                                motionRange.getFlat());

            if (motionRange.getAxis() == MotionEvent.AXIS_X) {
                xAxis = axis;
            }
            if (motionRange.getAxis() == MotionEvent.AXIS_Y) {
                yAxis = axis;
            }
            if (motionRange.getAxis() == MotionEvent.AXIS_HAT_X) {
                povX = axis;
            }
            if (motionRange.getAxis() == MotionEvent.AXIS_HAT_Y) {
                povY = axis;
            }

            addAxis(axis);
            axisIndex.put(motionRange.getAxis(), axis);
            return axis;
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
            return povX;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return povY;
        }

        @Override
        public int getXAxisIndex(){
            return xAxis.getAxisId();
        }

        @Override
        public int getYAxisIndex(){
            return yAxis.getAxisId();
        }
    }
}
