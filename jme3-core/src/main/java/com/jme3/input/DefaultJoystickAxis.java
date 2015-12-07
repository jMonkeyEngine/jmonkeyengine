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

import com.jme3.input.controls.JoyAxisTrigger;

/**
 *  Default implementation of the JoystickAxis interface.
 *
 *  @author Paul Speed
 */
public class DefaultJoystickAxis implements JoystickAxis {

    private InputManager inputManager;
    private Joystick parent;
    private int axisIndex;
    private String name;
    private String logicalId;
    private boolean isAnalog;
    private boolean isRelative;
    private float deadZone;

    /**
     *  Creates a new joystick axis instance. Only used internally.
     */
    public DefaultJoystickAxis(InputManager inputManager, Joystick parent,
                               int axisIndex, String name, String logicalId,
                               boolean isAnalog, boolean isRelative, float deadZone ) {
        this.inputManager = inputManager;
        this.parent = parent;
        this.axisIndex = axisIndex;
        this.name = name;
        this.logicalId = logicalId;
        this.isAnalog = isAnalog;
        this.isRelative = isRelative;
        this.deadZone = deadZone;
    }

    /**
     *  Assign the mappings to receive events from the given joystick axis.
     *
     *  @param positiveMapping The mapping to receive events when the axis is negative
     *  @param negativeMapping The mapping to receive events when the axis is positive
     */
    public void assignAxis(String positiveMapping, String negativeMapping){
        if (axisIndex != -1) {
            inputManager.addMapping(positiveMapping, new JoyAxisTrigger(parent.getJoyId(), axisIndex, false));
            inputManager.addMapping(negativeMapping, new JoyAxisTrigger(parent.getJoyId(), axisIndex, true));
        }
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
     *  Returns the axisId of this joystick axis.
     *
     *  @return the axisId of this joystick axis.
     */
    public int getAxisId() {
        return axisIndex;
    }

    /**
     *  Returns true if this is an analog axis, meaning the values
     *  are a continuous range instead of 1, 0, and -1.
     */
    public boolean isAnalog() {
        return isAnalog;
    }
    
    /**
     *  Returns true if this axis presents relative values.
     */
    public boolean isRelative() {
        return isRelative;
    }
    
    /**
     *  Returns the suggested dead zone for this axis.  Values less than this
     *  can be safely ignored.
     */
    public float getDeadZone() {
        return deadZone;
    }        

    /**
     *  Sets/overrides the dead zone for this axis.  This indicates that values
     *  within +/- deadZone should be ignored.
     */
    public void setDeadZone( float f ) {
        this.deadZone = f;
    }     

    @Override
    public String toString(){
        return "JoystickAxis[name=" + name + ", parent=" + parent.getName() + ", id=" + axisIndex 
                                    + ", logicalId=" + logicalId + ", isAnalog=" + isAnalog
                                    + ", isRelative=" + isRelative + ", deadZone=" + deadZone + "]";
    }
}
