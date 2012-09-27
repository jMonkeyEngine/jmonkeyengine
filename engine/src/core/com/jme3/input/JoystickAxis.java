package com.jme3.input;

import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;

/**
 *  Represents a single axis of a Joystick.
 *
 *  @author Paul Speed
 */
public interface JoystickAxis {

    public static final String X_AXIS = "x";
    public static final String Y_AXIS = "y";
    public static final String Z_AXIS = "z";
    public static final String Z_ROTATION = "rz";

    public static final String POV_X = "pov_x";
    public static final String POV_Y = "pov_y";

    /**
     *  Assign the mappings to receive events from the given joystick axis.
     *
     *  @param positiveMapping The mapping to receive events when the axis is negative
     *  @param negativeMapping The mapping to receive events when the axis is positive
     */
    public void assignAxis(String positiveMapping, String negativeMapping);
    
    /**
     *  Returns the joystick to which this axis object belongs.
     */
    public Joystick getJoystick(); 

    /**
     *  Returns the name of this joystick.
     *
     *  @return the name of this joystick.
     */
    public String getName(); 

    /**
     *  Returns the logical identifier of this joystick axis.
     *
     *  @return the logical identifier of this joystick.
     */
    public String getLogicalId(); 

    /**
     *  Returns the unique axisId of this joystick axis within a given 
     *  InputManager context.
     *
     *  @return the axisId of this joystick axis.
     */
    public int getAxisId(); 

    /**
     *  Returns true if this is an analog axis, meaning the values
     *  are a continuous range instead of 1, 0, and -1.
     */
    public boolean isAnalog(); 
    
    /**
     *  Returns true if this axis presents relative values.
     */
    public boolean isRelative(); 
    
    /**
     *  Returns the suggested dead zone for this axis.  Values less than this
     *  can be safely ignored.
     */
    public float getDeadZone();
}
