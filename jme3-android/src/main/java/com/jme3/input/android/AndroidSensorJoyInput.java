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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.view.Surface;
import android.view.WindowManager;
import com.jme3.input.AbstractJoystick;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.SensorJoystickAxis;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.math.FastMath;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AndroidSensorJoyInput converts the Android Sensor system into Joystick events.
 * A single joystick is configured and includes data for all configured sensors
 * as separate axes of the joystick.
 *
 * Each axis is named according to the static strings in SensorJoystickAxis.
 * Refer to the strings defined in SensorJoystickAxis for a list of supported
 * sensors and their axis data.  Each sensor type defined in SensorJoystickAxis
 * will be attempted to be configured.  If the device does not support a particular
 * sensor, the axis will return null if joystick.getAxis(String name) is called.
 *
 * The joystick.getXAxis and getYAxis methods of the joystick are configured to
 * return the device orientation values in the device's X and Y directions.
 *
 * @author iwgeric
 */
public class AndroidSensorJoyInput implements SensorEventListener {
    private final static Logger logger = Logger.getLogger(AndroidSensorJoyInput.class.getName());

    private AndroidJoyInput joyInput;
    private SensorManager sensorManager = null;
    private WindowManager windowManager = null;
    private IntMap<SensorData> sensors = new IntMap<>();
    private int lastRotation = 0;
    private boolean loaded = false;

    public AndroidSensorJoyInput(AndroidJoyInput joyInput) {
        this.joyInput = joyInput;
    }

    /**
     * Internal class to enclose data for each sensor.
     */
    private class SensorData {
        int androidSensorType = -1;
        int androidSensorSpeed = SensorManager.SENSOR_DELAY_GAME;
        Sensor sensor = null;
        int sensorAccuracy = -1;
        float[] lastValues;
        final Object valuesLock = new Object();
        ArrayList<AndroidSensorJoystickAxis> axes = new ArrayList<>();
        boolean enabled = false;
        boolean haveData = false;

        public SensorData(int androidSensorType, Sensor sensor) {
            this.androidSensorType = androidSensorType;
            this.sensor = sensor;
        }

    }

