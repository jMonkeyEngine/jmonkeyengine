/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DisplayInfoTest {

    @Test
    void reportsCurrentDisplayModeBitDepth() {
        Displays displays = new Displays();
        int displayIndex = displays.addNewMonitor(1L);

        displays.setInfo(displayIndex, "Test display", 1920, 1080, 60, 32);

        assertEquals(32, displays.get(displayIndex).getBitDepth());
    }

    @Test
    void legacySetInfoLeavesBitDepthUnavailable() {
        Displays displays = new Displays();
        int displayIndex = displays.addNewMonitor(1L);

        displays.setInfo(displayIndex, "Test display", 1920, 1080, 60);

        assertEquals(0, displays.get(displayIndex).getBitDepth());
    }

    @Test
    void bitDepthIsPartOfDisplayInfoEquality() {
        Displays first = new Displays();
        int firstIndex = first.addNewMonitor(1L);
        first.setInfo(firstIndex, "Test display", 1920, 1080, 60, 24);

        Displays second = new Displays();
        int secondIndex = second.addNewMonitor(1L);
        second.setInfo(secondIndex, "Test display", 1920, 1080, 60, 32);

        assertNotEquals(first.get(firstIndex), second.get(secondIndex));
    }
}
