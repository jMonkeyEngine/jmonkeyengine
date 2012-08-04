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
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.SensorInput;
import com.jme3.input.event.MotionSensorEvent;
import com.jme3.math.Vector3f;
import com.jme3.system.android.JmeAndroidSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Android specific implementation of SensorInput.
 *
 * @author iwgeric
 */
public class AndroidSensorInput implements SensorInput, SensorEventListener {
    private final static Logger logger = Logger.getLogger(AndroidSensorInput.class.getName());

    private SensorManager sensorManager = null;
    private RawInputListener listener = null;
    private Map<Integer, SensorData> sensors = new HashMap<Integer, SensorData>();
    private boolean initialized = false;
    private WindowManager window;
    private Display disp;

    private final float[] curAccValues = new float[3];
    private final float[] curMagValues = new float[3];
    private final float[] curInclination = new float[9];
    private final float[] curRotation = new float[9];
    private final float[] rotatedRotation = new float[9];

    private final ArrayList<MotionSensorEvent> eventQueue = new ArrayList<MotionSensorEvent>();

    /**
     * Internal class to enclose data for each sensor.
     */
    private class SensorData {
        int androidSensorType = -1;
        int androidSensorSpeed = SensorManager.SENSOR_DELAY_UI;
        Sensor sensor = null;
        Vector3f lastValues = new Vector3f();
        float minChangePercent = 0f;
        boolean enabled = false;
        boolean paused = false;

        public SensorData(int androidSensorType, Sensor sensor) {
            this.androidSensorType = androidSensorType;
            this.sensor = sensor;
        }
    }

    /**
     * Pauses the active sensors to save battery.  Mostly used internally so that
     * the sensors can be deactivated while the game Activity is
     * in the background to save battery life
     */
    public void pauseSensors() {
        for (Entry<Integer, SensorData> entry : sensors.entrySet()) {
            SensorData sensorData = entry.getValue();
            if (sensorData.sensor != null) {
                unRegisterListener(entry.getKey());
                sensorData.paused = true;
            }
        }
    }

    /**
     * Resumes paused sensors.  Mostly used internally so that
     * the sensors can be reactivated when the game Activity is
     * placed back onto the forefront.
     */
    public void resumeSensors() {
        for (Entry<Integer, SensorData> entry : sensors.entrySet()) {
            SensorData sensorData = entry.getValue();
            if (sensorData.sensor != null && sensorData.paused) {
                if (registerListener(entry.getKey())) {
                    sensorData.paused = false;
                }
            }
        }
    }

    /**
     * Used internally by the context to reset the Sensor Manager on device rotations.
     * Necessary because a new Activity is created on a device rotation, so the
     * Sensor Manager needs to be reset with the new Activity.
     */
    public void resetSensorManager() {
        initSensorManager();
    }

    private void initSensorManager() {
        window = JmeAndroidSystem.getActivity().getWindowManager();
        disp = window.getDefaultDisplay();

        sensorManager = (SensorManager) JmeAndroidSystem.getActivity().getSystemService(Context.SENSOR_SERVICE);

        initSensor(SensorInput.SENSOR_TYPE_MAGNETIC_FIELD);
        initSensor(SensorInput.SENSOR_TYPE_ACCELEROMETER);
        initSensor(SensorInput.SENSOR_TYPE_ORIENTATION);

    }

    private boolean initSensor(int sensorType) {
        boolean result = false;
        boolean previouslyActive = false;

        SensorData sensorData = sensors.get((Integer)sensorType);
        if (sensorData != null) {
            if (sensorData.enabled) {
                previouslyActive = true;
            }
            unRegisterListener(sensorType);
        } else {
            sensorData = new SensorData(sensorType, null);
            sensors.put(sensorType, sensorData);
        }

        switch (sensorType) {
            case SensorInput.SENSOR_TYPE_MAGNETIC_FIELD:
                sensorData.androidSensorType = Sensor.TYPE_MAGNETIC_FIELD;
                sensorData.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                break;
            case SensorInput.SENSOR_TYPE_ACCELEROMETER:
                sensorData.androidSensorType = Sensor.TYPE_ACCELEROMETER;
                sensorData.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                break;
            case SensorInput.SENSOR_TYPE_ORIENTATION:
                sensorData.androidSensorType = Sensor.TYPE_ORIENTATION;
                //Orientation is not a sensor anymore but rather a call to SensorMangaer
                //  to get the current orientation based on the Magnetic and Accelerometer sensor data
                sensorData.sensor = null;
                break;

            default:
                throw new IllegalArgumentException("Invalid Sensor Type.");
        }

        if (sensorData.sensor != null || sensorType == SensorInput.SENSOR_TYPE_ORIENTATION) {
            logger.log(Level.INFO, "Sensor Type {0} found.", sensorType);
            if (previouslyActive) {
                logger.log(Level.INFO, "Reactivating Sensor Type {0}.", sensorType);
                registerListener(sensorType);
            }
            result = true;
        }


        return result;
    }

