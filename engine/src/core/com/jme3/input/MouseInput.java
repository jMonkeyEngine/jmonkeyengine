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
 * A specific API for interfacing with the mouse.
 */
public interface MouseInput extends Input {

    /**
     * Mouse X axis.
     */
    public static final int AXIS_X = 0;
    
    /**
     * Mouse Y axis.
     */
    public static final int AXIS_Y = 1;
    
    /**
     * Mouse wheel axis.
     */
    public static final int AXIS_WHEEL = 2;

    /**
     * Left mouse button.
     */
    public static final int BUTTON_LEFT   = 0;
    
    /**
     * Right mouse button.
     */
    public static final int BUTTON_RIGHT  = 1;
    
    /**
     * Middle mouse button.
     */
    public static final int BUTTON_MIDDLE = 2;

    /**
     * Set whether the mouse cursor should be visible or not.
     * 
     * @param visible Whether the mouse cursor should be visible or not.
     */
    public void setCursorVisible(boolean visible);

    /**
     * Returns the number of buttons the mouse has. Typically 3 for most mice.
     * 
     * @return the number of buttons the mouse has.
     */
    public int getButtonCount();
}
