/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.input.virtual;

import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;

/**
 * Xbox-like layout that only shows controls currently bound in the input manager.
 */
public class VirtualJoystickDynamicLayout extends VirtualJoystickXboxLayout {

    private final boolean useTouchForRightAnalog;
    private long visibilityMask = -1L;

    public VirtualJoystickDynamicLayout(boolean useTouchForRightAnalog) {
        this.useTouchForRightAnalog = useTouchForRightAnalog;
    }

    @Override
    public void update(VirtualJoystick joystick) {
        if (joystick == null) {
            return;
        }

        boolean leftTrigger = joystick.isButtonBound(JoystickButton.BUTTON_XBOX_LT)
                || joystick.isAxisBound(JoystickAxis.AXIS_XBOX_LEFT_TRIGGER);
        boolean rightTrigger = joystick.isButtonBound(JoystickButton.BUTTON_XBOX_RT)
                || joystick.isAxisBound(JoystickAxis.AXIS_XBOX_RIGHT_TRIGGER);

        boolean leftStick = joystick.isAxisBound(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X)
                || joystick.isAxisBound(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y);
        boolean rightStick = joystick.isAxisBound(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X)
                || joystick.isAxisBound(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y);
        if (useTouchForRightAnalog && joystick.hasPointerLookBindings()) {
            rightStick = false;
        }

        long nextVisibilityMask = 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_A) ? 1L : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_B) ? 1L << 1 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_X) ? 1L << 2 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_Y) ? 1L << 3 : 0L;
        nextVisibilityMask |= leftTrigger ? 1L << 4 : 0L;
        nextVisibilityMask |= rightTrigger ? 1L << 5 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_LB) ? 1L << 6 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_RB) ? 1L << 7 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_BACK) ? 1L << 8 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_START) ? 1L << 9 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_L3) ? 1L << 10 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_R3) ? 1L << 11 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_DPAD_UP) ? 1L << 12 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_DPAD_DOWN) ? 1L << 13 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_DPAD_LEFT)
                || joystick.isAxisBound(JoystickAxis.POV_X) ? 1L << 14 : 0L;
        nextVisibilityMask |= joystick.isButtonBound(JoystickButton.BUTTON_XBOX_DPAD_RIGHT)
                || joystick.isAxisBound(JoystickAxis.POV_X) ? 1L << 15 : 0L;
        nextVisibilityMask |= leftStick ? 1L << 16 : 0L;
        nextVisibilityMask |= rightStick ? 1L << 17 : 0L;

        if (visibilityMask == nextVisibilityMask) {
            return;
        }
        visibilityMask = nextVisibilityMask;

        setButtonVisible(JoystickButton.BUTTON_XBOX_A, (nextVisibilityMask & 1L) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_B, (nextVisibilityMask & (1L << 1)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_X, (nextVisibilityMask & (1L << 2)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_Y, (nextVisibilityMask & (1L << 3)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_LT, (nextVisibilityMask & (1L << 4)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_RT, (nextVisibilityMask & (1L << 5)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_LB, (nextVisibilityMask & (1L << 6)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_RB, (nextVisibilityMask & (1L << 7)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_BACK, (nextVisibilityMask & (1L << 8)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_START, (nextVisibilityMask & (1L << 9)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_L3, (nextVisibilityMask & (1L << 10)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_R3, (nextVisibilityMask & (1L << 11)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_DPAD_UP, (nextVisibilityMask & (1L << 12)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_DPAD_DOWN, (nextVisibilityMask & (1L << 13)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_DPAD_LEFT, (nextVisibilityMask & (1L << 14)) != 0L);
        setButtonVisible(JoystickButton.BUTTON_XBOX_DPAD_RIGHT, (nextVisibilityMask & (1L << 15)) != 0L);
        setAxisVisible(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, leftStick);
        setAxisVisible(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X, rightStick);
    }
}
