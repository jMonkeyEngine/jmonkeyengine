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
package com.jme3.input;

/**
 * Represents a joystick axis based on an external sensor
 * (ie. Android Device Orientation sensors)
 * Sensor joystick axes can be calibrated to 
 * set the zero position dynamically
 * 
 * @author iwgeric
 */
public interface SensorJoystickAxis {
    public static String ORIENTATION_X = "Orientation_X";
    public static String ORIENTATION_Y = "Orientation_Y";
    public static String ORIENTATION_Z = "Orientation_Z";
    

    /**
     * Calibrates the axis to the current value.  Future axis values will be
     * sent as a delta from the calibratation value.
     */
    public void calibrateCenter();
    
    /**
     * Method to allow users to set the raw sensor value that represents
     * the maximum joystick axis value.  Values sent to InputManager are scaled
     * using the maxRawValue.
     * 
     * @param maxRawValue Raw sensor value that will be used to scale joystick axis value
     */
    public void setMaxRawValue(float maxRawValue);
    
    /**
     * Returns the current maximum raw sensor value that is being used to scale
     * the joystick axis value.
     * 
     * @return maxRawValue The current maximum raw sensor value used for scaling the joystick axis value
     */
    public float getMaxRawValue();
}
