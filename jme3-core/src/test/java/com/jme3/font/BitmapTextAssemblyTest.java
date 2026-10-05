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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture2D;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import org.junit.jupiter.api.Test;

/**
 * Headless regression tests for bitmap-text mesh assembly. The synthetic font
 * needs neither an asset manager nor a renderer or graphics context.
 */
public class BitmapTextAssemblyTest {

    private static final int ATLAS_WIDTH = 4096;
    private static final int ATLAS_HEIGHT = 64;
    private static final float EPSILON = 0.00001f;
    private static final Type[] BUFFER_TYPES = {
        Type.Position, Type.TexCoord, Type.Color, Type.Index
    };

    @Test
    public void assemblesOneHundredDistinctQuadsInCharacterOrder() {
        BitmapFont font = createFont(1);
        String characters = distinctCharacters(100);
        BitmapText text = assemble(font, characters);
        Mesh mesh = mesh(text, 0);
        assertBufferSizes(mesh, 100);

        float x = 0;
        for (int i = 0; i < characters.length(); i++) {
            BitmapCharacter glyph = font.getCharSet().getCharacter(characters.charAt(i));
            assertQuad(mesh, i, glyph, x, -glyph.getYOffset());
            assertColor(mesh, i, 255, 255, 255, 255);
            x += glyph.getXAdvance();
        }
    }

    @Test
    public void colorTagsPreservePrefixAdjacentRangesAndTrailingTags() {
        BitmapText text = new BitmapText(createFont(1));
        text.setColor(new ColorRGBA(0.2f, 0.4f, 0.6f, 0.8f));
        text.setText("AB\\#f00#C\\#0f08#D\\#123456#E"
                + "\\#010203#\\#aBcDeF80#F\\#0ff#G\\#01020304#");
        text.updateLogicalState(0);
        Mesh mesh = mesh(text, 0);
        assertBufferSizes(mesh, 7);
        assertColor(mesh, 0, 51, 102, 153, 204);
        assertColor(mesh, 1, 51, 102, 153, 204);
        assertColor(mesh, 2, 255, 0, 0, 255);
        assertColor(mesh, 3, 0, 255, 0, 136);
        assertColor(mesh, 4, 18, 52, 86, 255);
        assertColor(mesh, 5, 171, 205, 239, 128);
        assertColor(mesh, 6, 0, 255, 255, 255);
        assertLayoutEquals(assemble(text.getFont(), "ABCDEFG"), text);
    }

    @Test
    public void baseAlphaOverrideAndResetRestoreTaggedAndUntaggedColors() {
        BitmapText text = new BitmapText(createFont(1));
        text.setColor(new ColorRGBA(0.2f, 0.4f, 0.6f, 0.8f));
        text.setText("A\\#f00#B\\#0f08#C\\#123456#D"
                + "\\#010203#\\#abcdef80#E\\#1234#");
        text.updateLogicalState(0);
        Mesh mesh = mesh(text, 0);
        byte[] originalColors = colors(mesh);
        assertColor(mesh, 0, 51, 102, 153, 204);
        assertColor(mesh, 1, 255, 0, 0, 255);
        assertColor(mesh, 2, 0, 255, 0, 136);
        assertColor(mesh, 3, 18, 52, 86, 255);
        assertColor(mesh, 4, 171, 205, 239, 128);

        text.setAlpha(0.25f);
        text.updateLogicalState(0);
        byte[] overriddenColors = colors(mesh);
        for (int i = 0; i < overriddenColors.length; i++) {
            int expected = i % 4 == 3 ? 63 : Byte.toUnsignedInt(originalColors[i]);
            assertEquals(expected, Byte.toUnsignedInt(overriddenColors[i]), "color byte " + i);
        }

        text.setAlpha(-1);
        text.updateLogicalState(0);
        assertEquals(-1f, text.getAlpha());
        assertArrayEquals(originalColors, colors(mesh));
    }

    @Test
    public void resettingAlphaWithoutABaseColorRestoresOpaquePrefix() {
        BitmapText text = assemble(createFont(1), "A\\#abcdef80#B");
        text.setAlpha(0.5f);
        text.updateLogicalState(0);
        assertColor(mesh(text, 0), 0, 255, 255, 255, 127);
        assertColor(mesh(text, 0), 1, 171, 205, 239, 127);
        text.setAlpha(-1);
        text.updateLogicalState(0);
        assertColor(mesh(text, 0), 0, 255, 255, 255, 255);
        assertColor(mesh(text, 0), 1, 171, 205, 239, 128);
    }

