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

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture2D;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import org.junit.jupiter.api.Test;

/**
 * Checks buffer reuse, selective updates, and clone ownership without a renderer.
 */
public class BitmapTextPageTest {
    private static final Type[] TYPES = {Type.Position, Type.TexCoord, Type.Index, Type.Color};
    private static final int[] ELEMENTS_PER_QUAD = {12, 8, 6, 16};

    @Test
    void incrementalGrowthReusesSpareCapacityAndKeepsExactLogicalLengths() {
        BitmapFont font = createFont();
        BitmapTextPage page = new BitmapTextPage(font);
        Mesh mesh = page.getMesh();
        Buffer[] previous = buffers(mesh);
        boolean[] reused = new boolean[TYPES.length];
        for (int length = 1; length <= 32; length++) {
            rebuild(page, font, repeat('A', length));
            Buffer[] current = buffers(mesh);
            for (int i = 0; i < TYPES.length; i++) {
                reused[i] |= current[i] == previous[i];
                assertEquals(length * ELEMENTS_PER_QUAD[i], current[i].limit(), TYPES[i].name());
                // BoundingBox consumes the position buffer when updating bounds.
                if (TYPES[i] != Type.Position) {
                    assertEquals(0, current[i].position(), TYPES[i].name());
                }
                assertTrue(current[i].capacity() >= current[i].limit(), TYPES[i].name());
            }
            assertEquals(length * 4, mesh.getVertexCount());
            assertEquals(length * 2, mesh.getTriangleCount());
            assertIndices(mesh, length);
            previous = current;
        }
        for (int i = 0; i < TYPES.length; i++) {
            assertTrue(reused[i], TYPES[i] + " should reuse capacity while text grows");
        }
        rebuild(page, font, "AA");
        for (int i = 0; i < TYPES.length; i++) {
            assertSame(previous[i], mesh.getBuffer(TYPES[i]).getData(), TYPES[i].name());
        }
        assertIndices(mesh, 2);
        rebuild(page, font, repeat('B', 32));
        for (int i = 0; i < TYPES.length; i++) {
            assertSame(previous[i], mesh.getBuffer(TYPES[i]).getData(), TYPES[i].name());
        }
        assertIndices(mesh, 32);
    }

    @Test
    void sameQuadCountPreservesIndexBufferWithoutUploadingIt() {
        BitmapFont font = createFont();
        BitmapTextPage page = new BitmapTextPage(font);
        rebuild(page, font, "AAAA");
        Mesh mesh = page.getMesh();
        Buffer[] original = buffers(mesh);
        clearFlags(mesh);
        rebuild(page, font, "BBBB");
        for (int i = 0; i < TYPES.length; i++) {
            assertSame(original[i], mesh.getBuffer(TYPES[i]).getData(), TYPES[i].name());
        }
        assertFalse(mesh.getBuffer(Type.Index).isUpdateNeeded());
        assertTrue(mesh.getBuffer(Type.Position).isUpdateNeeded());
        assertTrue(mesh.getBuffer(Type.TexCoord).isUpdateNeeded());
        assertIndices(mesh, 4);
    }

    @Test
    void colorUpdatesLeaveOtherBuffersCleanAndSkipUnchangedPackedColors() {
        BitmapFont font = createFont();
        BitmapTextPage page = new BitmapTextPage(font);
        Letters letters = rebuild(page, font, "ABAB");
        Mesh mesh = page.getMesh();
        Buffer[] original = buffers(mesh);
        clearFlags(mesh);
        letters.setColor(ColorRGBA.Red);
        page.updateColors();
        for (int i = 0; i < TYPES.length; i++) {
            assertSame(original[i], mesh.getBuffer(TYPES[i]).getData(), TYPES[i].name());
            assertEquals(TYPES[i] == Type.Color, mesh.getBuffer(TYPES[i]).isUpdateNeeded(), TYPES[i].name());
        }
        ByteBuffer colors = (ByteBuffer) mesh.getBuffer(Type.Color).getData();
        for (int vertex = 0; vertex < mesh.getVertexCount(); vertex++) {
            assertEquals(255, Byte.toUnsignedInt(colors.get(vertex * 4)));
            assertEquals(0, Byte.toUnsignedInt(colors.get(vertex * 4 + 1)));
            assertEquals(0, Byte.toUnsignedInt(colors.get(vertex * 4 + 2)));
            assertEquals(255, Byte.toUnsignedInt(colors.get(vertex * 4 + 3)));
        }
        clearFlags(mesh);
        page.updateColors();
        assertClean(mesh);
    }

    @Test
    void emptyPagesStayCleanAfterTheirDrawCountsHaveBeenCleared() {
        BitmapFont font = createFont();
        BitmapTextPage page = new BitmapTextPage(font);
        Mesh mesh = page.getMesh();
        clearFlags(mesh);
        page.assemble();
        page.updateColors();
        assertClean(mesh);

        rebuild(page, font, "AB");
        Buffer[] previous = buffers(mesh);
        clearFlags(mesh);
        page.clearQuads();
        page.assemble();
        assertEquals(0, mesh.getVertexCount());
        assertEquals(0, mesh.getTriangleCount());
        for (int i = 0; i < TYPES.length; i++) {
            assertSame(previous[i], mesh.getBuffer(TYPES[i]).getData(), TYPES[i].name());
            assertEquals(0, previous[i].limit(), TYPES[i].name());
            assertTrue(mesh.getBuffer(TYPES[i]).isUpdateNeeded(), TYPES[i].name());
        }
        clearFlags(mesh);
        page.assemble();
        page.updateColors();
        assertClean(mesh);
    }

