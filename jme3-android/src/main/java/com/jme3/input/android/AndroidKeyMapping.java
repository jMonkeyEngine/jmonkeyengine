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

package com.jme3.input.android;

import com.jme3.input.KeyInput;
import java.util.logging.Logger;

/**
 * AndroidKeyMapping is just a utility to convert the Android keyCodes into
 * jME KeyCodes so that events received in jME's KeyEvent will match between
 * Desktop and Android.
 *
 * @author iwgeric
 */
public class AndroidKeyMapping {
    private static final Logger logger = Logger.getLogger(AndroidKeyMapping.class.getName());

    private static final int[] ANDROID_TO_JME = {
        0x0, // unknown
        0x0, // key code soft left
        0x0, // key code soft right
        KeyInput.KEY_HOME,
        KeyInput.KEY_ESCAPE, // key back
        0x0, // key call
        0x0, // key endcall
        KeyInput.KEY_0,
        KeyInput.KEY_1,
        KeyInput.KEY_2,
        KeyInput.KEY_3,
        KeyInput.KEY_4,
        KeyInput.KEY_5,
        KeyInput.KEY_6,
        KeyInput.KEY_7,
        KeyInput.KEY_8,
        KeyInput.KEY_9,
        KeyInput.KEY_MULTIPLY,
        0x0, // key pound
        KeyInput.KEY_UP,
        KeyInput.KEY_DOWN,
        KeyInput.KEY_LEFT,
        KeyInput.KEY_RIGHT,
        KeyInput.KEY_RETURN, // dpad center
        0x0, // volume up
        0x0, // volume down
        KeyInput.KEY_POWER, // power (?)
        0x0, // camera
        0x0, // clear
        KeyInput.KEY_A,
        KeyInput.KEY_B,
        KeyInput.KEY_C,
        KeyInput.KEY_D,
        KeyInput.KEY_E,
        KeyInput.KEY_F,
        KeyInput.KEY_G,
        KeyInput.KEY_H,
        KeyInput.KEY_I,
        KeyInput.KEY_J,
        KeyInput.KEY_K,
        KeyInput.KEY_L,
        KeyInput.KEY_M,
        KeyInput.KEY_N,
        KeyInput.KEY_O,
        KeyInput.KEY_P,
        KeyInput.KEY_Q,
        KeyInput.KEY_R,
        KeyInput.KEY_S,
        KeyInput.KEY_T,
        KeyInput.KEY_U,
        KeyInput.KEY_V,
        KeyInput.KEY_W,
        KeyInput.KEY_X,
        KeyInput.KEY_Y,
        KeyInput.KEY_Z,
        KeyInput.KEY_COMMA,
        KeyInput.KEY_PERIOD,
        KeyInput.KEY_LMENU,
        KeyInput.KEY_RMENU,
        KeyInput.KEY_LSHIFT,
        KeyInput.KEY_RSHIFT,
        //        0x0, // fn
        //        0x0, // cap (?)

        KeyInput.KEY_TAB,
        KeyInput.KEY_SPACE,
        0x0, // sym (?) symbol
        0x0, // explorer
        0x0, // envelope
        KeyInput.KEY_RETURN, // newline/enter
        KeyInput.KEY_BACK, //used to be KeyInput.KEY_DELETE,
        KeyInput.KEY_GRAVE,
        KeyInput.KEY_MINUS,
        KeyInput.KEY_EQUALS,
        KeyInput.KEY_LBRACKET,
        KeyInput.KEY_RBRACKET,
        KeyInput.KEY_BACKSLASH,
        KeyInput.KEY_SEMICOLON,
        KeyInput.KEY_APOSTROPHE,
        KeyInput.KEY_SLASH,
        KeyInput.KEY_AT, // at (@)
        KeyInput.KEY_NUMLOCK, //0x0, // num
        0x0, //headset hook
        0x0, //focus
        KeyInput.KEY_ADD,
        KeyInput.KEY_LMETA, //menu
        0x0,//notification
        0x0,//search
        0x0,//media play/pause
        0x0,//media stop
        0x0,//media next
        0x0,//media previous
        0x0,//media rewind
        0x0,//media fastforward
        0x0,//mute
    };

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private AndroidKeyMapping() {
    }

    public static int getJmeKey(int androidKey) {
        if (androidKey > ANDROID_TO_JME.length) {
            return androidKey;
        } else {
            return ANDROID_TO_JME[androidKey];
        }
    }

}
