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

package com.jme3.input.lwjgl;

import com.jme3.input.AbstractJoystick;
import com.jme3.input.DefaultJoystickAxis;
import com.jme3.input.DefaultJoystickButton;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.JoystickCompatibilityMappings;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Component.POV;

public class JInputJoyInput implements JoyInput {

    private static final Logger logger = Logger.getLogger(InputManager.class.getName());

    private boolean initialized = false;
    private JInputJoystick[] joysticks;
    private RawInputListener listener;

    private Map<Controller, JInputJoystick> joystickIndex = new HashMap<>();

    @Override
    public void setJoyRumble(int joyId, float amount){

        if( joyId >= joysticks.length )
            throw new IllegalArgumentException();

        Controller c = joysticks[joyId].controller;
        for (Rumbler r : c.getRumblers()){
            r.rumble(amount);
        }
    }

    @Override
    public Joystick[] loadJoysticks(InputManager inputManager){
        ControllerEnvironment ce =
            ControllerEnvironment.getDefaultEnvironment();

        Controller[] cs = ce.getControllers();
        List<Joystick> list = new ArrayList<>();
        for( Controller c : ce.getControllers() ) {
            if (c.getType() == Controller.Type.KEYBOARD
             || c.getType() == Controller.Type.MOUSE)
                continue;

            logger.log(Level.FINE, "Attempting to create joystick for: \"{0}\"", c);

            // Try to create it like a joystick
            JInputJoystick stick = new JInputJoystick(inputManager, this, c, list.size(), c.getName());
            for( Component comp : c.getComponents() ) {
                stick.addComponent(comp);
            }

            // If it has no axes then we'll assume it's not
            // a joystick
            if( stick.getAxisCount() == 0 ) {
                logger.log(Level.FINE, "Not a joystick: {0}", c);
                continue;
            }

            joystickIndex.put(c, stick);
            list.add(stick);
        }

        joysticks = list.toArray( new JInputJoystick[list.size()] );

        return joysticks;
    }

    @Override
    public void initialize() {
        initialized = true;
    }

