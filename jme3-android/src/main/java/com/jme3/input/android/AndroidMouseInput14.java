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

import android.view.MotionEvent;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.system.AppSettings;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AndroidMouseInput14</code> implements <code>MouseInput</code> to add mouse support for jME3
 * uses the onGenericMotion events that where added in Android rev 12 and MotionEvent.getButtonState
 * from Android rev 14 so added "14" suffix to the class to specify the Android required rev and
 * match other classes naming
 *
 * @author joliver82
 */
public class AndroidMouseInput14 implements MouseInput {
    private static final Logger logger = Logger.getLogger(AndroidMouseInput14.class.getName());

    protected AndroidInputHandler inputHandler;

    private boolean initialized = false;
    private RawInputListener listener = null;
    private ConcurrentLinkedQueue<InputEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private float scaleX = 1f;
    private float scaleY = 1f;

    protected class MouseState {
        int x, y, wheel;
        boolean left, right, center;

        protected void setStartPosition(int startingX,int startingY) {
            x = startingX;
            y = startingY;
        }

        protected int updateX(int newX) {
            int deltaX=newX-x;
            x=newX;
            return deltaX;
        }

        protected int incrementX(int deltaX) {
            x+=deltaX;
            return x;
        }

        protected int updateY(int newY) {
            int deltaY=newY-y;
            y=newY;
            return deltaY;
        }

        protected int incrementY(int deltaY) {
            y+=deltaY;
            return y;
        }

        protected int incrementWheel(int deltaWheel) {
            wheel+=deltaWheel;
            return wheel;
        }

        protected boolean updateLeftButton(boolean left) {
            if(this.left == left) {
                return false;
            }
            this.left = left;
            return true;
        }

        protected boolean updateRightButton(boolean right) {
            if(this.right == right) {
                return false;
            }
            this.right = right;
            return true;
        }

        protected boolean updateCenterButton(boolean center) {
            if(this.center == center) {
                return false;
            }
            this.center = center;
            return true;
        }
    }

    MouseState currentMouseState = new MouseState();

    public AndroidMouseInput14(AndroidInputHandler inputHandler) {
        this.inputHandler = inputHandler;
    }

    protected int getJmeX(float origX) {
        return (int) (origX * scaleX);
    }

    protected int getJmeY(float origY) {
        return (int) (origY * scaleY);
    }

