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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.ColorRGBA;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for cached letter state and forward-only color-tag traversal.
 */
public class LettersReuseTest {

    @Test
    public void retainsQuadsByParsedIndexAcrossShrinkingClearingAndGrowth() {
        BitmapFont font = BitmapTextAssemblyTest.createFont(1);
        StringBlock block = block("ABCDE");
        Letters letters = new Letters(font, block, false);
        List<LetterQuad> original = quads(letters);

        replaceText(letters, block, "ZYX");
        assertSameQuads(original, letters, 3);
        replaceText(letters, block, "");
        assertSame(letters.getTail(), letters.getHead().getNext());
        replaceText(letters, block, "QWERTYU");
        assertSameQuads(original, letters, original.size());
        List<LetterQuad> expanded = quads(letters);
        assertEquals(7, expanded.size());
        for (int i = 0; i < expanded.size(); i++) {
            assertEquals(block.getText().charAt(i), expanded.get(i).getChar());
            assertEquals(i, expanded.get(i).getIndex());
        }
        assertNotSame(original.get(4), expanded.get(5));
    }

    @Test
    public void reusedQuadsResetExplicitStylesColorsAndAllWrappingState() {
        BitmapFont font = BitmapTextAssemblyTest.createFont(1);
        BitmapCharacterSet styled = BitmapTextAssemblyTest.createFont(1).getCharSet();
        styled.getCharacter('A').setWidth(30);
        styled.getCharacter('A').setXAdvance(31);
        styled.setStyle(1);
        font.getCharSet().merge(styled);
        ColorRGBA baseColor = new ColorRGBA(0.2f, 0.4f, 0.6f, 0.8f);

        for (LineWrapMode mode : LineWrapMode.values()) {
            StringBlock block = block("AB CD\nEF");
            block.setTextBox(new Rectangle(0, 0, 17, 100));
            block.setLineWrapMode(mode);
            Letters letters = new Letters(font, block, false);
            LetterQuad first = letters.getHead().getNext();
            letters.setColor(baseColor);
            letters.setStyle(0, block.getText().length(), 1);
            letters.setColor(0, 3, ColorRGBA.Red);
            letters.setBaseAlpha(0.25f);
            letters.update();

            block.setTextBox(null);
            replaceText(letters, block, "ZA");
            letters.update();
            assertSame(first, letters.getHead().getNext());
            assertSame(font.getCharSet().getCharacter('A'), first.getNext().getBitmapChar());

            Letters fresh = new Letters(font, block.clone(), false);
            fresh.setColor(baseColor);
            fresh.update();
            assertLetterDataEquals(fresh, letters);
        }
    }

    @Test
    public void reuseFollowsContextDependentGlyphParserOutput() {
        BitmapFont font = BitmapTextAssemblyTest.createFont(1);
        font.setGlyphParser(new TestGlyphParser() {
            @Override
            public CharSequence parse(CharSequence text) {
                return text.length() == 2 ? "ZA" : "BCDE";
            }
        });
        StringBlock block = block("AB");
        Letters letters = new Letters(font, block, false);
        List<LetterQuad> original = quads(letters);
        assertEquals('Z', original.get(0).getChar());

        replaceText(letters, block, "ABC");
        assertSameQuads(original, letters, 2);
        List<LetterQuad> reshaped = quads(letters);
        assertEquals(4, reshaped.size());
        for (int i = 0; i < reshaped.size(); i++) {
            assertEquals("BCDE".charAt(i), reshaped.get(i).getChar());
        }
    }