    @Override
    public void update() {
        ControllerEnvironment ce =
            ControllerEnvironment.getDefaultEnvironment();

        Controller[] cs = ce.getControllers();
        Event e = new Event();
        for (int i = 0; i < cs.length; i++){
            Controller c = cs[i];

            JInputJoystick stick = joystickIndex.get(c);
            if( stick == null )
                continue;

            if( !c.poll() )
                continue;

            int joyId = stick.getJoyId();

            EventQueue q = c.getEventQueue();
            while (q.getNextEvent(e)){
                Identifier id = e.getComponent().getIdentifier();
                if (id == Identifier.Axis.POV){
                    float rawX = 0, rawY = 0, x, y;
                    float v = e.getValue();

                    if (v == POV.CENTER){
                        rawX = 0; rawY = 0;
                    }else if (v == POV.DOWN){
                        rawX = 0; rawY = -1f;
                    }else if (v == POV.DOWN_LEFT){
                        rawX = -1f; rawY = -1f;
                    }else if (v == POV.DOWN_RIGHT){
                        rawX = 1f; rawY = -1f;
                    }else if (v == POV.LEFT){
                        rawX = -1f; rawY = 0;
                    }else if (v == POV.RIGHT){
                        rawX = 1f; rawY = 0;
                    }else if (v == POV.UP){
                        rawX = 0; rawY = 1f;
                    }else if (v == POV.UP_LEFT){
                        rawX = -1f; rawY = 1f;
                    }else if (v == POV.UP_RIGHT){
                        rawX = 1f; rawY = 1f;
                    }

                    x = JoystickCompatibilityMappings.remapAxisRange(stick.povX, rawX);
                    y = JoystickCompatibilityMappings.remapAxisRange(stick.povY, rawY);
                    JoyAxisEvent evt1 = new JoyAxisEvent(stick.povX, x, rawX);
                    JoyAxisEvent evt2 = new JoyAxisEvent(stick.povY, y, rawY);
                    listener.onJoyAxisEvent(evt1);
                    listener.onJoyAxisEvent(evt2);
                }else if (id instanceof Axis){
                    float rawValue = e.getValue();
                    float value = JoystickCompatibilityMappings.remapAxisRange(stick.povY, rawValue);

                    JoystickAxis axis = stick.axisIndex.get(e.getComponent());
                    JoyAxisEvent evt = new JoyAxisEvent(axis, value, rawValue);
                    listener.onJoyAxisEvent(evt);
                }else if (id instanceof Button){

                    JoystickButton button = stick.buttonIndex.get(e.getComponent());
                    JoyButtonEvent evt = new JoyButtonEvent(button, e.getValue() == 1f);
                    listener.onJoyButtonEvent(evt);
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
        return 0;
    }

    protected class JInputJoystick extends AbstractJoystick {

        private JoystickAxis nullAxis;
        private Controller controller;
        private JoystickAxis xAxis;
        private JoystickAxis yAxis;
        private JoystickAxis povX;
        private JoystickAxis povY;
        private Map<Component, JoystickAxis> axisIndex = new HashMap<>();
        private Map<Component, JoystickButton> buttonIndex = new HashMap<>();
    
        public JInputJoystick( InputManager inputManager, JoyInput joyInput, Controller controller,
                               int joyId, String name ) {
            super( inputManager, joyInput, joyId, name );

            this.controller = controller;

            this.nullAxis = new DefaultJoystickAxis( getInputManager(), this, -1,
                                                     "Null", "null", false, false, 0 );
            this.xAxis = nullAxis;
            this.yAxis = nullAxis;
            this.povX = nullAxis;
            this.povY = nullAxis;
        }

        protected void addComponent( Component comp ) {

            Identifier id = comp.getIdentifier();
            if( id instanceof Button ) {
                addButton(comp);
            } else if( id instanceof Axis ) {
                addAxis(comp);
            } else {
                logger.log(Level.FINE, "Ignoring: \"{0}\"", comp);
            }
        }

        protected void addButton( Component comp ) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Adding button: \"{0}\" id:" + comp.getIdentifier(), comp);
            }
            Identifier id = comp.getIdentifier();
            if( !(id instanceof Button) ) {
                throw new IllegalArgumentException( "Component is not an button:" + comp );
            }

            String name = comp.getName();
            String original = id.getName();
            try {
                Integer.parseInt(original);
            } catch (NumberFormatException e){
                original = String.valueOf(buttonIndex.size());
            }
            String logicalId = JoystickCompatibilityMappings.remapButton( controller.getName(), original );
            if (logger.isLoggable(Level.FINE) && !Objects.equals(logicalId, original)) {
                logger.log(Level.FINE, "Remapped:" + original + " to:" + logicalId);
            }

            JoystickButton button = new DefaultJoystickButton( getInputManager(), this, getButtonCount(),
                                                               name, logicalId );
            addButton(button);
            buttonIndex.put( comp, button );
        }

        protected void addAxis( Component comp ) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Adding axis: \"{0}\" id:" + comp.getIdentifier(), comp );
            }

            Identifier id = comp.getIdentifier();
            if( !(id instanceof Axis) ) {
                throw new IllegalArgumentException( "Component is not an axis:" + comp );
            }

            String name = comp.getName();
            String original = id.getName();
            String logicalId = JoystickCompatibilityMappings.remapAxis( controller.getName(), original );
            if(logger.isLoggable(Level.FINE) && !Objects.equals(logicalId, original)) {
                logger.log(Level.FINE, "Remapped:" + original + " to:" + logicalId);
            }

            JoystickAxis axis = new DefaultJoystickAxis( getInputManager(),
                                                         this, getAxisCount(), name, logicalId,
                                                         comp.isAnalog(), comp.isRelative(),
                                                         comp.getDeadZone() );
            addAxis(axis);
            axisIndex.put( comp, axis );

            // Support the X/Y axis indexes
            if( id == Axis.X ) {
                xAxis = axis;
            } else if( id == Axis.Y ) {
                yAxis = axis;
            } else if( id == Axis.POV ) {

                // Add two fake axes for the JME provided convenience
                // axes: AXIS_POV_X, AXIS_POV_Y
                povX = new DefaultJoystickAxis( getInputManager(),
                                                this, getAxisCount(), JoystickAxis.POV_X,
                                                id.getName() + "_x",
                                                comp.isAnalog(), comp.isRelative(), comp.getDeadZone() );
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Adding axis: \"{0}\" id:" + id.getName() + "_x", povX.getName() );
                }
                addAxis(povX);
                povY = new DefaultJoystickAxis( getInputManager(),
                                                this, getAxisCount(), JoystickAxis.POV_Y,
                                                id.getName() + "_y",
                                                comp.isAnalog(), comp.isRelative(), comp.getDeadZone() );
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Adding axis: \"{0}\" id:" + id.getName() + "_y", povY.getName() );
                }
                addAxis(povY);
            }

        }

        @Override
        public JoystickAxis getXAxis() {
            return xAxis;
        }

        @Override
        public JoystickAxis getYAxis() {
            return yAxis;
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return povX;
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return povY;
        }

        @Override
        public int getXAxisIndex(){
            return xAxis.getAxisId();
        }

        @Override
        public int getYAxisIndex(){
            return yAxis.getAxisId();
        }
    }
}



