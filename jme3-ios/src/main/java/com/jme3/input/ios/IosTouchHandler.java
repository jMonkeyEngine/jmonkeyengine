/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

package com.jme3.input.ios;

import com.jme3.input.event.InputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import static com.jme3.input.event.TouchEvent.Type.DOWN;
import static com.jme3.input.event.TouchEvent.Type.MOVE;
import static com.jme3.input.event.TouchEvent.Type.UP;
import com.jme3.math.Vector2f;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IosTouchHandler is the base class that receives touch inputs from the 
 * iOS system and creates the TouchEvents for jME.
 * 
 * @author iwgeric
 */
public class IosTouchHandler {
    private static final Logger logger = Logger.getLogger(IosTouchHandler.class.getName());
    
    final private HashMap<Integer, Vector2f> lastPositions = new HashMap<>();

    protected int numPointers = 1;
    
    protected IosInputHandler iosInput;

    public IosTouchHandler(IosInputHandler iosInput) {
        this.iosInput = iosInput;
    }

    public void initialize() {
    }
    
    public void destroy() {
    }
    
    public void actionDown(int pointerId, long time, float x, float y) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Inject input pointer: {0}, time: {1}, x: {2}, y: {3}",
                    new Object[]{pointerId, time, x, y});
        }
        float jmeX = iosInput.getJmeX(x);
        float jmeY = iosInput.invertY(iosInput.getJmeY(y));
        TouchEvent touch = iosInput.getFreeTouchEvent();
        touch.set(TouchEvent.Type.DOWN, jmeX, jmeY, 0, 0);
        touch.setPointerId(pointerId);//TODO: pointer ID
        touch.setTime(time);
        touch.setPressure(1.0f);
        //touch.setPressure(event.getPressure(pointerIndex)); //TODO: pressure

        lastPositions.put(pointerId, new Vector2f(jmeX, jmeY));

        processEvent(touch);
    }
    
    public void actionUp(int pointerId, long time, float x, float y) {
        float jmeX = iosInput.getJmeX(x);
        float jmeY = iosInput.invertY(iosInput.getJmeY(y));
        TouchEvent touch = iosInput.getFreeTouchEvent();
        touch.set(TouchEvent.Type.UP, jmeX, jmeY, 0, 0);
        touch.setPointerId(pointerId);//TODO: pointer ID
        touch.setTime(time);
        touch.setPressure(1.0f);
        //touch.setPressure(event.getPressure(pointerIndex)); //TODO: pressure
        lastPositions.remove(pointerId);

        processEvent(touch);
    }
    
    public void actionMove(int pointerId, long time, float x, float y) {
        float jmeX = iosInput.getJmeX(x);
        float jmeY = iosInput.invertY(iosInput.getJmeY(y));
        Vector2f lastPos = lastPositions.get(pointerId);
        if (lastPos == null) {
            lastPos = new Vector2f(jmeX, jmeY);
            lastPositions.put(pointerId, lastPos);
        }

        float dX = jmeX - lastPos.x;
        float dY = jmeY - lastPos.y;
        if (dX != 0 || dY != 0) {
            TouchEvent touch = iosInput.getFreeTouchEvent();
            touch.set(TouchEvent.Type.MOVE, jmeX, jmeY, dX, dY);
            touch.setPointerId(pointerId);
            touch.setTime(time);
            touch.setPressure(1.0f);
            //touch.setPressure(event.getPressure(p));
            lastPos.set(jmeX, jmeY);

            processEvent(touch);
        }
    }

    protected void processEvent(TouchEvent event) {
        // Add the touch event
        iosInput.addEvent(event);
        // MouseEvents do not support multi-touch, so only evaluate 1 finger pointer events
        if (iosInput.isSimulateMouse() && numPointers == 1) {
            InputEvent mouseEvent = generateMouseEvent(event);
            if (mouseEvent != null) {
                // Add the mouse event
                iosInput.addEvent(mouseEvent);
            }
        }
        
    }

    // TODO: Ring Buffer for mouse events?
    protected InputEvent generateMouseEvent(TouchEvent event) {
        InputEvent inputEvent = null;
        int newX;
        int newY;
        int newDX;
        int newDY;

        if (iosInput.isMouseEventsInvertX()) {
            newX = (int) (iosInput.invertX(event.getX()));
            newDX = (int)event.getDeltaX() * -1;
        } else {
            newX = (int) event.getX();
            newDX = (int)event.getDeltaX();
        }

        if (iosInput.isMouseEventsInvertY()) {
            newY = (int) (iosInput.invertY(event.getY()));
            newDY = (int)event.getDeltaY() * -1;
        } else {
            newY = (int) event.getY();
            newDY = (int)event.getDeltaY();
        }

        switch (event.getType()) {
            case DOWN:
                // Handle mouse down event
                inputEvent = new MouseButtonEvent(0, true, newX, newY);
                inputEvent.setTime(event.getTime());
                break;

            case UP:
                // Handle mouse up event
                inputEvent = new MouseButtonEvent(0, false, newX, newY);
                inputEvent.setTime(event.getTime());
                break;

            case HOVER_MOVE:
            case MOVE:
                inputEvent = new MouseMotionEvent(newX, newY, newDX, newDY, (int)event.getScaleSpan(), (int)event.getDeltaScaleSpan());
                inputEvent.setTime(event.getTime());
                break;
        }

        return inputEvent;
    }
    
}
