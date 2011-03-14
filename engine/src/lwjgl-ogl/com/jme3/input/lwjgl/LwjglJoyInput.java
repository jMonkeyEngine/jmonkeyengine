/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.system.lwjgl.LwjglTimer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

@Deprecated
class LwjglJoyInput implements JoyInput {

    private static final Logger logger = Logger.getLogger(LwjglKeyInput.class.getName());

    private RawInputListener listener;
    private boolean enabled = false;

    public void initialize() {
        try {
            Controllers.create();
            if (Controllers.getControllerCount() == 0 || !Controllers.isCreated()){
                logger.warning("Joysticks disabled.");
                return;
            }
            logger.info("Joysticks created.");
            enabled = true;
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "Failed to create joysticks", ex);
        }
    }

    public int getJoyCount() {
        return Controllers.getControllerCount();
    }

    public String getJoyName(int joyIndex) {
        return Controllers.getController(joyIndex).getName();
    }

    public int getAxesCount(int joyIndex) {
        return Controllers.getController(joyIndex).getAxisCount();
    }

    public int getButtonCount(int joyIndex) {
        return Controllers.getController(joyIndex).getButtonCount();
    }

    private void printController(Controller c){
        System.out.println("Name: "+c.getName());
        System.out.println("Index: "+c.getIndex());
        System.out.println("Button Count: "+c.getButtonCount());
        System.out.println("Axis Count: "+c.getAxisCount());

        int buttons = c.getButtonCount();
        for (int b = 0; b < buttons; b++) {
            System.out.println("Button " + b + " = " + c.getButtonName(b));
        }

        int axis = c.getAxisCount();
        for (int b = 0; b < axis; b++) {
            System.out.println("Axis " + b + " = " + c.getAxisName(b));
        }
    }

    public void update() {
        if (!enabled)
            return;

        Controllers.poll();
        while (Controllers.next()){
            Controller c = Controllers.getEventSource();
            if (Controllers.isEventAxis()){
                int realAxis = Controllers.getEventControlIndex();
                JoyAxisEvent evt = new JoyAxisEvent(c.getIndex(),
                                                    realAxis,
                                                    c.getAxisValue(realAxis));
                listener.onJoyAxisEvent(evt);
            }else if (Controllers.isEventPovX()){
                JoyAxisEvent evt = new JoyAxisEvent(c.getIndex(),
                                                    JoyInput.AXIS_POV_X,
                                                    c.getPovX());
                listener.onJoyAxisEvent(evt);
            }else if (Controllers.isEventPovY()){
                JoyAxisEvent evt = new JoyAxisEvent(c.getIndex(),
                                                    JoyInput.AXIS_POV_Y,
                                                    c.getPovY());
                listener.onJoyAxisEvent(evt);
            }else if (Controllers.isEventButton()){
                int btn = Controllers.getEventControlIndex();
                JoyButtonEvent evt = new JoyButtonEvent(c.getIndex(),
                                                        btn,
                                                        c.isButtonPressed(btn));
                listener.onJoyButtonEvent(evt);
            }
        }
        Controllers.clearEvents();
    }

    public void destroy() {
        if (!enabled)
            return;

        Controllers.destroy();
        logger.info("Joysticks destroyed.");
    }

    public boolean isInitialized() {
        if (!enabled)
            return false;
        
        return Controllers.isCreated();
    }

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return Sys.getTime() * LwjglTimer.LWJGL_TIME_TO_NANOS;
    }

    public void setJoyRumble(int joyId, float amount){
    }

    public Joystick[] loadJoysticks(InputManager inputManager) {
        int count = Controllers.getControllerCount();
        Joystick[] joysticks = new Joystick[count];
        for (int i = 0; i < count; i++){
            Controller c = Controllers.getController(i);
            Joystick j = new Joystick(inputManager, 
                                        this,
                                        i,
                                        c.getName(),
                                        c.getButtonCount(),
                                        c.getAxisCount(),
                                        -1,-1);
            joysticks[i] = j;
        }
        return joysticks;
    }

}
