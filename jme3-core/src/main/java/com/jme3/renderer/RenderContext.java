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
package com.jme3.renderer;

import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;

/**
 * Represents the current state of the graphics library. This class is used
 * internally to reduce state changes. NOTE: This class is specific to OpenGL.
 */
public class RenderContext {

    /**
     * @see RenderState#setFaceCullMode(com.jme3.material.RenderState.FaceCullMode)
     */
    public RenderState.FaceCullMode cullMode = RenderState.FaceCullMode.Off;

    /**
     * @see RenderState#setDepthTest(boolean) 
     */
    public boolean depthTestEnabled = false;

    /**
     * @see RenderState#setDepthWrite(boolean) 
     */
    public boolean depthWriteEnabled = true;

    /**
     * @see RenderState#setColorWrite(boolean) 
     */
    public boolean colorWriteEnabled = true;

    /**
     * @see Renderer#setClipRect(int, int, int, int) 
     */
    public boolean clipRectEnabled = false;

    /**
     * @see RenderState#setPolyOffset(float, float) 
     */
    public boolean polyOffsetEnabled = false;
    
    /**
     * @see RenderState#setPolyOffset(float, float) 
     */
    public float polyOffsetFactor = 0;
    
    /**
     * @see RenderState#setPolyOffset(float, float) 
     */
    public float polyOffsetUnits = 0;

    /**
     * @see Mesh#setPointSize(float) 
     */
    public float pointSize = 1;
    
    /**
     * @see RenderState#setLineWidth(float)
     */
    public float lineWidth = 1;

    /**
     * @see RenderState#setBlendMode(com.jme3.material.RenderState.BlendMode) 
     */
    public RenderState.BlendMode blendMode = RenderState.BlendMode.Off;

    /**
     * @see RenderState#setBlendEquation(com.jme3.material.RenderState.BlendEquation) 
     */
    public RenderState.BlendEquation blendEquation = RenderState.BlendEquation.Add;
    
    /**
     * @see RenderState#setBlendEquationAlpha(com.jme3.material.RenderState.BlendEquationAlpha) 
     */
    public RenderState.BlendEquationAlpha blendEquationAlpha = RenderState.BlendEquationAlpha.InheritColor;

    /**
     * @see RenderState#setWireframe(boolean) 
     */
    public boolean wireframe = false;

    /**
     * @see Renderer#setShader(com.jme3.shader.Shader) 
     */
    public int boundShaderProgram;
    
    /**
     * @see Renderer#setShader(com.jme3.shader.Shader) 
     */
    public Shader boundShader;

    /**
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer) 
     */
    public int boundFBO = 0;
    
    /**
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer) 
     */
    public FrameBuffer boundFB;

    /**
     * Currently bound Renderbuffer
     * 
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer) 
     */
    public int boundRB = 0;

    /**
     * Currently bound draw buffer
     * -2 = GL_NONE
     * -1 = GL_BACK
     *  0 = GL_COLOR_ATTACHMENT0
     *  n = GL_COLOR_ATTACHMENTn
     *  where n is an integer greater than 1
     * 
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer) 
     * @see FrameBuffer#setTargetIndex(int) 
     */
    public int boundDrawBuf = -1;

    /**
     * Currently bound read buffer
     *
     * @see RenderContext#boundDrawBuf
     * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer) 
     * @see FrameBuffer#setTargetIndex(int) 
     */
    public int boundReadBuf = -1;

    /**
     * Currently bound element array vertex buffer.
     * 
     * @see Renderer#renderMesh(com.jme3.scene.Mesh, int, int) 
     */
    public int boundElementArrayVBO;

    /**
     * @see Renderer#renderMesh(com.jme3.scene.Mesh, int, int) 
     */
    public int boundVertexArray;

    /**
     * Currently bound array vertex buffer.
     * 
     * @see Renderer#renderMesh(com.jme3.scene.Mesh, int, int) 
     */
    public int boundArrayVBO;
    
    /**
     * Currently bound pixel pack pixel buffer.
     */
    public int boundPixelPackPBO;

    public int numTexturesSet = 0;

