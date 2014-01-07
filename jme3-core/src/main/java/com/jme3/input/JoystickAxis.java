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
