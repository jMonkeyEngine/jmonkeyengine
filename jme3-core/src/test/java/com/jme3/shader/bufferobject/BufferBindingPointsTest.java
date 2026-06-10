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
package com.jme3.shader.bufferobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BufferBindingPointsTest {

    @Test
    public void testEngineBindingsUseEndOfRange() {
        assertEquals(29, BufferBindingPoints.getUserBindingCount(36));
        assertEquals(35, BufferBindingPoints.getEngineBinding(36, BufferBindingPoints.EngineBinding.Lights));
        assertEquals(34, BufferBindingPoints.getEngineBinding(36, BufferBindingPoints.EngineBinding.Data1));
        assertEquals(33, BufferBindingPoints.getEngineBinding(36, BufferBindingPoints.EngineBinding.Data2));
        assertEquals(32, BufferBindingPoints.getEngineBinding(36, BufferBindingPoints.EngineBinding.Data3));
        assertEquals(31, BufferBindingPoints.getEngineBinding(36, BufferBindingPoints.EngineBinding.Frame));
        assertEquals(30, BufferBindingPoints.getEngineBinding(36, BufferBindingPoints.EngineBinding.Object));
        assertEquals(29, BufferBindingPoints.getEngineBinding(36, BufferBindingPoints.EngineBinding.MatParams));
    }

    @Test
    public void testReservedRangeDetection() {
        assertFalse(BufferBindingPoints.isEngineReserved(36, 28));
        assertTrue(BufferBindingPoints.isEngineReserved(36, 29));
        assertTrue(BufferBindingPoints.isEngineReserved(36, 35));
        assertFalse(BufferBindingPoints.isEngineReserved(36, 36));
    }

    @Test
    public void testInsufficientBindings() {
        assertEquals(3, BufferBindingPoints.getUserBindingCount(3));
        assertEquals(-1, BufferBindingPoints.getEngineBinding(3, BufferBindingPoints.EngineBinding.MatParams));
        assertFalse(BufferBindingPoints.isEngineReserved(3, 0));
        assertFalse(BufferBindingPoints.isEngineReserved(3, 2));
    }
}
