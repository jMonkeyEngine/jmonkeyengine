/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.input.android;

import android.view.KeyEvent;
import android.view.MotionEvent;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import java.util.logging.Logger;

/**
 * <code>AndroidJoyInput14</code> extends <code>AndroidJoyInput</code>
 * to include support for physical joysticks/gamepads.
 *
 * @author iwgeric
 */
public class AndroidJoyInput14 extends AndroidJoyInput {
    private static final Logger logger = Logger.getLogger(AndroidJoyInput14.class.getName());

    private AndroidJoystickJoyInput14 joystickJoyInput;

    public AndroidJoyInput14(AndroidInputHandler inputHandler) {
        super(inputHandler);
        joystickJoyInput = new AndroidJoystickJoyInput14(this);
    }

    /**
     * Pauses the joystick device listeners to save battery life if they are not needed.
     * Used to pause when the activity pauses
     */
    @Override
    public void pauseJoysticks() {
        super.pauseJoysticks();

        if (joystickJoyInput != null) {
            joystickJoyInput.pauseJoysticks();
        }
    }

    /**
     * Resumes the joystick device listeners.
     * Used to resume when the activity comes to the top of the stack
     */
    @Override
    public void resumeJoysticks() {
        super.resumeJoysticks();
        if (joystickJoyInput != null) {
            joystickJoyInput.resumeJoysticks();
        }

    }

    @Override
    public void destroy() {
        super.destroy();
        if (joystickJoyInput != null) {
            joystickJoyInput.destroy();
        }
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager) {
        // load the simulated joystick for device orientation
        super.loadJoysticks(inputManager);
        // load physical gamepads/joysticks
        joystickList.addAll(joystickJoyInput.loadJoysticks(joystickList.size(), inputManager));
        // return the list of joysticks back to InputManager
        return joystickList.toArray( new Joystick[joystickList.size()] );
    }

    public boolean onGenericMotion(MotionEvent event) {
        return joystickJoyInput.onGenericMotion(event);
    }

    public boolean onKey(KeyEvent event) {
        return joystickJoyInput.onKey(event);
    }

}
