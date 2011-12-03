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

import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.system.lwjgl.LwjglAbstractDisplay;
import com.jme3.system.lwjgl.LwjglTimer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;

public class LwjglKeyInput implements KeyInput {

    private static final Logger logger = Logger.getLogger(LwjglKeyInput.class.getName());

    private LwjglAbstractDisplay context;

    private RawInputListener listener;

    public LwjglKeyInput(LwjglAbstractDisplay context){
        this.context = context;
    }

    public void initialize() {
        if (!context.isRenderable())
            return;
        
        try {
            Keyboard.create();
            Keyboard.enableRepeatEvents(true);
            logger.info("Keyboard created.");
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "Error while creating keyboard.", ex);
        }
    }

    public int getKeyCount(){
        return Keyboard.KEYBOARD_SIZE;
    }

    public void update() {
        if (!context.isRenderable())
            return;
        
        Keyboard.poll();
        while (Keyboard.next()){
            int keyCode = Keyboard.getEventKey();
            char keyChar = Keyboard.getEventCharacter();
            boolean pressed = Keyboard.getEventKeyState();
            boolean down = Keyboard.isRepeatEvent();
            long time = Keyboard.getEventNanoseconds();
            KeyInputEvent evt = new KeyInputEvent(keyCode, keyChar, pressed, down);
            evt.setTime(time);
            listener.onKeyEvent(evt);
        }
    }

    public void destroy() {
        if (!context.isRenderable())
            return;
        
        Keyboard.destroy();
        logger.info("Keyboard destroyed.");
    }

    public boolean isInitialized() {
        return Keyboard.isCreated();
    }

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return Sys.getTime() * LwjglTimer.LWJGL_TIME_TO_NANOS;
    }

}
