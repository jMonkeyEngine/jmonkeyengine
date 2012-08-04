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

package com.jme3.input;

import java.util.Set;

/**
 * A specific API for interfacing with sensors.
 *
 * In order to conserve battery power for handheld devices, sensors must be
 * enabled before data will be sent.  Use the setEnable method to enable or disable
 * the sensors.
 *
 * Sensor speed can also be set.  Constants in this class are used so that each
 * platform implementation can decide what absolute update rate to use. Use the
 * setSensorFrequency method to set the desired update rate.
 *
 * In order to minimize the amount of data sent to the application, there is a
 * method available to set how much change is required between sensor readings
 * before the new updated data is sent.  Use the setSensorMinChange method to set
 * a minimum data change percentage (percentage of max sensor range).  Data will
 * not be sent to the application until the data has changed by this amount.
 *
 *
 *
 *
 *
 * @author iwgeric
 */
public interface SensorInput extends Input {

    /**
     * Orientation Sensor.  Values returned in the onMotionSensorChanged event
     * are in radians.
     */
    public static final int SENSOR_TYPE_ORIENTATION = 0;

    /**
     * Accelerometer Sensor.  Values returned in the onMotionSensorChanged event
     * are in m/s^2.  Values include gravity.  To get true device acceleration,
     * gravity must be removed.
     */
    public static final int SENSOR_TYPE_ACCELEROMETER = 1;

    /**
     * Magnetic Field Sensor.  Values returned in the onMotionSensorChanged event
     * are in micro-Tesla (uT).
     */
    public static final int SENSOR_TYPE_MAGNETIC_FIELD = 2;

    /**
     * Slowest Sensor Update Speed
     */
    public static final int SENSOR_SPEED_SLOW = 0;

    /**
     * Medium Sensor Update Speed
     */
    public static final int SENSOR_SPEED_MEDIUM = 1;

    /**
     * Fastest Sensor Update Speed
     */
    public static final int SENSOR_SPEED_FAST = 2;

    /**
     * Returns whether a sensor is enabled or not.
     *
     * @param sensorType The sensor type.
     * @return whether a sensor is enabled or not.
     */
    public boolean isEnabled(int sensorType);

    /**
     * Sets enable/disable for a specific sensor type.
     *
     * @param sensorType The sensor type.
     * @param enable True to enable, False to disable.
     */
    public void setEnable(int sensorType, boolean enable);

    /**
     * Sets enable/disable for all sensor types.
     *
     * @param enable True to enable, False to disable.
     */
    public void setEnable(boolean enable);

    /**
     * Returns a list of available sensor types.
     *
     * @return a list of available sensor types.
     */
    public Set<Integer> getSensorTypes();

    /**
     * Set the minimum amount of change that is required before an event
     * is created for the sensor.  minChangePercent is defined as a percentage
     * of the maximum sensor range.
     *
     * @param sensorType The sensor type.
     * @param minChangePercent Percentage of changed required before creating an event.
     */
    public void setSensorMinChange(int sensorType, float minChangePercent);

    /**
     * Set the update frequency for the sensor.  Use the defined constants in
     * SensorInput for setting the speed becuase the actual update frequency is
     * platform dependant.
     *
     * @param sensorType The sensor type.
     * @param updateSpeed Target update speed as a constant (do not use absolute values)
     */
    public void setSensorFrequency(int sensorType, int updateSpeed);

}
