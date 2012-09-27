package com.jme3.input;

import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;

/**
 *  Represents a single button of a Joystick.
 *
 *  @author Paul Speed
 */
public interface JoystickButton {

    public static final String BUTTON_0 = "0";
    public static final String BUTTON_1 = "1";
    public static final String BUTTON_2 = "2";
    public static final String BUTTON_3 = "3";
    public static final String BUTTON_4 = "4";
    public static final String BUTTON_5 = "5";
    public static final String BUTTON_6 = "6";
    public static final String BUTTON_7 = "7";
    public static final String BUTTON_8 = "8";
    public static final String BUTTON_9 = "9";
    public static final String BUTTON_10 = "10";
    public static final String BUTTON_11 = "11";

    /**
     * Assign the mapping name to receive events from the given button index
     * on the joystick.
     *
     * @param mappingName The mapping to receive joystick button events.
     */
    public void assignButton(String mappingName);
    
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
     *  Returns the unique buttonId of this joystick axis within a given 
     *  InputManager context.
     *
     *  @return the buttonId of this joystick axis.
     */
    public int getButtonId(); 
}
