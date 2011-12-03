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

import com.jme3.input.event.*;

/**
 * An interface used for receiving raw input from devices.
 */
public interface RawInputListener {

    /**
     * Called before a batch of input will be sent to this 
     * <code>RawInputListener</code>. 
     */
    public void beginInput();
    
    /**
     * Called after a batch of input was sent to this
     * <code>RawInputListener</code>. 
     * 
     * The listener should set the {@link InputEvent#setConsumed() consumed flag}
     * on any events that have been consumed either at this call or previous calls.
     */
    public void endInput();

    /**
     * Invoked on joystick axis events.
     * 
     * @param evt 
     */
    public void onJoyAxisEvent(JoyAxisEvent evt);
    
    /**
     * Invoked on joystick button presses.
     * 
     * @param evt 
     */
    public void onJoyButtonEvent(JoyButtonEvent evt);
    
    /**
     * Invoked on mouse movement/motion events.
     * 
     * @param evt 
     */
    public void onMouseMotionEvent(MouseMotionEvent evt);
    
    /**
     * Invoked on mouse button events.
     * 
     * @param evt 
     */
    public void onMouseButtonEvent(MouseButtonEvent evt);
    
    /**
     * Invoked on keyboard key press or release events.
     * 
     * @param evt 
     */
    public void onKeyEvent(KeyInputEvent evt);
    
    
    /**
     * Invoked on touchscreen touch events.
     * 
     * @param evt 
     */
    public void onTouchEvent(TouchEvent evt);
}
