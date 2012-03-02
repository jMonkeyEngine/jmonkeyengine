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

package com.jme3.input;

/**
 * A specific API for interfacing with smartphone touch devices
 */
public interface TouchInput extends Input {

    /**
     * No filter, get all events
     */
    public static final int ALL = 0x00;
    /**
     * Home key
     */
    public static final int KEYCODE_HOME = 0x03;
    /**
     * Escape key.
     */
    public static final int KEYCODE_BACK = 0x04;
    /**
     * Context Menu key.
     */
    public static final int KEYCODE_MENU = 0x52;
    /**
     * Search key.
     */
    public static final int KEYCODE_SEARCH = 0x54;
    /**
     * Volume up key.
     */
    public static final int KEYCODE_VOLUME_UP = 0x18;        
    /**
     * Volume down key.
     */
    public static final int KEYCODE_VOLUME_DOWN = 0x19;    

    
    /**
     * Set if mouse events should be generated
     * 
     * @param simulate if mouse events should be generated
     */
    public void setSimulateMouse(boolean simulate);
    
    /**
     * Get if mouse events are generated
     *
     */
    public boolean getSimulateMouse();

    /**
     * Set if keyboard events should be generated
     * 
     * @param simulate if keyboard events should be generated
     */
    public void setSimulateKeyboard(boolean simulate);
    
    /**
     * Set if historic android events should be transmitted, can be used to get better performance and less mem
     * @see <a href="http://developer.android.com/reference/android/view/MotionEvent.html#getHistoricalX%28int,%20int%29">
     * http://developer.android.com/reference/android/view/MotionEvent.html#getHistoricalX%28int,%20int%29</a>
     * @param dontSendHistory turn of historic events if true, false else and default
     */
    public void setOmitHistoricEvents(boolean dontSendHistory);
    
}