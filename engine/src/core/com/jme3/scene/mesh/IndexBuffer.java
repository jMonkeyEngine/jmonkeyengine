/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.util.BufferUtils;
import java.nio.Buffer;

/**
 * <code>IndexBuffer</code> is an abstraction for integer index buffers,
 * it is used to retrieve indices without knowing in which format they 
 * are stored (ushort or uint).
 *
 * @author lex
 */
public abstract class IndexBuffer {
    
    /**
     * Creates an index buffer that can contain the given amount
     * of vertices.
     * Returns {@link IndexShortBuffer}
     * 
     * @param vertexCount The amount of vertices to contain
     * @param indexCount The amount of indices
     * to contain.
     * @return A new index buffer
     */
    public static IndexBuffer createIndexBuffer(int vertexCount, int indexCount){
        if (vertexCount > 65535){
            return new IndexIntBuffer(BufferUtils.createIntBuffer(indexCount));
        }else{
            return new IndexShortBuffer(BufferUtils.createShortBuffer(indexCount));
        }
    }
    
    /**
     * Returns the vertex index for the given index in the index buffer.
     * 
     * @param i The index inside the index buffer
     * @return 
     */
    public abstract int get(int i);
    
    /**
     * Puts the vertex index at the index buffer's index.
     * Implementations may throw an {@link UnsupportedOperationException}
     * if modifying the IndexBuffer is not supported (e.g. virtual index
     * buffers).
     */
    public abstract void put(int i, int value);
    
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
}
