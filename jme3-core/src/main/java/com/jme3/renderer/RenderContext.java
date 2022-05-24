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
package com.jme3.renderer;

import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import java.lang.ref.WeakReference;

/**
 * Represents the current state of the graphics library. This class is used
 * internally to reduce state changes. NOTE: This class is specific to OpenGL.
 */
public class RenderContext {
    /**
     * Number of texture units that JME supports.
     */
    public static final int maxTextureUnits = 16;

    /**
     * Criteria for culling faces.
     *
     * @see RenderState#setFaceCullMode(com.jme3.material.RenderState.FaceCullMode)
     */
    public RenderState.FaceCullMode cullMode;

    /**
     * Enables depth testing for color pixels.
     *
     * @see RenderState#setDepthTest(boolean)
     */
    public boolean depthTestEnabled;

    /**
     * Enables depth writing.
     *
     * @see RenderState#setDepthWrite(boolean)
     */
    public boolean depthWriteEnabled;

    /**
     * Enables color writing.
     *
     * @see RenderState#setColorWrite(boolean)
     */
    public boolean colorWriteEnabled;

    /**
     * Enables the clipping rectangle.
     *
     * @see Renderer#setClipRect(int, int, int, int)
     */
    public boolean clipRectEnabled;

    /**
     * Enables z-order offset for polygons.
     *
     * @see RenderState#setPolyOffset(float, float)
     */
    public boolean polyOffsetEnabled;

    /**
     * Maximum Z slope for z-order offset.
     *
     * @see RenderState#setPolyOffset(float, float)
     */
    public float polyOffsetFactor;

    /**
     * Minimum resolvable depth buffer value for z-order offset.
     *
     * @see RenderState#setPolyOffset(float, float)
     */
    public float polyOffsetUnits;

    /**
     * No longer used.
     */
    public float pointSize;

    /**
     * Line width for meshes.
     *
     * @see RenderState#setLineWidth(float)
     */
    public float lineWidth;

    /**
     * How to blend input pixels with those already in the color buffer.
     *
     * @see RenderState#setBlendMode(com.jme3.material.RenderState.BlendMode)
     */
    public RenderState.BlendMode blendMode;

    /**
     * RGB blend equation for BlendMode.Custom.
     *
     * @see RenderState#setBlendEquation(com.jme3.material.RenderState.BlendEquation)
     */
    public RenderState.BlendEquation blendEquation;

    /**
     * Alpha blend equation for BlendMode.Custom.
     *
     * @see RenderState#setBlendEquationAlpha(com.jme3.material.RenderState.BlendEquationAlpha)
     */
    public RenderState.BlendEquationAlpha blendEquationAlpha;

    /**
     * RGB source blend factor for BlendMode.Custom.
     *
     * @see RenderState#setCustomBlendFactors(com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc, com.jme3.material.RenderState.BlendFunc)
     */
    public RenderState.BlendFunc sfactorRGB;

    /**
     * RGB destination blend factor for BlendMode.Custom.
     *
     * @see RenderState#setCustomBlendFactors(com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc, com.jme3.material.RenderState.BlendFunc)
     */
    public RenderState.BlendFunc dfactorRGB;

    /**
     * Alpha source blend factor for BlendMode.Custom.
     *
     * @see RenderState#setCustomBlendFactors(com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc, com.jme3.material.RenderState.BlendFunc)
     */
    public RenderState.BlendFunc sfactorAlpha;

    /**
     * Alpha destination blend factor for BlendMode.Custom.
     *
     * @see RenderState#setCustomBlendFactors(com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc,
     *      com.jme3.material.RenderState.BlendFunc, com.jme3.material.RenderState.BlendFunc)
     */
    public RenderState.BlendFunc dfactorAlpha;

    /**
     * Enables wireframe rendering of triangle meshes.
     *
     * @see RenderState#setWireframe(boolean)
     */
    public boolean wireframe;

    /**
     * ID of the shader for rendering.
     *
     * @see Renderer#setShader(com.jme3.shader.Shader)
     */
    public int boundShaderProgram;

    /**
     * Shader for rendering.
     *
     * @see Renderer#setShader(com.jme3.shader.Shader)
     */
    public Shader boundShader;

    /**
     * ID of the bound FrameBuffer.
     *
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer)
     */
    public int boundFBO;

    /**
     * Currently bound FrameBuffer.
     *
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer)
     */
    public FrameBuffer boundFB;

    /**
     * Currently bound Renderbuffer.
     *
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer)
     */
    public int boundRB;

  
    /**
     * Currently bound element array vertex buffer.
     *
     * @see Renderer#renderMesh(com.jme3.scene.Mesh, int, int, com.jme3.scene.VertexBuffer[])
     */
    public int boundElementArrayVBO;

    /**
     * ID of the bound vertex array.
     *
     * @see Renderer#renderMesh(com.jme3.scene.Mesh, int, int, com.jme3.scene.VertexBuffer[])
     */
    public int boundVertexArray;

    /**
     * Currently bound array vertex buffer.
     *
     * @see Renderer#renderMesh(com.jme3.scene.Mesh, int, int, com.jme3.scene.VertexBuffer[])
     */
    public int boundArrayVBO;

    /**
     * Currently bound pixel pack pixel buffer.
     */
    public int boundPixelPackPBO;

