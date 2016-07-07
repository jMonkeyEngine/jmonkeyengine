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
package com.jme3.material.logic;

import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.UniformBinding;
import com.jme3.texture.Texture;
import java.util.EnumSet;

/**
 * <code>TechniqueDefLogic</code> is used to customize how 
 * a material should be rendered.
 * 
 * Typically used to implement {@link LightMode lighting modes}.
 * Implementations can register 
 * {@link TechniqueDef#addShaderUnmappedDefine(java.lang.String) unmapped defines} 
 * in their constructor and then later set them based on the geometry 
 * or light environment being rendered.
 * 
 * @author Kirill Vainer
 */
public interface TechniqueDefLogic {
    
    /**
     * Determine the shader to use for the given geometry / material combination.
     * 
     * @param assetManager The asset manager to use for loading shader source code,
     * shader nodes, and and lookup textures.
     * @param renderManager The render manager for which rendering is to be performed.
     * @param rendererCaps Renderer capabilities. The returned shader must
     * support these capabilities.
     * @param lights The lights with which the geometry shall be rendered. This
     * list must not include culled lights.
     * @param defines The define list used by the technique, any 
     * {@link TechniqueDef#addShaderUnmappedDefine(java.lang.String) unmapped defines}
     * should be set here to change shader behavior.
     * 
     * @return The shader to use for rendering.
     */
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager, 
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines);
    
    /**
     * Requests that the <code>TechniqueDefLogic</code> renders the given geometry.
     * 
     * Fixed material functionality such as {@link RenderState}, 
     * {@link MatParam material parameters}, and 
     * {@link UniformBinding uniform bindings}
     * have already been applied by the material, however, 
     * {@link RenderState}, {@link Uniform uniforms}, {@link Texture textures},
     * can still be overriden.
     * 
     * @param renderManager The render manager to perform the rendering against.
     * * @param shader The shader that was selected by this logic in 
     * {@link #makeCurrent(com.jme3.asset.AssetManager, com.jme3.renderer.RenderManager, java.util.EnumSet, com.jme3.shader.DefineList)}.
     * @param geometry The geometry to render
     * @param lights Lights which influence the geometry.
     */
    public void render(RenderManager renderManager, Shader shader, Geometry geometry, LightList lights, int lastTexUnit);
}
