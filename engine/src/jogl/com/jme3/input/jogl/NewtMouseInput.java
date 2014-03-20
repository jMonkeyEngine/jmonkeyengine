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

package com.jme3.input.jogl;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.PixelFormat;
import javax.media.nativewindow.util.PixelRectangle;
import javax.media.nativewindow.util.Point;

public class NewtMouseInput  implements MouseInput, MouseListener {
    
    public static int WHEEL_AMP = 40;   // arbitrary...  Java's mouse wheel seems to report something a lot lower than lwjgl's

    private static final Logger logger = Logger.getLogger(NewtMouseInput.class.getName());

    private boolean visible = true;

    private RawInputListener listener;

    private GLWindow component;

    private final ArrayList<MouseButtonEvent> eventQueue = new ArrayList<MouseButtonEvent>();
    private final ArrayList<MouseButtonEvent> eventQueueCopy = new ArrayList<MouseButtonEvent>();

    private int lastEventX;
    private int lastEventY;
    private int lastEventWheel;

    private int wheelPos;
    private Point location;
    private Point centerLocation;
    private Point centerLocationOnScreen;
    private Point lastKnownLocation;
    private boolean isRecentering;
    private boolean cursorMoved;
    private int eventsSinceRecenter;

    public NewtMouseInput() {
        location = new Point();
        centerLocation = new Point();
        centerLocationOnScreen = new Point();
        lastKnownLocation = new Point();
    }

    public void setInputSource(GLWindow comp) {
        if (component != null) {
            component.removeMouseListener(this);

            eventQueue.clear();

            wheelPos = 0;
            isRecentering = false;
            eventsSinceRecenter = 0;
            lastEventX = 0;
            lastEventY = 0;
            lastEventWheel = 0;
            location = new Point();
            centerLocation = new Point();
            centerLocationOnScreen = new Point();
            lastKnownLocation = new Point();
        }

        component = comp;
        component.addMouseListener(this);
    }

    public void initialize() {
    }

    public void destroy() {
    }

