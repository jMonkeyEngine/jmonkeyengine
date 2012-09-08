/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.math.FastMath;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AndroidSensorJoyInput converts the Android Sensor system into Joystick events.
 * Each sensor type is a seperate joystick that can be used with RawInputListener
 * or the onAnalog listener.
 *
 * Device Orientation is not a physicsal sensor, but rather a calculation based
 * on the current accelerometer and magnetic sensor.  Orientation is configured
 * as joystick[0], while physical sensors are configured with the joyId set to
 * the Android constant for the sensor type.
 *
 * Right now, only the Orientation is exposed as a Joystick.
 *
 * MainActivity needs the following line to enable Joysticks
 *    joystickEventsEnabled = true;
 *
 * Rumble needs the following line in the Manifest File
 *     <uses-permission android:name="android.permission.VIBRATE"/>
 * Because Andorid does not allow for the user to define the intensity of the
 * vibration, the rumble amount (ie strength) is converted into vibration pulses
 * The stronger the strength amount, the shorter the delay between pulses.  If
 * amount is 1, then the vibration stays on the whole time.  If amount is 0.5,
 * the vibration will a pulse of equal parts vibration and delay.
 * To turn off vibration, set rumble amount to 0.
 *
 * @author iwgeric
 */
public class AndroidSensorJoyInput implements JoyInput, SensorEventListener {
    private final static Logger logger = Logger.getLogger(AndroidSensorJoyInput.class.getName());

    private InputManager inputManager = null;
    private SensorManager sensorManager = null;
    private Vibrator vibrator = null;
    private long maxRumbleTime = 250;  // 250ms
    private RawInputListener listener = null;
    private IntMap<SensorData> sensors = new IntMap<SensorData>();
    private Joystick[] joysticks;
    private boolean initialized = false;
    private WindowManager window;
    private Display disp;
    private int lastRotation = 0;
    private final float[] orientationLastValues = new float[3];
    private final float[] maxOrientationValues = new float[] {FastMath.HALF_PI, FastMath.HALF_PI, FastMath.HALF_PI};

    private final ArrayList<JoyAxisEvent> eventQueue = new ArrayList<JoyAxisEvent>();

    /**
     * Internal class to enclose data for each sensor.
     */
    private class SensorData {
        int androidSensorType = -1;
        int androidSensorSpeed = SensorManager.SENSOR_DELAY_GAME;
        Sensor sensor = null;
        float maxRange = 0f;
        float resolution = 0f;
        float[] lastValues;
        final Object valuesLock = new Object();
        int joyID = -1;
        String joyName = "";
        boolean enabled = false;
        boolean haveData = false;
        boolean createJoystick = false;

        public SensorData(int androidSensorType, Sensor sensor) {
            this.androidSensorType = androidSensorType;
            this.sensor = sensor;
        }

    }

    private void initSensorManager() {
        initWindow();
        // Get instance of the SensorManager from the current Context
        sensorManager = (SensorManager) JmeAndroidSystem.getActivity().getSystemService(Context.SENSOR_SERVICE);
        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) JmeAndroidSystem.getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        initSensors();
    }

    /**
     * Used internally.  Do not use.
     * Allows the context to reset the current activity for getting device rotation
     */
    public void initWindow() {
        window = JmeAndroidSystem.getActivity().getWindowManager();
        disp = window.getDefaultDisplay();
    }

    private void initSensors() {
        SensorData sensorData;

        List<Sensor> availSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor: availSensors) {
            logger.log(Level.INFO, "{0} Sensor is available, Type: {1}, Vendor: {2}, Version: {3}",
                    new Object[]{sensor.getName(), sensor.getType(), sensor.getVendor(), sensor.getVersion()});
        }

        sensorData = initSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensorData != null) {
            sensorData.joyName = "Device Direction";
            sensorData.lastValues = new float[3];
            sensorData.createJoystick = false;
        }

        sensorData = initSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensorData != null) {
            sensorData.joyName = "Device Acceleration";
            sensorData.lastValues = new float[3];
            sensorData.createJoystick = false;
        }

