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
import java.nio.ByteBuffer;

import com.jme3.scene.VertexBuffer.Format;

/**
 * IndexBuffer implementation for {@link ByteBuffer}s.
 * 
 * @author lex
 */
public class IndexByteBuffer extends IndexBuffer {

    final private ByteBuffer buf;
    /**
     * the largest index value that can be put to the buffer
     */
    private int maxValue = 255;

    /**
     * Instantiate an IndexBuffer using the specified ByteBuffer and a maximum
     * index value of 255.
     *
     * @param buffer a pre-existing buffer (not null, alias created)
     */
    public IndexByteBuffer(ByteBuffer buffer) {
        buf = buffer;
        buf.rewind();
    }

    /**
     * Instantiate an IndexBuffer using the specified ByteBuffer and set its
     * maximum index value.
     *
     * @param buffer a pre-existing buffer (not null, alias created)
     * @param maxValue the desired maximum index value (&ge;0, &le;255)
     */
    public IndexByteBuffer(ByteBuffer buffer, int maxValue) {
        assert maxValue >= 0 && maxValue <= 255 : "out of range: " + maxValue;
        this.maxValue = maxValue;

        buf = buffer;
        buf.rewind();
    }

    @Override
    public int get() {
        return buf.get() & 0x000000FF;
    }

    @Override
    public int get(int i) {
        return buf.get(i) & 0x000000FF;
    }

    @Override
    public IndexByteBuffer put(int i, int value) {
        assert value >= 0 && value <= maxValue 
                : "IndexBuffer was created with elements too small for value="
                + value;

        buf.put(i, (byte) value);
        return this;
    }
    
    @Override
    public IndexByteBuffer put(int value) {
        assert value >= 0 && value <= maxValue 
                : "IndexBuffer was created with elements too small for value="
                + value;

        buf.put((byte) value);
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
        return Format.UnsignedByte;
    }

}
