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

import android.opengl.GLSurfaceView;
import android.os.Build;
import android.view.View;
import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.system.AppSettings;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AndroidInput</code> is the main class that connects the Android system
 * inputs to jME. It serves as the manager that gathers inputs from the various
 * Android input methods and provides them to jME's <code>InputManager</code>.
 *
 * @author iwgeric
 */
public class AndroidInputHandler implements TouchInput {
    private static final Logger logger = Logger.getLogger(AndroidInputHandler.class.getName());

    // Custom settings
    private boolean mouseEventsEnabled = true;
    private boolean mouseEventsInvertX = false;
    private boolean mouseEventsInvertY = false;
    private boolean keyboardEventsEnabled = false;
    private boolean joystickEventsEnabled = false;
    private boolean dontSendHistory = false;


    // Internal
    private GLSurfaceView view;
    private AndroidTouchHandler touchHandler;
    private AndroidKeyHandler keyHandler;
    private AndroidGestureHandler gestureHandler;
    private boolean initialized = false;
    private RawInputListener listener = null;
    private ConcurrentLinkedQueue<InputEvent> inputEventQueue = new ConcurrentLinkedQueue<InputEvent>();
    private final static int MAX_TOUCH_EVENTS = 1024;
    private final TouchEventPool touchEventPool = new TouchEventPool(MAX_TOUCH_EVENTS);
    private float scaleX = 1f;
    private float scaleY = 1f;


    public AndroidInputHandler() {
        int buildVersion = Build.VERSION.SDK_INT;
        logger.log(Level.INFO, "Android Build Version: {0}", buildVersion);
        if (buildVersion >= 14) {
            // add support for onHover and GenericMotionEvent (ie. gamepads)
            gestureHandler = new AndroidGestureHandler(this);
            touchHandler = new AndroidTouchHandler14(this, gestureHandler);
            keyHandler = new AndroidKeyHandler(this);
        } else if (buildVersion >= 8){
            gestureHandler = new AndroidGestureHandler(this);
            touchHandler = new AndroidTouchHandler(this, gestureHandler);
            keyHandler = new AndroidKeyHandler(this);
        }
    }

    public AndroidInputHandler(AndroidTouchHandler touchInput,
            AndroidKeyHandler keyInput, AndroidGestureHandler gestureHandler) {
        this.touchHandler = touchInput;
        this.keyHandler = keyInput;
        this.gestureHandler = gestureHandler;
    }

    public void setView(View view) {
        if (touchHandler != null) {
            touchHandler.setView(view);
        }
        if (keyHandler != null) {
            keyHandler.setView(view);
        }
        if (gestureHandler != null) {
            gestureHandler.setView(view);
        }
        this.view = (GLSurfaceView)view;
    }

    public View getView() {
        return view;
    }

    public float invertX(float origX) {
        return getJmeX(view.getWidth()) - origX;
    }

    public float invertY(float origY) {
        return getJmeY(view.getHeight()) - origY;
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
        joystickEventsEnabled = settings.useJoysticks();

        // view width and height are 0 until the view is displayed on the screen
        if (view.getWidth() != 0 && view.getHeight() != 0) {
            scaleX = (float)settings.getWidth() / (float)view.getWidth();
            scaleY = (float)settings.getHeight() / (float)view.getHeight();
        }
        logger.log(Level.FINE, "Setting input scaling, scaleX: {0}, scaleY: {1}",
                new Object[]{scaleX, scaleY});

    }

        // -----------------------------------------
    // JME3 Input interface
    @Override
    public void initialize() {
        touchEventPool.initialize();
        if (touchHandler != null) {
            touchHandler.initialize();
        }
        if (keyHandler != null) {
            keyHandler.initialize();
        }
        if (gestureHandler != null) {
            gestureHandler.initialize();
        }

        initialized = true;
    }

    @Override
    public void destroy() {
        initialized = false;

        touchEventPool.destroy();
        if (touchHandler != null) {
            touchHandler.destroy();
        }
        if (keyHandler != null) {
            keyHandler.destroy();
        }
        if (gestureHandler != null) {
            gestureHandler.destroy();
        }

        setView(null);
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
        inputEventQueue.add(event);
        if (event instanceof TouchEvent) {
            touchEventPool.storeEvent((TouchEvent)event);
        }
    }

    public void setSimulateMouse(boolean simulate) {
        this.mouseEventsEnabled = simulate;
    }

    public boolean isSimulateMouse() {
        return mouseEventsEnabled;
    }

    public boolean getSimulateMouse() {
        return mouseEventsEnabled;
    }

    public boolean isMouseEventsInvertX() {
        return mouseEventsInvertX;
    }

    public boolean isMouseEventsInvertY() {
        return mouseEventsInvertY;
    }

    public void setSimulateKeyboard(boolean simulate) {
        this.keyboardEventsEnabled = simulate;
    }

    public boolean isSimulateKeyboard() {
        return keyboardEventsEnabled;
    }

    public void setOmitHistoricEvents(boolean dontSendHistory) {
        this.dontSendHistory = dontSendHistory;
    }

}
