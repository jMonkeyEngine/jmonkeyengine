/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
        }
    }
    
}
