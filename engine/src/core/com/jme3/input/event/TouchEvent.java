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
 * <code>TouchEvent</code> represents a single event from multi-touch input devices
 * @author larynx
 */
public class TouchEvent extends InputEvent {

    public enum Type {

        /**
         * Touch down event, fields: posX, posY, pressure
         */
        DOWN,
        /**
         * Move/Drag event, fields: posX, posY, deltaX, deltaY, pressure
         */
        MOVE,
        /**
         * Touch up event, fields: posX, posY, pressure
         */
        UP,
        /**
         * Virtual keyboard or hardware key event down, fields: keyCode, characters
         */
        KEY_DOWN,
        /**
         * Virtual keyboard or hardware key event up, fields: keyCode, characters
         */
        KEY_UP,
        // Single finger gestures
        FLING,
        TAP,
        DOUBLETAP,
        LONGPRESSED,
        // Two finger scale events
        /**
         * Two finger scale event start, fields: posX/posY = getFocusX/Y, scaleFactor, scaleSpan  
         */
        SCALE_START,
        /**
         * Two finger scale event, fields: posX/posY = getFocusX/Y, scaleFactor, scaleSpan
         */
        SCALE_MOVE,
        /**
         * Two finger scale event end, fields: posX/posY = getFocusX/Y, scaleFactor, scaleSpan
         */
        SCALE_END,
        /**
         *  Scroll event 
         */
        SCROLL,
        /**
         * The user has performed a down MotionEvent and not performed a move or up yet. This event is commonly used to provide visual feedback to the user to let them know that their action has been recognized i.e. highlight an element.
         */
        SHOWPRESS,
        // Others
        OUTSIDE,
        IDLE
    }
    private Type type = Type.IDLE;
    private int pointerId;
    private float posX;
    private float posY;
    private float deltaX;
    private float deltaY;
    private float pressure;
    
    // Used only with KEY* events
    private int keyCode;
    private String characters;
    // Used only with SCALE* events
    private float scaleFactor;
    private float scaleSpan;

    public TouchEvent() {
        set(Type.IDLE, 0f, 0f, 0f, 0f);
    }

    public TouchEvent(Type type, float x, float y, float deltax, float deltay) {
        set(type, x, y, deltax, deltay);
    }

    public void set(Type type) {
        set(type, 0f, 0f, 0f, 0f);
    }

    public void set(Type type, float x, float y, float deltax, float deltay) {
        this.type = type;
        this.posX = x;
        this.posY = y;
        this.deltaX = deltax;
        this.deltaY = deltay;
        pointerId = 0;
        pressure = 0;
        keyCode = 0;
        scaleFactor = 0;
        scaleSpan = 0;
        characters = "";
        consumed = false;
    }

    /**
     * Returns the type of touch event.
     * 
     * @return the type of touch event.
     */
    public Type getType() {
        return type;
    }

    public float getX() {
        return posX;
    }

    public float getY() {
        return posY;
    }

    public float getDeltaX() {
        return deltaX;
    }

    public float getDeltaY() {
        return deltaY;
    }
    
    public float getPressure() 
    {
        return pressure;
    }
    
    public void setPressure(float pressure) 
    {
        this.pressure = pressure;
    }
    
    public int getPointerId() 
    {
        return pointerId;
    }

    public void setPointerId(int pointerId) {
        this.pointerId = pointerId;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public String getCharacters() {
        return characters;
    }

    public void setCharacters(String characters) {
        this.characters = characters;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public float getScaleSpan() {
        return scaleSpan;
    }

    public void setScaleSpan(float scaleSpan) {
        this.scaleSpan = scaleSpan;
    }
}
