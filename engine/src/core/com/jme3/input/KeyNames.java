package com.jme3.input;

import static com.jme3.input.KeyInput.*;

public class KeyNames {
    
    private static final String[] KEY_NAMES = new String[0xFF];
    
    static {
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
        KEY_NAMES[KEY_NUMPADCOMMA] = "Numpad .";
        KEY_NAMES[KEY_DIVIDE] = "Numpad /";
        
        
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
        
        KEY_NAMES[KEY_SYSRQ] = "SysEq";
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
    }
    
    public String getName(int keyId){
        return KEY_NAMES[keyId];
    }
}
