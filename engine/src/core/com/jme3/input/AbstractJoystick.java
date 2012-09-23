package com.jme3.input;

import java.util.*;

import com.jme3.input.controls.JoyAxisTrigger;
import com.jme3.input.controls.JoyButtonTrigger;

/**
 * A joystick represents a single joystick that is installed in the system.
 *
 * @author Kirill Vainer, Paul Speed
 */
public abstract class AbstractJoystick implements Joystick {

    private InputManager inputManager;
    private JoyInput joyInput;
    private int joyId;
    private String name;
    
    private List<JoystickAxis> axes = new ArrayList<JoystickAxis>();       
    private List<JoystickButton> buttons = new ArrayList<JoystickButton>();       

    /**
     * Creates a new joystick instance. Only used internally.
     */
    protected AbstractJoystick(InputManager inputManager, JoyInput joyInput,
                               int joyId, String name) {
        this.inputManager = inputManager;
        this.joyInput = joyInput;
        this.joyId = joyId;
        this.name = name;
    }
    
    protected InputManager getInputManager() {
        return inputManager;
    }
    
    protected JoyInput getJoyInput() {
        return joyInput; 
    }

    protected void addAxis( JoystickAxis axis ) {
        axes.add(axis);
    }

    protected void addButton( JoystickButton button ) {
        buttons.add(button);
    }

    /**
     * Rumbles the joystick for the given amount/magnitude.
     *
     * @param amount The amount to rumble. Should be between 0 and 1.
     */
    @Override
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
     * @deprecated Use JoystickButton.assignButton() instead.
     */
    @Override
    public void assignButton(String mappingName, int buttonId){
        if (buttonId < 0 || buttonId >= getButtonCount())
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
     * @deprecated Use JoystickAxis.assignAxis() instead.
     */
    @Override
    public void assignAxis(String positiveMapping, String negativeMapping, int axisId){
    
        // For backwards compatibility
        if( axisId == JoyInput.AXIS_POV_X ) {
            axisId = getPovXAxis().getAxisId();
        } else if( axisId == JoyInput.AXIS_POV_Y ) {
            axisId = getPovYAxis().getAxisId();
        }
    
        inputManager.addMapping(positiveMapping, new JoyAxisTrigger(joyId, axisId, false));
        inputManager.addMapping(negativeMapping, new JoyAxisTrigger(joyId, axisId, true));
    }

    /**
     * Returns the JoystickAxis with the specified name.
     *
     * @param name The name of the axis to search for as returned by JoystickAxis.getName().
     */
    @Override
    public JoystickAxis getAxis(String name) {
        for( JoystickAxis axis : axes ) {
            if( axis.getName().equals(name) )
                return axis;
        }
        return null;
    }

    /**
     * Returns a read-only list of all joystick axes for this Joystick.
     */
    @Override
    public List<JoystickAxis> getAxes() {
        return Collections.unmodifiableList(axes);
    }

    /**
     * Returns the number of axes on this joystick.
     *
     * @return the number of axes on this joystick.
     */
    @Override
    public int getAxisCount() {
        return axes.size();
    } 

    /**
     * Returns the JoystickButton with the specified name.
     *
     * @param name The name of the button to search for as returned by JoystickButton.getName().
     */
    @Override
    public JoystickButton getButton(String name) {
        for( JoystickButton b : buttons ) {
            if( b.getName().equals(name) )
                return b;
        }
        return null;
    }

    /**
     * Returns a read-only list of all joystick buttons for this Joystick.
     */
    @Override
    public List<JoystickButton> getButtons() {
        return Collections.unmodifiableList(buttons);
    }     

    /**
     * Returns the number of buttons on this joystick.
     *
     * @return the number of buttons on this joystick.
     */
    @Override
    public int getButtonCount() {
        return buttons.size();
    }
    
    /**
     * Returns the name of this joystick.
     *
     * @return the name of this joystick.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the joyId of this joystick.
     *
     * @return the joyId of this joystick.
     */
    @Override
    public int getJoyId() {
        return joyId;
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
    @Override
    public int getXAxisIndex(){
        return getXAxis().getAxisId();
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
    @Override
    public int getYAxisIndex(){
        return getYAxis().getAxisId();
    }

    @Override
    public String toString(){
        return "Joystick[name=" + name + ", id=" + joyId + ", buttons=" + getButtonCount()
                                + ", axes=" + getAxisCount() + "]";
    }
}