//        sensorData = initSensor(Sensor.TYPE_GYROSCOPE);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Rotation";
//            sensorData.lastValues = new float[3];
//            sensorData.createJoystick = false;
//        }
//
//        sensorData = initSensor(Sensor.TYPE_GRAVITY);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Gravity";
//            sensorData.lastValues = new float[3];
//            sensorData.createJoystick = false;
//        }
//
//        sensorData = initSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Linear Acceleration";
//            sensorData.lastValues = new float[3];
//            sensorData.createJoystick = false;
//        }
//
//        sensorData = initSensor(Sensor.TYPE_ROTATION_VECTOR);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Rotation Vector";
//            sensorData.lastValues = new float[4];
//            sensorData.createJoystick = false;
//        }
//
//        sensorData = initSensor(Sensor.TYPE_PROXIMITY);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Proximity";
//            sensorData.lastValues = new float[1];
//            sensorData.createJoystick = false;
//        }
//
//        sensorData = initSensor(Sensor.TYPE_LIGHT);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Light";
//            sensorData.lastValues = new float[1];
//            sensorData.createJoystick = false;
//        }
//
//        sensorData = initSensor(Sensor.TYPE_PRESSURE);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Pressure";
//            sensorData.lastValues = new float[1];
//            sensorData.createJoystick = false;
//        }
//
//        sensorData = initSensor(Sensor.TYPE_TEMPERATURE);
//        if (sensorData != null) {
//            sensorData.joyName = "Device Temperature";
//            sensorData.lastValues = new float[1];
//            sensorData.createJoystick = false;
//        }

    }

    private SensorData initSensor(int sensorType) {
        boolean success = false;

        SensorData sensorData = sensors.get(sensorType);
        if (sensorData != null) {
            unRegisterListener(sensorType);
        } else {
            sensorData = new SensorData(sensorType, null);
            sensors.put(sensorType, sensorData);
        }

        sensorData.androidSensorType = sensorType;
        sensorData.sensor = sensorManager.getDefaultSensor(sensorType);

        if (sensorData.sensor != null) {
            logger.log(Level.INFO, "Sensor Type {0} found.", sensorType);
            success = registerListener(sensorType);
        } else {
            logger.log(Level.INFO, "Sensor Type {0} not found.", sensorType);
        }

        if (success) {
            return sensorData;
        } else {
            return null;
        }
    }

    private boolean registerListener(int sensorType) {
        SensorData sensorData = sensors.get(sensorType);
        if (sensorData != null) {
            if (sensorData.enabled) {
                logger.log(Level.INFO, "Sensor Already Active: SensorType: {0}, active: {1}",
                        new Object[]{sensorType, sensorData.enabled});
                return true;
            }
            sensorData.haveData = false;
            if (sensorData.sensor != null) {
                if (sensorManager.registerListener(this, sensorData.sensor, sensorData.androidSensorSpeed)) {
                    sensorData.enabled = true;
                    logger.log(Level.INFO, "SensorType: {0}, actived: {1}",
                            new Object[]{sensorType, sensorData.enabled});
                    return true;
                } else {
                    sensorData.enabled = false;
                    logger.log(Level.INFO, "Sensor Type {0} activation failed.", sensorType);
                }
            }
        }
        return false;
    }

    private void unRegisterListener(int sensorType) {
        SensorData sensorData = sensors.get(sensorType);
        if (sensorData != null) {
            if (sensorData.sensor != null) {
                sensorManager.unregisterListener(this, sensorData.sensor);
            }
            sensorData.enabled = false;
            sensorData.haveData = false;
            logger.log(Level.INFO, "SensorType: {0} deactivated, active: {1}",
                    new Object[]{sensorType, sensorData.enabled});
        }
    }

    /**
     * Pauses the sensors to save battery life if the sensors are not needed.
     * Used to pause sensors when the activity pauses
     */
    public void pauseSensors() {
        for (Entry entry: sensors) {
            unRegisterListener(entry.getKey());
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    /**
     * Resumes the sensors.
     * Used to resume sensors when the activity comes to the top of the stack
     */
    public void resumeSensors() {
        for (Entry entry: sensors) {
            registerListener(entry.getKey());
        }
    }

    /*
     * Allows the orientation data to be rotated based on the current device
     * rotation.  This keeps the data aligned with the game when the user
     * rotates the device during game play.
     *
     * Android remapCoordinateSystem from the Android docs
     * remapCoordinateSystem(float[] inR, int X, int Y, float[] outR)
     *
     * @param   inR   the rotation matrix to be transformed. Usually it is the matrix
     *          returned by getRotationMatrix(float[], float[], float[], float[]).
     *
     * @param   outR  the transformed rotation matrix. inR and outR can be the same
     *          array, but it is not recommended for performance reason.
     *
     * X     defines on which world (Earth) axis and direction the X axis of the device is mapped.
     * Y     defines on which world (Earth) axis and direction the Y axis of the device is mapped.
     *
     * @return True if successful
     */
    private boolean remapCoordinates(float[] inR, float[] outR) {
        int xDir = SensorManager.AXIS_X;
        int yDir = SensorManager.AXIS_Y;
        int curRotation = getScreenRotation();
        if (lastRotation != curRotation) {
            logger.log(Level.INFO, "Device Rotation changed to: {0}", curRotation);
        }
        lastRotation = curRotation;

//        logger.log(Level.INFO, "Screen Rotation: {0}", getScreenRotation());
        switch (getScreenRotation()) {
            // device natural position
            case Surface.ROTATION_0:
                xDir = SensorManager.AXIS_X;
                yDir = SensorManager.AXIS_Y;
                break;
            // device rotated 90 deg counterclockwise
            case Surface.ROTATION_90:
                xDir = SensorManager.AXIS_Y;
                yDir = SensorManager.AXIS_MINUS_X;
                break;
            // device rotated 180 deg counterclockwise
            case Surface.ROTATION_180:
                xDir = SensorManager.AXIS_MINUS_X;
                yDir = SensorManager.AXIS_MINUS_Y;
                break;
            // device rotated 270 deg counterclockwise
            case Surface.ROTATION_270:
                xDir = SensorManager.AXIS_MINUS_Y;
                yDir = SensorManager.AXIS_X;
                break;
            default:
                break;
        }
        return SensorManager.remapCoordinateSystem(inR, xDir, yDir, outR);
    }

    /**
     * Returns the current device rotation.
     * Surface.ROTATION_0 = device in natural default rotation
     * Surface.ROTATION_90 = device in rotated 90deg counterclockwise
     * Surface.ROTATION_180 = device in rotated 180deg counterclockwise
     * Surface.ROTATION_270 = device in rotated 270deg counterclockwise
     *
     * When the Manifest locks the orientation, this value will not change during
     * gametime, but if the orientation of the screen is based off the sensor,
     * this value will change as the device is rotated.
     * @return Current device rotation amount
     */
    private int getScreenRotation() {
        return disp.getRotation();
    }

    /**
     * Calculates the device orientation based off the data recieved from the
     * Acceleration Sensor and Mangetic Field sensor
     * Values are returned relative to the Earth.
     *
     * From the Android Doc
     *
     * Computes the device's orientation based on the rotation matrix. When it returns, the array values is filled with the result:
     *  values[0]: azimuth, rotation around the Z axis.
     *  values[1]: pitch, rotation around the X axis.
     *  values[2]: roll, rotation around the Y axis.
     *
     * The reference coordinate-system used is different from the world
     * coordinate-system defined for the rotation matrix:
     *  X is defined as the vector product Y.Z (It is tangential to the ground at the device's current location and roughly points West).
     *  Y is tangential to the ground at the device's current location and points towards the magnetic North Pole.
     *  Z points towards the center of the Earth and is perpendicular to the ground.
     *
     * @return True if Orientation was calculated
     */
    private boolean updateOrientation() {
        SensorData sensorData;
        final float[] curInclinationMat = new float[16];
        final float[] curRotationMat = new float[16];
        final float[] rotatedRotationMat = new float[16];
        final float[] accValues = new float[3];
        final float[] magValues = new float[3];
        final float[] deltaOrientation = new float[3];

        // if the Gravity Sensor is available, use it for orientation, if not
        // use the accelerometer
        // NOTE: Seemed to work worse, so just using accelerometer
//        sensorData = sensors.get(Sensor.TYPE_GRAVITY);
//        if (sensorData == null) {
            sensorData = sensors.get(Sensor.TYPE_ACCELEROMETER);
//        }

        if (sensorData == null || !sensorData.enabled || !sensorData.haveData) {
            return false;
        }

        synchronized(sensorData.valuesLock) {
            accValues[0] = sensorData.lastValues[0];
            accValues[1] = sensorData.lastValues[1];
            accValues[2] = sensorData.lastValues[2];
        }

        sensorData = sensors.get(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensorData == null || !sensorData.enabled || !sensorData.haveData) {
            return false;
        }

        synchronized(sensorData.valuesLock) {
            magValues[0] = sensorData.lastValues[0];
            magValues[1] = sensorData.lastValues[1];
            magValues[2] = sensorData.lastValues[2];
        }

        if (SensorManager.getRotationMatrix(curRotationMat, curInclinationMat, accValues, magValues)) {
            final float [] orientValues = new float[3];
            if (remapCoordinates(curRotationMat, rotatedRotationMat)) {
                SensorManager.getOrientation(rotatedRotationMat, orientValues);
//                logger.log(Level.INFO, "Orientation Values: {0}, {1}, {2}",
//                        new Object[]{orientValues[0], orientValues[1], orientValues[2]});

                //values[0]: Azimuth - (the compass bearing east of magnetic north)
                //values[1]: Pitch, rotation around x-axis (is the phone leaning forward or back)
                //values[2]: Roll, rotation around y-axis (is the phone leaning over on its left or right side)

                //Azimuth (degrees of rotation around the z axis). This is the angle between magnetic north
                //and the device's y axis. For example, if the device's y axis is aligned with magnetic north
                //this value is 0, and if the device's y axis is pointing south this value is 180.
                //Likewise, when the y axis is pointing east this value is 90 and when it is pointing west
                //this value is 270.

                //Pitch (degrees of rotation around the x axis). This value is positive when the positive
                //z axis rotates toward the positive y axis, and it is negative when the positive z axis
                //rotates toward the negative y axis. The range of values is 180 degrees to -180 degrees.

                //Roll (degrees of rotation around the y axis). This value is positive when the
                //positive z axis rotates toward the positive x axis, and it is negative when the
                //positive z axis rotates toward the negative x axis. The range of values
                //is 90 degrees to -90 degrees.


//                // Azimuth scaling
//                if (orientValues[0]<0) {
//                    orientValues[0] += FastMath.TWO_PI;
//                }
//
//                // Pitch scaling
//                if (orientValues[1] < -FastMath.HALF_PI) {
//                    orientValues[1] += (-2*(FastMath.HALF_PI+orientValues[1]));
//                } else if (orientValues[1] > FastMath.HALF_PI) {
//                    orientValues[1] += (2*(FastMath.HALF_PI-orientValues[1]));
//                }
//
//                // Roll scaling
//                // NOT NEEDED

                // need to reorder to make it x, y, z order instead of z, x, y order
                deltaOrientation[0] = orientValues[1] - orientationLastValues[1];
                deltaOrientation[1] = orientValues[2] - orientationLastValues[2];
                deltaOrientation[2] = orientValues[0] - orientationLastValues[0];

//                logger.log(Level.INFO, "Sensor Values x:{0}, y:{1}, z:{2}, deg x:{3}, y:{4}, z:{5}",
//                        new Object[]{orientValues[1], orientValues[2], orientValues[0],
//                        orientValues[1]*FastMath.RAD_TO_DEG, orientValues[2]*FastMath.RAD_TO_DEG, orientValues[0]*FastMath.RAD_TO_DEG});

                synchronized (eventQueue){
                    // only send data to inputManager if it is different than last time
                    // orientValues[1] is the X axis -> JoyAxisEvent Axis 0
                    // orientValues[2] is the Y axis -> JoyAxisEvent Axis 1
                    // orientValues[0] is the Z axis -> JoyAxisEvent Axis 2
                    if (Math.abs(deltaOrientation[0]) > FastMath.ZERO_TOLERANCE) {
                        eventQueue.add(new JoyAxisEvent(0, 0, orientValues[1] / maxOrientationValues[1]));
                    }
                    if (Math.abs(deltaOrientation[1]) > FastMath.ZERO_TOLERANCE) {
                        eventQueue.add(new JoyAxisEvent(0, 1, orientValues[2] / maxOrientationValues[2]));
                    }
                    if (Math.abs(deltaOrientation[2]) > FastMath.ZERO_TOLERANCE) {
                        eventQueue.add(new JoyAxisEvent(0, 2, orientValues[0] / maxOrientationValues[0]));
                    }
                }

                orientationLastValues[0] = orientValues[0];
                orientationLastValues[1] = orientValues[1];
                orientationLastValues[2] = orientValues[2];

                return true;
            } else {
                //logger.log(Level.INFO, "remapCoordinateSystem failed");
            }

        } else {
            //logger.log(Level.INFO, "getRotationMatrix returned false");
        }

        return false;
    }

    // Start of JoyInput methods

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

            logger.log(Level.INFO, "Rumble amount: {0}, rumbleOnDur: {1}, rumbleOffDur: {2}",
                    new Object[]{amount, rumbleOnDur, rumbleOffDur});

            if (rumbleOnDur > 0) {
                vibrator.vibrate(rumblePattern, rumbleRepeatFrom);
            } else {
                vibrator.cancel();
            }
        }

    }

    public Joystick[] loadJoysticks(InputManager inputManager) {
        this.inputManager = inputManager;

        int joyIndex = 1;  // start with 1 for orientation
        for (Entry entry: sensors) {
            SensorData sensorData = (SensorData)entry.getValue();
            if (sensorData != null) {
                if (sensorData.sensor != null && sensorData.createJoystick) {
                    joyIndex++; // add 1 for each of the physical sensors configured and enabled

                }
            }
        }

        joysticks = new Joystick[joyIndex];
        joyIndex = 0;
        Joystick joystick;

        // manually create a joystick for orientation since orientation
        // is not an actual physical sensor
        // Do the orientation joystick first so it is compatible with PC systems
        // that only have a single joystick configured.
        joystick = new Joystick(inputManager,
                                    this,
                                    joyIndex,
                                    "Device Orientation",
                                    0, // button count
                                    3, // axis count
                                    0, 1); // xAxis, yAxis
        joysticks[joyIndex] = joystick;
        joyIndex++;

        // create a joystick for each physical sensor configured
        for (Entry entry: sensors) {
            SensorData sensorData = (SensorData)entry.getValue();
            if (sensorData != null) {
                if (sensorData.sensor != null && sensorData.createJoystick) {
                    sensorData.joyID = joyIndex;
                    joystick = new Joystick(inputManager,
                                                this,
                                                joyIndex,
                                                sensorData.joyName,
                                                0, // button count
                                                sensorData.lastValues.length, // axis count
                                                0, 1); // xAxis, yAxis
                    joysticks[joyIndex] = joystick;
                    joyIndex++;

                }
            }
        }

        return joysticks;
    }

    public void initialize() {
        logger.log(Level.INFO, "Doing Initialize.");
        initSensorManager();
        initialized = true;
    }

    public void update() {
        updateOrientation();
        synchronized (eventQueue){
            // flush events to listener
            if (listener != null && eventQueue.size() > 0) {
                for (int i = 0; i < eventQueue.size(); i++){
                    listener.onJoyAxisEvent(eventQueue.get(i));
                }
                eventQueue.clear();
            }
        }
    }

    public void destroy() {
        logger.log(Level.INFO, "Doing Destroy.");
        pauseSensors();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        sensors.clear();
        eventQueue.clear();
        joysticks = null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return System.nanoTime();
    }

    // End of JoyInput methods

    // Start of Android SensorEventListener methods

    public void onSensorChanged(SensorEvent se) {
        if (!initialized) {
            return;
        }

        int sensorType = se.sensor.getType();

        SensorData sensorData = sensors.get(sensorType);
        if (sensorData != null && sensorData.sensor.equals(se.sensor) && sensorData.enabled) {

            if (!sensorData.haveData) {
                sensorData.haveData = true;

            } else {
                if (sensorData.joyID != -1) {
                    final float[] deltaValues = new float[sensorData.lastValues.length];
                    for (int i=0; i<sensorData.lastValues.length; i++) {
                        deltaValues[i] = se.values[i] - sensorData.lastValues[i];
                    }

                    // TODO: need to scale physical sensor data to fit within
                    // joystick model of providing values of 0 to 1
                    synchronized (eventQueue){
                        for (int i=0; i<deltaValues.length; i++) {
                            if (FastMath.abs(deltaValues[i]) > sensorData.lastValues[i] + FastMath.ZERO_TOLERANCE) {
                                eventQueue.add(new JoyAxisEvent(sensorData.joyID, i, se.values[i]));
                            }
                        }
                    }
                }

            }

            synchronized(sensorData.valuesLock) {
                for (int i=0; i<sensorData.lastValues.length; i++) {
                    sensorData.lastValues[i] = se.values[i];
                }
            }

        }
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
        int sensorType = sensor.getType();
        SensorData sensorData = sensors.get(sensorType);
        if (sensorData != null) {
            logger.log(Level.INFO, "onAccuracyChanged for {0} ({1}): accuracy: {2}",
                    new Object[]{sensor.toString(), sensorData.joyName, i});
            logger.log(Level.INFO, "MaxRange: {0}, Resolution: {1}",
                    new Object[]{sensor.getMaximumRange(), sensor.getResolution()});
            if (sensorData.sensor != null && sensorData.sensor.equals(sensor)) {
                sensorData.resolution = sensor.getResolution();
                sensorData.maxRange = sensor.getMaximumRange();
            }
        }
    }

    // End of SensorEventListener methods

}