    @Test
    public void emptyAndTagOnlyTextHaveEmptyBuffersOnEveryPage() {
        BitmapText text = new BitmapText(createFont(2));
        for (String value : new String[]{"", "\\#123#\\#1234#\\#123456#\\#12345678#", "\n\n", null}) {
            text.setText(value);
            text.updateLogicalState(0);
            for (int page = 0; page < 2; page++) {
                assertBufferSizes(mesh(text, page), 0);
            }
        }
    }

    @Test
    public void reusedBuffersHandleNonemptyEmptyAndNonemptyText() {
        BitmapFont font = createFont(2);
        BitmapText text = assemble(font, distinctCharacters(100));
        for (String value : new String[]{"", "Z\\#ff000080#AB", distinctCharacters(100)}) {
            text.setText(value);
            text.updateLogicalState(0);
            BitmapText fresh = assemble(font, value);
            assertMeshEquals(fresh, text);
        }
    }

    @Test
    public void alternatingFontPagesPreserveOrderAndUsePageLocalIndices() {
        BitmapFont font = createFont(2);
        String characters = distinctCharacters(100);
        BitmapText text = assemble(font, characters);
        int[] pageQuadCounts = new int[2];
        float x = 0;
        for (int i = 0; i < characters.length(); i++) {
            BitmapCharacter glyph = font.getCharSet().getCharacter(characters.charAt(i));
            int page = glyph.getPage();
            assertEquals(i % 2, page);
            assertQuad(mesh(text, page), pageQuadCounts[page]++, glyph, x, -glyph.getYOffset());
            x += glyph.getXAdvance();
        }
        assertBufferSizes(mesh(text, 0), 50);
        assertBufferSizes(mesh(text, 1), 50);

        // Clearing one page must reset its counts even while another remains populated.
        text.setText("A");
        text.updateLogicalState(0);
        assertBufferSizes(mesh(text, 0), 0);
        assertBufferSizes(mesh(text, 1), 1);
        assertMeshEquals(assemble(font, "A"), text);
    }

    @Test
    public void legacyArrayFlagProducesIdenticalMeshesForBothDirections() {
        for (boolean rightToLeft : new boolean[]{false, true}) {
            BitmapFont font = createFont(2);
            font.setRightToLeft(rightToLeft);
            for (String value : new String[]{"", "ABC", "A\\#ff000080#B\nCD", "AB CD EF GH"}) {
                BitmapText arrayBased = new BitmapText(font, rightToLeft, true);
                BitmapText legacy = new BitmapText(font, rightToLeft, false);
                BitmapText defaultConstructor = new BitmapText(font);
                for (BitmapText text : new BitmapText[]{arrayBased, legacy, defaultConstructor}) {
                    text.setBox(new Rectangle(7, 101, 40, 120));
                    text.setAlignment(Align.Center);
                    text.setVerticalAlignment(VAlign.Bottom);
                    text.setText(value);
                    text.updateLogicalState(0);
                }
                assertMeshEquals(arrayBased, legacy);
                assertMeshEquals(arrayBased, defaultConstructor);
            }
        }
    }

    @Test
    public void tagsDoNotChangePlainMultilineOrWrappedAlignment() {
        BitmapFont font = createFont(2);
        String[] plain = {"ABCD", "AB\nCD", "AB CD EF GH"};
        String[] tagged = {"A\\#f00#B\\#0f08#CD\\#123#",
            "A\\#f00#B\n\\#0f08#CD\\#123#",
            "A\\#f00#B CD \\#0f08#EF GH\\#123#"};
        for (Align alignment : Align.values()) {
            for (VAlign verticalAlignment : VAlign.values()) {
                for (int i = 0; i < plain.length; i++) {
                    BitmapText expected = new BitmapText(font);
                    BitmapText actual = new BitmapText(font);
                    for (BitmapText text : new BitmapText[]{expected, actual}) {
                        text.setBox(new Rectangle(7, 101, i == 2 ? 40 : 100, 120));
                        text.setAlignment(alignment);
                        text.setVerticalAlignment(verticalAlignment);
                    }
                    expected.setText(plain[i]);
                    actual.setText(tagged[i]);
                    expected.updateLogicalState(0);
                    actual.updateLogicalState(0);
                    assertLayoutEquals(expected, actual);
                    if (i > 0) {
                        assertTrue(actual.getLineCount() > 1);
                    }
                }
            }
        }
    }