    /**
     * Current bound texture IDs for each texture unit.
     * 
     * @see Renderer#setTexture(int, com.jme3.texture.Texture) 
     */
    public Image[] boundTextures = new Image[16];

    /**
     * IDList for texture units
     * 
     * @see Renderer#setTexture(int, com.jme3.texture.Texture) 
     */
    public IDList textureIndexList = new IDList();

    /**
     * Currently bound texture unit
     * 
     * @see Renderer#setTexture(int, com.jme3.texture.Texture) 
     */
    public int boundTextureUnit = 0;

    /**
     * Stencil Buffer state
     */
    public boolean stencilTest = false;
    public RenderState.StencilOperation frontStencilStencilFailOperation = RenderState.StencilOperation.Keep;
    public RenderState.StencilOperation frontStencilDepthFailOperation = RenderState.StencilOperation.Keep;
    public RenderState.StencilOperation frontStencilDepthPassOperation = RenderState.StencilOperation.Keep;
    public RenderState.StencilOperation backStencilStencilFailOperation = RenderState.StencilOperation.Keep;
    public RenderState.StencilOperation backStencilDepthFailOperation = RenderState.StencilOperation.Keep;
    public RenderState.StencilOperation backStencilDepthPassOperation = RenderState.StencilOperation.Keep;
    public RenderState.TestFunction frontStencilFunction = RenderState.TestFunction.Always;
    public RenderState.TestFunction backStencilFunction = RenderState.TestFunction.Always;

    /**
     * Vertex attribs currently bound and enabled. If a slot is null, then
     * it is disabled.
     */
    public VertexBuffer[] boundAttribs = new VertexBuffer[16];

    /**
     * IDList for vertex attributes
     */
    public IDList attribIndexList = new IDList();
    
    /**
     * depth test function
     */
    public RenderState.TestFunction depthFunc = RenderState.TestFunction.Less;
    
     /**
     * alpha test function
     */
    public RenderState.TestFunction alphaFunc = RenderState.TestFunction.Greater;

    public int initialDrawBuf;
    public int initialReadBuf;
    
    public ColorRGBA clearColor = new ColorRGBA(0,0,0,0);
    
    /**
     * Reset the RenderContext to default GL state
     */
    public void reset(){
        cullMode = RenderState.FaceCullMode.Off;
        depthTestEnabled = false;
        depthWriteEnabled = false;
        colorWriteEnabled = false;
        clipRectEnabled = false;
        polyOffsetEnabled = false;
        polyOffsetFactor = 0;
        polyOffsetUnits = 0;
        pointSize = 1;
        blendMode = RenderState.BlendMode.Off;
        blendEquation = RenderState.BlendEquation.Add;
        blendEquationAlpha = RenderState.BlendEquationAlpha.InheritColor;
        wireframe = false;
        boundShaderProgram = 0;
        boundShader = null;
        boundFBO = 0;
        boundFB = null;
        boundRB = 0;
        boundDrawBuf = -1; 
        boundReadBuf = -1;
        boundElementArrayVBO = 0;
        boundVertexArray = 0;
        boundArrayVBO = 0;
        boundPixelPackPBO = 0;
        numTexturesSet = 0;
        for (int i = 0; i < boundTextures.length; i++)
            boundTextures[i] = null;

        textureIndexList.reset();
        boundTextureUnit = 0;
        for (int i = 0; i < boundAttribs.length; i++)
            boundAttribs[i] = null;

        attribIndexList.reset();
        
        stencilTest = false;
        frontStencilStencilFailOperation = RenderState.StencilOperation.Keep;
        frontStencilDepthFailOperation = RenderState.StencilOperation.Keep;
        frontStencilDepthPassOperation = RenderState.StencilOperation.Keep;
        backStencilStencilFailOperation = RenderState.StencilOperation.Keep;
        backStencilDepthFailOperation = RenderState.StencilOperation.Keep;
        backStencilDepthPassOperation = RenderState.StencilOperation.Keep;
        frontStencilFunction = RenderState.TestFunction.Always;
        backStencilFunction = RenderState.TestFunction.Always;
        
        depthFunc = RenderState.TestFunction.LessOrEqual;    
        alphaFunc = RenderState.TestFunction.Greater;
        clearColor.set(0,0,0,0);
    }
}
