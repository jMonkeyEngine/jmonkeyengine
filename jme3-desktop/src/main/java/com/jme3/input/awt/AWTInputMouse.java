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
package com.jme3.input.awt;

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
import com.jme3.system.JmeContext;
import com.jme3.system.awt.AWTContext;

/**
 * The implementation of the {@link MouseInput} dedicated to AWT {@link Component component}.
 * <p>
 * This class is based on the <a href="http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html">JavaFX</a> original code provided by Alexander Brui (see <a href="https://github.com/JavaSaBr/JME3-JFX">JME3-FX</a>)
 * </p>
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Alexander Brui (JavaSaBr)
 */
public class AWTInputMouse extends AWTInput implements MouseInput, MouseListener, MouseMotionListener, MouseWheelListener {

    private Map<Integer, Integer> buttonMapping;
    
    /**
     * The scale factor for scrolling.
     */
    private int wheelScale = 10;

    private final LinkedList<MouseMotionEvent> mouseMotionEvents;

    private final LinkedList<MouseButtonEvent> mouseButtonEvents;

    private int mouseX;
    private int mouseY;
    private int mouseWheel;

    /**
     * Create a new {@link MouseInput Mouse input} that is binded with AWT Mouse. By default, mouse button mapping is:
     * <ul>
     * <li>{@link MouseEvent.BUTTON1} is mapped with {@link BUTTON_LEFT}
     * <li>{@link MouseEvent.BUTTON2} is mapped with {@link BUTTON_MIDDLE}
     * <li>{@link MouseEvent.BUTTON3} is mapped with {@link BUTTON_RIGHT}
     * </ul>
     * The button mapping can be changed using {@link #setButtonMapping(Map) setButtonMapping() method}.
     */
    public AWTInputMouse() {
        super();
        mouseMotionEvents = new LinkedList<MouseMotionEvent>();
        mouseButtonEvents = new LinkedList<MouseButtonEvent>();
        
        buttonMapping = new HashMap<>();
        buttonMapping.put(MouseEvent.BUTTON1, BUTTON_LEFT);
        buttonMapping.put(MouseEvent.BUTTON2, BUTTON_MIDDLE);
        buttonMapping.put(MouseEvent.BUTTON3, BUTTON_RIGHT);
    }
    
    /**
     * Create a new {@link MouseInput Mouse input} that is binded with AWT Mouse and the given context. 
     * By default, mouse button mapping is:
     * <ul>
     * <li>{@link MouseEvent.BUTTON1} is mapped with {@link BUTTON_LEFT}
     * <li>{@link MouseEvent.BUTTON2} is mapped with {@link BUTTON_MIDDLE}
     * <li>{@link MouseEvent.BUTTON3} is mapped with {@link BUTTON_RIGHT}
     * </ul>
     * The button mapping can be changed using {@link #setButtonMapping(Map) setButtonMapping() method}.
     * @param context to context to use.
     */
    public AWTInputMouse(JmeContext context) {
        super(context);
        mouseMotionEvents = new LinkedList<MouseMotionEvent>();
        mouseButtonEvents = new LinkedList<MouseButtonEvent>();
        
        buttonMapping = new HashMap<>();
        buttonMapping.put(MouseEvent.BUTTON1, BUTTON_LEFT);
        buttonMapping.put(MouseEvent.BUTTON2, BUTTON_MIDDLE);
        buttonMapping.put(MouseEvent.BUTTON3, BUTTON_RIGHT);
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

    /**
     * Get the scale to apply to Wheel motion.
     * @return the scale to apply to Wheel motion.
     * @see #getSetWheelScale(int)
     */
    public int getWheelScale() {
    	return wheelScale;
    }
    
    /**
     * Set the scale to apply to Wheel motion.
     * @param scale the scale to apply to Wheel motion.
     * @see #getWheelScale()
     */
    public void getSetWheelScale(int scale) {
      wheelScale = scale;
    }
    
    /**
     * Get the button mapping between JME and AWT. 
     * Within the map, keys are the JME button and values are the AWT Mouse button that is affected.
     * @return the button mapping between JME and AWT. 
     * @see #setButtonMapping(Map)
     */
    public Map<Integer, Integer> getButtonMapping() {
    	return buttonMapping;
    }
    
    /**
     * Set the button mapping between JME and AWT. 
     * @param mapping the button mapping between JME and AWT. 
     * @see #getButtonMapping()
     */
    public void setButtonMapping(Map<Integer, Integer> mapping) {
    	this.buttonMapping = mapping;
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
        
        int y = 0;
        if ((context != null) && (context instanceof AWTContext)) {
        	y = ((AWTContext)context).getHeight() - (int) Math.round(ypos);
        } else {
        	y = (int) Math.round(ypos);
        }
        
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
        final Integer result = buttonMapping.get(i);
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
      onCursorPos(e.getX(), e.getY());
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
      onWheelScroll(e.getWheelRotation() * wheelScale, e.getWheelRotation() * wheelScale);
    }
}