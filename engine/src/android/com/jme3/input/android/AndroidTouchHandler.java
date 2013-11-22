/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

package com.jme3.input.android;

import android.view.MotionEvent;
import android.view.View;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import static com.jme3.input.event.TouchEvent.Type.DOWN;
import static com.jme3.input.event.TouchEvent.Type.MOVE;
import static com.jme3.input.event.TouchEvent.Type.UP;
import com.jme3.math.Vector2f;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * AndroidTouchHandler is the base class that receives touch inputs from the 
 * Android system and creates the TouchEvents for jME.  This class is designed
 * to handle the base touch events for Android rev 9 (Android 2.3).  This is
 * extended by other classes to add features that were introducted after
 * Android rev 9.
 * 
 * @author iwgeric
 */
public class AndroidTouchHandler implements View.OnTouchListener {
    private static final Logger logger = Logger.getLogger(AndroidTouchHandler.class.getName());
    
    final private HashMap<Integer, Vector2f> lastPositions = new HashMap<Integer, Vector2f>();

    protected int numPointers = 0;
    
    protected AndroidInputHandler androidInput;
    protected AndroidGestureHandler gestureHandler;

    public AndroidTouchHandler(AndroidInputHandler androidInput, AndroidGestureHandler gestureHandler) {
        this.androidInput = androidInput;
        this.gestureHandler = gestureHandler;
    }

    public void initialize() {
    }
    
    public void destroy() {
        setView(null);
    }
    
    public void setView(View view) {
        if (view != null) {
            view.setOnTouchListener(this);
        } else {
            androidInput.getView().setOnTouchListener(null);
        }
    }
    
    protected int getPointerIndex(MotionEvent event) {
        return (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    }
    
    protected int getPointerId(MotionEvent event) {
        return event.getPointerId(getPointerIndex(event));
    }
    
    protected int getAction(MotionEvent event) {
        return event.getAction() & MotionEvent.ACTION_MASK;
    }
    
    /**
     * onTouch gets called from android thread on touch events
     */
    public boolean onTouch(View view, MotionEvent event) {
        if (!androidInput.isInitialized() || view != androidInput.getView()) {
            return false;
        }
        
        boolean bWasHandled = false;
        TouchEvent touch = null;
        //    System.out.println("native : " + event.getAction());
        int action = getAction(event);
        int pointerIndex = getPointerIndex(event);
        int pointerId = getPointerId(event);
        Vector2f lastPos = lastPositions.get(pointerId);
        
        numPointers = event.getPointerCount();

        // final int historySize = event.getHistorySize();
        //final int pointerCount = event.getPointerCount();
        switch (getAction(event)) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                touch = androidInput.getFreeTouchEvent();
                touch.set(TouchEvent.Type.DOWN, event.getX(pointerIndex), androidInput.invertY(event.getY(pointerIndex)), 0, 0);
                touch.setPointerId(pointerId);
                touch.setTime(event.getEventTime());
                touch.setPressure(event.getPressure(pointerIndex));

                lastPos = new Vector2f(event.getX(pointerIndex), androidInput.invertY(event.getY(pointerIndex)));
                lastPositions.put(pointerId, lastPos);

                processEvent(touch);

                bWasHandled = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                touch = androidInput.getFreeTouchEvent();
                touch.set(TouchEvent.Type.UP, event.getX(pointerIndex), androidInput.invertY(event.getY(pointerIndex)), 0, 0);
                touch.setPointerId(pointerId);
                touch.setTime(event.getEventTime());
                touch.setPressure(event.getPressure(pointerIndex));
                lastPositions.remove(pointerId);

                processEvent(touch);

                bWasHandled = true;
                break;
            case MotionEvent.ACTION_MOVE:
                // Convert all pointers into events
                for (int p = 0; p < event.getPointerCount(); p++) {
                    lastPos = lastPositions.get(event.getPointerId(p));
                    if (lastPos == null) {
                        lastPos = new Vector2f(event.getX(p), androidInput.invertY(event.getY(p)));
                        lastPositions.put(event.getPointerId(p), lastPos);
                    }

                    float dX = event.getX(p) - lastPos.x;
                    float dY = androidInput.invertY(event.getY(p)) - lastPos.y;
                    if (dX != 0 || dY != 0) {
                        touch = androidInput.getFreeTouchEvent();
                        touch.set(TouchEvent.Type.MOVE, event.getX(p), androidInput.invertY(event.getY(p)), dX, dY);
                        touch.setPointerId(event.getPointerId(p));
                        touch.setTime(event.getEventTime());
                        touch.setPressure(event.getPressure(p));
                        lastPos.set(event.getX(p), androidInput.invertY(event.getY(p)));

                        processEvent(touch);

                        bWasHandled = true;
                    }
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
                break;

        }

        // Try to detect gestures
        if (gestureHandler != null) {
            gestureHandler.detectGesture(event);
        }

        return bWasHandled;
    }

    protected void processEvent(TouchEvent event) {
        // Add the touch event
        androidInput.addEvent(event);
        // MouseEvents do not support multi-touch, so only evaluate 1 finger pointer events
        if (androidInput.isSimulateMouse() && numPointers == 1) {
            InputEvent mouseEvent = generateMouseEvent(event);
            if (mouseEvent != null) {
                // Add the mouse event
                androidInput.addEvent(mouseEvent);
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

        if (androidInput.isMouseEventsInvertX()) {
            newX = (int) (androidInput.invertX(event.getX()));
            newDX = (int)event.getDeltaX() * -1;
        } else {
            newX = (int) event.getX();
            newDX = (int)event.getDeltaX();
        }

        if (androidInput.isMouseEventsInvertY()) {
            newY = (int) (androidInput.invertY(event.getY()));
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
