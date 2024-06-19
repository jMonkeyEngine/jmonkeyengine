/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer.framegraph;

import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

/**
 * A quad specifically for rendering fullscreen.
 * 
 * @author codex
 */
public class FullScreenQuad {
    
    private final Geometry geometry;
    private final Material transferMat;
    
    /**
     * 
     * @param assetManager 
     */
    public FullScreenQuad(AssetManager assetManager) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(
            0, 0, 0,
            1, 0, 0,
            0, 1, 0,
            1, 1, 0
        ));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(
            0, 1, 2,
            1, 3, 2
        ));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(
            1, 0,
            1, 1,
            0, 0,
            0, 1
        ));
        mesh.updateBound();
        mesh.updateCounts();
        mesh.setStatic();
        geometry = new Geometry("Screen", mesh);
        transferMat = new Material(assetManager, "Common/MatDefs/ShadingCommon/TextureTransfer.j3md");
    }
    
    /**
     * Renders the material on the quad.
     * 
     * @param rm
     * @param material 
     */
    public void render(RenderManager rm, Material material) {
        geometry.setMaterial(material);
        geometry.updateGeometricState();
        rm.renderGeometry(geometry);
    }
    /**
     * Renders the material with the light list on the quad.
     * 
     * @param rm
     * @param material
     * @param lights 
     */
    public void render(RenderManager rm, Material material, LightList lights) {
        geometry.setMaterial(material);
        geometry.updateGeometricState();
        rm.renderGeometry(geometry, lights);
    }
    /**
     * Renders the color and depth textures on the quad, where the depth texture
     * informs the depth value.
     * 
     * @param rm
     * @param color color texture, or null
     * @param depth depth texture, or null
     */
    public void render(RenderManager rm, Texture2D color, Texture2D depth) {
        boolean writeDepth = depth != null;
        if (color != null || writeDepth) {
            transferMat.setTexture("ColorMap", color);
            transferMat.setTexture("DepthMap", depth);
            transferMat.getAdditionalRenderState().setDepthTest(writeDepth);
            transferMat.getAdditionalRenderState().setDepthWrite(writeDepth);
            render(rm, transferMat);
            setAlphaDiscard(null);
        }
    }
    
    public void setAlphaDiscard(Float alphaDiscard) {
        if (alphaDiscard == null) {
            transferMat.clearParam("AlphaDiscard");
        } else {
            transferMat.setFloat("AlphaDiscard", alphaDiscard);
        }
    }
    
}
