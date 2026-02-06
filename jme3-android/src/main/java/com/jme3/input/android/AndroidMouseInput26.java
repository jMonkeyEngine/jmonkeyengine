/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.PointerIcon;

import com.jme3.cursors.plugins.JmeCursor;

import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AndroidMouseInput26</code> extends <code>AndroidMouseInput24</code> to improve mouse support
 * adding grab/capture support using onCapturedPointer events
 *
 * @author joliver82
 */
public class AndroidMouseInput26 extends AndroidMouseInput24{
    private static final Logger logger = Logger.getLogger(AndroidMouseInput26.class.getName());

    public AndroidMouseInput26(AndroidInputHandler inputHandler) {
        super(inputHandler);
    }

    public boolean onCapturedPointer(MotionEvent event) {
        boolean consumed = false;
        boolean btnEventReceived = false;
        boolean leftPressed = false, rightPressed = false, centerPressed = false;

        int btnAction = event.getActionButton();

        switch (event.getAction()) {
            case MotionEvent.ACTION_BUTTON_PRESS:
                if(btnAction == MotionEvent.BUTTON_PRIMARY) {
                    leftPressed = true;
                }
                if(btnAction == MotionEvent.BUTTON_SECONDARY) {
                    rightPressed = true;
                }
                if(btnAction == MotionEvent.BUTTON_TERTIARY) {
                    centerPressed = true;
                }
                btnEventReceived = true;
                break;

            case MotionEvent.ACTION_BUTTON_RELEASE:
            case MotionEvent.ACTION_CANCEL:
                if(btnAction == MotionEvent.BUTTON_PRIMARY) {
                    leftPressed = false;
                }
                if(btnAction == MotionEvent.BUTTON_SECONDARY) {
                    rightPressed = false;
                }
                if(btnAction == MotionEvent.BUTTON_TERTIARY) {
                    centerPressed = false;
                }
                btnEventReceived = true;
                break;

            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_SCROLL:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
                addMouseMotionEventRelativePositions(getJmeX(event.getX()), getJmeY(event.getY()), (int) event.getAxisValue(MotionEvent.AXIS_VSCROLL));
                consumed = true;
                break;
        }

        if (btnEventReceived) {
            consumed = addMouseButtonEvent(leftPressed, rightPressed, centerPressed, getJmeX(event.getX()), getJmeY(event.getY()));
        }

        return consumed;
    }

    @Override
    public void setMouseGrab(boolean grab) {
        if(grab) {
            inputHandler.getView().requestPointerCapture();
        } else {
            inputHandler.getView().releasePointerCapture();
        }
    }

}
