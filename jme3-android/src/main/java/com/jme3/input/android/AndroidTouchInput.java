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

import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import static com.jme3.input.event.TouchEvent.Type.DOWN;
import static com.jme3.input.event.TouchEvent.Type.MOVE;
import static com.jme3.input.event.TouchEvent.Type.UP;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AndroidTouchInput is the base class that receives touch inputs from the
 * Android system and creates the TouchEvents for jME.  This class is designed
 * to handle the base touch events for Android rev 9 (Android 2.3).  This is
 * extended by other classes to add features that were introduced after
 * Android rev 9.
 *
 * @author iwgeric
 */
public class AndroidTouchInput implements TouchInput {
    private static final Logger logger = Logger.getLogger(AndroidTouchInput.class.getName());

    private boolean mouseEventsEnabled = true;
    private boolean mouseEventsInvertX = false;
    private boolean mouseEventsInvertY = false;
    private boolean keyboardEventsEnabled = false;

    protected int numPointers = 0;
    final private HashMap<Integer, Vector2f> lastPositions = new HashMap<>();
    final private ConcurrentLinkedQueue<InputEvent> inputEventQueue = new ConcurrentLinkedQueue<>();
    private final static int MAX_TOUCH_EVENTS = 1024;
    private final TouchEventPool touchEventPool = new TouchEventPool(MAX_TOUCH_EVENTS);
    private float scaleX = 1f;
    private float scaleY = 1f;

    private boolean initialized = false;
    private RawInputListener listener = null;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleDetector;

    protected AndroidInputHandler androidInput;

    public AndroidTouchInput(AndroidInputHandler androidInput) {
        this.androidInput = androidInput;
    }