    @Test
    public void characterWrappingMatchesExplicitLineBreaksWithAlignment() {
        BitmapFont font = createFont(1);
        BitmapText wrapped = new BitmapText(font);
        BitmapText explicit = new BitmapText(font);
        for (BitmapText text : new BitmapText[]{wrapped, explicit}) {
            text.setBox(new Rectangle(7, 101, 23, 100));
            text.setAlignment(Align.Center);
            text.setVerticalAlignment(VAlign.Bottom);
            text.setLineWrapMode(LineWrapMode.Character);
        }
        wrapped.setText("ABC");
        explicit.setText("AB\nC");
        wrapped.updateLogicalState(0);
        explicit.updateLogicalState(0);
        assertMeshEquals(explicit, wrapped);
        assertEquals(2, wrapped.getLineCount());
        assertQuad(mesh(wrapped, 0), 0, font.getCharSet().getCharacter('A'), 8.5f, 40);
        assertQuad(mesh(wrapped, 0), 1, font.getCharSet().getCharacter('B'), 21.5f, 39);
        assertQuad(mesh(wrapped, 0), 2, font.getCharSet().getCharacter('C'), 14.5f, 18);
    }

    private static BitmapFont createFont(int pageCount) {
        BitmapCharacterSet characters = new BitmapCharacterSet();
        characters.setRenderedSize(16);
        characters.setLineHeight(20);
        characters.setBase(16);
        characters.setWidth(ATLAS_WIDTH);
        characters.setHeight(ATLAS_HEIGHT);
        for (char c = ' '; c < 0x100 + 100; c++) {
            int index = c - ' ';
            BitmapCharacter glyph = new BitmapCharacter();
            glyph.setChar(c);
            glyph.setX(index * 10);
            glyph.setY(index % 4 * 12);
            glyph.setWidth(c == ' ' ? 0 : 5 + index % 4);
            glyph.setHeight(c == ' ' ? 0 : 8 + index % 3);
            glyph.setXAdvance(c == ' ' ? 4 : 10 + index % 5);
            glyph.setYOffset(1 + index % 3);
            glyph.setPage(index % pageCount);
            characters.addCharacter(c, glyph);
        }
        MaterialDef definition = new MaterialDef(null, "Synthetic bitmap font");
        definition.addMaterialParamTexture(VarType.Texture2D, "ColorMap", null, null);
        Material[] pages = new Material[pageCount];
        for (int page = 0; page < pageCount; page++) {
            pages[page] = new Material(definition);
            pages[page].setTexture("ColorMap", new Texture2D());
        }
        BitmapFont font = new BitmapFont();
        font.setCharSet(characters);
        font.setPages(pages);
        return font;
    }

