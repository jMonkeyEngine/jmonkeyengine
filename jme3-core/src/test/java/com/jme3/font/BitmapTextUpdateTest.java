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

import static com.jme3.font.BitmapTextAssemblyTest.assertMeshEquals;
import static com.jme3.font.BitmapTextAssemblyTest.colors;
import static com.jme3.font.BitmapTextAssemblyTest.createFont;
import static com.jme3.font.BitmapTextAssemblyTest.mesh;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for selective text updates and independent cloned text.
 */
public class BitmapTextUpdateTest {
    private static final Type[] TYPES = {
        Type.Position, Type.TexCoord, Type.Color, Type.Index
    };

    @Test
    public void colorChangesPreserveWrappedClippedAndRtlLayout() {
        for (boolean rightToLeft : new boolean[]{false, true}) {
            for (LineWrapMode wrap : LineWrapMode.values()) {
                BitmapFont font = createFont(2);
                font.setRightToLeft(rightToLeft);
                BitmapText text = new BitmapText(font);
                text.setBox(new Rectangle(7, 110, 45, 100));
                text.setAlignment(BitmapFont.Align.Center);
                text.setVerticalAlignment(BitmapFont.VAlign.Bottom);
                text.setLineWrapMode(wrap);
                text.setEllipsisChar('.');
                text.setText("ABCD EF\nGH");
                text.updateLogicalState(0);
                for (int operation = 0; operation < 4; operation++) {
                    PageSnapshot[] before = snapshot(text);
                    clearUpdates(text);
                    switch (operation) {
                        case 0:
                            text.setAlpha(0.3f);
                            break;
                        case 1:
                            text.setColor(new ColorRGBA(0.2f, 0.5f, 0.8f, 0.7f));
                            break;
                        case 2:
                            text.setColor(1, 3, ColorRGBA.Red);
                            break;
                        default:
                            text.setColor("[AG]", ColorRGBA.Blue);
                            break;
                    }
                    text.updateLogicalState(0);
                    for (int page = 0; page < before.length; page++) {
                        before[page].assertLayoutUnchanged(text, page);
                        assertFalse(mesh(text, page).getBuffer(Type.Position).isUpdateNeeded());
                        assertFalse(mesh(text, page).getBuffer(Type.TexCoord).isUpdateNeeded());
                        assertFalse(mesh(text, page).getBuffer(Type.Index).isUpdateNeeded());
                    }
                }
            }
        }
    }

    @Test
    public void readingDimensionsFlushesOnlyPendingColors() {
        BitmapText text = text(createFont(2), "ABCD");
        float width = text.getLineWidth();
        float height = text.getHeight();
        int lines = text.getLineCount();
        clearUpdates(text);
        text.setAlpha(0.5f);
        assertEquals(width, text.getLineWidth());
        assertEquals(height, text.getHeight());
        assertEquals(lines, text.getLineCount());
        for (int page = 0; page < text.getQuantity(); page++) {
            assertTrue(mesh(text, page).getBuffer(Type.Color).isUpdateNeeded());
            assertFalse(mesh(text, page).getBuffer(Type.Position).isUpdateNeeded());
            assertFalse(mesh(text, page).getBuffer(Type.TexCoord).isUpdateNeeded());
            assertFalse(mesh(text, page).getBuffer(Type.Index).isUpdateNeeded());
        }
        clearUpdates(text);
        text.updateLogicalState(0);
        assertClean(text);
    }

    @Test
    public void layoutChangesAreNotLostWhenColorsAlsoChange() {
        BitmapFont font = createFont(2);
        for (boolean colorFirst : new boolean[]{false, true}) {
            BitmapText actual = text(font, "AB CD");
            clearUpdates(actual);
            if (colorFirst) {
                actual.setAlpha(0.5f);
                actual.setSize(32);
            } else {
                actual.setSize(32);
                actual.setAlpha(0.5f);
            }
            actual.updateLogicalState(0);
            BitmapText expected = new BitmapText(font);
            expected.setText("AB CD");
            expected.setSize(32);
            expected.setAlpha(0.5f);
            expected.updateLogicalState(0);
            assertMeshEquals(expected, actual);
            for (int page = 0; page < actual.getQuantity(); page++) {
                assertTrue(mesh(actual, page).getBuffer(Type.Position).isUpdateNeeded());
                assertFalse(mesh(actual, page).getBuffer(Type.Index).isUpdateNeeded());
            }
        }
    }

