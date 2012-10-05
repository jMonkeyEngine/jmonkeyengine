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
     * Returns the JoystickAxis with the specified logical ID.
     *
     * @param logicalId The id of the axis to search for as returned by JoystickAxis.getLogicalId().
     */
    public JoystickAxis getAxis(String logicalId);

    /**
     * Returns a read-only list of all joystick axes for this Joystick.
     */
    public List<JoystickAxis> getAxes();

    /**
     * Returns the JoystickButton with the specified logical ID.
     *
     * @param logicalId The id of the axis to search for as returned by JoystickButton.getLogicalId().
     */
    public JoystickButton getButton(String logicalId);

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
