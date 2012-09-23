package com.jme3.input;

import java.util.List;

/**
 * A joystick represents a single joystick that is installed in the system.
 *
 * @author Paul Speed, Kirill Vainer
 */
public interface Joystick {

    /**
     * Rumbles the joystick for the given amount/magnitude.
     *
     * @param amount The amount to rumble. Should be between 0 and 1.
     */
    public void rumble(float amount);

    /**
     * Assign the mapping name to receive events from the given button index
     * on the joystick.
     *
     * @param mappingName The mapping to receive joystick button events.
     * @param buttonId The button index.
     *
     * @see Joystick#getButtonCount()
     * @deprecated Use JoystickButton.assignButton() instead.
     */
    public void assignButton(String mappingName, int buttonId);

    /**
     * Assign the mappings to receive events from the given joystick axis.
     *
     * @param positiveMapping The mapping to receive events when the axis is negative
     * @param negativeMapping The mapping to receive events when the axis is positive
     * @param axisId The axis index.
     *
     * @see Joystick#getAxisCount()
     * @deprecated Use JoystickAxis.assignAxis() instead.
     */
    public void assignAxis(String positiveMapping, String negativeMapping, int axisId); 

    /**
     * Returns the JoystickAxis with the specified name.
     *
     * @param name The name of the axis to search for as returned by JoystickAxis.getName().
     */
    public JoystickAxis getAxis(String name);

    /**
     * Returns a read-only list of all joystick axes for this Joystick.
     */
    public List<JoystickAxis> getAxes();

    /**
     * Returns the JoystickButton with the specified name.
     *
     * @param name The name of the button to search for as returned by JoystickButton.getName().
     */
    public JoystickButton getButton(String name);

    /**
     * Returns a read-only list of all joystick buttons for this Joystick.
     */
    public List<JoystickButton> getButtons();

    /**
     * Returns the X axis for this joystick.
     *
     * <p>E.g. for most gamepads, the left control stick X axis will be returned.
     *
     * @see JoystickAxis#assignAxis(java.lang.String, java.lang.String)
     */
    public JoystickAxis getXAxis();     

    /**
     * Returns the Y axis for this joystick.
     *
     * <p>E.g. for most gamepads, the left control stick Y axis will be returned.
     *
     * @see JoystickAxis#assignAxis(java.lang.String, java.lang.String)
     */
    public JoystickAxis getYAxis();     

    /**
     * Returns the POV X axis for this joystick.  This is a convenience axis 
     * providing an x-axis subview of the HAT axis.
     *
     * @see JoystickAxis#assignAxis(java.lang.String, java.lang.String)
     */
    public JoystickAxis getPovXAxis();     

    /**
     * Returns the POV Y axis for this joystick.  This is a convenience axis 
     * providing an y-axis subview of the HAT axis.
     *
     * @see JoystickAxis#assignAxis(java.lang.String, java.lang.String)
     */
    public JoystickAxis getPovYAxis();     

    /**
     * Gets the index number for the X axis on the joystick.
     *
     * <p>E.g. for most gamepads, the left control stick X axis will be returned.
     *
     * @return The axis index for the X axis for this joystick.
     *
     * @see Joystick#assignAxis(java.lang.String, java.lang.String, int)
     */
    public int getXAxisIndex();

    /**
     * Gets the index number for the Y axis on the joystick.
     *
     * <p>E.g. for most gamepads, the left control stick Y axis will be returned.
     *
     * @return The axis index for the Y axis for this joystick.
     *
     * @see Joystick#assignAxis(java.lang.String, java.lang.String, int)
     */
    public int getYAxisIndex();

    /**
     * Returns the number of axes on this joystick.
     *
     * @return the number of axes on this joystick.
     */
    public int getAxisCount(); 

    /**
     * Returns the number of buttons on this joystick.
     *
     * @return the number of buttons on this joystick.
     */
    public int getButtonCount();

    /**
     * Returns the name of this joystick.
     *
     * @return the name of this joystick.
     */
    public String getName();

    /**
     * Returns the joyId of this joystick.
     *
     * @return the joyId of this joystick.
     */
    public int getJoyId();

}
