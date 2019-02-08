/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.input;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.system.AWTContext;

/**
 * The implementation of the {@link MouseInput} dedicated to AWT {@link Component component}.
 * <p>
 * This class is based on the <a href="http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html">JavaFX</a> original code provided by Alexander Brui (see <a href="https://github.com/JavaSaBr/JME3-JFX">JME3-FX</a>)
 * </p>
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Alexander Brui (JavaSaBr)
 */
public class AWTMouseInput extends AWTInput implements MouseInput, MouseListener, MouseMotionListener, MouseWheelListener {

    private static final Map<Integer, Integer> MOUSE_BUTTON_TO_JME = new HashMap<>();

    static {
        MOUSE_BUTTON_TO_JME.put(MouseEvent.BUTTON1, BUTTON_LEFT);
        MOUSE_BUTTON_TO_JME.put(MouseEvent.BUTTON2, BUTTON_RIGHT);
        MOUSE_BUTTON_TO_JME.put(MouseEvent.BUTTON3, BUTTON_MIDDLE);
    }

    /**
     * The scale factor for scrolling.
     */
    private static final int WHEEL_SCALE = 10;

    private final LinkedList<MouseMotionEvent> mouseMotionEvents;

    private final LinkedList<MouseButtonEvent> mouseButtonEvents;

    private int mouseX;
    private int mouseY;
    private int mouseWheel;

    public AWTMouseInput(AWTContext context) {
        super(context);
        mouseMotionEvents = new LinkedList<MouseMotionEvent>();
        mouseButtonEvents = new LinkedList<MouseButtonEvent>();
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addMouseWheelListener(this);
    }

    @Override
    public void unbind() {
        if (component != null) {
          component.removeMouseListener(this);
          component.removeMouseMotionListener(this);
          component.removeMouseWheelListener(this);
        }
        super.unbind();
    }

    @Override
    protected void updateImpl() {
        while (!mouseMotionEvents.isEmpty()) {
            listener.onMouseMotionEvent(mouseMotionEvents.poll());
        }
        while (!mouseButtonEvents.isEmpty()) {
            listener.onMouseButtonEvent(mouseButtonEvents.poll());
        }
    }

    private void onWheelScroll(final double xOffset, final double yOffset) {

        mouseWheel += yOffset;

        final MouseMotionEvent mouseMotionEvent = new MouseMotionEvent(mouseX, mouseY, 0, 0, mouseWheel, (int) Math.round(yOffset));
        mouseMotionEvent.setTime(getInputTimeNanos());

        EXECUTOR.addToExecute(new Runnable() {

          @Override
          public void run() {
            mouseMotionEvents.add(mouseMotionEvent);
          }
          
        });
    }

    private void onCursorPos(double xpos, double ypos) {

        int xDelta;
        int yDelta;
        int x = (int) Math.round(xpos);
        int y = context.getHeight() - (int) Math.round(ypos);

        if (mouseX == 0) mouseX = x;
        if (mouseY == 0) mouseY = y;

        xDelta = x - mouseX;
        yDelta = y - mouseY;

        mouseX = x;
        mouseY = y;

        if (xDelta == 0 && yDelta == 0) return;

        final MouseMotionEvent mouseMotionEvent = new MouseMotionEvent(x, y, xDelta, yDelta, mouseWheel, 0);
        mouseMotionEvent.setTime(getInputTimeNanos());

        EXECUTOR.addToExecute(new Runnable() {

          @Override
          public void run() {
            mouseMotionEvents.add(mouseMotionEvent);
          }
          
        });
    }

    private void onMouseButton(MouseEvent event, final boolean pressed) {

        final MouseButtonEvent mouseButtonEvent = new MouseButtonEvent(convertButton(event.getButton()), pressed, mouseX, mouseY);
        mouseButtonEvent.setTime(getInputTimeNanos());

        EXECUTOR.addToExecute(new Runnable() {

          @Override
          public void run() {
            mouseButtonEvents.add(mouseButtonEvent);
          }
          
        });
    }

    private int convertButton(int i) {
        final Integer result = MOUSE_BUTTON_TO_JME.get(i);
        return result == null ? 0 : result;
    }

    @Override
    public void setCursorVisible(final boolean visible) {
    }

    @Override
    public int getButtonCount() {
        return 3;
    }

    @Override
    public void setNativeCursor(JmeCursor cursor) {
    }

    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void mouseMoved(java.awt.event.MouseEvent e) {
      onCursorPos(e.getX(), e.getY());
    }

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
      onMouseButton(e, true);
    }

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
      onMouseButton(e, false);
    }

    @Override
    public void mouseEntered(java.awt.event.MouseEvent e) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent e) {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      onWheelScroll(e.getWheelRotation() * WHEEL_SCALE, e.getWheelRotation() * WHEEL_SCALE);
    }
}