    /**
     * No longer used.
     */
    public int numTexturesSet;

    /**
     * Current bound texture IDs for each texture unit.
     *
     * @see Renderer#setTexture(int, com.jme3.texture.Texture)
     */
    public final WeakReference<Image> boundTextures[]
            = new WeakReference[maxTextureUnits];

    /**
     * IDList for texture units.
     *
     * @see Renderer#setTexture(int, com.jme3.texture.Texture)
     */
    public final IDList textureIndexList = new IDList();

    /**
     * Currently bound texture unit.
     *
     * @see Renderer#setTexture(int, com.jme3.texture.Texture)
     */
    public int boundTextureUnit;

    /**
     * Stencil Buffer state.
     */
    public boolean stencilTest;
    /**
     * Action taken when the stencil test fails on a front-facing polygon.
     */
    public RenderState.StencilOperation frontStencilStencilFailOperation;
    /**
     * Action taken when the stencil test passes but the depth test fails on a front-facing polygon.
     */
    public RenderState.StencilOperation frontStencilDepthFailOperation;
    /**
     * Action taken when both tests pass on a front-facing polygon.
     */
    public RenderState.StencilOperation frontStencilDepthPassOperation;
    /**
     * Action taken when the stencil test fails on a back-facing polygon.
     */
    public RenderState.StencilOperation backStencilStencilFailOperation;
    /**
     * Action taken when the stencil test passes but the depth test fails on a back-facing polygon.
     */
    public RenderState.StencilOperation backStencilDepthFailOperation;
    /**
     * Action taken when both tests pass on a back-facing polygon.
     */
    public RenderState.StencilOperation backStencilDepthPassOperation;
    /**
     * Stencil test function for front-facing polygons.
     */
    public RenderState.TestFunction frontStencilFunction;
    /**
     * Stencil test function for back-facing polygons.
     */
    public RenderState.TestFunction backStencilFunction;

    /**
     * Vertex attribs currently bound and enabled. If a slot is null, then
     * it is disabled.
     */
    public final WeakReference<VertexBuffer>[] boundAttribs = new WeakReference[16];

    /**
     * IDList for vertex attributes.
     */
    public final IDList attribIndexList = new IDList();

    /**
     * Depth test function.
     */
    public RenderState.TestFunction depthFunc;

    /**
     * Alpha test function.
     */
    public RenderState.TestFunction alphaFunc;

    /**
     * ID of the initial draw buffer.
     */
    public int initialDrawBuf;
    /**
     * ID of the initial read buffer.
     */
    public int initialReadBuf;

    /**
     * Color applied when a color buffer is cleared.
     */
    public ColorRGBA clearColor = new ColorRGBA(0, 0, 0, 0);

    /**
     * Instantiates a context with appropriate default values.
     */
    public RenderContext() {
        init();
    }


    private void init() {
        cullMode = RenderState.FaceCullMode.Off;
        depthTestEnabled = false;
        depthWriteEnabled = true;
        colorWriteEnabled = true;
        clipRectEnabled = false;
        polyOffsetEnabled = false;
        polyOffsetFactor = 0;
        polyOffsetUnits = 0;
        pointSize = 1;
        lineWidth = 1;
        blendMode = RenderState.BlendMode.Off;
        blendEquation = RenderState.BlendEquation.Add;
        blendEquationAlpha = RenderState.BlendEquationAlpha.InheritColor;
        sfactorRGB = RenderState.BlendFunc.One;
        dfactorRGB = RenderState.BlendFunc.One;
        sfactorAlpha = RenderState.BlendFunc.One;
        dfactorAlpha = RenderState.BlendFunc.One;
        wireframe = false;

        boundShaderProgram = 0;
        boundShader = null;
        boundFBO = 0;
        boundFB = null;
        boundRB = 0;

        boundElementArrayVBO = 0;
        boundVertexArray = 0;
        boundArrayVBO = 0;
        boundPixelPackPBO = 0;
        numTexturesSet = 0;
        boundTextureUnit = 0;
        stencilTest = false;

        frontStencilStencilFailOperation = RenderState.StencilOperation.Keep;
        frontStencilDepthFailOperation = RenderState.StencilOperation.Keep;
        frontStencilDepthPassOperation = RenderState.StencilOperation.Keep;
        backStencilStencilFailOperation = RenderState.StencilOperation.Keep;
        backStencilDepthFailOperation = RenderState.StencilOperation.Keep;
        backStencilDepthPassOperation = RenderState.StencilOperation.Keep;
        frontStencilFunction = RenderState.TestFunction.Always;
        backStencilFunction = RenderState.TestFunction.Always;

        depthFunc = RenderState.TestFunction.Less;
        alphaFunc = RenderState.TestFunction.Greater;
        cullMode = RenderState.FaceCullMode.Off;

        clearColor.set(0, 0, 0, 0);
    }

    /**
     * Resets the RenderContext to default GL state.
     */
    public void reset() {
        init();

        for (int i = 0; i < boundTextures.length; i++) {
            boundTextures[i] = null;
        }

        textureIndexList.reset();

        for (int i = 0; i < boundAttribs.length; i++) {
            boundAttribs[i] = null;
        }

        attribIndexList.reset();
    }
}
