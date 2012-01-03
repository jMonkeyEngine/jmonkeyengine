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

package com.jme3.input.event;

import com.jme3.input.MouseInput;

/**
 * Mouse button press/release event.
 * 
 * @author Kirill Vainer
 */
public class MouseButtonEvent extends InputEvent {

    private int x;
    private int y;
    private int btnIndex;
    private boolean pressed;

    public MouseButtonEvent(int btnIndex, boolean pressed, int x, int y) {
        this.btnIndex = btnIndex;
        this.pressed = pressed;
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the mouse button index.
     * <p>
     * See constants in {@link MouseInput}.
     * 
     * @return the mouse button index.
     */
    public int getButtonIndex() {
        return btnIndex;
    }

    /**
     * Returns true if the mouse button was pressed, false if it was released.
     * 
     * @return true if the mouse button was pressed, false if it was released.
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Returns true if the mouse button was released, false if it was pressed.
     * 
     * @return true if the mouse button was released, false if it was pressed.
     */
    public boolean isReleased() {
        return !pressed;
    }
    
    /**
     * The X coordinate of the mouse when the event was generated.
     * @return X coordinate of the mouse when the event was generated.
     */
    public int getX() {
        return x;
    }
    
    /**
     * The Y coordinate of the mouse when the event was generated.
     * @return Y coordinate of the mouse when the event was generated.
     */
    public int getY() {
        return y;
    }
    
    @Override
    public String toString(){
        String str = "MouseButton(BTN="+btnIndex;
        if (pressed){
            return str + ", PRESSED)";
        }else{
            return str + ", RELEASED)";
        }
    }

}
