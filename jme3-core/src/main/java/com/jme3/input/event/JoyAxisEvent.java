/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.input.event;

import com.jme3.input.InputManager;
import com.jme3.input.JoystickAxis;

/**
 * Joystick axis event.
 * 
 * @author Kirill Vainer, Paul Speed
 */
public class JoyAxisEvent extends InputEvent {

    private JoystickAxis axis;
    private float value;

    public JoyAxisEvent(JoystickAxis axis, float value) {
        this.axis = axis;
        this.value = value;
    }

    /**
     * Returns the JoystickAxis that triggered this event.
     *
     * @see com.jme3.input.JoystickAxis#assignAxis(java.lang.String, java.lang.String)
     */
    public JoystickAxis getAxis() {
        return axis;
    }

    /**
     * Returns the joystick axis index.
     * 
     * @return joystick axis index.
     * 
     * @see com.jme3.input.JoystickAxis#assignAxis(java.lang.String, java.lang.String)
     */
    public int getAxisIndex() {
        return axis.getAxisId();
    }

    /**
     * The joystick index.
     * 
     * @return joystick index.
     * 
     * @see InputManager#getJoysticks() 
     */
    public int getJoyIndex() {
        return axis.getJoystick().getJoyId();
    }

    /**
     * The value of the axis.
     * 
     * @return value of the axis.
     */
    public float getValue() {
        return value;
    }
}
