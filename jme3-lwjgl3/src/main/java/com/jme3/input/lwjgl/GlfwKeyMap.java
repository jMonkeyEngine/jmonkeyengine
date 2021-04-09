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

import static org.lwjgl.glfw.GLFW.*;
import static com.jme3.input.KeyInput.*;

public class GlfwKeyMap {

    private static final int[] GLFW_TO_JME_KEY_MAP = new int[GLFW_KEY_LAST + 1];

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private GlfwKeyMap() {
    }

    private static void reg(final int jmeKey, final int glfwKey) {
        GLFW_TO_JME_KEY_MAP[glfwKey] = jmeKey;
    }

    static {
        reg(KEY_ESCAPE, GLFW_KEY_ESCAPE);
        reg(KEY_1, GLFW_KEY_1);
        reg(KEY_2, GLFW_KEY_2);
        reg(KEY_3, GLFW_KEY_3);
        reg(KEY_4, GLFW_KEY_4);
        reg(KEY_5, GLFW_KEY_5);
        reg(KEY_6, GLFW_KEY_6);
        reg(KEY_7, GLFW_KEY_7);
        reg(KEY_8, GLFW_KEY_8);
        reg(KEY_9, GLFW_KEY_9);
        reg(KEY_0, GLFW_KEY_0);
        reg(KEY_MINUS, GLFW_KEY_MINUS);
        reg(KEY_EQUALS, GLFW_KEY_EQUAL);
        reg(KEY_BACK, GLFW_KEY_BACKSPACE);
        reg(KEY_TAB, GLFW_KEY_TAB);
        reg(KEY_Q, GLFW_KEY_Q);
        reg(KEY_W, GLFW_KEY_W);
        reg(KEY_E, GLFW_KEY_E);
        reg(KEY_R, GLFW_KEY_R);
        reg(KEY_T, GLFW_KEY_T);
        reg(KEY_Y, GLFW_KEY_Y);
        reg(KEY_U, GLFW_KEY_U);
        reg(KEY_I, GLFW_KEY_I);
        reg(KEY_O, GLFW_KEY_O);
        reg(KEY_P, GLFW_KEY_P);
        reg(KEY_LBRACKET, GLFW_KEY_LEFT_BRACKET);
        reg(KEY_RBRACKET, GLFW_KEY_RIGHT_BRACKET);
        reg(KEY_RETURN, GLFW_KEY_ENTER);
        reg(KEY_LCONTROL, GLFW_KEY_LEFT_CONTROL);
        reg(KEY_A, GLFW_KEY_A);
        reg(KEY_S, GLFW_KEY_S);
        reg(KEY_D, GLFW_KEY_D);
        reg(KEY_F, GLFW_KEY_F);
        reg(KEY_G, GLFW_KEY_G);
        reg(KEY_H, GLFW_KEY_H);
        reg(KEY_J, GLFW_KEY_J);
        reg(KEY_K, GLFW_KEY_K);
        reg(KEY_L, GLFW_KEY_L);
        reg(KEY_SEMICOLON, GLFW_KEY_SEMICOLON);
        reg(KEY_APOSTROPHE, GLFW_KEY_APOSTROPHE);
        reg(KEY_GRAVE, GLFW_KEY_GRAVE_ACCENT);
        reg(KEY_LSHIFT, GLFW_KEY_LEFT_SHIFT);
        reg(KEY_BACKSLASH, GLFW_KEY_BACKSLASH);
        reg(KEY_Z, GLFW_KEY_Z);
        reg(KEY_X, GLFW_KEY_X);
        reg(KEY_C, GLFW_KEY_C);
        reg(KEY_V, GLFW_KEY_V);
        reg(KEY_B, GLFW_KEY_B);
        reg(KEY_N, GLFW_KEY_N);
        reg(KEY_M, GLFW_KEY_M);
        reg(KEY_COMMA, GLFW_KEY_COMMA);
        reg(KEY_PERIOD, GLFW_KEY_PERIOD);
        reg(KEY_SLASH, GLFW_KEY_SLASH);
        reg(KEY_RSHIFT, GLFW_KEY_RIGHT_SHIFT);
        reg(KEY_MULTIPLY, GLFW_KEY_KP_MULTIPLY);
        reg(KEY_LMENU, GLFW_KEY_LEFT_ALT);
        reg(KEY_SPACE, GLFW_KEY_SPACE);
        reg(KEY_CAPITAL, GLFW_KEY_CAPS_LOCK);
        reg(KEY_F1, GLFW_KEY_F1);
        reg(KEY_F2, GLFW_KEY_F2);
        reg(KEY_F3, GLFW_KEY_F3);
        reg(KEY_F4, GLFW_KEY_F4);
        reg(KEY_F5, GLFW_KEY_F5);
        reg(KEY_F6, GLFW_KEY_F6);
        reg(KEY_F7, GLFW_KEY_F7);
        reg(KEY_F8, GLFW_KEY_F8);
        reg(KEY_F9, GLFW_KEY_F9);
        reg(KEY_F10, GLFW_KEY_F10);
        reg(KEY_NUMLOCK, GLFW_KEY_NUM_LOCK);
        reg(KEY_SCROLL, GLFW_KEY_SCROLL_LOCK);
        reg(KEY_NUMPAD7, GLFW_KEY_KP_7);
        reg(KEY_NUMPAD8, GLFW_KEY_KP_8);
        reg(KEY_NUMPAD9, GLFW_KEY_KP_9);
        reg(KEY_SUBTRACT, GLFW_KEY_KP_SUBTRACT);
        reg(KEY_NUMPAD4, GLFW_KEY_KP_4);
        reg(KEY_NUMPAD5, GLFW_KEY_KP_5);
        reg(KEY_NUMPAD6, GLFW_KEY_KP_6);
        reg(KEY_ADD, GLFW_KEY_KP_ADD);
        reg(KEY_NUMPAD1, GLFW_KEY_KP_1);
        reg(KEY_NUMPAD2, GLFW_KEY_KP_2);
        reg(KEY_NUMPAD3, GLFW_KEY_KP_3);
        reg(KEY_NUMPAD0, GLFW_KEY_KP_0);
        reg(KEY_DECIMAL, GLFW_KEY_KP_DECIMAL);
        reg(KEY_F11, GLFW_KEY_F11);
        reg(KEY_F12, GLFW_KEY_F12);
        reg(KEY_F13, GLFW_KEY_F13);
        reg(KEY_F14, GLFW_KEY_F14);
        reg(KEY_F15, GLFW_KEY_F15);
        //reg(KEY_KANA, GLFW_KEY_);
        //reg(KEY_CONVERT, GLFW_KEY_);
        //reg(KEY_NOCONVERT, GLFW_KEY_);
        //reg(KEY_YEN, GLFW_KEY_);
        //reg(KEY_NUMPADEQUALS, GLFW_KEY_);
        //reg(KEY_CIRCUMFLEX, GLFW_KEY_);
        //reg(KEY_AT, GLFW_KEY_);
        //reg(KEY_COLON, GLFW_KEY_);
        //reg(KEY_UNDERLINE, GLFW_KEY_);
        //reg(KEY_KANJI, GLFW_KEY_);
        //reg(KEY_STOP, GLFW_KEY_);
        //reg(KEY_AX, GLFW_KEY_);
        //reg(KEY_UNLABELED, GLFW_KEY_);
        reg(KEY_NUMPADENTER, GLFW_KEY_KP_ENTER);
        reg(KEY_RCONTROL, GLFW_KEY_RIGHT_CONTROL);
        //reg(KEY_NUMPADCOMMA, GLFW_KEY_);
        reg(KEY_DIVIDE, GLFW_KEY_KP_DIVIDE);
        reg(KEY_SYSRQ, GLFW_KEY_PRINT_SCREEN);
        reg(KEY_RMENU, GLFW_KEY_RIGHT_ALT);
        reg(KEY_PAUSE, GLFW_KEY_PAUSE);
        reg(KEY_HOME, GLFW_KEY_HOME);
        reg(KEY_UP, GLFW_KEY_UP);
        reg(KEY_PRIOR, GLFW_KEY_PAGE_UP);
        reg(KEY_LEFT, GLFW_KEY_LEFT);
        reg(KEY_RIGHT, GLFW_KEY_RIGHT);
        reg(KEY_END, GLFW_KEY_END);
        reg(KEY_DOWN, GLFW_KEY_DOWN);
        reg(KEY_NEXT, GLFW_KEY_PAGE_DOWN);
        reg(KEY_INSERT, GLFW_KEY_INSERT);
        reg(KEY_DELETE, GLFW_KEY_DELETE);
        reg(KEY_LMETA, GLFW_KEY_LEFT_SUPER);
        reg(KEY_RMETA, GLFW_KEY_RIGHT_SUPER);
    }

    /**
     * Returns the jme keycode that matches the specified glfw keycode
     * @param glfwKey the glfw keycode
     */
    public static int toJmeKeyCode(final int glfwKey) {
        return GLFW_TO_JME_KEY_MAP[glfwKey];
    }


    /**
     * Returns the glfw keycode that matches the specified jme keycode or
     * GLFW_KEY_UNKNOWN if there isn't any match.
     * 
     * @param jmeKey the jme keycode
     */
    public static int fromJmeKeyCode(final int jmeKey) {
        for (int i = 0; i < GLFW_TO_JME_KEY_MAP.length; i++) {
            if (GLFW_TO_JME_KEY_MAP[i] == jmeKey) return i;
        }
        return GLFW_KEY_UNKNOWN;
    }
    
}
