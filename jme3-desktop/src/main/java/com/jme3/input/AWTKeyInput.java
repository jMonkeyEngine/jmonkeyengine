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
package com.jme3.input;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.system.AWTContext;


/**
 * The implementation of the {@link KeyInput} dedicated to AWT {@link Component component}.
 * <p>
 * This class is based on the <a href="http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html">JavaFX</a> original code provided by Alexander Brui (see <a href="https://github.com/JavaSaBr/JME3-JFX">JME3-FX</a>)
 * </p>
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Alexander Brui (JavaSaBr)
 */
public class AWTKeyInput extends AWTInput implements KeyInput, KeyListener{

    private static final Map<Integer, Integer> KEY_CODE_TO_JME = new HashMap<>();

    static {
        KEY_CODE_TO_JME.put(KeyEvent.VK_ESCAPE, KEY_ESCAPE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_0, KEY_0);
        KEY_CODE_TO_JME.put(KeyEvent.VK_1, KEY_1);
        KEY_CODE_TO_JME.put(KeyEvent.VK_2, KEY_2);
        KEY_CODE_TO_JME.put(KeyEvent.VK_3, KEY_3);
        KEY_CODE_TO_JME.put(KeyEvent.VK_4, KEY_4);
        KEY_CODE_TO_JME.put(KeyEvent.VK_5, KEY_5);
        KEY_CODE_TO_JME.put(KeyEvent.VK_6, KEY_6);
        KEY_CODE_TO_JME.put(KeyEvent.VK_7, KEY_7);
        KEY_CODE_TO_JME.put(KeyEvent.VK_8, KEY_8);
        KEY_CODE_TO_JME.put(KeyEvent.VK_9, KEY_9);
        KEY_CODE_TO_JME.put(KeyEvent.VK_MINUS, KEY_MINUS);
        KEY_CODE_TO_JME.put(KeyEvent.VK_EQUALS, KEY_EQUALS);
        KEY_CODE_TO_JME.put(KeyEvent.VK_BACK_SPACE, KEY_BACK);
        KEY_CODE_TO_JME.put(KeyEvent.VK_TAB, KEY_TAB);
        KEY_CODE_TO_JME.put(KeyEvent.VK_Q, KEY_Q);
        KEY_CODE_TO_JME.put(KeyEvent.VK_W, KEY_W);
        KEY_CODE_TO_JME.put(KeyEvent.VK_E, KEY_E);
        KEY_CODE_TO_JME.put(KeyEvent.VK_R, KEY_R);
        KEY_CODE_TO_JME.put(KeyEvent.VK_T, KEY_T);
        KEY_CODE_TO_JME.put(KeyEvent.VK_U, KEY_U);
        KEY_CODE_TO_JME.put(KeyEvent.VK_I, KEY_I);
        KEY_CODE_TO_JME.put(KeyEvent.VK_O, KEY_O);
        KEY_CODE_TO_JME.put(KeyEvent.VK_P, KEY_P);
        KEY_CODE_TO_JME.put(KeyEvent.VK_OPEN_BRACKET, KEY_LBRACKET);
        KEY_CODE_TO_JME.put(KeyEvent.VK_CLOSE_BRACKET, KEY_RBRACKET);
        KEY_CODE_TO_JME.put(KeyEvent.VK_ENTER, KEY_RETURN);
        KEY_CODE_TO_JME.put(KeyEvent.VK_CONTROL, KEY_LCONTROL);
        KEY_CODE_TO_JME.put(KeyEvent.VK_A, KEY_A);
        KEY_CODE_TO_JME.put(KeyEvent.VK_S, KEY_S);
        KEY_CODE_TO_JME.put(KeyEvent.VK_D, KEY_D);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F, KEY_F);
        KEY_CODE_TO_JME.put(KeyEvent.VK_G, KEY_G);
        KEY_CODE_TO_JME.put(KeyEvent.VK_H, KEY_H);
        KEY_CODE_TO_JME.put(KeyEvent.VK_J, KEY_J);
        KEY_CODE_TO_JME.put(KeyEvent.VK_Y, KEY_Y);
        KEY_CODE_TO_JME.put(KeyEvent.VK_K, KEY_K);
        KEY_CODE_TO_JME.put(KeyEvent.VK_L, KEY_L);
        KEY_CODE_TO_JME.put(KeyEvent.VK_SEMICOLON, KEY_SEMICOLON);
        KEY_CODE_TO_JME.put(KeyEvent.VK_QUOTE, KEY_APOSTROPHE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_DEAD_GRAVE, KEY_GRAVE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_SHIFT, KEY_LSHIFT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_BACK_SLASH, KEY_BACKSLASH);
        KEY_CODE_TO_JME.put(KeyEvent.VK_Z, KEY_Z);
        KEY_CODE_TO_JME.put(KeyEvent.VK_X, KEY_X);
        KEY_CODE_TO_JME.put(KeyEvent.VK_C, KEY_C);
        KEY_CODE_TO_JME.put(KeyEvent.VK_V, KEY_V);
        KEY_CODE_TO_JME.put(KeyEvent.VK_B, KEY_B);
        KEY_CODE_TO_JME.put(KeyEvent.VK_N, KEY_N);
        KEY_CODE_TO_JME.put(KeyEvent.VK_M, KEY_M);
        KEY_CODE_TO_JME.put(KeyEvent.VK_COMMA, KEY_COMMA);
        KEY_CODE_TO_JME.put(KeyEvent.VK_PERIOD, KEY_PERIOD);
        KEY_CODE_TO_JME.put(KeyEvent.VK_SLASH, KEY_SLASH);
        KEY_CODE_TO_JME.put(KeyEvent.VK_MULTIPLY, KEY_MULTIPLY);
        KEY_CODE_TO_JME.put(KeyEvent.VK_SPACE, KEY_SPACE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_CAPS_LOCK, KEY_CAPITAL);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F1, KEY_F1);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F2, KEY_F2);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F3, KEY_F3);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F4, KEY_F4);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F5, KEY_F5);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F6, KEY_F6);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F7, KEY_F7);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F8, KEY_F8);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F9, KEY_F9);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F10, KEY_F10);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUM_LOCK, KEY_NUMLOCK);
        KEY_CODE_TO_JME.put(KeyEvent.VK_SCROLL_LOCK, KEY_SCROLL);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD7, KEY_NUMPAD7);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD8, KEY_NUMPAD8);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD9, KEY_NUMPAD9);
        KEY_CODE_TO_JME.put(KeyEvent.VK_SUBTRACT, KEY_SUBTRACT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD4, KEY_NUMPAD4);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD5, KEY_NUMPAD5);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD6, KEY_NUMPAD6);
        KEY_CODE_TO_JME.put(KeyEvent.VK_ADD, KEY_ADD);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD1, KEY_NUMPAD1);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD2, KEY_NUMPAD2);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD3, KEY_NUMPAD3);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NUMPAD0, KEY_NUMPAD0);
        KEY_CODE_TO_JME.put(KeyEvent.VK_DECIMAL, KEY_DECIMAL);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F11, KEY_F11);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F12, KEY_F12);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F13, KEY_F13);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F14, KEY_F14);
        KEY_CODE_TO_JME.put(KeyEvent.VK_F15, KEY_F15);
        KEY_CODE_TO_JME.put(KeyEvent.VK_KANA, KEY_KANA);
        KEY_CODE_TO_JME.put(KeyEvent.VK_CONVERT, KEY_CONVERT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_NONCONVERT, KEY_NOCONVERT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_CIRCUMFLEX, KEY_CIRCUMFLEX);
        KEY_CODE_TO_JME.put(KeyEvent.VK_AT, KEY_AT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_COLON, KEY_COLON);
        KEY_CODE_TO_JME.put(KeyEvent.VK_UNDERSCORE, KEY_UNDERLINE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_STOP, KEY_STOP);
        KEY_CODE_TO_JME.put(KeyEvent.VK_DIVIDE, KEY_DIVIDE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_PAUSE, KEY_PAUSE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_HOME, KEY_HOME);
        KEY_CODE_TO_JME.put(KeyEvent.VK_UP, KEY_UP);
        KEY_CODE_TO_JME.put(KeyEvent.VK_PAGE_UP, KEY_PRIOR);
        KEY_CODE_TO_JME.put(KeyEvent.VK_LEFT, KEY_LEFT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_RIGHT, KEY_RIGHT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_END, KEY_END);
        KEY_CODE_TO_JME.put(KeyEvent.VK_DOWN, KEY_DOWN);
        KEY_CODE_TO_JME.put(KeyEvent.VK_PAGE_DOWN, KEY_NEXT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_INSERT, KEY_INSERT);
        KEY_CODE_TO_JME.put(KeyEvent.VK_DELETE, KEY_DELETE);
        KEY_CODE_TO_JME.put(KeyEvent.VK_ALT, KEY_LMENU);
        KEY_CODE_TO_JME.put(KeyEvent.VK_META, KEY_RCONTROL);
    }

    private final LinkedList<KeyInputEvent> keyInputEvents;

    public AWTKeyInput(AWTContext context) {
        super(context);
        keyInputEvents = new LinkedList<KeyInputEvent>();
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.addKeyListener(this);
    }

    @Override
    public void unbind() {
        if (component != null) {
            component.removeKeyListener(this);
        }
        super.unbind();
    }

    private void onKeyEvent(KeyEvent keyEvent, boolean pressed) {

        int code = convertKeyCode(keyEvent.getID());
        char keyChar = keyEvent.getKeyChar();

        final KeyInputEvent event = new KeyInputEvent(code, keyChar, pressed, false);
        event.setTime(getInputTimeNanos());

        EXECUTOR.addToExecute(new Runnable() {

          @Override
          public void run() {
            keyInputEvents.add(event);
          }
          
        });
    }

    @Override
    protected void updateImpl() {
        while (!keyInputEvents.isEmpty()) {
            listener.onKeyEvent(keyInputEvents.poll());
        }
    }

    private int convertKeyCode(int keyCode) {
        final Integer code = KEY_CODE_TO_JME.get(keyCode);
        return code == null ? KEY_UNKNOWN : code;
    }

    @Override
    public void keyTyped(KeyEvent e) {
      System.out.println("Key typed "+e.getKeyChar());
      //onKeyEvent(e, false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
      System.out.println("Key pressed "+e.getKeyChar());
      onKeyEvent(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
      System.out.println("Key released "+e.getKeyChar());
      onKeyEvent(e, false);
    }

    @Override
    public String getKeyName(int key){
      throw new UnsupportedOperationException("getKeyName is not implemented in AWTKeyInput");
    }
}
