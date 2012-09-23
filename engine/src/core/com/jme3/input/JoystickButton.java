package com.jme3.input;

import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;

/**
 *  Represents a single button of a Joystick.
 *
 *  @author Paul Speed
 */
public interface JoystickButton {

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
