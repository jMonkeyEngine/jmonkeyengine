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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.input.virtual;

import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.virtual.VirtualJoystickTheme.TextureKey;

/**
 * Default Xbox-like virtual joystick layout.
 */
public class VirtualJoystickXboxLayout extends VirtualJoystickLayout {

    public VirtualJoystickXboxLayout() {
        float leftStickColumn = 0.145f;
        float dpadColumn = 0.255f;
        float rightStickColumn = 0.745f;
        float faceColumn = 0.855f;
        float leftStickRow = 0.565f;
        float faceRow = 0.535f;
        float lowerRow = 0.285f;
        float faceButtonSize = 0.086f;
        float faceButtonOffset = 0.09f;
        float shoulderSize = 0.09f;

        addButtonElement(JoystickButton.BUTTON_XBOX_A, "", faceColumn, faceRow, 0f, -faceButtonOffset,
                faceButtonSize, TextureKey.BUTTON, TextureKey.BUTTON_A_ICON);
        addButtonElement(JoystickButton.BUTTON_XBOX_B, "", faceColumn, faceRow, faceButtonOffset, 0f,
                faceButtonSize, TextureKey.BUTTON, TextureKey.BUTTON_B_ICON);
        addButtonElement(JoystickButton.BUTTON_XBOX_X, "", faceColumn, faceRow, -faceButtonOffset, 0f,
                faceButtonSize, TextureKey.BUTTON, TextureKey.BUTTON_X_ICON);
        addButtonElement(JoystickButton.BUTTON_XBOX_Y, "", faceColumn, faceRow, 0f, faceButtonOffset,
                faceButtonSize, TextureKey.BUTTON, TextureKey.BUTTON_Y_ICON);

        addButtonElement(JoystickButton.BUTTON_XBOX_LT, "LT", leftStickColumn, 0.94f, shoulderSize, 2f,
                TextureKey.BUTTON_WIDE);
        addButtonElement(JoystickButton.BUTTON_XBOX_RT, "RT", faceColumn, 0.94f, shoulderSize, 2f,
                TextureKey.BUTTON_WIDE);
        addButtonElement(JoystickButton.BUTTON_XBOX_LB, "LB", leftStickColumn, 0.79f, shoulderSize, 2f,
                TextureKey.BUTTON_WIDE);
        addButtonElement(JoystickButton.BUTTON_XBOX_RB, "RB", faceColumn, 0.79f, shoulderSize, 2f,
                TextureKey.BUTTON_WIDE);

        addButtonElement(JoystickButton.BUTTON_XBOX_BACK, "", 0.44f, 0.06f, 0.078f, 2f,
                TextureKey.BUTTON_WIDE, TextureKey.BUTTON_BACK_ICON);
        addButtonElement(JoystickButton.BUTTON_XBOX_START, "", 0.56f, 0.06f, 0.078f, 2f,
                TextureKey.BUTTON_WIDE, TextureKey.BUTTON_START_ICON);
        addButtonElement(JoystickButton.BUTTON_XBOX_L3, "L3", leftStickColumn, leftStickRow, -0.145f, -0.07f, 0.065f,
                TextureKey.BUTTON);
        addButtonElement(JoystickButton.BUTTON_XBOX_R3, "R3", rightStickColumn, lowerRow, 0.145f, -0.07f, 0.065f,
                TextureKey.BUTTON);

        addButtonElement(JoystickButton.BUTTON_XBOX_DPAD_UP, "", dpadColumn, lowerRow, 0f, 0.064f, 0.085f,
                TextureKey.DPAD_UP);
        addButtonElement(JoystickButton.BUTTON_XBOX_DPAD_DOWN, "", dpadColumn, lowerRow, 0f, -0.064f, 0.085f,
                TextureKey.DPAD_DOWN);
        addButtonElement(JoystickButton.BUTTON_XBOX_DPAD_LEFT, "", dpadColumn, lowerRow, -0.064f, 0f, 0.085f,
                TextureKey.DPAD_LEFT);
        addButtonElement(JoystickButton.BUTTON_XBOX_DPAD_RIGHT, "", dpadColumn, lowerRow, 0.064f, 0f, 0.085f,
                TextureKey.DPAD_RIGHT);

        addAxisElement(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X, JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y,
                "", leftStickColumn, leftStickRow, 0.235f, TextureKey.STICK_PAD, TextureKey.STICK_NUB);
        addAxisElement(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X, JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y,
                "", rightStickColumn, lowerRow, 0.235f, TextureKey.STICK_PAD, TextureKey.STICK_NUB);
    }
}