    private static String distinctCharacters(int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append((char) (0x100 + i));
        }
        return result.toString();
    }

    private static BitmapText assemble(BitmapFont font, String value) {
        BitmapText text = new BitmapText(font);
        text.setText(value);
        text.updateLogicalState(0);
        return text;
    }

    private static Mesh mesh(BitmapText text, int page) {
        return ((Geometry) text.getChild(page)).getMesh();
    }

    private static void assertBufferSizes(Mesh mesh, int quadCount) {
        int[] elementsPerQuad = {12, 8, 16, 6};
        for (int i = 0; i < BUFFER_TYPES.length; i++) {
            Buffer buffer = mesh.getBuffer(BUFFER_TYPES[i]).getData();
            assertEquals(quadCount * elementsPerQuad[i], buffer.limit(), BUFFER_TYPES[i] + " limit");
        }
        assertEquals(quadCount * 4, mesh.getVertexCount());
        assertEquals(quadCount * 2, mesh.getTriangleCount());
        assertTrue(mesh.getBuffer(Type.Color).isNormalized());
    }

    private static void assertQuad(Mesh mesh, int quad, BitmapCharacter glyph, float x, float y) {
        float right = x + glyph.getWidth();
        float bottom = y - glyph.getHeight();
        float[] expectedPositions = {x, y, 0, x, bottom, 0, right, bottom, 0, right, y, 0};
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        for (int i = 0; i < expectedPositions.length; i++) {
            assertEquals(expectedPositions[i], positions.get(quad * 12 + i), EPSILON,
                    "quad " + quad + ", position " + i);
        }
        float u0 = (float) glyph.getX() / ATLAS_WIDTH;
        float u1 = u0 + (float) glyph.getWidth() / ATLAS_WIDTH;
        float v0 = 1f - (float) glyph.getY() / ATLAS_HEIGHT;
        float v1 = 1f - ((float) glyph.getY() / ATLAS_HEIGHT + (float) glyph.getHeight() / ATLAS_HEIGHT);
        float[] expectedTexCoords = {u0, v0, u0, v1, u1, v1, u1, v0};
        FloatBuffer texCoords = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        for (int i = 0; i < expectedTexCoords.length; i++) {
            assertEquals(expectedTexCoords[i], texCoords.get(quad * 8 + i), EPSILON,
                    "quad " + quad + ", texture coordinate " + i);
        }
        int[] offsets = {0, 1, 2, 0, 2, 3};
        ShortBuffer indices = (ShortBuffer) mesh.getBuffer(Type.Index).getData();
        for (int i = 0; i < offsets.length; i++) {
            assertEquals(quad * 4 + offsets[i], Short.toUnsignedInt(indices.get(quad * 6 + i)),
                    "quad " + quad + ", index " + i);
        }
    }

    private static void assertColor(Mesh mesh, int quad, int red, int green, int blue, int alpha) {
        int[] expected = {red, green, blue, alpha};
        ByteBuffer colors = (ByteBuffer) mesh.getBuffer(Type.Color).getData();
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int component = 0; component < 4; component++) {
                int actual = Byte.toUnsignedInt(colors.get(quad * 16 + vertex * 4 + component));
                assertEquals(expected[component], actual,
                        "quad " + quad + ", vertex " + vertex + ", color component " + component);
            }
        }
    }

    private static byte[] colors(Mesh mesh) {
        ByteBuffer buffer = ((ByteBuffer) mesh.getBuffer(Type.Color).getData()).duplicate();
        buffer.rewind();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    private static void assertLayoutEquals(BitmapText expected, BitmapText actual) {
        assertEquals(expected.getQuantity(), actual.getQuantity());
        assertEquals(expected.getLineCount(), actual.getLineCount());
        assertEquals(expected.getLineWidth(), actual.getLineWidth(), EPSILON);
        assertEquals(expected.getHeight(), actual.getHeight(), EPSILON);
        for (int page = 0; page < expected.getQuantity(); page++) {
            Mesh expectedMesh = mesh(expected, page);
            Mesh actualMesh = mesh(actual, page);
            assertBufferSizes(actualMesh, expectedMesh.getVertexCount() / 4);
            for (Type type : new Type[]{Type.Position, Type.TexCoord, Type.Index}) {
                Buffer expectedBuffer = expectedMesh.getBuffer(type).getData();
                Buffer actualBuffer = actualMesh.getBuffer(type).getData();
                assertEquals(expectedBuffer.limit(), actualBuffer.limit(),
                        "page " + page + ", buffer " + type);
                for (int i = 0; i < expectedBuffer.limit(); i++) {
                    String message = "page " + page + ", buffer " + type + ", element " + i;
                    if (type == Type.Index) {
                        assertEquals(((ShortBuffer) expectedBuffer).get(i),
                                ((ShortBuffer) actualBuffer).get(i), message);
                    } else {
                        assertEquals(((FloatBuffer) expectedBuffer).get(i),
                                ((FloatBuffer) actualBuffer).get(i), EPSILON, message);
                    }
                }
            }
        }
    }

    private static void assertMeshEquals(BitmapText expected, BitmapText actual) {
        assertLayoutEquals(expected, actual);
        for (int page = 0; page < expected.getQuantity(); page++) {
            assertArrayEquals(colors(mesh(expected, page)), colors(mesh(actual, page)),
                    "page " + page + " colors");
        }
    }
}
