/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * One page per BitmapText Font Texture.
 * @author Lim, YongHoon
 */
class BitmapTextPage extends Geometry {

    private float[] pos;
    private float[] tc;
    private byte[] color;
    private final Texture2D texture;
    private ArrayList<LetterQuad> pageQuads = new ArrayList<>();
    private int initializedIndexQuads;

    /**
     * Creates the page at the given index for the specified font.
     *
     * @param font the font to use (not null)
     * @param page the index of the font page to display
     */
    BitmapTextPage(BitmapFont font, int page) {
        super("BitmapFont", new Mesh());
        setRequiresUpdates(false);
        setBatchHint(BatchHint.Never);
        if (font == null) {
            throw new IllegalArgumentException("font cannot be null.");
        }

        Material mat = font.getPage(page);
        if (mat == null) {
            throw new IllegalStateException("The font's texture was not found!");
        }

        setMaterial(mat);
        this.texture = (Texture2D) mat.getTextureParam("ColorMap").getTextureValue();

        // initialize buffers
        Mesh m = getMesh();
        m.setBuffer(Type.Position, 3, new float[0]);
        m.setBuffer(Type.TexCoord, 2, new float[0]);
        m.setBuffer(Type.Color, 4, new byte[0]);
        m.setBuffer(Type.Index, 3, new short[0]);

        // scale colors from 0 - 255 range into 0 - 1
        m.getBuffer(Type.Color).setNormalized(true);

        // The buffers are always array-based: one quad is stored in these
        // arrays and reused for every quad of this page.
        pos = new float[4 * 3];  // 4 vertices * 3 floats
        tc = new float[4 * 2];  // 4 vertices * 2 floats
        color = new byte[4 * 4];   // 4 vertices * 4 bytes
    }

    /**
     * @deprecated The arrayBased parameter is ignored; array-based buffers are
     * always used. Use {@link #BitmapTextPage(BitmapFont, int)} instead.
     *
     * @param font the font to use (not null)
     * @param arrayBased ignored
     * @param page the index of the font page to display
     */
    @Deprecated
    BitmapTextPage(BitmapFont font, boolean arrayBased, int page) {
        this(font, page);
    }

    /**
     * @deprecated The arrayBased parameter is ignored; array-based buffers are
     * always used. Use {@link #BitmapTextPage(BitmapFont, int)} instead.
     *
     * @param font the font to use (not null)
     * @param arrayBased ignored
     */
    @Deprecated
    BitmapTextPage(BitmapFont font, boolean arrayBased) {
        this(font, 0);
    }

    BitmapTextPage(BitmapFont font) {
        this(font, 0);
    }

    Texture2D getTexture() {
        return texture;
    }

    @Override
    public BitmapTextPage clone() {
        return (BitmapTextPage) super.clone();
    }

    /**
     * Called internally by com.jme3.util.clone.Cloner. Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        Mesh originalMesh = this.mesh;
        super.cloneFields(cloner, original);

        // Each text needs independent mesh data, even when the caller did not
        // request deep mesh cloning.
        if (this.mesh == originalMesh) {
            this.mesh = mesh.deepClone();
        }
        pos = pos.clone();
        tc = tc.clone();
        color = color.clone();
        // BitmapText rebuilds the clone's membership from its own Letters.
        pageQuads = new ArrayList<>(pageQuads.size());
        // A deep-cloned buffer may contain only its logical length, rather
        // than the original buffer's spare, previously initialized capacity.
        initializedIndexQuads = mesh.getBuffer(Type.Index).getData().limit() / 6;
    }

    /**
     * Clears cached membership before BitmapText redistributes its letters.
     */
    void clearQuads() {
        pageQuads.clear();
    }

    /**
     * Adds a printable quad in text order during the layout pass.
     *
     * @param quad the quad belonging to this font page
     */
    void addQuad(LetterQuad quad) {
        pageQuads.add(quad);
    }