    @Test
    public void reapplyingBaseColorStillClearsSubstringOverrides() {
        BitmapFont font = createFont(2);
        BitmapText text = text(font, "ABCD");
        text.setColor(ColorRGBA.White);
        text.setColor(1, 3, ColorRGBA.Red);
        text.updateLogicalState(0);
        clearUpdates(text);
        text.setColor(ColorRGBA.White);
        text.updateLogicalState(0);
        assertMeshEquals(text(font, "ABCD"), text);
        assertTrue(mesh(text, 0).getBuffer(Type.Color).isUpdateNeeded());
        assertTrue(mesh(text, 1).getBuffer(Type.Color).isUpdateNeeded());
    }

    @Test
    public void repeatedAlphaResetRestoresOriginalTagsAfterSubstringOverrides() {
        BitmapFont font = createFont(1);
        BitmapText text = text(font, "A\\#f008#BC");
        byte[] original = colors(mesh(text, 0));
        text.setAlpha(-1);
        text.setColor(1, 3, ColorRGBA.Blue);
        text.updateLogicalState(0);
        clearUpdates(text);
        assertEquals(-1f, text.getAlpha());
        text.setAlpha(-1);
        text.updateLogicalState(0);
        assertArrayEquals(original, colors(mesh(text, 0)));
        assertTrue(mesh(text, 0).getBuffer(Type.Color).isUpdateNeeded());
        assertFalse(mesh(text, 0).getBuffer(Type.Position).isUpdateNeeded());
        clearUpdates(text);
        text.setAlpha(-1);
        text.updateLogicalState(0);
        assertClean(text);
    }

    @Test
    public void unchangedScalarSettingsDoNotUploadBuffers() {
        BitmapText text = text(createFont(2), "ABCD");
        clearUpdates(text);
        text.setText(new String(text.getText()));
        text.setSize(text.getSize());
        text.setAlignment(text.getAlignment());
        text.setVerticalAlignment(text.getVerticalAlignment());
        text.setLineWrapMode(text.getLineWrapMode());
        text.setTabWidth(50);
        text.setEllipsisChar('\u2026');
        text.updateLogicalState(0);
        assertClean(text);
    }

    @Test
    public void equalBoxesSkipRefreshButChangedBoundsRelayout() {
        BitmapFont font = createFont(2);
        Rectangle box = new Rectangle(0, 100, 25, 100);
        BitmapText actual = text(font, "AB CD EF");
        actual.setBox(box);
        actual.updateLogicalState(0);
        clearUpdates(actual);
        actual.setBox(box.clone());
        actual.updateLogicalState(0);
        assertClean(actual);
        box = new Rectangle(0, 100, 120, 100);
        actual.setBox(box);
        actual.updateLogicalState(0);
        BitmapText expected = new BitmapText(font);
        expected.setBox(box.clone());
        expected.setText("AB CD EF");
        expected.updateLogicalState(0);
        assertMeshEquals(expected, actual);
    }

    @Test
    public void styleChangesRelayoutFollowingLettersEvenWithPendingColors() {
        BitmapFont font = styledFont();
        BitmapText text = text(font, "ABC");
        FloatBuffer positions = (FloatBuffer) mesh(text, 0).getBuffer(Type.Position).getData();
        float oldThirdX = positions.get(24);
        text.setStyle(1, 2, 1);
        text.setAlpha(0.5f);
        text.updateLogicalState(0);
        assertEquals(8, mesh(text, 0).getVertexCount());
        assertEquals(4, mesh(text, 1).getVertexCount());
        FloatBuffer newPositions = (FloatBuffer) mesh(text, 0).getBuffer(Type.Position).getData();
        assertEquals(oldThirdX + 7, newPositions.get(12), 0.00001f);
    }

    @Test
    public void clonedTextsPreserveStylesAndColorsAndUpdateIndependently() {
        BitmapText source = new BitmapText(styledFont());
        source.setColor(new ColorRGBA(0.2f, 0.4f, 0.6f, 0.8f));
        source.setText("A\\#f008#BC DE");
        source.setStyle(0, 3, 1);
        source.setColor(2, 3, ColorRGBA.Blue);
        source.setAlpha(0.6f);
        source.updateLogicalState(0);
        BitmapText clone = source.clone();
        clone.updateLogicalState(0);
        assertMeshEquals(source, clone);
        assertEquals(source.getAlpha(), clone.getAlpha());
        assertEquals(source.getColor(), clone.getColor());
        assertNotSame(source.getColor(), clone.getColor());
        for (int page = 0; page < source.getQuantity(); page++) {
            assertNotSame(mesh(source, page), mesh(clone, page));
            for (Type type : TYPES) {
                assertNotSame(mesh(source, page).getBuffer(type).getData(),
                        mesh(clone, page).getBuffer(type).getData());
            }
        }
        PageSnapshot[] sourceBefore = snapshot(source);
        clone.setAlpha(0.1f);
        clone.updateLogicalState(0);
        assertUnchanged(sourceBefore, source);
        PageSnapshot[] cloneBefore = snapshot(clone);
        source.setText("ZY");
        source.updateLogicalState(0);
        assertUnchanged(cloneBefore, clone);
        PageSnapshot[] changedSource = snapshot(source);
        clone.setColor(0, 2, ColorRGBA.Green);
        clone.updateLogicalState(0);
        assertUnchanged(changedSource, source);
    }

