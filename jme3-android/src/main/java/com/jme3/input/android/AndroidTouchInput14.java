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

package com.jme3.input.android;

import android.view.MotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector2f;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * AndroidTouchHandler14 extends AndroidTouchHandler to process the onHover
 * events added in Android rev 14 (Android 4.0).
 *
 * @author iwgeric
 */
public class AndroidTouchInput14 extends AndroidTouchInput {
    private static final Logger logger = Logger.getLogger(AndroidTouchInput14.class.getName());
    final private HashMap<Integer, Vector2f> lastHoverPositions = new HashMap<>();

    public AndroidTouchInput14(AndroidInputHandler androidInput) {
        super(androidInput);
    }

    public boolean onHover(MotionEvent event) {
        boolean consumed = false;
        int action = getAction(event);
        int pointerId = getPointerId(event);
        int pointerIndex = getPointerIndex(event);
        Vector2f lastPos = lastHoverPositions.get(pointerId);
        float jmeX;
        float jmeY;

        numPointers = event.getPointerCount();

//        logger.log(Level.INFO, "onHover pointerId: {0}, action: {1}, x: {2}, y: {3}, numPointers: {4}",
//                new Object[]{pointerId, action, event.getX(), event.getY(), event.getPointerCount()});

        TouchEvent touchEvent;
        switch (action) {
            case MotionEvent.ACTION_HOVER_ENTER:
                jmeX = getJmeX(event.getX(pointerIndex));
                jmeY = invertY(getJmeY(event.getY(pointerIndex)));
                touchEvent = getFreeTouchEvent();
                touchEvent.set(TouchEvent.Type.HOVER_START, jmeX, jmeY, 0, 0);
                touchEvent.setPointerId(pointerId);
                touchEvent.setTime(event.getEventTime());
                touchEvent.setPressure(event.getPressure(pointerIndex));

                lastPos = new Vector2f(jmeX, jmeY);
                lastHoverPositions.put(pointerId, lastPos);

                addEvent(touchEvent);
                consumed = true;
                break;
            case MotionEvent.ACTION_HOVER_MOVE:
                // Convert all pointers into events
                for (int p = 0; p < event.getPointerCount(); p++) {
                    jmeX = getJmeX(event.getX(p));
                    jmeY = invertY(getJmeY(event.getY(p)));
                    lastPos = lastHoverPositions.get(event.getPointerId(p));
                    if (lastPos == null) {
                        lastPos = new Vector2f(jmeX, jmeY);
                        lastHoverPositions.put(event.getPointerId(p), lastPos);
                    }

                    float dX = jmeX - lastPos.x;
                    float dY = jmeY - lastPos.y;
                    if (dX != 0 || dY != 0) {
                        touchEvent = getFreeTouchEvent();
                        touchEvent.set(TouchEvent.Type.HOVER_MOVE, jmeX, jmeY, dX, dY);
                        touchEvent.setPointerId(event.getPointerId(p));
                        touchEvent.setTime(event.getEventTime());
                        touchEvent.setPressure(event.getPressure(p));
                        lastPos.set(jmeX, jmeY);

                        addEvent(touchEvent);

                    }
                }
                consumed = true;
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                jmeX = getJmeX(event.getX(pointerIndex));
                jmeY = invertY(getJmeY(event.getY(pointerIndex)));
                touchEvent = getFreeTouchEvent();
                touchEvent.set(TouchEvent.Type.HOVER_END, jmeX, jmeY, 0, 0);
                touchEvent.setPointerId(pointerId);
                touchEvent.setTime(event.getEventTime());
                touchEvent.setPressure(event.getPressure(pointerIndex));
                lastHoverPositions.remove(pointerId);

                addEvent(touchEvent);
                consumed = true;
                break;
            default:
                consumed = false;
                break;
        }

        return consumed;

    }

}