    public void loadSettings(AppSettings settings) {
        // view width and height are 0 until the view is displayed on the screen
        if (inputHandler.getView().getWidth() != 0 && inputHandler.getView().getHeight() != 0) {
            scaleX = settings.getWidth() / (float)inputHandler.getView().getWidth();
            scaleY = settings.getHeight() / (float)inputHandler.getView().getHeight();
            currentMouseState.setStartPosition(inputHandler.getView().getWidth()/2, inputHandler.getView().getHeight()/2);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Setting input scaling, scaleX: {0}, scaleY: {1}",
                    new Object[]{scaleX, scaleY});
        }

    }

    protected void addMouseMotionEventFixedPositions(int x, int y, int deltaWheel) {
        int deltaX=currentMouseState.updateX(x);
        int deltaY=currentMouseState.updateY(y);
        int wheel=currentMouseState.incrementWheel(deltaWheel);

        logger.log(Level.INFO, "Mouse motion event: " + x + "x" + y + " wheel: " + wheel);

        eventQueue.add(new MouseMotionEvent(x, y, deltaX, deltaY, wheel, deltaWheel));
    }

    protected void addMouseMotionEventRelativePositions(int deltaX, int deltaY, int deltaWheel) {
        int x=currentMouseState.incrementX(deltaX);
        int y=currentMouseState.incrementY(deltaY);
        int wheel=currentMouseState.incrementWheel(deltaWheel);

        logger.log(Level.INFO, "Mouse motion event: " + x + "x" + y + " wheel: " + wheel);

        eventQueue.add(new MouseMotionEvent(x, y, deltaX, deltaY, wheel, deltaWheel));
    }

    protected boolean addMouseButtonEvent(boolean left, boolean right, boolean center, int x, int y) {
        boolean eventAdded = false;
        if(currentMouseState.updateLeftButton(left)) {
            eventQueue.add(new MouseButtonEvent(MouseInput.BUTTON_LEFT, left, x, y));
            logger.log(Level.INFO, "Mouse button left: " + left);
            eventAdded = true;
        }
        if(currentMouseState.updateRightButton(right)) {
            eventQueue.add(new MouseButtonEvent(MouseInput.BUTTON_RIGHT, right, x, y));
            logger.log(Level.INFO, "Mouse button right: " + right);
            eventAdded = true;
        }
        if(currentMouseState.updateCenterButton(center)) {
            eventQueue.add(new MouseButtonEvent(MouseInput.BUTTON_MIDDLE, center, x, y));
            logger.log(Level.INFO, "Mouse button center: " + center);
            eventAdded = true;
        }
        return eventAdded;
    }

    public boolean onHover(MotionEvent event) {
        boolean consumed = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_SCROLL:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_HOVER_ENTER:
                addMouseMotionEventFixedPositions(getJmeX(event.getX()), getJmeY(event.getY()), (int) event.getAxisValue(MotionEvent.AXIS_VSCROLL));
                consumed = true;
                break;
        }

        return consumed;
    }

    public boolean onGenericMotion(MotionEvent event) {
        boolean consumed = false;
        boolean btnEventReceived = false;
        boolean leftPressed = false, rightPressed = false, centerPressed = false;

        int btnState = event.getButtonState();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if((btnState & MotionEvent.BUTTON_PRIMARY) == MotionEvent.BUTTON_PRIMARY) {
                    leftPressed = true;
                }
                if((btnState & MotionEvent.BUTTON_SECONDARY) == MotionEvent.BUTTON_SECONDARY) {
                    rightPressed = true;
                }
                if((btnState & MotionEvent.BUTTON_TERTIARY) == MotionEvent.BUTTON_TERTIARY) {
                    centerPressed = true;
                }
                btnEventReceived = true;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if((btnState & MotionEvent.BUTTON_PRIMARY) == MotionEvent.BUTTON_PRIMARY) {
                    leftPressed = false;
                }
                if((btnState & MotionEvent.BUTTON_SECONDARY) == MotionEvent.BUTTON_SECONDARY) {
                    rightPressed = false;
                }
                if((btnState & MotionEvent.BUTTON_TERTIARY) == MotionEvent.BUTTON_TERTIARY) {
                    centerPressed = false;
                }
                btnEventReceived = true;
                break;

            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_SCROLL:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_HOVER_MOVE:
                addMouseMotionEventFixedPositions(getJmeX(event.getX()), getJmeY(event.getY()), (int) event.getAxisValue(MotionEvent.AXIS_VSCROLL));
                consumed = true;
                break;
        }

        if (btnEventReceived) {
            consumed = addMouseButtonEvent(leftPressed, rightPressed, centerPressed, getJmeX(event.getX()), getJmeY(event.getY()));
        }

        return consumed;
    }

    @Override
    public void setCursorVisible(boolean visible) {
        logger.log(Level.FINE, "Cannot hide mouse till API 24");
    }

    @Override
    public void setMouseGrab(boolean grab) {
        logger.log(Level.FINE, "Cannot grab mouse till API 26");
    }

    @Override
    public int getButtonCount() {
        return 3; // No way to get the number of buttons, defaulting to 3 buttons
    }

    @Override
    public void setNativeCursor(JmeCursor cursor) {
        logger.log(Level.FINE, "Cannot change cursor till API 24");
    }

    @Override
    public void initialize() {
        initialized = true;
    }

    @Override
    public void update() {
        if (listener != null) {
            InputEvent inputEvent;

            while ((inputEvent = eventQueue.poll()) != null) {
                if (inputEvent instanceof MouseMotionEvent) {
                    listener.onMouseMotionEvent((MouseMotionEvent)inputEvent);
                } else if (inputEvent instanceof MouseButtonEvent) {
                    listener.onMouseButtonEvent((MouseButtonEvent)inputEvent);
                }
            }
        }

    }

    @Override
    public void destroy() {
        initialized = false;
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
}
