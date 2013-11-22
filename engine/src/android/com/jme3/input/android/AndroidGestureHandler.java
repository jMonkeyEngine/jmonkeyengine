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

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AndroidGestureHandler uses Gesture type listeners to create jME TouchEvents
 * for gestures.  This class is designed to handle the gestures supported 
 * on Android rev 9 (Android 2.3).  Extend this class to add functionality
 * added by Android after rev 9.
 * 
 * @author iwgeric
 */
public class AndroidGestureHandler implements 
        GestureDetector.OnGestureListener, 
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener {
    private static final Logger logger = Logger.getLogger(AndroidGestureHandler.class.getName());
    private AndroidInputHandler androidInput;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleDetector;
    float gestureDownX = -1f;
    float gestureDownY = -1f;
    float scaleStartX = -1f;
    float scaleStartY = -1f;

    public AndroidGestureHandler(AndroidInputHandler androidInput) {
        this.androidInput = androidInput;
    }
    
    public void initialize() {
    }
    
    public void destroy() {
        setView(null);
    }
    
    public void setView(View view) {
        if (view != null) {
            gestureDetector = new GestureDetector(view.getContext(), this);
            scaleDetector = new ScaleGestureDetector(view.getContext(), this);
        } else {
            gestureDetector = null;
            scaleDetector = null;
        }
    }
    
    public void detectGesture(MotionEvent event) {
        if (gestureDetector != null && scaleDetector != null) {
            gestureDetector.onTouchEvent(event);
            scaleDetector.onTouchEvent(event);
        }
    }

    private int getPointerIndex(MotionEvent event) {
        return (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    }
    
    private int getPointerId(MotionEvent event) {
        return event.getPointerId(getPointerIndex(event));
    }
    
    private void processEvent(TouchEvent event) {
        // Add the touch event
        androidInput.addEvent(event);
        if (androidInput.isSimulateMouse()) {
            InputEvent mouseEvent = generateMouseEvent(event);
            if (mouseEvent != null) {
                // Add the mouse event
                androidInput.addEvent(mouseEvent);
            }
        }
    }

    // TODO: Ring Buffer for mouse events?
    private InputEvent generateMouseEvent(TouchEvent event) {
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
        int wheel = (int) (event.getScaleSpan()); // might need to scale to match mouse wheel
        int dWheel = (int) (event.getDeltaScaleSpan()); // might need to scale to match mouse wheel

        if (androidInput.isMouseEventsInvertY()) {
            newY = (int) (androidInput.invertY(event.getY()));
            newDY = (int)event.getDeltaY() * -1;
        } else {
            newY = (int) event.getY();
            newDY = (int)event.getDeltaY();
        }

        switch (event.getType()) {
            case SCALE_MOVE:
                inputEvent = new MouseMotionEvent(newX, newY, newDX, newDY, wheel, dWheel);
                inputEvent.setTime(event.getTime());
                break;
        }

        return inputEvent;
    }
    
    /* Events from onGestureListener */
    
    public boolean onDown(MotionEvent event) {
        // start of all GestureListeners.  Not really a gesture by itself
        // so we don't create an event.
        // However, reset the scaleInProgress here since this is the beginning
        // of a series of gesture events.
//        logger.log(Level.INFO, "onDown pointerId: {0}, action: {1}, x: {2}, y: {3}", 
//                new Object[]{getPointerId(event), getAction(event), event.getX(), event.getY()});
        gestureDownX = event.getX();
        gestureDownY = androidInput.invertY(event.getY());
        return true;
    }

    public boolean onSingleTapUp(MotionEvent event) {
        // Up of single tap.  May be followed by a double tap later.
        // use onSingleTapConfirmed instead.
//        logger.log(Level.INFO, "onSingleTapUp pointerId: {0}, action: {1}, x: {2}, y: {3}", 
//                new Object[]{getPointerId(event), getAction(event), event.getX(), event.getY()});
        return true;
    }

    public void onShowPress(MotionEvent event) {
//        logger.log(Level.INFO, "onShowPress pointerId: {0}, action: {1}, x: {2}, y: {3}", 
//                new Object[]{getPointerId(event), getAction(event), event.getX(), event.getY()});
        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.SHOWPRESS, event.getX(), androidInput.invertY(event.getY()), 0, 0);
        touchEvent.setPointerId(getPointerId(event));
        touchEvent.setTime(event.getEventTime());
        touchEvent.setPressure(event.getPressure());
        processEvent(touchEvent);
    }

    public void onLongPress(MotionEvent event) {
//        logger.log(Level.INFO, "onLongPress pointerId: {0}, action: {1}, x: {2}, y: {3}", 
//                new Object[]{getPointerId(event), getAction(event), event.getX(), event.getY()});
        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.LONGPRESSED, event.getX(), androidInput.invertY(event.getY()), 0, 0);
        touchEvent.setPointerId(getPointerId(event));
        touchEvent.setTime(event.getEventTime());
        touchEvent.setPressure(event.getPressure());
        processEvent(touchEvent);
    }

    public boolean onScroll(MotionEvent startEvent, MotionEvent endEvent, float distX, float distY) {
        // if not scaleInProgess, send scroll events.  This is to avoid sending
        // scroll events when one of the fingers is lifted just before the other one.
        // Avoids sending the scroll for that brief period of time.
        // Return true so that the next event doesn't accumulate the distX and distY values.
        // Apparantly, both distX and distY are negative.  
        // Negate distX to get the real value, but leave distY negative to compensate
        // for the fact that jME has y=0 at bottom where Android has y=0 at top.
//        if (!scaleInProgress) {
        if (!scaleDetector.isInProgress()) {
//            logger.log(Level.INFO, "onScroll pointerId: {0}, startAction: {1}, startX: {2}, startY: {3}, endAction: {4}, endX: {5}, endY: {6}, dx: {7}, dy: {8}", 
//                    new Object[]{getPointerId(startEvent), getAction(startEvent), startEvent.getX(), startEvent.getY(), getAction(endEvent), endEvent.getX(), endEvent.getY(), distX, distY});

            TouchEvent touchEvent = androidInput.getFreeTouchEvent();
            touchEvent.set(TouchEvent.Type.SCROLL, endEvent.getX(), androidInput.invertY(endEvent.getY()), -distX, distY);
            touchEvent.setPointerId(getPointerId(endEvent));
            touchEvent.setTime(endEvent.getEventTime());
            touchEvent.setPressure(endEvent.getPressure());
            processEvent(touchEvent);
        }
        return true;
    }

    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float velocityX, float velocityY) {
        // Fling happens only once at the end of the gesture (all fingers up).
        // Fling returns the velocity of the finger movement in pixels/sec.
        // Therefore, the dX and dY values are actually velocity instead of distance values
        // Since this does not track the movement, use the start position and velocity values.
        
//        logger.log(Level.INFO, "onFling pointerId: {0}, startAction: {1}, startX: {2}, startY: {3}, endAction: {4}, endX: {5}, endY: {6}, velocityX: {7}, velocityY: {8}", 
//                new Object[]{getPointerId(startEvent), getAction(startEvent), startEvent.getX(), startEvent.getY(), getAction(endEvent), endEvent.getX(), endEvent.getY(), velocityX, velocityY});

        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.FLING, startEvent.getX(), androidInput.invertY(startEvent.getY()), velocityX, velocityY);
        touchEvent.setPointerId(getPointerId(endEvent));
        touchEvent.setTime(endEvent.getEventTime());
        touchEvent.setPressure(endEvent.getPressure());
        processEvent(touchEvent);
        return true;
    }

    /* Events from onDoubleTapListener */
    
    public boolean onSingleTapConfirmed(MotionEvent event) {
        // Up of single tap when no double tap followed.
//        logger.log(Level.INFO, "onSingleTapConfirmed pointerId: {0}, action: {1}, x: {2}, y: {3}", 
//                new Object[]{getPointerId(event), getAction(event), event.getX(), event.getY()});
        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.TAP, event.getX(), androidInput.invertY(event.getY()), 0, 0);
        touchEvent.setPointerId(getPointerId(event));
        touchEvent.setTime(event.getEventTime());
        touchEvent.setPressure(event.getPressure());
        processEvent(touchEvent);
        return true;
    }

    public boolean onDoubleTap(MotionEvent event) {
        //The down motion event of the first tap of the double-tap
        // We could use this event to fire off a double tap event, or use 
        // DoubleTapEvent with a check for the UP action
//        logger.log(Level.INFO, "onDoubleTap pointerId: {0}, action: {1}, x: {2}, y: {3}", 
//                new Object[]{getPointerId(event), getAction(event), event.getX(), event.getY()});
        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.DOUBLETAP, event.getX(), androidInput.invertY(event.getY()), 0, 0);
        touchEvent.setPointerId(getPointerId(event));
        touchEvent.setTime(event.getEventTime());
        touchEvent.setPressure(event.getPressure());
        processEvent(touchEvent);
        return true;
    }

    public boolean onDoubleTapEvent(MotionEvent event) {
        //Notified when an event within a double-tap gesture occurs, including the down, move(s), and up events.
        // this means it will get called multiple times for a single double tap
//        logger.log(Level.INFO, "onDoubleTapEvent pointerId: {0}, action: {1}, x: {2}, y: {3}", 
//                new Object[]{getPointerId(event), getAction(event), event.getX(), event.getY()});
//        if (getAction(event) == MotionEvent.ACTION_UP) {
//            TouchEvent touchEvent = touchEventPool.getNextFreeEvent();
//            touchEvent.set(TouchEvent.Type.DOUBLETAP, event.getX(), androidInput.invertY(event.getY()), 0, 0);
//            touchEvent.setPointerId(getPointerId(event));
//            touchEvent.setTime(event.getEventTime());
//            touchEvent.setPressure(event.getPressure());
//            processEvent(touchEvent);
//        }
        return true;
    }

    /* Events from ScaleGestureDetector */
    
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        // Scale uses a focusX and focusY instead of x and y.  Focus is the middle
        // of the fingers.  Therefore, use the x and y values from the Down event
        // so that the x and y values don't jump to the middle position.
        // return true or all gestures for this beginning event will be discarded
        logger.log(Level.INFO, "onScaleBegin");
        scaleStartX = gestureDownX;
        scaleStartY = gestureDownY;
        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.SCALE_START, scaleStartX, scaleStartY, 0f, 0f);
        touchEvent.setPointerId(0);
        touchEvent.setTime(scaleGestureDetector.getEventTime());
        touchEvent.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touchEvent.setDeltaScaleSpan(0f);
        touchEvent.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touchEvent.setScaleSpanInProgress(scaleDetector.isInProgress());
        processEvent(touchEvent);
        
        return true;
    }

    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        // return true or all gestures for this event will be accumulated
        logger.log(Level.INFO, "onScale");
        scaleStartX = gestureDownX;
        scaleStartY = gestureDownY;
        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.SCALE_MOVE, scaleStartX, scaleStartY, 0f, 0f);
        touchEvent.setPointerId(0);
        touchEvent.setTime(scaleGestureDetector.getEventTime());
        touchEvent.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touchEvent.setDeltaScaleSpan(scaleGestureDetector.getCurrentSpan() - scaleGestureDetector.getPreviousSpan());
        touchEvent.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touchEvent.setScaleSpanInProgress(scaleDetector.isInProgress());
        processEvent(touchEvent);
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        logger.log(Level.INFO, "onScaleEnd");
        scaleStartX = gestureDownX;
        scaleStartY = gestureDownY;
        TouchEvent touchEvent = androidInput.getFreeTouchEvent();
        touchEvent.set(TouchEvent.Type.SCALE_END, scaleStartX, scaleStartY, 0f, 0f);
        touchEvent.setPointerId(0);
        touchEvent.setTime(scaleGestureDetector.getEventTime());
        touchEvent.setScaleSpan(scaleGestureDetector.getCurrentSpan());
        touchEvent.setDeltaScaleSpan(scaleGestureDetector.getCurrentSpan() - scaleGestureDetector.getPreviousSpan());
        touchEvent.setScaleFactor(scaleGestureDetector.getScaleFactor());
        touchEvent.setScaleSpanInProgress(scaleDetector.isInProgress());
        processEvent(touchEvent);
    }
}
