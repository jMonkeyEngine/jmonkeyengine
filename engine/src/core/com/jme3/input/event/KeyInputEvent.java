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

import com.jme3.input.KeyInput;

/**
 * Keyboard key event.
 * 
 * @author Kirill Vainer
 */
public class KeyInputEvent extends InputEvent {

    private int keyCode;
    private char keyChar;
    private boolean pressed;
    private boolean repeating;

    public KeyInputEvent(int keyCode, char keyChar, boolean pressed, boolean repeating) {
        this.keyCode = keyCode;
        this.keyChar = keyChar;
        this.pressed = pressed;
        this.repeating = repeating;
    }

    /**
     * Returns the key character. Returns 0 if the key has no character.
     * 
     * @return the key character. 0 if the key has no character.
     */
    public char getKeyChar() {
        return keyChar;
    }

    /**
     * The key code.
     * <p>
     * See KEY_*** constants in {@link KeyInput}.
     * 
     * @return key code.
     */
    public int getKeyCode() {
        return keyCode;
    }

    /**
     * Returns true if this event is key press, false is it was a key release.
     * 
     * @return true if this event is key press, false is it was a key release.
     */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Returns true if this event is a repeat event. Not used anymore.
     * 
     * @return true if this event is a repeat event
     */
    public boolean isRepeating() {
        return repeating;
    }

    /**
     * Returns true if this event is a key release, false if it was a key press.
     * 
     * @return true if this event is a key release, false if it was a key press.
     */
    public boolean isReleased() {
        return !pressed;
    }

    @Override
    public String toString(){
        String str = "Key(CODE="+keyCode;
        if (keyChar != '\0')
            str = str + ", CHAR=" + keyChar;
            
        if (repeating){
            return str + ", REPEATING)";
        }else if (pressed){
            return str + ", PRESSED)";
        }else{
            return str + ", RELEASED)";
        }
    }
}
