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
