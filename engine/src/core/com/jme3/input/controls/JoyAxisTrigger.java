/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

public class JoyAxisTrigger implements Trigger {

    private final int joyId, axisId;
    private final boolean negative;

    /**
     * Use {@link Joystick#assignAxis(java.lang.String, java.lang.String, int) }
     * instead.
     */
    public JoyAxisTrigger(int joyId, int axisId, boolean negative) {
        this.joyId = joyId;
        this.axisId = axisId;
        this.negative = negative;
    }

    public static int joyAxisHash(int joyId, int joyAxis, boolean negative){
        assert joyAxis >= 0 && joyAxis <= 255;
        return (2048 * joyId) | (negative ? 1280 : 1024) | (joyAxis & 0xff);
    }

    public int getAxisId() {
        return axisId;
    }

    public int getJoyId() {
        return joyId;
    }

    public boolean isNegative() {
        return negative;
    }

    public String getName() {
        return "JoyAxis[joyId="+joyId+", axisId="+axisId+", neg="+negative+"]";
    }

    public int triggerHashCode() {
        return joyAxisHash(joyId, axisId, negative);
    }
    
}
