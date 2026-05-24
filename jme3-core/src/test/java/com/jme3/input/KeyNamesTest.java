/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for the {@link KeyNames} class.
 *
 * Verifies that key codes defined in {@link KeyInput} correctly map
 * to their human-readable names, and that unmapped codes return null.
 */
public class KeyNamesTest {

    /**
     * Verify that common key codes return the expected human-readable names,
     * covering letters, digits, special keys, modifiers, and arrow keys.
     * Also verifies that an unmapped code returns null and that KEY_UNKNOWN
     * maps to the string "Unknown".
     */
    @Test
    public void testKeyNamesMapping() {
        // Letter keys
        assertEquals("A", KeyNames.getName(KeyInput.KEY_A));
        assertEquals("Z", KeyNames.getName(KeyInput.KEY_Z));

        // Digit keys
        assertEquals("0", KeyNames.getName(KeyInput.KEY_0));
        assertEquals("9", KeyNames.getName(KeyInput.KEY_9));

        // Special keys
        assertEquals("Space",     KeyNames.getName(KeyInput.KEY_SPACE));
        assertEquals("Enter",     KeyNames.getName(KeyInput.KEY_RETURN));
        assertEquals("Backspace", KeyNames.getName(KeyInput.KEY_BACK));
        assertEquals("Esc",       KeyNames.getName(KeyInput.KEY_ESCAPE));

        // Arrow keys
        assertEquals("Up",    KeyNames.getName(KeyInput.KEY_UP));
        assertEquals("Down",  KeyNames.getName(KeyInput.KEY_DOWN));
        assertEquals("Left",  KeyNames.getName(KeyInput.KEY_LEFT));
        assertEquals("Right", KeyNames.getName(KeyInput.KEY_RIGHT));

        // Modifier keys
        assertEquals("Left Shift",  KeyNames.getName(KeyInput.KEY_LSHIFT));
        assertEquals("Right Shift", KeyNames.getName(KeyInput.KEY_RSHIFT));
        assertEquals("Left Ctrl",   KeyNames.getName(KeyInput.KEY_LCONTROL));

        // KEY_UNKNOWN should map to the string "Unknown"
        assertEquals("Unknown", KeyNames.getName(KeyInput.KEY_UNKNOWN));

        // An unmapped key code should return null
        assertNull(KeyNames.getName(0x5C));
    }
}