    @Test
    public void cloningPendingTextAndColorChangesUsesIndependentCurrentLetters() {
        BitmapText source = text(createFont(2), "ABCD");
        source.setText("ZY\\#08f4#X");
        source.setColor(0, 1, ColorRGBA.Red);
        BitmapText clone = source.clone();
        clone.updateLogicalState(0);
        source.updateLogicalState(0);
        assertMeshEquals(source, clone);
        clone.setText("ABCDEF");
        clone.updateLogicalState(0);
        source.setAlpha(0.5f);
        source.updateLogicalState(0);
        assertMeshEquals(text(source.getFont(), "ABCDEF"), clone);
    }

    private static BitmapFont styledFont() {
        BitmapFont font = createFont(1);
        BitmapFont styled = createFont(1);
        BitmapCharacter wider = styled.getCharSet().getCharacter('B');
        wider.setXAdvance(wider.getXAdvance() + 7);
        styled.setStyle(1);
        font.merge(styled);
        return font;
    }

    private static BitmapText text(BitmapFont font, String value) {
        BitmapText text = new BitmapText(font);
        text.setText(value);
        text.updateLogicalState(0);
        return text;
    }

    private static void clearUpdates(BitmapText text) {
        for (int page = 0; page < text.getQuantity(); page++) {
            for (Type type : TYPES) {
                mesh(text, page).getBuffer(type).clearUpdateNeeded();
            }
        }
    }

    private static void assertClean(BitmapText text) {
        for (int page = 0; page < text.getQuantity(); page++) {
            for (Type type : TYPES) {
                assertFalse(mesh(text, page).getBuffer(type).isUpdateNeeded(), type.toString());
            }
        }
    }

    private static PageSnapshot[] snapshot(BitmapText text) {
        PageSnapshot[] result = new PageSnapshot[text.getQuantity()];
        for (int page = 0; page < result.length; page++) {
            result[page] = new PageSnapshot(text, page);
        }
        return result;
    }

    private static void assertUnchanged(PageSnapshot[] before, BitmapText text) {
        for (int page = 0; page < before.length; page++) {
            before[page].assertLayoutUnchanged(text, page);
            assertArrayEquals(before[page].colorData, colors(mesh(text, page)));
        }
    }

    private static float[] floats(Mesh mesh, Type type) {
        FloatBuffer buffer = ((FloatBuffer) mesh.getBuffer(type).getData()).duplicate();
        buffer.rewind();
        float[] result = new float[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    private static short[] indices(Mesh mesh) {
        ShortBuffer buffer = ((ShortBuffer) mesh.getBuffer(Type.Index).getData()).duplicate();
        buffer.rewind();
        short[] result = new short[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    private static class PageSnapshot {
        final Geometry geometry;
        final Mesh mesh;
        final Buffer[] buffers;
        final float[] positions;
        final float[] texCoords;
        final short[] indexData;
        final byte[] colorData;
        final BoundingVolume bound;

        PageSnapshot(BitmapText text, int page) {
            geometry = (Geometry) text.getChild(page);
            mesh = geometry.getMesh();
            buffers = new Buffer[TYPES.length];
            for (int i = 0; i < TYPES.length; i++) {
                buffers[i] = mesh.getBuffer(TYPES[i]).getData();
            }
            positions = floats(mesh, Type.Position);
            texCoords = floats(mesh, Type.TexCoord);
            indexData = indices(mesh);
            colorData = colors(mesh);
            bound = mesh.getBound().clone();
        }

        void assertLayoutUnchanged(BitmapText text, int page) {
            assertSame(geometry, text.getChild(page));
            assertSame(mesh, geometry.getMesh());
            for (int i = 0; i < TYPES.length; i++) {
                assertSame(buffers[i], mesh.getBuffer(TYPES[i]).getData());
            }
            assertArrayEquals(positions, floats(mesh, Type.Position));
            assertArrayEquals(texCoords, floats(mesh, Type.TexCoord));
            assertArrayEquals(indexData, indices(mesh));
            assertEquals(bound, mesh.getBound());
        }
    }
}
