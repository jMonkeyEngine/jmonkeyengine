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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Vibrator;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class that manages various joystick devices.  Joysticks can be many forms
 * including a simulated joystick to communicate the device orientation as well
 * as physical gamepads. <br>
 * This class manages all the joysticks and feeds the inputs from each back
 * to jME's InputManager.
 *
 * This handler also supports the joystick.rumble(rumbleAmount) method.  In this
 * case, when joystick.rumble(rumbleAmount) is called, the Android device will vibrate
 * if the device has a built-in vibrate motor.
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
 * This is done to allow for battery conservation when sensor data or gamepads
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
    private Vibrator vibrator = null;
    private boolean vibratorActive = false;
    private long maxRumbleTime = 250;  // 250ms

    public AndroidJoyInput(AndroidInputHandler inputHandler) {
        this.inputHandler = inputHandler;
        sensorJoyInput = new AndroidSensorJoyInput(this);
    }

    public void setView(GLSurfaceView view) {
        if (view == null) {
            vibrator = null;
        } else {
            // Get instance of Vibrator from current Context
            vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null) {
                logger.log(Level.FINE, "Vibrator Service not found.");
            }
        }

        if (sensorJoyInput != null) {
            sensorJoyInput.setView(view);
        }
    }

    public void loadSettings(AppSettings settings) {

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
        if (vibrator != null && vibratorActive) {
            vibrator.cancel();
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
    public void setJoyRumble(int joyId, float amount) {
        // convert amount to pulses since Android doesn't allow intensity
        if (vibrator != null) {
            final long rumbleOnDur = (long)(amount * maxRumbleTime); // ms to pulse vibration on
            final long rumbleOffDur = maxRumbleTime - rumbleOnDur; // ms to delay between pulses
            final long[] rumblePattern = {
                0, // start immediately
                rumbleOnDur, // time to leave vibration on
                rumbleOffDur // time to delay between vibrations
            };
            final int rumbleRepeatFrom = 0; // index into rumble pattern to repeat from

//            logger.log(Level.FINE, "Rumble amount: {0}, rumbleOnDur: {1}, rumbleOffDur: {2}",
//                    new Object[]{amount, rumbleOnDur, rumbleOffDur});

            if (rumbleOnDur > 0) {
                vibrator.vibrate(rumblePattern, rumbleRepeatFrom);
                vibratorActive = true;
            } else {
                vibrator.cancel();
                vibratorActive = false;
            }
        }
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "loading joysticks for {0}", this.getClass().getName());
        }
        if (!disableSensors) {
            joystickList.add(sensorJoyInput.loadJoystick(joystickList.size(), inputManager));
        }
        return joystickList.toArray( new Joystick[joystickList.size()] );
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

    }

}
