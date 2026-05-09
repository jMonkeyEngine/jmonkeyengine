package com.jme3.input;

import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InputUtilityTest {

    @Test
    void keyNamesReturnStableNamesForRepresentativeKeyGroups() {
        new KeyNames();

        assertEquals("Unknown", KeyNames.getName(KeyInput.KEY_UNKNOWN));
        assertEquals("0", KeyNames.getName(KeyInput.KEY_0));
        assertEquals("A", KeyNames.getName(KeyInput.KEY_A));
        assertEquals("F12", KeyNames.getName(KeyInput.KEY_F12));
        assertEquals("Numpad Enter", KeyNames.getName(KeyInput.KEY_NUMPADENTER));
        assertEquals("Left Ctrl", KeyNames.getName(KeyInput.KEY_LCONTROL));
        assertEquals("Esc", KeyNames.getName(KeyInput.KEY_ESCAPE));
        assertEquals("Page Down", KeyNames.getName(KeyInput.KEY_PGDN));
        assertEquals("Kana", KeyNames.getName(KeyInput.KEY_KANA));
        assertNull(KeyNames.getName(KeyInput.KEY_LAST));
    }

    @Test
    void rawInputListenerAdapterAcceptsAllCallbacksAsNoOps() {
        RawInputListener listener = new RawInputListenerAdapter() {
        };

        listener.beginInput();
        listener.onJoyAxisEvent((JoyAxisEvent) null);
        listener.onJoyButtonEvent((JoyButtonEvent) null);
        listener.onMouseMotionEvent((MouseMotionEvent) null);
        listener.onMouseButtonEvent((MouseButtonEvent) null);
        listener.onKeyEvent((KeyInputEvent) null);
        listener.onTouchEvent((TouchEvent) null);
        listener.endInput();
    }
}