    private boolean registerListener(int sensorType) {
        SensorData sensorData = sensors.get((Integer)sensorType);
        if (sensorData != null) {
            if (sensorData.enabled) {
                logger.log(Level.INFO, "Sensor Already Active: SensorType: {0}, active: {1}",
                        new Object[]{sensorType, sensorData.enabled});
                return true;
            }
            if (sensorData.sensor != null) {
                if (sensorManager.registerListener(this, sensorData.sensor, sensorData.androidSensorSpeed)) {
                    sensorData.enabled = true;
                    logger.log(Level.INFO, "SensorType: {0}, active: {1}",
                            new Object[]{sensorType, sensorData.enabled});
                    logger.log(Level.INFO, "Sensor Type {0} activated.", sensorType);
                    return true;
                } else {
                    sensorData.enabled = false;
                    logger.log(Level.INFO, "Sensor Type {0} activation failed.", sensorType);
                }
            } else if (sensorType == SensorInput.SENSOR_TYPE_ORIENTATION) {
                logger.log(Level.INFO, "Sensor is Orientation");
                if (registerListener(SensorInput.SENSOR_TYPE_MAGNETIC_FIELD) && registerListener(SensorInput.SENSOR_TYPE_ACCELEROMETER)) {
                    sensorData.enabled = true;
                    logger.log(Level.INFO, "Magnetic and Acceleration Sensors Registered and Orientation Sensor being simulated.");
                    return true;
                }
            }
            sensorData.lastValues = null;
        }
        return false;
    }

    private void unRegisterListener(int sensorType) {
        SensorData sensorData = sensors.get((Integer)sensorType);
        if (sensorData != null) {
            if (sensorData.sensor != null) {
                sensorManager.unregisterListener(this, sensorData.sensor);
            } else if (sensorType == SensorInput.SENSOR_TYPE_ORIENTATION) {
                logger.log(Level.INFO, "Mangetic and Acceleration Sensors are being deactivated with Orientation Sensor.");
                unRegisterListener(SensorInput.SENSOR_TYPE_MAGNETIC_FIELD);
                unRegisterListener(SensorInput.SENSOR_TYPE_ACCELEROMETER);
            }
            sensorData.enabled = false;
            logger.log(Level.INFO, "SensorType: {0}, active: {1}",
                    new Object[]{sensorType, sensorData.enabled});
            logger.log(Level.INFO, "Sensor Type {0} deactivated.", sensorType);
        }
    }

    /*
     * Android remapCoordinateSystem from the Android docs
     * remapCoordinateSystem(float[] inR, int X, int Y, float[] outR)
     *
     * @param   inR   the rotation matrix to be transformed. Usually it is the matrix
     *          returned by getRotationMatrix(float[], float[], float[], float[]).
     *
     * @param   outR  the transformed rotation matrix. inR and outR can be the same
     *          array, but it is not recommended for performance reason.
     *
     * X     defines on which world axis and direction the X axis of the device is mapped.
     * Y     defines on which world axis and direction the Y axis of the device is mapped.
     *
     * @return True if successful
     */

