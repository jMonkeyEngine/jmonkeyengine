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
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

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
    private Point lastKnownLocation;
    private Point lockPosition;
    private boolean isRecentering;
    private boolean cursorMoved;
    private int eventsSinceRecenter;
    private volatile int mousePressedX;
    private volatile int mousePressedY;

    public NewtMouseInput() {
        location = new Point();
        centerLocation = new Point();
        lastKnownLocation = new Point();
        lockPosition = new Point();
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
            lastKnownLocation = new Point();
            lockPosition = new Point();
        }

        component = comp;
        component.addMouseListener(this);
        component.addWindowListener(new WindowAdapter(){

            @Override
            public void windowGainedFocus(WindowEvent e) {
                setCursorVisible(visible);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                //without those lines,
                //on Linux (OpenBox) the mouse is not restored if invisible (eg via Alt-Tab)
                component.setPointerVisible(true);
                component.confinePointer(false);
            }
            
        });
    }

    @Override
    public void initialize() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isInitialized() {
        return true;
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
    public void setCursorVisible(boolean visible) {
        this.visible = visible;
        component.setPointerVisible(visible);
        lockPosition.set(lastKnownLocation.getX(), lastKnownLocation.getY());
        hack_confinePointer();
    }

    private void hack_confinePointer() {
      if (component.hasFocus() && !component.isPointerVisible()) {
        recenterMouse(component);
      }
    }
    
    @Override
    public void update() {
      if (!component.hasFocus()) return;
        if (cursorMoved) {
            int newX = location.getX();
            int newY = location.getY();
            int newWheel = wheelPos;

            // invert DY
            int actualX = lastKnownLocation.getX();
            int actualY = component.getSurfaceHeight() - lastKnownLocation.getY();
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

    @Override
    public int getButtonCount() {
        return 3;
    }

    @Override
    public void mouseClicked(MouseEvent awtEvt) {
//        MouseButtonEvent evt = new MouseButtonEvent(getJMEButtonIndex(arg0), false);
//        listener.onMouseButtonEvent(evt);
    }

    @Override
    public void mousePressed(MouseEvent newtEvt) {
        mousePressedX = newtEvt.getX();
        mousePressedY = component.getSurfaceHeight() - newtEvt.getY();
        MouseButtonEvent evt = new MouseButtonEvent(getJMEButtonIndex(newtEvt), true, mousePressedX, mousePressedY);
        evt.setTime(newtEvt.getWhen());
        synchronized (eventQueue) {
            eventQueue.add(evt);
        }
    }

    @Override
     public void mouseReleased(MouseEvent awtEvt) {
        MouseButtonEvent evt = new MouseButtonEvent(getJMEButtonIndex(awtEvt), false, awtEvt.getX(), component.getSurfaceHeight() - awtEvt.getY());
        evt.setTime(awtEvt.getWhen());
        synchronized (eventQueue) {
            eventQueue.add(evt);
        }
    }

    @Override
    public void mouseEntered(MouseEvent awtEvt) {
        hack_confinePointer();
    }

    @Override
    public void mouseExited(MouseEvent awtEvt) {
        hack_confinePointer();
    }

    @Override
    public void mouseWheelMoved(MouseEvent awtEvt) {
        //FIXME not sure this is the right way to handle this case
        // [0] should be used when the shift key is down
        float dwheel = awtEvt.getRotation()[1];
        wheelPos += dwheel * WHEEL_AMP;
        cursorMoved = true;
    }

    @Override
    public void mouseDragged(MouseEvent awtEvt) {
        mouseMoved(awtEvt);
    }

    @Override
    public void mouseMoved(MouseEvent awtEvt) {
        if (isRecentering) {
            // MHenze (cylab) Fix Issue 35:
            // As long as the MouseInput is in recentering mode, nothing is done until the mouse is entered in the component
            // by the events generated by the robot. If this happens, the last known location is resetted.
            if ((lockPosition.getX() == awtEvt.getX() && lockPosition.getY() == awtEvt.getY()) || eventsSinceRecenter++ == 5) {
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
            hack_confinePointer();
            lastKnownLocation.setX(awtEvt.getX());
            lastKnownLocation.setY(awtEvt.getY());

            cursorMoved = true;
        }
    }
    
    // MHenze (cylab) Fix Issue 35: A method to generate recenter the mouse to allow the InputSystem to "grab" the mouse
    private void recenterMouse(final GLWindow component) {
        eventsSinceRecenter = 0;
        isRecentering = true;
        component.warpPointer(lockPosition.getX(), lockPosition.getY());
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

    @Override
    public void setNativeCursor(JmeCursor cursor) {
        final ByteBuffer pixels = Buffers.copyIntBufferAsByteBuffer(cursor.getImagesData());
        final DimensionImmutable size = new Dimension(cursor.getWidth(), cursor.getHeight());
        final PixelFormat pixFormat = PixelFormat.RGBA8888;
        final PixelRectangle.GenericPixelRect rec = new PixelRectangle.GenericPixelRect(pixFormat, size, 0, true, pixels);
        final PointerIcon joglCursor = component.getScreen().getDisplay().createPointerIcon(rec, cursor.getXHotSpot(), cursor.getHeight() - cursor.getYHotSpot());
        component.setPointerIcon(joglCursor);
    }
}
