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
package com.jme3.input.event;

import com.jme3.input.Joystick;
import com.jme3.input.JoystickButton;

/**
 * Joystick button event.
 * 
 * @author Kirill Vainer, Paul Speed
 */
public class JoyButtonEvent extends InputEvent {

    private JoystickButton button;
    private boolean pressed;

    public JoyButtonEvent(JoystickButton button, boolean pressed) {
        this.button = button;
        this.pressed = pressed;
    }

    /**
     * Returns the JoystickButton that triggered this event.
     *
     * @see JoystickAxis#assignAxis(java.lang.String, java.lang.String, int) 
     */
    public JoystickButton getButton() {
        return button;
    }

    /**
     * The button index.
     * 
     * @return button index.
     * 
     * @see Joystick#assignButton(java.lang.String, int) 
     */
    public int getButtonIndex() {
        return button.getButtonId();
    }

    /**
     * The joystick index.
     * 
     * @return joystick index.
     * 
     * @see com.jme3.input.InputManager#getJoysticks() 
     */
    public int getJoyIndex() {
        return button.getJoystick().getJoyId();
    }

    /**
     * Returns true if the event was a button press,
     * returns false if the event was a button release.
     * 
     * @return true if the event was a button press,
     * false if the event was a button release.
     */
    public boolean isPressed() {
        return pressed;
    }



}