    /**
     * Rebuilds layout-dependent buffers from this page's cached quads.
     */
    void assemble() {
        Mesh m = getMesh();
        int quadCount = pageQuads.size();
        boolean countChanged = m.getVertexCount() != quadCount * 4;
        if (quadCount == 0 && !countChanged) {
            return;
        }

        VertexBuffer pb = m.getBuffer(Type.Position);
        VertexBuffer tb = m.getBuffer(Type.TexCoord);
        VertexBuffer cb = m.getBuffer(Type.Color);
        FloatBuffer fpb = prepare((FloatBuffer) pb.getData(), quadCount * 12);
        FloatBuffer ftb = prepare((FloatBuffer) tb.getData(), quadCount * 8);
        ByteBuffer bcb = prepare((ByteBuffer) cb.getData(), quadCount * 16);

        for (int i = 0; i < quadCount; i++) {
            LetterQuad quad = pageQuads.get(i);
            quad.storeToArrays(pos, tc, color);
            fpb.put(pos);
            ftb.put(tc);
            bcb.put(color);
        }
        fpb.rewind();
        ftb.rewind();
        bcb.rewind();
        pb.updateData(fpb);
        tb.updateData(ftb);
        cb.updateData(bcb);

        // Index topology depends only on how many quads this page contains.
        if (countChanged) {
            updateIndices(quadCount);
            m.updateCounts();
        }
        updateModelBound();
    }

    /**
     * Refreshes vertex colors without changing layout, indices, or bounds.
     */
    void updateColors() {
        if (pageQuads.isEmpty()) {
            return;
        }
        VertexBuffer cb = getMesh().getBuffer(Type.Color);
        ByteBuffer bcb = (ByteBuffer) cb.getData();
        boolean changed = false;
        for (int i = 0; i < pageQuads.size(); i++) {
            pageQuads.get(i).storeColors(color);
            int offset = i * color.length;
            // All four vertices of a glyph have the same RGBA tuple. Compare
            // bytes explicitly so the buffer's native byte order is irrelevant.
            if (bcb.get(offset) != color[0] || bcb.get(offset + 1) != color[1]
                    || bcb.get(offset + 2) != color[2] || bcb.get(offset + 3) != color[3]) {
                bcb.position(offset);
                bcb.put(color);
                changed = true;
            }
        }
        bcb.rewind();
        if (changed) {
            cb.updateData(bcb);
        }
    }

    private void updateIndices(int quadCount) {
        VertexBuffer ib = getMesh().getBuffer(Type.Index);
        ShortBuffer indices = (ShortBuffer) ib.getData();
        int required = quadCount * 6;
        if (indices.capacity() < required) {
            indices = BufferUtils.createShortBuffer(growCapacity(indices.capacity(), required));
            initializedIndexQuads = 0;
        }
        indices.clear();
        if (quadCount > initializedIndexQuads) {
            indices.position(initializedIndexQuads * 6);
            for (int i = initializedIndexQuads; i < quadCount; i++) {
                short first = (short) (i * 4);
                indices.put(first).put((short) (first + 1)).put((short) (first + 2));
                indices.put(first).put((short) (first + 2)).put((short) (first + 3));
            }
            initializedIndexQuads = quadCount;
        }
        indices.rewind();
        indices.limit(required);
        // Even a shrink must notify the renderer of the changed draw length.
        ib.updateData(indices);
    }

    private static FloatBuffer prepare(FloatBuffer buffer, int required) {
        if (buffer.capacity() < required) {
            buffer = BufferUtils.createFloatBuffer(growCapacity(buffer.capacity(), required));
        }
        buffer.clear();
        buffer.limit(required);
        return buffer;
    }

    private static ByteBuffer prepare(ByteBuffer buffer, int required) {
        if (buffer.capacity() < required) {
            buffer = BufferUtils.createByteBuffer(growCapacity(buffer.capacity(), required));
        }
        buffer.clear();
        buffer.limit(required);
        return buffer;
    }

    private static int growCapacity(int capacity, int required) {
        // Leave spare capacity for incrementally growing text. All buffer
        // limits still describe only the currently used vertices or indices.
        long grown = (long) capacity + capacity / 2;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(grown, required));
    }
}
