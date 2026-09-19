/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayScaleUtilsTest {

    @Test
    void logicalModeKeepsWindowCoordinatesWhenTheyMatchDisplayScale() {
        int[] size = DisplayScaleUtils.resolveLogicalSize(
                AppSettings.DISPLAY_SCALE_DPI_AWARE, 1280, 720, 2560, 1440, 2f, 2f);

        assertArrayEquals(new int[] {1280, 720}, size);
    }

    @Test
    void nativePixelsUsesFramebufferSizeAsLogicalSize() {
        int[] size = DisplayScaleUtils.resolveLogicalSize(
                AppSettings.DISPLAY_SCALE_NATIVE_PIXELS, 1280, 720, 2560, 1440, 2f, 2f);

        assertArrayEquals(new int[] {2560, 1440}, size);
    }

    @Test
    void disabledUsesFramebufferSizeAsLogicalSize() {
        int[] size = DisplayScaleUtils.resolveLogicalSize(
                AppSettings.DISPLAY_SCALE_DISABLED, 1280, 720, 2560, 1440, 2f, 2f);

        assertArrayEquals(new int[] {2560, 1440}, size);
    }

    @Test
    void inputConversionCanTargetLogicalOrPhysicalCoordinates() {
        assertEquals(640, DisplayScaleUtils.toInputX(640, 1280, 1280));
        assertEquals(360, DisplayScaleUtils.toInputY(360, 720, 720));
        assertEquals(1280, DisplayScaleUtils.toInputX(640, 2560, 1280));
        assertEquals(720, DisplayScaleUtils.toInputY(360, 1440, 720));
    }
}
