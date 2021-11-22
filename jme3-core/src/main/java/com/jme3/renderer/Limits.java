/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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

/**
 * Allows querying the limits of certain features in
 * {@link Renderer}.
 *
 * <p>For example, maximum texture sizes or number of samples.
 *
 * @author Kirill Vainer
 */
public enum Limits {
    /**
     * Maximum number of vertex texture units, or number of textures that can be
     * used in the vertex shader.
     */
    VertexTextureUnits,
    /**
     * Maximum number of fragment texture units, or number of textures that can
     * be used in the fragment shader.
     */
    FragmentTextureUnits,
    /**
     * Maximum number of fragment uniform vectors.
     */
    FragmentUniformVectors,
    /**
     * Maximum number of vertex uniform vectors.
     */
    VertexUniformVectors,
    /**
     * Maximum number of vertex attributes.
     */
    VertexAttributes,
    /**
     * Maximum number of FrameBuffer samples.
     */
    FrameBufferSamples,
    /**
     * Maximum number of FrameBuffer attachments.
     */
    FrameBufferAttachments,
    /**
     * Maximum number of FrameBuffer MRT attachments.
     */
    FrameBufferMrtAttachments,
    /**
     * Maximum render buffer size.
     */
    RenderBufferSize,
    /**
     * Maximum texture size.
     */
    TextureSize,
    /**
     * Maximum cubemap size.
     */
    CubemapSize,
    /**
     * Maximum number of color texture samples.
     */
    ColorTextureSamples,
    /**
     * Maximum number of depth texture samples.
     */
    DepthTextureSamples,
    /**
     * Maximum degree of texture anisotropy.
     */
    TextureAnisotropy,

    // UBO
    /**
     * Maximum number of UBOs that may be accessed by a vertex shader.
     */
    UniformBufferObjectMaxVertexBlocks,
    /**
     * Maximum number of UBOs that may be accessed by a fragment shader.
     */
    UniformBufferObjectMaxFragmentBlocks,
    /**
     * Maximum number of UBOs that may be accessed by a geometry shader.
     */
    UniformBufferObjectMaxGeometryBlocks,
    /**
     * Maximum block size of a UBO.
     */
    UniformBufferObjectMaxBlockSize,

    // SSBO
    /**
     * Maximum size of an SSBO.
     */
    ShaderStorageBufferObjectMaxBlockSize,
    /**
     * Maximum number of active SSBOs that may be accessed by a vertex shader.
     */
    ShaderStorageBufferObjectMaxVertexBlocks,
    /**
     * Maximum number of active SSBOs that may be accessed by a fragment shader.
     */
    ShaderStorageBufferObjectMaxFragmentBlocks,
    /**
     * Maximum number of active SSBOs that may be accessed by a geometry shader.
     */
    ShaderStorageBufferObjectMaxGeometryBlocks,
    /**
     * Maximum number of active SSBOs that may be accessed by a tessellation control shader.
     */
    ShaderStorageBufferObjectMaxTessControlBlocks,
    /**
     * Maximum number of active SSBOs that may be accessed by a tessellation evaluation shader.
     */
    ShaderStorageBufferObjectMaxTessEvaluationBlocks,
    /**
     * Not implemented yet.
     */
    ShaderStorageBufferObjectMaxComputeBlocks,
    /**
     * Maximum number shader storage blocks across all active programs.
     */
    ShaderStorageBufferObjectMaxCombineBlocks,
}
