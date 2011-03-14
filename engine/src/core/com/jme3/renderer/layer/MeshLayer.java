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

package com.jme3.renderer.layer;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

/**
 * Layer for handling meshes.
 */
public interface MeshLayer {

    /**
     * Uploads the vertex buffer's data onto the GPU, assiging it an ID if
     * needed.
     */
    public void updateBufferData(VertexBuffer vb);

    /**
     * Deletes a vertex buffer from the GPU.
     * @param vb The vertex buffer to delete
     */
    public void deleteBuffer(VertexBuffer vb);

    /**
     * Sets the vertex attrib. This data is exposed in the shader depending
     * on type, e.g Type.Position would be given as inPosition, Type.Tangent is
     * given as inTangent, etc.
     *
     * @param vb
     * @throws InvalidArgumentException If the given vertex buffer is an
     * index buffer.
     */
    public void setVertexAttrib(VertexBuffer vb);

    /**
     * Draws the list of triangles given in the index buffer.
     * Each triangle is composed of 3 indices to a vertex, the attribs of which
     * are supplied using <code>setVertexAttrib</code>.
     * The int variable gl_VertexID can be used to access the current
     * vertex index inside the vertex shader.
     *
     * @param count The number of instances to draw
     */
    public void drawTriangleList(VertexBuffer indexBuf, Mesh.Mode mode, int count, int vertCount);

    /**
     * Clears all vertex attributes set with <code>setVertexAttrib</code>.
     */
    public void clearVertexAttribs();

    /**
     * Renders <code>count</code> meshes, with the geometry data supplied.
     * The shader which is currently set with <code>setShader</code> is
     * responsible for transforming the input verticies into clip space
     * and shading it based on the given vertex attributes.
     * The int variable gl_InstanceID can be used to access the current
     * instance of the mesh being rendered inside the vertex shader.
     *
     * @param mesh
     * @param count
     */
    public void renderMesh(Mesh mesh, int count);

}
