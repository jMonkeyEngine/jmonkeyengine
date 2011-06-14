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

/**
 * Class to trigger TouchEvents, keycode can be TouchInput.ALL(=0) or TouchInput.KEYCODE_*
 * @author larynx
 *
 */
public class TouchTrigger implements Trigger {
    
    private final int keyCode;
    
    /**
     * Constructor
     * @param keyCode can be zero to get all events or TouchInput.KEYCODE_*
     */
    public TouchTrigger(int keyCode) {
        super();
        this.keyCode = keyCode;
    }
    
    @Override
    public String getName() {
        if (keyCode != 0)
            return "TouchInput";
        else
            return "TouchInput KeyCode " + keyCode;
    }
    
    public static int touchHash(int keyCode){
        assert keyCode >= 0 && keyCode <= 255;
        return 0xfedcba98 + keyCode;
    }

    public int triggerHashCode() {
        return touchHash(keyCode);
    }
    
    public int getKeyCode(){
        return keyCode;
    }
}
