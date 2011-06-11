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

/**
 * Mouse movement event.
 * <p>
 * Movement events are only generated if the mouse is on-screen.
 * 
 * @author Kirill Vainer
 */
public class MouseMotionEvent extends InputEvent {

    private int x, y, dx, dy, wheel, deltaWheel;

    public MouseMotionEvent(int x, int y, int dx, int dy, int wheel, int deltaWheel) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.wheel = wheel;
        this.deltaWheel = deltaWheel;
    }

    /**
     * The change in wheel rotation.
     * 
     * @return change in wheel rotation.
     */
    public int getDeltaWheel() {
        return deltaWheel;
    }

    /**
     * The change in X coordinate
     * @return change in X coordinate
     */
    public int getDX() {
        return dx;
    }

    /**
     * The change in Y coordinate
     * 
     * @return change in Y coordinate
     */
    public int getDY() {
        return dy;
    }

    /**
     * Current mouse wheel value
     * @return Current mouse wheel value
     */
    public int getWheel() {
        return wheel;
    }

    /**
     * Current X coordinate
     * @return Current X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Current Y coordinate
     * @return Current Y coordinate
     */
    public int getY() {
        return y;
    }

    @Override
    public String toString(){
        return "MouseMotion(X="+x+", Y="+y+", DX="+dx+", DY="+dy+")";
    }

}