    private boolean remapCoordinates(float[] inR, float[] outR) {
        int xDir = SensorManager.AXIS_X;
        int yDir = SensorManager.AXIS_Y;

//        logger.log(Level.INFO, "Screen Rotation: {0}", getScreenRotation());
        if (getScreenRotation() == Surface.ROTATION_0) {
            xDir = SensorManager.AXIS_X;
            yDir = SensorManager.AXIS_Y;
        }
        if (getScreenRotation() == Surface.ROTATION_90) {
            xDir = SensorManager.AXIS_MINUS_Y;
            yDir = SensorManager.AXIS_MINUS_X;
        }
        if (getScreenRotation() == Surface.ROTATION_180) {
            xDir = SensorManager.AXIS_MINUS_X;
            yDir = SensorManager.AXIS_MINUS_Y;
        }
        if (getScreenRotation() == Surface.ROTATION_270) {
            xDir = SensorManager.AXIS_Y;
            yDir = SensorManager.AXIS_MINUS_X;
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

    private Integer getAndroidSensorSpeed(int sensorInputSpeed) {
        Integer androidSpeed = null;
        switch (sensorInputSpeed) {
            case SensorInput.SENSOR_SPEED_SLOW:
                androidSpeed = SensorManager.SENSOR_DELAY_UI;
                break;
            case SensorInput.SENSOR_SPEED_MEDIUM:
                androidSpeed = SensorManager.SENSOR_DELAY_NORMAL;
                break;
            case SensorInput.SENSOR_SPEED_FAST:
                androidSpeed = SensorManager.SENSOR_DELAY_GAME;
                break;
            default:
                throw new IllegalArgumentException("Invalid Sensor Speed.");
        }
        return androidSpeed;
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
     * @return
     */
    private boolean updateOrientation() {
        SensorData sensorData;
        sensorData = sensors.get((Integer)SensorInput.SENSOR_TYPE_MAGNETIC_FIELD);
        if (sensorData == null || !sensorData.enabled) {
            return false;
        }
        sensorData = sensors.get((Integer)SensorInput.SENSOR_TYPE_ACCELEROMETER);
        if (sensorData == null || !sensorData.enabled) {
            return false;
        }

        sensorData = sensors.get((Integer)SensorInput.SENSOR_TYPE_ORIENTATION);
        if (sensorData != null && sensorData.enabled) {

            // create new copies so they don't get updated during the getRotationMatrix call
            final float[] accValues = new float[3];
            final float[] magValues = new float[3];
            synchronized(curAccValues) {
                accValues[0] = curAccValues[0];
                accValues[1] = curAccValues[1];
                accValues[2] = curAccValues[2];
            }
            synchronized(curMagValues) {
                magValues[0] = curMagValues[0];
                magValues[1] = curMagValues[1];
                magValues[2] = curMagValues[2];
            }

            if (SensorManager.getRotationMatrix(curRotation, curInclination, accValues, magValues)) {
                final float [] orientValues = new float[3];
                if (remapCoordinates(curRotation, rotatedRotation)) {
                    SensorManager.getOrientation(rotatedRotation, orientValues);
//                    logger.log(Level.INFO, "Orientation Values: {0}, {1}, {2}",
//                            new Object[]{orientValues[0], orientValues[1], orientValues[2]});

                    updateEventQueue(SensorInput.SENSOR_TYPE_ORIENTATION,
                            orientValues[0], orientValues[1], orientValues[2], System.nanoTime());

                    return true;
                } else {
                    //logger.log(Level.INFO, "remapCoordinateSystem failed");
                }

            } else {
                //logger.log(Level.INFO, "getRotationMatrix returned false");
            }

        } else {
            if (!sensorData.enabled) {
                //logger.log(Level.INFO, "Orientation is not active");
            }
        }
        return false;
    }

    private void updateEventQueue(int sensorType, float x, float y, float z, long timestamp) {
//        logger.log(Level.INFO, "updateEventQueue for {0}: values: {1}, {2}, {3}",
//                new Object[]{sensorType, x, y, z});
        float lastX, lastY, lastZ;
        float dX, dY, dZ;

        SensorData sensorData = sensors.get((Integer)sensorType);

        if (sensorData != null) {
            // if lastValues is null, then this is the first scan after a registerListener
            // so set lastValues to the current values so dX,dY,dZ are zero this pass
            if (sensorData.lastValues == null) {
                sensorData.lastValues = new Vector3f(x, y, z);
            }

            lastX = sensorData.lastValues.x;
            lastY = sensorData.lastValues.y;
            lastZ = sensorData.lastValues.z;

            dX = x - lastX;
            dY = y - lastY;
            dZ = z - lastZ;

            if (dX != 0 && dY != 0 && dZ != 0) {
                MotionSensorEvent motionEvent = new MotionSensorEvent(sensorType, x, y, z, dX, dY, dZ);
                motionEvent.setTime(timestamp);
                sensorData.lastValues.x = x;
                sensorData.lastValues.y = y;
                sensorData.lastValues.z = z;

                synchronized (eventQueue){
                    eventQueue.add(motionEvent);
                }
            } else {
                //logger.log(Level.INFO, "No change in Sensor Data for: {0}", sensorType);
            }
        } else {
            //logger.log(Level.INFO, "Sensor Data is null for: {0}", sensorType);
        }
    }














// Start of methods from SensorInput

    public boolean isEnabled(int sensorType) {
        logger.log(Level.INFO, "Checking isEnabled for type: {0}", sensorType);
        SensorData sensorData = sensors.get((Integer)sensorType);
        if (sensorData == null) {
//            logger.log(Level.INFO, "sensor data is null, sensors size is: {0}", sensors.size());
            return false;
        }
        return sensors.get((Integer)sensorType).enabled;
    }

    public void setEnable(boolean enable) {
        for (Integer sensorType: sensors.keySet()) {
            setEnable(sensorType, enable);
        }
    }

    public void setEnable(int sensorType, boolean enable) {
        logger.log(Level.INFO, "Setting Sensor {0} Enable to {1}",
                new Object[]{sensorType, enable});
        if (enable) {
//            registerListener(sensorType, true);
            registerListener(sensorType);
        } else {
            unRegisterListener(sensorType);
        }
    }

    public void setSensorFrequency(int sensorType, int updateSpeed) {
        SensorData sensorData = sensors.get((Integer)sensorType);
        if (sensorData == null || sensorData.enabled) {
            throw new IllegalArgumentException("Sensor Type Not Configured or is already active.");
        }

        sensorData.androidSensorSpeed = getAndroidSensorSpeed(updateSpeed);
    }

    public Set<Integer> getSensorTypes() {
        return Collections.unmodifiableSet(sensors.keySet());
    }

    public void setSensorMinChange(int sensorType, float minChangePercent) {
        throw new UnsupportedOperationException("Not supported yet.");
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
            for (int i = 0; i < eventQueue.size(); i++){
                listener.onMotionSensorEvent(eventQueue.get(i));
            }
            eventQueue.clear();
        }
    }

    public void destroy() {
        for (Integer i: sensors.keySet()) {
            unRegisterListener(i);
        }
        logger.log(Level.INFO, "Doing Destroy");
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        sensors.clear();
        eventQueue.clear();
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

// End of methods from SensorInput

// Start of methods from SensorEventListener

    public void onSensorChanged(SensorEvent se) {
//        logger.log(Level.INFO, "onSensorChanged for {0}: values: {1}, {2}, {3}",
//                new Object[]{se.sensor.getType(), se.values[0], se.values[1], se.values[2]});
        SensorData sensorData;
        int sensorType;
        for (Entry<Integer, SensorData> entry : sensors.entrySet()) {
//            if (entry.getValue().sensor == null) {
//                logger.log(Level.INFO, "Sensor is null for SensorType: {0}", entry.getKey());
//            }
            if (entry.getValue().sensor != null && entry.getValue().sensor.equals(se.sensor)) {
                sensorType = entry.getKey();
                sensorData = entry.getValue();

                updateEventQueue(sensorType, se.values[0], se.values[1], se.values[2], se.timestamp);

                if (sensorType == SensorInput.SENSOR_TYPE_MAGNETIC_FIELD) {
                    synchronized(curMagValues) {
                        curMagValues[0] = se.values[0];
                        curMagValues[1] = se.values[1];
                        curMagValues[2] = se.values[2];
                    }
                }
                if (sensorType == SensorInput.SENSOR_TYPE_ACCELEROMETER) {
                    synchronized(curAccValues) {
                        curAccValues[0] = se.values[0];
                        curAccValues[1] = se.values[1];
                        curAccValues[2] = se.values[2];
                    }
                }
                break;
            }
        }
    }


    public void onAccuracyChanged(Sensor sensor, int i) {
        logger.log(Level.INFO, "onAccuracyChanged for {0}: accuracy: {1}",
                new Object[]{sensor.toString(), i});
    }

// End of methods from SensorEventListener

}
