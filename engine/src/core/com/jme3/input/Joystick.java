package com.jme3.input;

import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;

/**
 * A joystick represents a single joystick that is installed in the system.
 * 
 * @author Kirill Vainer
 */
public final class Joystick {

    private InputManager inputManager;
    private JoyInput joyInput;
    private int joyId;
    private int buttonCount;
    private int axisCount;
    private int axisXIndex, axisYIndex;
    private String name;

    /**
     * Creates a new joystick instance. Only used internally.
     */
    public Joystick(InputManager inputManager, JoyInput joyInput,
                    int joyId, String name, int buttonCount, int axisCount,
                    int xAxis, int yAxis){
        this.inputManager = inputManager;
        this.joyInput = joyInput;
        this.joyId = joyId;
        this.name = name;
        this.buttonCount = buttonCount;
        this.axisCount = axisCount;

        this.axisXIndex = xAxis;
        this.axisYIndex = yAxis;
    }

    /**
     * Rumbles the joystick for the given amount/magnitude.
     * 
     * @param amount The amount to rumble. Should be between 0 and 1.
     */
    public void rumble(float amount){
        joyInput.setJoyRumble(joyId, amount);
    }

    /**
     * Assign the mapping name to receive events from the given button index
     * on the joystick.
     * 
     * @param mappingName The mapping to receive joystick button events.
     * @param buttonId The button index.
     * 
     * @see Joystick#getButtonCount() 
     */
    public void assignButton(String mappingName, int buttonId){
        if (buttonId < 0 || buttonId >= buttonCount)
            throw new IllegalArgumentException();

        inputManager.addMapping(mappingName, new JoyButtonTrigger(joyId, buttonId));
    }

    /**
     * Assign the mappings to receive events from the given joystick axis.
     * 
     * @param positiveMapping The mapping to receive events when the axis is negative
     * @param negativeMapping The mapping to receive events when the axis is positive
     * @param axisId The axis index.
     * 
     * @see Joystick#getAxisCount() 
     */
    public void assignAxis(String positiveMapping, String negativeMapping, int axisId){
        inputManager.addMapping(positiveMapping, new JoyAxisTrigger(joyId, axisId, false));
        inputManager.addMapping(negativeMapping, new JoyAxisTrigger(joyId, axisId, true));
    }

    /**
     * Gets the index number for the X axis on the joystick.
     * 
     * <p>E.g. for most gamepads, the left control stick X axis will be returned.
     * 
     * @return The axis index for the X axis for this joystick.
     * 
     * @see Joystick#assignAxis(java.lang.String, java.lang.String, int) 
     */
    public int getXAxisIndex(){
        return axisXIndex;
    }

    /**
     * Gets the index number for the Y axis on the joystick.
     * 
     * <p>E.g. for most gamepads, the left control stick Y axis will be returned.
     * 
     * @return The axis index for the Y axis for this joystick.
     * 
     * @see Joystick#assignAxis(java.lang.String, java.lang.String, int) 
     */
    public int getYAxisIndex(){
        return axisYIndex;
    }

    /**
     * Returns the number of axes on this joystick.
     * 
     * @return the number of axes on this joystick.
     */
    public int getAxisCount() {
        return axisCount;
    }

    /**
     * Returns the number of buttons on this joystick.
     * 
     * @return the number of buttons on this joystick.
     */
    public int getButtonCount() {
        return buttonCount;
    }

    /**
     * Returns the name of this joystick.
     * 
     * @return the name of this joystick.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return "Joystick[name=" + name + ", id=" + joyId + ", buttons=" + buttonCount
                                + ", axes=" + axisCount + "]";
    }

}
