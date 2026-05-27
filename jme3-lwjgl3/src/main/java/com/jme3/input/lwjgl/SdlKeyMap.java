/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

import static com.jme3.input.KeyInput.*;
import static org.lwjgl.sdl.SDLScancode.*;

/**
 * Maps SDL scancodes to jME key codes.
 */
public final class SdlKeyMap {

    private static final int[] SDL_TO_JME_KEY_MAP = new int[SDL_SCANCODE_COUNT];

    private SdlKeyMap() {
    }

    private static void reg(final int jmeKey, final int sdlScancode) {
        SDL_TO_JME_KEY_MAP[sdlScancode] = jmeKey;
    }

    static {
        reg(KEY_ESCAPE, SDL_SCANCODE_ESCAPE);
        reg(KEY_1, SDL_SCANCODE_1);
        reg(KEY_2, SDL_SCANCODE_2);
        reg(KEY_3, SDL_SCANCODE_3);
        reg(KEY_4, SDL_SCANCODE_4);
        reg(KEY_5, SDL_SCANCODE_5);
        reg(KEY_6, SDL_SCANCODE_6);
        reg(KEY_7, SDL_SCANCODE_7);
        reg(KEY_8, SDL_SCANCODE_8);
        reg(KEY_9, SDL_SCANCODE_9);
        reg(KEY_0, SDL_SCANCODE_0);
        reg(KEY_MINUS, SDL_SCANCODE_MINUS);
        reg(KEY_EQUALS, SDL_SCANCODE_EQUALS);
        reg(KEY_BACK, SDL_SCANCODE_BACKSPACE);
        reg(KEY_TAB, SDL_SCANCODE_TAB);
        reg(KEY_Q, SDL_SCANCODE_Q);
        reg(KEY_W, SDL_SCANCODE_W);
        reg(KEY_E, SDL_SCANCODE_E);
        reg(KEY_R, SDL_SCANCODE_R);
        reg(KEY_T, SDL_SCANCODE_T);
        reg(KEY_Y, SDL_SCANCODE_Y);
        reg(KEY_U, SDL_SCANCODE_U);
        reg(KEY_I, SDL_SCANCODE_I);
        reg(KEY_O, SDL_SCANCODE_O);
        reg(KEY_P, SDL_SCANCODE_P);
        reg(KEY_LBRACKET, SDL_SCANCODE_LEFTBRACKET);
        reg(KEY_RBRACKET, SDL_SCANCODE_RIGHTBRACKET);
        reg(KEY_RETURN, SDL_SCANCODE_RETURN);
        reg(KEY_LCONTROL, SDL_SCANCODE_LCTRL);
        reg(KEY_A, SDL_SCANCODE_A);
        reg(KEY_S, SDL_SCANCODE_S);
        reg(KEY_D, SDL_SCANCODE_D);
        reg(KEY_F, SDL_SCANCODE_F);
        reg(KEY_G, SDL_SCANCODE_G);
        reg(KEY_H, SDL_SCANCODE_H);
        reg(KEY_J, SDL_SCANCODE_J);
        reg(KEY_K, SDL_SCANCODE_K);
        reg(KEY_L, SDL_SCANCODE_L);
        reg(KEY_SEMICOLON, SDL_SCANCODE_SEMICOLON);
        reg(KEY_APOSTROPHE, SDL_SCANCODE_APOSTROPHE);
        reg(KEY_GRAVE, SDL_SCANCODE_GRAVE);
        reg(KEY_LSHIFT, SDL_SCANCODE_LSHIFT);
        reg(KEY_BACKSLASH, SDL_SCANCODE_BACKSLASH);
        reg(KEY_Z, SDL_SCANCODE_Z);
        reg(KEY_X, SDL_SCANCODE_X);
        reg(KEY_C, SDL_SCANCODE_C);
        reg(KEY_V, SDL_SCANCODE_V);
        reg(KEY_B, SDL_SCANCODE_B);
        reg(KEY_N, SDL_SCANCODE_N);
        reg(KEY_M, SDL_SCANCODE_M);
        reg(KEY_COMMA, SDL_SCANCODE_COMMA);
        reg(KEY_PERIOD, SDL_SCANCODE_PERIOD);
        reg(KEY_SLASH, SDL_SCANCODE_SLASH);
        reg(KEY_RSHIFT, SDL_SCANCODE_RSHIFT);
        reg(KEY_MULTIPLY, SDL_SCANCODE_KP_MULTIPLY);
        reg(KEY_LMENU, SDL_SCANCODE_LALT);
        reg(KEY_SPACE, SDL_SCANCODE_SPACE);
        reg(KEY_CAPITAL, SDL_SCANCODE_CAPSLOCK);
        reg(KEY_F1, SDL_SCANCODE_F1);
        reg(KEY_F2, SDL_SCANCODE_F2);
        reg(KEY_F3, SDL_SCANCODE_F3);
        reg(KEY_F4, SDL_SCANCODE_F4);
        reg(KEY_F5, SDL_SCANCODE_F5);
        reg(KEY_F6, SDL_SCANCODE_F6);
        reg(KEY_F7, SDL_SCANCODE_F7);
        reg(KEY_F8, SDL_SCANCODE_F8);
        reg(KEY_F9, SDL_SCANCODE_F9);
        reg(KEY_F10, SDL_SCANCODE_F10);
        reg(KEY_NUMLOCK, SDL_SCANCODE_NUMLOCKCLEAR);
        reg(KEY_SCROLL, SDL_SCANCODE_SCROLLLOCK);
        reg(KEY_NUMPAD7, SDL_SCANCODE_KP_7);
        reg(KEY_NUMPAD8, SDL_SCANCODE_KP_8);
        reg(KEY_NUMPAD9, SDL_SCANCODE_KP_9);
        reg(KEY_SUBTRACT, SDL_SCANCODE_KP_MINUS);
        reg(KEY_NUMPAD4, SDL_SCANCODE_KP_4);
        reg(KEY_NUMPAD5, SDL_SCANCODE_KP_5);
        reg(KEY_NUMPAD6, SDL_SCANCODE_KP_6);
        reg(KEY_ADD, SDL_SCANCODE_KP_PLUS);
        reg(KEY_NUMPAD1, SDL_SCANCODE_KP_1);
        reg(KEY_NUMPAD2, SDL_SCANCODE_KP_2);
        reg(KEY_NUMPAD3, SDL_SCANCODE_KP_3);
        reg(KEY_NUMPAD0, SDL_SCANCODE_KP_0);
        reg(KEY_DECIMAL, SDL_SCANCODE_KP_PERIOD);
        reg(KEY_F11, SDL_SCANCODE_F11);
        reg(KEY_F12, SDL_SCANCODE_F12);
        reg(KEY_F13, SDL_SCANCODE_F13);
        reg(KEY_F14, SDL_SCANCODE_F14);
        reg(KEY_F15, SDL_SCANCODE_F15);
        reg(KEY_NUMPADENTER, SDL_SCANCODE_KP_ENTER);
        reg(KEY_RCONTROL, SDL_SCANCODE_RCTRL);
        reg(KEY_DIVIDE, SDL_SCANCODE_KP_DIVIDE);
        reg(KEY_SYSRQ, SDL_SCANCODE_PRINTSCREEN);
        reg(KEY_RMENU, SDL_SCANCODE_RALT);
        reg(KEY_PAUSE, SDL_SCANCODE_PAUSE);
        reg(KEY_HOME, SDL_SCANCODE_HOME);
        reg(KEY_UP, SDL_SCANCODE_UP);
        reg(KEY_PRIOR, SDL_SCANCODE_PAGEUP);
        reg(KEY_LEFT, SDL_SCANCODE_LEFT);
        reg(KEY_RIGHT, SDL_SCANCODE_RIGHT);
        reg(KEY_END, SDL_SCANCODE_END);
        reg(KEY_DOWN, SDL_SCANCODE_DOWN);
        reg(KEY_NEXT, SDL_SCANCODE_PAGEDOWN);
        reg(KEY_INSERT, SDL_SCANCODE_INSERT);
        reg(KEY_DELETE, SDL_SCANCODE_DELETE);
        reg(KEY_LMETA, SDL_SCANCODE_LGUI);
        reg(KEY_RMETA, SDL_SCANCODE_RGUI);
    }

    public static int toJmeKeyCode(final int sdlScancode) {
        if (sdlScancode < 0 || sdlScancode >= SDL_TO_JME_KEY_MAP.length) {
            return KEY_UNKNOWN;
        }
        return SDL_TO_JME_KEY_MAP[sdlScancode];
    }

    public static int fromJmeKeyCode(final int jmeKey) {
        for (int i = 0; i < SDL_TO_JME_KEY_MAP.length; i++) {
            if (SDL_TO_JME_KEY_MAP[i] == jmeKey) {
                return i;
            }
        }
        return SDL_SCANCODE_UNKNOWN;
    }
}