    public void setView(GLSurfaceView view) {
        pauseSensors();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (view == null) {
            windowManager = null;
            sensorManager = null;
        } else {
            // Get instance of the WindowManager from the current Context
            windowManager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
            // Get instance of the SensorManager from the current Context
            sensorManager = (SensorManager) view.getContext().getSystemService(Context.SENSOR_SERVICE);
        }
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
            logger.log(Level.FINE, "Sensor Type {0} found.", sensorType);
            success = registerListener(sensorType);
        } else {
            logger.log(Level.FINE, "Sensor Type {0} not found.", sensorType);
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
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Sensor Already Active: SensorType: {0}, active: {1}",
                            new Object[]{sensorType, sensorData.enabled});
                }
                return true;
            }
            sensorData.haveData = false;
            if (sensorData.sensor != null) {
                if (sensorManager.registerListener(this, sensorData.sensor, sensorData.androidSensorSpeed)) {
                    sensorData.enabled = true;
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "SensorType: {0}, enabled: {1}",
                                new Object[]{sensorType, sensorData.enabled});
                    }
                    return true;
                } else {
                    sensorData.enabled = false;
                    logger.log(Level.FINE, "Sensor Type {0} activation failed.", sensorType);
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
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "SensorType: {0} deactivated, active: {1}",
                        new Object[]{sensorType, sensorData.enabled});
            }
        }
    }

    /**
     * Pauses the sensors to save battery life if the sensors are not needed.
     * Used to pause sensors when the activity pauses
     */
    public void pauseSensors() {
        for (Entry entry: sensors) {
            if (entry.getKey() != Sensor.TYPE_ORIENTATION) {
                unRegisterListener(entry.getKey());
            }
        }
    }

    /**
     * Resumes the sensors.
     * Used to resume sensors when the activity comes to the top of the stack
     */
    public void resumeSensors() {
        for (Entry entry: sensors) {
            if (entry.getKey() != Sensor.TYPE_ORIENTATION) {
                registerListener(entry.getKey());
            }
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
            logger.log(Level.FINE, "Device Rotation changed to: {0}", curRotation);
        }
        lastRotation = curRotation;

//        logger.log(Level.FINE, "Screen Rotation: {0}", getScreenRotation());
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
     * game time, but if the orientation of the screen is based off the sensor,
     * this value will change as the device is rotated.
     * @return Current device rotation amount
     */
    private int getScreenRotation() {
        return windowManager.getDefaultDisplay().getRotation();
    }

    /**
     * Calculates the device orientation based off the data received from the
     * Acceleration Sensor and Magnetic Field sensor
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
        AndroidSensorJoystickAxis axis;
        final float[] curInclinationMat = new float[16];
        final float[] curRotationMat = new float[16];
        final float[] rotatedRotationMat = new float[16];
        final float[] accValues = new float[3];
        final float[] magValues = new float[3];
        final float[] orderedOrientation = new float[3];

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

        if (sensorData.sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
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

        if (sensorData.sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
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
//                logger.log(Level.FINE, "Orientation Values: {0}, {1}, {2}",
//                        new Object[]{orientValues[0], orientValues[1], orientValues[2]});


                // need to reorder to make it x, y, z order instead of z, x, y order
                orderedOrientation[0] = orientValues[1];
                orderedOrientation[1] = orientValues[2];
                orderedOrientation[2] = orientValues[0];

                sensorData = sensors.get(Sensor.TYPE_ORIENTATION);
                if (sensorData != null && sensorData.axes.size() > 0) {
                    for (int i=0; i<orderedOrientation.length; i++) {
                        axis = sensorData.axes.get(i);
                        if (axis != null) {
                            axis.setCurRawValue(orderedOrientation[i]);
                            if (!sensorData.haveData) {
                                sensorData.haveData = true;
                            } else {
                                if (axis.isChanged()) {
                                    joyInput.addEvent(new JoyAxisEvent(axis, axis.getJoystickAxisValue(), axis.getJoystickAxisValue()));
                                }
                            }
                        }
                    }
                } else if (sensorData != null) {
                    if (!sensorData.haveData) {
                        sensorData.haveData = true;
                    }
                }

                return true;
            } else {
                logger.log(Level.FINE, "remapCoordinateSystem failed");
            }

        } else {
            logger.log(Level.FINE, "getRotationMatrix returned false");
        }

        return false;
    }

    // Start of JoyInput methods

    public Joystick loadJoystick(int joyId, InputManager inputManager) {
        SensorData sensorData;
        AndroidSensorJoystickAxis axis;

        AndroidSensorJoystick joystick = new AndroidSensorJoystick(inputManager,
                                    joyInput,
                                    joyId,
                                    "AndroidSensorsJoystick");

        List<Sensor> availSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        if (logger.isLoggable(Level.FINE)) {
            for (Sensor sensor : availSensors) {
                logger.log(Level.FINE, "{0} Sensor is available, Type: {1}, Vendor: {2}, Version: {3}",
                        new Object[]{sensor.getName(), sensor.getType(), sensor.getVendor(), sensor.getVersion()});
            }
        }

        // manually create orientation sensor data since orientation is not a physical sensor
        sensorData = new SensorData(Sensor.TYPE_ORIENTATION, null);
        sensorData.lastValues = new float[3];
        sensors.put(Sensor.TYPE_ORIENTATION, sensorData);
        axis = joystick.addAxis(SensorJoystickAxis.ORIENTATION_X, SensorJoystickAxis.ORIENTATION_X, joystick.getAxisCount(), FastMath.HALF_PI);
        joystick.setYAxis(axis); // joystick y axis = rotation around device x axis
        sensorData.axes.add(axis);
        axis = joystick.addAxis(SensorJoystickAxis.ORIENTATION_Y, SensorJoystickAxis.ORIENTATION_Y, joystick.getAxisCount(), FastMath.HALF_PI);
        joystick.setXAxis(axis); // joystick x axis = rotation around device y axis
        sensorData.axes.add(axis);
        axis = joystick.addAxis(SensorJoystickAxis.ORIENTATION_Z, SensorJoystickAxis.ORIENTATION_Z, joystick.getAxisCount(), FastMath.HALF_PI);
        sensorData.axes.add(axis);

        // add axes for physical sensors
        sensorData = initSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensorData != null) {
            sensorData.lastValues = new float[3];
            sensors.put(Sensor.TYPE_MAGNETIC_FIELD, sensorData);
//            axis = joystick.addAxis(SensorJoystickAxis.MAGNETIC_X, "MagneticField_X", joystick.getAxisCount(), 1f);
//            sensorData.axes.add(axis);
//            axis = joystick.addAxis(SensorJoystickAxis.MAGNETIC_Y, "MagneticField_Y", joystick.getAxisCount(), 1f);
//            sensorData.axes.add(axis);
//            axis = joystick.addAxis(SensorJoystickAxis.MAGNETIC_Z, "MagneticField_Z", joystick.getAxisCount(), 1f);
//            sensorData.axes.add(axis);
        }

        sensorData = initSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensorData != null) {
            sensorData.lastValues = new float[3];
            sensors.put(Sensor.TYPE_ACCELEROMETER, sensorData);
//            axis = joystick.addAxis(SensorJoystickAxis.ACCELEROMETER_X, "Accelerometer_X", joystick.getAxisCount(), 1f);
//            sensorData.axes.add(axis);
//            axis = joystick.addAxis(SensorJoystickAxis.ACCELEROMETER_Y, "Accelerometer_Y", joystick.getAxisCount(), 1f);
//            sensorData.axes.add(axis);
//            axis = joystick.addAxis(SensorJoystickAxis.ACCELEROMETER_Z, "Accelerometer_Z", joystick.getAxisCount(), 1f);
//            sensorData.axes.add(axis);
        }

//        sensorData = initSensor(Sensor.TYPE_GYROSCOPE);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[3];
//        }
//
//        sensorData = initSensor(Sensor.TYPE_GRAVITY);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[3];
//        }
//
//        sensorData = initSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[3];
//        }
//
//        sensorData = initSensor(Sensor.TYPE_ROTATION_VECTOR);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[4];
//        }
//
//        sensorData = initSensor(Sensor.TYPE_PROXIMITY);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[1];
//        }
//
//        sensorData = initSensor(Sensor.TYPE_LIGHT);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[1];
//        }
//
//        sensorData = initSensor(Sensor.TYPE_PRESSURE);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[1];
//        }
//
//        sensorData = initSensor(Sensor.TYPE_TEMPERATURE);
//        if (sensorData != null) {
//            sensorData.lastValues = new float[1];
//        }


        loaded = true;
        return joystick;
    }

    public void update() {
        if (!loaded) {
            return;
        }
        updateOrientation();
    }

    public void destroy() {
        logger.log(Level.FINE, "Doing Destroy.");
        pauseSensors();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        sensors.clear();
        loaded = false;
        sensorManager = null;
    }

    // Start of Android SensorEventListener methods

    @Override
    public void onSensorChanged(SensorEvent se) {
        if (!loaded) {
            return;
        }
//        logger.log(Level.FINE, "onSensorChanged for {0}: accuracy: {1}, values: {2}",
//                new Object[]{se.sensor.getName(), se.accuracy, se.values});

        int sensorType = se.sensor.getType();

        SensorData sensorData = sensors.get(sensorType);
        if (sensorData != null) {
//            logger.log(Level.FINE, "sensorData name: {0}, enabled: {1}, unreliable: {2}",
//                    new Object[]{sensorData.sensor.getName(), sensorData.enabled, sensorData.sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE});
        }
        if (sensorData != null && sensorData.sensor.equals(se.sensor) && sensorData.enabled) {

            if (sensorData.sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                return;
            }
            synchronized(sensorData.valuesLock) {
                for (int i=0; i<sensorData.lastValues.length; i++) {
                    sensorData.lastValues[i] = se.values[i];
                }
            }

            if (sensorData.axes.size() > 0) {
                AndroidSensorJoystickAxis axis;
                for (int i=0; i<se.values.length; i++) {
                    axis = sensorData.axes.get(i);
                    if (axis != null) {
                        axis.setCurRawValue(se.values[i]);
                        if (!sensorData.haveData) {
                            sensorData.haveData = true;
                        } else {
                            if (axis.isChanged()) {
                                JoyAxisEvent event = new JoyAxisEvent(axis, axis.getJoystickAxisValue(), axis.getJoystickAxisValue());
//                                logger.log(Level.INFO, "adding JoyAxisEvent: {0}", event);
                                joyInput.addEvent(event);
//                                joyHandler.addEvent(new JoyAxisEvent(axis, axis.getJoystickAxisValue()));
                            }
                        }
                    }
                }
            } else {
                if (!sensorData.haveData) {
                    sensorData.haveData = true;
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        int sensorType = sensor.getType();
        SensorData sensorData = sensors.get(sensorType);
        if (sensorData != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "onAccuracyChanged for {0}: accuracy: {1}",
                        new Object[]{sensor.getName(), i});
                logger.log(Level.FINE, "MaxRange: {0}, Resolution: {1}",
                        new Object[]{sensor.getMaximumRange(), sensor.getResolution()});
            }
            sensorData.sensorAccuracy = i;
        }
    }

    // End of SensorEventListener methods

    protected class AndroidSensorJoystick extends AbstractJoystick {
        private JoystickAxis nullAxis;
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povX;
        private JoystickAxis povY;

        public AndroidSensorJoystick( InputManager inputManager, JoyInput joyInput,
                                int joyId, String name){

            super( inputManager, joyInput, joyId, name );

            this.nullAxis = new DefaultJoystickAxis( getInputManager(), this, -1,
                                                     "Null", "null", false, false, 0 );
            this.xAxis = nullAxis;
            this.yAxis = nullAxis;
            this.povX = nullAxis;
            this.povY = nullAxis;

        }

        protected AndroidSensorJoystickAxis addAxis(String axisName, String logicalName, int axisNum, float maxRawValue) {
            AndroidSensorJoystickAxis axis;

            axis = new AndroidSensorJoystickAxis(
                    getInputManager(),          // InputManager (InputManager)
                    this,                       // parent Joystick (Joystick)
                    axisNum,                    // Axis Index (int)
                    axisName,                   // Axis Name (String)
                    logicalName,                // Logical ID (String)
                    true,                       // isAnalog (boolean)
                    false,                      // isRelative (boolean)
                    0.01f,                      // Axis Deadzone (float)
                    maxRawValue);               // Axis Max Raw Value (float)

            super.addAxis(axis);

            return axis;
        }

        protected void setXAxis(JoystickAxis axis) {
            xAxis = axis;
        }
        protected void setYAxis(JoystickAxis axis) {
            yAxis = axis;
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

    }

    public class AndroidSensorJoystickAxis extends DefaultJoystickAxis implements SensorJoystickAxis {
        float zeroRawValue = 0f;
        float curRawValue = 0f;
        float lastRawValue = 0f;
        boolean hasChanged = false;
        float maxRawValue = FastMath.HALF_PI;
        boolean enabled = true;

        public AndroidSensorJoystickAxis(InputManager inputManager, Joystick parent,
                           int axisIndex, String name, String logicalId,
                           boolean isAnalog, boolean isRelative, float deadZone,
                           float maxRawValue) {
            super(inputManager, parent, axisIndex, name, logicalId, isAnalog, isRelative, deadZone);

            this.maxRawValue = maxRawValue;
        }

        @Override
        public float getMaxRawValue() {
            return maxRawValue;
        }

        @Override
        public void setMaxRawValue(float maxRawValue) {
            this.maxRawValue = maxRawValue;
        }

        protected float getLastRawValue() {
            return lastRawValue;
        }
        protected void setCurRawValue(float rawValue) {
            this.curRawValue = rawValue;
            if (Math.abs(curRawValue - lastRawValue) > getDeadZone()) {
                hasChanged = true;
                lastRawValue = curRawValue;
            } else {
                hasChanged = false;
            }
        }

        protected float getJoystickAxisValue() {
            return (lastRawValue-zeroRawValue) / maxRawValue;
        }

        protected boolean isChanged() {
            return hasChanged;
        }

        @Override
        public void calibrateCenter() {
            zeroRawValue = lastRawValue;
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Calibrating axis {0} to {1}",
                        new Object[]{getName(), zeroRawValue});
            }
        }

    }
}
