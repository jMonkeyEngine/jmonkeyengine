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
import java.util.logging.Logger;

/**
 * <code>AndroidMouseInput24</code> extends <code>AndroidMouseInput14</code> to improve mouse support
 * using new events defined in API rev 24 and adding support for cursor change and cursor visibility
 *
 * @author joliver82
 */
public class AndroidMouseInput24 extends AndroidMouseInput14{
    private static final Logger logger = Logger.getLogger(AndroidMouseInput24.class.getName());

    public AndroidMouseInput24(AndroidInputHandler inputHandler) {
        super(inputHandler);
    }

    @Override
    public boolean onGenericMotion(MotionEvent event) {

        boolean consumed = super.onGenericMotion(event);

        if (!consumed) {
            boolean leftPressed = false, rightPressed = false, centerPressed = false;
            boolean btnEventReceived = false;
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
            }

            if (btnEventReceived) {
                consumed = addMouseButtonEvent(leftPressed, rightPressed, centerPressed, getJmeX(event.getX()), getJmeY(event.getY()));
            }
        }
        return consumed;
    }

    @Override
    public void setCursorVisible(boolean visible) {
        if(inputHandler.getView()!=null) {
            if(visible) {
                inputHandler.getView().setPointerIcon(null);
            } else {
                inputHandler.getView().setPointerIcon(PointerIcon.getSystemIcon(inputHandler.getView().getContext(), PointerIcon.TYPE_NULL));
            }
        }
    }

    @Override
    public void setNativeCursor(JmeCursor cursor) {
        if(inputHandler.getView()!=null) {
            if(cursor!=null) {
                // Translate into Android Bitmap format ARGB888. Assuming input image as RGBA
                int bufferSize = cursor.getHeight()*cursor.getWidth();
                int[] outputBitmap=new int[bufferSize];
                IntBuffer inputImage = cursor.getImagesData().asReadOnlyBuffer();
                inputImage.clear();
                int[] tmpPixel = new int[4];
                for(int i=0 ; i< bufferSize; ++i) {
                    inputImage.get(tmpPixel);
                    outputBitmap[i] = (tmpPixel[3] & 0xff) << 24 | (tmpPixel[0] & 0xff) << 16 | (tmpPixel[1] & 0xff) << 8 | (tmpPixel[2] & 0xff);
                }
                PointerIcon pointer = PointerIcon.create(
                        Bitmap.createBitmap(outputBitmap, cursor.getWidth(), cursor.getHeight(), Bitmap.Config.ARGB_8888),
                        cursor.getXHotSpot(),
                        cursor.getYHotSpot());
                inputHandler.getView().setPointerIcon(pointer);
            } else {
                inputHandler.getView().setPointerIcon(null);
            }
        }
    }

}
