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

import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * <code>WrappedIndexBuffer</code> converts vertex indices from a non list based
 * mesh mode such as {@link Mode#TriangleStrip} or {@link Mode#LineLoop}
 * into a list based mode such as {@link Mode#Triangles} or {@link Mode#Lines}.
 * As it is often more convenient to read vertex data in list format
 * than in a non-list format, using this class is recommended to avoid
 * convoluting classes used to process mesh data from an external source.
 * 
 * @author Kirill Vainer
 */
public class WrappedIndexBuffer extends VirtualIndexBuffer {

    private final IndexBuffer ib;

    public WrappedIndexBuffer(Mesh mesh){
        super(mesh.getVertexCount(), mesh.getMode());
        this.ib = mesh.getIndexBuffer();
        switch (meshMode){
            case Points:
                numIndices = mesh.getTriangleCount();
                break;
            case Lines:
            case LineLoop:
            case LineStrip:
                numIndices = mesh.getTriangleCount() * 2;
                break;
            case Triangles:
            case TriangleStrip:
            case TriangleFan:
                numIndices = mesh.getTriangleCount() * 3;
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public int get(int i) {
        int superIdx = super.get(i);
        return ib.get(superIdx);
    }

    @Override
    public Buffer getBuffer() {
        return ib.getBuffer();
    }
    
    public static void convertToList(Mesh mesh){
        IndexBuffer inBuf = mesh.getIndicesAsList();
        IndexBuffer outBuf = IndexBuffer.createIndexBuffer(mesh.getVertexCount(),
                                                           inBuf.size());

        for (int i = 0; i < inBuf.size(); i++){
            outBuf.put(i, inBuf.get(i));
        }

        mesh.clearBuffer(Type.Index);
        switch (mesh.getMode()){
            case LineLoop:
            case LineStrip:
                mesh.setMode(Mode.Lines);
                break;
            case TriangleStrip:
            case TriangleFan:
                mesh.setMode(Mode.Triangles);
                break;
            default:
                break;
        }
        if (outBuf instanceof IndexIntBuffer){
            mesh.setBuffer(Type.Index, 3, (IntBuffer)outBuf.getBuffer());
        }else{
            mesh.setBuffer(Type.Index, 3, (ShortBuffer)outBuf.getBuffer());
        }
    }
    
}
