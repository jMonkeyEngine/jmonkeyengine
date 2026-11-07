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
package com.jme3.font;

import static com.jme3.font.BitmapTextAssemblyTest.createFont;
import static com.jme3.font.BitmapTextAssemblyTest.mesh;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

/** Regression coverage for behavior retained while redistributing page quads. */
public class BitmapTextCompatibilityTest {

    @Test
    public void glyphsWithUnavailablePagesAreSkippedWithoutChangingLayout() {
        BitmapFont font = createFont(2);
        font.getCharSet().getCharacter('A').setPage(-1);
        font.getCharSet().getCharacter('B').setPage(2);
        BitmapText text = new BitmapText(font);
        text.setText("ABCD");
        text.updateLogicalState(0);

        // The old per-page filter skipped unavailable pages. Their characters
        // still participated in text layout, so C and D keep their advances.
        assertEquals(4, mesh(text, 0).getVertexCount());
        assertEquals(4, mesh(text, 1).getVertexCount());
        float firstVisibleX = font.getCharSet().getCharacter('A').getXAdvance()
                + font.getCharSet().getCharacter('B').getXAdvance();
        FloatBuffer positions = (FloatBuffer) mesh(text, 1).getBuffer(Type.Position).getData();
        assertEquals(firstVisibleX, positions.get(0), 0.00001f);

        text.setAlpha(0.25f);
        text.updateLogicalState(0);
        assertEquals(firstVisibleX, positions.get(0), 0.00001f);

        text.setText("AB");
        text.updateLogicalState(0);
        assertEquals(0, mesh(text, 0).getVertexCount());
        assertEquals(0, mesh(text, 1).getVertexCount());
    }
}
