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

package com.jme3.renderer;

import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.nio.ByteBuffer;
import java.util.EnumSet;

public interface Renderer {

    /**
     * @return The capabilities of the renderer.
     */
    public EnumSet<Caps> getCaps();

    /**
     * @return The statistics allow tracking of how data
     * per frame, such as number of objects rendered, number of triangles, etc.
     */
    public Statistics getStatistics();

    /**
     * Clears certain channels of the current bound framebuffer.
     *
     * @param color True if to clear colors (RGBA)
     * @param depth True if to clear depth/z
     * @param stencil True if to clear stencil buffer (if available, otherwise
     * ignored)
     */
    public void clearBuffers(boolean color, boolean depth, boolean stencil);

    /**
     * Sets the background (aka clear) color.
     * @param color
     */
    public void setBackgroundColor(ColorRGBA color);

    /**
     * Applies the given renderstate, making the neccessary
     * GL calls so that the state is applied.
     */
    public void applyRenderState(RenderState state);

    /**
     * Set the range of the depth values for objects. 
     * @param start
     * @param end
     */
    public void setDepthRange(float start, float end);

    /**
     * Called when a new frame has been rendered.
     */
    public void onFrame();

    /**
     * @param transform The world transform to use. This changes
     * the world matrix given in the shader.
     */
    public void setWorldMatrix(Matrix4f worldMatrix);

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix);

    public void setViewPort(int x, int y, int width, int height);

    public void setClipRect(int x, int y, int width, int height);

    public void clearClipRect();

    public void setLighting(LightList lights);

    /**
     * @param shader Sets the shader to use for rendering, uploading it
     * if neccessary.
     */
    public void setShader(Shader shader);

    /**
     * @param shader The shader to delete. This method also deletes
     * the attached shader sources.
     */
    public void deleteShader(Shader shader);

    /**
     * Deletes the provided shader source.
     * @param source
     */
    public void deleteShaderSource(ShaderSource source);

    /**
     * Copies contents from src to dst, scaling if neccessary.
     */
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst);

    /**
     * Sets the framebuffer that will be drawn to.
     */
    public void setFrameBuffer(FrameBuffer fb);

    /**
     * Reads the pixels currently stored in the specified framebuffer
     * into the given ByteBuffer object. 
     * Only color pixels are transferred, the format is BGRA with 8 bits 
     * per component. The given byte buffer should have at least
     * fb.getWidth() * fb.getHeight() * 4 bytes remaining.
     * @param fb
     * @param byteBuf
     */
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf);

    /**
     * Deletes a framebuffer and all attached renderbuffers
     */
    public void deleteFrameBuffer(FrameBuffer fb);

    /**
     * Sets the texture to use for the given texture unit.
     */
    public void setTexture(int unit, Texture tex);

    /**
     * Deletes a texture from the GPU.
     * @param tex
     */
    public void deleteImage(Image image);

    /**
     * Uploads a vertex buffer to the GPU.
     * 
     * @param vb The vertex buffer to upload
     */
    public void updateBufferData(VertexBuffer vb);

    /**
     * Deletes a vertex buffer from the GPU.
     * @param vb The vertex buffer to delete
     */
    public void deleteBuffer(VertexBuffer vb);

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
    public void renderMesh(Mesh mesh, int lod, int count);

    /**
     * Called on restart() to reset all GL objects
     */
    public void resetGLObjects();

    /**
     * Called when the display is restarted to delete
     * all created GL objects.
     */
    public void cleanup();

    /**
     * sets alpha to coverage
     * @param value
     */
    public void setAlphaToCoverage(boolean value);
    
}
