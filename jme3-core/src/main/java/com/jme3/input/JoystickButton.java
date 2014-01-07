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
