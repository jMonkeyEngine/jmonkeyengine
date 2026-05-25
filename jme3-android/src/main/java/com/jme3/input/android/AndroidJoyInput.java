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

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.virtual.VirtualJoystick;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class that manages various joystick devices.  Joysticks can be many forms
 * including a simulated joystick to communicate the device orientation as well
 * as physical joysticks. <br>
 * This class manages all the joysticks and feeds the inputs from each back
 * to jME's InputManager.
 *
 * This handler also supports redirecting joystick.rumble(rumbleAmount) to the
 * Android device vibrator if AppSettings#setOnDeviceJoystickRumble(boolean) is
 * enabled and the device has a built-in vibrate motor.
 *
 * Because Android does not allow for the user to define the intensity of the
 * vibration, the rumble amount (ie strength) is converted into vibration pulses
 * The stronger the strength amount, the shorter the delay between pulses.  If
 * amount is 1, then the vibration stays on the whole time.  If amount is 0.5,
 * the vibration will a pulse of equal parts vibration and delay.
 * To turn off vibration, set rumble amount to 0.
 *
 * MainActivity needs the following line to enable Joysticks on Android platforms
 *    joystickEventsEnabled = true;
 * This is done to allow for battery conservation when sensor data or joysticks
 * are not required by the application.
 *
 * {@code
 * To use the joystick rumble feature, the following line needs to be
 * added to the Android Manifest File
 *     <uses-permission android:name="android.permission.VIBRATE"/>
 * }
 * @author iwgeric
 */
public class AndroidJoyInput implements JoyInput {
    private static final Logger logger = Logger.getLogger(AndroidJoyInput.class.getName());
    public static boolean disableSensors = false;

    protected AndroidInputHandler inputHandler;
    protected List<Joystick> joystickList = new ArrayList<>();
//    private boolean dontSendHistory = false;


    // Internal
    private boolean initialized = false;
    private RawInputListener listener = null;
    private ConcurrentLinkedQueue<InputEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private AndroidSensorJoyInput sensorJoyInput;
    private boolean onDeviceJoystickRumble = false;
    private String virtualJoystickMode = AppSettings.VIRTUAL_JOYSTICK_AUTO_MINIMIZED;
    private boolean useJoysticks = true;
    private boolean physicalJoystickAvailable = false;
    private boolean keyboardSuppressedAutoJoystick = false;
    private VirtualJoystick virtualJoystick;

    public AndroidJoyInput(AndroidInputHandler inputHandler) {
        this.inputHandler = inputHandler;
        sensorJoyInput = new AndroidSensorJoyInput(this);
    }

    public void setView(GLSurfaceView view) {
        if (sensorJoyInput != null) {
            sensorJoyInput.setView(view);
        }
    }

    public void loadSettings(AppSettings settings) {
        onDeviceJoystickRumble = settings.isOnDeviceJoystickRumble();
        virtualJoystickMode = settings.getVirtualJoystickMode();
        useJoysticks = settings.useJoysticks();
    }

    boolean isOnDeviceJoystickRumble() {
        return onDeviceJoystickRumble;
    }

    public void addEvent(InputEvent event) {
        eventQueue.add(event);
    }

    /**
     * Pauses the joystick device listeners to save battery life if they are not needed.
     * Used to pause when the activity pauses
     */
    public void pauseJoysticks() {
        if (sensorJoyInput != null) {
            sensorJoyInput.pauseSensors();
        }
        if (onDeviceJoystickRumble) {
            JmeSystem.stopRumble();
        }
        if (virtualJoystick != null) {
            virtualJoystick.onPointerCancel(0L);
        }

    }

    /**
     * Resumes the joystick device listeners.
     * Used to resume when the activity comes to the top of the stack
     */
    public void resumeJoysticks() {
        if (sensorJoyInput != null) {
            sensorJoyInput.resumeSensors();
        }

    }

