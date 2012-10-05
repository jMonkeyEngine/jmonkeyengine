/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.scene.Mesh.Mode;
import java.nio.Buffer;

/**
 * IndexBuffer implementation that generates vertex indices sequentially
 * based on a specific Mesh {@link Mode}.
 * The generated indices are as if the mesh is in the given mode
 * but contains no index buffer, thus this implementation will
 * return the indices if the index buffer was there and contained sequential
 * triangles.
 * Example:
 * <ul>
 * <li>{@link Mode#Triangles}: 0, 1, 2 | 3, 4, 5 | 6, 7, 8 | ...</li>
 * <li>{@link Mode#TriangleStrip}: 0, 1, 2 | 2, 1, 3 | 2, 3, 4 | ...</li>
 * <li>{@link Mode#TriangleFan}: 0, 1, 2 | 0, 2, 3 | 0, 3, 4 | ...</li>
 * </ul>
 * 
 * @author Kirill Vainer
 */
public class VirtualIndexBuffer extends IndexBuffer {

    protected int numVerts = 0;
    protected int numIndices = 0;
    protected Mode meshMode;
 
    public VirtualIndexBuffer(int numVerts, Mode meshMode){
        this.numVerts = numVerts;
        this.meshMode = meshMode;
        switch (meshMode) {
            case Points:
                numIndices = numVerts;
                return;
            case LineLoop:
                numIndices = (numVerts - 1) * 2 + 1;
                return;
            case LineStrip:
                numIndices = (numVerts - 1) * 2;
                return;
            case Lines:
                numIndices = numVerts;
                return;
            case TriangleFan:
                numIndices = (numVerts - 2) * 3;
                return;
            case TriangleStrip:
                numIndices = (numVerts - 2) * 3;
                return;
            case Triangles:
                numIndices = numVerts;
                return;
            case Hybrid:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public int get(int i) {
        if (meshMode == Mode.Triangles || meshMode == Mode.Lines || meshMode == Mode.Points){
            return i;
        }else if (meshMode == Mode.LineStrip){
            return (i + 1) / 2;
        }else if (meshMode == Mode.LineLoop){
            return (i == (numVerts-1)) ? 0 : ((i + 1) / 2);
        }else if (meshMode == Mode.TriangleStrip){
           int triIndex   = i/3;
           int vertIndex  = i%3;
           boolean isBack = (i/3)%2==1;
           if (!isBack){
                return triIndex + vertIndex;
           }else{
               switch (vertIndex){
                   case 0: return triIndex + 1;
                   case 1: return triIndex;
                   case 2: return triIndex + 2;
                   default: throw new AssertionError();
               }
            }
        }else if (meshMode == Mode.TriangleFan){
            int vertIndex = i%3;
            if (vertIndex == 0)
                return 0;
            else
                return (i / 3) + vertIndex;
        }else{
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void put(int i, int value) {
        throw new UnsupportedOperationException("Does not represent index buffer");
    }

    @Override
    public int size() {
        return numIndices;
    }

    @Override
    public Buffer getBuffer() {
        return null;
    }

}
