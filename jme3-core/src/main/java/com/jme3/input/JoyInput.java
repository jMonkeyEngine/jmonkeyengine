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
 * A specific API for interfacing with joysticks or gaming controllers.
 */
public interface JoyInput extends Input {

    /**
     * The X axis for POV (point of view hat).
     */
    public static final int AXIS_POV_X = 254;
    
    /**
     * The Y axis for POV (point of view hat).
     */
    public static final int AXIS_POV_Y = 255;

    /**
     * Causes the joystick at <code>joyId</code> index to rumble with
     * the given amount.
     * <p>
     * The rumble continues until this method is called with 0 or
     * {@link #stopJoyRumble(int)} is called.
     * 
     * @param joyId The joystick index
     * @param amount Rumble amount. Should be between 0 and 1.
     */
    public default void setJoyRumble(int joyId, float amount) {
        setJoyRumble(joyId, amount, amount, Float.POSITIVE_INFINITY);
    }

    /**
     * Causes the joystick at <code>joyId</code> index to rumble with
     * separate high and low frequency amounts for the given duration.
     *
     * @param joyId The joystick index
     * @param amountHigh High frequency rumble amount. Should be between 0 and 1.
     * @param amountLow Low frequency rumble amount. Should be between 0 and 1.
     * @param duration Rumble duration in seconds.
     */
    public void setJoyRumble(int joyId, float amountHigh, float amountLow, float duration);

    /**
     * Stops any rumble currently playing on the joystick at <code>joyId</code>.
     *
     * @param joyId the joystick index
     */
    public default void stopJoyRumble(int joyId) {
        setJoyRumble(joyId, 0f);
    }
    
    /**
     * Loads a list of joysticks from the system.
     * 
     * @param inputManager The input manager requesting to load joysticks
     * @return A list of joysticks that are installed.
     */
    public Joystick[] loadJoysticks(InputManager inputManager);
}