    @Override
    public void initialize() {
        initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void destroy() {
        initialized = false;

        if (sensorJoyInput != null) {
            sensorJoyInput.destroy();
        }

        setView(null);
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }

    @Override
    public void setJoyRumble(int joyId, float amountHigh, float amountLow, float duration) {
        if (onDeviceJoystickRumble && JmeSystem.isDeviceRumbleSupported()) {
            JmeSystem.rumble(amountHigh, amountLow, duration);
        }
    }

    @Override
    public void stopJoyRumble(int joyId) {
        if (onDeviceJoystickRumble && JmeSystem.isDeviceRumbleSupported()) {
            JmeSystem.stopRumble();
        }
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "loading joysticks for {0}", this.getClass().getName());
        }
        joystickList.clear();
        if (!disableSensors) {
            joystickList.add(sensorJoyInput.loadJoystick(joystickList.size(), inputManager));
        }
        physicalJoystickAvailable = false;
        if (shouldCreateVirtualJoystick()) {
            virtualJoystick = new VirtualJoystick(inputManager, this, joystickList.size());
            updateVirtualJoystickAutoVisibility();
            joystickList.add(virtualJoystick);
        } else {
            virtualJoystick = null;
        }
        return joystickList.toArray( new Joystick[joystickList.size()] );
    }

    public boolean onTouch(MotionEvent event) {
        if (virtualJoystick == null || inputHandler.getView() == null) {
            return false;
        }

        boolean consumed = false;
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        long time = event.getEventTime();

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                consumed = virtualJoystick.onPointerDown(event.getPointerId(pointerIndex),
                        toJmeX(event.getX(pointerIndex)), toJmeY(event.getY(pointerIndex)), time);
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                consumed = virtualJoystick.onPointerUp(event.getPointerId(pointerIndex),
                        toJmeX(event.getX(pointerIndex)), toJmeY(event.getY(pointerIndex)), time);
                break;
            case MotionEvent.ACTION_CANCEL:
                consumed = virtualJoystick.onPointerCancel(time);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    consumed = virtualJoystick.onPointerMove(event.getPointerId(i),
                            toJmeX(event.getX(i)), toJmeY(event.getY(i)), time) || consumed;
                }
                break;
            default:
                break;
        }
        return consumed;
    }

    public void onKeyboardInput() {
        if (isAutoMode(virtualJoystickMode)) {
            keyboardSuppressedAutoJoystick = true;
            updateVirtualJoystickAutoVisibility();
        }
    }

    @Override
    public void update() {
        if (sensorJoyInput != null) {
            sensorJoyInput.update();
        }

        if (listener != null) {
            InputEvent inputEvent;

            while ((inputEvent = eventQueue.poll()) != null) {
                if (inputEvent instanceof JoyAxisEvent) {
                    listener.onJoyAxisEvent((JoyAxisEvent)inputEvent);
                } else if (inputEvent instanceof JoyButtonEvent) {
                    listener.onJoyButtonEvent((JoyButtonEvent)inputEvent);
                }
            }
        }
        if (virtualJoystick != null) {
            virtualJoystick.dispatchEvents(listener);
        }

    }

    private float toJmeX(float x) {
        return inputHandler.touchInput.getJmeX(x);
    }

    private float toJmeY(float y) {
        return inputHandler.touchInput.invertY(inputHandler.touchInput.getJmeY(y));
    }

    protected void setPhysicalJoystickAvailable(boolean available) {
        physicalJoystickAvailable = available;
        updateVirtualJoystickAutoVisibility();
    }

    private boolean shouldCreateVirtualJoystick() {
        return useJoysticks && !AppSettings.VIRTUAL_JOYSTICK_DISABLED.equals(virtualJoystickMode);
    }

    private void updateVirtualJoystickAutoVisibility() {
        if (virtualJoystick == null) {
            return;
        }
        boolean active = isEnabledMode(virtualJoystickMode)
                || (isAutoMode(virtualJoystickMode)
                && !physicalJoystickAvailable
                && !keyboardSuppressedAutoJoystick);
        virtualJoystick.setEnabled(active);
        if (active && isMinimizedMode(virtualJoystickMode)) {
            virtualJoystick.setShown(false);
        }
    }

    private static boolean isEnabledMode(String mode) {
        return AppSettings.VIRTUAL_JOYSTICK_ENABLED.equals(mode)
                || AppSettings.VIRTUAL_JOYSTICK_ENABLED_MINIMIZED.equals(mode);
    }

    private static boolean isAutoMode(String mode) {
        return AppSettings.VIRTUAL_JOYSTICK_AUTO.equals(mode)
                || AppSettings.VIRTUAL_JOYSTICK_AUTO_MINIMIZED.equals(mode);
    }

    private static boolean isMinimizedMode(String mode) {
        return AppSettings.VIRTUAL_JOYSTICK_ENABLED_MINIMIZED.equals(mode)
                || AppSettings.VIRTUAL_JOYSTICK_AUTO_MINIMIZED.equals(mode);
    }

}
