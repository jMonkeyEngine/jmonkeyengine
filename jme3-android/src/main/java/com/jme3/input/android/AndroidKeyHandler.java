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

package com.jme3.input.android;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.TouchEvent;
import java.util.logging.Logger;

/**
 * AndroidKeyHandler recieves onKey events from the Android system and creates
 * the jME KeyEvents.  onKey is used by Android to receive keys from the keyboard
 * or device buttons.  All key events are consumed by jME except for the Volume
 * buttons and menu button.
 * 
 * This class also provides the functionality to display or hide the soft keyboard
 * for inputing single key events.  Use OGLESContext to display an dialog to type
 * in complete strings.
 * 
 * @author iwgeric
 */
public class AndroidKeyHandler implements View.OnKeyListener {
    private static final Logger logger = Logger.getLogger(AndroidKeyHandler.class.getName());
    
    private AndroidInputHandler androidInput;
    private boolean sendKeyEvents = true;
    
    public AndroidKeyHandler(AndroidInputHandler androidInput) {
        this.androidInput = androidInput;
    }
    
    public void initialize() {
    }
    
    public void destroy() {
    }
    
    public void setView(View view) {
        if (view != null) {
            view.setOnKeyListener(this);
        } else {
            androidInput.getView().setOnKeyListener(null);
        }
    }
    
    /**
     * onKey gets called from android thread on key events
     */
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if (androidInput.isInitialized() && view != androidInput.getView()) {
            return false;
        }
        
        TouchEvent evt;
        // TODO: get touch event from pool
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            evt = new TouchEvent();
            evt.set(TouchEvent.Type.KEY_DOWN);
            evt.setKeyCode(keyCode);
            evt.setCharacters(event.getCharacters());
            evt.setTime(event.getEventTime());

            // Send the event
            androidInput.addEvent(evt);

        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            evt = new TouchEvent();
            evt.set(TouchEvent.Type.KEY_UP);
            evt.setKeyCode(keyCode);
            evt.setCharacters(event.getCharacters());
            evt.setTime(event.getEventTime());

            // Send the event
            androidInput.addEvent(evt);

        }
        
        
        KeyInputEvent kie;
        char unicodeChar = (char)event.getUnicodeChar();
        int jmeKeyCode = AndroidKeyMapping.getJmeKey(keyCode);
        
        boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean repeating = pressed && event.getRepeatCount() > 0;

        kie = new KeyInputEvent(jmeKeyCode, unicodeChar, pressed, repeating);
        kie.setTime(event.getEventTime());
        androidInput.addEvent(kie);
//        logger.log(Level.FINE, "onKey keyCode: {0}, jmeKeyCode: {1}, pressed: {2}, repeating: {3}", 
//                new Object[]{keyCode, jmeKeyCode, pressed, repeating});
//        logger.log(Level.FINE, "creating KeyInputEvent: {0}", kie);
        
        // consume all keys ourself except Volume Up/Down and Menu
        //   Don't do Menu so that typical Android Menus can be created and used
        //   by the user in MainActivity
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || 
                (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) || 
                (keyCode == KeyEvent.KEYCODE_MENU)) {
            return false;
        } else {
            return true;
        }
   }
    
    public void showVirtualKeyboard (final boolean visible) {
        androidInput.getView().getHandler().post(new Runnable() {

            public void run() {
                InputMethodManager manager = 
                        (InputMethodManager)androidInput.getView().getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (visible) {
                    manager.showSoftInput(androidInput.getView(), 0);
                    sendKeyEvents = true;
                } else {
                    manager.hideSoftInputFromWindow(androidInput.getView().getWindowToken(), 0);
                    sendKeyEvents = false;
                }
            }
        });
    }
    
}
