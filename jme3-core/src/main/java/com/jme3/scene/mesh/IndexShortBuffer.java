/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.scene.mesh;

import java.nio.Buffer;
import java.nio.ShortBuffer;

import com.jme3.scene.VertexBuffer.Format;

/**
 * IndexBuffer implementation for {@link ShortBuffer}s.
 * 
 * @author lex
 */
public class IndexShortBuffer extends IndexBuffer {

    final private ShortBuffer buf;
    /**
     * the largest index value that can be put to the buffer
     */
    private int maxValue = 65_535;

    /**
     * Instantiate an IndexBuffer using the specified ShortBuffer and a maximum
     * index value of 65_535.
     *
     * @param buffer a pre-existing buffer (not null, alias created)
     */
    public IndexShortBuffer(ShortBuffer buffer) {
        buf = buffer;
        buf.rewind();
    }

    /**
     * Instantiate an IndexBuffer using the specified ShortBuffer and set its
     * maximum index value.
     *
     * @param buffer a pre-existing buffer (not null, alias created)
     * @param maxValue the desired maximum index value (&ge;0, &le;65_535)
     */
    public IndexShortBuffer(ShortBuffer buffer, int maxValue) {
        assert maxValue >= 0 && maxValue <= 65_535 : "out of range: " + maxValue;
        this.maxValue = maxValue;

        buf = buffer;
        buf.rewind();
    }

    @Override
    public int get() {
        return buf.get() & 0x0000FFFF;
    }
    @Override
    public int get(int i) {
        return buf.get(i) & 0x0000FFFF;
    }

    @Override
    public IndexShortBuffer put(int i, int value) {
        assert value >= 0 && value <= maxValue 
                : "IndexBuffer was created with elements too small for value="
                + value;

        buf.put(i, (short) value);
        return this;
    }
    
    @Override
    public IndexShortBuffer put(int value) {
        assert value >= 0 && value <= maxValue 
                : "IndexBuffer was created with elements too small for value="
                + value;

        buf.put((short) value);
        return this;
    }

    @Override
    public int size() {
        return buf.limit();
    }

    @Override
    public Buffer getBuffer() {
        return buf;
    }
    
    @Override
    public Format getFormat () {
        return Format.UnsignedShort;
    }
}
