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

import com.jme3.input.controls.JoyButtonTrigger;

/**
 *  Default implementation of the JoystickButton interface.
 *
 *  @author Paul Speed
 */
public class DefaultJoystickButton implements JoystickButton {
    
    private InputManager inputManager;
    private Joystick parent;
    private int buttonIndex;
    private String name;
    private String logicalId;

    public DefaultJoystickButton( InputManager inputManager, Joystick parent, int buttonIndex,
                                  String name, String logicalId ) {
        this.inputManager = inputManager;
        this.parent = parent;
        this.buttonIndex = buttonIndex;
        this.name = name;
        this.logicalId = logicalId;        
    }                                   

    /**
     * Assign the mapping name to receive events from the given button index
     * on the joystick.
     *
     * @param mappingName The mapping to receive joystick button events.
     */
    public void assignButton(String mappingName) {
        inputManager.addMapping(mappingName, new JoyButtonTrigger(parent.getJoyId(), buttonIndex));
    }
    
    /**
     *  Returns the joystick to which this axis object belongs.
     */
    public Joystick getJoystick() {
        return parent;
    } 

    /**
     *  Returns the name of this joystick.
     *
     *  @return the name of this joystick.
     */
    public String getName() {
        return name;
    } 

    /**
     *  Returns the logical identifier of this joystick axis.
     *
     *  @return the logical identifier of this joystick.
     */
    public String getLogicalId() {
        return logicalId;
    } 

    /**
     *  Returns the unique buttonId of this joystick axis within a given 
     *  InputManager context.
     *
     *  @return the buttonId of this joystick axis.
     */
    public int getButtonId() {
        return buttonIndex;
    }
     
    @Override
    public String toString(){
        return "JoystickButton[name=" + getName() + ", parent=" + parent.getName() + ", id=" + getButtonId() 
                                    + ", logicalId=" + getLogicalId() + "]";
    }
}