    public boolean isInitialized() {
        return true;
    }

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return System.nanoTime();
    }

    public void setCursorVisible(boolean visible) {
        if (this.visible != visible) {
            lastKnownLocation.setX(0);
            lastKnownLocation.setY(0);

            this.visible = visible;
            component.setPointerVisible(visible);
            if (!visible) {
                recenterMouse(component);
            }
        }
    }

    public void update() {
        if (cursorMoved) {
            int newX = location.getX();
            int newY = location.getY();
            int newWheel = wheelPos;

            // invert DY
            int actualX = lastKnownLocation.getX();
            int actualY = component.getHeight() - lastKnownLocation.getY();
            MouseMotionEvent evt = new MouseMotionEvent(actualX, actualY,
                                                        newX - lastEventX,
                                                        lastEventY - newY,
                                                        wheelPos, lastEventWheel - wheelPos);
            listener.onMouseMotionEvent(evt);

            lastEventX = newX;
            lastEventY = newY;
            lastEventWheel = newWheel;

            cursorMoved = false;
        }

        synchronized (eventQueue) {
            eventQueueCopy.clear();
            eventQueueCopy.addAll(eventQueue);
            eventQueue.clear();
        }

        int size = eventQueueCopy.size();
        for (int i = 0; i < size; i++) {
            listener.onMouseButtonEvent(eventQueueCopy.get(i));
        }
    }

    public int getButtonCount() {
        return 3;
    }

    public void mouseClicked(MouseEvent awtEvt) {
//        MouseButtonEvent evt = new MouseButtonEvent(getJMEButtonIndex(arg0), false);
//        listener.onMouseButtonEvent(evt);
    }

    public void mousePressed(MouseEvent newtEvt) {
        MouseButtonEvent evt = new MouseButtonEvent(getJMEButtonIndex(newtEvt), true, newtEvt.getX(), newtEvt.getY());
        evt.setTime(newtEvt.getWhen());
        synchronized (eventQueue) {
            eventQueue.add(evt);
        }
    }

    public void mouseReleased(MouseEvent awtEvt) {
        MouseButtonEvent evt = new MouseButtonEvent(getJMEButtonIndex(awtEvt), false, awtEvt.getX(), awtEvt.getY());
        evt.setTime(awtEvt.getWhen());
        synchronized (eventQueue) {
            eventQueue.add(evt);
        }
    }

    public void mouseEntered(MouseEvent awtEvt) {
        if (!visible) {
            recenterMouse(component);
        }
    }

    public void mouseExited(MouseEvent awtEvt) {
        if (!visible) {
            recenterMouse(component);
        }
    }

    public void mouseWheelMoved(MouseEvent awtEvt) {
        //FIXME not sure this is the right way to handle this case
        // [0] should be used when the shift key is down
        float dwheel = awtEvt.getRotation()[1];
        wheelPos += dwheel * WHEEL_AMP;
        cursorMoved = true;
    }

    public void mouseDragged(MouseEvent awtEvt) {
        mouseMoved(awtEvt);
    }

    public void mouseMoved(MouseEvent awtEvt) {
        if (isRecentering) {
            // MHenze (cylab) Fix Issue 35:
            // As long as the MouseInput is in recentering mode, nothing is done until the mouse is entered in the component
            // by the events generated by the robot. If this happens, the last known location is resetted.
            if ((centerLocation.getX() == awtEvt.getX() && centerLocation.getY() == awtEvt.getY()) || eventsSinceRecenter++ == 5) {
                lastKnownLocation.setX(awtEvt.getX());
                lastKnownLocation.setY(awtEvt.getY());
                isRecentering = false;
            }
        } else {
            // MHenze (cylab) Fix Issue 35:
            // Compute the delta and absolute coordinates and recenter the mouse if necessary
            int dx = awtEvt.getX() - lastKnownLocation.getX();
            int dy = awtEvt.getY() - lastKnownLocation.getY();
            location.setX(location.getX() + dx);
            location.setY(location.getY() + dy);
            if (!visible) {
                recenterMouse(component);
            }
            lastKnownLocation.setX(awtEvt.getX());
            lastKnownLocation.setY(awtEvt.getY());

            cursorMoved = true;
        }
    }

    // MHenze (cylab) Fix Issue 35: A method to generate recenter the mouse to allow the InputSystem to "grab" the mouse
    private void recenterMouse(final GLWindow component) {
        eventsSinceRecenter = 0;
        isRecentering = true;
        centerLocation.setX(component.getWidth() / 2);
        centerLocation.setY(component.getHeight() / 2);
        centerLocationOnScreen.setX(centerLocation.getX());
        centerLocationOnScreen.setY(centerLocation.getY());
        
        component.warpPointer(centerLocationOnScreen.getX(), centerLocationOnScreen.getY());
    }

    private int getJMEButtonIndex(MouseEvent awtEvt) {
        int index;
        switch (awtEvt.getButton()) {
            default:
            case MouseEvent.BUTTON1: //left
                index = MouseInput.BUTTON_LEFT;
                break;
            case MouseEvent.BUTTON2: //middle
                index = MouseInput.BUTTON_MIDDLE;
                break;
            case MouseEvent.BUTTON3: //right
                index = MouseInput.BUTTON_RIGHT;
                break;
            case MouseEvent.BUTTON4:
            case MouseEvent.BUTTON5:
            case MouseEvent.BUTTON6:
            case MouseEvent.BUTTON7:
            case MouseEvent.BUTTON8:
            case MouseEvent.BUTTON9:
                //FIXME
                index = 0;
                break;
        }
        return index;
    }

    public void setNativeCursor(JmeCursor cursor) {
        final ByteBuffer pixels = Buffers.copyIntBufferAsByteBuffer(cursor.getImagesData());
        final DimensionImmutable size = new Dimension(cursor.getWidth(), cursor.getHeight());
        final PixelFormat pixFormat = PixelFormat.RGBA8888;
        final PixelRectangle.GenericPixelRect rec = new PixelRectangle.GenericPixelRect(pixFormat, size, 0, true, pixels);
        final PointerIcon joglCursor = component.getScreen().getDisplay().createPointerIcon(rec, cursor.getXHotSpot(), cursor.getYHotSpot());
        component.setPointerIcon(joglCursor);
    }
}