    @Test
    void clonesOwnTheirBuffersAndRebuildMembershipAndIndexCapacity() {
        BitmapFont font = createFont();
        BitmapTextPage original = new BitmapTextPage(font);
        rebuild(original, font, repeat('A', 12));
        Letters originalLetters = rebuild(original, font, "AA");
        BitmapTextPage clone = original.clone();
        assertNotSame(original.getMesh(), clone.getMesh());
        for (Type type : TYPES) {
            assertNotSame(original.getMesh().getBuffer(type).getData(),
                    clone.getMesh().getBuffer(type).getData(), type.name());
        }
        byte[] cloneColors = colorBytes(clone.getMesh());
        clearFlags(clone.getMesh());
        originalLetters.setColor(ColorRGBA.Red);
        original.updateColors();
        clone.updateColors();
        assertArrayEquals(cloneColors, colorBytes(clone.getMesh()));
        assertClean(clone.getMesh());

        Letters cloneLetters = rebuild(clone, font, repeat('B', 8));
        assertIndices(clone.getMesh(), 8);
        cloneLetters.setColor(ColorRGBA.Blue);
        clone.updateColors();
        original.updateColors();
        assertIndices(original.getMesh(), 2);
        assertEquals(255, Byte.toUnsignedInt(colorBytes(original.getMesh())[0]));
        assertEquals(0, Byte.toUnsignedInt(colorBytes(original.getMesh())[2]));
        assertEquals(0, Byte.toUnsignedInt(colorBytes(clone.getMesh())[0]));
        assertEquals(255, Byte.toUnsignedInt(colorBytes(clone.getMesh())[2]));
    }

    private static BitmapFont createFont() {
        BitmapCharacterSet characters = new BitmapCharacterSet();
        characters.setRenderedSize(16);
        characters.setLineHeight(20);
        characters.setBase(16);
        characters.setWidth(64);
        characters.setHeight(32);
        for (char c = 'A'; c <= 'B'; c++) {
            BitmapCharacter glyph = new BitmapCharacter();
            glyph.setChar(c);
            glyph.setX((c - 'A') * 16);
            glyph.setWidth(c == 'A' ? 6 : 8);
            glyph.setHeight(12);
            glyph.setXAdvance(c == 'A' ? 8 : 10);
            characters.addCharacter(c, glyph);
        }
        MaterialDef definition = new MaterialDef(null, "Synthetic bitmap font");
        definition.addMaterialParamTexture(VarType.Texture2D, "ColorMap", null, null);
        Material material = new Material(definition);
        material.setTexture("ColorMap", new Texture2D());
        BitmapFont font = new BitmapFont();
        font.setCharSet(characters);
        font.setPages(new Material[]{material});
        return font;
    }

    private static Letters rebuild(BitmapTextPage page, BitmapFont font, String text) {
        StringBlock block = new StringBlock();
        block.setSize(16);
        block.setText(text);
        Letters letters = new Letters(font, block, false);
        letters.update();
        page.clearQuads();
        letters.rewind();
        while (letters.nextCharacter()) {
            if (letters.isPrintable()) {
                page.addQuad(letters.getQuad());
            }
        }
        page.assemble();
        return letters;
    }

    private static String repeat(char value, int count) {
        StringBuilder text = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            text.append(value);
        }
        return text.toString();
    }

    private static Buffer[] buffers(Mesh mesh) {
        Buffer[] result = new Buffer[TYPES.length];
        for (int i = 0; i < TYPES.length; i++) {
            result[i] = mesh.getBuffer(TYPES[i]).getData();
        }
        return result;
    }

    private static void clearFlags(Mesh mesh) {
        for (Type type : TYPES) {
            mesh.getBuffer(type).clearUpdateNeeded();
        }
    }

    private static void assertClean(Mesh mesh) {
        for (Type type : TYPES) {
            assertFalse(mesh.getBuffer(type).isUpdateNeeded(), type.name());
        }
    }

    private static byte[] colorBytes(Mesh mesh) {
        ByteBuffer data = ((ByteBuffer) mesh.getBuffer(Type.Color).getData()).duplicate();
        data.rewind();
        byte[] result = new byte[data.remaining()];
        data.get(result);
        return result;
    }

    private static void assertIndices(Mesh mesh, int quadCount) {
        ShortBuffer indices = (ShortBuffer) mesh.getBuffer(Type.Index).getData();
        int[] offsets = {0, 1, 2, 0, 2, 3};
        assertEquals(quadCount * 6, indices.limit());
        for (int quad = 0; quad < quadCount; quad++) {
            for (int i = 0; i < offsets.length; i++) {
                assertEquals(quad * 4 + offsets[i], Short.toUnsignedInt(indices.get(quad * 6 + i)));
            }
        }
    }
}
