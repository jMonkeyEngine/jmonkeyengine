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

import static com.jme3.input.KeyInput.*;

public class KeyNames {

    private static final String[] KEY_NAMES = new String[0xFF];
    private static final String[] KEY_VARNAMES = new String[0xFF];

    static {
        KEY_NAMES[KEY_UNKNOWN] = "Unknown";
        KEY_NAMES[KEY_0] = "0";
        KEY_NAMES[KEY_1] = "1";
        KEY_NAMES[KEY_2] = "2";
        KEY_NAMES[KEY_3] = "3";
        KEY_NAMES[KEY_4] = "4";
        KEY_NAMES[KEY_5] = "5";
        KEY_NAMES[KEY_6] = "6";
        KEY_NAMES[KEY_7] = "7";
        KEY_NAMES[KEY_8] = "8";
        KEY_NAMES[KEY_9] = "9";

        KEY_NAMES[KEY_Q] = "Q";
        KEY_NAMES[KEY_W] = "W";
        KEY_NAMES[KEY_E] = "E";
        KEY_NAMES[KEY_R] = "R";
        KEY_NAMES[KEY_T] = "T";
        KEY_NAMES[KEY_Y] = "Y";
        KEY_NAMES[KEY_U] = "U";
        KEY_NAMES[KEY_I] = "I";
        KEY_NAMES[KEY_O] = "O";
        KEY_NAMES[KEY_P] = "P";
        KEY_NAMES[KEY_A] = "A";
        KEY_NAMES[KEY_S] = "S";
        KEY_NAMES[KEY_D] = "D";
        KEY_NAMES[KEY_F] = "F";
        KEY_NAMES[KEY_G] = "G";
        KEY_NAMES[KEY_H] = "H";
        KEY_NAMES[KEY_J] = "J";
        KEY_NAMES[KEY_K] = "K";
        KEY_NAMES[KEY_L] = "L";
        KEY_NAMES[KEY_Z] = "Z";
        KEY_NAMES[KEY_X] = "X";
        KEY_NAMES[KEY_C] = "C";
        KEY_NAMES[KEY_V] = "V";
        KEY_NAMES[KEY_B] = "B";
        KEY_NAMES[KEY_N] = "N";
        KEY_NAMES[KEY_M] = "M";

        KEY_NAMES[KEY_F1] = "F1";
        KEY_NAMES[KEY_F2] = "F2";
        KEY_NAMES[KEY_F3] = "F3";
        KEY_NAMES[KEY_F4] = "F4";
        KEY_NAMES[KEY_F5] = "F5";
        KEY_NAMES[KEY_F6] = "F6";
        KEY_NAMES[KEY_F7] = "F7";
        KEY_NAMES[KEY_F8] = "F8";
        KEY_NAMES[KEY_F9] = "F9";
        KEY_NAMES[KEY_F10] = "F10";
        KEY_NAMES[KEY_F11] = "F11";
        KEY_NAMES[KEY_F12] = "F12";
        KEY_NAMES[KEY_F13] = "F13";
        KEY_NAMES[KEY_F14] = "F14";
        KEY_NAMES[KEY_F15] = "F15";

        KEY_NAMES[KEY_NUMPAD0] = "Numpad 0";
        KEY_NAMES[KEY_NUMPAD1] = "Numpad 1";
        KEY_NAMES[KEY_NUMPAD2] = "Numpad 2";
        KEY_NAMES[KEY_NUMPAD3] = "Numpad 3";
        KEY_NAMES[KEY_NUMPAD4] = "Numpad 4";
        KEY_NAMES[KEY_NUMPAD5] = "Numpad 5";
        KEY_NAMES[KEY_NUMPAD6] = "Numpad 6";
        KEY_NAMES[KEY_NUMPAD7] = "Numpad 7";
        KEY_NAMES[KEY_NUMPAD8] = "Numpad 8";
        KEY_NAMES[KEY_NUMPAD9] = "Numpad 9";

        KEY_NAMES[KEY_NUMPADEQUALS] = "Numpad =";
        KEY_NAMES[KEY_NUMPADENTER] = "Numpad Enter";
        KEY_NAMES[KEY_NUMPADCOMMA] = "Numpad ,";
        KEY_NAMES[KEY_DIVIDE] = "Numpad /";
        KEY_NAMES[KEY_SUBTRACT] = "Numpad -";
        KEY_NAMES[KEY_DECIMAL] = "Numpad .";

        KEY_NAMES[KEY_LMENU] = "Left Alt";
        KEY_NAMES[KEY_RMENU] = "Right Alt";

        KEY_NAMES[KEY_LCONTROL] = "Left Ctrl";
        KEY_NAMES[KEY_RCONTROL] = "Right Ctrl";

        KEY_NAMES[KEY_LSHIFT] = "Left Shift";
        KEY_NAMES[KEY_RSHIFT] = "Right Shift";

        KEY_NAMES[KEY_LMETA] = "Left Option";
        KEY_NAMES[KEY_RMETA] = "Right Option";

        KEY_NAMES[KEY_MINUS] = "-";
        KEY_NAMES[KEY_EQUALS] = "=";
        KEY_NAMES[KEY_LBRACKET] = "[";
        KEY_NAMES[KEY_RBRACKET] = "]";
        KEY_NAMES[KEY_SEMICOLON] = ";";
        KEY_NAMES[KEY_APOSTROPHE] = "'";
        KEY_NAMES[KEY_GRAVE] = "`";
        KEY_NAMES[KEY_BACKSLASH] = "\\";
        KEY_NAMES[KEY_COMMA] = ",";
        KEY_NAMES[KEY_PERIOD] = ".";
        KEY_NAMES[KEY_SLASH] = "/";
        KEY_NAMES[KEY_MULTIPLY] = "*";
        KEY_NAMES[KEY_ADD] = "+";
        KEY_NAMES[KEY_COLON] = ":";
        KEY_NAMES[KEY_UNDERLINE] = "_";
        KEY_NAMES[KEY_AT] = "@";

        KEY_NAMES[KEY_APPS] = "Apps";
        KEY_NAMES[KEY_POWER] = "Power";
        KEY_NAMES[KEY_SLEEP] = "Sleep";

        KEY_NAMES[KEY_STOP] = "Stop";
        KEY_NAMES[KEY_ESCAPE] = "Esc";
        KEY_NAMES[KEY_RETURN] = "Enter";
        KEY_NAMES[KEY_SPACE] = "Space";
        KEY_NAMES[KEY_BACK] = "Backspace";
        KEY_NAMES[KEY_TAB] = "Tab";

        KEY_NAMES[KEY_SYSRQ] = "SysRq";
        KEY_NAMES[KEY_PAUSE] = "Pause";

        KEY_NAMES[KEY_HOME] = "Home";
        KEY_NAMES[KEY_PGUP] = "Page Up";
        KEY_NAMES[KEY_PGDN] = "Page Down";
        KEY_NAMES[KEY_END] = "End";
        KEY_NAMES[KEY_INSERT] = "Insert";
        KEY_NAMES[KEY_DELETE] = "Delete";

        KEY_NAMES[KEY_UP] = "Up";
        KEY_NAMES[KEY_LEFT] = "Left";
        KEY_NAMES[KEY_RIGHT] = "Right";
        KEY_NAMES[KEY_DOWN] = "Down";

        KEY_NAMES[KEY_NUMLOCK] = "Num Lock";
        KEY_NAMES[KEY_CAPITAL] = "Caps Lock";
        KEY_NAMES[KEY_SCROLL] = "Scroll Lock";

        KEY_NAMES[KEY_KANA] = "Kana";
        KEY_NAMES[KEY_CONVERT] = "Convert";
        KEY_NAMES[KEY_NOCONVERT] = "No Convert";
        KEY_NAMES[KEY_YEN] = "Yen";
        KEY_NAMES[KEY_CIRCUMFLEX] = "Circumflex";
        KEY_NAMES[KEY_KANJI] = "Kanji";
        KEY_NAMES[KEY_AX] = "Ax";
        KEY_NAMES[KEY_UNLABELED] = "Unlabeled";

        KEY_VARNAMES[KEY_UNKNOWN] = "KEY_UNKNOWN";
        KEY_VARNAMES[KEY_0] = "KEY_0";
        KEY_VARNAMES[KEY_1] = "KEY_1";
        KEY_VARNAMES[KEY_2] = "KEY_2";
        KEY_VARNAMES[KEY_3] = "KEY_3";
        KEY_VARNAMES[KEY_4] = "KEY_4";
        KEY_VARNAMES[KEY_5] = "KEY_5";
        KEY_VARNAMES[KEY_6] = "KEY_6";
        KEY_VARNAMES[KEY_7] = "KEY_7";
        KEY_VARNAMES[KEY_8] = "KEY_8";
        KEY_VARNAMES[KEY_9] = "KEY_9";

        KEY_VARNAMES[KEY_Q] = "KEY_Q";
        KEY_VARNAMES[KEY_W] = "KEY_W";
        KEY_VARNAMES[KEY_E] = "KEY_E";
        KEY_VARNAMES[KEY_R] = "KEY_R";
        KEY_VARNAMES[KEY_T] = "KEY_T";
        KEY_VARNAMES[KEY_Y] = "KEY_Y";
        KEY_VARNAMES[KEY_U] = "KEY_U";
        KEY_VARNAMES[KEY_I] = "KEY_I";
        KEY_VARNAMES[KEY_O] = "KEY_O";
        KEY_VARNAMES[KEY_P] = "KEY_P";
        KEY_VARNAMES[KEY_A] = "KEY_A";
        KEY_VARNAMES[KEY_S] = "KEY_S";
        KEY_VARNAMES[KEY_D] = "KEY_D";
        KEY_VARNAMES[KEY_F] = "KEY_F";
        KEY_VARNAMES[KEY_G] = "KEY_G";
        KEY_VARNAMES[KEY_H] = "KEY_H";
        KEY_VARNAMES[KEY_J] = "KEY_J";
        KEY_VARNAMES[KEY_K] = "KEY_K";
        KEY_VARNAMES[KEY_L] = "KEY_L";
        KEY_VARNAMES[KEY_Z] = "KEY_Z";
        KEY_VARNAMES[KEY_X] = "KEY_X";
        KEY_VARNAMES[KEY_C] = "KEY_C";
        KEY_VARNAMES[KEY_V] = "KEY_V";
        KEY_VARNAMES[KEY_B] = "KEY_B";
        KEY_VARNAMES[KEY_N] = "KEY_N";
        KEY_VARNAMES[KEY_M] = "KEY_M";

        KEY_VARNAMES[KEY_F1] = "KEY_F1";
        KEY_VARNAMES[KEY_F2] = "KEY_F2";
        KEY_VARNAMES[KEY_F3] = "KEY_F3";
        KEY_VARNAMES[KEY_F4] = "KEY_F4";
        KEY_VARNAMES[KEY_F5] = "KEY_F5";
        KEY_VARNAMES[KEY_F6] = "KEY_F6";
        KEY_VARNAMES[KEY_F7] = "KEY_F7";
        KEY_VARNAMES[KEY_F8] = "KEY_F8";
        KEY_VARNAMES[KEY_F9] = "KEY_F9";
        KEY_VARNAMES[KEY_F10] = "KEY_F10";
        KEY_VARNAMES[KEY_F11] = "KEY_F11";
        KEY_VARNAMES[KEY_F12] = "KEY_F12";
        KEY_VARNAMES[KEY_F13] = "KEY_F13";
        KEY_VARNAMES[KEY_F14] = "KEY_F14";
        KEY_VARNAMES[KEY_F15] = "KEY_F15";

        KEY_VARNAMES[KEY_NUMPAD0] = "KEY_NUMPAD0";
        KEY_VARNAMES[KEY_NUMPAD1] = "KEY_NUMPAD1";
        KEY_VARNAMES[KEY_NUMPAD2] = "KEY_NUMPAD2";
        KEY_VARNAMES[KEY_NUMPAD3] = "KEY_NUMPAD3";
        KEY_VARNAMES[KEY_NUMPAD4] = "KEY_NUMPAD4";
        KEY_VARNAMES[KEY_NUMPAD5] = "KEY_NUMPAD5";
        KEY_VARNAMES[KEY_NUMPAD6] = "KEY_NUMPAD6";
        KEY_VARNAMES[KEY_NUMPAD7] = "KEY_NUMPAD7";
        KEY_VARNAMES[KEY_NUMPAD8] = "KEY_NUMPAD8";
        KEY_VARNAMES[KEY_NUMPAD9] = "KEY_NUMPAD9";

        KEY_VARNAMES[KEY_NUMPADEQUALS] = "KEY_NUMPADEQUALS";
        KEY_VARNAMES[KEY_NUMPADENTER] = "KEY_NUMPADENTER";
        KEY_VARNAMES[KEY_NUMPADCOMMA] = "KEY_NUMPADCOMMA";
        KEY_VARNAMES[KEY_DIVIDE] = "KEY_DIVIDE";
        KEY_VARNAMES[KEY_SUBTRACT] = "KEY_SUBTRACT";
        KEY_VARNAMES[KEY_DECIMAL] = "KEY_DECIMAL";

        KEY_VARNAMES[KEY_LMENU] = "KEY_LMENU";
        KEY_VARNAMES[KEY_RMENU] = "KEY_RMENU";

        KEY_VARNAMES[KEY_LCONTROL] = "KEY_LCONTROL";
        KEY_VARNAMES[KEY_RCONTROL] = "KEY_RCONTROL";

        KEY_VARNAMES[KEY_LSHIFT] = "KEY_LSHIFT";
        KEY_VARNAMES[KEY_RSHIFT] = "KEY_RSHIFT";

        KEY_VARNAMES[KEY_LMETA] = "KEY_LMETA";
        KEY_VARNAMES[KEY_RMETA] = "KEY_RMETA";

        KEY_VARNAMES[KEY_MINUS] = "KEY_MINUS";
        KEY_VARNAMES[KEY_EQUALS] = "KEY_EQUALS";
        KEY_VARNAMES[KEY_LBRACKET] = "KEY_LBRACKET";
        KEY_VARNAMES[KEY_RBRACKET] = "KEY_RBRACKET";
        KEY_VARNAMES[KEY_SEMICOLON] = "KEY_SEMICOLON";
        KEY_VARNAMES[KEY_APOSTROPHE] = "KEY_APOSTROPHE";
        KEY_VARNAMES[KEY_GRAVE] = "KEY_GRAVE";
        KEY_VARNAMES[KEY_BACKSLASH] = "KEY_BACKSLASH";
        KEY_VARNAMES[KEY_COMMA] = "KEY_COMMA";
        KEY_VARNAMES[KEY_PERIOD] = "KEY_PERIOD";
        KEY_VARNAMES[KEY_SLASH] = "KEY_SLASH";
        KEY_VARNAMES[KEY_MULTIPLY] = "KEY_MULTIPLY";
        KEY_VARNAMES[KEY_ADD] = "KEY_ADD";
        KEY_VARNAMES[KEY_COLON] = "KEY_COLON";
        KEY_VARNAMES[KEY_UNDERLINE] = "KEY_UNDERLINE";
        KEY_VARNAMES[KEY_AT] = "KEY_AT";

        KEY_VARNAMES[KEY_APPS] = "KEY_APPS";
        KEY_VARNAMES[KEY_POWER] = "KEY_POWER";
        KEY_VARNAMES[KEY_SLEEP] = "KEY_SLEEP";

        KEY_VARNAMES[KEY_STOP] = "KEY_STOP";
        KEY_VARNAMES[KEY_ESCAPE] = "KEY_ESCAPE";
        KEY_VARNAMES[KEY_RETURN] = "KEY_RETURN";
        KEY_VARNAMES[KEY_SPACE] = "KEY_SPACE";
        KEY_VARNAMES[KEY_BACK] = "KEY_BACK";
        KEY_VARNAMES[KEY_TAB] = "KEY_TAB";

        KEY_VARNAMES[KEY_SYSRQ] = "KEY_SYSRQ";
        KEY_VARNAMES[KEY_PAUSE] = "KEY_PAUSE";

        KEY_VARNAMES[KEY_HOME] = "KEY_HOME";
        KEY_VARNAMES[KEY_PGUP] = "KEY_PGUP";
        KEY_VARNAMES[KEY_PGDN] = "KEY_PGDN";
        KEY_VARNAMES[KEY_END] = "KEY_END";
        KEY_VARNAMES[KEY_INSERT] = "KEY_INSERT";
        KEY_VARNAMES[KEY_DELETE] = "KEY_DELETE";

        KEY_VARNAMES[KEY_UP] = "KEY_UP";
        KEY_VARNAMES[KEY_LEFT] = "KEY_LEFT";
        KEY_VARNAMES[KEY_RIGHT] = "KEY_RIGHT";
        KEY_VARNAMES[KEY_DOWN] = "KEY_DOWN";

        KEY_VARNAMES[KEY_NUMLOCK] = "KEY_NUMLOCK";
        KEY_VARNAMES[KEY_CAPITAL] = "KEY_CAPITAL";
        KEY_VARNAMES[KEY_SCROLL] = "KEY_SCROLL";

        KEY_VARNAMES[KEY_KANA] = "KEY_KANA";
        KEY_VARNAMES[KEY_CONVERT] = "KEY_CONVERT";
        KEY_VARNAMES[KEY_NOCONVERT] = "KEY_NOCONVERT";
        KEY_VARNAMES[KEY_YEN] = "KEY_YEN";
        KEY_VARNAMES[KEY_CIRCUMFLEX] = "KEY_CIRCUMFLEX";
        KEY_VARNAMES[KEY_KANJI] = "KEY_KANJI";
        KEY_VARNAMES[KEY_AX] = "KEY_AX";
        KEY_VARNAMES[KEY_UNLABELED] = "KEY_UNLABELED";

    }

    public static String getName(int keyId) {
        return KEY_NAMES[keyId];
    }

    public static String getVarName(int keyId) {
        return KEY_VARNAMES[keyId];
    }
}
