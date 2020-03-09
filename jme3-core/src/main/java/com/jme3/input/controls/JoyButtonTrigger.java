/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.input.controls;

import com.jme3.input.Joystick;

public class JoyButtonTrigger implements Trigger {

    private final int joyId, buttonId;

    /**
     * Use {@link Joystick#assignButton(java.lang.String, int) } instead.
     * 
     * @param joyId
     * @param axisId 
     */
    public JoyButtonTrigger(int joyId, int axisId) {
        this.joyId = joyId;
        this.buttonId = axisId;
    }

    public static int joyButtonHash(int joyId, int joyButton){
        assert joyButton >= 0 && joyButton <= 255;
        return (2048 * joyId) | 1536 | (joyButton & 0xff);
    }

    public int getAxisId() {
        return buttonId;
    }

    public int getJoyId() {
        return joyId;
    }

    @Override
    public String getName() {
        return "JoyButton[joyId="+joyId+", axisId="+buttonId+"]";
    }

    @Override
    public int triggerHashCode() {
        return joyButtonHash(joyId, buttonId);
    }

}