    public GestureDetector getGestureDetector() {
        return gestureDetector;
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    public ScaleGestureDetector getScaleDetector() {
        return scaleDetector;
    }

    public void setScaleDetector(ScaleGestureDetector scaleDetector) {
        this.scaleDetector = scaleDetector;
    }

    public float invertX(float origX) {
        return getJmeX(androidInput.getView().getWidth()) - origX;
    }

    public float invertY(float origY) {
        return getJmeY(androidInput.getView().getHeight()) - origY;
    }

    public float getJmeX(float origX) {
        return origX * scaleX;
    }

    public float getJmeY(float origY) {
        return origY * scaleY;
    }

    public void loadSettings(AppSettings settings) {
        keyboardEventsEnabled = settings.isEmulateKeyboard();
        mouseEventsEnabled = settings.isEmulateMouse();
        mouseEventsInvertX = settings.isEmulateMouseFlipX();
        mouseEventsInvertY = settings.isEmulateMouseFlipY();

        // view width and height are 0 until the view is displayed on the screen
        if (androidInput.getView().getWidth() != 0 && androidInput.getView().getHeight() != 0) {
            scaleX = settings.getWidth() / (float)androidInput.getView().getWidth();
            scaleY = settings.getHeight() / (float)androidInput.getView().getHeight();
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Setting input scaling, scaleX: {0}, scaleY: {1}",
                    new Object[]{scaleX, scaleY});
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

    public boolean onTouch(MotionEvent event) {
        if (!isInitialized()) {
            return false;
        }

        boolean bWasHandled = false;
        TouchEvent touch = null;
        //    System.out.println("native : " + event.getAction());
        getAction(event);
        int pointerIndex = getPointerIndex(event);
        int pointerId = getPointerId(event);
        Vector2f lastPos = lastPositions.get(pointerId);
        float jmeX;
        float jmeY;

        numPointers = event.getPointerCount();

        // final int historySize = event.getHistorySize();
        //final int pointerCount = event.getPointerCount();
        switch (getAction(event)) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                jmeX = getJmeX(event.getX(pointerIndex));
                jmeY = invertY(getJmeY(event.getY(pointerIndex)));
                touch = getFreeTouchEvent();
                touch.set(TouchEvent.Type.DOWN, jmeX, jmeY, 0, 0);
                touch.setPointerId(pointerId);
                touch.setTime(event.getEventTime());
                touch.setPressure(event.getPressure(pointerIndex));

                lastPos = new Vector2f(jmeX, jmeY);
                lastPositions.put(pointerId, lastPos);

                addEvent(touch);
                addEvent(generateMouseEvent(touch));

                bWasHandled = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                jmeX = getJmeX(event.getX(pointerIndex));
                jmeY = invertY(getJmeY(event.getY(pointerIndex)));
                touch = getFreeTouchEvent();
                touch.set(TouchEvent.Type.UP, jmeX, jmeY, 0, 0);
                touch.setPointerId(pointerId);
                touch.setTime(event.getEventTime());
                touch.setPressure(event.getPressure(pointerIndex));
                lastPositions.remove(pointerId);

                addEvent(touch);
                addEvent(generateMouseEvent(touch));

                bWasHandled = true;
                break;
            case MotionEvent.ACTION_MOVE:
                // Convert all pointers into events
                for (int p = 0; p < event.getPointerCount(); p++) {
                    jmeX = getJmeX(event.getX(p));
                    jmeY = invertY(getJmeY(event.getY(p)));
                    lastPos = lastPositions.get(event.getPointerId(p));
                    if (lastPos == null) {
                        lastPos = new Vector2f(jmeX, jmeY);
                        lastPositions.put(event.getPointerId(p), lastPos);
                    }

                    float dX = jmeX - lastPos.x;
                    float dY = jmeY - lastPos.y;
                    if (dX != 0 || dY != 0) {
                        touch = getFreeTouchEvent();
                        touch.set(TouchEvent.Type.MOVE, jmeX, jmeY, dX, dY);
                        touch.setPointerId(event.getPointerId(p));
                        touch.setTime(event.getEventTime());
                        touch.setPressure(event.getPressure(p));
                        lastPos.set(jmeX, jmeY);

                        addEvent(touch);
                        addEvent(generateMouseEvent(touch));

                        bWasHandled = true;
                    }
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
                break;

        }

        // Try to detect gestures
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }
        if (scaleDetector != null) {
            scaleDetector.onTouchEvent(event);
        }

        return bWasHandled;
    }

    // TODO: Ring Buffer for mouse events?
    public InputEvent generateMouseEvent(TouchEvent event) {
        InputEvent inputEvent = null;
        int newX;
        int newY;
        int newDX;
        int newDY;

        // MouseEvents do not support multi-touch, so only evaluate 1 finger pointer events
        if (!isSimulateMouse() || numPointers > 1) {
            return null;
        }


        if (isMouseEventsInvertX()) {
            newX = (int) (invertX(event.getX()));
            newDX = (int)event.getDeltaX() * -1;
        } else {
            newX = (int) event.getX();
            newDX = (int)event.getDeltaX();
        }

        if (isMouseEventsInvertY()) {
            newY = (int) (invertY(event.getY()));
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


    public boolean onKey(KeyEvent event) {
        if (!isInitialized()) {
            return false;
        }

        TouchEvent evt;
        // TODO: get touch event from pool
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            evt = new TouchEvent();
            evt.set(TouchEvent.Type.KEY_DOWN);
            evt.setKeyCode(event.getKeyCode());
            evt.setCharacters(event.getCharacters());
            evt.setTime(event.getEventTime());

            // Send the event
            addEvent(evt);

        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            evt = new TouchEvent();
            evt.set(TouchEvent.Type.KEY_UP);
            evt.setKeyCode(event.getKeyCode());
            evt.setCharacters(event.getCharacters());
            evt.setTime(event.getEventTime());

            // Send the event
            addEvent(evt);

        }

        if (isSimulateKeyboard()) {
            KeyInputEvent kie;
            char unicodeChar = (char)event.getUnicodeChar();
            int jmeKeyCode = AndroidKeyMapping.getJmeKey(event.getKeyCode());

            boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
            boolean repeating = pressed && event.getRepeatCount() > 0;

            kie = new KeyInputEvent(jmeKeyCode, unicodeChar, pressed, repeating);
            kie.setTime(event.getEventTime());
            addEvent(kie);
//            logger.log(Level.FINE, "onKey keyCode: {0}, jmeKeyCode: {1}, pressed: {2}, repeating: {3}",
//                    new Object[]{event.getKeyCode(), jmeKeyCode, pressed, repeating});
//            logger.log(Level.FINE, "creating KeyInputEvent: {0}", kie);
        }

        // Consume all keys ourselves except Volume Up/Down and Menu.
        //   Don't do Menu so that typical Android Menus can be created and used
        //   by the user in MainActivity.
        if ((event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) ||
                (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) ||
                (event.getKeyCode() == KeyEvent.KEYCODE_MENU)) {
            return false;
        } else {
            return true;
        }

   }




    // -----------------------------------------
    // JME3 Input interface
    @Override
    public void initialize() {
        touchEventPool.initialize();

        initialized = true;
    }

    @Override
    public void destroy() {
        initialized = false;

        touchEventPool.destroy();

    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }

    @Override
    public void update() {
        if (listener != null) {
            InputEvent inputEvent;

            while ((inputEvent = inputEventQueue.poll()) != null) {
                if (inputEvent instanceof TouchEvent) {
                    listener.onTouchEvent((TouchEvent)inputEvent);
                } else if (inputEvent instanceof MouseButtonEvent) {
                    listener.onMouseButtonEvent((MouseButtonEvent)inputEvent);
                } else if (inputEvent instanceof MouseMotionEvent) {
                    listener.onMouseMotionEvent((MouseMotionEvent)inputEvent);
                } else if (inputEvent instanceof KeyInputEvent) {
                    listener.onKeyEvent((KeyInputEvent)inputEvent);
                }
            }
        }
    }

    // -----------------------------------------

    public TouchEvent getFreeTouchEvent() {
            return touchEventPool.getNextFreeEvent();
    }

    public void addEvent(InputEvent event) {
        if (event == null) {
            return;
        }

        //logger.log(Level.INFO, "event: {0}", event);

        inputEventQueue.add(event);
        if (event instanceof TouchEvent) {
            touchEventPool.storeEvent((TouchEvent)event);
        }

    }

    @Override
    public void setSimulateMouse(boolean simulate) {
        this.mouseEventsEnabled = simulate;
    }

    @Override
    public boolean isSimulateMouse() {
        return mouseEventsEnabled;
    }

    public boolean isMouseEventsInvertX() {
        return mouseEventsInvertX;
    }

    public boolean isMouseEventsInvertY() {
        return mouseEventsInvertY;
    }

    @Override
    public void setSimulateKeyboard(boolean simulate) {
        this.keyboardEventsEnabled = simulate;
    }

    @Override
    public boolean isSimulateKeyboard() {
        return keyboardEventsEnabled;
    }

    @Override
    public void setOmitHistoricEvents(boolean dontSendHistory) {
        // not implemented
    }

}
