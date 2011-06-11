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

import com.jme3.input.MouseInput;

/**
 * A <code>MouseAxisTrigger</code> is used as a mapping to mouse axis,
 * a mouse axis is movement along the X axis (left/right), Y axis (up/down)
 * and the mouse wheel (scroll up/down).
 *
 * @author Kirill Vainer
 */
public class MouseAxisTrigger implements Trigger {

    private int mouseAxis;
    private boolean negative;

    /**
     * Create a new <code>MouseAxisTrigger</code>.
     * <p>
     * @param mouseAxis Mouse axis. See AXIS_*** constants in {@link MouseInput}
     * @param negative True if listen to negative axis events, false if
     * listen to positive axis events.
     */
    public MouseAxisTrigger(int mouseAxis, boolean negative){
        if (mouseAxis < 0 || mouseAxis > 2)
            throw new IllegalArgumentException("Mouse Axis must be between 0 and 2");

        this.mouseAxis = mouseAxis;
        this.negative = negative;
    }

    public int getMouseAxis(){
        return mouseAxis;
    }

    public boolean isNegative() {
        return negative;
    }

    public String getName() {
        String sign = negative ? "Negative" : "Positive";
        switch (mouseAxis){
            case MouseInput.AXIS_X: return "Mouse X Axis " + sign;
            case MouseInput.AXIS_Y: return "Mouse Y Axis " + sign;
            case MouseInput.AXIS_WHEEL: return "Mouse Wheel " + sign;
            default: throw new AssertionError();
        }
    }

    public static int mouseAxisHash(int mouseAxis, boolean negative){
        assert mouseAxis >= 0 && mouseAxis <= 255;
        return (negative ? 768 : 512) | (mouseAxis & 0xff);
    }

    public int triggerHashCode() {
        return mouseAxisHash(mouseAxis, negative);
    }
}
