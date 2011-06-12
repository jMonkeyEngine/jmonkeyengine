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
 * A <code>MouseButtonTrigger</code> is used as a mapping to receive events
 * from mouse buttons. It is generally expected for a mouse to have at least
 * a left and right mouse button, but some mice may have a lot more buttons
 * than that.
 *
 * @author Kirill Vainer
 */
public class MouseButtonTrigger implements Trigger {

    private final int mouseButton;

    /**
     * Create a new <code>MouseButtonTrigger</code> to receive mouse button events.
     * 
     * @param mouseButton Mouse button index. See BUTTON_*** constants in
     * {@link MouseInput}.
     */
    public MouseButtonTrigger(int mouseButton) {
        if  (mouseButton < 0)
            throw new IllegalArgumentException("Mouse Button cannot be negative");

        this.mouseButton = mouseButton;
    }

    public int getMouseButton() {
        return mouseButton;
    }

    public String getName() {
        return "Mouse Button " + mouseButton;
    }

    public static int mouseButtonHash(int mouseButton){
        assert mouseButton >= 0 && mouseButton <= 255;
        return 256 | (mouseButton & 0xff);
    }

    public int triggerHashCode() {
        return mouseButtonHash(mouseButton);
    }

}
