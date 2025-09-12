/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jme3.scene.VertexBuffer.Format;
import com.jme3.util.BufferUtils;

/**
 * <code>IndexBuffer</code> is an abstraction for integer index buffers,
 * it is used to retrieve indices without knowing in which format they 
 * are stored (ushort or uint).
 *
 * @author lex
 */
public abstract class IndexBuffer {
    
    public static IndexBuffer wrapIndexBuffer(Buffer buf) {
        if (buf instanceof ByteBuffer) {
            return new IndexByteBuffer((ByteBuffer) buf);
        } else if (buf instanceof ShortBuffer) {
            return new IndexShortBuffer((ShortBuffer) buf);
        } else if (buf instanceof IntBuffer) {
            return new IndexIntBuffer((IntBuffer) buf);
        } else {
            throw new UnsupportedOperationException("Index buffer type unsupported: "+ buf.getClass());
        }
    }

    /**
     * Create an IndexBuffer with the specified capacity.
     *
     * @param vertexCount the number of vertices that will be indexed into
     * (determines number of bits per element)
     * @param indexCount the number of indices the IndexBuffer must hold
     * (determines number of elements in the buffer)
     * @return a new, appropriately sized IndexBuffer, which may be an
     * {@link IndexByteBuffer}, an {@link IndexShortBuffer}, or an
     * {@link IndexIntBuffer}
     */
    public static IndexBuffer createIndexBuffer(int vertexCount,
            int indexCount) {
        IndexBuffer result;

        if (vertexCount < 128) { // TODO: could be vertexCount <= 256
            ByteBuffer buffer = BufferUtils.createByteBuffer(indexCount);
            int maxIndexValue = Math.max(0, vertexCount - 1);
            result = new IndexByteBuffer(buffer, maxIndexValue);

        } else if (vertexCount < 65536) { // TODO: could be <= 65536
            ShortBuffer buffer = BufferUtils.createShortBuffer(indexCount);
            int maxIndexValue = vertexCount - 1;
            result = new IndexShortBuffer(buffer, maxIndexValue);

        } else {
            IntBuffer buffer = BufferUtils.createIntBuffer(indexCount);
            result = new IndexIntBuffer(buffer);
        }

        return result;
    }

    /**
     * @see Buffer#rewind()
     */
    public void rewind() {
        getBuffer().rewind();
    }

    /**
     * @return the count (&ge;0)
     * @see Buffer#remaining()
     */
    public int remaining() {
        return getBuffer().remaining();
    }

    /**
     * Returns the vertex index for the current position.
     *
     * @return the index
     */
    public abstract int get();

    /**
     * Returns the vertex index for the given index in the index buffer.
     * 
     * @param i The index inside the index buffer
     * @return the index
     */
    public abstract int get(int i);
    
    /**
     * Absolute put method.
     * 
     * <p>Puts the vertex index at the index buffer's index.
     * Implementations may throw an {@link UnsupportedOperationException}
     * if modifying the IndexBuffer is not supported (e.g. virtual index
     * buffers).</p>
     * 
     * @param i The buffer index
     * @param value The vertex index
     * @return This buffer
     */
    public abstract IndexBuffer put(int i, int value);
    
    /**
     * Relative put method.
     * 
     * <p>Puts the vertex index at the current position, then increments the
     * position. Implementations may throw an 
     * {@link UnsupportedOperationException} if modifying the IndexBuffer is not
     * supported (e.g. virtual index buffers).</p>
     * 
     * @param value The vertex index
     * @return This buffer
     */
    public abstract IndexBuffer put(int value);
    
    /**
     * Returns the size of the index buffer.
     * 
     * @return the size of the index buffer.
     */
    public abstract int size();
    
    /**
     * Returns the underlying data-type specific {@link Buffer}.
     * Implementations may return null if there's no underlying
     * buffer.
     * 
     * @return the underlying {@link Buffer}.
     */
    public abstract Buffer getBuffer();
    
    /**
     * Returns the format of the data stored in this buffer.
     * 
     * <p>This method can be used to set an {@link IndexBuffer} to a 
     * {@link com.jme3.scene.Mesh Mesh}:</p>
     * <pre>
     * mesh.setBuffer(Type.Index, 3, 
     *     indexBuffer.getFormat(), indexBuffer);
     * </pre>
     * @return an enum value
     */
    public abstract Format getFormat();
}