    @Test
    public void tagsKeepAdjacentRangeAndPlainTextEndSemanticsAfterShaping() {
        BitmapFont font = BitmapTextAssemblyTest.createFont(1);
        font.setGlyphParser(new TestGlyphParser() {
            @Override
            public CharSequence parse(CharSequence text) {
                return "ABCDE";
            }
        });
        StringBlock block = block("A\\#f00#\\#0f08#B\\#abcdef80#C\\#00f#");
        Letters letters = new Letters(font, block, false);
        List<LetterQuad> quads = quads(letters);
        assertColor(quads.get(0), 255, 255, 255, 255);
        assertColor(quads.get(1), 0, 255, 0, 136);
        assertColor(quads.get(2), 171, 205, 239, 128);
        assertColor(quads.get(3), 255, 255, 255, 255);
        assertColor(quads.get(4), 255, 255, 255, 255);

        letters.setBaseAlpha(0.5f);
        letters.setBaseAlpha(-1);
        assertColor(quads.get(0), 255, 255, 255, 255);
        assertColor(quads.get(1), 0, 255, 0, 136);
        assertColor(quads.get(2), 171, 205, 239, 128);
        assertColor(quads.get(3), 255, 255, 255, 255);
        assertColor(quads.get(4), 255, 255, 255, 255);
    }

    @Test
    public void invalidationIncludesValidQuadsAfterAnAlreadyInvalidOne() {
        BitmapFont font = BitmapTextAssemblyTest.createFont(1);
        Letters letters = new Letters(font, block("ABC"), false);
        letters.update();
        List<LetterQuad> quads = quads(letters);
        quads.get(0).invalidate();
        assertFalse(quads.get(1).isInvalid());
        letters.invalidate();
        for (LetterQuad quad : quads) {
            assertTrue(quad.isInvalid());
        }
    }

    private static StringBlock block(String text) {
        StringBlock block = new StringBlock();
        block.setSize(16);
        block.setText(text);
        return block;
    }

    private static void replaceText(Letters letters, StringBlock block, String text) {
        block.setText(text);
        letters.setText(text);
    }

    private static List<LetterQuad> quads(Letters letters) {
        List<LetterQuad> result = new ArrayList<>();
        for (LetterQuad quad = letters.getHead().getNext(); !quad.isTail(); quad = quad.getNext()) {
            result.add(quad);
        }
        return result;
    }

    private static void assertSameQuads(List<LetterQuad> expected, Letters actual, int count) {
        List<LetterQuad> actualQuads = quads(actual);
        for (int i = 0; i < count; i++) {
            assertSame(expected.get(i), actualQuads.get(i), "quad " + i);
        }
    }

    private static void assertLetterDataEquals(Letters expected, Letters actual) {
        List<LetterQuad> expectedQuads = quads(expected);
        List<LetterQuad> actualQuads = quads(actual);
        assertEquals(expectedQuads.size(), actualQuads.size());
        for (int i = 0; i < expectedQuads.size(); i++) {
            float[] expectedPositions = new float[12];
            float[] actualPositions = new float[12];
            float[] expectedTexCoords = new float[8];
            float[] actualTexCoords = new float[8];
            byte[] expectedColors = new byte[16];
            byte[] actualColors = new byte[16];
            expectedQuads.get(i).storeToArrays(expectedPositions, expectedTexCoords, expectedColors);
            actualQuads.get(i).storeToArrays(actualPositions, actualTexCoords, actualColors);
            assertArrayEquals(expectedPositions, actualPositions, 0.00001f);
            assertArrayEquals(expectedTexCoords, actualTexCoords, 0.00001f);
            assertArrayEquals(expectedColors, actualColors);
        }
    }

    private static void assertColor(LetterQuad quad, int red, int green, int blue, int alpha) {
        byte[] color = new byte[16];
        quad.storeColors(color);
        int[] components = {red, green, blue, alpha};
        for (int i = 0; i < color.length; i++) {
            assertEquals(components[i % 4], Byte.toUnsignedInt(color[i]), "color byte " + i);
        }
    }

    private abstract static class TestGlyphParser implements GlyphParser {
        @Override
        public void write(JmeExporter exporter) {
        }

        @Override
        public void read(JmeImporter importer) {
        }
    }
}
