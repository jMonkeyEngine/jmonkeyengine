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

import android.opengl.GLSurfaceView;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.jme3.input.JoyInput;
import com.jme3.input.TouchInput;
import com.jme3.system.AppSettings;
import java.util.logging.Logger;

/**
 * <code>AndroidInput</code> is the main class that connects the Android system
 * inputs to jME. It receives the inputs from the Android View and passes them
 * to the appropriate classes based on the source of the input.<br>
 * This class is to be extended when new functionality is released in Android.
 *
 * @author iwgeric
 */
public class AndroidInputHandler implements View.OnTouchListener,
                                            View.OnKeyListener {

    private static final Logger logger = Logger.getLogger(AndroidInputHandler.class.getName());

    protected GLSurfaceView view;
    protected AndroidTouchInput touchInput;
    protected AndroidJoyInput joyInput;


    public AndroidInputHandler() {
        touchInput = new AndroidTouchInput(this);
        joyInput = new AndroidJoyInput(this);
    }

    public void setView(View view) {
        if (this.view != null && view != null && this.view.equals(view)) {
            return;
        }

        if (this.view != null) {
            removeListeners(this.view);
        }

        this.view = (GLSurfaceView)view;

        if (this.view != null) {
            addListeners(this.view);
        }

        joyInput.setView((GLSurfaceView)view);
    }

    public View getView() {
        return view;
    }

    protected void removeListeners(GLSurfaceView view) {
        view.setOnTouchListener(null);
        view.setOnKeyListener(null);
        touchInput.setGestureDetector(null);
        touchInput.setScaleDetector(null);
    }

    protected void addListeners(GLSurfaceView view) {
        view.setOnTouchListener(this);
        view.setOnKeyListener(this);
        AndroidGestureProcessor gestureHandler = new AndroidGestureProcessor(touchInput);
        touchInput.setGestureDetector(new GestureDetector(
                view.getContext(), gestureHandler));
        touchInput.setScaleDetector(new ScaleGestureDetector(
                view.getContext(), gestureHandler));
    }

    public void loadSettings(AppSettings settings) {
        touchInput.loadSettings(settings);
    }

    public TouchInput getTouchInput() {
        return touchInput;
    }

    public JoyInput getJoyInput() {
        return joyInput;
    }

    /*
     *  Android input events include the source from which the input came from.
     *  We must look at the source of the input event to determine which type
     *  of jME input it belongs to.
     *  If the input is from a gamepad or joystick source, the event is sent
     *  to the JoyInput class to convert the event into jME joystick events.
     *  </br>
     *  If the input is from a touchscreen source, the event is sent to the
     *  TouchProcessor to convert the event into touch events.
     *  The TouchProcessor also converts the events into Mouse and Key events
     *  if AppSettings is set to simulate Mouse or Keyboard events.
     *
     *  Android reports the source as a bitmask as shown below.</br>
     *
     *  InputDevice Sources
     *     0000 0000 0000 0000 0000 0000 0000 0000 - 32 bit bitmask
     *
     *     0000 0000 0000 0000 0000 0000 1111 1111 - SOURCE_CLASS_MASK       (0x000000ff)
     *     0000 0000 0000 0000 0000 0000 0000 0000 - SOURCE_CLASS_NONE       (0x00000000)
     *     0000 0000 0000 0000 0000 0000 0000 0001 - SOURCE_CLASS_BUTTON     (0x00000001)
     *     0000 0000 0000 0000 0000 0000 0000 0010 - SOURCE_CLASS_POINTER    (0x00000002)
     *     0000 0000 0000 0000 0000 0000 0000 0100 - SOURCE_CLASS_TRACKBALL  (0x00000004)
     *     0000 0000 0000 0000 0000 0000 0000 1000 - SOURCE_CLASS_POSITION   (0x00000008)
     *     0000 0000 0000 0000 0000 0000 0001 0000 - SOURCE_CLASS_JOYSTICK   (0x00000010)
     *
     *     1111 1111 1111 1111 1111 1111 0000 0000 - Source_Any              (0xffffff00)
     *     0000 0000 0000 0000 0000 0000 0000 0000 - SOURCE_UNKNOWN          (0x00000000)
     *     0000 0000 0000 0000 0000 0001 0000 0001 - SOURCE_KEYBOARD         (0x00000101)
     *     0000 0000 0000 0000 0000 0010 0000 0001 - SOURCE_DPAD             (0x00000201)
     *     0000 0000 0000 0000 0000 0100 0000 0001 - SOURCE_GAMEPAD          (0x00000401)
     *     0000 0000 0000 0000 0001 0000 0000 0010 - SOURCE_TOUCHSCREEN      (0x00001002)
     *     0000 0000 0000 0000 0010 0000 0000 0010 - SOURCE_MOUSE            (0x00002002)
     *     0000 0000 0000 0000 0100 0000 0000 0010 - SOURCE_STYLUS           (0x00004002)
     *     0000 0000 0000 0001 0000 0000 0000 0100 - SOURCE_TRACKBALL        (0x00010004)
     *     0000 0000 0001 0000 0000 0000 0000 1000 - SOURCE_TOUCHPAD         (0x00100008)
     *     0000 0000 0010 0000 0000 0000 0000 0000 - SOURCE_TOUCH_NAVIGATION (0x00200000)
     *     0000 0001 0000 0000 0000 0000 0001 0000 - SOURCE_JOYSTICK         (0x01000010)
     *     0000 0010 0000 0000 0000 0000 0000 0001 - SOURCE_HDMI             (0x02000001)
     *
     * Example values reported by Android for Source
     * 4,098 = 0x00001002 =
     *     0000 0000 0000 0000 0001 0000 0000 0010 - SOURCE_CLASS_POINTER
     *                                               SOURCE_TOUCHSCREEN
     * 1,281 = 0x00000501 =
     *     0000 0000 0000 0000 0000 0101 0000 0001 - SOURCE_CLASS_BUTTON
     *                                               SOURCE_KEYBOARD
     *                                               SOURCE_GAMEPAD
     * 16,777,232 = 0x01000010 =
     *     0000 0001 0000 0000 0000 0000 0001 0000 - SOURCE_CLASS_JOYSTICK
     *                                               SOURCE_JOYSTICK
     *
     * 16,778,513 = 0x01000511 =
     *     0000 0001 0000 0000 0000 0101 0001 0001 - SOURCE_CLASS_BUTTON
     *                                               SOURCE_CLASS_JOYSTICK
     *                                               SOURCE_GAMEPAD
     *                                               SOURCE_KEYBOARD
     *                                               SOURCE_JOYSTICK
     *
     * 257 = 0x00000101 =
     *     0000 0000 0000 0000 0000 0001 0000 0001 - SOURCE_CLASS_BUTTON
     *                                               SOURCE_KEYBOARD
     *
     *
     *
     */


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view != getView()) {
            return false;
        }

        boolean consumed = false;

        int source = event.getSource();
//        logger.log(Level.INFO, "onTouch source: {0}", source);

        boolean isTouch = ((source & InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN);
//        logger.log(Level.INFO, "onTouch source: {0}, isTouch: {1}",
//                new Object[]{source, isTouch});

        if (isTouch && touchInput != null) {
            // send the event to the touch processor
            consumed = touchInput.onTouch(event);
        }

        return consumed;

    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if (view != getView()) {
            return false;
        }

        boolean consumed = false;

        int source = event.getSource();
//        logger.log(Level.INFO, "onKey source: {0}", source);

        boolean isTouch =
                ((source & InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) ||
                ((source & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD);
//        logger.log(Level.INFO, "onKey source: {0}, isTouch: {1}",
//                new Object[]{source, isTouch});

        if (touchInput != null) {
            consumed = touchInput.onKey(event);
        }

        return consumed;

    }

}
